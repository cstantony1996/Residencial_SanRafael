<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  // Detecta rol desde la sesión (cubrimos ambos nombres de atributo usados en el proyecto)
  String rol = (String) session.getAttribute("usuarioRol");
  if (rol == null) rol = (String) session.getAttribute("rol");

  boolean esGuardia = false;
  if (rol != null) {
      String r = rol.trim();
      esGuardia = "GUARDIA".equalsIgnoreCase(r)
               || "SEGURIDAD".equalsIgnoreCase(r)
               || "AGENTE".equalsIgnoreCase(r)
               || "Agente de seguridad de residencial".equalsIgnoreCase(r);
  }

  if (!esGuardia) {
      // Saca a quien no sea guardia/agente
      response.sendRedirect(request.getContextPath() + "/index.jsp");
      return;
  }
%>


<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <title>Control de Acceso - Garita</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/tabla-usuarios.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/garita.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">

        <script src="https://unpkg.com/html5-qrcode" defer></script>

        <!-- FIX defensivo por si hay caché de CSS: fuerza layout del header -->
        <style>
            .hero{display:flex;align-items:center;gap:16px;flex-wrap:nowrap}
            .hero-actions{margin-left:auto;display:flex;gap:8px;align-items:center}
        </style>
    </head>
    <body>

        <div class="garita-container">
            <!-- Header tipo “hero” -->
            <section class="hero">
                <div class="hero-left">
                    <div class="hero-badge"><i class="fa-solid fa-qrcode"></i></div>
                    <div>
                        <h1 class="title">Control de Acceso</h1>
                        <p class="subtitle"><i class="fa-solid fa-lock"></i> Usa HTTPS o localhost para habilitar la cámara</p>
                    </div>
                </div>
                <div class="hero-actions">
                    <a class="btn btn-back" href="${pageContext.request.contextPath}/vistas/menuGuardia.jsp">
                        <i class="fa-solid fa-arrow-left-long"></i> Regresar
                    </a>
                </div>
            </section>

            <!-- Controles -->
            <section class="panel">
                <div class="row">
                    <div class="field">
                        <label for="tipo"><i class="fa-solid fa-person-walking"></i> Tipo de acceso</label>
                        <select id="tipo">
                            <option value="peaton">Peatón</option>
                            <option value="vehiculo">Vehículo</option>
                        </select>
                    </div>

                    <div class="field">
                        <label for="punto"><i class="fa-solid fa-torii-gate"></i> Punto de control</label>
                        <select id="punto">
                            <c:choose>
                                <c:when test="${not empty puntos}">
                                    <c:forEach var="p" items="${puntos}">
                                        <option value="${p.id}">${p.nombre} (${p.tipo})</option>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <option value="1">Acceso Peatonal Norte (peatonal)</option>
                                    <option value="2">Garita Principal (vehicular)</option>
                                </c:otherwise>
                            </c:choose>
                        </select>
                    </div>

                    <div class="field">
                        <label for="camera-select"><i class="fa-solid fa-camera"></i> Cámara</label>
                        <select id="camera-select">
                            <option value="">Cargando cámaras…</option>
                        </select>
                    </div>
                </div>

                <div class="row controls-row">
                    <div class="buttons">
                        <button class="btn btn-primary" id="btn-start"><i class="fa-solid fa-play"></i> Iniciar cámara</button>
                        <button class="btn" id="btn-stop" disabled><i class="fa-solid fa-stop"></i> Detener</button>
                        <button class="btn" id="btn-clear"><i class="fa-solid fa-eraser"></i> Limpiar</button>
                    </div>

                    <div class="segmented" role="group" aria-label="Dirección">
                        <button type="button" id="btn-entrada" class="seg active"><i class="fa-solid fa-right-to-bracket"></i> Entrada</button>
                        <button type="button" id="btn-salida"  class="seg"><i class="fa-solid fa-right-from-bracket"></i> Salida</button>
                    </div>

                    <label class="switch">
                        <input type="checkbox" id="chk-beep" checked>
                        <span class="slider"></span>
                        <span class="switch-label">Beep al permitir</span>
                    </label>
                </div>
            </section>

            <!-- Scanner + aside -->
            <section class="scanner-wrap">
                <!-- Solo cámara (sin overlay ni marco) -->
                <div id="qr-reader" class="qr-box"></div>

                <aside class="side-card">
                    <div class="fallback">
                        <div class="muted" style="margin-bottom:6px"><i class="fa-solid fa-keyboard"></i> Fallback lector USB (pegar texto del QR y Enter):</div>
                        <input id="usb-input" class="usb" type="text" placeholder="Pegue aquí el contenido leído por el lector">
                    </div>

                    <div class="result">
                        <div id="result-banner" class="banner">
                            <div class="banner-title" id="result-title">Esperando lectura…</div>
                            <div id="result-desc" class="muted"></div>
                        </div>

                        <details class="raw">
                            <summary><i class="fa-regular fa-eye"></i> Ver contenido del QR leído</summary>
                            <pre id="raw-text" class="raw-pre"></pre>
                        </details>
                    </div>
                </aside>
            </section>
        </div>

        <!-- Sonidos -->
        <audio id="beep" preload="auto">
            <source src="${pageContext.request.contextPath}/sounds/correcto.mp3" type="audio/mpeg">
        </audio>
        <audio id="beep-error" preload="auto">
            <source src="${pageContext.request.contextPath}/sounds/error.mp3" type="audio/mpeg">
        </audio>

        <script>
            (function () {
                var $ = function (sel) {
                    return document.querySelector(sel);
                };

                var btnStart = $('#btn-start');
                var btnStop = $('#btn-stop');
                var btnClear = $('#btn-clear');
                var tipoSel = $('#tipo');
                var puntoSel = $('#punto');
                var camSel = $('#camera-select');

                var usbInp = $('#usb-input');
                var banner = $('#result-banner');
                var titleEl = $('#result-title');
                var descEl = $('#result-desc');
                var rawEl = $('#raw-text');

                var beepEl = $('#beep');
                var beepErrorEl = $('#beep-error');
                var chkBeep = $('#chk-beep');
                var qrBoxEl = document.getElementById('qr-reader');

                // Dirección
                var direccion = 'entrada';
                var btnEntrada = $('#btn-entrada');
                var btnSalida = $('#btn-salida');

                btnEntrada.addEventListener('click', function () {
                    direccion = 'entrada';
                    btnEntrada.classList.add('active');
                    btnSalida.classList.remove('active');
                });
                btnSalida.addEventListener('click', function () {
                    direccion = 'salida';
                    btnSalida.classList.add('active');
                    btnEntrada.classList.remove('active');
                });

                // Estado escáner
                var scanner = null, scanning = false, busy = false, lastText = null, lastAt = 0, cameras = [];

                function setBanner(status, title, desc) {
                    banner.className = 'banner ' + (status || '');
                    titleEl.textContent = title || '';
                    descEl.textContent = desc || '';
                }
                function successFX() {
                    if (chkBeep.checked) {
                        try {
                            beepEl.currentTime = 0;
                            beepEl.play();
                        } catch (_) {
                        }
                    }
                }
                function errorFX() {
                    try {
                        beepErrorEl.currentTime = 0;
                        beepErrorEl.play();
                    } catch (_) {
                    }
                }
                function isMobile() {
                    return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
                }

                function seleccionarPuntoPorTipo() {
                    var opts = Array.prototype.slice.call(puntoSel.options || []);
                    var needle = (tipoSel.value === 'vehiculo') ? 'vehicular' : 'peatonal';
                    var found = null;
                    for (var i = 0; i < opts.length; i++) {
                        var t = (opts[i].textContent || opts[i].innerText || '').toLowerCase();
                        if (t.indexOf(needle) !== -1) {
                            found = opts[i];
                            break;
                        }
                    }
                    if (found) {
                        puntoSel.value = found.value;
                        puntoSel.disabled = true;
                    } else {
                        puntoSel.disabled = false;
                    }
                }
                function onTipoChange() {
                    seleccionarPuntoPorTipo();
                }
                onTipoChange();
                tipoSel.addEventListener('change', onTipoChange);

                // Cargar cámaras
                async function loadCameras() {
                    if (!window.Html5Qrcode)
                        return;
                    try {
                        cameras = await Html5Qrcode.getCameras();
                        camSel.innerHTML = '';
                        if (!cameras || cameras.length === 0) {
                            camSel.innerHTML = '<option value="">No hay cámaras disponibles</option>';
                            camSel.disabled = true;
                            return;
                        }
                        var preferredId = cameras[0].id;
                        var back = cameras.find(function (c) {
                            return /back|rear|environment/i.test(c.label || '');
                        });
                        if (isMobile() && back)
                            preferredId = back.id;

                        cameras.forEach(function (c, i) {
                            var opt = document.createElement('option');
                            opt.value = c.id;
                            opt.textContent = c.label || ('Cámara ' + (i + 1));
                            camSel.appendChild(opt);
                        });
                        camSel.value = preferredId;
                        camSel.disabled = false;
                    } catch (e) {
                        console.error(e);
                        camSel.innerHTML = '<option value="">Error listando cámaras</option>';
                        camSel.disabled = true;
                    }
                }
                document.addEventListener('DOMContentLoaded', loadCameras);

                camSel.addEventListener('change', async function () {
                    if (!scanning || !scanner)
                        return;
                    try {
                        await scanner.stop();
                        await scanner.start(
                                camSel.value || {facingMode: "environment"},
                                {fps: 10}, // sin qrbox
                                onDecoded, onScanFailure
                                );
                        setBanner('', 'Escaneando…', 'Cámara cambiada');
                    } catch (e) {
                        console.error(e);
                        setBanner('warn', 'No se pudo cambiar de cámara', (e && e.message) || '');
                    }
                });

                // Iniciar
                btnStart.addEventListener('click', async function () {
                    if (!window.Html5Qrcode) {
                        alert('Librería html5-qrcode no cargó.');
                        return;
                    }
                    try {
                        if (!scanner)
                            scanner = new Html5Qrcode("qr-reader");
                        if (!cameras || cameras.length === 0)
                            await loadCameras();

                        var deviceOrConfig = camSel.value ? camSel.value : {facingMode: "environment"};
                        await scanner.start(deviceOrConfig, {fps: 10}, onDecoded, onScanFailure); // sin qrbox
                        scanning = true;
                        btnStart.disabled = true;
                        btnStop.disabled = false;
                        qrBoxEl.classList.add('is-scanning');
                        setBanner('', 'Escaneando…', 'Apunta el QR a la cámara');
                    } catch (e) {
                        console.error(e);
                        setBanner('warn', 'No se pudo iniciar la cámara', (e && e.message) || 'Verifica permisos/HTTPS');
                    }
                });

                // Detener
                btnStop.addEventListener('click', async function () {
                    try {
                        if (scanner && scanning)
                            await scanner.stop();
                        scanning = false;
                        btnStart.disabled = false;
                        btnStop.disabled = true;
                        qrBoxEl.classList.remove('is-scanning');
                        setBanner('', 'Cámara detenida', 'Pulsa “Iniciar cámara” para reintentar');
                    } catch (e) {
                        console.error(e);
                    }
                });

                // Limpiar
                btnClear.addEventListener('click', function () {
                    rawEl.textContent = '';
                    lastText = null;
                    lastAt = 0;
                    setBanner('', 'Esperando lectura…', '');
                });

                function onScanFailure(_) {}

                // === Proceso al decodificar ===
                async function onDecoded(text) {
                    var now = Date.now();
                    if (text === lastText && (now - lastAt) < 1500)
                        return;
                    lastText = text;
                    lastAt = now;
                    rawEl.textContent = text;

                    // Flash verde visual
                    qrBoxEl.classList.add('hit');
                    setTimeout(function () {
                        qrBoxEl.classList.remove('hit');
                    }, 650);

                    if (busy)
                        return;
                    busy = true;
                    try {
                        var opt = puntoSel.options[puntoSel.selectedIndex];
                        var optText = (opt && (opt.textContent || opt.innerText) || '').toLowerCase();
                        if (tipoSel.value === 'peaton' && optText.indexOf('peatonal') === -1) {
                            setBanner('warn', 'Punto/tipo no coinciden', 'Ajustando a punto peatonal…');
                            seleccionarPuntoPorTipo();
                        }
                        if (tipoSel.value === 'vehiculo' && optText.indexOf('vehicular') === -1) {
                            setBanner('warn', 'Punto/tipo no coinciden', 'Ajustando a punto vehicular…');
                            seleccionarPuntoPorTipo();
                        }

                        var payload = buildPayload(text);
                        if (!payload) {
                            setBanner('error', 'QR no reconocido', 'Formato no válido.');
                            busy = false;
                            return;
                        }

                        var fd = new URLSearchParams();
                        fd.set('modo', payload.modo);
                        fd.set('tipo', tipoSel.value);
                        fd.set('puntoId', puntoSel.value);
                        fd.set('direccion', direccion);
                        fd.set('datosQR', text);

                        if (payload.modo === 'token') {
                            fd.set('tk', payload.tk);
                        } else {
                            fd.set('correo', payload.correo || '');
                            fd.set('lote', payload.lote || '');
                            if (payload.numeroCasa != null)
                                fd.set('numeroCasa', String(payload.numeroCasa));
                        }

                        setBanner('', 'Validando…', 'Consultando servidor');

                        var res = await fetch('${pageContext.request.contextPath}/acceso/validar', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                            body: fd.toString(),
                            cache: 'no-store'
                        });

                        if (!res.ok) {
                            setBanner('error', 'Error servidor', 'No se pudo validar.');
                            busy = false;
                            return;
                        }

                        var data = await res.json();
                        if (data.resultado === 'permitido') {
                            setBanner('ok', 'ACCESO PERMITIDO', data.motivo || 'Bienvenido');
                            successFX();
                            notifyArduino('permitido');
                        } else {
                            setBanner('deny', 'ACCESO DENEGADO', data.motivo || 'No autorizado');
                            errorFX();
                            notifyArduino('denegado');
                        }
                    } catch (e) {
                        console.error(e);
                        setBanner('error', 'Error inesperado', e && e.message ? e.message : 'Revise consola');
                    } finally {
                        setTimeout(function () {
                            busy = false;
                        }, 800);
                    }
                }

                function buildPayload(text) {
                    // ?tk=TOKEN
                    try {
                        var u = new URL(text, window.location.origin);
                        var tk = u.searchParams.get('tk');
                        if (tk)
                            return {modo: 'token', tk: tk};
                    } catch (_) {
                    }

                    // token compacto: b64url.b64url
                    if (text.indexOf('.') !== -1 && /^[A-Za-z0-9\-_]+$/.test(text.split('.')[0])) {
                        return {modo: 'token', tk: text.trim()};
                    }

                    // Texto K:V multilínea
                    var map = parseKeyValue(text);
                    var correo = map['correo'] || map['email'] || null;
                    var lote = map['lote'] || null;
                    var casaStr = map['no. casa'] || map['no casa'] || map['casa'] || null;
                    var numeroCasa = casaStr ? parseInt(casaStr.replace(/\D+/g, ''), 10) : null;

                    if (correo && lote && Number.isInteger(numeroCasa)) {
                        return {modo: 'texto', correo: correo, lote: lote, numeroCasa: numeroCasa};
                    }
                    return null;
                }
                function parseKeyValue(raw) {
                    var out = {};
                    raw.split(/\r?\n/).forEach(function (line) {
                        var x = line.split(':');
                        if (x.length >= 2) {
                            var key = x[0].trim().toLowerCase();
                            var val = x.slice(1).join(':').trim();
                            out[key] = val;
                        }
                    });
                    return out;
                }

                async function notifyArduino(estado) {
                    try {
                        var p = new URLSearchParams();
                        p.set('estado', estado);
                        p.set('tipo', tipoSel.value);
                        p.set('puntoId', puntoSel.value);
                        p.set('direccion', direccion);

                        var res = await fetch('${pageContext.request.contextPath}/arduino/pulse', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                            body: p.toString(),
                            cache: 'no-store'
                        });

                        var data = {};
                        try {
                            data = await res.json();
                        } catch (_) {
                        }
                        if (estado === 'permitido' && (!res.ok || !data.ok)) {
                            var m = (data && data.msg) ? data.msg : 'No se pudo accionar el servo.';
                            setBanner('warn', 'Arduino no respondió', m);
                            errorFX();
                        }
                    } catch (e) {
                        if (estado === 'permitido') {
                            setBanner('warn', 'Error comunicando con Arduino', e.message || 'Fallo inesperado');
                            errorFX();
                        }
                    }
                }
            })();
        </script>
    </body>
</html>
