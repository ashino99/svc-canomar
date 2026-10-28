/* Service worker de SVC-Canomar: permite abrir las pantallas principales sin señal (RF24). */
const CACHE = 'canomar-v2';
const BASE = self.registration.scope;
const ESTATICOS = ['css/app.css', 'js/app.js', 'js/vender.js', 'offline.html', 'manifest.webmanifest', 'img/icono-192.png', 'img/icono-512.png'];
const PAGINAS = ['vender', 'inicio', 'precios', 'saldo'];

self.addEventListener('install', e => {
    e.waitUntil(caches.open(CACHE).then(c => c.addAll(ESTATICOS.map(r => BASE + r))).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
    e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});

self.addEventListener('fetch', e => {
    const req = e.request;
    if (req.method !== 'GET' || !req.url.startsWith(BASE)) return;
    const ruta = req.url.substring(BASE.length).split('?')[0];
    if (req.mode === 'navigate') {
        // Páginas: primero la red; si no hay señal, la última copia guardada
        e.respondWith(fetch(req).then(r => {
            if (r.ok && PAGINAS.includes(ruta)) {
                const copia = r.clone();
                caches.open(CACHE).then(c => c.put(req, copia));
            }
            return r;
        }).catch(() => caches.match(req).then(r => r || caches.match(BASE + 'offline.html'))));
        return;
    }
    if (ESTATICOS.includes(ruta)) {
        e.respondWith(caches.match(req).then(r => r || fetch(req)));
    }
});
