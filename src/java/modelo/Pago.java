package modelo;

import java.time.LocalDateTime;

public class Pago {

        private int id;
        private int usuarioId;
        private int tipoPagoId;

        // Para la vista:
        private String tipo;        // código (ej. "MANT")
        private String tipoNombre;  // nombre (ej. "Mantenimiento")

        private String mesAnio;     // "2025-10" o similar
        private double montoBase;
        private double mora;
        private double total;

        private LocalDateTime fechaPago;

        private String observaciones;
        private String tarjetaMasked;
        private String authCode;

        public enum Status {
                PENDIENTE, APROBADO, RECHAZADO, ANULADO
        }
        private Status status;

        // ==== Getters/Setters requeridos por EL/JSP ====
        public int getId() {
                return id;
        }

        public void setId(int id) {
                this.id = id;
        }

        public int getUsuarioId() {
                return usuarioId;
        }

        public void setUsuarioId(int usuarioId) {
                this.usuarioId = usuarioId;
        }

        public int getTipoPagoId() {
                return tipoPagoId;
        }

        public void setTipoPagoId(int tipoPagoId) {
                this.tipoPagoId = tipoPagoId;
        }

        public String getTipo() {
                return tipo;
        }

        public void setTipo(String tipo) {
                this.tipo = tipo;
        }

        public String getTipoNombre() {
                return tipoNombre;
        }

        public void setTipoNombre(String tipoNombre) {
                this.tipoNombre = tipoNombre;
        }

        public String getMesAnio() {
                return mesAnio;
        }

        public void setMesAnio(String mesAnio) {
                this.mesAnio = mesAnio;
        }

        public double getMontoBase() {
                return montoBase;
        }

        public void setMontoBase(double montoBase) {
                this.montoBase = montoBase;
        }

        public double getMora() {
                return mora;
        }

        public void setMora(double mora) {
                this.mora = mora;
        }

        public double getTotal() {
                return total;
        }

        public void setTotal(double total) {
                this.total = total;
        }

        public LocalDateTime getFechaPago() {
                return fechaPago;
        }

        public void setFechaPago(LocalDateTime fechaPago) {
                this.fechaPago = fechaPago;
        }

        public String getObservaciones() {
                return observaciones;
        }

        public void setObservaciones(String observaciones) {
                this.observaciones = observaciones;
        }

        public String getTarjetaMasked() {
                return tarjetaMasked;
        }

        public void setTarjetaMasked(String tarjetaMasked) {
                this.tarjetaMasked = tarjetaMasked;
        }

        public String getAuthCode() {
                return authCode;
        }

        public void setAuthCode(String authCode) {
                this.authCode = authCode;
        }

        public Status getStatus() {
                return status;
        }

        public void setStatus(Status status) {
                this.status = status;
        }
}
