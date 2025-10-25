package Usuario;

import java.sql.Timestamp;

public class Usuario {

        private int id;
        private String dpi;
        private String nombre;
        private String apellidos;
        private String correo;
        private String contraseña;
        private String rol;
        private String lote;
        private Integer numeroCasa;
        private Timestamp fechaCreacion;
        private String rutaQR;

        // Constructor completo
        public Usuario(int id, String dpi, String nombre, String apellidos,
                String correo, String contraseña, String rol,
                String lote, Integer numeroCasa) {
                this.id = id;
                this.dpi = dpi;
                this.nombre = nombre;
                this.apellidos = apellidos;
                this.correo = correo;
                this.contraseña = contraseña;
                this.rol = rol;
                this.lote = lote;
                this.numeroCasa = numeroCasa;
        }

        // Constructor sin ID para inserciones
        public Usuario(String dpi, String nombre, String apellidos,
                String correo, String contraseña, String rol,
                String lote, Integer numeroCasa) {
                this(0, dpi, nombre, apellidos, correo, contraseña, rol, lote, numeroCasa);
        }

        // Getters y Setters
        public int getId() {
                return id;
        }

        public void setId(int id) {
                this.id = id;
        }

        public String getDpi() {
                return dpi;
        }

        public void setDpi(String dpi) {
                this.dpi = dpi;
        }

        public String getNombre() {
                return nombre;
        }

        public void setNombre(String nombre) {
                this.nombre = nombre;
        }

        public String getApellidos() {
                return apellidos;
        }

        public void setApellidos(String apellidos) {
                this.apellidos = apellidos;
        }

        public String getCorreo() {
                return correo;
        }

        public void setCorreo(String correo) {
                this.correo = correo;
        }

        public String getContraseña() {
                return contraseña;
        }

        public void setContraseña(String contraseña) {
                this.contraseña = contraseña;
        }

        public String getRol() {
                return rol;
        }

        public void setRol(String rol) {
                this.rol = rol;
        }

        public String getLote() {
                return lote;
        }

        public void setLote(String lote) {
                this.lote = lote;
        }

        public Integer getNumeroCasa() {
                return numeroCasa;
        }

        public void setNumeroCasa(Integer numeroCasa) {
                this.numeroCasa = numeroCasa;
        }

        public Timestamp getFechaCreacion() {
                return fechaCreacion;
        }

        public void setFechaCreacion(Timestamp fechaCreacion) {
                this.fechaCreacion = fechaCreacion;
        }

        public String getRutaQR() {
                return rutaQR;
        }

        public void setRutaQR(String rutaQR) {
                this.rutaQR = rutaQR;
        }

        @Override
        public String toString() {
                return "Usuario{"
                        + "id=" + id
                        + ", dpi='" + dpi + '\''
                        + ", nombre='" + nombre + '\''
                        + ", apellidos='" + apellidos + '\''
                        + ", correo='" + correo + '\''
                        + ", rol='" + rol + '\''
                        + ", lote='" + lote + '\''
                        + ", numeroCasa=" + numeroCasa
                        + '}';
        }
}
