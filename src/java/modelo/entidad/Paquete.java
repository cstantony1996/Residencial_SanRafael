package modelo.entidad;

import java.util.Date;

public class Paquete {

        private Integer idPaquete;
        private String numeroGuia;
        private Integer idUsuarioDest;
        private Date fechaRecepcion;
        private Integer idAgenteReceptor;
        private boolean entregado;
        private Date fechaEntrega;
        private Integer idAgenteEntrega;
        private String observaciones;

        private String nombreDestinatario;
        private String apellidosDestinatario;
        private Integer numeroCasa;
        private String lote;

        public Integer getIdPaquete() {
                return idPaquete;
        }

        public void setIdPaquete(Integer idPaquete) {
                this.idPaquete = idPaquete;
        }

        public String getNumeroGuia() {
                return numeroGuia;
        }

        public void setNumeroGuia(String numeroGuia) {
                this.numeroGuia = numeroGuia;
        }

        public Integer getIdUsuarioDest() {
                return idUsuarioDest;
        }

        public void setIdUsuarioDest(Integer idUsuarioDest) {
                this.idUsuarioDest = idUsuarioDest;
        }

        public Date getFechaRecepcion() {
                return fechaRecepcion;
        }

        public void setFechaRecepcion(Date fechaRecepcion) {
                this.fechaRecepcion = fechaRecepcion;
        }

        public Integer getIdAgenteReceptor() {
                return idAgenteReceptor;
        }

        public void setIdAgenteReceptor(Integer idAgenteReceptor) {
                this.idAgenteReceptor = idAgenteReceptor;
        }

        public boolean isEntregado() {
                return entregado;
        }

        public void setEntregado(boolean entregado) {
                this.entregado = entregado;
        }

        public Date getFechaEntrega() {
                return fechaEntrega;
        }

        public void setFechaEntrega(Date fechaEntrega) {
                this.fechaEntrega = fechaEntrega;
        }

        public Integer getIdAgenteEntrega() {
                return idAgenteEntrega;
        }

        public void setIdAgenteEntrega(Integer idAgenteEntrega) {
                this.idAgenteEntrega = idAgenteEntrega;
        }

        public String getObservaciones() {
                return observaciones;
        }

        public void setObservaciones(String observaciones) {
                this.observaciones = observaciones;
        }

        public String getNombreDestinatario() {
                return nombreDestinatario;
        }

        public void setNombreDestinatario(String nombreDestinatario) {
                this.nombreDestinatario = nombreDestinatario;
        }

        public String getApellidosDestinatario() {
                return apellidosDestinatario;
        }

        public void setApellidosDestinatario(String apellidosDestinatario) {
                this.apellidosDestinatario = apellidosDestinatario;
        }

        public Integer getNumeroCasa() {
                return numeroCasa;
        }

        public void setNumeroCasa(Integer numeroCasa) {
                this.numeroCasa = numeroCasa;
        }

        public String getLote() {
                return lote;
        }

        public void setLote(String lote) {
                this.lote = lote;
        }

}
