package Utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.mail.*;
import javax.mail.internet.*;

import Conexion_DB.Conexion;

public class CorreoUtil {

        // === Config SMTP global para todo el proyecto (con fallback a ENV) ===
        private static final String REMITENTE = "residencialmicasita43@gmail.com";
        private static final String CLAVE_APP = "ghgx qnzu zixk zbci"; // App Password
        private static final String FROM_NAME = "Administración Residencial San Rafael";

        private static final DateTimeFormatter FMT_DDMMYYYY_HHMM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        /* ===================== Infra de correo ===================== */
        private static Session buildSession() {
                final String user = getenvOrDefault("SMTP_USER", REMITENTE);
                final String pass = getenvOrDefault("SMTP_PASS", CLAVE_APP);
                final String host = getenvOrDefault("SMTP_HOST", "smtp.gmail.com");
                final String port = getenvOrDefault("SMTP_PORT", "587");

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");   // asegura TLS
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                props.put("mail.smtp.ssl.trust", host);             // confiar en host smtp
                props.put("mail.smtp.host", host);
                props.put("mail.smtp.port", port);
                props.put("mail.mime.charset", "UTF-8");
                // timeouts
                props.put("mail.smtp.timeout", "7000");
                props.put("mail.smtp.connectiontimeout", "7000");

                return Session.getInstance(props, new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(user, pass);
                        }
                });
        }

        private static String fromAddress() {
                String envFrom = System.getenv("MAIL_FROM");
                if (envFrom != null && !envFrom.trim().isEmpty()) {
                        return envFrom.trim();
                }
                String envUser = System.getenv("SMTP_USER");
                return (envUser != null && !envUser.trim().isEmpty()) ? envUser.trim() : REMITENTE;
        }

        private static String fromName() {
                String env = System.getenv("MAIL_FROM_NAME");
                return (env == null || env.trim().isEmpty()) ? FROM_NAME : env.trim();
        }

        private static String escapeHtml(String s) {
                if (s == null) {
                        return "";
                }
                return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        private static void setStdHeaders(Message m) throws MessagingException {
                m.addHeader("X-Priority", "3");
                m.addHeader("X-Mailer", "JavaMail");
                m.setSentDate(new java.util.Date());
                // Reply-To opcional al remitente
                try {
                        m.setReplyTo(new Address[]{new InternetAddress(fromAddress())});
                } catch (Exception ignore) {
                }
        }

        /* ===========================================================
       0) MÉTODOS GENÉRICOS (para NotificationService y uso general)
       =========================================================== */
        /**
         * Envío de correo de texto plano (NO lanza checked exceptions).
         */
        public static void enviarCorreo(String para, String asunto, String cuerpoPlano) {
                if (para == null || para.trim().isEmpty()) {
                        logMail("generico_texto", para, safe(asunto), false, "destinatario_vacio", 0L, null, null);
                        return;
                }
                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;
                try {
                        Session session = buildSession();
                        MimeMessage msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(para, false));
                        msg.setSubject(MimeUtility.encodeText(asunto == null ? "" : asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        msg.setText(cuerpoPlano == null ? "" : cuerpoPlano, "UTF-8");
                        Transport.send(msg);
                        ok = true;
                } catch (Exception ex) {
                        err = trimLen(ex.toString(), 600);
                        ex.printStackTrace(); // RN01: no romper flujo
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("generico_texto", para, safe(asunto), ok, err, ms, null, null);
                }
        }

        /**
         * Envío con cuerpo HTML + alternativa de texto plano (NO lanza checked
         * exceptions).
         */
        public static void enviarCorreoHtml(String para, String asunto, String html, String plano) {
                if (para == null || para.trim().isEmpty()) {
                        logMail("generico_html", para, safe(asunto), false, "destinatario_vacio", 0L, null, null);
                        return;
                }
                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;
                try {
                        Session session = buildSession();
                        MimeMessage msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(para, false));
                        msg.setSubject(MimeUtility.encodeText(asunto == null ? "" : asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        MimeBodyPart text = new MimeBodyPart();
                        text.setText(plano == null ? "" : plano, "UTF-8");
                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(html == null ? "" : html, "text/html; charset=UTF-8");

                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(text);
                        alt.addBodyPart(htmlPart);

                        msg.setContent(alt);
                        Transport.send(msg);
                        ok = true;
                } catch (Exception ex) {
                        err = trimLen(ex.toString(), 600);
                        ex.printStackTrace();
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("generico_html", para, safe(asunto), ok, err, ms, null,
                                json("len_html", len(html), "len_texto", len(plano)));
                }
        }

        public static void enviarAvisoResidenteVisita(
                String correoResidente,
                String nombreResidente,
                String nombreVisitante,
                String validezTexto
        ) throws MessagingException, UnsupportedEncodingException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;

                try {
                        Session session = buildSession();

                        // RN6 pide asunto exacto
                        String asunto = "Notificación de accesos creados";

                        // Usamos la fecha/hora del servidor en el momento del envío
                        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
                        java.time.format.DateTimeFormatter fFecha = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        java.time.format.DateTimeFormatter fHora = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                        String fecha = ahora.format(fFecha);
                        String hora = ahora.format(fHora);

                        // Texto EXACTO RN6 (sin comillas externas). Variables con escape en HTML.
                        String html = new StringBuilder()
                                .append("<p>El código QR fue generado exitosamente para la persona <b>")
                                .append(escapeHtml(nombreVisitante)).append("</b> el día <b>")
                                .append(escapeHtml(fecha)).append("</b> a las <b>")
                                .append(escapeHtml(hora))
                                .append("</b> para acceder al condominio. Este código tiene una validez de <b>")
                                .append(escapeHtml(validezTexto))
                                .append("</b>. En caso de cualquier irregularidad, por favor contacte al administrador del sistema.</p>")
                                .toString();

                        String plano
                                = "El código QR fue generado exitosamente para la persona " + nombreVisitante
                                + " el día " + fecha + " a las " + hora + " para acceder al condominio. "
                                + "Este código tiene una validez de " + validezTexto + ". "
                                + "En caso de cualquier irregularidad, por favor contacte al administrador del sistema.";

                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(correoResidente));
                        msg.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        MimeBodyPart text = new MimeBodyPart();
                        text.setText(plano, "UTF-8");
                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(html, "text/html; charset=UTF-8");

                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(text);
                        alt.addBodyPart(htmlPart);

                        msg.setContent(alt);
                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | UnsupportedEncodingException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        // Mantengo la clave de plantilla original para no romper reportes previos
                        logMail("aviso_residente_visita", correoResidente, "Notificación de accesos creados",
                                ok, err, ms, null,
                                json("residente", nombreResidente, "visitante", nombreVisitante, "validez", validezTexto));
                }
        }

        public static void enviarQRVisitaAVisitante(
                String correoVisitante,
                String nombreVisitante,
                String nombreResidente,
                String rutaPNG,
                String validezTexto
        ) throws MessagingException, IOException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;
                Integer bytes = null;

                try {
                        Session session = buildSession();

                        String asunto = "Notificación de accesos creados";

                        // HTML fiel a RN7 (con <ul>) y detalles requeridos
                        String html = new StringBuilder()
                                .append("<p>¡Hola!</p>")
                                .append("<p>Se ha generado exitosamente tu código QR de acceso al residencial. ")
                                .append("A continuación, encontrarás los detalles de tu registro:</p>")
                                .append("<p><b>Nombre del visitante:</b> ").append(escapeHtml(nombreVisitante)).append("<br>")
                                .append("<b>Validez del código QR:</b> ").append(escapeHtml(validezTexto)).append("</p>")
                                .append("<p><b>Instrucciones importantes:</b></p>")
                                .append("<ul>")
                                .append("<li>Guarda este correo o el código QR adjunto.</li>")
                                .append("<li>Preséntalo al llegar al residencial para que el personal de seguridad lo escanee y valide tu acceso.</li>")
                                .append("</ul>")
                                .append("<p>¡Gracias por coordinar tu visita con anticipación!</p>")
                                .toString();

                        // Texto plano espejo (con guiones como viñetas)
                        String plano = "¡Hola!\n"
                                + "Se ha generado exitosamente tu código QR de acceso al residencial. A continuación, encontrarás los detalles de tu registro:\n"
                                + "Nombre del visitante: " + nombreVisitante + "\n"
                                + "Validez del código QR: " + validezTexto + "\n"
                                + "Instrucciones importantes:\n"
                                + "- Guarda este correo o el código QR adjunto.\n"
                                + "- Preséntalo al llegar al residencial para que el personal de seguridad lo escanee y valide tu acceso.\n"
                                + "¡Gracias por coordinar tu visita con anticipación!";

                        File adjFile = new File(rutaPNG);
                        if (!adjFile.exists() || !adjFile.isFile()) {
                                throw new IOException("No se encontró el archivo PNG del QR en: " + rutaPNG);
                        }
                        bytes = (int) Math.min(Integer.MAX_VALUE, adjFile.length());

                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(correoVisitante));
                        msg.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        // Alternativas texto/HTML
                        MimeBodyPart text = new MimeBodyPart();
                        text.setText(plano, "UTF-8");
                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(html, "text/html; charset=UTF-8");
                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(text);
                        alt.addBodyPart(htmlPart);

                        // Cuerpo
                        MimeBodyPart cuerpo = new MimeBodyPart();
                        cuerpo.setContent(alt);

                        // Adjunto QR (igual que ya tenías)
                        MimeBodyPart adj = new MimeBodyPart();
                        adj.attachFile(adjFile);
                        adj.setFileName(MimeUtility.encodeText("QR_visita.png", "UTF-8", "B"));
                        adj.setHeader("Content-Transfer-Encoding", "base64");

                        // Ensamble mixed
                        MimeMultipart mixed = new MimeMultipart("mixed");
                        mixed.addBodyPart(cuerpo);
                        mixed.addBodyPart(adj);

                        msg.setContent(mixed);
                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | IOException ex) {
                        err = (ex.toString().length() > 600) ? ex.toString().substring(0, 600) : ex.toString();
                        throw ex;
                } finally {
                        long ms = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        // Mantengo la clave de plantilla original
                        logMail("qr_visita", correoVisitante, "Notificación de accesos creados",
                                ok, err, ms, bytes,
                                json("visitante", nombreVisitante, "residente", nombreResidente, "validez", validezTexto, "archivo", rutaPNG));
                }
        }


        /* ===========================================================
       C) Notificación de ACCESO AUTORIZADO (CU Acceso QR)
       =========================================================== */
        public static void enviarNotificacionAcceso(
                String destinatario,
                String nombrePersona,
                LocalDateTime fechaHoraUso,
                String validezTexto
        ) throws MessagingException, UnsupportedEncodingException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;

                try {
                        Session session = buildSession();

                        DateTimeFormatter fFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        DateTimeFormatter fHora = DateTimeFormatter.ofPattern("HH:mm");

                        String subject = "Notificación de acceso";
                        String fecha = fechaHoraUso.format(fFecha);
                        String hora = fechaHoraUso.format(fHora);

                        String html = "<p>El código QR generado para la persona <b>" + escapeHtml(nombrePersona)
                                + "</b> fue utilizado exitosamente el día <b>" + fecha + "</b> a las <b>"
                                + escapeHtml(hora) + "</b> para acceder al condominio. "
                                + "Validez del código: <b>" + escapeHtml(validezTexto) + "</b>.</p>";

                        String texto = "El código QR generado para " + nombrePersona + " fue utilizado el "
                                + fecha + " a las " + hora + ". Validez: " + validezTexto + ".";

                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
                        msg.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
                        setStdHeaders(msg);

                        MimeBodyPart t = new MimeBodyPart();
                        t.setText(texto, "UTF-8");
                        MimeBodyPart h = new MimeBodyPart();
                        h.setContent(html, "text/html; charset=UTF-8");

                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(t);
                        alt.addBodyPart(h);

                        msg.setContent(alt);
                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | UnsupportedEncodingException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("notificacion_acceso", destinatario, "Notificación de acceso",
                                ok, err, ms, null,
                                json("persona", nombrePersona, "validez", validezTexto,
                                        "uso_en", fechaHoraUso == null ? null : fechaHoraUso.toString()));
                }
        }

        // Alias para compatibilidad
        public static void enviarNotificacionAccesoRN2(
                String destinatario,
                String nombrePersona,
                LocalDateTime fechaHoraUso,
                String validezTexto
        ) throws MessagingException, UnsupportedEncodingException {
                enviarNotificacionAcceso(destinatario, nombrePersona, fechaHoraUso, validezTexto);
        }

        /* ===========================================================
       D) Cuentas de RESIDENTES (con QR adjunto)
       =========================================================== */
        public static void enviarCorreoConQR(String destinatario, String rutaQR, String nombreCompleto)
                throws MessagingException, IOException, UnsupportedEncodingException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;
                Integer bytes = null;

                try {
                        Session session = buildSession();

                        String asunto = "Tu código de acceso QR - Residencial San Rafael";
                        String html = new StringBuilder()
                                .append("<p>Hola <b>").append(escapeHtml(nombreCompleto)).append("</b>,</p>")
                                .append("<p>Adjuntamos tu <b>código QR de acceso</b> al residencial.</p>")
                                .append("<p><b>Indicaciones:</b><br>")
                                .append("• Guarda este correo o el archivo adjunto.<br>")
                                .append("• Preséntalo en la garita para su lectura.<br>")
                                .append("• No lo compartas con terceros.</p>")
                                .toString();

                        String plano = "Hola " + nombreCompleto + ",\n\n"
                                + "Adjuntamos tu código QR de acceso al residencial.\n\n"
                                + "Indicaciones:\n"
                                + "- Guarda este correo o el archivo adjunto.\n"
                                + "- Preséntalo en la garita para su lectura.\n"
                                + "- No lo compartas con terceros.\n";

                        File archivoQR = new File(rutaQR);
                        if (!archivoQR.exists() || !archivoQR.isFile()) {
                                throw new IOException("No se encontró el archivo PNG del QR en: " + rutaQR);
                        }
                        bytes = (int) Math.min(Integer.MAX_VALUE, archivoQR.length());

                        Message mensaje = new MimeMessage(session);
                        mensaje.setFrom(new InternetAddress(fromAddress(), fromName()));
                        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
                        mensaje.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(mensaje);

                        MimeBodyPart cuerpoTxt = new MimeBodyPart();
                        cuerpoTxt.setText(plano, "UTF-8");
                        MimeBodyPart cuerpoHtml = new MimeBodyPart();
                        cuerpoHtml.setContent(html, "text/html; charset=UTF-8");
                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(cuerpoTxt);
                        alt.addBodyPart(cuerpoHtml);

                        MimeBodyPart cuerpo = new MimeBodyPart();
                        cuerpo.setContent(alt);

                        MimeBodyPart adjuntoQR = new MimeBodyPart();
                        adjuntoQR.attachFile(archivoQR);
                        adjuntoQR.setFileName(MimeUtility.encodeText("QR_residente.png", "UTF-8", "B"));
                        adjuntoQR.setHeader("Content-Transfer-Encoding", "base64");

                        MimeMultipart mixed = new MimeMultipart("mixed");
                        mixed.addBodyPart(cuerpo);
                        mixed.addBodyPart(adjuntoQR);

                        mensaje.setContent(mixed);
                        Transport.send(mensaje);
                        ok = true;
                } catch (MessagingException | IOException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("qr_residente_nuevo", destinatario, "Tu código de acceso QR - Residencial San Rafael",
                                ok, err, ms, bytes, json("nombre", nombreCompleto, "archivo", rutaQR));
                }

        }

        public static void enviarCorreoQRActualizado(String destinatario, String rutaQR)
                throws MessagingException, UnsupportedEncodingException, IOException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;
                Integer bytes = null;

                try {
                        Session session = buildSession();

                        String asunto = "Actualización de tu código QR";
                        String cuerpoPlano = "Hola,\n\n"
                                + "Tu código QR de acceso a Residencial San Rafael ha sido actualizado.\n"
                                + "Por favor, utiliza el nuevo código que se adjunta en este correo.\n\n"
                                + "Saludos cordiales,\n"
                                + "Administración";

                        File archivoQR = new File(rutaQR);
                        if (!archivoQR.exists() || !archivoQR.isFile()) {
                                throw new IOException("No se encontró el archivo PNG del QR en: " + rutaQR);
                        }
                        bytes = (int) Math.min(Integer.MAX_VALUE, archivoQR.length());

                        Message mensaje = new MimeMessage(buildSession());
                        mensaje.setFrom(new InternetAddress(fromAddress(), fromName()));
                        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
                        mensaje.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(mensaje);

                        MimeBodyPart cuerpoTexto = new MimeBodyPart();
                        cuerpoTexto.setText(cuerpoPlano, "UTF-8");

                        MimeBodyPart adjuntoQR = new MimeBodyPart();
                        adjuntoQR.attachFile(archivoQR);
                        adjuntoQR.setFileName(MimeUtility.encodeText("QR_residente_actualizado.png", "UTF-8", "B"));
                        adjuntoQR.setHeader("Content-Transfer-Encoding", "base64");

                        MimeMultipart mixed = new MimeMultipart("mixed");
                        mixed.addBodyPart(cuerpoTexto);
                        mixed.addBodyPart(adjuntoQR);

                        mensaje.setContent(mixed);
                        Transport.send(mensaje);
                        ok = true;
                } catch (MessagingException | IOException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("qr_residente_actualizado", destinatario, "Actualización de tu código QR",
                                ok, err, ms, bytes, json("archivo", rutaQR));
                }

        }

        /* ===== Overloads de compatibilidad ===== */
        public static void enviarCorreoConQR(String destinatario, File archivoQR, String nombreCompleto)
                throws MessagingException, IOException, UnsupportedEncodingException {
                enviarCorreoConQR(destinatario, archivoQR.getAbsolutePath(), nombreCompleto);
        }

        public static void enviarCorreoConQR(String destinatario, String rutaQR)
                throws MessagingException, IOException, UnsupportedEncodingException {
                enviarCorreoConQR(destinatario, rutaQR, "Residente");
        }

        public static void enviarCorreoQRActualizado(String destinatario, File archivoQR)
                throws MessagingException, UnsupportedEncodingException, IOException {
                enviarCorreoQRActualizado(destinatario, archivoQR.getAbsolutePath());
        }

        // Alias para tolerar el typo común "Acutalizado"
        public static void enviarCorreoQRAcutalizado(String destinatario, String rutaQR)
                throws MessagingException, UnsupportedEncodingException, IOException {
                enviarCorreoQRActualizado(destinatario, rutaQR);
        }

        // ====== NUEVO: correo genérico para soporte (HTML + texto plano) ======
        public static void enviarSoporteReporteMantenimiento(
                String destinatario,
                String asunto,
                String html,
                String plano
        ) throws MessagingException, UnsupportedEncodingException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;

                try {
                        Session session = buildSession();

                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
                        msg.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        MimeBodyPart text = new MimeBodyPart();
                        text.setText(plano == null ? "" : plano, "UTF-8");

                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(html == null ? "" : html, "text/html; charset=UTF-8");

                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(text);
                        alt.addBodyPart(htmlPart);

                        msg.setContent(alt);
                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | UnsupportedEncodingException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("soporte_rep_mant", destinatario, safe(asunto),
                                ok, err, ms, null,
                                json("len_html", len(html), "len_texto", len(plano)));
                }
        }

        // (Opcional) Overload para varios destinatarios
        public static void enviarSoporteReporteMantenimiento(
                String[] destinatarios,
                String asunto,
                String html,
                String plano
        ) throws MessagingException, UnsupportedEncodingException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;

                try {
                        Session session = buildSession();

                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));

                        InternetAddress[] tos = new InternetAddress[destinatarios.length];
                        for (int i = 0; i < destinatarios.length; i++) {
                                tos[i] = new InternetAddress(destinatarios[i]);
                        }
                        msg.setRecipients(Message.RecipientType.TO, tos);

                        msg.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        MimeBodyPart text = new MimeBodyPart();
                        text.setText(plano == null ? "" : plano, "UTF-8");

                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(html == null ? "" : html, "text/html; charset=UTF-8");

                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(text);
                        alt.addBodyPart(htmlPart);

                        msg.setContent(alt);
                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | UnsupportedEncodingException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        String joined = (destinatarios == null) ? "" : String.join(",", destinatarios);
                        logMail("soporte_rep_mant_multi", joined, safe(asunto),
                                ok, err, ms, null,
                                json("len_html", len(html), "len_texto", len(plano), "dest_count", (destinatarios == null ? 0 : destinatarios.length)));
                }
        }

        /* ===========================================================
       E) Notificación de RESERVA DE ÁREA al RESIDENTE (NUEVO)
       =========================================================== */
        public static void enviarConfirmacionReserva(
                String correoDestinatario,
                String nombreResidente,
                String nombreArea,
                LocalDate fecha,
                int horaInicio,
                int horaFin
        ) throws MessagingException, UnsupportedEncodingException {

                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;

                try {
                        Session session = buildSession();

                        DateTimeFormatter fFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        String fechaStr = (fecha == null) ? "" : fecha.format(fFecha);

                        // Normaliza el rango [inicio, fin) y formatea como HH:mm
                        int hiNum = Math.max(0, Math.min(23, horaInicio));
                        int hfNum = Math.max(1, Math.min(24, horaFin));
                        String hi = String.format("%02d:00", hiNum);
                        String hf = String.format("%02d:00", hfNum);

                        String asunto = "Confirmación de reserva - " + escapeHtml(nombreArea);

                        String html = new StringBuilder()
                                .append("<p>Estimado(a) <b>").append(escapeHtml(nombreResidente)).append("</b>,</p>")
                                .append("<p>Su reserva para el área común <b>").append(escapeHtml(nombreArea)).append("</b> ")
                                .append("ha sido <b>confirmada</b> exitosamente para el día <b>").append(escapeHtml(fechaStr))
                                .append("</b> en el horario de <b>").append(escapeHtml(hi)).append("</b> a <b>")
                                .append(escapeHtml(hf)).append("</b>.</p>")
                                .append("<p><b>Le recordamos:</b><br>")
                                .append("• Revisar las políticas de uso del espacio.<br>")
                                .append("• Respetar los tiempos asignados.<br>")
                                .append("• Notificar con al menos 24 horas de anticipación en caso de cancelación o modificación.</p>")
                                .append("<p>Gracias por contribuir a un uso ordenado de nuestros recursos comunitarios.</p>")
                                .toString();

                        String plano = "Estimado(a) " + nombreResidente + ",\n\n"
                                + "Su reserva para el área común " + nombreArea + " ha sido confirmada exitosamente "
                                + "para el día " + fechaStr + " en el horario de " + hi + " a " + hf + ".\n\n"
                                + "Le recordamos:\n"
                                + "- Revisar las políticas de uso del espacio.\n"
                                + "- Respetar los tiempos asignados.\n"
                                + "- Notificar con al menos 24 horas de anticipación en caso de cancelación o modificación.\n\n"
                                + "Gracias por contribuir a un uso ordenado de nuestros recursos comunitarios.\n";

                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(correoDestinatario));
                        msg.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        MimeBodyPart text = new MimeBodyPart();
                        text.setText(plano, "UTF-8");

                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(html, "text/html; charset=UTF-8");

                        MimeMultipart alt = new MimeMultipart("alternative");
                        alt.addBodyPart(text);
                        alt.addBodyPart(htmlPart);

                        msg.setContent(alt);
                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | UnsupportedEncodingException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("confirmacion_reserva", correoDestinatario, "Confirmación de reserva - " + safe(nombreArea),
                                ok, err, ms, null,
                                json("residente", nombreResidente, "area", nombreArea,
                                        "fecha", (fecha == null ? null : fecha.toString()),
                                        "desde", horaInicio, "hasta", horaFin));
                }
        }

        //notifica al residente que su paqueteria fue ENTREGADO
        public static void enviarNotificacionEntregaPaquete(String correoResidente, String numeroGuia, LocalDateTime fechaHoraEntrega) throws MessagingException, UnsupportedEncodingException {
                long t0 = System.nanoTime();
                boolean ok = false;
                String err = null;

                final String asunto = "Entrega de Paquetería";
                final String fechaTxt = (fechaHoraEntrega == null) ? "" : FMT_DDMMYYYY_HHMM.format(fechaHoraEntrega);

                final String cuerpoPlano = "Se le informa que se ha entregado paquete con identificación " + (numeroGuia == null ? "(sin guía)" : numeroGuia) + ", en la fecha " + fechaTxt + ".";

                try {
                        Session session = buildSession();

                        MimeMessage msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(fromAddress(), fromName()));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(correoResidente));
                        msg.setSubject(MimeUtility.encodeText(asunto, "UTF-8", "B"));
                        setStdHeaders(msg);

                        msg.setText(cuerpoPlano, "UTF-8");

                        Transport.send(msg);
                        ok = true;
                } catch (MessagingException | UnsupportedEncodingException ex) {
                        err = trimLen(ex.toString(), 600);
                        throw ex;
                } finally {
                        long ms = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        logMail("paq_entrega_residente", correoResidente, asunto, ok, err, ms, null, json("guia", numeroGuia, "entrega", fechaHoraEntrega == null ? null : fechaHoraEntrega.toString()));
                }
        }

        public static void enviarNotificacionEntregaPaquete(
                String correoResidente,
                String numeroGuia,
                java.sql.Timestamp fechaHoraEntrega
        ) throws MessagingException, UnsupportedEncodingException {
                LocalDateTime ldt = (fechaHoraEntrega == null) ? null : fechaHoraEntrega.toLocalDateTime();
                enviarNotificacionEntregaPaquete(correoResidente, numeroGuia, ldt);
        }


        /* ===================== Helpers genéricos ===================== */
        private static String getenvOrDefault(String k, String def) {
                String v = System.getenv(k);
                return (v == null || v.trim().isEmpty()) ? def : v.trim();
        }

        private static String safe(String s) {
                return s == null ? "" : s;
        }

        private static String trimLen(String s, int max) {
                if (s == null) {
                        return null;
                }
                if (s.length() <= max) {
                        return s;
                }
                return s.substring(0, Math.max(0, max));
        }

        private static Integer len(String s) {
                return s == null ? null : s.length();
        }

        // JSON mínimo sin dependencias
        private static String jstr(String s) {
                if (s == null) {
                        return "null";
                }
                return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        private static String jsonKV(String k, Object v) {
                String key = jstr(k);
                String val;
                if (v == null) {
                        val = "null";
                } else if (v instanceof Number || v instanceof Boolean) {
                        val = String.valueOf(v);
                } else {
                        val = jstr(String.valueOf(v));
                }
                return key + ":" + val;
        }

        private static String json(Object... kvPairs) {
                try {
                        StringBuilder sb = new StringBuilder("{");
                        for (int i = 0; i < kvPairs.length; i += 2) {
                                if (i > 0) {
                                        sb.append(',');
                                }
                                sb.append(jsonKV(String.valueOf(kvPairs[i]),
                                        (i + 1 < kvPairs.length) ? kvPairs[i + 1] : null));
                        }
                        return sb.append('}').toString();
                } catch (Exception e) {
                        return null;
                }
        }

        /* ===================== Bitácora de correo ===================== */
        private static volatile boolean LOG_SCHEMA_READY = false;

        private static void ensureLogSchema() {
                if (LOG_SCHEMA_READY) {
                        return;
                }
                synchronized (CorreoUtil.class) {
                        if (LOG_SCHEMA_READY) {
                                return;
                        }
                        try (Connection con = Conexion.getConnection();
                                Statement st = con.createStatement()) {
                                st.execute(
                                        "CREATE TABLE IF NOT EXISTS mail_log ("
                                        + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                                        + "  plantilla VARCHAR(80) NOT NULL,"
                                        + "  para VARCHAR(300) NULL,"
                                        + "  asunto VARCHAR(300) NULL,"
                                        + "  ok TINYINT(1) NOT NULL,"
                                        + "  error TEXT NULL,"
                                        + "  dur_ms INT NULL,"
                                        + "  bytes INT NULL,"
                                        + "  extra LONGTEXT NULL,"
                                        + "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                        + "  KEY idx_ok (ok),"
                                        + "  KEY idx_created (created_at)"
                                        + ")"
                                );
                                LOG_SCHEMA_READY = true;
                        } catch (Exception e) {
                                // No bloquear si falla el auto-setup
                                System.err.println("[WARN] mail_log schema: " + e.getMessage());
                        }
                }
        }

        private static void logMail(String plantilla, String para, String asunto,
                boolean ok, String error, Long durMs, Integer bytes, String extraJson) {
                ensureLogSchema();
                final String sql = "INSERT INTO mail_log(plantilla,para,asunto,ok,error,dur_ms,bytes,extra) VALUES (?,?,?,?,?,?,?,?)";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, trimLen(plantilla, 80));
                        ps.setString(2, trimLen(para, 300));
                        ps.setString(3, trimLen(asunto, 300));
                        ps.setInt(4, ok ? 1 : 0);
                        if (error == null) {
                                ps.setNull(5, Types.LONGVARCHAR);
                        } else {
                                ps.setString(5, error);
                        }
                        if (durMs == null) {
                                ps.setNull(6, Types.INTEGER);
                        } else {
                                ps.setInt(6, (int) Math.min(Integer.MAX_VALUE, Math.max(0, durMs)));
                        }
                        if (bytes == null) {
                                ps.setNull(7, Types.INTEGER);
                        } else {
                                ps.setInt(7, bytes);
                        }
                        if (extraJson == null) {
                                ps.setNull(8, Types.LONGVARCHAR);
                        } else {
                                ps.setString(8, extraJson);
                        }
                        ps.executeUpdate();
                } catch (Exception e) {
                        // No bloquear la app si falla la bitácora
                        System.err.println("[WARN] mail_log insert: " + e.getMessage());
                }
        }
}
