'use strict';
const themeSelect = document.querySelector('#theme');
let preference = 'system';
try { preference = localStorage.getItem('odmani-site-theme') || 'system'; } catch {}
if (!['system', 'light', 'dark'].includes(preference)) preference = 'system';
function applyTheme(value) {
  if (value === 'system') delete document.documentElement.dataset.theme;
  else document.documentElement.dataset.theme = value;
  if (themeSelect) themeSelect.value = value;
}
applyTheme(preference);
themeSelect?.addEventListener('change', event => {
  applyTheme(event.target.value);
  try { localStorage.setItem('odmani-site-theme', event.target.value); } catch {}
});

const feedbackForm = document.querySelector('#feedback-form');
feedbackForm?.addEventListener('submit', event => {
  event.preventDefault();
  if (!feedbackForm.reportValidity()) return;
  const category = document.querySelector('#feedback-type').value;
  const message = document.querySelector('#feedback-message').value.trim();
  if (!message) { document.querySelector('#feedback-message').focus(); return; }
  const subject = `ODMANI (BETA) — ${category}`;
  const body = `Aplicativo: ODMANI (BETA)\nVersão: 2.1.0-alpha05 (8)\nCategoria: ${category}\n\n${message}`;
  document.querySelector('#feedback-status').textContent = 'Seu aplicativo de e-mail será aberto. Revise a mensagem e confirme o envio nele. Se não abrir, utilize o endereço de e-mail ao lado.';
  window.location.href = `mailto:studio.47.agencia.co@gmail.com?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
});
