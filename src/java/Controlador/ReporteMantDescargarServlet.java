package Controlador;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// PDFBox
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
//import org.apache.pdfbox.pdmodel.edit.PDPageContentStream; // para PDFBox 1.x
// Para PDFBox 2.x usa:
import org.apache.pdfbox.pdmodel.PDPageContentStream;   // <-- quita la import 1.x si usas 2.x

@WebServlet(name = "ReporteMantDescargarServlet", urlPatterns = {"/reporteMantDescargar"})
public class ReporteMantDescargarServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Intenta usar sesión si existe, pero no 404 si no
        HttpSession s = req.getSession(false);

        String nombre = getOr(req, s, "rm_nombre", "nombre");
        String tipo = getOr(req, s, "rm_tipo", "tipo");
        String desc = getOr(req, s, "rm_desc", "desc");
        String fecha = getOr(req, s, "rm_fecha", "fecha");

        boolean hayDatos = !(isEmpty(nombre) && isEmpty(tipo) && isEmpty(desc) && isEmpty(fecha));

        // ===== Construir texto del PDF =====
        List<String> lines = new ArrayList<>();
        lines.add("Nuevo reporte de mantenimiento");
        lines.add("");
        if (hayDatos) {
            lines.add("El residente: " + nullToDash(nombre) + " ha ingresado un reporte del sistema. El detalle del reporte es:");
            lines.add("Tipo de inconveniente: " + nullToDash(tipo));
            lines.add("Descripción:");
            for (String ln : wrap(desc, 95)) {
                lines.add("  " + ln);
            }
            lines.add("Fecha y hora: " + nullToDash(fecha));
        } else {
            lines.add("No se encontraron datos del último reporte.");
            lines.add("Genera el correo y vuelve a intentar descargar.");
        }
        lines.add("");
        lines.add("Por favor, tomar las acciones correspondientes.");

        // ===== Generar PDF (PDFBox 2.x) =====
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        float margin = 50f;
        float y = page.getMediaBox().getHeight() - margin;
        float leading = 14f;

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
        cs.beginText();
        cs.newLineAtOffset(margin, y);
        cs.showText("Reporte de Mantenimiento");
        cs.endText();

        y -= 24f;

        cs.setFont(PDType1Font.HELVETICA, 11);
        for (String ln : lines) {
            if (y < margin + leading) {
                cs.close();
                PDPage next = new PDPage(PDRectangle.LETTER);
                doc.addPage(next);
                cs = new PDPageContentStream(doc, next);
                y = next.getMediaBox().getHeight() - margin;
                cs.setFont(PDType1Font.HELVETICA, 11);
            }
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText(safe(ln));
            cs.endText();
            y -= leading;
        }
        cs.close();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String fileName = "reporte_mantenimiento_" + ts + ".pdf";

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        resp.setContentLength(baos.size());
        baos.writeTo(resp.getOutputStream());
        resp.getOutputStream().flush();
    }

    // ===== helpers =====
    private static String getOr(HttpServletRequest req, HttpSession s, String sessionKey, String paramKey) {
        String v = s != null ? str(s.getAttribute(sessionKey)) : "";
        if (isEmpty(v)) {
            v = str(req.getParameter(paramKey));
        }
        return v;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullToDash(String s) {
        return isEmpty(s) ? "-" : s;
    }

    private static List<String> wrap(String text, int max) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String[] parts = text.replace("\r", "").split("\n");
        for (String p : parts) {
            String t = p.trim();
            while (t.length() > max) {
                int cut = t.lastIndexOf(' ', max);
                if (cut <= 0) {
                    cut = max;
                }
                out.add(t.substring(0, cut));
                t = t.substring(cut).trim();
            }
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\t", "    ");
    }
}
