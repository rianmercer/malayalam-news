// Cache the app shell and channel logos for fast repeat launches. Never cache YouTube streams.
const CACHE = "mlnews-v11";
// Install only the small app shell. Channel art is cached as each visible image is requested,
// so a first-time install doesn't wait for every logo download before activating.
const SHELL = [
  "./", "./index.html", "./manifest.webmanifest",
  "./icon-192.png", "./malayalam-news-logo.webp"
];

self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key)))
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", event => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== "GET" || url.origin !== self.location.origin) return;

  const isAppAsset = request.mode === "navigate"
    || /\.(html|png|webp|webmanifest|wav)$/i.test(url.pathname);
  if (!isAppAsset) return;

  const refresh = caches.open(CACHE).then(cache =>
    fetch(request).then(response => {
      if (response && response.ok) cache.put(request, response.clone());
      return response;
    })
  );
  event.waitUntil(refresh.then(() => {}).catch(() => {}));

  event.respondWith(
    caches.match(request).then(cached => {
      if (cached) return cached;
      // The native shell uses ?app=1 to suppress website audio. Reuse the
      // cached app-shell document regardless of that query parameter.
      const isShellDocument = request.mode === "navigate"
        && (/\/$/.test(url.pathname) || /\/index\.html$/i.test(url.pathname));
      return (isShellDocument ? caches.match(request, { ignoreSearch: true }) : Promise.resolve(null))
        .then(shellCached => shellCached || refresh.catch(() => caches.match(request)));
    }).then(response =>
      response || new Response("Malayalam News is unavailable offline.", {
        status: 503,
        headers: { "Content-Type": "text/plain; charset=utf-8" }
      })
    )
  );
});
