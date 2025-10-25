package Vehiculo;

import java.sql.Timestamp;

public class Vehiculo {

    private Integer id;
    private Integer usuarioId;
    private String placa;
    private String marca;
    private String modelo;
    private String color;
    private boolean activo;
    private Timestamp creadoEn;

    public Vehiculo() {
    }

    public Vehiculo(Integer id, Integer usuarioId, String placa, String marca, String modelo, String color, boolean activo) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.color = color;
        this.activo = activo;
    }

    public Vehiculo(Integer usuarioId, String placa, String marca, String modelo, String color) {
        this(null, usuarioId, placa, marca, modelo, color, true);
    }

    // Getters/setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Timestamp getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Timestamp creadoEn) {
        this.creadoEn = creadoEn;
    }
}
