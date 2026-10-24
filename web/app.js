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
// Distribution is locked until a signed, approved release is explicitly configured.
// No debug APK or test build may be linked from this page.
