"""Website smoke check: download link, no horizontal overflow on a 360px phone, TH/EN toggle."""
import pathlib, sys
from playwright.sync_api import sync_playwright

STABLE = "https://github.com/XenoORIGIN/xe-launcher/releases/latest/download/xe-launcher.apk"
page_url = (pathlib.Path(__file__).parent / "index.html").resolve().as_uri()
ok = True
def check(cond, msg):
    global ok
    print(("PASS " if cond else "FAIL ") + msg); ok = ok and cond

with sync_playwright() as p:
    b = p.chromium.launch(executable_path="/usr/bin/google-chrome", args=["--no-sandbox"])
    pg = b.new_page(viewport={"width": 360, "height": 780}, device_scale_factor=2)
    pg.route("https://api.github.com/**", lambda r: r.fulfill(status=200, content_type="application/json", body='{"tag_name":"v1.0.7"}'))
    pg.goto(page_url); pg.wait_for_timeout(600)
    check(pg.get_attribute("#download", "href") == STABLE, "download button points at the stable APK URL")
    check(pg.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "no horizontal overflow at 360px")
    check("1.0.7" in (pg.text_content("#version") or ""), "version label filled from GitHub")
    th = pg.text_content("h1")
    pg.click("#lang"); pg.wait_for_timeout(200)
    en = pg.text_content("h1")
    check(th != en and en.strip() != "", "language toggle swaps the headline")
    if len(sys.argv) > 1:
        pg.click("#lang"); pg.wait_for_timeout(200)
        pg.set_viewport_size({"width": 390, "height": 844}); pg.wait_for_timeout(300)
        pg.screenshot(path=sys.argv[1], full_page=True)
    b.close()
sys.exit(0 if ok else 1)
