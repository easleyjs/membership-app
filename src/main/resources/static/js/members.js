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
  let isLoading = true;
  let loadError = null;
  let hasLoadedOnce = false;
  let loadVersion = 0;
  let checkIns = [];
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
        const [memberData, userData] = await Promise.all([
          App.api.getMembers(),
          App.api.getUsers(),
        ]);
        if (version !== loadVersion) return;
        const memberList = Array.isArray(memberData) ? memberData : memberData?.members || [];
        const users = Array.isArray(userData) ? userData : userData?.users || [];
        const rolesByMemberId = new Map(users.map((user) => [user.memberId, user.role]));
        members = memberList.map((member) => normalizeMember(member, rolesByMemberId.get(member.id)));
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
    document.querySelectorAll(".chip").forEach((chip) => {
      chip.classList.toggle("active", chip.dataset.filter === filter);
    });
  }

  function updateDoorFilters(eventResult, checkInResult) {
    const select = document.getElementById("doorEvent");
    const checkedInFilter = document.getElementById("checkedInFilter");
    const status = document.getElementById("doorDataStatus");
    const previousEvent = select.value;
    const events = eventResult.value ?? [];
    eventNames = new Map(events.map(({ id, name }) => [id, name]));
    const eventError = eventResult.status === "rejected";
    const checkInError = checkInResult.status === "rejected";

    select.replaceChildren(...events.map(({ id, name }) => new Option(name, String(id))));
    if (events.some(({ id }) => String(id) === previousEvent)) select.value = previousEvent;
    if (!events.length) {
      select.add(new Option(eventError ? "Events unavailable" : "No events", ""));
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

  function matchesSearch(m) {
    return m.name.toLowerCase().includes(searchTerm.trim().toLowerCase());
  }

  function statusLabel(status) {
    if (status === "banned") return "Banned";
    return status === "active" ? "Active" : "Inactive";
  }

  let activeMember = null;

  function openDetail(member) {
    activeMember = member;

    const overlay = document.getElementById("memberModalOverlay");
    const avatar = document.getElementById("modalAvatar");
    const name = document.getElementById("modalName");
    const memberNumber = document.getElementById("modalMemberNumber");
    const status = document.getElementById("modalStatus");
    const statusText = document.getElementById("modalStatusText");
    const email = document.getElementById("modalEmail");
    const suspendBtn = document.getElementById("modalSuspendBtn");
    const reissueBtn = document.getElementById("modalReissueBtn");

    avatar.textContent = initials(member.name);
    name.textContent = member.name || "—";
    memberNumber.textContent = `Member #${member.id}`;
    status.className = `badge badge-status is-${member.status}`;
    statusText.textContent = statusLabel(member.status).toUpperCase();
    email.textContent = member.email || "—";

    const isActive = member.status === "active";
    suspendBtn.textContent = isActive ? "Suspend" : "Reactivate";
    suspendBtn.classList.toggle("is-reactivate", !isActive);
    suspendBtn.disabled = false;
    reissueBtn.disabled = false;
    reissueBtn.textContent = "Reissue QR";
    if (mode === "door") {
      const eventId = document.getElementById("doorEvent").value;
      App.door.openDetail(member, eventNames, eventId ? Number(eventId) : null, (checkIn, isCurrentDetail) => {
        if (isCurrentDetail) closeDetail();
        if (!checkIns.some(({ id }) => id === checkIn.id)) checkIns.push(checkIn);
        if (mode === "door" && activeFilter === "checked-in") render();
      });
    }

    overlay.classList.add("open");
    document.getElementById("modalCloseBtn").focus();
  }

  function closeDetail() {
    document.getElementById("memberModalOverlay").classList.remove("open");
    App.door.closeDetail();
    activeMember = null;
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

    if (isLoading) {
      list.innerHTML = "";
      empty.textContent = "Loading members…";
      empty.style.display = "block";
      pagination.style.display = "none";
      heading.textContent = "Members";
      return;
    }

    if (loadError) {
      list.innerHTML = "";
      empty.textContent = `Couldn't load members: ${loadError}`;
      empty.style.display = "block";
      pagination.style.display = "none";
      heading.textContent = "Members";
      return;
    }

    const eventId = document.getElementById("doorEvent").value;
    const checkedInMembers = new Set();
    if (activeFilter === "checked-in" && eventId) {
      for (const checkIn of checkIns) {
        if (String(checkIn.eventId) === eventId) checkedInMembers.add(checkIn.memberId);
      }
    }
    const filtered = members.filter((m) => matchesFilter(m, checkedInMembers) && matchesSearch(m));

    heading.textContent = `Members (${filtered.length})`;
    list.innerHTML = "";

    if (filtered.length === 0) {
      empty.textContent = "No members match this filter.";
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

      card.addEventListener("click", () => openDetail(m));
      card.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          openDetail(m);
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
    const modalSuspendBtn = document.getElementById("modalSuspendBtn");
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

    addBtn.addEventListener("click", () => {
      // Placeholder — open an "add member" form here
      console.log("Add member clicked");
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
      // Placeholder — open an edit form for activeMember here
      console.log("Edit clicked:", activeMember?.id);
    });

    modalSuspendBtn.addEventListener("click", async () => {
      if (!activeMember) return;
      const nextStatus = activeMember.status === "active" ? "INACTIVE" : "ACTIVE";

      modalSuspendBtn.disabled = true;
      modalSuspendBtn.textContent = "Updating…";

      try {
        await App.admin.changeStatus(activeMember.id, nextStatus);
        closeDetail();
        loadData(); // refresh the list so the card reflects the new status
      } catch (err) {
        modalSuspendBtn.disabled = false;
        modalSuspendBtn.textContent = activeMember.status === "active" ? "Suspend" : "Reactivate";
        alert(`Couldn't update status: ${err.message}`);
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
