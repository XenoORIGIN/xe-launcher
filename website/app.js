(() => {
  const T = {
    en: {
      eyebrow: "Android 10 or newer · APK file", title: "A Liquid Glass Home screen",
      lead: "Clean and minimal, with dark glass icons, automatic app categories, glass widgets and smooth animation everywhere.",
      download: "Download APK", featuresTitle: "Made to look good",
      f1: "The dock, category bar, buttons and menus are blurred glass with bright rims that bend what's behind them.",
      f2t: "Icons your way", f2: "Dark glass, black and white, colour glass, clear glass or the original, in 5 shapes. Rename apps and use your own icon pictures.",
      f3t: "Automatic categories", f3: "Swipe up to see apps by category. Reorder, hide, add or delete categories, or let Xe sort your apps again.",
      f4t: "Glass widgets", f4: "10 Xe widgets: clocks, calendar, search, photo, note, battery and more. Widgets from other apps work too.",
      f5t: "Your wallpaper", f5: "Import a picture, drag and pinch to fit, set dim and blur, then use it on the Home screen or lock screen.",
      f6t: "Update alerts", f6: "Xe tells you when a new version is out. One tap downloads it, and it installs over the old one with your settings kept.",
      shotsTitle: "Screenshots", allVersions: "All versions",
      playTitle: "Try it", playSub: "Swipe up for categories, swipe sideways to switch, tap to open an app, hold for the menu.",
      cTheme: "Theme", cStyle: "Icon style", cShape: "Icon shape", lens: "Glass follows your finger",
      tilt: "Tilt your phone and the light on the glass rims moves with it.",
      themesTitle: "Themes", themesSub: "10 themes come with the app. Tap one to try it on the phone above.",
      secret: "✦ There's a secret somewhere on this page",
      installTitle: "Install on Samsung",
      s1: "Tap Download APK above", s2: "Open the file and allow your browser to “Install unknown apps”",
      s3: "Tap Install. New versions install over the old one and keep your settings",
      s4: "Settings › Apps › Choose default apps › Home app › Xe Launcher",
      androidOnly: "Works on Android 10 or newer", version: "Latest: %s",
    },
    th: { version: "เวอร์ชันล่าสุด %s" },
  };
  const nodes = [...document.querySelectorAll("[data-i18n]")];
  nodes.forEach(n => (n.dataset.th = n.textContent));
  let saved = null; try { saved = localStorage.getItem("xe-lang"); } catch (e) {}
  let lang = (saved || (navigator.language || "th").slice(0, 2)) === "en" ? "en" : "th";
  let ver = "";
  const btn = document.getElementById("lang");
  const verEl = document.getElementById("version");
  function apply() {
    document.documentElement.lang = lang;
    nodes.forEach(n => (n.textContent = lang === "en" ? T.en[n.dataset.i18n] ?? n.dataset.th : n.dataset.th));
    btn.textContent = lang === "en" ? "ไทย" : "EN";
    dispatchEvent(new Event("xe-lang"));
    verEl.textContent = ver ? (T[lang].version || T.en.version).replace("%s", ver) : "";
  }
  const io = "IntersectionObserver" in window ? new IntersectionObserver(es => es.forEach(e => {
    if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
  }), { rootMargin: "0px 0px -8% 0px" }) : null;
  document.querySelectorAll(".reveal").forEach((el, i) => {
    if (io) { el.style.transitionDelay = (el.closest(".grid,ol") ? (i % 3) * 70 : 0) + "ms"; io.observe(el); } else el.classList.add("in");
  });
  btn.addEventListener("click", () => { lang = lang === "en" ? "th" : "en"; try { localStorage.setItem("xe-lang", lang); } catch (e) {} apply(); });
  apply();
  fetch("https://api.github.com/repos/XenoORIGIN/xe-launcher/releases/latest", { headers: { Accept: "application/vnd.github+json" } })
    .then(r => (r.ok ? r.json() : null))
    .then(j => { if (j && j.tag_name) { ver = j.tag_name; apply(); } })
    .catch(() => {});
})();
