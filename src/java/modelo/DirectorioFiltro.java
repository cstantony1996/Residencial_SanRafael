package modelo;

public class DirectorioFiltro {

    private String nombre;
    private String apellidos;
    private String lote;
    private Integer numeroCasa;
    private int page; // 1-based
    private int size;

    public DirectorioFiltro() {
        this.page = 1;
        this.size = 10;
    }

    // getters y setters
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page <= 0 ? 1 : page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = (size <= 0 || size > 100) ? 10 : size;
    }
}
