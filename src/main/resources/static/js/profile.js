(function () {
  const form = document.getElementById("passwordForm");
  const current = document.getElementById("currentPassword");
  const password = document.getElementById("newPassword");
  const confirm = document.getElementById("confirmPassword");
  const submit = document.getElementById("savePassword");
  const feedback = document.getElementById("passwordFeedback");

  function showFeedback(message, success = false) {
    feedback.textContent = message;
    feedback.hidden = !message;
    feedback.classList.toggle("is-success", success);
  }

  form.addEventListener("input", () => showFeedback(""));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (submit.disabled) return;
    if (Array.from(password.value).length < 12 || new TextEncoder().encode(password.value).length > 72) {
      showFeedback("Use at least 12 characters and at most 72 bytes for your password.");
      password.focus();
      return;
    }
    if (password.value !== confirm.value) {
      showFeedback("Enter the same new password in both fields.");
      confirm.focus();
      return;
    }
    submit.disabled = true;
    submit.textContent = "Updating…";
    // Keep the fields stable while this request is in progress.
    [current, password, confirm].forEach((input) => { input.readOnly = true; });
    try {
      await App.api.changePassword(current.value, password.value);
      form.reset();
      showFeedback("Your password has been updated.", true);
    } catch (failure) {
      if (failure.status === 401) {
        window.location.replace("login.html");
        return;
      }
      showFeedback(failure.message);
    } finally {
      [current, password, confirm].forEach((input) => { input.readOnly = false; });
      submit.disabled = false;
      submit.textContent = "Update password";
    }
  });

  async function init() {
    try {
      const user = await App.auth.restoreSession();
      document.getElementById("profileIdentity").textContent = user.displayName;
      document.getElementById("sessionStatus").hidden = true;
      document.body.classList.remove("session-pending");
    } catch (failure) {
      if (failure.status === 401) window.location.replace("login.html");
      else document.getElementById("sessionStatus").textContent =
        `Couldn't restore your session: ${failure.message} Reload the page to retry.`;
    }
  }
  init();
})();
