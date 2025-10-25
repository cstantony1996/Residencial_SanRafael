package Utils;

public class LuhnUtil {
        // valida numero de tarjeta (solo digitos, 13-19( usando algoritmo de Luhn
        public static boolean valida(String pan) {
                if (pan == null) return false;
                String s = pan.replaceAll("\\s+", "");
                if (!s.matches("\\d{13,19}")) return false;
                int sum = 0; 
                boolean alt = false;
                for (int i = s.length() - 1; i >= 0; i--) {
                        int n = s.charAt(i) - '0';
                        if (alt) {
                                n *= 2; 
                                if (n > 9) n -= 9;
                        }
                        sum += n;
                        alt = !alt;
                }
                return sum % 10 == 0;
        }
        
        public static String maskUltimos4(String pan) {
                String s = pan.replaceAll("\\s+", "");
                String ult4 = s.substring(s.length() - 4);
                return "**** **** **** " + ult4;
        }
        
}
