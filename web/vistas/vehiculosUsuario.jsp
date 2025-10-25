<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Vehículos del Usuario</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <!-- Base y estilos del módulo -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/tabla-usuarios.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/vehiculos.css">
    <!-- Iconos -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<div class="veh-wrap">

    <!-- Header -->
    <header class="veh-header">
        <div class="hdr-left">
            <div class="hdr-badge"><i class="fa-solid fa-car-side"></i></div>
            <div>
                <h1 class="hdr-title">Vehículos del usuario</h1>
                <div class="hdr-sub">Gestiona y consulta los vehículos activos del usuario #${usuarioId}.</div>
            </div>
        </div>
        <div class="hdr-right">
            <a class="btn btn-back" href="${pageContext.request.contextPath}/UsuarioController">
                <i class="fa-solid fa-arrow-left-long"></i> Volver
            </a>
        </div>
    </header>

    <!-- Alertas flash -->
    <c:if test="${not empty sessionScope.flash_msg}">
        <div id="flash-msg"
             class="alert 
             ${sessionScope.flash_type == 'success' ? 'alert-success' :
               sessionScope.flash_type == 'warning' ? 'alert-warning' :
               sessionScope.flash_type == 'danger'  ? 'alert-danger'  : 'alert-info'}">
            ${sessionScope.flash_msg}
            <button type="button" class="alert-close" aria-label="Cerrar">&times;</button>
        </div>
        <c:remove var="flash_msg" scope="session"/>
        <c:remove var="flash_type" scope="session"/>

        <script>
            (function () {
                var el = document.getElementById('flash-msg');
                if (!el) return;
                var btn = el.querySelector('.alert-close');
                if (btn) btn.addEventListener('click', function(){ el.remove(); });
                setTimeout(function(){
                    el.classList.add('fade-out');
                    setTimeout(function(){ el && el.remove(); }, 350);
                }, 3500);
            })();
        </script>
    </c:if>

    <!-- Tarjeta: agregar vehículo -->
    <section class="veh-card">
        <div class="veh-card-head">
            <div class="card-icon"><i class="fa-solid fa-square-plus"></i></div>
            <div class="card-head-text">
                <div class="card-title">Agregar vehículo</div>
                <div class="card-sub">La placa se normaliza a MAYÚSCULAS y sin espacios.</div>
            </div>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/VehiculoController" autocomplete="off">
            <input type="hidden" name="accion" value="agregar">
            <input type="hidden" name="usuarioId" value="${usuarioId}">

            <div class="veh-grid">
                <div class="field">
                    <label class="label"><i class="fa-solid fa-id-card-clip"></i> Placa <span class="req">*</span></label>
                    <div class="input-group">
                        <span class="input-icon"><i class="fa-solid fa-tag"></i></span>
                        <input class="input" name="placa" placeholder="P123ABC" required
                               oninput="this.value=this.value.toUpperCase().replace(/\s+/g,'')">
                    </div>
                </div>

                <div class="field">
                    <label class="label"><i class="fa-solid fa-industry"></i> Marca</label>
                    <div class="input-group">
                        <span class="input-icon"><i class="fa-solid fa-industry"></i></span>
                        <input class="input" name="marca" placeholder="Toyota">
                    </div>
                </div>

                <div class="field">
                    <label class="label"><i class="fa-solid fa-clipboard-list"></i> Modelo</label>
                    <div class="input-group">
                        <span class="input-icon"><i class="fa-solid fa-clipboard-list"></i></span>
                        <input class="input" name="modelo" placeholder="Corolla">
                    </div>
                </div>

                <div class="field">
                    <label class="label"><i class="fa-solid fa-palette"></i> Color</label>
                    <div class="input-group">
                        <span class="input-icon"><i class="fa-solid fa-palette"></i></span>
                        <input class="input" name="color" placeholder="Negro">
                    </div>
                </div>
            </div>

            <div class="row-actions">
                <button class="btn btn-primary" type="submit">
                    <i class="fa-solid fa-plus"></i> Agregar
                </button>
                <button class="btn btn-ghost" type="reset">
                    <i class="fa-solid fa-eraser"></i> Limpiar
                </button>
            </div>
        </form>
    </section>

    <!-- Tabla -->
    <c:if test="${not empty vehiculos}">
        <section class="veh-table-card">
            <div class="tbl-head">
                <i class="fa-solid fa-table-list" style="opacity:.9"></i>
                <span>Vehículos activos</span>
                <span class="muted" style="margin-left:auto">Total: ${fn:length(vehiculos)}</span>
            </div>

            <div class="tbl-wrap">
                <table class="veh-table">
                    <thead>
                    <tr>
                        <th class="hide-sm">ID</th>
                        <th>Placa</th>
                        <th>Marca</th>
                        <th>Modelo</th>
                        <th>Color</th>
                        <th class="hide-md">Creado</th>
                        <th>Acciones</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="v" items="${vehiculos}">
                        <tr>
                            <td class="hide-sm">${v.id}</td>
                            <td>
                                <span class="plate-pill" title="${v.placa}">${v.placa}</span>
                            </td>
                            <td>${empty v.marca ? '-' : v.marca}</td>
                            <td>${empty v.modelo ? '-' : v.modelo}</td>
                            <td>${empty v.color ? '-' : v.color}</td>
                            <td class="hide-md">${v.creadoEn}</td>
                            <td>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/VehiculoController"
                                      style="display:inline"
                                      onsubmit="return confirm('¿Desactivar este vehículo?');">
                                    <input type="hidden" name="accion" value="desactivar">
                                    <input type="hidden" name="usuarioId" value="${usuarioId}">
                                    <input type="hidden" name="id" value="${v.id}">
                                    <button class="btn btn-danger" type="submit" title="Desactivar">
                                        <i class="fa-solid fa-power-off"></i> Desactivar
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </c:if>

    <c:if test="${empty vehiculos}">
        <p class="muted" style="margin-top:10px">Este usuario no tiene vehículos activos.</p>
    </c:if>
</div>
</body>
</html>
