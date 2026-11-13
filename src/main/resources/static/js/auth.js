/* auth.js
 * Current session user, role checks, and sign-in/sign-out.
 */
window.App = window.App || {};

(function () {
  let currentUser = null;

  async function restoreSession() {
    currentUser = await App.api.getCurrentUser();
    return currentUser;
  }

  async function logout() {
    await App.api.logout();
    currentUser = null;
    window.location.replace("login.html");
  }

  function isAdmin() {
    return currentUser?.role === "ADMIN";
  }

  window.App.auth = { login: App.api.login, restoreSession, logout, isAdmin };
})();
