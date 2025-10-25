package Servicio;

import Conexion_DB.Conexion;
import java.sql.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio de autenticación con autodetección de columnas. - Usuario insensible
 * a mayúsculas/minúsculas (compara en minúsculas). - Soporta contraseñas
 * hasheadas con BCrypt (prefijo "$2"). - Respeta columna "estado" (NULL o
 * 'ACTIVO' permiten login). - "correo" es opcional: si no existe, no rompe.
 */
public class AuthService {

        /**
         * DTO del usuario autenticado (para sesión).
         */
        public static class AuthUser {

                public final int id;
                public final String nombre;   // nombre/usuario como viene de BD
                public final String rol;
                public final String correo;   // puede ser null si no hay columna
                public String apellidos;      // si luego quieres completarlo

                public AuthUser(int id, String nombre, String rol, String correo) {
                        this.id = id;
                        this.nombre = nombre;
                        this.rol = rol;
                        this.correo = correo;
                }

                public int getId() {
                        return id;
                }

                public String getNombre() {
                        return nombre;
                }

                public String getRol() {
                        return rol;
                }

                public String getCorreo() {
                        return correo;
                }

        }

        private static final String TABLE = "usuarios";

        // Candidatos de nombres de columnas (en minúscula)
        private static final String[] USER_COL_CAND = {"nombre", "usuario", "username", "user"};
        private static final String[] PASS_COL_CAND = {"password", "contrasena", "contraseña", "contrasenia", "clave", "pass", "password_hash"};
        private static final String[] ROL_COL_CAND = {"rol", "role", "tipo", "perfil"};
        private static final String[] MAIL_COL_CAND = {"correo", "email", "mail"};

        // Columnas detectadas (se cachean estáticamente para ahorrar metadata)
        private static String userCol = null, passCol = null, rolCol = null, correoCol = null, idCol = "id";
        private static boolean hasEstado = false;

        /**
         * Autentica al usuario. Si la columna de contraseña en BD contiene un
         * hash BCrypt (empieza con "$2"), se valida con BCrypt; de lo
         * contrario, se compara texto plano.
         *
         * @param usuario usuario ingresado (no sensible a
         * mayúsculas/minúsculas)
         * @param password contraseña ingresada en texto plano
         * @return AuthUser si es válido; null si credenciales inválidas
         */
        public AuthUser login(String usuario, String password) throws Exception {
                String uIn = safe(usuario).trim();
                String pIn = safe(password);

                try (Connection cn = Conexion.getConnection()) {
                        ensureColumnNames(cn);

                        // Comparamos el USUARIO en minúsculas para evitar problemas de casing
                        // Nota: usamos LOWER() en SQL solo en la columna de usuario, no en contraseña.
                        final String sql
                                = "SELECT " + bt(idCol) + " AS id, "
                                + bt(userCol) + " AS nombre, "
                                + bt(rolCol) + " AS rol, "
                                + (correoCol != null ? bt(correoCol) : "NULL") + " AS correo, "
                                + bt(passCol) + " AS pass_val "
                                + "FROM " + bt(TABLE) + " "
                                + "WHERE LOWER(" + bt(userCol) + ") = LOWER(?) "
                                + (hasEstado ? "AND (" + bt("estado") + " IS NULL OR " + bt("estado") + "='activo') " : "")
                                + "LIMIT 1";

                        try (PreparedStatement ps = cn.prepareStatement(sql)) {
                                ps.setString(1, uIn);
                                try (ResultSet rs = ps.executeQuery()) {
                                        if (!rs.next()) {
                                                return null;
                                        }

                                        String passDb = rs.getString("pass_val");
                                        if (!passwordMatches(pIn, passDb)) {
                                                return null; // contraseña incorrecta
                                        }

                                        return new AuthUser(
                                                rs.getInt("id"),
                                                rs.getString("nombre"),
                                                rs.getString("rol"),
                                                rs.getString("correo")
                                        );
                                }
                        }
                }
        }

        // ------------------ Helpers internos ------------------
        /**
         * Envuelve identificadores con backticks para MySQL.
         */
        private static String bt(String ident) {
                return "`" + ident + "`";
        }

        /**
         * Inicializa nombres de columnas desde metadata solo una vez.
         */
        private synchronized void ensureColumnNames(Connection cn) throws SQLException {
                if (userCol != null && passCol != null && rolCol != null) {
                        return;
                }

                Set<String> cols = tableColumnsLower(cn, TABLE);

                userCol = pick(cols, USER_COL_CAND);
                passCol = pick(cols, PASS_COL_CAND);
                rolCol = pick(cols, ROL_COL_CAND);
                correoCol = pick(cols, MAIL_COL_CAND); // puede quedar null (opcional)
                hasEstado = cols.contains("estado");

                if (cols.contains("id")) {
                        idCol = "id";
                } else if (cols.contains("id_usuario")) {
                        idCol = "id_usuario";
                }

                if (userCol == null) {
                        throw new SQLException("No se encontró columna de USUARIO en " + TABLE);
                }
                if (passCol == null) {
                        throw new SQLException("No se encontró columna de CONTRASEÑA en " + TABLE);
                }
                if (rolCol == null) {
                        throw new SQLException("No se encontró columna de ROL en " + TABLE);
                }
                // correoCol puede ser null, lo manejamos en el SELECT con "NULL AS correo"
        }

        /**
         * Obtiene columnas (lowercase) de la tabla dada usando metadata JDBC.
         */
        private static Set<String> tableColumnsLower(Connection cn, String table) throws SQLException {
                Set<String> cols = new HashSet<>();
                DatabaseMetaData md = cn.getMetaData();
                // Para MySQL: catalog = base actual; schemaPattern suele ser null
                try (ResultSet rs = md.getColumns(cn.getCatalog(), null, table, null)) {
                        while (rs.next()) {
                                cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                        }
                }
                return cols;
        }

        /**
         * Devuelve el primer candidato presente en la tabla, o null si ninguno.
         */
        private static String pick(Set<String> colsLower, String[] candidatesLower) {
                for (String c : candidatesLower) {
                        if (colsLower.contains(c)) {
                                return c;
                        }
                }
                return null;
        }

        /**
         * Verifica contraseña con soporte BCrypt y fallback plano.
         */
        private static boolean passwordMatches(String plainInput, String dbValue) {
                String p = safe(plainInput);
                String db = safe(dbValue);

                // Si el valor de BD parece un hash BCrypt ($2a, $2b, $2y), valida con BCrypt
                if (db.startsWith("$2a$") || db.startsWith("$2b$") || db.startsWith("$2y$")) {
                        try {
                                // Evita dependencia dura: usa reflexión; si no está la lib, cae a false salvo que coincida plano
                                Class<?> clz = Class.forName("org.mindrot.jbcrypt.BCrypt");
                                boolean ok = (Boolean) clz.getMethod("checkpw", String.class, String.class).invoke(null, p, db);
                                return ok;
                        } catch (Throwable t) {
                                // Si no tienes jBCrypt en el classpath, como fallback compara plano (útil mientras migras)
                                return p.equals(db);
                        }
                }

                // De lo contrario, asume que BD guarda texto plano (no recomendado, pero soportado)
                return p.equals(db);
        }

        /**
         * Evita NPE con strings.
         */
        private static String safe(String s) {
                return s == null ? "" : s;
        }
}
