(() => {
  const T = {
    en: {
      eyebrow: "For Android · APK file", title: "A Liquid Glass Home screen",
      lead: "Clean and minimal, with black-and-white glass icons, automatic app categories and smooth animation everywhere.",
      download: "Download APK", featuresTitle: "Made to look good",
      f1: "The dock, category bar, buttons and menus are blurred glass with bright rims that bend what's behind them.",
      f2t: "Dark glass icons", f2: "Every app's real icon in black and white. Or pick colour glass, clear glass or the original.",
      f3t: "Automatic categories", f3: "Swipe up to see apps by category. Move apps and add, delete or rename categories.",
      f4t: "Smooth animation", f4: "A liquid bubble flows between categories, icons bounce in one by one, buttons squish when pressed.",
      f5t: "Thai and English", f5: "Clean type in both languages. Switch language in Settings.",
      f6: "Make your own wallpapers, icons and widgets — coming soon.",
      installTitle: "Install on Samsung",
      s1: "Tap Download APK above", s2: "Open the file and allow your browser to “Install unknown apps”",
      s3: "Tap Install. New versions install over the old one and keep your settings",
      s4: "Settings › Apps › Choose default apps › Home app › Xe Launcher",
      androidOnly: "Android 10 or newer only", version: "Latest: %s",
    },
    th: { version: "เวอร์ชันล่าสุด %s" },
  };
  const nodes = [...document.querySelectorAll("[data-i18n]")];
  nodes.forEach(n => (n.dataset.th = n.textContent));
  let lang = (localStorage.getItem("xe-lang") || (navigator.language || "th").slice(0, 2)) === "en" ? "en" : "th";
  let ver = "";
  const btn = document.getElementById("lang");
  const verEl = document.getElementById("version");
  function apply() {
    document.documentElement.lang = lang;
    nodes.forEach(n => (n.textContent = lang === "en" ? T.en[n.dataset.i18n] ?? n.dataset.th : n.dataset.th));
    btn.textContent = lang === "en" ? "ไทย" : "EN";
    verEl.textContent = ver ? (T[lang].version || T.en.version).replace("%s", ver) : "";
  }
  btn.addEventListener("click", () => { lang = lang === "en" ? "th" : "en"; try { localStorage.setItem("xe-lang", lang); } catch (e) {} apply(); });
  apply();
  fetch("https://api.github.com/repos/XenoORIGIN/xe-launcher/releases/latest", { headers: { Accept: "application/vnd.github+json" } })
    .then(r => (r.ok ? r.json() : null))
    .then(j => { if (j && j.tag_name) { ver = j.tag_name; apply(); } })
    .catch(() => {});
})();
