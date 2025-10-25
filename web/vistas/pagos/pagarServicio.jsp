<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="PagosDAO.TipoPagosDAO,java.util.List,modelo.TipoPago"%>
<%
        // Seguridad de sesión
        String rol = (String) session.getAttribute("usuarioRol");
        Integer userId = (Integer) session.getAttribute("usuarioId");
        String userNombre = (String) session.getAttribute("usuarioNombre");
        if (rol == null || userId == null) {
                response.sendRedirect(request.getContextPath() + "/vistas/login.jsp");
                return;
        }
        // CSRF
        String csrf = Utils.CsrfUtil.ensureCsrfToken(session);

        if (request.getAttribute("tiposPago") == null) {
                try {
                        List<TipoPago> _tipos = new TipoPagosDAO().listarActivos();
                        request.setAttribute("tiposPago", _tipos);
                } catch (Exception e) {
                        request.setAttribute("tiposPago", java.util.Collections.emptyList());
                }
        }
%>
<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <title>Pagar Servicio</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link href="${pageContext.request.contextPath}/CSS/pagar-servicio.css" rel="stylesheet">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>

                <style>
                        .btn-glass-back {
                                position: absolute;
                                left: 20px;
                                padding: 16px 40px;
                                font-size: 17px;
                                font-weight: 600;
                                color: white;
                                background: rgba(255, 255, 255, 0.05);
                                border: 1px solid rgba(255, 255, 255, 0.1);
                                border-radius: 20px;
                                cursor: pointer;
                                backdrop-filter: blur(20px) saturate(180%);
                                transition: all 0.4s;
                                display: flex;
                                align-items: center;
                                gap: 12px;
                                overflow: hidden;
                                text-decoration: none;
                                margin-top: 28px;
                        }

                        .btn-glass-back::before {
                                content: "";
                                position: absolute;
                                top: -50%;
                                left: -50%;
                                width: 200%;
                                height: 200%;
                                background: radial-gradient(circle, rgba(167, 139, 250, 0.3) 0%, transparent 70%);
                                opacity: 0;
                                transition: opacity 0.5s;
                        }

                        .btn-glass-back:hover::before {
                                opacity: 1;
                                animation: rotate 3s linear infinite;
                        }

                        @keyframes rotate {
                                from { transform: rotate(0deg); }
                                to { transform: rotate(360deg); }
                        }

                        .btn-glass-back:hover {
                                background: rgba(255, 255, 255, 0.1);
                                border-color: rgba(167, 139, 250, 0.5);
                                box-shadow: 0 15px 50px rgba(167, 139, 250, 0.3);
                                transform: translateY(-3px);
                        }

                        .btn-glass-back .icon {
                                position: relative;
                                z-index: 1;
                                transition: transform 0.3s;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                width: 32px;
                                height: 32px;
                        }

                        .btn-glass-back .icon svg {
                                width: 24px;
                                height: 24px;
                                filter: drop-shadow(0 2px 4px rgba(167, 139, 250, 0.3));
                        }

                        .btn-glass-back .text {
                                position: relative;
                                z-index: 1;
                        }

                        .btn-glass-back:hover .icon {
                                transform: translateX(-5px);
                        }

                        .btn-glass-back:hover .icon svg {
                                filter: drop-shadow(0 0 10px rgba(167, 139, 250, 0.8));
                        }
                </style>
        </head>
        <body>

                <button class="btn-glass-back" onclick="window.location.href='<c:url value='/vistas/menuResidente.jsp'/>'">
                        <span class="icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                                <polyline points="9 22 9 12 15 12 15 22"/>
                                </svg>
                        </span>
                        <span class="text">Ir al inicio</span>
                </button>


                <div class="container">
                        <header class="header">
                                <h1>Pago Seguro</h1>
                                <p>Completa tu información para procesar el pago</p>
                        </header>

                        <!-- IMPORTANTE: id="paymentForm" para que el JS lo encuentre -->
                        <form id="paymentForm" method="post" action="${pageContext.request.contextPath}/pagos/registrar" autocomplete="off" novalidate>
                                <input type="hidden" name="csrf" value="<%=csrf%>">
                                <input type="hidden" id="mesAnio" name="mesAnio" value=""><!-- Se setea tras calcular -->

                                <!-- === NUEVO (no rompe el CSS): inputs ocultos para lo que esperan los servlets === -->
                                <input type="hidden" id="usuarioId"      name="usuarioId"      value="<%=userId%>">
                                <input type="hidden" id="nombreUsuario"  name="nombreUsuario"  value="<%= (userNombre == null ? "" : userNombre)%>">
                                <input type="hidden" id="tipoPagoId"     name="tipoPagoId"     value="">
                                <input type="hidden" id="tarjetaPan"     name="tarjetaPan"     value="">

                                <!-- Método de pago (solo Tarjeta) -->
                                <section class="card full-width">
                                        <div class="card-header">
                                                <div class="card-icon">💳</div>
                                                <h2 class="card-title">Método de Pago</h2>
                                        </div>
                                        <div class="payment-methods">
                                                <div class="payment-method active" data-method="card" aria-label="Pago con tarjeta">
                                                        <div class="payment-method-icon">💳</div>
                                                        <div>Tarjeta</div>
                                                </div>
                                        </div>
                                </section>

                                <div class="cards-grid">

                                        <!-- Información de tarjeta -->
                                        <section class="card full-width">
                                                <div class="card-header">
                                                        <div class="card-icon">💎</div>
                                                        <h2 class="card-title">Información de Tarjeta</h2>
                                                </div>

                                                <!-- Visual tarjeta -->
                                                <div class="credit-card-visual" aria-hidden="true">
                                                        <div class="card-chip"></div>
                                                        <div class="card-number-display" id="cardDisplay">•••• •••• •••• ••••</div>
                                                        <div class="card-details-display">
                                                                <div>
                                                                        <div class="muted-sm">Titular</div>
                                                                        <div id="nameDisplay">NOMBRE APELLIDO</div>
                                                                </div>
                                                                <div>
                                                                        <div class="muted-sm">Vence</div>
                                                                        <div id="expiryDisplay">MM/AA</div>
                                                                </div>
                                                        </div>
                                                </div>

                                                <!-- Campos de tarjeta (mantengo tus name/id originales) -->
                                                <div class="form-group">
                                                        <label for="cardNumber">Número de Tarjeta</label>
                                                        <input type="text" id="cardNumber" name="tarjeta" placeholder="1234 5678 9012 3456" maxlength="19" autocomplete="cc-number" inputmode="numeric" required>
                                                </div>

                                                <div class="form-group">
                                                        <label for="cardName">Nombre del Titular</label>
                                                        <input type="text" id="cardName" name="titular" placeholder="Como aparece en la tarjeta" autocomplete="cc-name" required>
                                                </div>

                                                <div class="form-row-3">
                                                        <div class="form-group">
                                                                <label for="expiry">Fecha de Expiración</label>
                                                                <input type="text" id="expiry" name="vencimiento" placeholder="MM/AA" maxlength="5" autocomplete="cc-exp" required>
                                                        </div>
                                                        <div class="form-group">
                                                                <label for="cvv">CVV</label>
                                                                <input type="password" id="cvv" name="cvv" placeholder="123" maxlength="4" autocomplete="cc-csc" inputmode="numeric" required>
                                                        </div>
                                                        <!-- ZIP eliminado -->
                                                </div>
                                        </section>

                                        <!-- Parámetros del pago -->
                                        <section class="card">
                                                <div class="card-header">
                                                        <div class="card-icon">⚙️</div>
                                                        <h2 class="card-title">Detalles del Pago</h2>
                                                </div>

                                                <!-- Mantengo id/name="tipoPago", pero leo catálogo -->
                                                <div class="form-group">
                                                        <label for="tipoPago">Tipo de pago</label>
                                                        <select id="tipoPago" name="tipoPago" required>
                                                                <option value="">Seleccione…</option>
                                                                <c:forEach var="t" items="${tiposPago}">
                                                                        <!-- value = ID del catálogo; guardo metadata para UI -->
                                                                        <option value="${t.id}" data-codigo="${t.codigo}" data-monto="${t.montoBase}" data-recurrente="${t.recurrente}">
                                                                                ${t.nombre} (${t.codigo})
                                                                        </option>
                                                                </c:forEach>
                                                        </select>
                                                </div>

                                                <div class="form-group">
                                                        <label for="observaciones">Observaciones</label>
                                                        <textarea id="observaciones" name="observaciones" rows="3" minlength="5" placeholder="Motivo o nota del pago (obligatorio)" required></textarea>
                                                </div>

                                                <button id="btnConsultar" type="button" class="btn-secondary" disabled>Calcular total</button>
                                        </section>

                                        <!-- Resumen (se muestra SOLO tras consultar) -->
                                        <section id="resumenCard" class="card hide">
                                                <div class="card-header">
                                                        <div class="card-icon">🧾</div>
                                                        <h2 class="card-title">Resumen</h2>
                                                </div>

                                                <div class="resume-list">
                                                        <div class="resume-row"><span>Tipo</span><strong id="resTipo">-</strong></div>
                                                        <div class="resume-row"><span>Mes/Año</span><strong id="resMes">-</strong></div>
                                                        <div class="resume-row"><span>Monto base</span><strong id="resBase">Q0.00</strong></div>
                                                        <div class="resume-row"><span>Mora</span><strong id="resMora">Q0.00</strong></div>
                                                        <div class="resume-total"><span>Total</span><strong id="resTotal">Q0.00</strong></div>
                                                        <div class="muted-sm" id="resFecha">Fecha de pago: —</div>
                                                </div>
                                        </section>

                                </div>

                                <!-- Botón de pagar -->
                                <section class="card full-width">
                                        <button type="submit" id="btnPagar" class="submit-btn" disabled>
                                                <i class="fa-solid fa-lock"></i> Completar Pago
                                        </button>
                                        <div class="security-badge">Transacción segura y encriptada</div>
                                </section>
                        </form>
                </div>

                <script>
        /* ==== Refs (no cambio tus ids/classes) ==== */
        const form = document.getElementById('paymentForm');
        const btnPagar = document.getElementById('btnPagar');
        const btnConsultar = document.getElementById('btnConsultar');
        const tipoPago = document.getElementById('tipoPago');
        const obs = document.getElementById('observaciones');
        const cardNumber = document.getElementById('cardNumber');
        const cardName = document.getElementById('cardName');
        const expiry = document.getElementById('expiry');
        const cvv = document.getElementById('cvv');
        const cardDisplay = document.getElementById('cardDisplay');
        const nameDisplay = document.getElementById('nameDisplay');
        const expiryDisplay = document.getElementById('expiryDisplay');
        const resumenCard = document.getElementById('resumenCard');
        const resTipo = document.getElementById('resTipo');
        const resMes = document.getElementById('resMes');
        const resBase = document.getElementById('resBase');
        const resMora = document.getElementById('resMora');
        const resTotal = document.getElementById('resTotal');
        const resFecha = document.getElementById('resFecha');
        const mesAnio = document.getElementById('mesAnio');
        // Ocultos backend
        const usuarioId = document.getElementById('usuarioId');
        const nombreUsuario = document.getElementById('nombreUsuario');
        const tipoPagoId = document.getElementById('tipoPagoId');
        const tarjetaPan = document.getElementById('tarjetaPan');
        const ctx = '${pageContext.request.contextPath}';
        const VALIDAR_LUHN = false;
        let sending = false;
        /* ==== Helpers validación ==== */
        function luhnOk16(number) {
        const s = (number || '').replace(/\s+/g, '');
        if (!/^\d{16}$/.test(s)) return false;
        let sum = 0, alt = false;
        for (let i = s.length - 1; i >= 0; i--) {
        let n = s.charCodeAt(i) - 48;
        if (alt) { n *= 2; if (n > 9) n -= 9; }
        sum += n; alt = !alt;
        }
        return sum % 10 === 0;
        }
        function vencimientoOk(mmYY) {
        if (!/^\d{2}\/\d{2}$/.test(mmYY)) return false;
        const mm = parseInt(mmYY.slice(0, 2), 10), yy = 2000 + parseInt(mmYY.slice(3), 10);
        if (mm < 1 || mm > 12) return false;
        const now = new Date(), cur = now.getFullYear() * 100 + (now.getMonth() + 1), val = yy * 100 + mm;
        return val >= cur;
        }
        function titularOk(s) { return /^[a-záéíóúñ\s]{4,}$/i.test((s || '').trim()); }
        function cvvOk(s) { return /^\d{3,4}$/.test((s || '').trim()); }
        function numeroTarjetaOk(number) {
        const s = (number || '').replace(/\s+/g, '');
        if (!/^\d{16}$/.test(s)) return false;
        return VALIDAR_LUHN ? luhnOk16(number) : true;
        }
        function mark(el, ok) { if (!el) return; el.classList.remove('input-error', 'input-success'); el.classList.add(ok?'input-success':'input-error'); }
        function limpiarMarca(el){ if (!el) return; el.classList.remove('input-error', 'input-success'); }

        /* ==== UI / formateos ==== */
        tipoPago.addEventListener('change', () => {
        const okTipo = !!tipoPago.value;
        tipoPagoId.value = tipoPago.value || ""; // <-- mantener sincronizado el hidden
        okTipo ? mark(tipoPago, true) : limpiarMarca(tipoPago);
        btnConsultar.disabled = !okTipo;
        if (resumenCard) resumenCard.classList.add('hide');
        validarTodo();
        });
        cardNumber.addEventListener('input', (e) => {
        let v = e.target.value.replace(/\D/g, '').slice(0, 16);
        v = (v.match(/.{1,4}/g) || []).join(' ');
        e.target.value = v;
        if (cardDisplay) {
        if (v.length) {
        const mask = v.split('').map((ch, i) => ch === ' ' ? ' ' : (i < v.length - 4 ? '•' : ch)).join('');
        cardDisplay.textContent = mask || '•••• •••• •••• ••••';
        } else cardDisplay.textContent = '•••• •••• •••• ••••';
        }
        validarTodo();
        });
        cardName.addEventListener('input', (e) => {
        if (nameDisplay) nameDisplay.textContent = (e.target.value || 'NOMBRE APELLIDO').toUpperCase();
        validarTodo();
        });
        expiry.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, '').slice(0, 4);
        if (value.length >= 3) value = value.slice(0, 2) + '/' + value.slice(2);
        e.target.value = value;
        if (expiryDisplay) expiryDisplay.textContent = value || 'MM/AA';
        validarTodo();
        });
        cvv.addEventListener('input', validarTodo);
        obs.addEventListener('input', validarTodo);
        /* ==== Calcular (envía tipoPagoId) ==== */
        btnConsultar.addEventListener('click', async () => {
        if (!tipoPago.value) { mark(tipoPago, false); return; }
        try {
        const params = new URLSearchParams();
        params.append('tipoPagoId', tipoPago.value);
        params.append('usuarioId', usuarioId.value || '');
        params.append('nombreUsuario', nombreUsuario.value || '');
        if (mesAnio.value) params.append('mesAnio', mesAnio.value);
        const csrf = document.querySelector('input[name="csrf"]')?.value || '';
        if (csrf) params.append('csrf', csrf);
        const res = await fetch(ctx + '/pagos/calcular', {
        method: 'POST',
                headers: {'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
                body: params
        });
        const data = await res.json();
        if (data.error) { alert(data.error); return; }

        const opt = tipoPago.options[tipoPago.selectedIndex];
        const codigo = opt?.dataset?.codigo || data.tipo || '-';
        resTipo.textContent = codigo;
        resMes.textContent = data.mesAnio ? data.mesAnio : '/';
        resBase.textContent = 'Q' + Number(data.montoBase).toFixed(2);
        resMora.textContent = 'Q' + Number(data.mora).toFixed(2);
        resTotal.textContent = 'Q' + Number(data.total).toFixed(2);
        resFecha.textContent = 'Fecha de pago: ' + (data.fechaPago || new Date().toISOString().slice(0, 16).replace('T', ' '));
        mesAnio.value = data.mesAnio ? data.mesAnio : '';
        resumenCard.classList.remove('hide');
        validarTodo();
        } catch (err) {
        console.error(err);
        alert('No se pudo calcular el total.');
        }
        });
        /* ==== Validación principal -> habilitar botón ==== */
        function validarTodo() {
        const okNumero = numeroTarjetaOk(cardNumber.value);
        const okTitular = titularOk(cardName.value);
        const okVence = vencimientoOk(expiry.value);
        const okCvv = cvvOk(cvv.value);
        const okObs = (obs.value || '').trim().length >= 5;
        const okTipo = !!tipoPago.value;
        cardNumber.value ? mark(cardNumber, okNumero) : limpiarMarca(cardNumber);
        cardName.value  ? mark(cardName, okTitular)  : limpiarMarca(cardName);
        expiry.value    ? mark(expiry, okVence)    : limpiarMarca(expiry);
        cvv.value       ? mark(cvv, okCvv)      : limpiarMarca(cvv);
        obs.value       ? mark(obs, okObs)      : limpiarMarca(obs);
        okTipo          ? mark(tipoPago, true)       : limpiarMarca(tipoPago);
        btnPagar.disabled = !(okNumero && okTitular && okVence && okCvv && okObs && okTipo);
        btnConsultar.disabled = !okTipo;
        }
        document.addEventListener('DOMContentLoaded', validarTodo);
        /* ==== Skeleton Loader ==== */
        function createSkeletonLoader() {
        const skeletonHTML = `
     <div id="pageSkeleton" class="page-skeleton-overlay">
       <div class="page-skeleton-container">
         <div class="skeleton-header"><div class="skeleton-title"></div><div class="skeleton-subtitle"></div></div>
         <div class="skeleton-cards-grid">
           <div class="skeleton-card full-width"><div class="skeleton-card-header"><div class="skeleton-icon"></div><div class="skeleton-text"></div></div><div class="skeleton-line"></div></div>
           <div class="skeleton-card full-width"><div class="skeleton-card-header"><div class="skeleton-icon"></div><div class="skeleton-text"></div></div><div class="skeleton-line"></div><div class="skeleton-line"></div><div class="skeleton-line short"></div></div>
           <div class="skeleton-card"><div class="skeleton-card-header"><div class="skeleton-icon"></div><div class="skeleton-text"></div></div><div class="skeleton-line"></div><div class="skeleton-line"></div></div>
           <div class="skeleton-card"><div class="skeleton-card-header"><div class="skeleton-icon"></div><div class="skeleton-text"></div></div><div class="skeleton-line"></div><div class="skeleton-line short"></div></div>
           <div class="skeleton-card full-width"><div class="skeleton-button"></div></div>
         </div>
       </div>
     </div>`;
        document.body.insertAdjacentHTML('afterbegin', skeletonHTML);
        }
        function hideSkeletonLoader() {
        const skeleton = document.getElementById('pageSkeleton');
        if (skeleton) { skeleton.classList.add('fade-out'); setTimeout(() => skeleton.remove(), 400); }
        }
        createSkeletonLoader();
        window.addEventListener('load', () => setTimeout(hideSkeletonLoader, 500));
        document.addEventListener('DOMContentLoaded', hideSkeletonLoader);
        /* ==== Payment Loader ==== */
        function createPaymentLoader() {
        const loaderHTML = `
     <div id="paymentLoader" class="payment-loader-overlay">
       <div class="payment-loader-content">
         <div class="payment-loader-rings">
           <div class="ring ring-1"></div><div class="ring ring-2"></div><div class="ring ring-3"></div>
           <div class="center-icon">💳</div>
         </div>
         <div class="payment-loader-text"><h3>Procesando pago</h3><p>Por favor espera un momento...</p></div>
       </div>
     </div>`;
        if (!document.getElementById('paymentLoader')) {
        document.body.insertAdjacentHTML('beforeend', loaderHTML);
        }
        }
        function showPaymentLoader(){ createPaymentLoader(); const l = document.getElementById('paymentLoader'); if (l) setTimeout(() => l.classList.add('active'), 10); }
        function hidePaymentLoader(){ const l = document.getElementById('paymentLoader'); if (l) l.classList.remove('active'); }

        // ======= OVERLAY DE ÉXITO (simple, sin datos) =======
        function createSuccessScreen() {
        const html = `
     <div id="successOverlay"
          style="position:fixed; inset:0; background:rgba(10,15,28,.92);
                 display:flex; align-items:center; justify-content:center;
                 z-index:2147483647;">
       <div style="background:rgba(255,255,255,.07);
                   border:1px solid rgba(255,255,255,.15);
                   border-radius:18px; width:min(520px,92vw);
                   padding:32px 28px; text-align:center;
                   box-shadow:0 20px 60px rgba(0,0,0,.55)">
         <div style="width:88px;height:88px;border-radius:50%;
                     margin:0 auto 18px;background:#22c55e;
                     display:flex;align-items:center;justify-content:center;">
           <svg viewBox="0 0 76 76" width="56" height="56">
             <circle cx="38" cy="38" r="36" fill="none"></circle>
             <path id="successPath"
                   fill="none" stroke="#FFFFFF" stroke-width="5"
                   stroke-linecap="round" stroke-linejoin="round"
                   d="M17.7,40.9l10.9,10.9l28.7-28.7"/>
           </svg>
         </div>

         <!-- Textos actualizados -->
         <h2 style="color:#e6e8ee;font-size:28px;margin:0 0 6px">¡Pago completado!</h2>
         <p style="color:#9fb1c3;margin:0 0 22px">Tu pago se procesó correctamente.</p>

         <button id="successBackBtn"
                 style="background:linear-gradient(90deg,#22c55e,#16a34a);
                        color:#07120a;border:0;border-radius:12px;
                        padding:12px 16px;font-weight:700;cursor:pointer;
                        width:100%;max-width:220px">Regresar</button>
       </div>
     </div>`;
   if (!document.getElementById('successOverlay')) {
     document.body.insertAdjacentHTML('beforeend', html);
   }

   // Animación del check
   const path = document.getElementById('successPath');
   if (path) {
     const len = path.getTotalLength();
     path.style.strokeDasharray = len;
     path.style.strokeDashoffset = len;
     requestAnimationFrame(() => {
       path.style.transition = 'stroke-dashoffset 350ms ease 100ms';
       path.style.strokeDashoffset = 0;
     });
   }

   // Botón regresar
   const back = document.getElementById('successBackBtn');
   if (back) back.addEventListener('click', (e) => { e.preventDefault(); closeSuccessAndReset(); });
 }
 function showSuccessScreen() { createSuccessScreen(); }
 function hideSuccessScreen() { const o = document.getElementById('successOverlay'); if (o) o.remove(); }
 function closeSuccessAndReset() {
   hideSuccessScreen();
   if (form) {
     form.reset();
     if (cardDisplay)  cardDisplay.textContent  = '•••• •••• •••• ••••';
     if (nameDisplay)  nameDisplay.textContent  = 'NOMBRE APELLIDO';
     if (expiryDisplay)expiryDisplay.textContent= 'MM/AA';
     if (resumenCard)  resumenCard.classList.add('hide');
     if (mesAnio)      mesAnio.value = '';
     if (tipoPagoId)   tipoPagoId.value = '';
     if (tarjetaPan)   tarjetaPan.value = '';
     ['cardNumber','cardName','expiry','cvv','tipoPago','observaciones'].forEach(id=>{
       const el = document.getElementById(id);
       if (el) el.classList.remove('input-success','input-error');
     });
     validarTodo();
   }
   window.scrollTo({ top: 0, behavior: 'smooth' });
 }

 // ======= OVERLAY DE ERROR =======
 function showErrorScreen(msg) {
   const html = `
     <div id="errorOverlay"
          style="position:fixed; inset:0; background:rgba(10,15,28,.92);
                 display:flex; align-items:center; justify-content:center; z-index:2147483647;">
       <div style="background:rgba(255,255,255,.07); border:1px solid rgba(255,255,255,.15);
                   border-radius:18px; width:min(520px,92vw); padding:28px; text-align:center;">
         <div style="width:88px;height:88px;border-radius:50%;margin:0 auto 18px;
                     background:#ef4444;display:flex;align-items:center;justify-content:center;">
           <svg viewBox="0 0 24 24" width="46" height="46" fill="none" stroke="#fff" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
             <circle cx="12" cy="12" r="10"></circle>
             <line x1="15" y1="9" x2="9" y2="15"></line>
             <line x1="9" y1="9" x2="15" y2="15"></line>
           </svg>
         </div>
         <h2 style="color:#e6e8ee;margin:0 0 8px">No se pudo completar el pago</h2>
         <p style="color:#cbd5e1;margin:0 0 18px">${msg || 'Intenta nuevamente.'}</p>
         <button id="errorCloseBtn"
                 style="background:linear-gradient(90deg,#f97316,#ef4444);
                        color:#0b0f16;border:0;border-radius:12px;padding:12px 16px;
                        font-weight:700;cursor:pointer;width:100%;max-width:220px">Cerrar</button>
       </div>
     </div>`;
   if (!document.getElementById('errorOverlay')) {
     document.body.insertAdjacentHTML('beforeend', html);
   }
   const btn = document.getElementById('errorCloseBtn');
   if (btn) btn.addEventListener('click', () => {
     const o = document.getElementById('errorOverlay'); if (o) o.remove();
   });
 }

 /* ==== Submit con fetch x-www-form-urlencoded ==== */
 document.addEventListener('DOMContentLoaded', function () {
   if (!form) return;

   form.addEventListener('submit', function (e) {
     e.preventDefault();
     if (btnPagar.disabled || sending) return;

     sending = true;
     btnPagar.disabled = true;

     // Asegurar ocultos
     tipoPagoId.value = tipoPago.value || '';
     tarjetaPan.value = (cardNumber.value || '').replace(/\s+/g, '');

     showPaymentLoader();

     // Construir x-www-form-urlencoded
     const params = new URLSearchParams();
     params.append('csrf', document.querySelector('input[name="csrf"]')?.value || '');
     params.append('usuarioId', usuarioId.value || '');
     params.append('nombreUsuario', nombreUsuario.value || '');
     params.append('tipoPagoId', tipoPagoId.value || '');
     params.append('mesAnio', mesAnio.value || '');
     params.append('observaciones', obs.value || '');
     params.append('tarjetaPan', tarjetaPan.value || '');
     // campos visuales (si el backend los usa)
     params.append('tarjeta', cardNumber.value || '');
     params.append('titular', cardName.value || '');
     params.append('vencimiento', expiry.value || '');
     params.append('cvv', cvv.value || '');

     fetch(form.action, {
       method: 'POST',
       headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
       body: params.toString()
     })
     .then(response => {
       const ct = response.headers.get('content-type') || '';
       if (ct.includes('application/json')) return response.json();
       return response.text().then(t => { try { return JSON.parse(t); } catch { throw new Error('Respuesta no válida del servidor'); } });
     })
     .then(data => {
       // === FIX: calcular flag de éxito correctamente (antes usabas "success" que NO existía) ===
       const ok = data && (data.ok === true || data.success === true || data.pagoId != null || data.pageId != null || data.id != null);
       if (ok) {
         const enriched = Object.assign({}, data, {
           tipo:  (resTipo?.textContent || data.tipo || data.tipoCodigo || 'Servicio'),
           fecha: ((resFecha?.textContent || '').replace('Fecha de pago: ',''))
                  || new Date().toLocaleString('es-GT')
         });
         showSuccessScreen(enriched);
       } else {
         showErrorScreen(data.error || data.message || 'Error al procesar el pago.');
       }
     })
     .catch(err => {
       console.error('Error:', err);
       showErrorScreen('Error de conexión. Por favor intenta de nuevo.');
     })
     .finally(() => {
       hidePaymentLoader();
       sending = false;
       btnPagar.disabled = false;
     });
   });
 });
                </script>




        </body>
</html>
