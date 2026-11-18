(function () {
  const token = window.location.hash.slice(1);
  if (window.location.hash) window.history.replaceState(null, "", window.location.pathname);

  const form = document.getElementById("inviteForm");
  const error = document.getElementById("inviteError");
  const password = document.getElementById("password");
  const confirmPassword = document.getElementById("confirmPassword");
  const submit = form.querySelector("button");

  function showError(message) {
    error.textContent = message;
    error.classList.toggle("visible", Boolean(message));
  }

  if (!/^[0-9a-f]{64}$/.test(token)) {
    showError("Open the link in your invitation email or ask an admin to resend your invite.");
    return;
  }

  form.style.removeProperty("display");
  form.addEventListener("input", () => showError(""));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (submit.disabled) return;
    if (password.value !== confirmPassword.value) {
      showError("Enter the same password in both fields.");
      confirmPassword.focus();
      return;
    }

    submit.disabled = true;
    try {
      await App.api.acceptInvite(token, password.value);
      form.reset();
      window.location.replace("login.html");
    } catch (failure) {
      showError(failure.message);
      submit.disabled = false;
    }
  });
})();
