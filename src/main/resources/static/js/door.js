/* door.js
 * Camera scanner and check-in result.
 * TODO: camera scanning itself is still a placeholder — this is a
 * stub so router.js has something to call when the Door view is shown.
 */
window.App = window.App || {};

(function () {

  let detail = null;
  let checkInToastTimer;
  const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" });
  const element = (id) => document.getElementById(id);

  function init() {
    // TODO: start the camera scanner, read a QR code, and call
    // App.api.checkIn(qrPayload) with the result. Show the check-in
    // outcome (member name, status, allow/deny) in the Door view.
  }

  // Manual check-in — triggered from the member detail modal when a scan
  // didn't work or someone's doing a manual search at the door.
  async function checkInMember(member, eventId, overrideMembership = false) {
    try {
      const result = await App.api.checkInManual(member.memberId, eventId, overrideMembership);
      // TODO: show a proper check-in confirmation, same as the scan flow will
      return result;
    } catch (err) {
      console.error("Manual check-in failed:", err.message);
      throw err;
    }
  }

  async function loadPages(fetchPage, current) {
    const entries = [];
    for (let page = 0; detail === current; page++) {
      const result = await fetchPage(page);
      entries.push(...result.content);
      if (page + 1 >= result.totalPages) break;
    }
    return entries;
  }

  function showMessage(id, message) {
    const target = element(id);
    target.textContent = message;
    target.hidden = !message;
  }

  function showCheckInToast(name) {
    const toast = element("checkInToast");
    const message = element("checkInToastMessage");
    clearTimeout(checkInToastTimer);
    toast.hidden = false;
    message.textContent = `${name || "Member"} successfully checked-in.`;
    checkInToastTimer = setTimeout(() => {
      toast.hidden = true;
      message.textContent = "";
    }, 5000);
  }

  function selectPanel(panel) {
    for (const name of ["CheckIns", "Notes"]) {
      const selected = name === panel;
      element(`modal${name}Tab`).setAttribute("aria-pressed", String(selected));
      element(`modal${name}Panel`).hidden = !selected;
    }
  }

  function showNoteForm(visible) {
    element("modalNoteForm").hidden = !visible;
    element("modalAddNote").setAttribute("aria-expanded", String(visible));
  }

  function entry(meta, content) {
    const row = document.createElement("li");
    const heading = document.createElement("span");
    heading.className = "modal-entry-meta";
    heading.textContent = meta;
    const body = document.createElement("p");
    body.className = "modal-entry-content";
    body.textContent = content;
    row.append(heading, body);
    return row;
  }

  function staffName(checkIn) {
    return checkIn.checkedInByName || "Unknown staff";
  }

  function renderCheckIns(current) {
    const sorted = current.checkIns.slice().sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
    const latest = sorted.find((checkIn) => current.eventId == null || checkIn.eventId === current.eventId);
    element("modalCheckedIn").textContent = latest
      ? `${dateTime.format(new Date(latest.createdAt))} (${staffName(latest)})`
      : "No check-in";
    element("modalCheckInsList").replaceChildren(...sorted.map((checkIn) => {
      const event = checkIn.eventId == null ? "No event" : current.events.get(checkIn.eventId) || `Event #${checkIn.eventId}`;
      return entry(`${dateTime.format(new Date(checkIn.createdAt))} · ${staffName(checkIn)}`, event);
    }));
    showMessage("modalCheckInsMessage", sorted.length ? "" : "No check-ins yet.");
  }

  function renderNotes(current) {
    element("modalNoteWarning").hidden = !current.notes.length;
    element("modalNotesList").replaceChildren(...current.notes.map((note) =>
      entry(`${dateTime.format(new Date(note.createdAt))} · ${note.createdByName || note.createdBy || "Unknown staff"}`, note.content)
    ));
    showMessage("modalNotesMessage", current.notes.length ? "" : "No notes yet.");
  }

  async function openDetail(member, events, eventId, onCheckIn) {
    const current = { member, events, eventId, onCheckIn, checkIns: [], notes: [] };
    detail = current;
    selectPanel("CheckIns");
    showNoteForm(false);
    element("modalNoteContent").value = "";
    element("modalNoteWarning").hidden = !member.hasNotes;
    element("modalCheckInsList").replaceChildren();
    element("modalNotesList").replaceChildren();
    element("modalSaveNote").disabled = false;
    element("modalCancelNote").disabled = false;
    element("modalCheckInBtn").disabled = false;
    element("modalOverrideBtn").disabled = eventId == null;
    element("modalMembershipType").textContent = "Loading…";
    element("modalCheckedIn").textContent = "Loading…";
    showMessage("modalCheckInsMessage", "Loading check-ins…");
    showMessage("modalNotesMessage", "Loading notes…");
    showMessage("modalCheckInFeedback", "");
    showMessage("modalNoteFeedback", "");

    const loadMemberships = async () => {
      try {
        const memberships = await loadPages((page) => App.api.getMemberships(member.id, page), current);
        if (detail !== current) return;
        const activeTypes = new Set(memberships.filter(({ status }) => status === "ACTIVE").map(({ type }) => type));
        element("modalMembershipType").textContent = activeTypes.has("PAID") ? "Paid"
          : activeTypes.has("FREE") ? "Free" : "No active membership";
      } catch (error) {
        if (detail === current) element("modalMembershipType").textContent = "Unavailable";
      }
    };
    const loadCheckIns = async () => {
      try {
        const history = await App.api.getCheckIns(member.memberId);
        if (detail !== current) return;
        current.checkIns = history;
        renderCheckIns(current);
      } catch (error) {
        if (detail !== current) return;
        element("modalCheckedIn").textContent = "Unavailable";
        showMessage("modalCheckInsMessage", `Couldn't load check-ins: ${error.message}`);
      }
    };
    const loadNotes = async () => {
      try {
        const notes = await loadPages((page) => App.api.getMemberNotes(member.id, page), current);
        if (detail !== current) return;
        const byId = new Map([...current.notes, ...notes].map((note) => [note.id, note]));
        current.notes = [...byId.values()].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
        member.hasNotes = current.notes.length > 0;
        renderNotes(current);
      } catch (error) {
        if (detail === current) showMessage("modalNotesMessage", `Couldn't load notes: ${error.message}`);
      }
    };
    await Promise.all([loadMemberships(), loadCheckIns(), loadNotes()]);
  }

  function closeDetail() {
    detail = null;
  }

  function bindDetailEvents(staffId) {
    async function submitCheckIn(overrideMembership) {
      const current = detail;
      const checkInBtn = element("modalCheckInBtn");
      const overrideBtn = element("modalOverrideBtn");
      if (!current || checkInBtn.disabled || (overrideMembership && overrideBtn.disabled)) return;
      checkInBtn.disabled = true;
      overrideBtn.disabled = true;
      showMessage("modalCheckInFeedback", "Checking in…");
      try {
        const checkIn = await checkInMember(current.member, current.eventId, overrideMembership);
        current.onCheckIn(checkIn, detail === current);
        showCheckInToast(current.member.name);
      } catch (error) {
        if (detail === current) showMessage("modalCheckInFeedback", `Couldn't check in: ${error.message}`);
      } finally {
        if (detail === current) {
          checkInBtn.disabled = false;
          overrideBtn.disabled = current.eventId == null;
        }
      }
    }

    element("modalCheckInBtn").addEventListener("click", () => submitCheckIn(false));
    element("modalOverrideBtn").addEventListener("click", () => submitCheckIn(true));
    element("modalCheckInsTab").addEventListener("click", () => selectPanel("CheckIns"));
    element("modalNotesTab").addEventListener("click", () => selectPanel("Notes"));

    const noteContent = element("modalNoteContent");
    const addNote = element("modalAddNote");
    const cancelNote = element("modalCancelNote");
    const saveNote = element("modalSaveNote");
    addNote.addEventListener("click", () => {
      showNoteForm(true);
      showMessage("modalNoteFeedback", "");
      noteContent.focus();
    });
    cancelNote.addEventListener("click", () => {
      showNoteForm(false);
      noteContent.value = "";
      showMessage("modalNoteFeedback", "");
      addNote.focus();
    });
    element("modalNoteForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const current = detail;
      if (!current || saveNote.disabled) return;
      const content = noteContent.value.trim();
      if (!content) {
        showMessage("modalNoteFeedback", "Enter a note.");
        noteContent.focus();
        return;
      }
      saveNote.disabled = true;
      cancelNote.disabled = true;
      showMessage("modalNoteFeedback", "Saving note…");
      try {
        const note = await App.api.addMemberNote(current.member.id, staffId, content);
        current.member.hasNotes = true;
        if (detail !== current) return;
        current.notes = [note, ...current.notes.filter(({ id }) => id !== note.id)];
        renderNotes(current);
        noteContent.value = "";
        showNoteForm(false);
        showMessage("modalNoteFeedback", "Note added.");
        if (!element("modalNotesPanel").hidden) addNote.focus();
      } catch (error) {
        if (detail === current) showMessage("modalNoteFeedback", `Couldn't save note: ${error.message}`);
      } finally {
        if (detail === current) {
          saveNote.disabled = false;
          cancelNote.disabled = false;
        }
      }
    });
  }

  window.App.door = { init, checkInMember, openDetail, closeDetail, bindDetailEvents };

})();
