<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
        String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Historial de Pagos</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
                <link rel="stylesheet" href="<%=ctx%>/CSS/pagos.css">
        </head>
        <body>
                <div class="container">
                        <!-- Header -->
                        <div class="header glass">
                                <div class="header-row">
                                        <div>
                                                <h1><i class="fas fa-home"></i> Residencial San Rafael</h1>
                                                <p>Historial de Pagos y Transacciones</p>
                                        </div>
                                        <a href="<%=ctx%>/vistas/menuResidente.jsp" class="btn-glass-back">
                                                <span class="icon">
                                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" 
                                                             stroke-linecap="round" stroke-linejoin="round">
                                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                                                        <polyline points="9 22 9 12 15 12 15 22"/>
                                                        </svg>
                                                </span>
                                                <span class="text">Ir al inicio</span>
                                        </a>
                                </div>
                        </div>

                        <!-- Card Principal -->
                        <div class="card glass">
                                <div class="card-header">
                                        <h2 class="card-title">
                                                <i class="fas fa-receipt"></i>
                                                Historial de Pagos
                                        </h2>
                                        <a href="<%=ctx%>/vistas/pagos/pagarServicio.jsp" class="btn btn-primary">
                                                <i class="fas fa-credit-card"></i> Realizar Pago
                                        </a>
                                </div>

                                <div class="stats-bar">
                                        <div class="stat-item">
                                                <i class="fas fa-list"></i>
                                                <span>Total: <strong>${empty totalRows ? 0 : totalRows}</strong> pagos</span>
                                        </div>
                                        <div class="stat-item">
                                                <i class="fas fa-file-alt"></i>
                                                <span>Página <strong>${empty page ? 1 : page}</strong> de <strong>${empty totalPages ? 1 : totalPages}</strong></span>
                                        </div>
                                </div>

                                <!-- Tabla -->
                                <div class="table-wrapper">
                                        <c:choose>
                                                <c:when test="${not empty pagos}">
                                                        <table class="package-table">
                                                                <thead>
                                                                        <tr>
                                                                                <th>No.</th>
                                                                                <th>Tipo de Pago</th>
                                                                                <th class="text-right">Monto</th>
                                                                                <th>Fecha de Pago</th>
                                                                                <th>Observaciones</th>
                                                                        </tr>
                                                                </thead>
                                                                <tbody>
                                                                        <c:forEach var="p" items="${pagos}" varStatus="st">
                                                                                <tr>
                                                                                        <td>${(page - 1) * size + st.index + 1}</td>
                                                                                        <td>
                                                                                                <c:choose>
                                                                                                        <c:when test="${not empty p.tipoNombre}">${p.tipoNombre}</c:when>
                                                                                                        <c:otherwise>${p.tipo}</c:otherwise>
                                                                                                </c:choose>
                                                                                        </td>
                                                                                        <td class="text-right amount">Q ${p.total}</td>
                                                                                        <td>${p.fechaPago}</td>
                                                                                        <td class="obs-text">
                                                                                                <c:choose>
                                                                                                        <c:when test="${not empty p.observaciones}">${p.observaciones}</c:when>
                                                                                                        <c:otherwise>—</c:otherwise>
                                                                                                </c:choose>
                                                                                        </td>
                                                                                </tr>
                                                                        </c:forEach>
                                                                </tbody>
                                                        </table>
                                                </c:when>
                                                <c:otherwise>
                                                        <div class="empty-state">
                                                                <i class="fas fa-inbox"></i>
                                                                <h3>No hay registros de pagos</h3>
                                                                <p>Los pagos registrados aparecerán aquí</p>
                                                        </div>
                                                </c:otherwise>
                                        </c:choose>
                                </div>

                                <!-- Paginación -->
                                <c:if test="${not empty pagos}">
                                        <div class="pagination">
                                                <div class="pagination-info">
                                                        Página ${empty page ? 1 : page} de ${empty totalPages ? 1 : totalPages}
                                                        <span class="sep">·</span>
                                                        Total: ${empty totalRows ? 0 : totalRows}
                                                </div>

                                                <div class="pagination-controls">
                                                        <form method="get" action="<%=ctx%>/pagos" id="sizeForm" class="size-form">
                                                                <label for="sizeSel">Mostrar:</label>
                                                                <select id="sizeSel" name="size" onchange="document.getElementById('sizeForm').submit()">
                                                                        <option value="5" <c:if test="${size==5}">selected</c:if>>5</option>
                                                                        <option value="10" <c:if test="${size==10}">selected</c:if>>10</option>
                                                                        <option value="20" <c:if test="${size==20}">selected</c:if>>20</option>
                                                                        <option value="50" <c:if test="${size==50}">selected</c:if>>50</option>
                                                                        <option value="100" <c:if test="${size==100}">selected</c:if>>100</option>
                                                                        </select>
                                                                        <input type="hidden" name="page" value="1"/>
                                                                </form>

                                                                <div class="page-btns">
                                                                        <a class="page-btn ${page <= 1 ? 'disabled' : ''}" 
                                                                   href="<%=ctx%>/pagos?page=1&size=${size}">
                                                                        <i class="fas fa-angles-left"></i>
                                                                </a>
                                                                <a class="page-btn ${page <= 1 ? 'disabled' : ''}" 
                                                                   href="<%=ctx%>/pagos?page=${page - 1}&size=${size}">
                                                                        <i class="fas fa-chevron-left"></i>
                                                                </a>
                                                                <a class="page-btn ${page >= totalPages ? 'disabled' : ''}" 
                                                                   href="<%=ctx%>/pagos?page=${page + 1}&size=${size}">
                                                                        <i class="fas fa-chevron-right"></i>
                                                                </a>
                                                                <a class="page-btn ${page >= totalPages ? 'disabled' : ''}" 
                                                                   href="<%=ctx%>/pagos?page=${totalPages}&size=${size}">
                                                                        <i class="fas fa-angles-right"></i>
                                                                </a>
                                                        </div>
                                                </div>
                                        </div>
                                </c:if>
                        </div>
                </div>
        </body>
</html>