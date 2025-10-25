<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <title>Login - Residencial San Rafael</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <!-- Estilos -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/login.css">
        <!-- Iconos -->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    </head>
    <body>
        <main class="login-screen">
            <section class="auth-card">
                <!-- Lado hero -->
                <div class="hero">
                    <div class="hero-logo">
                        <i class="fa-solid fa-house-chimney"></i>
                    </div>
                    <h2 class="hero-title">Residencial San Rafael</h2>
                    <p class="hero-sub">Control de acceso y administración en un solo lugar.</p>
                    <ul class="hero-points">
                        <li><i class="fa-solid fa-shield-halved"></i> Seguridad y control</li>
                        <li><i class="fa-solid fa-qrcode"></i> Accesos por QR</li>
                        <li><i class="fa-regular fa-envelope"></i> Notificaciones</li>
                    </ul>
                </div>

                <!-- Lado formulario -->
                <div class="form-side">
                    <h1 class="form-title">Inicia sesión</h1>
                    <p class="form-sub">Bienvenido de vuelta</p>

                    <c:if test="${not empty error}">
                        <div class="alert error">
                            <i class="fa-solid fa-triangle-exclamation"></i>
                            <span>${error}</span>
                        </div>
                    </c:if>

                    <form method="post" action="${pageContext.request.contextPath}/login" autocomplete="off" class="login-form">
                        <!-- Usuario -->
                        <label class="field">
                            <span class="label">Usuario</span>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-regular fa-user"></i></span>
                                <input class="input" type="text" id="nombre" name="nombre" required
                                       autocomplete="username" placeholder="Tu usuario">
                            </div>
                        </label>

                        <!-- Password -->
                        <label class="field">
                            <span class="label">Contraseña</span>
                            <div class="input-group">
                                <span class="input-icon"><i class="fa-solid fa-lock"></i></span>
                                <input class="input" type="password" id="password" name="password" required
                                       autocomplete="current-password" placeholder="••••••••">
                                <button type="button" class="icon-btn" id="togglePwd" aria-label="Mostrar u ocultar contraseña">
                                    <i class="fa-regular fa-eye"></i>
                                </button>
                            </div>
                        </label>

                        <div class="form-row">
                            <label class="check">
                                <input type="checkbox" name="remember">
                                <span>Recordarme</span>
                            </label>
                        </div>

                        <button class="btn btn-primary" type="submit">
                            <i class="fa-solid fa-right-to-bracket"></i> Entrar
                        </button>

                        <div class="divider"><span>o</span></div>
                    </form>

                    <footer class="legal">
                        <small>© <span id="yy"></span> Residencial San Rafael</small>
                    </footer>
                </div>
            </section>
        </main>

        <script>
            // Año dinámico
            document.getElementById('yy').textContent = new Date().getFullYear();

            // Mostrar/ocultar contraseña
            const toggle = document.getElementById('togglePwd');
            const pwd = document.getElementById('password');
            toggle.addEventListener('click', () => {
                const isText = pwd.type === 'text';
                pwd.type = isText ? 'password' : 'text';
                toggle.firstElementChild.className = isText ? 'fa-regular fa-eye' : 'fa-regular fa-eye-slash';
                pwd.focus();
            });
        </script>
    </body>
</html>
