/* members.js
 * Search, filter chips, the member card list, pagination, and the user
 * menu. Also the entry point for opening a member's detail view.
 */
window.App = window.App || {};

(function () {

  // Sample data — kept for reference/offline testing. Live data now comes
  // from GET /api/members (Door view) or GET /api/users (Admin view) via
  // loadData() below.
  //
  // const SAMPLE_MEMBERS = [
  //   { id: 1,  name: "Jane Doe",       role: "admin",  status: "active" },
  //   { id: 2,  name: "Mike Chen",      role: "staff",  status: "active" },
  //   { id: 3,  name: "Sam Rivera",     role: "member", status: "inactive" },
  //   { id: 4,  name: "Priya Anand",    role: "member", status: "active" },
  //   { id: 5,  name: "Wren Sokolov",   role: "member", status: "active" },
  //   { id: 6,  name: "Devon Blake",    role: "staff",  status: "inactive" },
  //   { id: 7,  name: "Terra Nguyen",   role: "member", status: "active" },
  //   { id: 8,  name: "Alex Ferro",     role: "member", status: "inactive" },
  //   { id: 9,  name: "Jarrett Cole",   role: "member", status: "active" },
  //   { id: 10, name: "Nina Ostrov",    role: "staff",  status: "active" },
  //   { id: 11, name: "Owen Marsh",     role: "member", status: "active" },
  //   { id: 12, name: "Ivy Calloway",   role: "member", status: "inactive" },
  //   { id: 13, name: "Leo Bautista",   role: "staff",  status: "active" },
  //   { id: 14, name: "Ruth Ellison",   role: "member", status: "active" },
  //   { id: 15, name: "Callum Reyes",   role: "member", status: "inactive" },
  //   { id: 16, name: "Freya Holt",     role: "member", status: "active" },
  //   { id: 17, name: "Marcus Diallo",  role: "staff",  status: "active" },
  //   { id: 18, name: "Talia Novak",    role: "member", status: "inactive" },
  //   { id: 19, name: "Roman Kessler",  role: "member", status: "active" },
  //   { id: 20, name: "Esme Whitlock",  role: "admin",  status: "active" },
  //   { id: 21, name: "Gideon Park",    role: "member", status: "active" },
  //   { id: 22, name: "Aria Fontaine",  role: "member", status: "inactive" },
  //   { id: 23, name: "Otis Bramwell",  role: "member", status: "active" },
  //   { id: 24, name: "Lena Marchetti", role: "staff",  status: "inactive" },
  // ];

  let members = [];
  let entities = [];
  let section = "members";
  const sections = {
    members: { label: "Members", singular: "member", search: "Name, member number, or email" },
    users: { label: "Users", singular: "user", search: "User name, email, or role" },
    events: { label: "Events", singular: "event", search: "Event name, location, or status" },
  };
  let isLoading = true;
  let loadError = null;
  let hasLoadedOnce = false;
  let loadVersion = 0;
  let checkIns = [];
  let checkedInFilterTimer;
  let eventNames = new Map();

  let activeFilter = "all";
  let searchTerm = "";
  let pageSize = 10;
  let currentPage = 1;
  let mode = "door";

  function normalizeMember(raw, role) {
    return {
      id: raw.memberNumber,
      memberId: raw.id,
      name: `${raw.firstName || ""} ${raw.lastName || ""}`.trim(),
      firstName: raw.firstName,
      lastName: raw.lastName,
      status: (raw.status || "").toLowerCase(),
      email: raw.email || "",
      role: role?.toLowerCase(),
      hasNotes: raw.hasNotes,
    };
  }

  async function loadData() {
    const version = ++loadVersion;
    isLoading = true;
    loadError = null;
    render();

    try {
      if (mode === "admin") {
        if (section === "events") {
          const events = await App.api.getEvents();
          if (version === loadVersion) entities = events;
          return;
        }
        const [memberData, userData, events] = await Promise.all([
          App.api.getMembers(),
          App.api.getUsers(),
          section === "members" ? App.api.getEvents().catch(() => []) : [],
        ]);
        if (version !== loadVersion) return;
        const memberList = Array.isArray(memberData) ? memberData : memberData?.members || [];
        const users = Array.isArray(userData) ? userData : userData?.users || [];
        if (section === "users") {
          const membersById = new Map(memberList.map((member) => [member.id, member]));
          entities = users.map((user) => {
            const member = membersById.get(user.memberId);
            return {
              ...user,
              name: `${user.firstName || ""} ${user.lastName || ""}`.trim(),
              email: member?.email || "",
              memberNumber: member?.memberNumber,
            };
          });
          return;
        }
        const rolesByMemberId = new Map(users.map((user) => [user.memberId, user.role]));
        members = memberList.map((member) => normalizeMember(member, rolesByMemberId.get(member.id)));
        rememberEvents(events);
      } else {
        const [memberResult, eventResult, checkInResult] = await Promise.allSettled([
          App.api.getMembers(),
          App.api.getEvents(),
          App.api.getCheckIns(),
        ]);
        if (version !== loadVersion) return;
        updateDoorFilters(eventResult, checkInResult);
        if (memberResult.status === "rejected") throw memberResult.reason;
        const data = memberResult.value;
        const list = Array.isArray(data) ? data : data?.members || [];
        members = list.map((member) => normalizeMember(member));
      }
    } catch (err) {
      if (version !== loadVersion) return;
      loadError = err.message || "Couldn't load data.";
      members = [];
      entities = [];
    } finally {
      if (version === loadVersion) {
        isLoading = false;
        render();
      }
    }
  }

  function selectFilter(filter) {
    activeFilter = filter;
    currentPage = 1;
    document.querySelectorAll("#chips [data-filter]").forEach((chip) => {
      const selected = chip.dataset.filter === filter;
      chip.classList.toggle("active", selected);
      chip.setAttribute("aria-pressed", String(selected));
    });
  }

  function rememberEvents(events) {
    eventNames = new Map(events.map(({ id, name }) => [id, name]));
  }

  function updateDoorFilters(eventResult, checkInResult) {
    const select = document.getElementById("doorEvent");
    const checkedInFilter = document.getElementById("checkedInFilter");
    const status = document.getElementById("doorDataStatus");
    const previousEvent = select.value;
    const allEvents = eventResult.value ?? [];
    rememberEvents(allEvents);
    const events = allEvents.filter(({ status }) => status === "ACTIVE");
    const eventError = eventResult.status === "rejected";
    const checkInError = checkInResult.status === "rejected";

    select.replaceChildren(...events.map(({ id, name }) => new Option(name, String(id))));
    if (events.some(({ id }) => String(id) === previousEvent)) select.value = previousEvent;
    if (!events.length) {
      select.add(new Option(eventError ? "Events unavailable" : "No active events", ""));
    }
    select.disabled = !events.length;
    checkIns = checkInResult.value ?? [];
    checkedInFilter.disabled = !events.length || checkInError;
    if (checkedInFilter.disabled && activeFilter === "checked-in") selectFilter("all");

    status.textContent = [
      eventError ? "Couldn't load events." : "",
      checkInError ? "Couldn't load check-ins." : "",
    ].filter(Boolean).join(" ");
    status.hidden = !status.textContent;
  }

  function initials(name) {
    return name.split(" ").map((p) => p[0]).slice(0, 2).join("").toUpperCase();
  }

  function matchesFilter(m, checkedInMembers) {
    if (mode === "door" && (activeFilter === "admin" || activeFilter === "staff")) {
      return true; // shouldn't happen, but fail open to "all" behavior
    }
    if (activeFilter === "all") return true;
    if (activeFilter === "admin") return m.role === "admin";
    if (activeFilter === "staff") return m.role === "staff";
    if (activeFilter === "active") return m.status === "active";
    if (activeFilter === "inactive") return m.status === "inactive";
    if (activeFilter === "banned") return m.status === "banned";
    if (activeFilter === "checked-in") return checkedInMembers.has(m.memberId);
    return true;
  }

  function matchesSearch(member, query) {
    return member.name.toLowerCase().includes(query)
      || member.email.toLowerCase().includes(query)
      || (member.id != null && /^#?\d+$/.test(query)
        && String(member.id).startsWith(query.replace(/^#/, "")));
  }

  function matchesUserFilter(user) {
    switch (activeFilter) {
      case "admin":
      case "staff":
        return user.role?.toLowerCase() === activeFilter;
      case "active":
      case "inactive":
        return user.enabled === (activeFilter === "active");
      default:
        return true;
    }
  }

  function statusLabel(status) {
    if (status === "banned") return "Banned";
    return status === "active" ? "Active" : "Inactive";
  }

  let activeMember = null;
  let detailTrigger = null;
  const pendingBans = new Set();

  function renderMemberStatus(member) {
    document.getElementById("modalStatus").className = `badge badge-status is-${member.status}`;
    document.getElementById("modalStatusText").textContent = statusLabel(member.status).toUpperCase();
    const ban = document.getElementById("modalBanBtn");
    ban.textContent = member.status === "banned" ? "Unban" : "Ban";
    ban.disabled = pendingBans.has(member.id);
  }

  function openDetail(member, trigger) {
    activeMember = member;
    detailTrigger = trigger;
    const search = document.getElementById("search");
    if (document.activeElement === search) search.blur();

    const overlay = document.getElementById("memberModalOverlay");
    const name = document.getElementById("modalName");
    const memberNumber = document.getElementById("modalMemberNumber");
    const email = document.getElementById("modalEmail");
    const reissueBtn = document.getElementById("modalReissueBtn");

    name.textContent = member.name || "—";
    memberNumber.textContent = `Member #${member.id}`;
    renderMemberStatus(member);
    email.textContent = member.email || "—";

    document.getElementById("modalBanFeedback").hidden = true;
    reissueBtn.disabled = false;
    reissueBtn.textContent = "Reissue QR";
    const eventId = mode === "door" ? document.getElementById("doorEvent").value : "";
    App.door.openDetail(member, eventNames, eventId ? Number(eventId) : null, {
      mode,
      onCheckIn(checkIn, isCurrentDetail) {
        if (!checkIns.some(({ id }) => id === checkIn.id)) checkIns.push(checkIn);
        if (mode === "door" && activeFilter === "checked-in") render();
        if (isCurrentDetail && mode === "door") closeDetail();
      },
    });

    overlay.classList.add("open");
    document.getElementById("modalCloseBtn").focus({ preventScroll: true });
  }

  function updateSectionControls() {
    const config = sections[section];
    const search = document.getElementById("search");
    search.placeholder = config.search;
    search.setAttribute("aria-label", `Search ${config.label.toLowerCase()} by ${config.search.toLowerCase()}`);
    const add = document.getElementById("addBtn");
    add.title = `Add ${config.singular}`;
    add.setAttribute("aria-label", add.title);
    const chips = document.getElementById("chips");
    const isUsers = section === "users";
    chips.hidden = section === "events";
    chips.querySelector('[data-filter="banned"]').hidden = section !== "members";
    chips.querySelector('[data-filter="active"]').textContent = isUsers ? "Enabled" : "Active";
    chips.querySelector('[data-filter="inactive"]').textContent = isUsers ? "Disabled" : "Inactive";
    document.querySelectorAll("[data-section]").forEach((button) => {
      button.setAttribute("aria-pressed", String(button.dataset.section === section));
    });
  }

  function resetSearch() {
    searchTerm = "";
    document.getElementById("search").value = "";
    document.getElementById("clearSearch").classList.remove("visible");
  }

  function selectSection(next) {
    if (next === section) return;
    closeDetail();
    section = next;
    resetSearch();
    selectFilter("all");
    updateSectionControls();
    loadData();
  }

  function closeDetail() {
    if (!activeMember) return;
    document.getElementById("memberModalOverlay").classList.remove("open");
    App.door.closeDetail();
    activeMember = null;
    const target = detailTrigger?.isConnected ? detailTrigger : document.getElementById("resultsHeading");
    detailTrigger = null;
    target.focus({ preventScroll: true });
  }

  function setMode(newMode) {
    // Admin mode is only reachable by users with the admin role — silently
    // refuse and stay put otherwise (defends against direct App.members.setMode calls too).
    if (newMode === "admin" && !App.auth.isAdmin()) {
      console.warn("Admin mode requires the admin role.");
      newMode = "door";
    }

    const modeChanged = newMode !== mode;
    if (modeChanged && activeMember) closeDetail();
    mode = newMode;

    const body = document.body;
    const chips = document.getElementById("chips");
    const scanBtn = document.getElementById("scanBtn");
    const switchModeBtn = document.getElementById("switchModeBtn");
    const title = document.querySelector(".header-top h1");

    const isDoor = mode === "door";
    if (isDoor && section !== "members") {
      section = "members";
      resetSearch();
      selectFilter("all");
    }
    updateSectionControls();

    body.classList.toggle("door-mode", isDoor);
    chips.classList.toggle("door-mode", isDoor);
    scanBtn.classList.toggle("visible", isDoor);
    switchModeBtn.textContent = isDoor ? "Switch to Admin" : "Switch to Door";
    switchModeBtn.style.display = App.auth.isAdmin() ? "" : "none";
    const sectionTitle = isDoor ? "OUTLAND Door" : "OUTLAND Admin";
    document.title = sectionTitle;
    if (title) title.textContent = sectionTitle;

    if ((isDoor && (activeFilter === "admin" || activeFilter === "staff")) ||
        (!isDoor && activeFilter === "checked-in")) {
      selectFilter("all");
    }

    currentPage = 1;

    // Refresh the member list and mode-specific data on entry.
    if (modeChanged || !hasLoadedOnce) {
      hasLoadedOnce = true;
      loadData();
    } else {
      render();
    }
  }

  function render() {
    const list = document.getElementById("list");
    const empty = document.getElementById("empty");
    const heading = document.getElementById("resultsHeading");
    const pagination = document.getElementById("pagination");
    const pageInfo = document.getElementById("pageInfo");
    const prevPage = document.getElementById("prevPage");
    const nextPage = document.getElementById("nextPage");
    if (!list) return; // members view isn't on this page
    clearTimeout(checkedInFilterTimer);
    const { label } = sections[section];

    if (isLoading) {
      list.innerHTML = "";
      empty.textContent = `Loading ${label.toLowerCase()}…`;
      empty.style.display = "block";
      pagination.style.display = "none";
      heading.textContent = label;
      return;
    }

    if (loadError) {
      list.innerHTML = "";
      empty.textContent = `Couldn't load ${label.toLowerCase()}: ${loadError}`;
      empty.style.display = "block";
      pagination.style.display = "none";
      heading.textContent = label;
      return;
    }

    const eventId = document.getElementById("doorEvent").value;
    const checkedInMembers = new Set();
    if (activeFilter === "checked-in" && eventId) {
      const eventCheckIns = checkIns.filter((checkIn) => String(checkIn.eventId) === eventId);
      const { entries, timer } = App.door.currentCheckIns(eventCheckIns, render);
      checkedInFilterTimer = timer;
      for (const { memberId } of entries) checkedInMembers.add(memberId);
    }
    const query = searchTerm.trim().toLowerCase();
    const isUsers = section === "users";
    const filtered = section === "members"
      ? members.filter((m) => matchesFilter(m, checkedInMembers) && matchesSearch(m, query))
      : entities.filter((record) => {
        if (isUsers && !matchesUserFilter(record)) return false;
        const fields = isUsers
          ? [record.name, record.email, record.role] : [record.name, record.location, record.status];
        return fields.some((field) => field?.toLowerCase().includes(query));
      });

    heading.textContent = `${label} (${filtered.length})`;
    list.innerHTML = "";

    if (filtered.length === 0) {
      empty.textContent = `No ${label.toLowerCase()} match.`;
      empty.style.display = "block";
      pagination.style.display = "none";
      return;
    }
    empty.style.display = "none";
    pagination.style.display = "flex";

    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (currentPage > totalPages) currentPage = totalPages;
    if (currentPage < 1) currentPage = 1;

    const start = (currentPage - 1) * pageSize;
    const pageItems = filtered.slice(start, start + pageSize);

    pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
    prevPage.disabled = currentPage <= 1;
    nextPage.disabled = currentPage >= totalPages;

    pageItems.forEach((m) => {
      if (section !== "members") {
        const card = document.createElement("button");
        card.type = "button";
        card.className = "card editor-card";
        const title = document.createElement("span");
        title.className = "card-name";
        title.textContent = m.name;
        const summary = document.createElement("span");
        summary.className = "modal-entry-meta";
        summary.textContent = section === "users"
          ? [m.email, m.role, m.enabled ? "Enabled" : "Disabled"].filter(Boolean).join(" · ")
          : [m.location, m.status, m.startsAt && new Date(m.startsAt).toLocaleString()].filter(Boolean).join(" · ");
        card.append(title, summary);
        card.addEventListener("click", () => App.admin.openEditor(section, m, loadData));
        list.appendChild(card);
        return;
      }
      const card = document.createElement("div");
      card.className = "card";
      card.tabIndex = 0;

      const roleBadge =
        m.role === "admin"
          ? `<span class="badge badge-role badge-role-admin">Admin</span>`
          : m.role === "staff"
          ? `<span class="badge badge-role">Staff</span>`
          : "";

      card.innerHTML = `
        <div class="avatar"></div>
        <div class="card-body">
          <div class="card-name"><span class="card-name-text"></span><span class="card-member-number"></span></div>
          <div class="card-tags">
            ${roleBadge}
            <span class="badge badge-status is-${m.status}">
              <span class="dot"></span>${statusLabel(m.status)}
            </span>
          </div>
        </div>
      `;
      card.querySelector(".avatar").textContent = initials(m.name);
      card.querySelector(".card-name-text").textContent = m.name;
      card.querySelector(".card-member-number").textContent = m.id == null ? "" : ` (#${m.id})`;

      card.addEventListener("click", () => openDetail(m, card));
      card.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          openDetail(m, card);
        }
      });

      list.appendChild(card);
    });
  }

  function bindEvents(user) {
    const chips = document.getElementById("chips");
    const doorEvent = document.getElementById("doorEvent");
    const search = document.getElementById("search");
    const clearSearch = document.getElementById("clearSearch");
    const perPage = document.getElementById("perPage");
    const prevPage = document.getElementById("prevPage");
    const nextPage = document.getElementById("nextPage");
    const addBtn = document.getElementById("addBtn");
    const userMenuBtn = document.getElementById("userMenuBtn");
    const userDropdown = document.getElementById("userDropdown");
    const profileBtn = document.getElementById("profileBtn");
    const logoutBtn = document.getElementById("logoutBtn");
    const switchModeBtn = document.getElementById("switchModeBtn");
    const scanBtn = document.getElementById("scanBtn");
    const memberModalOverlay = document.getElementById("memberModalOverlay");
    const modalCloseBtn = document.getElementById("modalCloseBtn");
    const modalEditBtn = document.getElementById("modalEditBtn");
    const modalBanBtn = document.getElementById("modalBanBtn");
    const modalReissueBtn = document.getElementById("modalReissueBtn");

    if (!chips) return; // members view isn't on this page

    chips.addEventListener("click", (e) => {
      const btn = e.target.closest(".chip");
      if (!btn || btn.disabled) return;
      selectFilter(btn.dataset.filter);
      render();
    });

    doorEvent.addEventListener("change", () => {
      currentPage = 1;
      render();
    });

    search.addEventListener("input", (e) => {
      searchTerm = e.target.value;
      clearSearch.classList.toggle("visible", searchTerm.length > 0);
      currentPage = 1;
      render();
    });

    clearSearch.addEventListener("click", () => {
      search.value = "";
      searchTerm = "";
      clearSearch.classList.remove("visible");
      currentPage = 1;
      search.focus();
      render();
    });

    perPage.addEventListener("change", (e) => {
      pageSize = parseInt(e.target.value, 10);
      currentPage = 1;
      render();
    });

    prevPage.addEventListener("click", () => {
      if (currentPage > 1) {
        currentPage--;
        render();
      }
    });

    nextPage.addEventListener("click", () => {
      currentPage++;
      render();
    });

    App.admin.bindEditor();
    document.getElementById("adminSections").addEventListener("click", (event) => {
      const button = event.target.closest("[data-section]");
      if (mode === "admin" && button) selectSection(button.dataset.section);
    });
    addBtn.addEventListener("click", () => {
      if (mode === "admin") App.admin.openEditor(section, null, () => {
        resetSearch();
        selectFilter("all");
        loadData();
      });
    });

    // User menu dropdown
    function closeUserMenu() {
      userDropdown.classList.remove("open");
      userMenuBtn.setAttribute("aria-expanded", "false");
    }

    userMenuBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      const isOpen = userDropdown.classList.toggle("open");
      userMenuBtn.setAttribute("aria-expanded", String(isOpen));
    });

    document.addEventListener("click", (e) => {
      if (!userDropdown.contains(e.target) && e.target !== userMenuBtn) {
        closeUserMenu();
      }
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        closeUserMenu();
        closeDetail();
      }
    });

    profileBtn.addEventListener("click", () => {
      // Placeholder — navigate to profile page here
      console.log("Profile clicked");
      closeUserMenu();
    });

    logoutBtn.addEventListener("click", async () => {
      logoutBtn.disabled = true;
      try {
        await App.auth.logout();
      } catch (err) {
        alert(`Couldn't sign out: ${err.message}`);
        logoutBtn.disabled = false;
      }
    });

    switchModeBtn.addEventListener("click", () => {
      closeUserMenu();
      setMode(mode === "door" ? "admin" : "door");
    });

    scanBtn.addEventListener("click", () => {
      // Placeholder — this will launch the scan popup later
      console.log("Scan clicked");
    });

    // Member detail modal
    modalCloseBtn.addEventListener("click", closeDetail);
    App.door.bindDetailEvents(user.id);
    memberModalOverlay.addEventListener("keydown", (event) => {
      if (event.key === "Escape") closeDetail();
    });

    memberModalOverlay.addEventListener("click", (e) => {
      if (e.target === memberModalOverlay) closeDetail();
    });

    modalEditBtn.addEventListener("click", () => {
      if (mode !== "admin" || !activeMember) return;
      const member = activeMember;
      closeDetail();
      App.admin.openEditor("members", member, loadData);
    });

    modalBanBtn.addEventListener("click", async () => {
      const member = activeMember;
      if (mode !== "admin" || !member || modalBanBtn.disabled) return;
      const banning = member.status !== "banned";
      const message = document.getElementById("modalBanFeedback");
      pendingBans.add(member.id);
      modalBanBtn.disabled = true;
      message.hidden = true;
      try {
        const updated = await App.api.setMemberBanned(member.id, banning);
        member.status = updated.status.toLowerCase();
        const listed = members.find(({ id }) => id === member.id);
        if (listed) listed.status = member.status;
        if (!isLoading && section === "members") render();
        if (mode === "admin" && activeMember?.id === member.id) {
          activeMember.status = member.status;
          message.textContent = banning ? "Member banned." : "Member unbanned.";
          message.hidden = false;
        }
      } catch (err) {
        if (mode === "admin" && activeMember?.id === member.id) {
          message.textContent = `Couldn't update ban: ${err.message}`;
          message.hidden = false;
        }
      } finally {
        pendingBans.delete(member.id);
        if (mode === "admin" && activeMember?.id === member.id) renderMemberStatus(activeMember);
      }
    });

    modalReissueBtn.addEventListener("click", async () => {
      if (!activeMember) return;

      modalReissueBtn.disabled = true;
      modalReissueBtn.textContent = "Reissuing…";

      try {
        await App.admin.reissueQr(activeMember.id);
        modalReissueBtn.textContent = "Reissued ✓";
        setTimeout(() => {
          modalReissueBtn.disabled = false;
          modalReissueBtn.textContent = "Reissue QR";
        }, 1500);
      } catch (err) {
        modalReissueBtn.disabled = false;
        modalReissueBtn.textContent = "Reissue QR";
        alert(`Couldn't reissue QR: ${err.message}`);
      }
    });
  }

  async function init() {
    const status = document.getElementById("sessionStatus");
    let user;
    try {
      user = await App.auth.restoreSession();
    } catch (err) {
      if (err.status === 401) {
        window.location.replace("login.html");
      } else {
        status.textContent = `Couldn't restore your session: ${err.message} Reload the page to retry.`;
      }
      return;
    }

    const userMenuBtn = document.getElementById("userMenuBtn");
    userMenuBtn.textContent = initials(user.displayName);
    userMenuBtn.setAttribute("aria-label", user.displayName);
    status.hidden = true;
    document.body.classList.remove("session-pending");
    bindEvents(user);
    setMode(App.auth.isAdmin() ? "admin" : "door");
  }

  window.App.members = { init, openDetail, closeDetail, setMode, loadData };

  document.addEventListener("DOMContentLoaded", init);

})();
