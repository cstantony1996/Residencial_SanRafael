<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");
%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <title>${usuario != null ? 'Editar' : 'Nuevo'} Usuario</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <!-- (Mantén si lo necesitas para otros módulos) -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/tabla-usuarios.css">
        <!-- Estilos de este formulario (sobrescriben lo anterior) -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/formulario-usuarios.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    </head>
    <body>
        <div class="usrf-wrap">

            <!-- Header -->
            <div class="usrf-header">
                <div class="hdr-left">
                    <div class="hdr-badge"><i class="fa-solid fa-user-plus"></i></div>
                    <div>
                        <h1 class="hdr-title">
                            <c:choose>
                                <c:when test="${usuario != null}">Editar Usuario</c:when>
                                <c:otherwise>Nuevo Usuario</c:otherwise>
                            </c:choose>
                        </h1>
                        <p class="hdr-sub">Completa los datos del usuario del residencial.</p>
                    </div>
                </div>
                <div class="hdr-right">
                    <a class="btn btn-back" href="<c:url value='/vistas/menuAdmin.jsp'/>">
                        <i class="fa-solid fa-arrow-left-long"></i> Regresar
                    </a>
                </div>
            </div>

            <!-- Mensajes -->
            <c:if test="${not empty error}">
                <div class="alert alert-danger"><i class="fa-solid fa-triangle-exclamation"></i> ${error}</div>
            </c:if>
            <c:if test="${not empty param.mensaje}">
                <div class="alert alert-success"><i class="fa-solid fa-circle-check"></i> ${param.mensaje}</div>
            </c:if>

            <!-- Card del formulario (una columna) -->
            <div class="usrf-card">
                <form id="usuarioForm" action="UsuarioController" method="post" novalidate>
                    <c:if test="${usuario != null}">
                        <input type="hidden" name="id" value="${usuario.id}">
                        <input type="hidden" name="accion" value="actualizar">
                    </c:if>
                    <c:if test="${usuario == null}">
                        <input type="hidden" name="accion" value="insertar">
                    </c:if>

                    <!-- DPI -->
                    <div class="field">
                        <label class="label">DPI Usuario<span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-regular fa-id-card"></i></span>
                            <input class="input" id="dpi" name="dpi"
                                   value="${usuario != null ? usuario.dpi : ''}"
                                   maxlength="13" required
                                   ${usuario != null ? 'readonly' : ''}/>
                        </div>
                        <div class="hint">Ingrese el DPI sin guiones (13 dígitos).</div>
                    </div>

                    <!-- Correo -->
                    <div class="field">
                        <label class="label">Correo electrónico <span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-regular fa-envelope"></i></span>
                            <input class="input" id="correo" name="correo" type="email"
                                   maxlength="100" required
                                   value="${usuario != null ? usuario.correo : ''}"/>
                        </div>
                    </div>

                    <!-- Nombre -->
                    <div class="field">
                        <label class="label">Nombre del Usuario<span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-regular fa-user"></i></span>
                            <input class="input" id="nombre" name="nombre"
                                   maxlength="50" required
                                   value="${usuario != null ? usuario.nombre : ''}"/>
                        </div>
                    </div>

                    <!-- Apellidos -->
                    <div class="field">
                        <label class="label">Apellidos del Usuario<span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-solid fa-user-pen"></i></span>
                            <input class="input" id="apellidos" name="apellidos"
                                   maxlength="50" required
                                   value="${usuario != null ? usuario.apellidos : ''}"/>
                        </div>
                    </div>

                    <!-- Contraseña (solo al crear) -->
                    <c:if test="${usuario == null}">
                        <div class="field">
                            <label class="label">Contraseña <span class="req">*</span></label>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-solid fa-key"></i></span>
                                <input class="input" id="password" name="password"
                                       type="password" minlength="6" maxlength="50" required/>
                            </div>
                            <div class="hint">Mínimo 6 caracteres.</div>
                        </div>
                    </c:if>

                    <!-- Rol -->
                    <div class="field">
                        <label class="label">Rol <span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-solid fa-user-gear"></i></span>
                            <select class="select" id="rol" name="rol" required>
                                <option value="">Seleccione un rol</option>
                                <option value="administrador" ${usuario != null && usuario.rol == 'administrador' ? 'selected' : ''}>Administrador de residencial</option>
                                <option value="agente" ${usuario != null && usuario.rol == 'agente' ? 'selected' : ''}>Agente de seguridad de residencial</option>
                                <option value="residente" ${usuario != null && usuario.rol == 'residente' ? 'selected' : ''}>Residente</option>
                            </select>
                        </div>
                    </div>

                    <!-- Lote -->
                    <div class="field">
                        <label class="label">Lote <span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-solid fa-layer-group"></i></span>
                            <select class="select" id="lote" name="lote" required>
                                <option value="">Seleccione un lote</option>
                                <c:forTokens var="letra" items="A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z" delims=",">
                                    <option value="${letra}" ${usuario != null && usuario.lote == letra ? 'selected' : ''}>Lote ${letra}</option>
                                </c:forTokens>
                            </select>
                        </div>
                    </div>

                    <!-- Número de casa -->
                    <div class="field">
                        <label class="label">Número de casa <span class="req">*</span></label>
                        <div class="input-group">
                            <span class="input-icon"><i class="fa-solid fa-house"></i></span>
                            <select class="select" id="numero_casa" name="numero_casa" required>
                                <option value="">Seleccione un número</option>
                                <c:forEach var="i" begin="1" end="50">
                                    <option value="${i}" ${usuario != null && usuario.numeroCasa == i ? 'selected' : ''}>Casa ${i}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <!-- Acciones -->
                    <div class="form-actions">
                        <button id="btnSubmit" type="submit" class="btn btn-primary" disabled>
                            <c:choose>
                                <c:when test="${usuario != null}">
                                    <i class="fa-solid fa-floppy-disk"></i> Actualizar Usuario
                                </c:when>
                                <c:otherwise>
                                    <i class="fa-solid fa-user-plus"></i> Crear Usuario
                                </c:otherwise>
                            </c:choose>
                        </button>
                        <a class="btn btn-secondary" href="<c:url value='/vistas/menuAdmin.jsp'/>">
                            <i class="fa-solid fa-xmark"></i> Cancelar
                        </a>
                    </div>
                </form>
            </div>
        </div>

        <script>
            // DPI numérico
            (function () {
                var dpi = document.getElementById('dpi');
                if (dpi && !dpi.readOnly) {
                    dpi.addEventListener('input', function (e) {
                        e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 13);
                    });
                }
            })();

            // Email feedback
            (function () {
                var el = document.getElementById('correo');
                if (!el)
                    return;
                el.addEventListener('blur', function (e) {
                    var v = e.target.value.trim();
                    var ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
                    e.target.style.borderColor = ok || !v ? '' : '#ef4444';
                    e.target.style.boxShadow = ok || !v ? '' : '0 0 0 3px rgba(239,68,68,.25)';
                });
            })();

            // Validación + rol agente
            (function () {
                var form = document.getElementById('usuarioForm');
                var btn = document.getElementById('btnSubmit');
                var rol = document.getElementById('rol');
                var lote = document.getElementById('lote');
                var casa = document.getElementById('numero_casa');

                function toggleRol() {
                    var agente = rol && rol.value === 'agente';
                    if (!lote || !casa)
                        return;
                    lote.disabled = agente;
                    casa.disabled = agente;
                    lote.required = !agente;
                    casa.required = !agente;
                    if (agente) {
                        lote.value = '';
                        casa.value = '';
                    }
                }
                function updateBtn() {
                    if (btn)
                        btn.disabled = !form.checkValidity();
                }

                if (form) {
                    form.addEventListener('input', updateBtn);
                    form.addEventListener('change', updateBtn);
                    form.addEventListener('submit', function (e) {
                        if (!form.checkValidity()) {
                            e.preventDefault();
                            updateBtn();
                            return;
                        }
                        btn.classList.add('loading');
                        btn.style.pointerEvents = 'none';
                    });
                }
                if (rol)
                    rol.addEventListener('change', function () {
                        toggleRol();
                        updateBtn();
                    });

                toggleRol();
                updateBtn();
            })();
        </script>
    </body>
</html>
