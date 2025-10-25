package Conexion_DB;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;

public class Conexion {

        // ======= CONFIG LOCAL (fallback) =======
        // Si no hay JNDI, se usará esto (puedes ajustarlo).
        private static final String FALLBACK_URL
                = "jdbc:mysql://127.0.0.1:3308/proyectofinal"
                + "?useSSL=false&sslMode=DISABLED"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Guatemala";
        private static final String FALLBACK_USER = "root";
        private static final String FALLBACK_PASS = "";

        // ======= JNDI =======
        private static final String JNDI_NAME = "jdbc/micasitaDS";
        private static volatile DataSource ds;  // cacheado

        static {
                // Cargar driver SOLO para fallback (DriverManager)
                try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                        /* ok, si usamos JNDI no se necesita */ }
                // Intentar resolver JNDI al arrancar (si falla, probaremos al primer getConnection)
                try {
                        ds = (DataSource) new InitialContext().lookup(JNDI_NAME);
                } catch (Exception ignore) {
                        ds = null;
                }
        }

        /**
         * Obtiene una conexión: 1) Intenta por JNDI (jdbc/micasitaDS) ->
         * GlassFish/pool 2) Si no existe, usa DriverManager con la URL local
         * (fallback)
         */
        public static Connection getConnection() throws SQLException {
                // Intento JNDI (si aún no está resuelto, reintenta)
                if (ds == null) {
                        synchronized (Conexion.class) {
                                if (ds == null) {
                                        try {
                                                ds = (DataSource) new InitialContext().lookup(JNDI_NAME);
                                        } catch (NamingException ne) {
                                                /* queda en null para fallback */ }
                                }
                        }
                }
                if (ds != null) {
                        return ds.getConnection();
                }
                // Fallback local (útil para dev si corres sin GlassFish o sin pool)
                return DriverManager.getConnection(FALLBACK_URL, FALLBACK_USER, FALLBACK_PASS);
        }

        // ======= Helpers de cierre seguros =======
        public static void close(Connection con) {
                if (con != null) {
                        try {
                                con.close();
                        } catch (SQLException ignore) {
                        }
                }
        }

        public static void close(PreparedStatement ps) {
                if (ps != null) {
                        try {
                                ps.close();
                        } catch (SQLException ignore) {
                        }
                }
        }

        public static void close(Statement st) {
                if (st != null) {
                        try {
                                st.close();
                        } catch (SQLException ignore) {
                        }
                }
        }

        public static void close(ResultSet rs) {
                if (rs != null) {
                        try {
                                rs.close();
                        } catch (SQLException ignore) {
                        }
                }
        }

        // ======= Helpers de transacción (opcional, por si te sirve) =======
        public static void beginTx(Connection c) throws SQLException {
                if (c != null) {
                        c.setAutoCommit(false);
                }
        }

        public static void commitTx(Connection c) {
                if (c != null) {
                        try {
                                c.commit();
                        } catch (SQLException ignore) {
                        }
                }
        }

        public static void rollbackTx(Connection c) {
                if (c != null) {
                        try {
                                c.rollback();
                        } catch (SQLException ignore) {
                        }
                }
        }
}
