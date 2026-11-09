// ideabox Vitrin: çevrimdışı çalışma.
// İnternet varken her zaman en yenisi gelir (ağ önce); yoksa saklanan kopya açılır.
const SAKLA = "vitrin-v4";
const ILK = ["./", "index.html", "manifest.webmanifest", "simgeler/vitrin-192.png", "simgeler/vitrin-64.png"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(SAKLA).then(c => c.addAll(ILK)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", e => {
  e.waitUntil(caches.keys()
    .then(adlar => Promise.all(adlar.filter(a => a !== SAKLA).map(a => caches.delete(a))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", e => {
  if (e.request.method !== "GET" || !e.request.url.startsWith(self.location.origin)) return;
  e.respondWith(
    fetch(e.request)
      .then(yanit => {
        if (yanit.ok) { const kopya = yanit.clone(); caches.open(SAKLA).then(c => c.put(e.request, kopya)); }
        return yanit;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }))
  );
});
