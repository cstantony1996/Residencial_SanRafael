package Servicio;

import DirectorioDAO.DirectorioDAO;
import modelo.DirectorioFiltro;
import modelo.DirectorioItem;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DirectorioService {
    
    private final DirectorioDAO dao;
    
    public DirectorioService(DirectorioDAO dao) {
        this.dao = dao;
    }
    
    public static String validar(DirectorioFiltro f) {
        boolean loteVacio = (f.getLote() == null || f.getLote().trim().isEmpty());
        boolean casaVacia = (f.getNumeroCasa() == null);
        if (loteVacio ^ casaVacia) {
            return "Debe seleccionar Lote y Número de casa juntos, o dejar ambos vacíos";
        }
        return null;
    }
    
    public List<DirectorioItem> buscar(DirectorioFiltro f) throws SQLException {
        String err = validar(f);
        if (err != null) {
            return Collections.emptyList();
        }
        return dao.buscar(f);
    }
    
    public int contar(DirectorioFiltro f) throws SQLException {
        String err = validar(f);
        if (err != null) return 0;
        return dao.contar(f);
    }
}
