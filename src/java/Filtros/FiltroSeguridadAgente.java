package Filtros;

import Servicio.AuthService.AuthUser;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter(urlPatterns = {"/paqueteria/*"})
public class FiltroSeguridadAgente implements Filter {

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                HttpSession s = req.getSession(false);

                AuthUser u = (s == null) ? null : (AuthUser) s.getAttribute("user");
                if (u == null || u.getRol() == null || !u.getRol().equalsIgnoreCase("agente")) {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json; charset=UTF-8");
                        res.getWriter().write("{\"ok\":false,\"error\":\"Acceso denegado. Se requiere rol agente.\"}");
                        return;
                }

                // Normaliza atributos que usan los servicios
                s.setAttribute("usuarioRol", "agente");
                s.setAttribute("usuarioId", u.getId());

                chain.doFilter(request, response);
        }
}
