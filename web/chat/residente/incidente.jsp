<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
        String ctx = request.getContextPath();
%>
<!doctype html>
<html lang="es">
        <head>
                <meta charset="utf-8">
                <title>Reportar incidente</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/incidente.css">
        </head>
        <body>
                <div class="dashboard">
                        <!-- Header -->
                        <header class="hero">
                                <div class="hero-left">
                                        <div class="hero-badge">🚨</div>
                                        <div>
                                                <h1 class="title"><span class="gradient">Reportar incidente</span></h1>
                                                <p class="subtitle">Completa los campos y presiona “Guardar”.</p>
                                        </div>
                                </div>
                                <div class="hero-actions">
                                        <a class="btn btn-secondary" href="<%= ctx%>/vistas/comunicacion.jsp">← Volver</a>
                                </div>
                        </header>

                        <!-- Form Card -->
                        <main class="content">
                                <section class="card">
                                        <form id="frm" autocomplete="off">
                                                <div class="form-grid">
                                                        <div class="form-field">
                                                                <label class="label" for="tipo">Tipo de incidente</label>
                                                                <select class="input" id="tipo" required>
                                                                        <option value="">Seleccione…</option>
                                                                        <option>Disturbios</option>
                                                                        <option>Ruido</option>
                                                                        <option>Accidente vehicular</option>
                                                                        <option>Daños inmobiliarios</option>
                                                                        <option>Otros</option>
                                                                </select>
                                                        </div>
                                                        <div class="form-field">
                                                                <label class="label" for="when">Fecha y hora</label>
                                                                <input class="input" id="when" type="datetime-local" required>
                                                        </div>

                                                        <div class="form-field form-field--full">
                                                                <label class="label" for="desc">
                                                                        Descripción <span class="counter" id="cnt">0/200</span>
                                                                </label>
                                                                <textarea class="input textarea" id="desc" maxlength="200"
                                                                          placeholder="Describe lo ocurrido (máx. 200 caracteres)" required></textarea>
                                                                <div class="hint">Evita datos sensibles; añade solo lo necesario para que seguridad actúe.</div>
                                                        </div>
                                                </div>

                                                <div class="actions">
                                                        <button type="submit" class="btn btn-primary" id="btnSave">Guardar</button>
                                                        <a class="btn btn-secondary" href="<%= ctx%>/vistas/comunicacion.jsp">Cancelar</a>
                                                </div>

                                                <div class="status" id="status" role="alert" aria-live="polite"></div>
                                        </form>
                                </section>
                        </main>
                </div>

                <script>
                        (function () {
                                const ctx = "<%= ctx%>";
                                const frm = document.getElementById('frm');
                                const tipo = document.getElementById('tipo');
                                const when = document.getElementById('when');
                                const desc = document.getElementById('desc');
                                const btn = document.getElementById('btnSave');
                                const status = document.getElementById('status');
                                const cnt = document.getElementById('cnt');

                                // Contador de caracteres
                                function updateCnt() {
                                        cnt.textContent = (desc.value || '').length + "/200";
                                }
                                desc.addEventListener('input', updateCnt);
                                updateCnt();

                                function showStatus(ok, msg) {
                                        status.className = 'status ' + (ok ? 'ok' : 'err');
                                        status.textContent = msg || '';
                                }
                                function clearStatus() {
                                        status.className = 'status';
                                        status.textContent = '';
                                }

                                frm.addEventListener('submit', function (ev) {
                                        ev.preventDefault();
                                        clearStatus();

                                        const t = (tipo.value || '').trim();
                                        const w = (when.value || '').trim();
                                        const d = (desc.value || '').trim();

                                        if (!t)
                                                return showStatus(false, 'Seleccione el tipo de incidente');
                                        if (!w)
                                                return showStatus(false, 'Indique fecha y hora');
                                        if (!d || d.length > 200)
                                                return showStatus(false, 'Descripción requerida (máx. 200)');

                                        btn.disabled = true;

                                        fetch(ctx + '/api/chat/incidentes', {
                                                method: 'POST',
                                                headers: {'Content-Type': 'application/json'},
                                                body: JSON.stringify({incidentType: t, occuredAt: w, description: d})
                                        })
                                                .then(r => r.ok ? r.json() : r.json().catch(() => ({})).then(j => Promise.reject(j)))
                                                .then(j => {
                                                        showStatus(true, 'Se ha creado el incidente con éxito (ID ' + j.incidentId + ').');
                                                        tipo.value = '';
                                                        when.value = '';
                                                        desc.value = '';
                                                        updateCnt();
                                                })
                                                .catch(err => {
                                                        const msg = (err && (err.error || err.message)) ? (err.error || err.message)
                                                                : 'No se pudo crear el incidente';
                                                        showStatus(false, msg);
                                                })
                                                .finally(() => {
                                                        btn.disabled = false;
                                                });
                                });
                        })();
                </script>
        </body>
</html>
