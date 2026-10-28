/* SVC-Canomar · funciones comunes del navegador del celular.
 * - Cola de ventas sin conexión y sincronización (RF24, RF25, RF26)
 * - Confirmaciones, buscador, totales en vivo y compartir el estado de cuenta (RF37)
 */
(function () {
    'use strict';
    var CLAVE = 'canomar.pendientes';

    function leer() {
        try { return JSON.parse(localStorage.getItem(CLAVE) || '[]'); } catch (e) { return []; }
    }
    function guardar(lista) {
        try { localStorage.setItem(CLAVE, JSON.stringify(lista)); } catch (e) { /* almacenamiento lleno */ }
        pintarChip();
    }
    function csrf() {
        var t = document.querySelector('meta[name="_csrf"]');
        var h = document.querySelector('meta[name="_csrf_header"]');
        return t && h ? { header: h.content, token: t.content } : null;
    }
    function base() {
        var link = document.querySelector('link[rel="manifest"]');
        return link ? link.getAttribute('href').replace(/manifest\.webmanifest$/, '') : '/';
    }

    /* ---------- RF26: indicador de sincronización ---------- */
    function pintarChip() {
        var chip = document.getElementById('chip-sync');
        if (!chip) return;
        var n = leer().length;
        chip.className = 'chip';
        if (!navigator.onLine) {
            chip.classList.add('chip-off');
            chip.textContent = n ? 'Sin señal · ' + n : 'Sin señal';
        } else if (n) {
            chip.classList.add('chip-pend');
            chip.textContent = n + (n === 1 ? ' pendiente' : ' pendientes');
        } else {
            chip.classList.add('chip-ok');
            chip.textContent = 'Enviado';
        }
        var aviso = document.getElementById('aviso-offline');
        if (aviso) aviso.classList.toggle('oculto', navigator.onLine);
        var cuenta = document.getElementById('aviso-pendientes');
        if (cuenta) {
            cuenta.className = 'alerta ' + (n ? 'alerta-aviso' : 'alerta-ok');
            cuenta.textContent = n ? 'Tiene ' + n + (n === 1 ? ' venta sin enviar' : ' ventas sin enviar') + '. Sincronice antes de salir' : 'Ventas sin enviar: 0 · puede cerrar sesión con seguridad';
        }
    }

    /* ---------- RF25: envía la cola al servidor ---------- */
    var enviando = false;
    function sincronizar(manual) {
        var lista = leer();
        if (enviando || !lista.length || !navigator.onLine) { pintarChip(); return; }
        var c = csrf();
        if (!c) return;
        enviando = true;
        var cab = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        cab[c.header] = c.token;
        fetch(base() + 'api/ventas/sync', { method: 'POST', headers: cab, body: JSON.stringify(lista), credentials: 'same-origin' })
            .then(function (r) {
                var tipo = r.headers.get('Content-Type') || '';
                if (!r.ok || tipo.indexOf('json') < 0) throw new Error('sesion');
                return r.json();
            })
            .then(function (res) {
                var resueltos = {};
                var rechazos = [];
                res.forEach(function (x) {
                    resueltos[x.uuid] = true;
                    if (x.estado === 'RECHAZADA') rechazos.push(x.mensaje);
                });
                guardar(leer().filter(function (v) { return !resueltos[v.uuid]; }));
                var ok = res.filter(function (x) { return x.estado === 'SINCRONIZADA'; }).length;
                if (ok) aviso('ok', ok + (ok === 1 ? ' venta sincronizada' : ' ventas sincronizadas'));
                if (rechazos.length) aviso('error', 'Ventas rechazadas por el servidor: ' + rechazos.join(' · '));
            })
            .catch(function () {
                if (manual) aviso('aviso', 'No se pudo enviar. Revise la señal o vuelva a iniciar sesión; se reintentará.');
            })
            .finally(function () { enviando = false; pintarChip(); });
    }

    function aviso(tipo, texto) {
        var main = document.querySelector('main');
        if (!main) return;
        var d = document.createElement('div');
        d.className = 'alerta alerta-' + tipo;
        d.textContent = texto;
        main.insertBefore(d, main.firstChild);
    }

    window.Canomar = {
        encolar: function (venta) { var l = leer(); l.push(venta); guardar(l); },
        pendientes: function () { return leer().length; },
        sincronizar: sincronizar,
        aviso: aviso
    };

    window.addEventListener('online', function () { pintarChip(); sincronizar(false); });
    window.addEventListener('offline', pintarChip);
    document.addEventListener('DOMContentLoaded', function () {
        pintarChip();
        sincronizar(false);
        var chip = document.getElementById('chip-sync');
        if (chip) chip.addEventListener('click', function () { sincronizar(true); });

        // Confirmación antes de acciones delicadas (desactivar, anular)
        document.querySelectorAll('form[data-confirmar]').forEach(function (f) {
            f.addEventListener('submit', function (e) { if (!confirm(f.getAttribute('data-confirmar'))) e.preventDefault(); });
        });

        // Cerrar sesión: avisa si quedan ventas sin enviar (RF03)
        var salir = document.getElementById('form-salir');
        if (salir) salir.addEventListener('submit', function (e) {
            var n = leer().length;
            if (n && !confirm('Tiene ' + n + (n === 1 ? ' venta sin enviar' : ' ventas sin enviar') + '. Si cierra sesión no se perderán, pero no se enviarán hasta que vuelva a ingresar. ¿Cerrar sesión?')) e.preventDefault();
        });
        // Cuadre: no cuadrar con ventas pendientes (RF39)
        var cuadre = document.getElementById('form-cuadre');
        if (cuadre) cuadre.addEventListener('submit', function (e) {
            var n = leer().length;
            if (n) { e.preventDefault(); aviso('error', 'Sincronice ' + (n === 1 ? 'la venta pendiente' : 'las ' + n + ' ventas pendientes') + ' antes de cuadrar (RF39).'); window.scrollTo(0, 0); }
        });

        // Buscador de listas
        document.querySelectorAll('input[data-filtra]').forEach(function (inp) {
            inp.addEventListener('input', function () {
                var q = inp.value.toLowerCase();
                document.querySelectorAll(inp.getAttribute('data-filtra')).forEach(function (el) {
                    el.style.display = el.textContent.toLowerCase().indexOf(q) >= 0 ? '' : 'none';
                });
            });
        });

        // Precios: advierte si un precio queda por debajo del costo (RF15)
        document.querySelectorAll('.precio-dia').forEach(function (inp) {
            var revisar = function () {
                var costo = parseFloat(inp.getAttribute('data-costo'));
                var v = parseFloat(inp.value);
                inp.style.borderColor = (!isNaN(costo) && !isNaN(v) && v < costo) ? '#C42828' : '';
            };
            inp.addEventListener('input', revisar);
            revisar();
        });

        // Nueva compra: líneas dinámicas y total (RF11)
        var tplC = document.getElementById('tpl-compra');
        if (tplC) {
            var cont = document.getElementById('lineas-compra');
            var totalC = function () {
                var kg = 0, s = 0;
                cont.querySelectorAll('.linea-venta').forEach(function (l) {
                    kg += parseFloat(l.querySelector('[name=kilos]').value) || 0;
                    s += parseFloat(l.querySelector('[name=costo]').value) || 0;
                });
                document.getElementById('total-compra').textContent = kg.toFixed(2) + ' kg · S/ ' + s.toFixed(2);
            };
            var agregar = function () {
                var n = tplC.content.firstElementChild.cloneNode(true);
                n.querySelector('.quitar').addEventListener('click', function () { n.remove(); totalC(); });
                n.querySelectorAll('input').forEach(function (i) { i.addEventListener('input', totalC); });
                cont.appendChild(n);
            };
            document.getElementById('agregar-compra').addEventListener('click', agregar);
            agregar();
        }

        // Cierre del puesto: diferencia en vivo (RF27)
        var reales = document.querySelectorAll('.kilos-real');
        if (reales.length) {
            var totalCierre = function () {
                var t = 0;
                reales.forEach(function (inp) {
                    var esp = parseFloat(inp.getAttribute('data-esperado')) || 0;
                    var v = parseFloat(inp.value);
                    var dif = inp.closest('.tarjeta').querySelector('.dif');
                    if (isNaN(v)) { dif.textContent = ''; return; }
                    t += v;
                    var pct = esp > 0 ? Math.abs(v - esp) * 100 / esp : (v > 0 ? 100 : 0);
                    dif.textContent = 'Diferencia ' + (v - esp).toFixed(2) + ' kg (' + pct.toFixed(1) + '%)';
                    dif.className = 't2 dif ' + (pct > 5 ? 'rojo' : 'verde');
                });
                document.getElementById('total-cierre').textContent = t.toFixed(2) + ' kg';
            };
            reales.forEach(function (i) { i.addEventListener('input', totalCierre); });
        }

        // Factura: suma de ventas seleccionadas (RF43)
        var checks = document.querySelectorAll('.venta-factura');
        if (checks.length) {
            var suma = function () {
                var s = 0;
                checks.forEach(function (c) { if (c.checked) s += parseFloat(c.getAttribute('data-total')) || 0; });
                document.getElementById('suma-factura').textContent = 'S/ ' + s.toFixed(2);
            };
            checks.forEach(function (c) { c.addEventListener('change', suma); });
        }

        // Compartir estado de cuenta desde el celular (RF37)
        var comp = document.getElementById('compartir-estado');
        if (comp) comp.addEventListener('click', function () {
            var url = comp.getAttribute('data-url');
            fetch(url, { credentials: 'same-origin' }).then(function (r) { return r.blob(); }).then(function (b) {
                var archivo = new File([b], comp.getAttribute('data-nombre'), { type: 'application/pdf' });
                if (navigator.canShare && navigator.canShare({ files: [archivo] })) {
                    return navigator.share({ files: [archivo], title: 'Estado de cuenta' });
                }
                window.open(url, '_blank');
                aviso('aviso', 'Este navegador no permite compartir: se abrió el PDF para descargarlo.');
            }).catch(function () { aviso('error', 'No se pudo generar el PDF'); });
        });
    });

    // PWA: instalable y con modo sin conexión (requiere HTTPS o localhost)
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', function () {
            navigator.serviceWorker.register(base() + 'sw.js').catch(function () { /* sin HTTPS no se registra */ });
        });
    }
})();
