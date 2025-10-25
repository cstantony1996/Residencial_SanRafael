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
                <meta charset="UTF-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <title>Centro de atención</title>
                <link rel="stylesheet" href="<%=request.getContextPath()%>/CSS/chat-guard.css"/>

        </head>
        <body>
                <div class="chat-shell">

                        <!-- SIDEBAR -->
                        <aside class="sidebar" aria-label="Conversaciones">
                                <header class="sidebar-top">
                                        <div class="brand">
                                                <div class="brand-badge">🛡️</div>
                                                <h1 class="brand-title">Consultas</h1>
                                        </div>
                                </header>

                                <div class="sidebar-search">
                                        <div class="search-box">
                                                <svg class="search-ico" width="16" height="16" viewBox="0 0 24 24" fill="none">
                                                <path d="M21 21l-4.3-4.3m1.3-5.2a6.5 6.5 0 11-13.0 0 6.5 6.5 0 0113.0 0z"
                                                      stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                                                </svg>
                                                <input id="conv-search" type="search" placeholder="Buscar conversación" aria-label="Buscar conversación"/>
                                        </div>
                                </div>

                                <nav id="conv-list" class="conv-list" role="list" aria-live="polite">
                                        <!-- items renderizados por chat.js -->
                                </nav>

                                <!-- Botón Regresar fijo al fondo -->
                                <footer class="sidebar-footer">
                                        <a class="btn-back" href="<%=request.getContextPath()%>/vistas/menuGuardia.jsp">← Regresar al menú</a>
                                </footer>
                        </aside>

                        <!-- PANE PRINCIPAL -->
                        <main class="pane" aria-label="Chat">

                                <!-- Header del chat -->
                                <header id="chat-header" class="chat-header" hidden>
                                        <div class="peer">
                                                <div id="peer-avatar" class="avatar lg">R</div>
                                                <div class="peer-meta">
                                                        <div id="contact-name" class="peer-name">—</div>
                                                        <div class="peer-presence">
                                                                <span class="dot" aria-hidden="true"></span>
                                                                <span id="presence-text" class="presence-text">Desconectado</span>
                                                        </div>
                                                </div>
                                        </div>

                                        <div class="header-actions">
                                                <button id="btn-close-conv" class="btn btn-ghost" type="button"
                                                        title="Cerrar conversación"
                                                        onclick="document.getElementById('empty-state').hidden = false;document.getElementById('msgs').hidden = true;document.getElementById('composer').hidden = true;document.getElementById('chat-header').hidden = true;">
                                                        Cerrar
                                                </button>
                                        </div>
                                </header>

                                <nav id="threads" class="threads" hidden aria-label="Hilos de la conversación"></nav>

                                <!-- Estado vacío -->
                                <section id="empty-state" class="empty-state" aria-live="polite">
                                        <div class="empty-card">
                                                <div class="empty-ico">📥</div>
                                                <h2>Selecciona una conversación</h2>
                                                <p>Elige un chat en la lista para responder.</p>
                                        </div>
                                </section>

                                <!-- Mensajes -->
                                <section id="msgs" class="msgs" aria-live="polite" hidden></section>

                                <!-- Composer -->
                                <footer id="composer" class="composer" hidden>
                                        <textarea id="text" rows="1" placeholder="Escribe una respuesta" aria-label="Escribe una respuesta"></textarea>
                                        <button id="send" class="btn btn-primary" title="Enviar"><span class="ico">➤</span> Enviar</button>
                                </footer>
                        </main>
                </div>

                <!-- Template de conversación (para vista tipo WhatsApp) -->
                <template id="tpl-conv-item">
                        <button class="conv-item" role="listitem" data-id="" data-peer="">
                                <div class="avatar sm">R</div>
                                <div class="conv-main">
                                        <div class="conv-name">Nombre del residente</div>
                                        <div class="conv-last">Vista previa del último mensaje…</div>
                                </div>
                                <div class="conv-meta">
                                        <div class="conv-time">21:07</div>
                                        <span class="conv-badge" aria-label="no leídos" hidden>0</span>
                                </div>
                        </button>
                </template>

                <script>
            const BASE = '<%=request.getContextPath()%>';
            const CURRENT_ROLE = 'AGENTE';
                </script>
                <script src="<%=request.getContextPath()%>/chat/chat.js?v=13"></script>
        </body>
</html>
