<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- ====== Back URL según el rol guardado por LoginServlet (sessionScope.rol) ====== --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="roleKey" value="${sessionScope.rol}" />
<c:choose>
        <c:when test="${roleKey == 'ADMIN'}">
                <c:set var="backUrl" value="${ctx}/vistas/menuAdmin.jsp"/>
        </c:when>
        <c:when test="${roleKey == 'GUARDIA'}">
                <c:set var="backUrl" value="${ctx}/vistas/menuGuardia.jsp"/>
        </c:when>
        <c:otherwise>
                <c:set var="backUrl" value="${ctx}/vistas/menuResidente.jsp"/>
        </c:otherwise>
</c:choose>

<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <title>Directorio Residencial</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="${ctx}/CSS/tabla-usuarios.css">
                <link rel="stylesheet" href="${ctx}/CSS/directorio.css">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
        </head>
        <body>
                <c:set var="isSearch" value="${not empty f.nombre or not empty f.apellidos or not empty f.lote or not empty f.numeroCasa}" />

                <div class="dir-shell">
                        <div class="card">
                                <!-- Encabezado -->
                                <div class="card-head">
                                        <div class="head-left">
                                                <div class="badge"><i class="fa-solid fa-address-book"></i></div>
                                                <div>
                                                        <h1 class="title">Directorio Residencial</h1>
                                                        <div class="subtitle">Consulta por nombre, apellidos, lote o número de casa.</div>
                                                </div>
                                        </div>

                                        <!-- Botón Regresar dependiente del rol -->
                                        <a class="btn btn-back" href="${backUrl}">
                                                <i class="fa-solid fa-arrow-left-long"></i> Regresar
                                        </a>
                                </div>

                                <!-- Mensajes de error -->
                                <c:if test="${not empty error}">
                                        <div class="alert error">
                                                <i class="fa-solid fa-triangle-exclamation"></i>
                                                <div>${error}</div>
                                                <button type="button" class="alert-close" aria-label="Cerrar">&times;</button>
                                        </div>
                                </c:if>

                                <!-- 🔹 Nuevo: mensaje cuando no hay resultados -->
                                <c:if test="${not empty dirEmptyMsg}">
                                        <div class="alert info" style="text-align:center; margin-top:10px;">
                                                <i class="fa-solid fa-circle-info"></i>
                                                <div>${dirEmptyMsg}</div>
                                        </div>
                                </c:if>

                                <!-- Formulario -->
                                <form class="filter-form" method="get" action="${ctx}/directorio" autocomplete="off">
                                        <div class="grid">
                                                <div class="f-group">
                                                        <label class="f-label"><i class="fa-regular fa-user"></i> Nombres</label>
                                                        <div class="input-wrap">
                                                                <span class="input-icon"><i class="fa-regular fa-user"></i></span>
                                                                <input class="input" id="nombre" name="nombre" type="text" value="${fn:escapeXml(f.nombre)}">
                                                        </div>
                                                </div>

                                                <div class="f-group">
                                                        <label class="f-label"><i class="fa-regular fa-id-card"></i> Apellidos</label>
                                                        <div class="input-wrap">
                                                                <span class="input-icon"><i class="fa-regular fa-id-card"></i></span>
                                                                <input class="input" id="apellidos" name="apellidos" type="text" value="${fn:escapeXml(f.apellidos)}">
                                                        </div>
                                                </div>

                                                <div class="f-group">
                                                        <label class="f-label"><i class="fa-solid fa-layer-group"></i> Lote</label>
                                                        <div class="input-wrap">
                                                                <span class="input-icon"><i class="fa-solid fa-layer-group"></i></span>
                                                                <select class="select" id="lote" name="lote">
                                                                        <option value="">(sin lote)</option>
                                                                        <c:set var="alphabet" value="ABCDEFGHIJKLMNOPQRSTUVWXYZ"/>
                                                                        <c:forEach var="i" begin="0" end="${fn:length(alphabet) - 1}">
                                                                                <c:set var="opt" value="${fn:substring(alphabet, i, i+1)}" />
                                                                                <option value="${opt}" <c:if test="${f.lote==opt}">selected</c:if>>${opt}</option>
                                                                        </c:forEach>
                                                                </select>
                                                        </div>
                                                </div>

                                                <div class="f-group">
                                                        <label class="f-label"><i class="fa-solid fa-house-chimney"></i> Número de casa</label>
                                                        <div class="input-wrap">
                                                                <span class="input-icon"><i class="fa-solid fa-house-chimney"></i></span>
                                                                <select class="select" id="numero_casa" name="numero_casa">
                                                                        <option value="">(sin número)</option>
                                                                        <c:forEach var="n" begin="1" end="200">
                                                                                <option value="${n}" <c:if test="${f.numeroCasa==n}">selected</c:if>>${n}</option>
                                                                        </c:forEach>
                                                                </select>
                                                        </div>
                                                </div>

                                                <div class="f-group small">
                                                        <label class="f-label"><i class="fa-solid fa-list-ol"></i> Por página</label>
                                                        <div class="input-wrap">
                                                                <span class="input-icon"><i class="fa-solid fa-list-ol"></i></span>
                                                                <select class="select" id="size" name="size">
                                                                        <c:forEach var="s" items="${[10,20,50]}">
                                                                                <option value="${s}" <c:if test="${f.size==s}">selected</c:if>>${s}</option>
                                                                        </c:forEach>
                                                                </select>
                                                        </div>
                                                </div>
                                        </div>

                                        <div class="form-actions">
                                                <button class="btn btn-primary" type="submit"><i class="fa-solid fa-magnifying-glass"></i> Buscar</button>
                                                <a class="btn btn-ghost" href="${ctx}/directorio"><i class="fa-solid fa-broom-wide"></i> Limpiar</a>
                                                <input type="hidden" name="page" value="${f.page}"/>
                                        </div>
                                </form>

                                <!-- Resultados -->
                                <c:if test="${not empty resultados}">
                                        <div class="results-head">
                                                <div class="muted">Total: <strong>${total}</strong></div>
                                        </div>

                                        <div class="table-wrap">
                                                <table class="dir-table">
                                                        <thead>
                                                                <tr>
                                                                        <th>Nombre</th>
                                                                        <th>Apellidos</th>
                                                                                <c:if test="${isSearch}">
                                                                                <th>Correo</th>
                                                                                </c:if>
                                                                        <th>Lote</th>
                                                                        <th>No. Casa</th>
                                                                </tr>
                                                        </thead>
                                                        <tbody>
                                                                <c:forEach var="r" items="${resultados}">
                                                                        <tr>
                                                                                <td>${r.nombre}</td>
                                                                                <td>${r.apellidos}</td>
                                                                                <c:if test="${isSearch}">
                                                                                        <td>${r.correo}</td>
                                                                                </c:if>
                                                                                <td>${r.lote}</td>
                                                                                <td>${r.numeroCasa != null ? r.numeroCasa : '-'}</td>
                                                                        </tr>
                                                                </c:forEach>
                                                        </tbody>
                                                </table>
                                        </div>

                                        <!-- Paginación -->
                                        <c:set var="totalPages" value="${(total + f.size - 1) / f.size}" />
                                        <div class="pager">
                                                <div class="muted">Página <strong>${f.page}</strong> de <strong>${totalPages}</strong></div>
                                                <div class="pager-right">
                                                        <c:if test="${f.page > 1}">
                                                                <a class="btn"
                                                                   href="${ctx}/directorio?nombre=${fn:escapeXml(f.nombre)}&apellidos=${fn:escapeXml(f.apellidos)}&lote=${f.lote}&numero_casa=${f.numeroCasa}&size=${f.size}&page=${f.page-1}">
                                                                        Anterior
                                                                </a>
                                                        </c:if>
                                                        <c:if test="${f.page < totalPages}">
                                                                <a class="btn"
                                                                   href="${ctx}/directorio?nombre=${fn:escapeXml(f.nombre)}&apellidos=${fn:escapeXml(f.apellidos)}&lote=${f.lote}&numero_casa=${f.numeroCasa}&size=${f.size}&page=${f.page+1}">
                                                                        Siguiente
                                                                </a>
                                                        </c:if>
                                                </div>
                                        </div>
                                </c:if>

                                <c:if test="${empty resultados && empty error && empty dirEmptyMsg}">
                                        <p class="muted" style="margin-top:10px">Ingresa criterios y pulsa <strong>Buscar</strong>.</p>
                                </c:if>
                        </div>
                </div>

                <!-- Auto-cierre del mensaje -->
                <script>
                        (function () {
                                var alerts = document.querySelectorAll('.alert');
                                alerts.forEach(function (el) {
                                        var btn = el.querySelector('.alert-close');
                                        if (btn) {
                                                btn.addEventListener('click', function () {
                                                        el.classList.add('fade-out');
                                                        setTimeout(function () {
                                                                if (el && el.parentNode)
                                                                        el.parentNode.removeChild(el);
                                                        }, 350);
                                                });
                                        }
                                        setTimeout(function () {
                                                el.classList.add('fade-out');
                                                setTimeout(function () {
                                                        if (el && el.parentNode)
                                                                el.parentNode.removeChild(el);
                                                }, 350);
                                        }, 4000);
                                });
                        })();
                </script>
        </body>
</html>
