package VisitaDAO;

import Conexion_DB.Conexion;
import modelo.Visita;
import Utils.TokenQRUtil;

import java.sql.*;

public class VisitaDAO {

    public long registrarVisita(Visita v) throws Exception {
        String sqlInsVis = "INSERT INTO visitas(residente_id,nombre,dpi,correo,tipo_qr,intentos,expira_en,token,estado) "
                + "VALUES (?,?,?,?,?,?,?,NULL,'activo')";
        String sqlUpdToken = "UPDATE visitas SET token=? WHERE id=?";
        // 👇 incluimos visita_id
        String sqlInsTk = "INSERT INTO qr_tokens(token, usuario_id, visita_id, tipo, estado, usos_restantes, expira_en, creado_en) "
                + "VALUES (?,?,?,?,?,?,?, NOW())";

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement psV = cn.prepareStatement(sqlInsVis, Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement psU = cn.prepareStatement(sqlUpdToken);
                    PreparedStatement psT = cn.prepareStatement(sqlInsTk)) {

                // 1) Insertar visita (sin token)
                psV.setInt(1, v.getResidenteId());
                psV.setString(2, v.getNombre());
                psV.setString(3, v.getDpi());
                psV.setString(4, v.getCorreo()); // obligatorio
                psV.setString(5, v.getTipoQr());
                if ("intentos".equals(v.getTipoQr())) {
                    psV.setObject(6, v.getIntentos(), Types.INTEGER);
                    psV.setNull(7, Types.TIMESTAMP);
                } else {
                    psV.setNull(6, Types.INTEGER);
                    psV.setTimestamp(7, v.getExpiraEn());
                }
                psV.executeUpdate();

                long visitaId;
                try (ResultSet rs = psV.getGeneratedKeys()) {
                    visitaId = rs.next() ? rs.getLong(1) : 0L;
                }
                if (visitaId <= 0) {
                    throw new SQLException("No se obtuvo id de visita");
                }

                // 2) Token firmado con id de visita (usa tu TokenQRUtil real)
                long expEpochSec = 0L;
                if ("tiempo".equals(v.getTipoQr()) && v.getExpiraEn() != null) {
                    expEpochSec = v.getExpiraEn().toInstant().getEpochSecond();
                }
                String token = TokenQRUtil.generarTokenVisita((int) visitaId, expEpochSec);
                v.setId((int) visitaId);
                v.setToken(token);

                // 3) Actualizar visita con token
                psU.setString(1, token);
                psU.setLong(2, visitaId);
                psU.executeUpdate();

                // 4) Registrar en qr_tokens (incluimos visita_id)
                psT.setString(1, token);
                psT.setInt(2, v.getResidenteId());     // dueño (residente)
                psT.setLong(3, visitaId);              // 👈 visita_id
                psT.setString(4, "visitante");
                psT.setString(5, "activo");
                if ("intentos".equals(v.getTipoQr())) {
                    psT.setObject(6, v.getIntentos(), Types.INTEGER);
                    psT.setNull(7, Types.TIMESTAMP);
                } else {
                    psT.setNull(6, Types.INTEGER);
                    psT.setTimestamp(7, v.getExpiraEn());
                }
                psT.executeUpdate();

                cn.commit();
                return visitaId;

            } catch (Exception e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public boolean cancelar(long visitaId, int residenteId) throws SQLException {
        String sqlV = "UPDATE visitas SET estado='cancelado', cancelado_en=NOW() WHERE id=? AND residente_id=? AND estado='activo'";
        String sqlT = "UPDATE qr_tokens SET estado='invalido' WHERE token=(SELECT token FROM visitas WHERE id=?)";

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps1 = cn.prepareStatement(sqlV);
                    PreparedStatement ps2 = cn.prepareStatement(sqlT)) {
                ps1.setLong(1, visitaId);
                ps1.setInt(2, residenteId);
                int a = ps1.executeUpdate();

                ps2.setLong(1, visitaId);
                ps2.executeUpdate();

                cn.commit();
                return a > 0;
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }
}
