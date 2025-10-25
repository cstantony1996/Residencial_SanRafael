package Utils;

import java.security.SecureRandom;
import javax.servlet.http.*;

public class CsrfUtil {
        
        private static final SecureRandom RAND = new SecureRandom();
        public static final String CSRF_KEY = "csrfToken";
        
        public static String ensureCsrfToken(HttpSession session) {
                Object tok = session.getAttribute(CSRF_KEY);
                if (tok == null) {
                        byte[] b = new byte[16];
                        RAND.nextBytes(b);
                        StringBuilder sb = new StringBuilder();
                        for (byte x: b) sb.append(String.format("%02x", x));
                        String token = sb.toString();
                        session.setAttribute(CSRF_KEY, token);
                        return token;
                }
                return String.valueOf(tok);
        }
        
        public static boolean verify(HttpServletRequest req) {
                String in = req.getParameter("csrf");
                HttpSession s = req.getSession(false);
                if (s == null)
                        return false;
                Object tok = s.getAttribute(CSRF_KEY);
                return tok != null && tok.equals(in);
        }
        
}
