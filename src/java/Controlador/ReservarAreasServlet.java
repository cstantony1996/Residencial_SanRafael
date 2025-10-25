package Controlador;

import ReservasDAO.ReservasAreasDAO;
import ReservasDAO.ReservasAreasDAO.ReservaItem;
import Servicio.AuthService.AuthUser;
import Servicio.BitacoraService;
import Utils.CorreoUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@WebServlet(name = "ReservarAreasServlet",
        urlPatterns = {"/reservarAreas", "/reservaDeAreas"})
public class ReservarAreasServlet extends HttpServlet {

        private ReservasAreasDAO dao;

        @Override
        public void init() throws ServletException {
                try {
                        dao = new ReservasAreasDAO();
                } catch (Throwable t) {
                        t.printStackTrace();
                        dao = null; // modo degradado
                }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                Map<Integer, String> areas = new LinkedHashMap<>();
                areas.put(1, "Piscina");
                areas.put(2, "Salón");
                try {
                        if (dao != null) {
                                Map<Integer, String> dbAreas = dao.listarAreas();
                                if (dbAreas != null && !dbAreas.isEmpty()) {
                                        areas = dbAreas;
                                }
                        }
                } catch (Throwable t) {
                        t.printStackTrace();
                }
                req.setAttribute("areas", areas);

                Integer areaId = parseInt(req.getParameter("areaId"), firstKey(areas));
                Integer anio = parseInt(req.getParameter("anio"), LocalDate.now().getYear());
                Integer mes = parseInt(req.getParameter("mes"), LocalDate.now().getMonthValue());
                Integer dia = parseInt(req.getParameter("dia"), LocalDate.now().getDayOfMonth());
                Integer hstart = parseInt(req.getParameter("hstart"), 0);
                if (hstart < 0) {
                        hstart = 0;
                }
                if (hstart > 23) {
                        hstart = 16;
                }
                hstart = (hstart / 8) * 8;
                if (hstart > 16) {
                        hstart = 16;
                }

                LocalDate fecha = safeDate(anio, mes, dia);
                LocalDate hoy = LocalDate.now(); //referencia de hoy

                boolean[] horas = new boolean[24];
                Arrays.fill(horas, true);
                try {
                        if (fecha.isBefore(hoy)) {
                                // si la fecha es pasada, deshabilitar todas las horas
                                Arrays.fill(horas, false);
                                req.setAttribute("status", n(req.getParameter("status")).
                                        isEmpty() ? "past_date" : req.getParameter("status"));
                        } else if (dao != null) {
                                horas = dao.disponibilidadPorDia(areaId, fecha);
                        }
                } catch (Throwable t) {
                        t.printStackTrace();
                        Arrays.fill(horas, true);
                }

                List<ReservaItem> mias = Collections.emptyList();
                try {
                        Object u = req.getSession().getAttribute("user");
                        if (dao != null && u instanceof AuthUser) {
                                String correo = ((AuthUser) u).correo == null ? "" : ((AuthUser) u).correo.trim();
                                if (!correo.isEmpty()) {
                                        mias = dao.listarReservasActivasUsuario(areaId, correo, LocalDate.now());
                                }
                        }
                } catch (Throwable t) {
                        t.printStackTrace();
                }

                req.setAttribute("areaId", areaId);
                req.setAttribute("anio", anio);
                req.setAttribute("mes", mes);
                req.setAttribute("dia", dia);
                req.setAttribute("hstart", hstart);
                req.setAttribute("horas", horas);
                req.setAttribute("misReservas", mias);

                String status = req.getParameter("status"); // ok|conflict|bad_range|db_err|err|cancel_ok|cancel_err|past_date
                if (status != null) {
                        req.setAttribute("status", status);
                }

                req.getRequestDispatcher("/vistas/reservarAreas.jsp").forward(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                req.setCharacterEncoding("UTF-8");
                String accion = n(req.getParameter("accion"));

                System.out.println("===== DEBUG RESERVA =====");
                System.out.println("areaId raw: " + req.getParameter("areaId"));
                System.out.println("anio raw: " + req.getParameter("anio"));
                System.out.println("mes raw: " + req.getParameter("mes"));
                System.out.println("dia raw: " + req.getParameter("dia"));
                System.out.println("desdeHora raw: " + req.getParameter("desdeHora"));
                System.out.println("hastaHora raw: " + req.getParameter("hastaHora"));
                System.out.println("========================");

                if ("reservar".equals(accion)) {
                        int areaId = parseInt(req.getParameter("areaId"), 1);
                        System.out.println("areaId parseado: " + areaId);
                        int anio = parseInt(req.getParameter("anio"), LocalDate.now().getYear());
                        int mes = parseInt(req.getParameter("mes"), LocalDate.now().getMonthValue());
                        int dia = parseInt(req.getParameter("dia"), LocalDate.now().getDayOfMonth());
                        int desde = parseInt(req.getParameter("desdeHora"), 0);
                        int hasta = parseInt(req.getParameter("hastaHora"), 0);

                        int hstart = parseInt(req.getParameter("hstart"), 0);
                        if (hstart < 0) {
                                hstart = 0;
                        }
                        if (hstart > 16) {
                                hstart = 16;
                        }
                        hstart = (hstart / 8) * 8;

                        LocalDate fecha = safeDate(anio, mes, dia);
                        LocalDate hoy = LocalDate.now(); 

                        //bloquear reservas en días anteriores ===
                        if (fecha.isBefore(hoy)) {
                                BitacoraService.log(req, "Reservas", BitacoraService.Accion.RESERVA,
                                        entidad(areaId), null,
                                        "Intento de reserva en fecha pasada",
                                        null, jsonReserva(areaId, null, fecha, desde, hasta, "Usuario del sistema", null, "RECHAZADA"),
                                        false, null);

                                resp.sendRedirect(req.getContextPath() + "/reservarAreas?areaId=" + areaId
                                        + "&anio=" + anio + "&mes=" + mes + "&dia=" + dia
                                        + "&hstart=" + hstart + "&status=past_date");
                                return;
                        }

                        if (desde < 0 || desde > 23 || hasta < 1 || hasta > 24 || desde >= hasta) {
                                BitacoraService.log(req, "Reservas", BitacoraService.Accion.RESERVA,
                                        entidad(areaId), null,
                                        "Intento de reserva con rango inválido",
                                        null, jsonReserva(areaId, null, fecha, desde, hasta, null, null, "ACTIVA"),
                                        false, null);

                                resp.sendRedirect(req.getContextPath() + "/reservarAreas?areaId=" + areaId + "&anio=" + anio + "&mes=" + mes + "&dia=" + dia
                                        + "&hstart=" + hstart + "&status=bad_range");
                                return;
                        }

                        String reservadoPor = "Usuario del sistema";
                        String correoResidente = null;
                        Object u = req.getSession().getAttribute("user");
                        if (u instanceof AuthUser) {
                                AuthUser au = (AuthUser) u;
                                String full = ((au.nombre == null ? "" : au.nombre.trim()) + " " + (au.apellidos == null ? "" : au.apellidos.trim())).trim();
                                if (!full.isEmpty()) {
                                        reservadoPor = full;
                                }
                                correoResidente = au.correo == null ? null : au.correo.trim();
                        }

                        Integer newId = null;
                        boolean conflict = false, dbErr = false;
                        try {
                                if (dao != null) {
                                        newId = dao.reservarRetId(areaId, fecha, desde, hasta, reservadoPor, correoResidente);
                                        if (newId == null) {
                                                try {
                                                        conflict = dao.existeCruce(areaId, fecha, desde, hasta);
                                                } catch (Throwable ignore) {
                                                }
                                        }
                                } else {
                                        dbErr = true;
                                }
                        } catch (Throwable t) {
                                t.printStackTrace();
                                dbErr = true;
                                newId = null;
                        }

                        // Correo de confirmación
                        if (newId != null && correoResidente != null && !correoResidente.isEmpty()) {
                                try {
                                        Map<Integer, String> areas = (dao != null) ? dao.listarAreas() : new LinkedHashMap<>();
                                        if (areas.isEmpty()) {
                                                areas.put(1, "Piscina");
                                                areas.put(2, "Salón");
                                        }
                                        String nombreArea = areas.getOrDefault(areaId, "Área común");
                                        CorreoUtil.enviarConfirmacionReserva(correoResidente, reservadoPor, nombreArea, fecha, desde, hasta);
                                } catch (Throwable mailEx) {
                                        mailEx.printStackTrace();
                                }
                        }

                        // Bitácora
                        if (newId != null) {
                                BitacoraService.log(req, "Reservas", BitacoraService.Accion.RESERVA,
                                        entidad(areaId), String.valueOf(newId),
                                        "Reserva creada",
                                        null, jsonReserva(areaId, newId, fecha, desde, hasta, reservadoPor, correoResidente, "ACTIVA"),
                                        true, null);
                        } else {
                                String motivo = conflict ? "Conflicto de horario" : (dbErr ? "Error de BD o DAO nulo" : "Fallo desconocido");
                                BitacoraService.log(req, "Reservas", BitacoraService.Accion.RESERVA,
                                        entidad(areaId), null,
                                        "No se pudo crear la reserva: " + motivo,
                                        null, jsonReserva(areaId, null, fecha, desde, hasta, reservadoPor, correoResidente, "ACTIVA"),
                                        false, null);
                        }

                        String code = (newId != null) ? "ok" : (conflict ? "conflict" : (dbErr ? "db_err" : "err"));
                        resp.sendRedirect(req.getContextPath() + "/reservarAreas?areaId=" + areaId + "&anio=" + anio + "&mes=" + mes + "&dia=" + dia
                                + "&hstart=" + hstart + "&status=" + code);
                        return;
                }

                if ("cancelar".equals(accion)) {
                        int areaId = parseInt(req.getParameter("areaId"), 1);
                        int reservaId = parseInt(req.getParameter("reservaId"), -1);
                        int anio = parseInt(req.getParameter("anio"), LocalDate.now().getYear());
                        int mes = parseInt(req.getParameter("mes"), LocalDate.now().getMonthValue());
                        int dia = parseInt(req.getParameter("dia"), LocalDate.now().getDayOfMonth());
                        int hstart = parseInt(req.getParameter("hstart"), 0);
                        if (hstart < 0) {
                                hstart = 0;
                        }
                        if (hstart > 16) {
                                hstart = 16;
                        }
                        hstart = (hstart / 8) * 8;

                        String correo = "";
                        Object u = req.getSession().getAttribute("user");
                        if (u instanceof AuthUser) {
                                correo = ((AuthUser) u).correo == null ? "" : ((AuthUser) u).correo.trim();
                        }

                        ReservaItem antes = null;
                        try {
                                if (dao != null && reservaId > 0 && !correo.isEmpty()) {
                                        antes = dao.obtenerReservaPorId(areaId, reservaId, correo);
                                }
                        } catch (Throwable t) {
                                t.printStackTrace();
                        }

                        boolean ok = false;
                        try {
                                if (!correo.isEmpty() && dao != null) {
                                        ok = dao.cancelarReserva(areaId, reservaId, correo);
                                }
                        } catch (Throwable t) {
                                t.printStackTrace();
                        }

                        if (ok) {
                                // Bitácora OK (con antes/después)
                                BitacoraService.log(req, "Reservas", BitacoraService.Accion.RESERVA,
                                        entidad(areaId), String.valueOf(reservaId),
                                        "Cancelación de reserva",
                                        jsonReservaItem(antes), jsonReservaDespues(areaId, reservaId, antes, "CANCELADA"),
                                        true, null);
                        } else {
                                // Bitácora ERROR
                                BitacoraService.log(req, "Reservas", BitacoraService.Accion.RESERVA,
                                        entidad(areaId), String.valueOf(reservaId),
                                        "Intento de cancelación no aplicado",
                                        jsonReservaItem(antes), null,
                                        false, null);
                        }

                        resp.sendRedirect(req.getContextPath() + "/reservarAreas?areaId=" + areaId + "&anio=" + anio + "&mes=" + mes + "&dia=" + dia
                                + "&hstart=" + hstart + (ok ? "&status=cancel_ok" : "&status=cancel_err"));
                        return;
                }

                if ("filtrar".equals(accion)) {
                        int areaId = parseInt(req.getParameter("areaId"), 1);
                        int anio = parseInt(req.getParameter("anio"), LocalDate.now().getYear());
                        int mes = parseInt(req.getParameter("mes"), LocalDate.now().getMonthValue());
                        int dia = parseInt(req.getParameter("dia"), LocalDate.now().getDayOfMonth());
                        int hstart = parseInt(req.getParameter("hstart"), 0);
                        if (hstart < 0) {
                                hstart = 0;
                        }
                        if (hstart > 16) {
                                hstart = 16;
                        }
                        hstart = (hstart / 8) * 8;
                        resp.sendRedirect(req.getContextPath() + "/reservarAreas?areaId=" + areaId + "&anio=" + anio + "&mes=" + mes + "&dia=" + dia
                                + "&hstart=" + hstart);
                        return;
                }

                doGet(req, resp);
        }

        /* ============== helpers ============== */
        private static String n(String s) {
                return s == null ? "" : s.trim();
        }

        private static int parseInt(String s, int def) {
                try {
                        return Integer.parseInt(s);
                } catch (Exception e) {
                        return def;
                }
        }

        private static int firstKey(Map<Integer, String> m) {
                for (Integer k : m.keySet()) {
                        return k;
                }
                return 1;
        }

        private static LocalDate safeDate(int y, int m, int d) {
                try {
                        return LocalDate.of(y, m, d);
                } catch (Exception e) {
                        return LocalDate.now();
                }
        }

        private static String entidad(int areaId) {
                return (areaId == 1) ? "reservas_piscina" : (areaId == 2 ? "reservas_salon" : "reservas");
        }

        // ===== JSON para bitácora =====
        private static String jsonReserva(int areaId, Integer id, LocalDate fecha, int desde, int hasta,
                String reservadoPor, String correo, String estado) {
                String area = (areaId == 1) ? "Piscina" : (areaId == 2 ? "Salón" : "Área común");
                String f = (fecha == null ? "" : fecha.toString());
                return new StringBuilder(180)
                        .append("{")
                        .append("\"id\":").append(id == null ? "null" : id).append(',')
                        .append("\"areaId\":").append(areaId).append(',')
                        .append("\"area\":\"").append(esc(area)).append("\",")
                        .append("\"fecha\":\"").append(esc(f)).append("\",")
                        .append("\"desde\":").append(desde).append(',')
                        .append("\"hasta\":").append(hasta).append(',')
                        .append("\"reservadoPor\":\"").append(esc(nullToEmpty(reservadoPor))).append("\",")
                        .append("\"correo\":\"").append(esc(nullToEmpty(correo))).append("\",")
                        .append("\"estado\":\"").append(esc(nullToEmpty(estado))).append("\"")
                        .append("}")
                        .toString();
        }

        private static String jsonReservaItem(ReservaItem r) {
                if (r == null) {
                        return null;
                }
                return jsonReserva(r.areaId, r.id, r.fecha, r.desdeHora, r.hastaHora, r.reservadoPor, r.correoResidente, r.estado);
        }

        private static String jsonReservaDespues(int areaId, int id, ReservaItem antes, String nuevoEstado) {
                if (antes == null) {
                        // mínimo
                        return new StringBuilder(80)
                                .append("{\"id\":").append(id).append(",\"estado\":\"").append(esc(nuevoEstado)).append("\"}")
                                .toString();
                }
                return jsonReserva(areaId, id, antes.fecha, antes.desdeHora, antes.hastaHora, antes.reservadoPor, antes.correoResidente, nuevoEstado);
        }

        private static String esc(String s) {
                if (s == null) {
                        return "";
                }
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private static String nullToEmpty(String s) {
                return s == null ? "" : s;
        }
}
