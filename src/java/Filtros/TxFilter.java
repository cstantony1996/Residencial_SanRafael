package Filtros;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

public class TxFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
                // No-op: aquí podrías leer init-params desde web.xml si quisieras
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

                // Fuerza UTF-8 de forma segura (sin reventar si ya está definida)
                try {
                        request.setCharacterEncoding("UTF-8");
                } catch (Exception ignore) {
                }
                response.setCharacterEncoding("UTF-8");

                // Agrega un txid para la bitácora
                if (request instanceof HttpServletRequest) {
                        String txid = UUID.randomUUID().toString();
                        request.setAttribute("txid", txid);
                }

                chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
                // No-op
        }
}
