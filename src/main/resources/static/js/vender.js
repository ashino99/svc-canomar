/* P10 Vender: líneas por producto, importe automático (RF19), crédito o contado (RF17, RF18),
 * y guardado en el celular cuando no hay señal (RF24). */
(function () {
    'use strict';
    var form = document.getElementById('form-venta');
    if (!form) return;
    var lineas = document.getElementById('lineas');
    var tpl = document.getElementById('tpl-linea');
    var totalEl = document.getElementById('total');

    function uuid() {
        if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    }
    function nuevoUuid() { document.getElementById('uuid').value = uuid(); }

    function redondear(n) { return Math.round(n * 100) / 100; }

    function recalcular() {
        var total = 0;
        lineas.querySelectorAll('.linea-venta').forEach(function (l) {
            var kg = parseFloat(l.querySelector('[name=kilos]').value) || 0;
            var pr = parseFloat(l.querySelector('[name=precio]').value) || 0;
            var imp = redondear(redondear(kg) * pr);
            total += imp;
            l.querySelector('.importe').textContent = kg.toFixed(2) + ' kg × S/ ' + pr.toFixed(2) + ' = S/ ' + imp.toFixed(2);
        });
        totalEl.textContent = 'S/ ' + total.toFixed(2);
    }

    function agregar(btn) {
        var n = tpl.content.firstElementChild.cloneNode(true);
        n.querySelector('.nombre').textContent = btn.getAttribute('data-nombre');
        n.querySelector('[name=idProducto]').value = btn.getAttribute('data-id');
        var precio = n.querySelector('[name=precio]');
        precio.value = btn.getAttribute('data-precio');
        if (form.getAttribute('data-remate') === 'true') {
            precio.value = '';
            precio.placeholder = 'Remate S/';
        }
        var kilos = n.querySelector('[name=kilos]');
        var disp = parseFloat(btn.getAttribute('data-kilos')) || 0;
        kilos.addEventListener('input', function () {
            kilos.style.borderColor = (parseFloat(kilos.value) > disp) ? '#D67814' : '';
            recalcular();
        });
        precio.addEventListener('input', recalcular);
        n.querySelector('.quitar').addEventListener('click', function () { n.remove(); recalcular(); });
        lineas.appendChild(n);
        kilos.focus();
        recalcular();
    }

    document.querySelectorAll('#productos button').forEach(function (b) {
        b.addEventListener('click', function () { agregar(b); });
    });

    // Público (contado) o restaurante (crédito)
    var bloqueMedio = document.getElementById('bloque-medio');
    var bloqueCliente = document.getElementById('bloque-cliente');
    form.querySelectorAll('input[name=tipo][type=radio]').forEach(function (r) {
        r.addEventListener('change', function () {
            var credito = form.querySelector('input[name=tipo]:checked').value === 'CREDITO';
            bloqueMedio.classList.toggle('oculto', credito);
            bloqueCliente.classList.toggle('oculto', !credito);
            document.getElementById('cliente').required = credito;
        });
    });

    function tipoActual() {
        var r = form.querySelector('input[name=tipo]:checked') || form.querySelector('input[name=tipo]');
        return r ? r.value : 'CONTADO';
    }
    function ahoraLocal() {
        var d = new Date();
        var p = function (n) { return (n < 10 ? '0' : '') + n; };
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + 'T' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
    }

    form.addEventListener('submit', function (e) {
        if (!lineas.querySelector('.linea-venta')) {
            e.preventDefault();
            window.Canomar.aviso('error', 'Agregue al menos un producto');
            return;
        }
        if (navigator.onLine) return; // envío normal al servidor
        // RF24: sin señal, se guarda en el celular con su identificador único y la hora real
        e.preventDefault();
        var items = [];
        lineas.querySelectorAll('.linea-venta').forEach(function (l) {
            items.push({
                idProducto: parseInt(l.querySelector('[name=idProducto]').value, 10),
                kilos: parseFloat(l.querySelector('[name=kilos]').value),
                precio: parseFloat(l.querySelector('[name=precio]').value)
            });
        });
        var tipo = tipoActual();
        var cliente = document.getElementById('cliente');
        var venta = {
            uuid: document.getElementById('uuid').value || uuid(),
            fechaHora: ahoraLocal(),
            tipo: tipo,
            medio: tipo === 'CONTADO' ? form.querySelector('[name=medio]').value : null,
            idCliente: tipo === 'CREDITO' && cliente.value ? parseInt(cliente.value, 10) : null,
            remate: form.getAttribute('data-remate') === 'true',
            items: items
        };
        var totalTxt = totalEl.textContent;
        window.Canomar.encolar(venta);
        lineas.innerHTML = '';
        recalcular();
        nuevoUuid();
        window.Canomar.aviso('aviso', 'Sin conexión · venta guardada en el celular (' + totalTxt + '). Se enviará al volver la señal.');
    });

    nuevoUuid();
})();
