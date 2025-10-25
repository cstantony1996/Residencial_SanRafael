package modelo;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Entidad Visita. Mapea la tabla 'visitas'
 *
 * id INT PK residente_id INT NOT NULL nombre VARCHAR(150) -- nombre del
 * visitante dpi VARCHAR(25) NULL correo VARCHAR(200) NOT NULL tipo_qr
 * ENUM('intentos','tiempo') NOT NULL intentos INT NULL -- si tipo_qr='intentos'
 * expira_en DATETIME NULL -- si tipo_qr='tiempo' token VARCHAR(255) UNIQUE NULL
 * estado ENUM('activo','cancelado') NOT NULL DEFAULT 'activo' creado_en
 * DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP cancelado_en DATETIME NULL
 */
public class Visita implements Serializable {

    private int id;
    private int residenteId;

    // === Nombres "nuevos" esperados por VisitaService ===
    private String nombreVisitante;
    private String dpi;
    private String correoVisitante;
    /**
     * "intentos" | "tiempo"
     */
    private String tipo;
    /**
     * Solo si tipo="intentos"
     */
    private Integer intentosPermitidos;
    /**
     * Solo si tipo="tiempo"
     */
    private Timestamp expiraEn;

    private String token;
    private String estado;
    private Timestamp creadoEn;
    private Timestamp canceladoEn;

    public Visita() {
    }

    public Visita(int id, int residenteId, String nombreVisitante, String dpi, String correoVisitante,
            String tipo, Integer intentosPermitidos, Timestamp expiraEn,
            String token, String estado, Timestamp creadoEn, Timestamp canceladoEn) {
        this.id = id;
        this.residenteId = residenteId;
        this.nombreVisitante = nombreVisitante;
        this.dpi = dpi;
        this.correoVisitante = correoVisitante;
        this.tipo = tipo;
        this.intentosPermitidos = intentosPermitidos;
        this.expiraEn = expiraEn;
        this.token = token;
        this.estado = estado;
        this.creadoEn = creadoEn;
        this.canceladoEn = canceladoEn;
    }

    /* ================= Getters / Setters (nombres nuevos) ================= */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getResidenteId() {
        return residenteId;
    }

    public void setResidenteId(int residenteId) {
        this.residenteId = residenteId;
    }

    public String getNombreVisitante() {
        return nombreVisitante;
    }

    public void setNombreVisitante(String nombreVisitante) {
        this.nombreVisitante = nombreVisitante;
    }

    public String getDpi() {
        return dpi;
    }

    public void setDpi(String dpi) {
        this.dpi = dpi;
    }

    public String getCorreoVisitante() {
        return correoVisitante;
    }

    public void setCorreoVisitante(String correoVisitante) {
        this.correoVisitante = correoVisitante;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getIntentosPermitidos() {
        return intentosPermitidos;
    }

    public void setIntentosPermitidos(Integer intentosPermitidos) {
        this.intentosPermitidos = intentosPermitidos;
    }

    public Timestamp getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(Timestamp expiraEn) {
        this.expiraEn = expiraEn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Timestamp creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Timestamp getCanceladoEn() {
        return canceladoEn;
    }

    public void setCanceladoEn(Timestamp canceladoEn) {
        this.canceladoEn = canceladoEn;
    }

    /* ================= Compatibilidad hacia atrás =================
       Estos métodos permiten que código existente que usa los
       nombres antiguos compile sin tocar nada. Puedes eliminarlos
       cuando todo tu código use los nombres nuevos. */
    /**
     * @deprecated usa getNombreVisitante()
     */
    @Deprecated
    public String getNombre() {
        return nombreVisitante;
    }

    /**
     * @deprecated usa setNombreVisitante(...)
     */
    @Deprecated
    public void setNombre(String nombre) {
        this.nombreVisitante = nombre;
    }

    /**
     * @deprecated usa getCorreoVisitante()
     */
    @Deprecated
    public String getCorreo() {
        return correoVisitante;
    }

    /**
     * @deprecated usa setCorreoVisitante(...)
     */
    @Deprecated
    public void setCorreo(String correo) {
        this.correoVisitante = correo;
    }

    /**
     * @deprecated usa getTipo()
     */
    @Deprecated
    public String getTipoQr() {
        return tipo;
    }

    /**
     * @deprecated usa setTipo(...)
     */
    @Deprecated
    public void setTipoQr(String tipoQr) {
        this.tipo = tipoQr;
    }

    /**
     * @deprecated usa getIntentosPermitidos()
     */
    @Deprecated
    public Integer getIntentos() {
        return intentosPermitidos;
    }

    /**
     * @deprecated usa setIntentosPermitidos(...)
     */
    @Deprecated
    public void setIntentos(Integer intentos) {
        this.intentosPermitidos = intentos;
    }

    /* ================= Helpers ================= */
    public boolean esPorIntentos() {
        return "intentos".equalsIgnoreCase(tipo);
    }

    public boolean esPorTiempo() {
        return "tiempo".equalsIgnoreCase(tipo);
    }

    @Override
    public String toString() {
        return "Visita{"
                + "id=" + id
                + ", residenteId=" + residenteId
                + ", nombreVisitante='" + nombreVisitante + '\''
                + ", tipo='" + tipo + '\''
                + ", intentosPermitidos=" + intentosPermitidos
                + ", expiraEn=" + expiraEn
                + ", estado='" + estado + '\''
                + ", creadoEn=" + creadoEn
                + ", canceladoEn=" + canceladoEn
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Visita)) {
            return false;
        }
        Visita v = (Visita) o;
        if (id != 0 && v.id != 0) {
            return id == v.id;
        }
        return Objects.equals(token, v.token);
    }

    @Override
    public int hashCode() {
        return (id != 0) ? Integer.hashCode(id) : Objects.hashCode(token);
    }
}
