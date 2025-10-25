package Servicio;

import PagosDAO.PagoDAO;
import PagosDAO.TipoPagosDAO;
import modelo.Pago;
import modelo.TipoPago;
import modelo.dto.CalculoPagoDTO;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class PagoService {

        private final PagoDAO pagoDAO = new PagoDAO();
        private final TipoPagosDAO tipoDAO = new TipoPagosDAO();

        private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
        private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        private static final SecureRandom RAND = new SecureRandom();

        /**
         * Monto base desde catálogo por ID.
         * @param tipoPagoId
         * @return 
         */
        public BigDecimal obtenerMontoBasePorTipoId(int tipoPagoId) {
                TipoPago t = tipoDAO.buscarPorId(tipoPagoId);
                if (t == null || !t.isActivo()) {
                        throw new IllegalArgumentException("Tipo de pago inválido");
                }
                return t.getMontoBase();
        }

        /**
         * Ejemplo de mora: Q25 por mes vencido si ya pasó el día 5. Ajusta si
         * tu regla es otra.
         */
        public double calcularMora(YearMonth mesCobrado, LocalDate hoy) {
                LocalDate fechaCorte = LocalDate.of(mesCobrado.getYear(), mesCobrado.getMonth(), 6);
                if (!hoy.isAfter(fechaCorte)) {
                        return 0.0;
                }
                long mesesAtraso = ChronoUnit.MONTHS.between(YearMonth.from(fechaCorte), YearMonth.from(hoy));
                if (mesesAtraso < 0) {
                        mesesAtraso = 0;
                }
                return 25.0 * (mesesAtraso + 1);
        }

        /**
         * yyyy-MM del mes objetivo.
         */
        public String determinarMesObjetivo(int usuarioId, LocalDate fechaCreacionUsuario) throws SQLException {
                String ultimo = pagoDAO.findUltimoMesPagoMantenimiento(usuarioId);
                if (ultimo == null || ultimo.trim().isEmpty()) {
                        YearMonth base = (fechaCreacionUsuario != null) ? YearMonth.from(fechaCreacionUsuario) : YearMonth.now();
                        return MES_FMT.format(base);
                } else {
                        YearMonth ym = YearMonth.parse(ultimo);
                        return MES_FMT.format(ym.plusMonths(1));
                }
        }

        /**
         * Arma cálculo usando tipo **por ID** (lee catálogo).
         */
        public CalculoPagoDTO armarCalculo(int tipoPagoId, int usuarioId, String nombreUsuario,
                LocalDate fechaCreacionUsuario, LocalDateTime ahora) throws SQLException {
                TipoPago tipo = tipoDAO.buscarPorId(tipoPagoId);
                if (tipo == null || !tipo.isActivo()) {
                        throw new IllegalArgumentException("Tipo de pago inválido");
                }

                CalculoPagoDTO dto = new CalculoPagoDTO();
                dto.usuarioId = usuarioId;
                dto.nombreUsuario = nombreUsuario;
                dto.tipo = tipo.getCodigo();
                dto.montoBase = tipo.getMontoBase().doubleValue();

                if (tipo.isRecurrente()) {
                        String mesAnio = determinarMesObjetivo(usuarioId, fechaCreacionUsuario);
                        dto.mesAnio = mesAnio;
                        YearMonth ym = YearMonth.parse(mesAnio);
                        dto.mora = calcularMora(ym, ahora.toLocalDate());
                } else {
                        dto.mesAnio = null;
                        dto.mora = 0.0;
                }
                dto.total = dto.montoBase + dto.mora;
                return dto;
        }

        public CalculoPagoDTO armarCalculo(int tipoPagoId, int usuarioId, String nombreUsuario) throws SQLException {
                return armarCalculo(tipoPagoId, usuarioId, nombreUsuario, null, LocalDateTime.now());
        }

        /**
         * Registrar pago con PAN crudo usando tipo por ID.
         * @param usuarioId
         * @param nombreUsuario
         * @param tipoPagoId
         * @param mesAnio
         * @param observaciones
         * @param tarjetaPanCrudo
         * @param fechaPago
         * @return 
         * @throws java.sql.SQLException 
         */
        public Pago registrarPago(int usuarioId, String nombreUsuario, int tipoPagoId,
                String mesAnio, String observaciones, String tarjetaPanCrudo,
                LocalDateTime fechaPago) throws SQLException {

                if (observaciones == null || observaciones.trim().length() < 5) {
                        throw new IllegalArgumentException("Observaciones obligatorias (mínimo 5 caracteres).");
                }
                String pan = onlyDigits(tarjetaPanCrudo);
                if (pan.length() < 13 || pan.length() > 19) {
                        throw new IllegalArgumentException("PAN inválido.");
                }
                String masked = maskPan(pan);
                String authCode = String.format("%06d", RAND.nextInt(1_000_000));

                TipoPago tipo = tipoDAO.buscarPorId(tipoPagoId);
                if (tipo == null || !tipo.isActivo()) {
                        throw new IllegalArgumentException("Tipo inválido");
                }

                double base = tipo.getMontoBase().doubleValue();
                double mora = 0.0;
                if (tipo.isRecurrente()) {
                        YearMonth ym = (mesAnio == null || mesAnio.trim().isEmpty())
                                ? YearMonth.now() : YearMonth.parse(mesAnio);
                        mora = calcularMora(ym, LocalDate.now());
                        mesAnio = ym.toString();
                } else {
                        mesAnio = null;
                }
                double total = base + mora;

                // ... dentro de tu método guardar/crearPago ...
                Pago p = new Pago();

                p.setUsuarioId(usuarioId);
                p.setTipoPagoId(tipoPagoId);          // nuevo campo en modelo
                p.setTipo(tipo.getCodigo());          // compat con vistas
                p.setMesAnio(mesAnio);
                p.setMontoBase(base);
                p.setMora(mora);
                p.setTotal(total);
                p.setFechaPago(fechaPago != null ? fechaPago : java.time.LocalDateTime.now());
                p.setObservaciones(observaciones);
                p.setTarjetaMasked(masked);
                p.setAuthCode(authCode);
                p.setStatus(Pago.Status.APROBADO);

// inserta y devuelve id
                int id = pagoDAO.insert(p);
                p.setId(id);
                return p;

        }

        // Helpers
        private static String onlyDigits(String s) {
                return s == null ? "" : s.replaceAll("\\D", "");
        }

        private static String maskPan(String pan) {
                String d = onlyDigits(pan);
                if (d.length() < 4) {
                        return "****";
                }
                return "**** **** **** " + d.substring(d.length() - 4);
        }
}
