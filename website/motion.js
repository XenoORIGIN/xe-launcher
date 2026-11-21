/* Xe Launcher website motion: glass-shard intro, cards that tilt with the finger or the phone, hero phones that fan out on scroll. */
(() => {
  const html = document.documentElement;
  const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
  const $ = (s, r = document) => r.querySelector(s);

  /* ---------- intro: glass shards gather into the Xe icon, which then settles into the header ---------- */
  const intro = $("#intro");
  function endIntro() {
    if (!html.classList.contains("intro")) return;
    try { sessionStorage.setItem("xe-intro", "1"); } catch (e) {}
    html.classList.remove("intro");
    html.classList.add("intro-out");
    setTimeout(() => { html.classList.remove("intro-out"); intro?.remove(); }, 700);
  }
  if (intro && html.classList.contains("intro")) {
    const N = 4, box = intro.querySelector(".ilogo"), field = intro.querySelector(".shards");
    const size = box.getBoundingClientRect().width, cell = size / N;
    const rnd = (a, b) => a + Math.random() * (b - a);
    const jag = () => `polygon(${rnd(0, 8)}% ${rnd(0, 8)}%,${rnd(92, 100)}% ${rnd(0, 10)}%,${rnd(90, 100)}% ${rnd(92, 100)}%,${rnd(0, 10)}% ${rnd(90, 100)}%)`;
    const shards = [];
    for (let j = 0; j < N; j++) for (let i = 0; i < N; i++) {
      const s = document.createElement("i");
      s.className = "shard";
      s.style.cssText = `width:${cell + 1}px;height:${cell + 1}px;left:${i * cell}px;top:${j * cell}px;clip-path:${jag()};--h:${rnd(180, 320)}deg`;
      field.appendChild(s);
      const a = Math.random() * Math.PI * 2, d = rnd(.55, 1) * Math.max(innerWidth, innerHeight) * .62;
      shards.push(s.animate([
        { transform: `translate(${Math.cos(a) * d}px,${Math.sin(a) * d}px) rotate(${rnd(-260, 260)}deg) scale(${rnd(.6, 1.6)})`, opacity: 0 },
        { opacity: 1, offset: .25 },
        { transform: "none", opacity: 1 },
      ], { duration: rnd(1050, 1350), delay: rnd(0, 380), easing: "cubic-bezier(.16,1,.3,1)", fill: "both" }));
    }
    const tl = [];
    // Once every shard has landed, the real icon appears over them with a flash of light.
    Promise.all(shards.map(a => a.finished)).then(() => {
      intro.classList.add("whole");
      tl.push(setTimeout(() => intro.classList.add("named"), 380));
      tl.push(setTimeout(() => {
        // Fly the icon to the header logo, then reveal the page.
        const to = $(".brand img").getBoundingClientRect(), from = box.getBoundingClientRect();
        const dx = to.left + to.width / 2 - (from.left + from.width / 2), dy = to.top + to.height / 2 - (from.top + from.height / 2);
        box.animate([{ transform: "none" }, { transform: `translate(${dx}px,${dy}px) scale(${to.width / from.width})` }],
          { duration: 620, easing: "cubic-bezier(.6,0,.2,1)", fill: "forwards" });
        endIntro();
      }, 1250));
    });
    intro.addEventListener("click", () => { tl.forEach(clearTimeout); shards.forEach(a => a.finish()); endIntro(); });
    setTimeout(endIntro, 5000); // never keep anyone waiting if an animation stalls
  } else intro?.remove();

  if (reduce) return;

  /* ---------- cards tilt toward the finger (or with the phone when nobody touches them) ---------- */
  const tiltables = document.querySelectorAll(".features .card, .rel, .install li, .bug, .qa details");
  tiltables.forEach(el => {
    el.classList.add("tilt");
    const move = e => {
      const r = el.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height;
      if (x < 0 || x > 1 || y < 0 || y > 1) return leave();
      el.classList.add("touching");
      el.style.setProperty("--try", `${(x - .5) * 14}deg`);
      el.style.setProperty("--trx", `${(.5 - y) * 12}deg`);
      el.style.setProperty("--mx", `${x * 100}%`);
      el.style.setProperty("--my", `${y * 100}%`);
    };
    const leave = () => { el.classList.remove("touching"); ["--try", "--trx", "--mx", "--my"].forEach(p => el.style.removeProperty(p)); };
    el.addEventListener("pointermove", move, { passive: true });
    el.addEventListener("pointerdown", move, { passive: true });
    ["pointerleave", "pointerup", "pointercancel"].forEach(t => el.addEventListener(t, leave, { passive: true }));
  });

  /* ---------- hero phones fan out and lean back as the page scrolls ---------- */
  let ticking = false;
  const hero = $(".phones");
  function onScroll() {
    if (ticking) return; ticking = true;
    requestAnimationFrame(() => {
      ticking = false;
      if (!hero) return;
      const r = hero.getBoundingClientRect();
      const p = Math.max(0, Math.min(1, (innerHeight - r.top) / (innerHeight + r.height) * 1.6 - .35));
      html.style.setProperty("--sp", p.toFixed(3));
    });
  }
  addEventListener("scroll", onScroll, { passive: true });
  onScroll();
})();
