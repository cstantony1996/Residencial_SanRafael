package Controlador;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*; import java.io.*; import java.sql.*;

@WebServlet("/JDBCTest")
public class JDBCTest extends HttpServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain; charset=UTF-8");
    try (Connection cn = Conexion_DB.Conexion.getConnection();
         PreparedStatement ps = cn.prepareStatement("SELECT 1");
         ResultSet rs = ps.executeQuery()) {
      resp.getWriter().println(rs.next() ? "JDBC OK" : "Sin resultado");
    } catch (Exception e) { e.printStackTrace(resp.getWriter()); }
  }
}
