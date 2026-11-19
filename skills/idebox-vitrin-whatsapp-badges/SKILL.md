---
name: idebox-vitrin-whatsapp-badges
description: Add or modify manual WhatsApp unread-count badges, the per-app usage counter, smart sorting, and home-screen shortcuts on the idebox-vitrin static showcase page (app cards, total box, reset-all, home-screen icon badge, manifest shortcuts). Use when working on counters, badges, card order, or shortcuts in the idebox-vitrin repo (index.html + sw.js + manifest.webmanifest).
---

# idebox-vitrin WhatsApp badges

Static, server-less showcase page: `index.html` (data + CSS + JS in one file), `sw.js` (offline cache), `manifest.webmanifest`. UI language is Turkish. WhatsApp does not expose real unread counts to a web page, so the counters are **manual, per-device counters**, not real WhatsApp data.

## What exists (all in `index.html`)

| Feature | How it works |
|---|---|
| Card badge `.rozet` | Red round badge at the top-right corner of the app icon. Hidden at 0, shows `99+` above 99. Positioned on the card (not inside `.simge`, which clips with `overflow:hidden`). Uses `left:80px; transform:translateX(-100%)` so it never covers the app name. |
| Data field | Each entry in `UYGULAMALAR` has `whatsapp: N` (starting value; default 0). |
| Tap to change | Tap the icon (`.simge.sayac`) = +1; long press 500 ms = reset to 0 + vibrate. `stopPropagation` keeps the screenshot dialog from opening. |
| Persistence | `localStorage` key `vitrin-wa:<app name>`; try/catch around every access; falls back to the `whatsapp:` value. |
| Total box | `#say-wa` in the `.okunmamis` summary box, always visible (0 included), `999+` cap. `toplamYaz()` sums `el.dataset.wa` of all `.kart`. |
| Reset all | `#sifirla-hepsi` (disabled at 0) opens `#onay` confirm dialog (Cancel / "Evet, sıfırla"); each card exposes `el.sifirlaWa()`. |
| Home-screen icon badge | `toplamYaz()` calls `navigator.setAppBadge(total)` / `clearAppBadge()` only when the total changed (`simgeSon`); errors swallowed. |
| iPhone permission button | `#rozet-izin` ("Simge rozetini aç") shown only on iOS + `setAppBadge` supported + `Notification.permission === "default"`; click calls `Notification.requestPermission()` then re-sends the total. |

## Usage counter, smart sorting, home-screen shortcuts (added in cache v7)

| Feature | How it works |
|---|---|
| Usage counter | Every tap on a card body (not the icon, which calls `stopPropagation`) adds 1 to `localStorage` key `vitrin-kul:<app name>`; mirrored in `el.dataset.kul`. Silent, no UI. |
| Smart sorting | On page load only, `kullanim(u)` reads the counter; sort = usage desc, then `tarih` desc, then list order (stable sort). Cards never move while the user is tapping; the new order applies on the next open. |
| Shortcuts | `manifest.webmanifest` → `shortcuts`: Cafer Telefon, Vakit, Notlar with `url: ./?ac=<name>` (URL-encoded) and 256 px PNG icons. `telefon.svg` was rendered to `simgeler/telefon-256.png` because shortcut icons should be PNG. |
| Shortcut landing | `kisayol()` reads `?ac=`, finds the card by `h3` text, adds 1 to its usage counter, scrolls it to center after 300 ms, adds `.parla` (turquoise glow, `@keyframes parla`, 2 × 1 s), removes it after 2.1 s, then clears the query with `history.replaceState`. |
| Reset order (cache v8) | `#sira-sifirla` button at the page bottom above `<footer>` (turquoise outline, 56 px, full width up to 420 px). Disabled while every card has `dataset.kul == 0` (`siraDurum()`, called on load, card tap, shortcut, reset). Opens `#onay-sira` (same `.onay` styles, turquoise "Evet, sıfırla", note that WhatsApp counters stay). Yes = remove all `vitrin-kul:*` keys, set `dataset.kul = 0`, re-append cards per group by `dataset.sira` (default order index: date desc, list order), vibrate 40 ms. |
| Daily log (cache v9) | `localStorage` key `vitrin-gun` = `{"YYYY-MM-DD": {"<app>": n}}`, **local** date via `bugunTarih()`. `gunKaydet(ad)` is called from card taps and the shortcut handler (not WhatsApp icon taps); keeps only the last 7 days; calls `ozetYaz()`. |
| Daily summary card | `#gunluk` button below `.ozet`, above `#ara` (outside `.ozet` so `.ozet div` card styles do not apply). Shows "Bugün en çok: <b>App (n)</b>" + "Bugün toplam X dokunuş · Y uygulama", or "Bugün henüz bir uygulama açılmadı." when empty. Refreshes live, every 60 s and on `visibilitychange` so it resets after midnight. Tap = clear search/filter if the card is hidden, then `karteGit(kart)` (shared scroll + 2 s `.parla` glow, also used by shortcuts). "Sıralamayı sıfırla" also removes `vitrin-gun`. |
| 7-day mini chart (cache v10) | `#hafta` (`role="img"`, aria-label lists every day's total) inside `#gunluk`, under a divider. `haftaYaz()` runs at the start of `ozetYaz()`, so it refreshes with the summary (taps, shortcut, 60 s timer, `visibilitychange`, reset). One series: total taps per day from `vitrin-gun`, oldest left → today right; labels `Pz Pt Sa Ça Pe Cu Ct` by `getDay()`, today labelled "Bugün". Bars grow from the baseline, max 44 px, min 6 px, 4 px rounded top; value sits directly above each bar; zero days = 2 px `--ayrac` line with no number. Today = `--turkuaz`, other days = `color-mix(--turkuaz 38%)`. Today's column has `flex-grow: 1.45` so "Bugün" fits; under 360 px width the gap and label size shrink. |
| Export (cache v11) | `#disa-aktar` at the page bottom. `sayacVerisi()` = `{uygulama:"ideabox-vitrin", bicim:1, tarih:ISO, whatsapp:{ad:n}, kullanim:{ad:n}, gunluk}` (zeros omitted). On touch devices (`pointer: coarse`) with `navigator.canShare({files})` it opens the share sheet with a **`.txt`** file (Android Chrome refuses to share `.json`); `AbortError` = user cancelled, do nothing; any other error or no share support = download `vitrin-sayaclar-YYYY-MM-DD.json`. |
| Import | `#ice-aktar` opens hidden `#ice-dosya` (`.json,.txt`). `iceDenetle()` requires `uygulama === "ideabox-vitrin"`, keeps only app names in `UYGULAMALAR`, integers 1…1,000,000, `gunluk` dates within the last 7 days; files > 1 MB are rejected. `#onay-ice` shows file date + totals; error mode hides "Evet" and renames "Vazgeç" to "Tamam". Yes = **replace**: remove all `vitrin-wa:*` / `vitrin-kul:*`, write file values and `vitrin-gun`, then `location.reload()` so order, badges, summary and chart rebuild. |
| Limits | Apps have no URLs yet, so shortcuts open the showcase card, not the app. Manifest shortcuts work on Android only (not iOS); the page may need to be re-added to the home screen to pick them up. The shortcut list is static and does not follow usage. |

## Rules to keep

1. Bump `SAKLA` in `sw.js` (`vitrin-vN`) on every user-visible change, otherwise phones keep the old page.
2. Keep every `whatsapp:` value at `0` in the repo; test counts must never be committed.
3. Do not nest a `div` inside `.ozet > div` without resetting card styles (`.ozet div` is a card selector) — see `.ozet .okunmamis-bilgi`.
4. Mobile first: large text, 52 px+ tap targets, check 390 px width in light and dark themes.
5. Card taps, the shortcut handler and the reset must all read/write the usage count through `el.dataset.kul`; never keep a separate closure copy, or counts drift (a shortcut +1 was lost this way).
6. Render SVG icons to PNG by opening the SVG file itself with `page.goto('file://…svg')`; a `setContent` page cannot load local `file://` images and produces a blank PNG.
7. The icon badge only works for the page **installed to the home screen** (Android Chrome; iOS 16.4+ with notification permission). It cannot be verified in a headless browser — mock `navigator.setAppBadge` and tell the user to check on a real phone.

## Testing recipe (Playwright, local `python3 -m http.server`)

- Tap icons with `tap()`, reload, assert `#say-wa` text and visible `.rozet` count.
- Long press: `mouse.down()`, wait 700 ms, `mouse.up()`; scroll the target into view first (reload restores scroll, which can put the icon off-screen).
- Mock iOS via user agent + stubbed `Notification`; Android via Chrome user agent.
- After `reload()` the browser restores the old scroll position: wait, then `scrollTo(0,0)` before a top-of-page screenshot.
- Cards with screenshots (`ekran:`) open the `#perde` dialog on tap, which blocks further taps; use cards without screenshots for repeated-tap tests.
- Test the day change with `page.clock.install({ time: 23:58 })` then `clock.runFor(3 min)`.
- For card-sized screenshots use `locator('#gunluk').screenshot()`; a manual `clip` from `boundingBox()` can cut the card.
- Check text overflow automatically: any child with `scrollWidth > clientWidth + 1` is a fail; test at 320 px and 390 px.
- `navigator.share` cannot open in headless Chromium: stub `navigator.share`/`canShare` with `addInitScript` to test share, cancel (`AbortError`) and failure fallbacks; test import with `page.setInputFiles('#ice-dosya', path)`.
- Screenshot at 390×844, `deviceScaleFactor: 2`, both `colorScheme` values.

## Workflow used (user preferences)

Plan in 5 points → wait for approval → one step at a time, ask "Shall I continue?" after each step → backup branch before changes (`git branch yedek-<name>-oncesi`, local only) → commit + push to the designated `claude/...` branch → 3-question checklist (works? looks right on phone? anything missing?) → 3 next-step suggestions.

## Possible next steps

1. Add real app URLs so shortcuts and an "Aç" button open the apps directly.
2. Hide the mini chart while the whole week is empty.
3. Remind to export once a week (small note under the export button with the last export date).
