<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <title>Lista de Usuarios</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <!-- Base (si la usas) -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/tabla-usuarios.css">
        <!-- Estilos de esta vista -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/lista-usuarios.css">

        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    </head>
    <body>

        <div class="usr-wrap">
            <!-- Header -->
            <header class="usr-header">
                <div class="hdr-left">
                    <div class="hdr-badge"><i class="fa-solid fa-users"></i></div>
                    <div>
                        <h1 class="hdr-title">Lista de Usuarios</h1>
                        <p class="hdr-sub">Gestiona y visualiza los usuarios del sistema.</p>
                    </div>
                </div>

                <div class="hdr-right">
                    <!-- Selector de tamaño (opcional) -->
                    <form method="get" action="UsuarioController" class="size-form">
                        <input type="hidden" name="accion" value="listar">
                        <label for="sizeSel" class="muted">Por página</label>
                        <select id="sizeSel" name="size" onchange="this.form.submit()">
                            <c:set var="pageSize" value="${empty param.size ? (empty size ? 10 : size) : param.size}" />
                            <c:forEach var="s" items="${[10,20,50]}">
                                <option value="${s}" <c:if test="${pageSize == s}">selected</c:if>>${s}</option>
                            </c:forEach>
                        </select>
                    </form>

                    <a class="btn btn-back" href="<c:url value='/vistas/menuAdmin.jsp'/>">
                        <i class="fa-solid fa-arrow-left-long"></i> Regresar
                    </a>
                </div>
            </header>

            <!-- Flash message -->
            <c:if test="${not empty sessionScope.flash_msg}">
                <div id="flash-msg" class="alert alert-${empty sessionScope.flash_type ? 'info' : sessionScope.flash_type}">
                    ${sessionScope.flash_msg}
                    <button type="button" class="alert-close" aria-label="Cerrar">&times;</button>
                </div>
                <c:remove var="flash_msg" scope="session"/>
                <c:remove var="flash_type" scope="session"/>
                <script>
                    (function () {
                        var el = document.getElementById('flash-msg');
                        if (!el)
                            return;
                        var btn = el.querySelector('.alert-close');
                        if (btn)
                            btn.addEventListener('click', function () {
                                el.remove();
                            });
                        setTimeout(function () {
                            if (!el)
                                return;
                            el.classList.add('fade-out');
                            setTimeout(function () {
                                el && el.remove();
                            }, 350);
                        }, 4000);
                    })();
                </script>
            </c:if>

            <!-- Tabla -->
            <c:if test="${not empty usuarios}">
                <div class="usr-table-card">
                    <div class="tbl-head">
                        <i class="fa-solid fa-table"></i>
                        <span>Usuarios</span>
                        <span class="muted" style="margin-left:auto">Total: ${empty total ? fn:length(usuarios) : total}</span>
                    </div>

                    <div class="tbl-wrap">
                        <table class="usr-table">
                            <thead>
                                <tr>
                                    <!-- ID eliminado -->
                                    <th>DPI</th>
                                    <th>Nombre</th>
                                    <th>Apellidos</th>
                                    <th class="hide-sm">Correo</th>
                                    <th class="col-rol">Rol</th>
                                    <th class="hide-sm">Lote</th>
                                    <th class="hide-sm">No. Casa</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="usuario" items="${usuarios}">
                                    <tr>
                                        <td>${usuario.dpi}</td>

                                        <td>
                                            <div class="user-info">
                                                <div class="user-avatar">
                                                    <c:out value="${empty usuario.nombre ? 'U' : fn:toUpperCase(fn:substring(usuario.nombre,0,1))}" />
                                                </div>
                                                <div class="user-details">
                                                    <div class="user-name">${usuario.nombre}</div>
                                                    <div class="user-email hide-md">
                                                        <span class="truncate tooltip" data-tooltip="${usuario.correo}">${usuario.correo}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>

                                        <td>${usuario.apellidos}</td>
                                        <td class="hide-sm">
                                            <span class="truncate tooltip" data-tooltip="${usuario.correo}">${usuario.correo}</span>
                                        </td>

                                        <td class="col-rol">
                                            <span class="badge-role" data-role="${usuario.rol}">${usuario.rol}</span>
                                        </td>

                                        <td class="hide-sm">${usuario.lote}</td>
                                        <td class="hide-sm">${usuario.numeroCasa != null ? usuario.numeroCasa : '-'}</td>

                                        <td>
                                            <div class="action-buttons">
                                                <a class="btn-icon btn-edit"
                                                   href="UsuarioController?accion=editar&dpi=${usuario.dpi}" title="Editar">
                                                    <i class="fas fa-pen-to-square"></i>
                                                </a>
                                                <a class="btn-icon btn-delete"
                                                   href="UsuarioController?accion=eliminar&dpi=${usuario.dpi}"
                                                   title="Eliminar"
                                                   onclick="return confirm('¿Está seguro de eliminar este usuario?')">
                                                    <i class="fas fa-trash"></i>
                                                </a>
                                                <a class="btn-icon btn-vehiculos"
                                                   href="VehiculoController?accion=listar&usuarioId=${usuario.id}" title="Vehículos">
                                                    <i class="fas fa-car"></i>
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>

                    <!-- Paginación (si el controlador expone total/page/size) -->
                    <c:if test="${not empty total}">
                        <c:set var="page" value="${empty param.page ? (empty page ? 1 : page) : param.page}" />
                        <c:set var="size" value="${empty param.size ? (empty size ? 10 : size) : param.size}" />
                        <c:set var="totalPages" value="${(total + size - 1) / size}" />

                        <nav class="pager">
                            <div class="pager-left">
                                Mostrando página ${page} de ${totalPages} — Total: ${total}
                            </div>
                            <div class="pager-right">
                                <c:if test="${page > 1}">
                                    <a class="page-btn" href="UsuarioController?accion=listar&page=${page-1}&size=${size}">Anterior</a>
                                </c:if>
                                <c:forEach var="p" begin="1" end="${totalPages}">
                                    <a class="page-btn <c:if test='${p==page}'>active</c:if>'"
                                       href="UsuarioController?accion=listar&page=${p}&size=${size}">${p}</a>
                                </c:forEach>
                                <c:if test="${page < totalPages}">
                                    <a class="page-btn" href="UsuarioController?accion=listar&page=${page+1}&size=${size}">Siguiente</a>
                                </c:if>
                            </div>
                        </nav>
                    </c:if>
                </div>
            </c:if>

            <c:if test="${empty usuarios}">
                <div class="usr-empty">
                    <i class="fa-regular fa-face-frown"></i>
                    <div>No hay usuarios registrados.</div>
                </div>
            </c:if>
        </div>

    </body>
</html>
