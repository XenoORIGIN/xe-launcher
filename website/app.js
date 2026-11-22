(() => {
  const T = {
    en: {
      eyebrow: "Android 10 or newer · APK file", title: "A Liquid Glass Home screen",
      lead: "Clean and minimal, with dark glass icons, automatic app categories, glass widgets and smooth animation everywhere.",
      download: "Download APK", studioDl: "Get Xe Studio", inDev: "In development", studioNote: "Xe Studio is still being built. You can make icon themes now; wallpapers, widgets and the theme store are coming.", featuresTitle: "Made to look good",
      f1: "The dock, category bar, buttons and menus are blurred glass with bright rims that bend what's behind them.",
      f2t: "Icons your way", f2: "Dark glass, black and white, colour glass, clear glass or the original, in 5 shapes. Rename apps and use your own icon pictures.",
      f3t: "Automatic categories", f3: "Swipe up to see apps by category. Reorder, hide, add or delete categories, or let Xe sort your apps again.",
      f4t: "Glass widgets", f4: "10 Xe widgets: clocks, calendar, search, photo, note, battery and more. Widgets from other apps work too.",
      f5t: "Your wallpaper", f5: "Import a picture, drag and pinch to fit, set dim and blur, then use it on the Home screen or lock screen.",
      f6t: "Update alerts", f6: "A new version pops up within minutes. One tap downloads it, and it installs over the old one with your settings kept.",
      f7t: "One-tap themes", f7: "13 themes in the app change the wallpaper, icon style and icon shape all at once.",
      f8t: "Glass toys", f8: "Light on the glass moves as you tilt the phone. Hold an empty spot on Home and drag for a glass lens.",
      f9t: "Report bugs in the app", f9: "Describe a problem and email it from the app. If Xe closes unexpectedly, it offers to send a report next time.",
      newsTitle: "What's new",
      faqTitle: "Questions",
      q1: "My phone warns the app isn't safe?", a1: "Phones warn about every app that isn't from the Play Store. Tap “Install anyway”. Xe Launcher doesn't ask for access to your personal data, and all of its code is open on GitHub.",
      q2: "How do I update?", a2: "When a new version is out, Xe shows a notification. Tap it to download, then install over the old one. Everything stays. Or go to Settings › About › Check for updates.",
      q3: "Does it work on other phones?", a3: "It should work on Android 10 or newer from any brand that lets you change the Home app, such as Samsung, Xiaomi, OPPO, vivo, realme and Pixel. So far it has only been tested on a real Samsung Galaxy A16 5G; if something looks wrong on your phone, please send a bug report.",
      q4: "How do I go back to my old Home screen?", a4: "Settings › Apps › Choose default apps › Home app, then pick your old one (on Samsung it's One UI Home).",
      q5: "Does it use a lot of battery?", a5: "Blur and effects use some power. To save battery, set Settings › Appearance › Glass quality › Saver and turn off tilt light.",
      bugTitle: "Found a bug?", bugText: "In the app: Settings › Report a bug, which attaches your phone's details for you. Or email",
      shotsTitle: "Screenshots", allVersions: "All versions",
      playTitle: "Try it", playSub: "Swipe up for categories, swipe sideways to switch, tap to open an app, hold for the menu.",
      cTheme: "Theme", cStyle: "Icon style", cShape: "Icon shape", lens: "Glass follows your finger",
      tilt: "Tilt your phone and the light on the glass rims moves with it.",
      themesTitle: "Themes", themesSub: "The same 13 themes as in the app (Settings › Themes). Tap one to try it on the phone above.",
      secret: "✦ There's a secret somewhere on this page",
      installTitle: "Install on Samsung",
      s1: "Tap Download APK above", s2: "Open the file and allow your browser to “Install unknown apps”",
      s3: "Tap Install. New versions install over the old one and keep your settings",
      s4: "Settings › Apps › Choose default apps › Home app › Xe Launcher",
      androidOnly: "Works on Android 10 or newer · tested on a real Samsung Galaxy A16 5G", version: "Latest: %s",
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
    if (io) { if (el.closest(".grid,ol")) el.style.transitionDelay = (i % 3) * 90 + "ms"; io.observe(el); } else el.classList.add("in");
  });
  const NEWS = [
    { v: "1.0.14", d: "2026-11-22", th: ["วอลเปเปอร์ท้องฟ้าสดตรงกับเวลาจริง (คำนวณพระอาทิตย์ขึ้น-ตก) เปลี่ยนเองแม้ไม่ได้เปิด Xe และใช้กับหน้าล็อกได้ ภาพสวยขึ้น", "วิดเจ็ตมีขนาดมากขึ้นและสวยขึ้น กดค้างเพื่อย้าย ลากมุมเพื่อปรับขนาด มีป้ายขนาดและปุ่มขนาดสำเร็จรูป", "จัดการหมวดหมู่: แก้บั๊กลากแล้วค้าง ปัดเพื่อซ่อน เลือกสีและการเรียงของแต่ละหมวด", "\"ใช้บ่อยที่สุด\" เปลี่ยนเป็น \"ใช้นานที่สุด\"", "ไอคอนกระจกหักเหแสง (แสงสะท้อน หรือ เลนส์) Dock แบบน้ำ ภาพมีมิติ เปิดแอปเป็นฟองกระจก ขอบกระจกเปลี่ยนสีตามเวลา", "วอลเปเปอร์ธีมสวยขึ้น ธีมใหม่รวม 13 แบบ และเสียงแจ้งเตือนอัปเดตแบบกระจก"],
      en: ["Live Sky now matches the real time (computed sunrise and sunset), changes even when Xe isn't open, can go on the lock screen, and looks better", "More, prettier widget sizes. Long-press to move, drag a corner to resize, with a live size label and size presets", "Category editor: fixed rows getting stuck, swipe to hide, pick each category's colour and sort", "\"Most used\" is now \"Used longest\"", "Glass icons that bend the light (Light or Lens), a liquid dock, depth parallax, a glass bubble app launch and rims that change colour with the time of day", "Prettier theme wallpapers, 13 themes in all, and a glass chime for update alerts"] },
    { v: "1.0.13", d: "2026-11-21", th: ["แอปใหม่ Xe Studio: สร้างธีมไอคอนเอง แต่งทั้งชุดด้วยทรง สี เส้นขอบ เงา แล้วแตะแก้ทีละไอคอนด้วยรูป ตัวอักษร หรือวาดเอง", "กด \"ใช้กับ Xe\" ไอคอนใน Xe เปลี่ยนทันที และยังอยู่แม้ลบ Studio", "แก้แจ้งเตือนอัปเดตที่ไม่ขึ้น ถ้าตอนนั้นการแจ้งเตือนถูกปิดอยู่"],
      en: ["New app Xe Studio: make your own icon theme. Style every icon by shape, colour, outline and shadow, then change any one with a picture, a letter or a drawing", "Tap Use in Xe and Xe's icons change at once, and stay even if Studio is removed", "Fixed update alerts that never came if notifications were off at the time"] },
    { v: "1.0.12", d: "2026-11-21", th: ["ตัวเลขข้อความที่ยังไม่ได้อ่านบนไอคอน เช่น LINE ทั้งหน้าโฮม ลิ้นชัก และโฟลเดอร์", "เลือกป้ายได้ 3 แบบ: แดงคลาสสิก กระจกขาว จุดเล็ก", "ปุ่มส่งแจ้งเตือนทดสอบในหน้าเกี่ยวกับ"],
      en: ["Unread counts on icons, e.g. LINE, on Home, in the drawer and on folders", "Three badge styles: classic red, white glass, small dot", "Send a test alert from About"] },
    { v: "1.0.11", d: "2026-11-21", th: ["หน้าลิ้นชักแอปแบบ Bento: แอปที่ใช้บ่อยสุดได้การ์ดกระจกใหญ่ ที่ใช้บ่อยรองลงมาได้การ์ดกว้าง", "เปลี่ยนกลับเป็นตารางได้ในตั้งค่า"],
      en: ["Bento app pages: your most used app gets a big glass card, the next ones wide cards", "Switch back to the grid in Settings"] },
    { v: "1.0.10", d: "2026-11-21", th: ["หน้าลิ้นชักใหม่: หมวดที่มีแอปน้อยได้ไอคอนใหญ่ จำนวนแอปใต้ชื่อหมวด และแสงสีประจำหมวด", "ลูกเล่นใหม่: ไอคอนมีแรงโน้มถ่วง เขย่าแล้วกระจาย บ่อน้ำมีฟองให้จิ้ม และแอปไม่ได้ใช้นานมีฝุ่นเกาะให้ถู", "แตะสองครั้งที่โฮมแล้ววาดตัวอักษรเพื่อเปิดแอป", "ลากนิ้วบนแถบหมวดเพื่อไล่หมวด ไอคอนขยายตามนิ้ว", "วอลเปเปอร์ท้องฟ้าสดตามเวลาจริง และเกมลับในหน้าเกี่ยวกับ"],
      en: ["New drawer page: sparse categories get big icons, an app count and their own colour glow", "New toys: gravity icons that scatter when shaken, a pond with bubbles to pop, and dust on apps you never open", "Double tap Home and draw a letter to open an app", "Drag along the category bar to scrub, icons swell under your finger", "Live Sky wallpaper that follows the time of day, and a secret game in About"] },
    { v: "1.0.9", d: "2026-11-21", th: ["แก้เลือกหมวดในลิ้นชักแอปแล้วไอคอนโหลดสองรอบ"], en: ["Picking a drawer category no longer loads the icons twice"] },
    { v: "1.0.8", d: "2026-11-21", th: ["หน้าแนะนำตอนเปิดครั้งแรก: โลโก้กระจก เลือกภาษา สอนใช้งาน และลองทำจริง", "ลิ้นชักแอปไอคอนไหลขึ้นทีละแถว และหน้าโฮมเอียงมีมิติตอนปัด", "เปิดแอปขยายจากไอคอน กลับหน้าโฮมซูมเข้าที่", "หน้าตั้งค่าเปลี่ยนแบบซ้อนกระจก สวิตช์เด้งแบบเจลลี่"],
      en: ["First-run welcome: glass logo, language picker, tutorial and hands-on practice", "Drawer icons rise row by row; Home pages lean with depth as you swipe", "Apps zoom out of their icon; Home settles back in", "Settings pages slide like stacked glass; jelly switches"] },
    { v: "1.0.7", d: "2026-11-21", th: ["แก้ปัดบน Widget แล้วเปิดเมนูหรือเปิดแอปของ Widget เอง", "ไอคอนที่อยู่บนจอขึ้นก่อน ไม่ต้องรอทั้งเครื่อง และเลื่อนลื่นขึ้นตอนกำลังโหลด"],
      en: ["Swiping over a widget no longer opens its menu or the widget's app", "Icons on screen load first, and scrolling stays smooth while icons load"] },
    { v: "1.0.6", d: "2026-11-21", th: ["แจ้งเตือนอัปเดตมาเร็วขึ้น เช็กตอนเปิดแอป ตอนกลับหน้าโฮม และทุก 30 นาที", "แจ้งเตือนเด้งขึ้นบนจอ"],
      en: ["Update alerts arrive much sooner: on start, on returning Home and every 30 minutes", "Alerts pop up on screen"] },
    { v: "1.0.5", d: "2026-11-21", th: ["ธีม 10 แบบ แตะเดียวเปลี่ยนทั้งหน้า", "แจ้งบัคในแอป และถามส่งรายงานเมื่อแอปปิดเอง", "แสงบนกระจกขยับตามการเอียง", "เลนส์กระจกตามนิ้วบนหน้าโฮม", "ความลับในหน้าเกี่ยวกับ"],
      en: ["10 one-tap themes", "Report bugs from the app; offers a report after a crash", "Light on glass follows tilt", "Glass lens on Home", "A secret in Settings › About"] },
    { v: "1.0.4", d: "2026-11-21", th: ["Widget กระจกใหม่ 7 แบบ", "ตั้งค่าหมวดได้มากขึ้น ซ่อน จัดลำดับ จัดอัตโนมัติ", "ไอคอนโหลดเร็วขึ้น และแอนิเมชันใหม่", "แก้ Widget เปิดเมนูเองตอนปัดหน้า"],
      en: ["7 new glass widgets", "More category settings: hide, reorder, auto-sort", "Faster icons and new animations", "Widgets no longer open their menu while swiping"] },
  ];
  function renderNews() {
    const el = document.getElementById("timeline"); if (!el) return;
    const fmt = d => new Date(d + "T12:00:00").toLocaleDateString(lang === "en" ? "en-GB" : "th-TH", { day: "numeric", month: "short", year: "numeric" });
    el.innerHTML = NEWS.map((n, i) => `<article class="glass card rel${i === 0 ? " latest" : ""}"><header><b>${n.v}</b><span class="muted">${fmt(n.d)}</span>${i === 0 ? `<em>${lang === "en" ? "Latest" : "ล่าสุด"}</em>` : ""}</header><ul>${n[lang].map(x => `<li>${x}</li>`).join("")}</ul></article>`).join("");
  }
  addEventListener("xe-lang", renderNews);
  btn.addEventListener("click", () => { lang = lang === "en" ? "th" : "en"; try { localStorage.setItem("xe-lang", lang); } catch (e) {} apply(); });
  apply();
  renderNews();
  const top = document.querySelector(".top");
  const onScroll = () => top.classList.toggle("scrolled", scrollY > 12);
  addEventListener("scroll", onScroll, { passive: true }); onScroll();
  fetch("https://api.github.com/repos/XenoORIGIN/xe-launcher/releases/latest", { headers: { Accept: "application/vnd.github+json" } })
    .then(r => (r.ok ? r.json() : null))
    .then(j => { if (j && j.tag_name) { ver = j.tag_name; apply(); } })
    .catch(() => {});
})();
