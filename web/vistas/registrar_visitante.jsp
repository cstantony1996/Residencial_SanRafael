<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <title>Registrar visitante</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/tabla-usuarios.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/garita.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/visitante-form.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    </head>
    <body>
        <div class="wrap">
            <div class="card">

                <!-- Encabezado con botón Regresar -->
                <div class="card-head">
                    <div class="head-text">
                        <div class="title">Registro de visitantes</div>
                        <div class="subtitle">Genera un código QR para que tu visitante pueda acceder al residencial.</div>
                    </div>
                    <a class="btn btn-back" href="${pageContext.request.contextPath}/vistas/menuResidente.jsp">
                        <i class="fa-solid fa-arrow-left-long"></i> Regresar
                    </a>
                </div>

                <!-- Mensajes -->
                <c:if test="${not empty ok}">
                    <div class="alert success">
                        <i class="fa-solid fa-circle-check" style="color:#10b981;margin-top:2px"></i>
                        <div>
                            <div><strong>${ok}</strong></div>
                            <c:if test="${not empty qrDownloadUrl}">
                                <div class="qr-actions">
                                    <a class="btn btn-primary" href="${qrDownloadUrl}">
                                        <i class="fa-solid fa-download"></i> Descargar QR
                                    </a>
                                    <a class="btn btn-ghost" target="_blank" href="${qrViewUrl}">
                                        <i class="fa-regular fa-eye"></i> Ver QR
                                    </a>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </c:if>

                <c:if test="${not empty error}">
                    <div class="alert error">
                        <i class="fa-solid fa-triangle-exclamation" style="color:#f87171;margin-top:2px"></i>
                        <div>${error}</div>
                    </div>
                </c:if>

                <!-- Formulario -->
                <form method="post" action="${pageContext.request.contextPath}/visitante/registrar">
                    <div class="grid">
                        <div class="field">
                            <label class="label required"><i class="fa-regular fa-user"></i> Nombre del visitante</label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-regular fa-user"></i></span>
                                <input class="input" name="nombre" type="text" required autocomplete="off" />
                            </div>
                        </div>

                        <div class="field">
                            <label class="label"><i class="fa-regular fa-id-card"></i> DPI</label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-regular fa-id-card"></i></span>
                                <input
                                    class="input"
                                    name="dpi"
                                    type="text"
                                    inputmode="numeric"
                                    pattern="\d{13}"
                                    title="Debe contener exactamente 13 dígitos"
                                    maxlength="13"
                                    placeholder="Opcional"
                                    oninput="this.value=this.value.replace(/\D/g,'').slice(0,13)"
                                    autocomplete="off"
                                    />
                            </div>
                            <div class="hint">Exactamente 13 dígitos (sin guiones). Opcional.</div>
                        </div>

                        <div class="field">
                            <label class="label required"><i class="fa-regular fa-envelope"></i> Correo del visitante</label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-regular fa-envelope"></i></span>
                                <input class="input" name="correo" type="email" required autocomplete="off" />
                            </div>
                        </div>

                        <div class="field">
                            <label class="label"><i class="fa-solid fa-qrcode"></i> Tipo de QR</label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-solid fa-qrcode"></i></span>
                                <select class="select" name="tipoVisita" id="tipoVisita">
                                    <option value="intentos" selected>Por intentos</option>
                                    <option value="tiempo">Por tiempo</option>
                                </select>
                            </div>
                        </div>

                        <div class="field" id="f-intentos">
                            <label class="label required"><i class="fa-solid fa-list-ol"></i> Intentos permitidos</label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-solid fa-list-ol"></i></span>
                                <input
                                    class="input"
                                    id="intentosInput"
                                    name="intentos"
                                    type="number"
                                    min="2" max="10" step="2" value="2" required
                                    oninput="normalizarIntentos(this)"
                                    />
                            </div>
                            <div class="hint">Valores pares entre 2 y 10 (2, 4, 6, 8, 10).</div>
                        </div>

                        <div class="field" id="f-expira" style="display:none">
                            <label class="label required"><i class="fa-regular fa-clock"></i> Válido hasta</label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-regular fa-clock"></i></span>
                                <input class="input" id="expiraEn" name="expiraEn" type="datetime-local" />
                            </div>
                            <div class="hint">Fecha y hora límite para usar el QR.</div>
                        </div>
                    </div>

                    <div class="row-actions">
                        <button class="btn btn-primary" type="submit">
                            <i class="fa-solid fa-wand-magic-sparkles" style="margin-right:8px"></i> Generar QR
                        </button>
                        <button class="btn btn-ghost" type="reset" onclick="resetForm()">
                            <i class="fa-solid fa-eraser" style="margin-right:8px"></i> Limpiar
                        </button>
                        <span class="muted" style="margin-left:auto">* Obligatorios</span>
                    </div>
                </form>
            </div>
        </div>

        <script>
            (function () {
                const tipoSel = document.getElementById('tipoVisita');
                const fIntentos = document.getElementById('f-intentos');
                const fExpira = document.getElementById('f-expira');
                const expiraEn = document.getElementById('expiraEn');

                function toggleTipo() {
                    const byTime = (tipoSel.value === 'tiempo');
                    fIntentos.style.display = byTime ? 'none' : 'block';
                    fExpira.style.display = byTime ? 'block' : 'none';
                    fIntentos.querySelector('input').required = !byTime;
                    if (expiraEn)
                        expiraEn.required = byTime;

                    if (byTime && expiraEn) {
                        const now = new Date();
                        const pad = function (n) {
                            return String(n).padStart(2, '0');
                        };
                        
                        const local =
                                now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) +
                                'T' + pad(now.getHours()) + ':' + pad(now.getMinutes());
                        expiraEn.min = local;
                    }
                }

                function resetForm() {
                    setTimeout(function () {
                        tipoSel.value = 'intentos';
                        toggleTipo();
                        const intentos = document.getElementById('intentosInput');
                        if (intentos)
                            intentos.value = 2;
                    }, 0);
                }

                function normalizarIntentos(el) {
                    var v = parseInt(el.value || '0', 10);
                    if (isNaN(v))
                        v = 2;
                    v = Math.max(2, Math.min(10, v));
                    if (v % 2 !== 0)
                        v = v + 1;
                    el.value = v;
                }

                tipoSel.addEventListener('change', toggleTipo);
                toggleTipo();

                // Exponer reset/normalizar si los usas en atributos on*
                window.resetForm = resetForm;
                window.normalizarIntentos = normalizarIntentos;
            })();
        </script>

    </body>
</html>
