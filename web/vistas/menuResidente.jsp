<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="Servicio.AuthService.AuthUser"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
                <title>Menú Residente</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <!-- Estilos del menú -->
                <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/menu-residente.css">
                <!-- Iconos -->
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
        </head>
        <body>

                <!-- SKELETON FULL-PAGE -->
                <div id="pageSkeleton" class="page-skeleton-overlay">
                        <div class="page-skeleton-container">
                                <div class="skeleton-header">
                                        <div class="skeleton-title"></div>
                                        <div class="skeleton-subtitle"></div>
                                </div>

                                <div class="skeleton-cards-grid">
                                        <!-- Ajusta la cantidad según tu grid (3, 6, etc.) -->
                                        <div class="skeleton-card">
                                                <div class="skeleton-card-header">
                                                        <div class="skeleton-icon"></div>
                                                        <div class="skeleton-text" style="max-width:180px"></div>
                                                </div>
                                                <div class="skeleton-line"></div>
                                                <div class="skeleton-line short"></div>
                                                <div class="skeleton-button"></div>
                                        </div>

                                        <div class="skeleton-card">
                                                <div class="skeleton-card-header">
                                                        <div class="skeleton-icon"></div>
                                                        <div class="skeleton-text" style="max-width:200px"></div>
                                                </div>
                                                <div class="skeleton-line"></div>
                                                <div class="skeleton-line short"></div>
                                                <div class="skeleton-button"></div>
                                        </div>

                                        <div class="skeleton-card">
                                                <div class="skeleton-card-header">
                                                        <div class="skeleton-icon"></div>
                                                        <div class="skeleton-text" style="max-width:160px"></div>
                                                </div>
                                                <div class="skeleton-line"></div>
                                                <div class="skeleton-line short"></div>
                                                <div class="skeleton-button"></div>
                                        </div>
                                </div>
                        </div>
                </div>


                <main class="dashboard">
                        <!-- Encabezado / saludo -->
                        <header class="hero">
                                <div class="hero-left">
                                        <div class="hero-badge">
                                                <i class="fa-solid fa-user-shield"></i>
                                        </div>
                                        <div>
                                                <h1 class="title">Hola, <span class="gradient"><%= (u != null ? u.nombre : "Residente")%></span></h1>
                                                <p class="subtitle" id="saludoHora">Bienvenido/a al panel del residente.</p>
                                        </div>
                                </div>

                                <nav class="hero-actions">
                                        <a class="btn btn-danger" href="${pageContext.request.contextPath}/logout">
                                                <i class="fa-solid fa-power-off"></i> Salir
                                        </a>
                                </nav>
                        </header>

                        <!-- Rejilla 3×3 centrada -->
                        <section class="card-grid">
                                <!-- Registrar visitante -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon">
                                                <i class="fa-solid fa-qrcode"></i>
                                        </div>
                                        <h3 class="card-title">Registrar visitante</h3>
                                        <p class="card-text">
                                                Genera códigos QR para tus visitas, por intentos o por tiempo.
                                        </p>
                                        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/visitante">
                                                <i class="fa-solid fa-wand-magic-sparkles"></i> Registrar visita
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
                                        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/vistas/comunicacion.jsp">
                                                <i class="fa-regular fa-message"></i> Ir a comunicación
                                        </a>
                                </article>

                                <!-- Directorio -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon">
                                                <i class="fa-solid fa-address-book"></i>
                                        </div>
                                        <h3 class="card-title">Directorio</h3>
                                        <p class="card-text">
                                                Contactos y datos relevantes del residencial.
                                        </p>
                                        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/directorio">
                                                <i class="fa-solid fa-folder-open"></i> Abrir directorio
                                        </a>
                                </article>

                                <!-- Reporte de Mantenimiento -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon">
                                                <i class="fa-solid fa-screwdriver-wrench"></i>
                                        </div>
                                        <h3 class="card-title">Reporte de Mantenimiento</h3>
                                        <p class="card-text">
                                                Genera y descarga reportes de incidencias y trabajos realizados.
                                        </p>
                                        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/reporteDeMantenimiento">
                                                <i class="fa-solid fa-file-arrow-down"></i> Abrir reportes
                                        </a>
                                </article>

                                <!-- Reserva de Áreas -->
                                <article class="card">
                                        <div class="card-accent"></div>
                                        <div class="card-icon">
                                                <i class="fa-solid fa-calendar-check"></i>
                                        </div>
                                        <h3 class="card-title">Reserva de Áreas</h3>
                                        <p class="card-text">
                                                Agenda y gestiona horarios para salón, cancha, piscina y más.
                                        </p>
                                        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/reservarAreas">
                                                <i class="fa-solid fa-calendar-plus"></i> Reserva área
                                        </a>
                                </article>

                                <!-- Card: Pagos -->
                                <article class="card" aria-labelledby="card-pagos-title">
                                        <div class="card-accent"></div>
                                        <div class="card-icon" aria-hidden="true">
                                                <i class="fa-solid fa-credit-card"></i>
                                        </div>
                                        <h3 id="card-pagos-title" class="card-title">Pagos</h3>
                                        <p class="card-text">
                                                Realiza y consulta tus pagos del residencial (mantenimiento, multas y reinstalaciones).
                                        </p>
                                        <a class="btn btn-secondary"
                                           href="${pageContext.request.contextPath}/pagos"
                                           aria-label="Ir a gestionar pagos">
                                                <i class="fa-regular fa-file-lines"></i> Gestionar
                                        </a>
                                </article>



                        </section>
                </main>

                <script>
                        // Saludo según hora
                        (function () {
                                var h = new Date().getHours();
                                var t = (h < 12) ? "Buenos días" : (h < 19) ? "Buenas tardes" : "Buenas noches";
                                var p = document.getElementById('saludoHora');
                                if (p)
                                        p.textContent = t + ".";
                        })();

                        // Redirigir el botón "Reporte de Mantenimiento" al JSP de reportesMant.jsp
                        (function () {
                                try {
                                        var cards = document.querySelectorAll('.card');
                                        for (var i = 0; i < cards.length; i++) {
                                                var title = cards[i].querySelector('.card-title');
                                                if (title && title.textContent.trim().toLowerCase() === 'reporte de mantenimiento') {
                                                        var btn = cards[i].querySelector('a.btn');
                                                        if (btn) {
                                                                btn.href = '<c:url value="/vistas/reportesMant.jsp"/>';
                                                        }
                                                        break;
                                                }
                                        }
                                } catch (e) {
                                        console.error('No se pudo reasignar el link a reportesMant.jsp:', e);
                                }
                        })();

                        // Oculta el skeleton cuando la página ya está lista
                        (function () {
                                const skel = document.getElementById('pageSkeleton');
                                if (!skel)
                                        return;

                                // Cuando terminen recursos “pesados”
                                window.addEventListener('load', () => {
                                        skel.classList.add('fade-out');
                                });

                                // Remueve del DOM cuando termine la animación
                                skel.addEventListener('animationend', () => {
                                        if (skel && skel.parentNode)
                                                skel.parentNode.removeChild(skel);
                                });

                                // Helpers por si quieres mostrarlo en navegaciones AJAX
                                window.showSkeleton = () => {
                                        if (!document.body.contains(skel))
                                                document.body.appendChild(skel);
                                        skel.classList.remove('fade-out');
                                        skel.style.visibility = 'visible';
                                        skel.style.opacity = '1';
                                };
                                window.hideSkeleton = () => {
                                        skel.classList.add('fade-out');
                                };
                        })();
                </script>
        </body>
</html>
