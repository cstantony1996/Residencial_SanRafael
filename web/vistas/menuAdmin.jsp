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
        <title>Menú Administrador</title>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/tabla-usuarios.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/admin-menu.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    </head>
    <body>
        <div class="dashboard">
            <!-- Header -->
            <header class="hero">
                <div class="hero-left">
                    <div class="hero-badge"><i class="fa-solid fa-shield-halved"></i></div>
                    <div>
                        <h1 class="title">Administrador — <span class="gradient">Hola, <%=u.nombre%></span></h1>
                        <p class="subtitle">Panel de control y gestión del sistema.</p>
                    </div>
                </div>
                <div class="hero-actions">
                    <a class="btn btn-danger" href="${pageContext.request.contextPath}/logout">
                        <i class="fa-solid fa-power-off"></i> Salir
                    </a>
                </div>
            </header>

            <!-- Tarjetas (separadas) -->
            <section class="card-grid">
                <!-- Tarjeta: Listar usuarios -->
                <article class="card">
                    <span class="card-accent"></span>
                    <div class="card-icon"><i class="fa-regular fa-rectangle-list"></i></div>
                    <h3 class="card-title">Listar usuarios</h3>
                    <p class="card-text">Consulta, edita y administra los usuarios actuales.</p>
                    <div class="card-actions">
                        <a class="btn btn-ghost" href="<c:url value='/UsuarioController?accion=listar'/>">
                            <i class="fa-regular fa-folder-open"></i> Abrir lista
                        </a>
                    </div>
                </article>

                <!-- Tarjeta: Crear usuario -->
                <article class="card">
                    <span class="card-accent"></span>
                    <div class="card-icon"><i class="fa-solid fa-user-plus"></i></div>
                    <h3 class="card-title">Crear usuario</h3>
                    <p class="card-text">Da de alta un nuevo usuario con su rol correspondiente.</p>
                    <div class="card-actions">
                        <a class="btn" href="<c:url value='/UsuarioController?accion=nuevo'/>">
                            <i class="fa-solid fa-circle-plus"></i> Crear usuario
                        </a>
                    </div>
                </article>
            </section>
        </div>
    </body>
</html>
