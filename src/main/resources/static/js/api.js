/* api.js
 * All fetch() calls live here, plus one shared error type/handler.
 * Every other file talks to the backend through App.api — nothing else
 * should call fetch() directly.
 */
window.App = window.App || {};

(function () {

  const API_BASE = "/api";

  function createCheckInId() {
    if (typeof crypto.randomUUID === "function") return crypto.randomUUID();

    // getRandomValues is also available when the portal is served over HTTP.
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40; // UUID version 4
    bytes[8] = (bytes[8] & 0x3f) | 0x80; // RFC UUID variant
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
    return [
      hex.slice(0, 8),
      hex.slice(8, 12),
      hex.slice(12, 16),
      hex.slice(16, 20),
      hex.slice(20),
    ].join("-");
  }

  class ApiError extends Error {
    constructor(message, status) {
      super(message);
      this.name = "ApiError";
      this.status = status || 0;
    }
  }

  async function request(path, { method = "GET", body } = {}) {
    const headers = {};
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
      // Fetch a fresh token since Spring clears it on login and logout.
      const csrf = await request("/auth/csrf");
      headers[csrf.headerName] = csrf.token;
    }
    if (body !== undefined && !(body instanceof URLSearchParams)) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(body);
    }

    let res;
    try {
      res = await fetch(API_BASE + path, {
        credentials: "same-origin",
        method,
        headers,
        body,
      });
    } catch (err) {
      throw new ApiError("Network error — check your connection and try again.", 0);
    }

    const text = await res.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (_) {
        data = text;
      }
    }

    if (!res.ok) {
      const message = (data && data.message) || `Request failed (${res.status})`;
      throw new ApiError(message, res.status);
    }

    return data;
  }

  function manualCheckIn(path, memberId, eventId, overrideMembership) {
    return request(path, {
      method: "POST",
      body: {
        id: createCheckInId(),
        memberId,
        eventId,
        overrideMembership,
        method: "MANUAL",
        createdAt: new Date().toISOString(),
      },
    });
  }

  const api = {
    ApiError,

    // --- Auth ---
    login(email, password) {
      return request("/auth/login", {
        method: "POST",
        body: new URLSearchParams({ username: email, password }),
      });
    },
    logout() {
      return request("/auth/logout", { method: "POST" });
    },
    getCurrentUser() {
      return request("/auth/me", { method: "GET" });
    },

    // --- Members (Door view — no role info) ---
    getMembers(params = {}) {
      const query = new URLSearchParams(params).toString();
      return request(`/members${query ? `?${query}` : ""}`, { method: "GET" });
    },
    getMember(id) {
      return request(`/members/${id}`, { method: "GET" });
    },
    saveMember(fields, memberNumber) {
      return memberNumber == null
        ? request("/members", { method: "POST", body: fields })
        : request(`/members/${memberNumber}/update`, { method: "PUT", body: fields });
    },
    setMemberBanned(memberNumber, banned) {
      return request(`/members/${memberNumber}/${banned ? "ban" : "unban"}`, { method: "PUT" });
    },
    getMemberships(memberNumber, page) {
      return request(`/members/${memberNumber}/memberships?page=${page}`);
    },
    getMemberNotes(memberNumber, page) {
      return request(`/members/${memberNumber}/notes?page=${page}`);
    },
    addMemberNote(memberNumber, createdById, content) {
      return request(`/members/${memberNumber}/notes`, { method: "POST", body: { createdById, content } });
    },
    deleteMemberNote(memberNumber, noteId) {
      return request(`/members/${memberNumber}/notes/${noteId}`, { method: "DELETE" });
    },

    // --- Users (Admin view — includes role; AppUser 1:1 Member) ---
    getUsers(params = {}) {
      const query = new URLSearchParams(params).toString();
      return request(`/users${query ? `?${query}` : ""}`, { method: "GET" });
    },
    getUser(id) {
      return request(`/users/${id}`, { method: "GET" });
    },
    createUser(fields) {
      return request("/users", { method: "POST", body: fields });
    },
    searchEligibleMembers(query) {
      return request(`/users/eligible-members?${new URLSearchParams({ query })}`);
    },
    updateUser(id, fields) {
      return request(`/users/${id}`, { method: "PATCH", body: fields });
    },

    // --- Admin actions ---
    updateMemberStatus(id, status) {
      return request(`/members/${id}/status`, { method: "PATCH", body: { status } });
    },
    setMemberRole(id, role) {
      return request(`/members/${id}/role`, { method: "PATCH", body: { role } });
    },
    reissueQr(id) {
      return request(`/members/${id}/qr/reissue`, { method: "POST" });
    },

    // --- Door check-in ---
    getEvents() {
      return request("/events");
    },
    saveEvent(fields, id) {
      return id == null
        ? request("/events", { method: "POST", body: fields })
        : request(`/events/${id}`, { method: "PUT", body: fields });
    },
    deactivateEvent(id) {
      return request(`/events/${id}/disable`, { method: "PATCH" });
    },
    getCheckIns(memberId) {
      return request(memberId ? `/checkin/${memberId}` : "/checkin");
    },
    checkIn(qrPayload) {
      return request("/door/check-in", { method: "POST", body: { qrPayload } });
    },
    // Manual check-in from the member card (scan failed / no phone, etc).
    checkInManual(memberId, eventId, overrideMembership) {
      return manualCheckIn("/checkin", memberId, eventId, overrideMembership);
    },
    compCheckIn(memberId) {
      return manualCheckIn("/checkin/comp", memberId, null, true);
    },
  };

  window.App.api = api;

})();
