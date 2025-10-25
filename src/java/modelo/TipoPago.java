package modelo;

import java.math.BigDecimal;

public class TipoPago {
        
        private Integer id;
        private String codigo;
        private String nombre;
        private BigDecimal montoBase;
        private boolean recurrente;
        private boolean activo;

        public Integer getId() {
                return id;
        }

        public void setId(Integer id) {
                this.id = id;
        }

        public String getCodigo() {
                return codigo;
        }

        public void setCodigo(String codigo) {
                this.codigo = codigo;
        }

        public String getNombre() {
                return nombre;
        }

        public void setNombre(String nombre) {
                this.nombre = nombre;
        }

        public BigDecimal getMontoBase() {
                return montoBase;
        }

        public void setMontoBase(BigDecimal montoBase) {
                this.montoBase = montoBase;
        }

        public boolean isRecurrente() {
                return recurrente;
        }

        public void setRecurrente(boolean recurrente) {
                this.recurrente = recurrente;
        }

        public boolean isActivo() {
                return activo;
        }

        public void setActivo(boolean activo) {
                this.activo = activo;
        }
        
       
        
}
