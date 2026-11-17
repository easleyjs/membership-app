/* admin.js — entity forms and administrative member actions. */
window.App = window.App || {};

(function () {

  const element = (id) => document.getElementById(id);
  const titles = { members: "Member", users: "User", events: "Event" };
  let editor = null;

  function feedback(message) {
    const status = element("adminEditorFeedback");
    status.textContent = message;
    status.hidden = !message;
  }

  function openEditor(section, record, onSaved) {
    if (!App.auth.isAdmin() || editor) return;
    const current = { section, record, onSaved, saving: false };
    editor = current;
    const dialog = element("adminEditor");
    const template = section === "members" ? "memberFields"
      : section === "events" ? "eventFields"
      : record ? "editUserFields" : "newUserFields";
    element("adminEditorTitle").textContent = `${record ? "Edit" : "Add"} ${titles[section]}`;
    const fields = element("adminEditorFields");
    fields.replaceChildren(element(template).content.cloneNode(true));
    for (const input of fields.querySelectorAll("[name]")) {
      if (input.type === "checkbox") input.checked = Boolean(record?.[input.name]);
      else if (record) input.value = record[input.name] ?? "";
    }
    feedback("");
    const choosingMember = section === "users" && !record;
    const save = element("adminEditorSave");
    save.disabled = choosingMember;
    const deactivate = element("adminEditorDeactivate");
    deactivate.hidden = section !== "events" || record?.status !== "ACTIVE";
    deactivate.disabled = false;
    if (choosingMember) bindMemberSearch(current, save);
    dialog.showModal();
    if (section === "members" && record) loadMembershipType(current, save);
  }

  async function loadMembershipType(current, save) {
    const label = element("editorMembership");
    const select = label.querySelector("select");
    label.hidden = false;
    save.disabled = true;
    feedback("Loading membership type…");
    try {
      const { content } = await App.api.getMemberships(current.record.id, 0);
      if (editor !== current) return;
      const active = content.filter(({ program, status }) => program === "OUTLAND" && status === "ACTIVE");
      current.membershipType = active.some(({ type }) => type === "PAID") ? "PAID" : active[0]?.type ?? "";
      select.value = current.membershipType;
      select.disabled = false;
      feedback("");
    } catch (error) {
      if (editor === current) {
        label.hidden = true;
        feedback(`Couldn't load membership type: ${error.message}`);
      }
    } finally {
      if (editor === current) save.disabled = false;
    }
  }

  function bindMemberSearch(current, save) {
    const input = element("userMemberSearch");
    const matches = element("userMemberMatches");
    const status = element("userMemberSearchStatus");
    let searchVersion = 0;
    let searchTimer;

    function showStatus(message) {
      status.textContent = message;
      status.hidden = !message;
    }

    input.addEventListener("input", () => {
      clearTimeout(searchTimer);
      const version = ++searchVersion;
      const query = input.value.trim();
      current.memberId = null;
      save.disabled = true;
      matches.replaceChildren();
      matches.hidden = true;
      feedback("");
      showStatus(query ? "Searching…" : "");
      if (!query) return;

      searchTimer = setTimeout(async () => {
        if (editor !== current) return;
        try {
          const members = await App.api.searchEligibleMembers(query);
          if (editor !== current || searchVersion !== version) return;
          for (const member of members) {
            const label = `${member.firstName ?? ""} ${member.lastName ?? ""}`.trim();
            const choice = document.createElement("button");
            choice.type = "button";
            choice.textContent = `${label} (#${member.memberNumber}) · ${member.email}`;
            choice.addEventListener("click", () => {
              current.memberId = member.id;
              input.value = choice.textContent;
              input.focus();
              matches.replaceChildren();
              matches.hidden = true;
              showStatus("Member selected.");
              save.disabled = false;
            });
            matches.append(choice);
          }
          matches.hidden = members.length === 0;
          showStatus(members.length ? `${members.length} matches shown.` : "No matching members without an account.");
        } catch (error) {
          if (editor === current && searchVersion === version) {
            showStatus(`Couldn't search members: ${error.message}`);
          }
        }
      }, 250);
    });
  }

  function closeEditor() {
    if (!editor || editor.saving) return;
    editor = null;
    element("adminEditor").close();
    element("adminEditorFields").replaceChildren();
  }

  function bindEditor() {
    element("adminEditorClose").addEventListener("click", closeEditor);
    element("adminEditor").addEventListener("cancel", (event) => {
      event.preventDefault();
      closeEditor();
    });
    element("adminEditorForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const current = editor;
      const save = element("adminEditorSave");
      const deactivate = element("adminEditorDeactivate");
      const deactivating = event.submitter === deactivate;
      if (!current || !App.auth.isAdmin() || save.disabled || (deactivating && deactivate.hidden)) return;
      const { section, record, onSaved } = current;
      const form = event.currentTarget;
      const fields = Object.fromEntries(new FormData(form));
      const memberSearch = element("userMemberSearch");
      if (memberSearch) memberSearch.readOnly = true;
      current.saving = true;
      save.disabled = true;
      deactivate.disabled = true;
      feedback(deactivating ? "Deactivating…" : "Saving…");
      try {
        if (deactivating) {
          await App.api.deactivateEvent(record.id);
        } else if (section === "members") {
          for (const key of Object.keys(fields)) fields[key] = fields[key].trim();
          if (!fields.membershipType || fields.membershipType === current.membershipType) delete fields.membershipType;
          await App.api.saveMember(fields, record?.id);
        } else if (section === "users") {
          if (record) {
            await App.api.updateUser(record.id, { role: fields.role, enabled: form.elements.enabled.checked });
          } else {
            await App.api.createUser({ memberId: current.memberId, role: fields.role });
          }
        } else {
          fields.name = fields.name.trim();
          fields.startsAt ||= null;
          fields.endsAt ||= null;
          fields.membershipRequired = form.elements.membershipRequired.checked;
          fields.coverRequired = form.elements.coverRequired.checked;
          await App.api.saveEvent(fields, record?.id);
        }
        current.saving = false;
        closeEditor();
        onSaved();
      } catch (error) {
        current.saving = false;
        save.disabled = false;
        deactivate.disabled = false;
        if (memberSearch) memberSearch.readOnly = false;
        feedback(`Couldn't ${deactivating ? "deactivate event" : "save"}: ${error.message}`);
      }
    });
  }

  async function changeStatus(memberId, status) {
    try {
      const updated = await App.api.updateMemberStatus(memberId, status);
      // TODO: refresh the affected card/detail view with `updated`
      return updated;
    } catch (err) {
      console.error("Failed to update member status:", err.message);
      throw err;
    }
  }

  async function setRole(memberId, role) {
    try {
      const updated = await App.api.setMemberRole(memberId, role);
      // TODO: refresh the affected card/detail view with `updated`
      return updated;
    } catch (err) {
      console.error("Failed to update member role:", err.message);
      throw err;
    }
  }

  window.App.admin = { changeStatus, setRole, openEditor, bindEditor };

})();
