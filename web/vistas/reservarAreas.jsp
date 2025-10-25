<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="ReservasDAO.ReservasAreasDAO.ReservaItem"%>
<%
        java.util.Map areas = (java.util.Map) request.getAttribute("areas");
        Integer areaId = (Integer) request.getAttribute("areaId");
        Integer anio = (Integer) request.getAttribute("anio");
        Integer mes = (Integer) request.getAttribute("mes");
        Integer dia = (Integer) request.getAttribute("dia");
        boolean[] horas = (boolean[]) request.getAttribute("horas");
        String status = (String) request.getAttribute("status");
        Integer hstart = (Integer) request.getAttribute("hstart");
        java.util.List misReservas = (java.util.List) request.getAttribute("misReservas");

        if (areas == null) {
                areas = new java.util.LinkedHashMap();
        }
        if (areaId == null) {
                areaId = 1;
        }
        if (anio == null) {
                anio = java.time.LocalDate.now().getYear();
        }
        if (mes == null) {
                mes = java.time.LocalDate.now().getMonthValue();
        }
        if (dia == null) {
                dia = java.time.LocalDate.now().getDayOfMonth();
        }
        if (hstart == null) {
                hstart = 0;
        }
        int hend = Math.min(hstart + 8, 24);

        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate fechaSel;
        try {
                fechaSel = java.time.LocalDate.of(anio, mes, dia);
        } catch (Exception e) {
                fechaSel = hoy;
        }
        boolean fechaPasada = fechaSel.isBefore(hoy);
        request.setAttribute("fechaPasada", fechaPasada);

        String nombreArea = "Área común";
        try {
                if (areas != null && !areas.isEmpty() && areas.containsKey(areaId)) {
                        nombreArea = String.valueOf(areas.get(areaId));
                } else if (areaId == 1) {
                        nombreArea = "Piscina";
                } else if (areaId == 2) {
                        nombreArea = "Salón";
                }
        } catch (Exception ignore) {
        }

        int sugDesde = hstart;
        if (horas != null) {
                for (int h = hstart; h < hend; h++) {
                        if (horas[h]) {
                                sugDesde = h;
                                break;
                        }
                }
        }
        int sugHasta = Math.min(sugDesde + 1, 24);
%>
<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <title>Reservar Áreas Comunes</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/reservas.css">
        </head>
        <body>
                <div class="dashboard">
                        <!-- Header -->
                        <header class="hero">
                                <div class="hero-left">
                                        <div class="hero-badge">🏡</div>
                                        <div>
                                                <h1 class="title"><span class="gradient">Reservación de áreas comunes</span></h1>
                                                <p class="subtitle">
                                                        Consulta disponibilidad, filtra por fecha y crea/cancela reservas
                                                        <span class="chip"><%= nombreArea%></span>
                                                        <% if (fechaPasada) { %>
                                                        <span class="chip chip-warn">Fecha pasada (solo consulta)</span>
                                                        <% }%>
                                                </p>
                                        </div>
                                </div>
                                <div class="hero-actions">
                                        <a class="btn btn-secondary" href="<c:url value='/vistas/menuResidente.jsp'/>">Regresar</a>
                                </div>
                        </header>

                        <main class="content">
                                <section class="card">
                                        <!-- Alertas -->
                                        <div aria-live="polite">
                                                <c:choose>
                                                        <c:when test="${status == 'ok'}"><div class="alert ok">✓ ¡Reservación realizada correctamente!</div></c:when>
                                                        <c:when test="${status == 'conflict'}"><div class="alert err">✗ No fue posible reservar: ya existe otra reserva que se cruza con ese rango.</div></c:when>
                                                        <c:when test="${status == 'bad_range'}"><div class="alert err">✗ El rango de horas no es válido. "Hasta" debe ser mayor que "Desde".</div></c:when>
                                                        <c:when test="${status == 'db_err'}"><div class="alert err">✗ No fue posible reservar por un error de base de datos.</div></c:when>
                                                        <c:when test="${status == 'err'}"><div class="alert err">✗ No fue posible reservar por un motivo desconocido.</div></c:when>
                                                        <c:when test="${status == 'cancel_ok'}"><div class="alert ok">✓ La reserva fue cancelada correctamente.</div></c:when>
                                                        <c:when test="${status == 'cancel_err'}"><div class="alert err">✗ No fue posible cancelar la reserva.</div></c:when>
                                                        <c:when test="${status == 'past_date'}"><div class="alert err">✗ No se permiten reservas en fechas anteriores a hoy.</div></c:when>
                                                </c:choose>
                                        </div>

                                        <!-- FILTRO -->
                                        <form method="get" action="<%= request.getContextPath()%>/reservarAreas">
                                                <div class="form-grid">
                                                        <div class="form-field">
                                                                <label class="label">Área</label>
                                                                <select name="areaId" required class="input">
                                                                        <option value="" disabled <%= (areas.isEmpty() ? "selected" : "")%>>Selecciona un área…</option>
                                                                        <% if (!areas.isEmpty()) {
                                                                                        for (Object k : areas.keySet()) {
                                                                                                Integer id = (Integer) k;
                                                                                                String nom = String.valueOf(areas.get(k));
                                                                        %>
                                                                        <option value="<%= id%>" <%= id.equals(areaId) ? "selected" : ""%>><%= nom%></option>
                                                                        <%  }
                                                                        } else {%>
                                                                        <option value="1" <%= (areaId == 1 ? "selected" : "")%>>Piscina</option>
                                                                        <option value="2" <%= (areaId == 2 ? "selected" : "")%>>Salón</option>
                                                                        <% }%>
                                                                </select>
                                                        </div>

                                                        <div class="form-field">
                                                                <label class="label">Año</label>
                                                                <input class="input" type="number" name="anio" min="2024" max="2100" value="<%= anio%>">
                                                        </div>

                                                        <div class="form-field">
                                                                <label class="label">Mes</label>
                                                                <select class="input" name="mes" required>
                                                                        <% for (int i = 1; i <= 12; i++) {%>
                                                                        <option value="<%= i%>" <%= (mes == i ? "selected" : "")%>><%= (i < 10 ? ("0" + i) : ("" + i))%></option>
                                                                        <% } %>
                                                                </select>
                                                        </div>

                                                        <div class="form-field">
                                                                <label class="label">Día</label>
                                                                <select class="input" name="dia" required>
                                                                        <% for (int d1 = 1; d1 <= 31; d1++) {
                                                                                        boolean disablePastOpt = false;
                                                                                        try {
                                                                                                java.time.LocalDate opt = java.time.LocalDate.of(anio, mes, d1);
                                                                                                disablePastOpt = opt.isBefore(hoy);
                                                                                        } catch (Exception e) {
                                                                                                disablePastOpt = false;
                                                                                        }
                                                                        %>
                                                                        <option value="<%= d1%>" <%= (dia == d1 ? "selected" : "")%> <%= (disablePastOpt ? "disabled" : "")%>>
                                                                                <%= (d1 < 10 ? ("0" + d1) : ("" + d1))%>
                                                                                <%= (disablePastOpt ? " (no reservable)" : "")%>
                                                                        </option>
                                                                        <% }%>
                                                                </select>
                                                                <div class="muted">Los días anteriores a hoy aparecen deshabilitados.</div>
                                                        </div>
                                                </div>

                                                <input type="hidden" name="hstart" value="<%= hstart%>">
                                                <div class="actions">
                                                        <button type="submit" class="btn btn-primary" name="accion" value="filtrar">Filtrar</button>
                                                        <span class="spacer"></span>
                                                </div>
                                        </form>

                                        <!-- ESTADÍSTICAS -->
                                        <%
                                                int disponibles = 0, ocupados = 0;
                                                if (horas != null) {
                                                        for (int h = hstart; h < hend; h++) {
                                                                if (horas[h]) {
                                                                        disponibles++;
                                                                } else {
                                                                        ocupados++;
                                                                }
                                                        }
                                                }
                                        %>
                                        <div class="stats-bar">
                                                <div class="stat-card">
                                                        <div class="stat-icon green">✓</div>
                                                        <div class="stat-info">
                                                                <div class="stat-label">Disponibles</div>
                                                                <div class="stat-value"><%= disponibles%></div>
                                                        </div>
                                                </div>
                                                <div class="stat-card">
                                                        <div class="stat-icon red">✗</div>
                                                        <div class="stat-info">
                                                                <div class="stat-label">Ocupadas</div>
                                                                <div class="stat-value"><%= ocupados%></div>
                                                        </div>
                                                </div>
                                        </div>

                                        <!-- TIMELINE -->
                                        <h2 class="section-title">Disponibilidad para <%= String.format("%04d-%02d-%02d", anio, mes, dia)%> — <%= nombreArea%></h2>
                                        <div class="timeline-container">
                                                <div class="timeline-grid">
                                                        <%
                                                                for (int h = hstart; h < hend; h++) {
                                                                        String label = (h < 10 ? ("0" + h) : ("" + h)) + ":00";
                                                                        boolean libre = (horas != null ? horas[h] : true);
                                                                        String claseSlot = libre ? "disponible" : "ocupado";
                                                                        String icono = libre ? "✓" : "✗";
                                                                        String estado = libre ? "Disponible" : "Ocupado";
                                                        %>
                                                        <div class="timeline-slot <%= claseSlot%>">
                                                                <span class="slot-icon"><%= icono%></span>
                                                                <div class="slot-time"><%= label%></div>
                                                                <div class="slot-status"><%= estado%></div>
                                                        </div>
                                                        <% } %>
                                                </div>
                                        </div>

                                        <!-- PAGINACIÓN -->
                                        <div class="pager">
                                                <%
                                                        int prev = Math.max(0, hstart - 8);
                                                        int next = (hstart + 8 <= 16) ? hstart + 8 : hstart;
                                                        boolean canPrev = hstart > 0;
                                                        boolean canNext = hstart < 16;
                                                        String base = request.getContextPath() + "/reservarAreas?areaId=" + areaId + "&anio=" + anio + "&mes=" + mes + "&dia=" + dia;
                                                %>
                                                <a class="btn btn-secondary" href="<%= canPrev ? (base + "&hstart=" + prev) : "#"%>" <%= canPrev ? "" : "aria-disabled='true'"%> style="<%= canPrev ? "" : "pointer-events:none;opacity:.5"%>">Anterior</a>
                                                <span class="muted">Mostrando horas <%= String.format("%02d:00", hstart)%> a <%= String.format("%02d:00", hend)%></span>
                                                <a class="btn btn-secondary" href="<%= canNext ? (base + "&hstart=" + next) : "#"%>" <%= canNext ? "" : "aria-disabled='true'"%> style="<%= canNext ? "" : "pointer-events:none;opacity:.5"%>">Siguiente</a>
                                        </div>

                                        <!-- FORM RESERVA -->
                                        <h2 class="section-title">Crear Reserva</h2>
                                        <form id="frmReservar" method="post" action="<%= request.getContextPath()%>/reservarAreas" onsubmit="return prepararBitacoraReserva();">
                                                <input type="hidden" name="accion" value="reservar">
                                                <input type="hidden" name="areaId" value="<%= areaId%>">
                                                <input type="hidden" name="hstart" value="<%= hstart%>">
                                                <input type="hidden" id="b_meta_reserva" name="b_meta" value="">

                                                <div class="form-grid form-grid--2">
                                                        <div class="form-field">
                                                                <label class="label">Año</label>
                                                                <input class="input" id="anioR" type="number" name="anio" min="2024" max="2100" value="<%= anio%>" required>
                                                        </div>
                                                        <div class="form-field">
                                                                <label class="label">Mes</label>
                                                                <select class="input" id="mesR" name="mes" required>
                                                                        <% for (int i2 = 1; i2 <= 12; i2++) {%>
                                                                        <option value="<%= i2%>" <%= (mes == i2 ? "selected" : "")%>><%= (i2 < 10 ? ("0" + i2) : ("" + i2))%></option>
                                                                        <% } %>
                                                                </select>
                                                        </div>
                                                        <div class="form-field">
                                                                <label class="label">Día</label>
                                                                <select class="input" id="diaR" name="dia" required>
                                                                        <% for (int d2 = 1; d2 <= 31; d2++) {
                                                                                        boolean disablePastOpt2 = false;
                                                                                        try {
                                                                                                java.time.LocalDate opt = java.time.LocalDate.of(anio, mes, d2);
                                                                                                disablePastOpt2 = opt.isBefore(hoy);
                                                                                        } catch (Exception e) {
                                                                                                disablePastOpt2 = false;
                                                                                        }
                                                                        %>
                                                                        <option value="<%= d2%>" <%= (dia == d2 ? "selected" : "")%> <%= (disablePastOpt2 ? "disabled" : "")%>>
                                                                                <%= (d2 < 10 ? ("0" + d2) : ("" + d2))%> <%= (disablePastOpt2 ? " (no reservable)" : "")%>
                                                                        </option>
                                                                        <% }%>
                                                                </select>
                                                                <div class="muted">El rango es [desde, hasta). Fechas anteriores a hoy no son reservables.</div>
                                                        </div>
                                                        <div class="form-field"></div>

                                                        <div class="form-field">
                                                                <label class="label">Desde (hora)</label>
                                                                <select id="selDesde" name="desdeHora" required class="input" <%= fechaPasada ? "disabled" : ""%>>
                                                                        <% for (int h = 0; h <= 23; h++) {
                                                                                        boolean libre = (horas != null ? horas[h] : true);%>
                                                                        <option value="<%= h%>" <%= (h == sugDesde ? "selected" : "")%> <%= libre ? "" : "disabled"%>>
                                                                                <%= (h < 10 ? ("0" + h) : ("" + h))%>:00 <%= libre ? "" : " (ocupado)"%>
                                                                        </option>
                                                                        <% }%>
                                                                </select>
                                                                <div class="muted">Las horas no disponibles aparecen deshabilitadas.</div>
                                                        </div>
                                                        <div class="form-field">
                                                                <label class="label">Hasta (hora)</label>
                                                                <select id="selHasta" name="hastaHora" required class="input" <%= fechaPasada ? "disabled" : ""%>>
                                                                        <% for (int h = 1; h <= 24; h++) {%>
                                                                        <option value="<%= h%>" <%= (h == sugHasta ? "selected" : "")%>><%= (h < 10 ? ("0" + h) : ("" + h))%>:00</option>
                                                                        <% }%>
                                                                </select>
                                                                <div class="muted">El rango es [desde, hasta). Ej.: 10→12 ocupa 10–11 y 11–12.</div>
                                                        </div>
                                                </div>

                                                <div class="actions">
                                                        <button type="reset" class="btn btn-danger">Limpiar</button>
                                                        <span class="spacer"></span>
                                                        <button id="btnReservar" type="submit" class="btn btn-primary" <%= fechaPasada ? "disabled title='No se puede reservar en fechas pasadas'" : ""%>>Reservar</button>
                                                </div>
                                        </form>

                                        <!-- MIS RESERVAS -->
                                        <h2 class="section-title">Mis reservas activas — <%= nombreArea%></h2>
                                        <c:choose>
                                                <c:when test="${not empty misReservas}">
                                                        <div class="reservas-grid">
                                                                <%
                                                                        java.text.DecimalFormat df2 = new java.text.DecimalFormat("00");
                                                                        for (Object o : (java.util.List) misReservas) {
                                                                                ReservaItem r = (ReservaItem) o;
                                                                                String fechaStr = r.fecha.toString();
                                                                                String hi = df2.format(r.desdeHora) + ":00";
                                                                                String hf = df2.format(r.hastaHora) + ":00";
                                                                %>
                                                                <div class="reserva-card">
                                                                        <div class="reserva-header">
                                                                                <div class="reserva-icon">📅</div>
                                                                                <div>
                                                                                        <div class="reserva-date"><%= fechaStr%></div>
                                                                                        <div class="reserva-area"><%= nombreArea%></div>
                                                                                </div>
                                                                        </div>
                                                                        <div class="reserva-details">
                                                                                <div class="reserva-time">
                                                                                        <div class="reserva-time-label">Desde</div>
                                                                                        <div class="reserva-time-value"><%= hi%></div>
                                                                                </div>
                                                                                <div class="reserva-time">
                                                                                        <div class="reserva-time-label">Hasta</div>
                                                                                        <div class="reserva-time-value"><%= hf%></div>
                                                                                </div>
                                                                        </div>
                                                                        <div class="reserva-actions">
                                                                                <form class="inline frm-cancelar" method="post" action="<%= request.getContextPath()%>/reservarAreas">
                                                                                        <input type="hidden" name="accion" value="cancelar">
                                                                                        <input type="hidden" name="areaId" value="<%= areaId%>">
                                                                                        <input type="hidden" name="reservaId" value="<%= r.id%>">
                                                                                        <input type="hidden" name="anio" value="<%= anio%>">
                                                                                        <input type="hidden" name="mes" value="<%= mes%>">
                                                                                        <input type="hidden" name="dia" value="<%= dia%>">
                                                                                        <input type="hidden" name="hstart" value="<%= hstart%>">
                                                                                        <input type="hidden" name="b_meta" value="">
                                                                                        <!-- IMPORTANTE: botón NO envía el form -->
                                                                                        <button type="button" class="btn btn-danger btn-cancelar">Cancelar</button>
                                                                                </form>
                                                                        </div>
                                                                </div>
                                                                <% } %>
                                                        </div>
                                                </c:when>
                                                <c:otherwise>
                                                        <div class="empty-state">
                                                                <div class="empty-state-icon">📭</div>
                                                                <div class="empty-state-text">No tienes reservas activas próximas para esta área.</div>
                                                        </div>
                                                </c:otherwise>
                                        </c:choose>
                                </section>
                        </main>
                </div>

                <!-- Modal de confirmación -->
                <div id="modalConfirm" class="modal-overlay">
                        <div class="modal-box">
                                <div class="modal-title">¿Está seguro que desea cancelar esta reserva?</div>
                                <div class="modal-message">Esta acción no se puede deshacer.</div>
                                <div class="modal-actions">
                                        <button class="modal-btn modal-btn-cancel" onclick="cerrarModal()">No</button>
                                        <button class="modal-btn modal-btn-confirm" onclick="confirmarAccion()">Sí</button>
                                </div>
                        </div>
                </div>

                <script>
                        /* ===== Bitácora (cliente) ===== */
                        var csidStore = {};
                        (function ensureClientSessionId() {
                                try {
                                        if (!csidStore.csid)
                                                csidStore.csid = 'cs_' + Math.random().toString(16).slice(2) + Date.now().toString(36);
                                } catch (e) {
                                }
                        })();
                        function getCSID() {
                                return csidStore.csid || '';
                        }
                        function mkEvtId() {
                                return 'e_' + Math.random().toString(16).slice(2, 10) + Date.now().toString(36).slice(-6);
                        }
                        function tzOffset() {
                                return new Date().getTimezoneOffset();
                        }
                        function vp() {
                                return {w: (window.innerWidth || 0), h: (window.innerHeight || 0)};
                        }
                        function j(v) {
                                try {
                                        return JSON.stringify(v);
                                } catch (e) {
                                        return '';
                                }
                        }

                        /* ===== Modal ===== */
                        var formActual = null;
                        function mostrarModalConfirmacion(form) {
                                formActual = form;
                                var modal = document.getElementById('modalConfirm');
                                if (modal) {
                                        modal.classList.add('active');
                                        document.body.style.overflow = 'hidden';
                                }
                                return false;
                        }
                        function cerrarModal() {
                                var modal = document.getElementById('modalConfirm');
                                if (modal) {
                                        modal.classList.remove('active');
                                        document.body.style.overflow = '';
                                }
                                formActual = null;
                        }
                        function confirmarAccion() {
                                if (formActual) {
                                        var f = formActual;
                                        cerrarModal();
                                        setTimeout(function () {
                                                f.submit();
                                        }, 150);
                                }
                        }

                        /* Estado de horas para validación */
                        var HORAS = (function () {
                                var arr = [];
                        <% for (int h = 0; h < 24; h++) {%> arr.push(<%= (horas != null && horas.length > h && horas[h]) ? "true" : "false"%>);
                        <% }%>
                                return arr;
                        })();

                        // utilidades de fecha 
                        function ymdToDate(y, m, d) {
                                return new Date(y, m - 1, d);
                        }
                        function todayYMD() {
                                var t = new Date();
                                return {y: t.getFullYear(), m: t.getMonth() + 1, d: t.getDate()};
                        }
                        function isPast(y, m, d) {
                                var t = todayYMD();
                                var sel = ymdToDate(y, m, d);
                                var hoy = ymdToDate(t.y, t.m, t.d);
                                // Comparar solo fecha (00:00)
                                return sel.getTime() < hoy.getTime();
                        }

                        /* ===== Validación y bitácoras ===== */
                        function validarReservaRango() {
                                var y = parseInt(document.getElementById('anioR').value, 10);
                                var m = parseInt(document.getElementById('mesR').value, 10);
                                var d = parseInt(document.getElementById('diaR').value, 10);

                                if (isPast(y, m, d)) {
                                        alert('No se permiten reservas en fechas anteriores a hoy.');
                                        return false;
                                }

                                var sd = document.getElementById('selDesde'), sh = document.getElementById('selHasta');
                                var de = parseInt(sd.value, 10), ha = parseInt(sh.value, 10);
                                if (isNaN(de) || isNaN(ha) || ha <= de) {
                                        alert('El rango de horas no es válido. "Hasta" debe ser mayor que "Desde".');
                                        return false;
                                }
                                for (var k = de; k < ha; k++) {
                                        if (!HORAS[k]) {
                                                alert('Hay horas no disponibles dentro del rango seleccionado.');
                                                return false;
                                        }
                                }
                                return true;
                        }
                        function prepararBitacoraReserva() {
                                if (!validarReservaRango())
                                        return false;
                                var metaEl = document.getElementById('b_meta_reserva');
                                var sd = document.getElementById('selDesde');
                                var sh = document.getElementById('selHasta');
                                var y = parseInt(document.getElementById('anioR').value, 10);
                                var m = parseInt(document.getElementById('mesR').value, 10);
                                var d = parseInt(document.getElementById('diaR').value, 10);
                                var areaId = <%= areaId%>;
                                var nombreArea = "<%= nombreArea.replace("\"", "\\\"")%>";
                                var fecha = (y.toString().padStart(4, '0') + '-' + m.toString().padStart(2, '0') + '-' + d.toString().padStart(2, '0'));
                                var meta = {mod: 'Reservas', evt: 'CREAR', areaId: areaId, area: nombreArea, fecha: fecha,
                                        desde: parseInt(sd.value, 10), hasta: parseInt(sh.value, 10),
                                        src: 'reservarAreas.jsp', evtId: mkEvtId(), csid: getCSID(), tz: tzOffset(),
                                        ua: (navigator.userAgent || ''), vp: vp()};
                                metaEl.value = j(meta);
                                return true;
                        }
                        function prepararBitacoraCancelacion(form, areaId, reservaId, fechaISO, desde, hasta) {
                                var metaEl = form.querySelector('input[name="b_meta"]');
                                var meta = {mod: 'Reservas', evt: 'CANCELAR', areaId: areaId, reservaId: reservaId,
                                        fecha: (fechaISO || ''), desde: parseInt(desde, 10), hasta: parseInt(hasta, 10),
                                        src: 'reservarAreas.jsp', evtId: mkEvtId(), csid: getCSID(), tz: tzOffset(),
                                        ua: (navigator.userAgent || ''), vp: vp()};
                                metaEl.value = j(meta);
                                return true;
                        }

                        /* ===== Ajuste dinámico de "Hasta" ===== */
                        (function () {
                                var sd = document.getElementById('selDesde'), sh = document.getElementById('selHasta');
                                if (sd && sh) {
                                        sd.addEventListener('change', function () {
                                                var d = parseInt(sd.value, 10), h = parseInt(sh.value, 10);
                                                if (h <= d)
                                                        sh.value = Math.min(d + 1, 24);
                                        });
                                }
                        })();

                        /* ===== Bloqueo del botón Reservar si fecha pasada (reactivo) ===== */
                        (function () {
                                var yEl = document.getElementById('anioR');
                                var mEl = document.getElementById('mesR');
                                var dEl = document.getElementById('diaR');
                                var btn = document.getElementById('btnReservar');
                                var desde = document.getElementById('selDesde');
                                var hasta = document.getElementById('selHasta');

                                function syncBtn() {
                                        var y = parseInt(yEl.value, 10), m = parseInt(mEl.value, 10), d = parseInt(dEl.value, 10);
                                        var past = isPast(y, m, d);
                                        if (btn) {
                                                btn.disabled = !!past;
                                                btn.title = past ? 'No se puede reservar en fechas pasadas' : '';
                                        }
                                        if (desde)
                                                desde.disabled = !!past;
                                        if (hasta)
                                                hasta.disabled = !!past;
                                }
                                if (yEl && mEl && dEl) {
                                        yEl.addEventListener('input', syncBtn);
                                        mEl.addEventListener('change', syncBtn);
                                        dEl.addEventListener('change', syncBtn);
                                        // Inicial
                                        syncBtn();
                                }
                        })();

                        /* ===== Animaciones ===== */
                        (function animateCards() {
                                var cards = document.querySelectorAll('.reserva-card');
                                cards.forEach(function (card, i) {
                                        card.style.opacity = '0';
                                        card.style.transform = 'translateY(20px)';
                                        setTimeout(function () {
                                                card.style.transition = 'opacity .4s, transform .4s';
                                                card.style.opacity = '1';
                                                card.style.transform = 'translateY(0)';
                                        }, i * 100);
                                });
                        })();
                        (function animateSlots() {
                                var slots = document.querySelectorAll('.timeline-slot');
                                slots.forEach(function (slot, i) {
                                        slot.style.opacity = '0';
                                        slot.style.transform = 'scale(0.9)';
                                        setTimeout(function () {
                                                slot.style.transition = 'opacity .3s, transform .3s';
                                                slot.style.opacity = '1';
                                                slot.style.transform = 'scale(1)';
                                        }, i * 40);
                                });
                        })();

                        /* ===== Scroll a alertas ===== */
                        (function () {
                                var alert = document.querySelector('.alert');
                                if (alert) {
                                        setTimeout(function () {
                                                alert.scrollIntoView({behavior: 'smooth', block: 'center'});
                                        }, 100);
                                }
                        })();

                        /* ===== Sincronizar areaId entre formularios ===== */
                        (function () {
                                var filtroAreaSelect = document.querySelector('form[method="get"] select[name="areaId"]');
                                var reservaAreaHidden = document.querySelector('form#frmReservar input[name="areaId"]');
                                if (filtroAreaSelect && reservaAreaHidden) {
                                        filtroAreaSelect.addEventListener('change', function () {
                                                reservaAreaHidden.value = filtroAreaSelect.value;
                                        });
                                        if (filtroAreaSelect.value !== reservaAreaHidden.value) {
                                                reservaAreaHidden.value = filtroAreaSelect.value;
                                        }
                                }
                        })();

                        /* ===== Modal accesible ===== */
                        document.addEventListener('DOMContentLoaded', function () {
                                var cards = document.querySelectorAll('.reserva-card');
                                for (var i = 0; i < cards.length; i++) {
                                        (function (card) {
                                                var btn = card.querySelector('.btn-cancelar');
                                                var form = card.querySelector('.frm-cancelar');
                                                if (!btn || !form)
                                                        return;

                                                btn.addEventListener('click', function () {
                                                        var areaId = parseInt(form.querySelector('input[name="areaId"]').value, 10);
                                                        var reservaId = parseInt(form.querySelector('input[name="reservaId"]').value, 10);
                                                        var fechaEl = card.querySelector('.reserva-date');
                                                        var fechaStr = fechaEl ? (fechaEl.textContent || '').trim() : '';
                                                        var tv = card.querySelectorAll('.reserva-time-value');
                                                        var desde = tv.length >= 2 ? parseInt((tv[0].textContent || '0').split(':')[0], 10) : 0;
                                                        var hasta = tv.length >= 2 ? parseInt((tv[1].textContent || '0').split(':')[0], 10) : 0;

                                                        prepararBitacoraCancelacion(form, areaId, reservaId, fechaStr, desde, hasta);

                                                        window.formActual = form;
                                                        var modal = document.getElementById('modalConfirm');
                                                        if (modal) {
                                                                modal.classList.add('active');
                                                                document.body.style.overflow = 'hidden';
                                                                setTimeout(function () {
                                                                        var firstBtn = modal.querySelector('.modal-btn-confirm') || modal.querySelector('button');
                                                                        if (firstBtn)
                                                                                try {
                                                                                        firstBtn.focus();
                                                                                } catch (e) {
                                                                                }
                                                                }, 0);
                                                        }
                                                });
                                        })(cards[i]);
                                }

                                document.addEventListener('keydown', function (e) {
                                        if (e.key === 'Escape')
                                                cerrarModal();
                                });
                                var modal = document.getElementById('modalConfirm');
                                if (modal) {
                                        modal.addEventListener('click', function (e) {
                                                if (e.target === modal)
                                                        cerrarModal();
                                        });
                                        // asegurarlo al body
                                        if (modal.parentNode !== document.body) {
                                                document.body.appendChild(modal);
                                        }
                                }
                        });

                        // Funciones globales de modal (firmas esperadas)
                        function cerrarModal() {
                                var modal = document.getElementById('modalConfirm');
                                if (modal) {
                                        modal.classList.remove('active');
                                        document.body.style.overflow = '';
                                }
                                window.formActual = null;
                        }
                        function confirmarAccion() {
                                if (window.formActual) {
                                        var f = window.formActual;
                                        cerrarModal();
                                        setTimeout(function () {
                                                f.submit();
                                        }, 120);
                                }
                        }
                </script>
        </body>
</html>
