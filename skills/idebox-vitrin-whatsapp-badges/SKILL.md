---
name: idebox-vitrin-whatsapp-badges
description: Add or modify manual WhatsApp unread-count badges on the idebox-vitrin static showcase page (app cards, total box, reset-all, home-screen icon badge). Use when working on notification counters/badges in the idebox-vitrin repo (index.html + sw.js).
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

## Rules to keep

1. Bump `SAKLA` in `sw.js` (`vitrin-vN`) on every user-visible change, otherwise phones keep the old page.
2. Keep every `whatsapp:` value at `0` in the repo; test counts must never be committed.
3. Do not nest a `div` inside `.ozet > div` without resetting card styles (`.ozet div` is a card selector) — see `.ozet .okunmamis-bilgi`.
4. Mobile first: large text, 52 px+ tap targets, check 390 px width in light and dark themes.
5. The icon badge only works for the page **installed to the home screen** (Android Chrome; iOS 16.4+ with notification permission). It cannot be verified in a headless browser — mock `navigator.setAppBadge` and tell the user to check on a real phone.

## Testing recipe (Playwright, local `python3 -m http.server`)

- Tap icons with `tap()`, reload, assert `#say-wa` text and visible `.rozet` count.
- Long press: `mouse.down()`, wait 700 ms, `mouse.up()`; scroll the target into view first (reload restores scroll, which can put the icon off-screen).
- Mock iOS via user agent + stubbed `Notification`; Android via Chrome user agent.
- Screenshot at 390×844, `deviceScaleFactor: 2`, both `colorScheme` values.

## Workflow used (user preferences)

Plan in 5 points → wait for approval → one step at a time, ask "Shall I continue?" after each step → backup branch before changes (`git branch yedek-<name>-oncesi`, local only) → commit + push to the designated `claude/...` branch → 3-question checklist (works? looks right on phone? anything missing?) → 3 next-step suggestions.

## Possible next steps

1. Fit the three summary boxes on one row on phones.
2. Export/import counters as a file to move them between devices.
3. Total box tap opens WhatsApp.
