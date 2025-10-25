<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
        String ctx = request.getContextPath();
        String status = request.getParameter("status");
        boolean ok = "ok".equalsIgnoreCase(status);
        boolean err = "err".equalsIgnoreCase(status);

        // Datos guardados por el servlet para el comprobante
        String rmNombre = (String) session.getAttribute("rm_nombre");
        String rmTipo = (String) session.getAttribute("rm_tipo");
        String rmDesc = (String) session.getAttribute("rm_desc");
        String rmFecha = (String) session.getAttribute("rm_fecha");

        // Valor por defecto para datetime-local (YYYY-MM-DDTHH:mm)
        String nowIso = java.time.LocalDateTime.now().withSecond(0).withNano(0).toString();
%>
<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <title>Reporte de Mantenimiento</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/mantenimiento.css">
        </head>
        <body>
                <div class="dashboard">
                        <!-- Header -->
                        <header class="hero">
                                <div class="hero-left">
                                        <div class="hero-badge">🛠️</div>
                                        <div>
                                                <h1 class="title"><span class="gradient">Reporte de Mantenimiento</span></h1>
                                                <p class="subtitle">Describe el problema y envía el reporte a administración</p>
                                        </div>
                                </div>
                                <div class="hero-actions">
                                        <a class="btn btn-secondary" id="btnBack" href="<%= ctx%>/vistas/menuResidente.jsp">Regresar</a>
                                </div>
                        </header>

                        <main class="content">
                                <section class="card">
                                        <!-- Avisos -->
                                        <div id="alertOk" class="alert ok" role="status" style="<%= ok ? "" : "display:none;"%>">
                                                Tu reporte fue enviado a administración. ¡Gracias!
                                        </div>
                                        <div id="alertErr" class="alert err" role="alert" style="<%= err ? "" : "display:none;"%>">
                                                No se pudo enviar el correo. Intenta de nuevo.
                                        </div>

                                        <!-- Resumen post-envío -->
                                        <div class="card card--sub" style="<%= ok ? "" : "display:none;"%>">
                                                <h2 class="section-title">Resumen del reporte enviado</h2>
                                                <div class="table-wrap">
                                                        <table class="table">
                                                                <tbody>
                                                                        <tr><th>Residente</th><td><%= rmNombre == null ? "—" : rmNombre%></td></tr>
                                                                        <tr><th>Tipo</th><td><%= rmTipo == null ? "—" : rmTipo%></td></tr>
                                                                        <tr><th>Fecha y hora</th><td><%= rmFecha == null ? "—" : rmFecha%></td></tr>
                                                                        <tr><th>Descripción</th><td style="white-space:pre-wrap;"><%= rmDesc == null ? "—" : rmDesc%></td></tr>
                                                                </tbody>
                                                        </table>
                                                </div>
                                        </div>

                                        <!-- Formulario -->
                                        <form id="frmReporte"
                                              method="post"
                                              action="<%= ctx%>/reporteDeMantenimiento"
                                              accept-charset="UTF-8"
                                              novalidate>

                                                <div class="grid">
                                                        <div class="form-field">
                                                                <label class="label" for="tipo">Tipo de problema</label>
                                                                <select class="input" id="tipo" name="tipo" required>
                                                                        <option value="" disabled selected>Selecciona una opción…</option>
                                                                        <option value="lentitud del sistema">Lentitud del sistema</option>
                                                                        <option value="error al realizar una accion">Error al realizar una acción</option>
                                                                        <option value="error al acceder a una opcion">Error al acceder a una opción</option>
                                                                        <option value="error de visualizacion">Error de visualización</option>
                                                                        <option value="otros">Otros</option>
                                                                </select>

                                                                <div id="otrosBox" class="otros" aria-hidden="true" style="display:none;">
                                                                        <label class="label" for="otrosDetalle">Especificar “Otros”</label>
                                                                        <input class="input" type="text" id="otrosDetalle" name="otrosDetalle"
                                                                               maxlength="150" placeholder="Describe brevemente el tipo de problema">
                                                                        <div class="help">Máx. 150 caracteres.</div>
                                                                </div>
                                                        </div>

                                                        <div class="form-field">
                                                                <label class="label" for="descripcion">Descripción</label>
                                                                <textarea class="input textarea" id="descripcion" name="descripcion" required
                                                                          maxlength="4000"
                                                                          placeholder="Describe lo que ocurrió, pasos para reproducir, capturas, etc."></textarea>
                                                                <div class="help">Hasta 4000 caracteres.</div>
                                                        </div>

                                                        <div class="row">
                                                                <div class="form-field">
                                                                        <label class="label" for="fechaHora">Fecha y hora</label>
                                                                        <input class="input" type="datetime-local" id="fechaHora" name="fechaHora"
                                                                               value="<%= nowIso%>" required>
                                                                        <div class="help">Selecciona la fecha y la hora del incidente.</div>
                                                                </div>
                                                                <div class="form-field"></div>
                                                        </div>
                                                </div>

                                                <div class="actions">
                                                        <button type="reset" class="btn btn-danger" id="btnLimpiar">Limpiar</button>
                                                        <span class="spacer"></span>
                                                        <a id="btnDescargar" class="btn btn-secondary"
                                                           href="<%= ctx%>/reporteMantDescargar"
                                                           style="<%= ok ? "" : "display:none;"%>">Descargar reporte</a>
                                                        <button type="submit" class="btn btn-primary" id="btnEnviar">Enviar reporte</button>
                                                </div>
                                        </form>
                                </section>
                        </main>
                </div>

                <script>
                        (function () {
                                // Toggle “Otros”
                                var sel = document.getElementById('tipo'),
                                        box = document.getElementById('otrosBox'),
                                        inp = document.getElementById('otrosDetalle');

                                function toggleOtros() {
                                        var isOtros = sel.value === 'otros';
                                        box.style.display = isOtros ? 'block' : 'none';
                                        box.setAttribute('aria-hidden', isOtros ? 'false' : 'true');
                                        if (isOtros) {
                                                inp.setAttribute('required', 'required');
                                                if (!inp.maxLength)
                                                        inp.maxLength = 150;
                                        } else {
                                                inp.removeAttribute('required');
                                                inp.value = '';
                                        }
                                }
                                sel.addEventListener('change', toggleOtros);
                                toggleOtros();

                                // Validación mínima
                                document.getElementById('frmReporte').addEventListener('submit', function (e) {
                                        var tipo = sel.value;
                                        var desc = (document.getElementById('descripcion').value || '').trim();
                                        var fecha = (document.getElementById('fechaHora').value || '').trim();

                                        if (!tipo) {
                                                alert('Selecciona el tipo de problema.');
                                                e.preventDefault();
                                                return;
                                        }
                                        if (tipo === 'otros' && !(inp.value || '').trim()) {
                                                alert('Especifica “Otros”.');
                                                e.preventDefault();
                                                return;
                                        }
                                        if (!desc) {
                                                alert('La descripción es obligatoria.');
                                                e.preventDefault();
                                                return;
                                        }
                                        if (!fecha) {
                                                alert('Selecciona fecha y hora del incidente.');
                                                e.preventDefault();
                                                return;
                                        }
                                });

                                // Reaplicar UI tras limpiar
                                document.getElementById('btnLimpiar').addEventListener('click', function () {
                                        setTimeout(toggleOtros, 0);
                                });
                        })();
                </script>
        </body>
</html>
