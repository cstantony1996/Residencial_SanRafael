<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
        String ctx = request.getContextPath();
%>
<!doctype html>
<html lang="es">
        <head>
                <meta charset="utf-8">
                <title>Comunicación interna</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/comunicacion.css">
        </head>
        <body>
                <div class="dashboard">
                        <!-- Hero -->
                        <header class="hero">
                                <div class="hero-left">
                                        <div class="hero-badge">💬</div>
                                        <div>
                                                <h1 class="title"><span class="gradient">Comunicación interna</span></h1>
                                                <p class="subtitle">Elige qué deseas hacer</p>
                                        </div>
                                </div>
                                <div class="hero-actions">
                                        <a class="btn btn-secondary" href="<%= ctx%>/vistas/menuResidente.jsp">← Volver al menú</a>
                                </div>
                        </header>

                        <!-- Tarjetas -->
                        <main class="content">
                                <section class="card-grid">
                                        <!-- Consulta general -->
                                        <article class="card">
                                                <div class="card-accent"></div>
                                                <div class="card-icon">🗨️</div>
                                                <h3 class="card-title">Consulta general</h3>
                                                <p class="card-text">
                                                        Inicia una conversación 1 a 1 con un guardia activo. El sistema garantiza una sola conversación por guardia.
                                                </p>
                                                <div class="cta">
                                                        <a class="btn btn-primary" href="<%= ctx%>/chat/residente/chat.jsp">Ir al chat</a>
                                                        <div class="small">Se listarán guardias activos para crear la conversación.</div>
                                                </div>
                                        </article>

                                        <!-- Reportar incidente -->
                                        <article class="card">
                                                <div class="card-accent"></div>
                                                <div class="card-icon">🚨</div>
                                                <h3 class="card-title">Reportar incidente</h3>
                                                <p class="card-text">
                                                        Envía un reporte (disturbios, ruido, accidente vehicular, daños, otros). Se notificará a seguridad.
                                                </p>
                                                <div class="cta">
                                                        <a class="btn btn-primary" href="<%= ctx%>/chat/residente/incidente.jsp">Reportar incidente</a>
                                                        <a class="btn btn-secondary" href="<%= ctx%>/vistas/menuResidente.jsp">Cancelar</a>
                                                </div>
                                        </article>
                                </section>
                        </main>
                </div>
        </body>
</html>
