<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="Servicio.AuthService.AuthUser"%>
<%
        AuthUser u = (AuthUser) session.getAttribute("user");
        if (u == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
        }
%>
<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <title>Menú Guardia</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <!-- Estilos del tema -->
                <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/menu-guardia.css">
                <!-- Iconos -->
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
        </head>
        <body>
                <div class="dashboard">
                        <!-- Header -->
                        <header class="hero">
                                <div class="hero-left">
                                        <div class="hero-badge"><i class="fa-solid fa-shield-halved"></i></div>
                                        <div>
                                                <h1 class="title">Guardia de Seguridad — <span class="gradient"><%=u.nombre%></span></h1>
                                                <p class="subtitle">Accesos y utilidades rápidas de la garita.</p>
                                        </div>
                                </div>

                                <div class="hero-actions">
                                        <a class="btn btn-danger" href="${pageContext.request.contextPath}/logout">
                                                <i class="fa-solid fa-power-off"></i> Salir
                                        </a>
                                </div>
                        </header>

                        <!-- Tarjetas -->
                        <section class="card-grid">
                                <!-- Control de acceso -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon"><i class="fa-solid fa-qrcode"></i></div>
                                        <h3 class="card-title">Control de Acceso</h3>
                                        <p class="card-text">Abrir el lector de códigos QR de la garita.</p>
                                        <a class="btn" href="${pageContext.request.contextPath}/vistas/garita.jsp">
                                                <i class="fa-solid fa-right-to-bracket"></i> Ir a Garita
                                        </a>
                                </article>

                                <!-- Directorio -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon"><i class="fa-solid fa-address-book"></i></div>
                                        <h3 class="card-title">Directorio</h3>
                                        <p class="card-text">Consulta rápida de vecinos por nombre, lote o casa.</p>
                                        <a class="btn" href="${pageContext.request.contextPath}/directorio">
                                                <i class="fa-regular fa-folder-open"></i> Abrir directorio
                                        </a>
                                </article>

                                <!-- Comunicación interna -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon">
                                                <i class="fa-solid fa-bullhorn"></i>
                                        </div>
                                        <h3 class="card-title">Comunicación interna</h3>
                                        <p class="card-text">
                                                Avisos, noticias y mensajes del residencial.
                                        </p>
                                        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/chat/guardia/chat.jsp">
                                                <i class="fa-regular fa-message"></i> Ir a comunicación
                                        </a>
                                </article>

                                <!-- Paquetería -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon"><i class="fa-solid fa-box"></i></div>
                                        <h3 class="card-title">Paquetería</h3>
                                        <p class="card-text">Registrar recepción y gestionar entregas pendientes.</p>
                                        <a class="btn" href="${pageContext.request.contextPath}/paqueteria">
                                                <i class="fa-solid fa-truck-fast"></i> Ir a Paquetería
                                        </a>
                                </article>

                                <!-- (Tarjeta fantasma para mantener centrado si hay 2) -->
                                <div class="card card-ghost" aria-hidden="true"></div>
                        </section>
                </div>
        </body>
</html>
