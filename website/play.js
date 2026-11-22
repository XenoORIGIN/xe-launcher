/* Xe Launcher website: playable phone, themes, glass lens, tilt shine and the Supernova easter egg. */
(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
  const lang = () => (document.documentElement.lang === "en" ? "en" : "th");
  const tr = (th, en) => (lang() === "en" ? en : th);

  /* Tabler-style outline glyphs (MIT) */
  const P = {
    phone: "M5 4h4l2 5l-2.5 1.5a11 11 0 0 0 5 5l1.5 -2.5l5 2v4a2 2 0 0 1 -2 2a16 16 0 0 1 -15 -15a2 2 0 0 1 2 -2",
    message: "M8 9h8M8 13h6M18 4a3 3 0 0 1 3 3v8a3 3 0 0 1 -3 3h-5l-5 3v-3h-2a3 3 0 0 1 -3 -3v-8a3 3 0 0 1 3 -3h12z",
    mail: "M3 7a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-10zM3 7l9 6l9 -6",
    camera: "M5 7h1a2 2 0 0 0 2 -2a1 1 0 0 1 1 -1h6a1 1 0 0 1 1 1a2 2 0 0 0 2 2h1a2 2 0 0 1 2 2v9a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-9a2 2 0 0 1 2 -2M9 13a3 3 0 1 0 6 0a3 3 0 0 0 -6 0",
    photo: "M15 8h.01M3 6a3 3 0 0 1 3 -3h12a3 3 0 0 1 3 3v12a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3v-12zM3 16l5 -5c.928 -.893 2.072 -.893 3 0l5 5M14 14l1 -1c.928 -.893 2.072 -.893 3 0l3 3",
    music: "M3 17a3 3 0 1 0 6 0a3 3 0 0 0 -6 0M13 17a3 3 0 1 0 6 0a3 3 0 0 0 -6 0M9 17v-13h10v13M9 8h10",
    video: "M15 10l4.553 -2.276a1 1 0 0 1 1.447 .894v6.764a1 1 0 0 1 -1.447 .894l-4.553 -2.276v-4zM3 8a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2z",
    play: "M7 4v16l13 -8z",
    heart: "M19.5 12.572l-7.5 7.428l-7.5 -7.428a5 5 0 1 1 7.5 -6.566a5 5 0 1 1 7.5 6.572",
    users: "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0M3 21v-2a4 4 0 0 1 4 -4h4a4 4 0 0 1 4 4v2M16 3.13a4 4 0 0 1 0 7.75M21 21v-2a4 4 0 0 0 -3 -3.85",
    game: "M2 8a2 2 0 0 1 2 -2h16a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2zM6 12h4M8 10v4M15 11v.01M18 13v.01",
    rocket: "M4 13a8 8 0 0 1 7 7a6 6 0 0 0 3 -5a9 9 0 0 0 6 -8a3 3 0 0 0 -3 -3a9 9 0 0 0 -8 6a6 6 0 0 0 -5 3M7 14a6 6 0 0 0 -3 6a6 6 0 0 0 6 -3M14 9a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
    bag: "M6.331 8h11.339a2 2 0 0 1 1.977 2.304l-1.255 8.152a3 3 0 0 1 -2.966 2.544h-6.852a3 3 0 0 1 -2.965 -2.544l-1.255 -8.152a2 2 0 0 1 1.977 -2.304zM9 11v-5a3 3 0 0 1 6 0v5",
    wallet: "M17 8v-3a1 1 0 0 0 -1 -1h-10a2 2 0 0 0 0 4h12a1 1 0 0 1 1 1v3m0 4v3a1 1 0 0 1 -1 1h-12a2 2 0 0 1 -2 -2v-12M20 12v4h-4a2 2 0 0 1 0 -4h4",
    coin: "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0M14.8 9a2 2 0 0 0 -1.8 -1h-2a2 2 0 1 0 0 4h2a2 2 0 1 1 0 4h-2a2 2 0 0 1 -1.8 -1M12 7v10",
    map: "M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0M17.657 16.657l-4.243 4.243a2 2 0 0 1 -2.827 0l-4.244 -4.243a8 8 0 1 1 11.314 0z",
    world: "M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0M3.6 9h16.8M3.6 15h16.8M11.5 3a17 17 0 0 0 0 18M12.5 3a17 17 0 0 1 0 18",
    calendar: "M4 7a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-12zM16 3v4M8 3v4M4 11h16M11 15h1M12 15v3",
    notes: "M5 5a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2zM9 7h6M9 11h6M9 15h4",
    calc: "M4 5a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2zM8 8a1 1 0 0 1 1 -1h6a1 1 0 0 1 1 1v1a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1zM8 14v.01M12 14v.01M16 14v.01M8 17v.01M12 17v.01M16 17v.01",
    clock: "M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0M12 7v5l3 3",
    sliders: "M14 6a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M4 6h10M18 6h2M6 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M4 12h2M10 12h10M15 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M4 18h11M19 18h1",
    brief: "M3 9a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v9a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2zM8 7v-2a2 2 0 0 1 2 -2h4a2 2 0 0 1 2 2v2M12 12v.01M3 13a20 20 0 0 0 18 0",
    tool: "M7 10h3v-3l-3.5 -3.5a6 6 0 0 1 8 8l6 6a2 2 0 0 1 -3 3l-6 -6a6 6 0 0 1 -8 -8l3.5 3.5",
    search: "M3 10a7 7 0 1 0 14 0a7 7 0 1 0 -14 0M21 21l-6 -6",
    dots: "M5 12h.01M12 12h.01M19 12h.01",
    edit: "M4 20h4l10.5 -10.5a2.828 2.828 0 1 0 -4 -4l-10.5 10.5v4M13.5 6.5l4 4",
    info: "M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0M12 9h.01M11 12h1v4h1",
    trash: "M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2 -2l1 -12M9 7v-3a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v3",
    sparkle: "M16 18a2 2 0 0 1 2 2a2 2 0 0 1 2 -2a2 2 0 0 1 -2 -2a2 2 0 0 1 -2 2zm0 -12a2 2 0 0 1 2 2a2 2 0 0 1 2 -2a2 2 0 0 1 -2 -2a2 2 0 0 1 -2 2zm-7 12a6 6 0 0 1 6 -6a6 6 0 0 1 -6 -6a6 6 0 0 1 -6 6a6 6 0 0 1 6 6z",
  };
  const g = (k, cls = "") => `<svg class="gl ${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${P[k]}"/></svg>`;

  /* id: [thai, english, glyph, colour, category] */
  const A = {
    phone: ["โทรศัพท์", "Phone", "phone", "#34C759", "comm"],
    msg: ["ข้อความ", "Messages", "message", "#0A84FF", "comm"],
    mail: ["อีเมล", "Mail", "mail", "#2F6BFF", "comm"],
    chat: ["แชท", "Chat", "message", "#5865F2", "social"],
    friends: ["เพื่อน", "Friends", "heart", "#FF2D55", "social"],
    people: ["ชุมชน", "Community", "users", "#BF5AF2", "social"],
    camera: ["กล้อง", "Camera", "camera", "#3A3A3C", "media"],
    photos: ["รูปภาพ", "Photos", "photo", "#FF9F0A", "media"],
    music: ["เพลง", "Music", "music", "#FF375F", "media"],
    video: ["วิดีโอ", "Video", "video", "#FF453A", "media"],
    games: ["เกม", "Games", "game", "#AF52DE", "games"],
    rocket: ["จรวด", "Rocket", "rocket", "#5E5CE6", "games"],
    shop: ["ช้อป", "Shop", "bag", "#FF6A00", "shop"],
    mall: ["ห้าง", "Mall", "bag", "#FF2D7A", "shop"],
    deals: ["ดีลเด็ด", "Deals", "coin", "#FFB800", "shop"],
    wallet: ["วอลเล็ต", "Wallet", "wallet", "#30B158", "money"],
    bank: ["ธนาคาร", "Bank", "coin", "#0A7AFF", "money"],
    maps: ["แผนที่", "Maps", "map", "#32ADE6", "travel"],
    trip: ["ทริป", "Trips", "world", "#FF9F0A", "travel"],
    work: ["งาน", "Work", "brief", "#64D2FF", "work"],
    study: ["เรียน", "Study", "notes", "#5856D6", "work"],
    meet: ["ประชุม", "Meet", "video", "#00C7BE", "work"],
    web: ["เว็บ", "Browser", "world", "#0A84FF", "tools"],
    cal: ["ปฏิทิน", "Calendar", "calendar", "#FF3B30", "tools"],
    notes: ["โน้ต", "Notes", "notes", "#FFD60A", "tools"],
    calc: ["เครื่องคิดเลข", "Calculator", "calc", "#FF9500", "tools"],
    clock: ["นาฬิกา", "Clock", "clock", "#1C1C1E", "tools"],
    settings: ["ตั้งค่า", "Settings", "sliders", "#8E8E93", "tools"],
  };
  const CATS = [
    ["comm", "สื่อสาร", "Communication", "message"], ["social", "โซเชียล", "Social", "users"],
    ["media", "บันเทิง", "Media", "play"], ["games", "เกม", "Games", "game"],
    ["shop", "ช้อปปิ้ง", "Shopping", "bag"], ["money", "การเงิน", "Finance", "wallet"],
    ["travel", "เดินทางและอาหาร", "Travel", "map"], ["work", "งานและการเรียน", "Work", "brief"],
    ["tools", "เครื่องมือ", "Tools", "tool"],
  ];
  const HOME = ["photos", "camera", "music", "maps", "cal", "notes", "shop", "games"];
  const DOCK = ["phone", "msg", "web", "settings"];

  const STYLES = [["dark", "กระจกดำ", "Dark glass"], ["mono", "ขาวดำ", "Mono"], ["color", "กระจกสี", "Colour glass"], ["clear", "กระจกใส", "Clear glass"], ["orig", "ต้นฉบับ", "Original"]];
  const SHAPES = [["squircle", "Squircle"], ["circle", "Circle"], ["rounded", "Rounded"], ["square", "Square"], ["tear", "Teardrop"]];
  const THEMES = [
    { id: "xe_black", name: "Xe Black", style: "dark", shape: "squircle" },
    { id: "xe_blue", name: "Xe Blue", style: "clear", shape: "squircle" },
    { id: "aurora", name: "Aurora", style: "color", shape: "circle" },
    { id: "sunset", name: "Sunset", style: "color", shape: "rounded" },
    { id: "mono_wave", name: "Mono Wave", style: "mono", shape: "square" },
    { id: "ocean", name: "Ocean", style: "clear", shape: "tear" },
    { id: "violet_glass", name: "Violet Glass", style: "clear", shape: "squircle" },
    { id: "ember", name: "Ember", style: "dark", shape: "circle" },
    { id: "rose", name: "Rose", style: "orig", shape: "circle", light: true },
    { id: "mint", name: "Mint", style: "orig", shape: "rounded", light: true },
    { id: "liquid_azure", name: "Liquid Azure", style: "clear", shape: "squircle" },
    { id: "frost_aurora", name: "Frost Aurora", style: "color", shape: "circle" },
    { id: "chrome_night", name: "Chrome Night", style: "mono", shape: "squircle" },
  ];
  const NOVA = { id: "nova", name: "Supernova", style: "color", shape: "tear", secret: true };

  const hex = c => [1, 3, 5].map(i => parseInt(c.slice(i, i + 2), 16));
  const lum = c => { const [r, gg, b] = hex(c); return (0.2126 * r + 0.7152 * gg + 0.0722 * b) / 255; };
  const wallCss = t => (t.secret ? "" : `url(assets/wall/${t.id}.webp)`);

  function icon(id, st, sh, label = true) {
    const [th, en, gl, c] = A[id];
    const L = lum(c);
    const m = Math.round(40 + L * 200);
    const vars = `--c:${c};--m:rgb(${m},${m},${m});--mg:${m > 185 ? "#000" : "#fff"};--og:${L > 0.72 ? "#1a1a1a" : "#fff"}`;
    return `<div class="app" data-app="${id}"><div class="ic s-${st} sh-${sh}" style="${vars}">${g(gl)}</div>${label ? `<span>${tr(th, en)}</span>` : ""}</div>`;
  }

  function now() {
    const d = new Date();
    const t = d.toLocaleTimeString(lang() === "en" ? "en-GB" : "th-TH", { hour: "2-digit", minute: "2-digit" });
    const date = d.toLocaleDateString(lang() === "en" ? "en-GB" : "th-TH", { weekday: "long", day: "numeric", month: "long" });
    return { t, date };
  }

  function homeHTML(s) {
    const { t, date } = now();
    return `<div class="home view">
      <div class="wclock glass-in"><b class="wt">${t}</b><span class="wd"><i></i>${date}</span></div>
      <div class="agrid">${HOME.map(id => icon(id, s.style, s.shape)).join("")}</div>
      <div class="pdots"><i class="on"></i><i></i></div>
      <div class="dock glass-in">${DOCK.map(id => icon(id, s.style, s.shape, false)).join("")}</div>
    </div>`;
  }

  function drawerHTML(s) {
    const cat = CATS[s.cat];
    const apps = Object.keys(A).filter(k => A[k][4] === cat[0]);
    return `<div class="drawer view">
      <div class="dhead"><b>${tr(cat[1], cat[2])}</b><span class="rb glass-in">${g("search")}</span><span class="rb glass-in">${g("bag")}</span><span class="rb glass-in">${g("dots")}</span></div>
      <div class="agrid dgrid">${apps.map(id => icon(id, s.style, s.shape)).join("")}</div>
      <div class="catbar glass-in"><span class="bubble"></span>${CATS.map((c, i) => `<button class="cb${i === s.cat ? " on" : ""}" data-cat="${i}" aria-label="${tr(c[1], c[2])}">${g(c[3])}</button>`).join("")}</div>
    </div>`;
  }

  function screenHTML(s, mini) {
    return `<div class="wall" style="background-image:${wallCss(s.theme)}"></div><div class="wall b"></div>
      <div class="status"><span class="st">${now().t}</span><span class="si"><i></i><i></i><b></b></span></div>
      ${homeHTML(s)}${mini ? "" : drawerHTML(s)}
      ${mini ? "" : `<div class="appwin"></div><div class="ctx glass-in"></div><div class="hint glass-in">${tr("ปัดขึ้นเพื่อดูแอปตามหมวด", "Swipe up for app categories")}</div>`}
      <div class="hbar"></div>`;
  }

  /* ---------- the playable phone ---------- */
  const dev = $("#dev");
  if (!dev) return;
  const S = { theme: THEMES[0], style: "dark", shape: "squircle", cat: 0, open: false, hinted: false };
  const scr = $(".scr", dev);

  function paint() {
    dev.classList.toggle("light", !!S.theme.light);
    dev.classList.toggle("nova", !!S.theme.secret);
    scr.innerHTML = screenHTML(S, false);
    if (S.hinted) $(".hint", scr)?.remove();
    setDrawer(S.open ? 1 : 0, false);
    placeBubble(false);
  }

  function setWall(t) {
    const [a, b] = $$(".wall", scr);
    b.style.backgroundImage = wallCss(t);
    b.classList.add("show");
    setTimeout(() => { a.style.backgroundImage = wallCss(t); b.classList.remove("show"); }, 520);
  }

  function restyle() {
    $$(".ic", scr).forEach(el => { el.className = `ic s-${S.style} sh-${S.shape}`; });
  }

  function applyTheme(t, scroll) {
    S.theme = t; S.style = t.style; S.shape = t.shape;
    dev.classList.toggle("light", !!t.light);
    dev.classList.toggle("nova", !!t.secret);
    setWall(t); restyle(); syncChips();
    if (scroll) dev.scrollIntoView({ behavior: reduce ? "auto" : "smooth", block: "center" });
  }

  function setDrawer(p, anim = true) {
    const home = $(".home", scr), dr = $(".drawer", scr);
    if (!home || !dr) return;
    [home, dr].forEach(el => (el.style.transition = anim ? "" : "none"));
    dr.style.transform = `translateY(${(1 - p) * 100}%)`;
    dr.style.opacity = Math.min(1, p * 1.6);
    home.style.transform = `scale(${1 - p * 0.08})`;
    home.style.opacity = 1 - p;
    home.style.filter = `blur(${p * 6}px)`;
    $$(".wall", scr).forEach(w => (w.style.filter = `blur(${p * 14}px) brightness(${1 - p * 0.35})`));
    if (!anim) requestAnimationFrame(() => [home, dr].forEach(el => (el.style.transition = "")));
  }

  function placeBubble(stretch = true) {
    const bar = $(".catbar", scr); if (!bar) return;
    const btn = $(`.cb[data-cat="${S.cat}"]`, bar), bub = $(".bubble", bar);
    const x = btn.offsetLeft + btn.offsetWidth / 2 - bub.offsetWidth / 2;
    const bx = parseFloat(bub.dataset.x || x);
    bub.dataset.x = x;
    bub.style.transform = `translateX(${x}px)`;
    if (stretch && !reduce && Math.abs(bx - x) > 2) {
      bub.animate([{ scale: "1 1" }, { scale: "1.45 .82" }, { scale: "1 1" }], { duration: 420, easing: "ease-out" });
    }
    $$(".cb", bar).forEach(b => b.classList.toggle("on", +b.dataset.cat === S.cat));
  }

  function setCat(i) {
    if (i < 0 || i >= CATS.length || i === S.cat) return;
    const dir = i > S.cat ? 1 : -1;
    S.cat = i;
    const old = $(".dgrid", scr);
    const tmp = document.createElement("div");
    tmp.innerHTML = drawerHTML(S);
    $(".dhead b", scr).textContent = tr(CATS[i][1], CATS[i][2]);
    const fresh = $(".dgrid", tmp);
    old.replaceWith(fresh);
    if (!reduce) fresh.animate([{ transform: `translateX(${dir * 30}%)`, opacity: 0 }, { transform: "none", opacity: 1 }], { duration: 360, easing: "cubic-bezier(.2,.9,.3,1)" });
    placeBubble(true);
  }

  function openApp(el) {
    const id = el.closest("[data-app]").dataset.app;
    const ic = $(".ic", el.closest("[data-app]"));
    const [th, en, gl, c] = A[id];
    const sr = scr.getBoundingClientRect(), r = ic.getBoundingClientRect();
    const win = $(".appwin", scr);
    win.innerHTML = `<div class="aw-in">${g(gl)}<b>${tr(th, en)}</b><small>${tr("แตะเพื่อกลับหน้าหลัก", "Tap to go home")}</small></div>`;
    win.style.setProperty("--c", c);
    win.dataset.from = JSON.stringify([r.left - sr.left, r.top - sr.top, r.width / sr.width, r.height / sr.height]);
    win.style.transition = "none";
    win.style.transform = `translate(${r.left - sr.left}px,${r.top - sr.top}px) scale(${r.width / sr.width},${r.height / sr.height})`;
    win.style.borderRadius = "40%";
    win.classList.add("on");
    win.getBoundingClientRect();
    win.style.transition = "";
    win.style.transform = "none";
    win.style.borderRadius = "";
  }

  function closeApp() {
    const win = $(".appwin.on", scr); if (!win) return false;
    const [x, y, sx, sy] = JSON.parse(win.dataset.from);
    win.style.transform = `translate(${x}px,${y}px) scale(${sx},${sy})`;
    win.style.borderRadius = "40%";
    win.classList.add("closing");
    setTimeout(() => win.classList.remove("on", "closing"), 380);
    return true;
  }

  const MENU = [["edit", "เปลี่ยนไอคอน", "Change icon"], ["info", "ข้อมูลแอป", "App info"], ["trash", "เอาออกจากหน้าหลัก", "Remove from Home"]];
  function showCtx(appEl) {
    const ctx = $(".ctx", scr);
    const sr = scr.getBoundingClientRect(), r = appEl.getBoundingClientRect();
    ctx.innerHTML = MENU.map(m => `<button>${g(m[0])}<span>${tr(m[1], m[2])}</span></button>`).join("");
    const below = r.bottom - sr.top + 8, w = sr.width * 0.62;
    const left = Math.min(Math.max(8, r.left - sr.left + r.width / 2 - w / 2), sr.width - w - 8);
    const top = below + 150 > sr.height ? r.top - sr.top - 8 - ctx.offsetHeight - 120 : below;
    ctx.style.left = left + "px"; ctx.style.top = Math.max(40, top) + "px"; ctx.style.width = w + "px";
    ctx.classList.add("on");
    appEl.classList.add("lifted");
    if (navigator.vibrate) try { navigator.vibrate(12); } catch (e) {}
  }
  function hideCtx() {
    const ctx = $(".ctx.on", scr); if (!ctx) return false;
    ctx.classList.remove("on"); $$(".lifted", scr).forEach(e => e.classList.remove("lifted")); return true;
  }

  let ptr = null, longT = 0, clockTaps = 0, clockT = 0;
  scr.addEventListener("pointerdown", e => {
    const app = e.target.closest("[data-app]");
    ptr = { x: e.clientX, y: e.clientY, t: performance.now(), app, moved: false, mode: null, cb: e.target.closest(".cb"), clock: e.target.closest(".wclock") };
    try { scr.setPointerCapture(e.pointerId); } catch (err) {}
    if (app && !$(".appwin.on", scr)) {
      app.classList.add("press");
      longT = setTimeout(() => { if (ptr && !ptr.moved) { app.classList.remove("press"); showCtx(app); ptr = null; } }, 460);
    }
  });
  scr.addEventListener("pointermove", e => {
    if (!ptr) return;
    const dx = e.clientX - ptr.x, dy = e.clientY - ptr.y;
    if (!ptr.moved && Math.hypot(dx, dy) > 9) {
      ptr.moved = true; clearTimeout(longT); ptr.app?.classList.remove("press");
      ptr.mode = Math.abs(dy) > Math.abs(dx) ? "v" : "h";
    }
    if (ptr.mode === "v" && !$(".appwin.on", scr)) {
      const h = scr.clientHeight * 0.5;
      const p = S.open ? 1 - Math.max(0, dy) / h : -Math.min(0, dy) / h;
      setDrawer(Math.max(0, Math.min(1, p)), false);
    }
  });
  const end = e => {
    clearTimeout(longT);
    if (!ptr) return;
    const p0 = ptr; ptr = null;
    p0.app?.classList.remove("press");
    const dx = e.clientX - p0.x, dy = e.clientY - p0.y;
    if (p0.mode === "v" && !$(".appwin.on", scr)) {
      const h = scr.clientHeight * 0.5;
      const p = S.open ? 1 - Math.max(0, dy) / h : -Math.min(0, dy) / h;
      S.open = p > 0.3;
      if (S.open && !S.hinted) { S.hinted = true; $(".hint", scr)?.remove(); }
      setDrawer(S.open ? 1 : 0);
      return;
    }
    if (p0.mode === "h" && S.open && Math.abs(dx) > 36) { setCat(S.cat + (dx < 0 ? 1 : -1)); return; }
    if (p0.moved || e.type === "pointercancel") return;
    if (hideCtx()) return;
    if (closeApp()) return;
    if (p0.cb) { setCat(+p0.cb.dataset.cat); return; }
    if (p0.clock) {
      const t = performance.now(); clockTaps = t - clockT < 600 ? clockTaps + 1 : 1; clockT = t;
      p0.clock.animate([{ scale: 1 }, { scale: 0.95 }, { scale: 1 }], { duration: 220 });
      if (clockTaps >= 5) { clockTaps = 0; nova(true); }
      return;
    }
    if (p0.app) openApp(p0.app);
  };
  scr.addEventListener("pointerup", end);
  scr.addEventListener("pointercancel", end);
  scr.addEventListener("contextmenu", e => e.preventDefault());

  setInterval(() => {
    const { t, date } = now();
    $$(".st,.wt").forEach(el => (el.textContent = t));
    $$(".wd").forEach(el => (el.innerHTML = `<i></i>${date}`));
  }, 15000);

  /* ---------- chips and gallery ---------- */
  const themeList = () => (document.documentElement.classList.contains("nova-unlocked") ? [...THEMES, NOVA] : THEMES);
  function chips() {
    $("#themeChips").innerHTML = themeList().map((t, i) => `<button class="chip t${t.secret ? " secret" : ""}" data-i="${i}"><i style="background-image:${wallCss(t)}"></i>${t.name}</button>`).join("");
    $("#styleChips").innerHTML = STYLES.map(s => `<button class="chip" data-s="${s[0]}">${tr(s[1], s[2])}</button>`).join("");
    $("#shapeChips").innerHTML = SHAPES.map(s => `<button class="chip shp" data-h="${s[0]}"><i class="sh-${s[0]}"></i>${s[1]}</button>`).join("");
    syncChips();
  }
  function syncChips() {
    $$("#themeChips .chip").forEach(b => b.classList.toggle("on", themeList()[+b.dataset.i] === S.theme));
    $$("#styleChips .chip").forEach(b => b.classList.toggle("on", b.dataset.s === S.style));
    $$("#shapeChips .chip").forEach(b => b.classList.toggle("on", b.dataset.h === S.shape));
  }
  $("#themeChips").addEventListener("click", e => { const b = e.target.closest(".chip"); if (b) applyTheme(themeList()[+b.dataset.i]); });
  $("#styleChips").addEventListener("click", e => { const b = e.target.closest(".chip"); if (b) { S.style = b.dataset.s; restyle(); syncChips(); } });
  $("#shapeChips").addEventListener("click", e => { const b = e.target.closest(".chip"); if (b) { S.shape = b.dataset.h; restyle(); syncChips(); } });

  function gallery() {
    $("#gallery").innerHTML = themeList().map((t, i) => `<button class="tcard reveal in" data-i="${i}">
      <div class="dev mini${t.light ? " light" : ""}${t.secret ? " nova" : ""}"><div class="scr">${screenHTML({ theme: t, style: t.style, shape: t.shape, cat: 0 }, true)}</div></div>
      <b>${t.name}</b><span>${tr(STYLES.find(s => s[0] === t.style)[1], STYLES.find(s => s[0] === t.style)[2])} · ${SHAPES.find(s => s[0] === t.shape)[1]}</span></button>`).join("");
  }
  $("#gallery").addEventListener("click", e => { const b = e.target.closest(".tcard"); if (b) applyTheme(themeList()[+b.dataset.i], true); });

  /* ---------- liquid glass lens that follows the finger ---------- */
  const LS = 88;
  const lens = document.createElement("div");
  lens.className = "lens"; lens.setAttribute("aria-hidden", "true");
  document.body.appendChild(lens);
  (function lensFilter() {
    const n = 96, c = document.createElement("canvas"); c.width = c.height = n;
    const x = c.getContext("2d"), d = x.createImageData(n, n);
    const sm = (a, b, v) => { const t = Math.min(1, Math.max(0, (v - a) / (b - a))); return t * t * (3 - 2 * t); };
    for (let j = 0; j < n; j++) for (let i = 0; i < n; i++) {
      const u = (i + 0.5) / n * 2 - 1, v = (j + 0.5) / n * 2 - 1, r = Math.hypot(u, v), k = (j * n + i) * 4;
      const s = r < 1 ? 0.28 * (1 - r * r) + 0.7 * sm(0.62, 1, r) : 0;
      d.data[k] = 128 - 127 * u * s; d.data[k + 1] = 128 - 127 * v * s; d.data[k + 2] = 128; d.data[k + 3] = 255;
    }
    x.putImageData(d, 0, 0);
    const ns = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(ns, "svg");
    svg.setAttribute("width", "0"); svg.setAttribute("height", "0"); svg.style.position = "absolute";
    svg.innerHTML = `<filter id="xelens" x="0" y="0" width="${LS}" height="${LS}" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
      <feImage href="${c.toDataURL()}" x="0" y="0" width="${LS}" height="${LS}" preserveAspectRatio="none" result="m"/>
      <feDisplacementMap in="SourceGraphic" in2="m" scale="34" xChannelSelector="R" yChannelSelector="G"/></filter>`;
    document.body.appendChild(svg);
    if (/Chrome\//.test(navigator.userAgent)) lens.classList.add("refract");
  })();
  let lensOn = true, lx = -999, ly = -999, tx = -999, ty = -999, vx = 0, vy = 0, shown = false, hideT = 0;
  function lensTo(x, y, show) {
    if (!lensOn) return;
    tx = x; ty = y;
    if (!shown && show) { lx = x; ly = y; shown = true; lens.classList.add("on"); }
    clearTimeout(hideT);
  }
  function lensHide(ms) { clearTimeout(hideT); hideT = setTimeout(() => { shown = false; lens.classList.remove("on"); }, ms); }
  (function loop() {
    if (shown) {
      const ax = (tx - lx) * 0.22, ay = (ty - ly) * 0.22;
      vx = vx * 0.55 + ax; vy = vy * 0.55 + ay; lx += vx; ly += vy;
      const sp = Math.min(Math.hypot(vx, vy) / 40, 0.32), a = Math.atan2(vy, vx);
      lens.style.transform = `translate(${lx - LS / 2}px,${ly - LS / 2}px) rotate(${a}rad) scale(${1 + sp},${1 - sp * 0.6}) rotate(${-a}rad)`;
    }
    requestAnimationFrame(loop);
  })();
  addEventListener("mousemove", e => lensTo(e.clientX, e.clientY, true), { passive: true });
  document.addEventListener("mouseleave", () => lensHide(0));
  addEventListener("touchstart", e => { const t = e.touches[0]; lensTo(t.clientX, t.clientY, true); }, { passive: true });
  addEventListener("touchmove", e => { const t = e.touches[0]; lensTo(t.clientX, t.clientY, true); }, { passive: true });
  addEventListener("touchend", () => lensHide(650), { passive: true });
  const lensBtn = $("#lensToggle");
  lensBtn?.addEventListener("click", () => {
    lensOn = !lensOn; lensBtn.classList.toggle("on", lensOn);
    if (!lensOn) lensHide(0);
  });

  /* ---------- tilt: glass shine follows how the phone is held ---------- */
  const root = document.documentElement.style;
  let gx = 0, gy = 0, sgx = 0, sgy = 0, gotTilt = false;
  const clamp = v => Math.max(-1, Math.min(1, v));
  addEventListener("deviceorientation", e => {
    if (e.gamma == null) return;
    gotTilt = true; gx = clamp(e.gamma / 28); gy = clamp((e.beta - 50) / 28);
  });
  addEventListener("mousemove", e => { if (!gotTilt) { gx = clamp(e.clientX / innerWidth * 2 - 1); gy = clamp(e.clientY / innerHeight * 2 - 1); } }, { passive: true });
  let asked = false;
  addEventListener("touchend", () => {
    if (asked || typeof DeviceOrientationEvent === "undefined" || typeof DeviceOrientationEvent.requestPermission !== "function") return;
    asked = true; DeviceOrientationEvent.requestPermission().catch(() => {});
  }, { passive: true });
  (function tilt() {
    sgx += (gx - sgx) * 0.08; sgy += (gy - sgy) * 0.08;
    if (!reduce) { root.setProperty("--gx", sgx.toFixed(3)); root.setProperty("--gy", sgy.toFixed(3)); }
    requestAnimationFrame(tilt);
  })();

  /* ---------- Supernova easter egg ---------- */
  let logoTaps = 0, logoT = 0;
  $(".brand").addEventListener("click", e => {
    e.preventDefault();
    const t = performance.now(); logoTaps = t - logoT < 650 ? logoTaps + 1 : 1; logoT = t;
    const img = $(".brand img"); img.animate([{ rotate: "0deg", scale: 1 }, { rotate: `${logoTaps * 24}deg`, scale: 1.15 }, { rotate: "0deg", scale: 1 }], { duration: 380, easing: "ease-out" });
    if (logoTaps >= 5) { logoTaps = 0; nova(!document.documentElement.classList.contains("nova-on")); }
  });
  function burst(x, y) {
    if (reduce) return;
    for (let i = 0; i < 26; i++) {
      const b = document.createElement("span"); b.className = "bub";
      const a = Math.random() * Math.PI * 2, d = 90 + Math.random() * 220, s = 10 + Math.random() * 34;
      b.style.cssText = `left:${x}px;top:${y}px;width:${s}px;height:${s}px;--dx:${Math.cos(a) * d}px;--dy:${Math.sin(a) * d}px;--h:${Math.random() * 360}deg;animation-delay:${Math.random() * 120}ms`;
      document.body.appendChild(b); setTimeout(() => b.remove(), 1700);
    }
  }
  function toast(msg) {
    let el = $(".toast"); if (!el) { el = document.createElement("div"); el.className = "toast glass"; document.body.appendChild(el); }
    el.textContent = msg; el.classList.add("on"); clearTimeout(el._t); el._t = setTimeout(() => el.classList.remove("on"), 3200);
  }
  function nova(on) {
    const h = document.documentElement;
    const first = !h.classList.contains("nova-unlocked");
    h.classList.toggle("nova-on", on);
    if (on) {
      h.classList.add("nova-unlocked");
      const r = $(".brand img").getBoundingClientRect(); burst(r.left + r.width / 2, r.top + r.height / 2);
      const d = dev.getBoundingClientRect(); if (d.top < innerHeight && d.bottom > 0) burst(d.left + d.width / 2, d.top + d.height * 0.25);
      $$(".app .ic", scr).forEach((el, i) => el.animate([{ rotate: "0deg" }, { rotate: "360deg" }], { duration: 700, delay: i * 40, easing: "cubic-bezier(.3,1.4,.5,1)" }));
      if (first) { chips(); gallery(); }
      toast(tr(first ? "เจอโหมดลับ Supernova! ปลดล็อกธีมลับแล้ว" : "Supernova เปิดแล้ว", first ? "You found Supernova! A secret theme is unlocked" : "Supernova on"));
      if (first) applyTheme(NOVA);
    } else toast(tr("ปิด Supernova แล้ว", "Supernova off"));
  }

  /* ---------- boot + language ---------- */
  function relabel() { paint(); chips(); gallery(); }
  relabel();
  addEventListener("xe-lang", relabel);
})();
