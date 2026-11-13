/* api.js
 * All fetch() calls live here, plus one shared error type/handler.
 * Every other file talks to the backend through App.api — nothing else
 * should call fetch() directly.
 */
window.App = window.App || {};

(function () {

  const API_BASE = "/api";

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

    // --- Users (Admin view — includes role; AppUser 1:1 Member) ---
    getUsers(params = {}) {
      const query = new URLSearchParams(params).toString();
      return request(`/users${query ? `?${query}` : ""}`, { method: "GET" });
    },
    getUser(id) {
      return request(`/users/${id}`, { method: "GET" });
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
    checkIn(qrPayload) {
      return request("/door/check-in", { method: "POST", body: { qrPayload } });
    },
    // Manual check-in from the member card (scan failed / no phone, etc).
    // ASSUMPTION: same endpoint, keyed by memberId instead of a QR payload —
    // adjust if the backend expects something different.
    checkInManual(memberId) {
      return request("/door/check-in", { method: "POST", body: { memberId, method: "manual" } });
    },
  };

  window.App.api = api;

})();
