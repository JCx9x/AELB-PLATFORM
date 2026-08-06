package es.aelb.backendweb.infrastructure.pdf;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Genera un PDF A4 ligero con dos columnas y fuentes estándar, sin dependencias externas. */
public final class ChampionshipRegistrationsPdfGenerator {
    private static final float[] COLUMNS = {36, 307};
    private static final float BOTTOM = 44;

    public byte[] generate(String championshipName, LocalDate eventDate, List<Entry> entries) {
        Map<String, List<Entry>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        entries.forEach(entry -> grouped.computeIfAbsent(entry.categoryName(), ignored -> new ArrayList<>()).add(entry));
        grouped.values().forEach(list -> list.sort(Comparator.comparing(Entry::lastName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Entry::firstName, String.CASE_INSENSITIVE_ORDER)));

        List<Page> pages = new ArrayList<>();
        Cursor cursor = new Cursor(pages, championshipName, eventDate);
        for (Map.Entry<String, List<Entry>> group : grouped.entrySet()) {
            cursor.ensure(18);
            cursor.text(9, true, group.getKey());
            cursor.down(14);
            for (Entry entry : group.getValue()) {
                cursor.ensure(12);
                cursor.text(8, false, "· " + entry.lastName() + ", " + entry.firstName() + " · DNI: " + entry.dni());
                cursor.down(11);
            }
            cursor.down(6);
        }
        if (entries.isEmpty()) {
            cursor.text(10, false, "No hay inscripciones registradas.");
        }
        for (int i = 0; i < pages.size(); i++) pages.get(i).text(535, 24, 8, false, "Página " + (i + 1) + " de " + pages.size());
        return new PdfWriter(pages).write();
    }

    public record Entry(String categoryName, String firstName, String lastName, String dni) {}

    private static final class Cursor {
        private final List<Page> pages; private final String championshipName; private final LocalDate eventDate;
        private Page page; private int column; private float y;
        Cursor(List<Page> pages, String championshipName, LocalDate eventDate) { this.pages = pages; this.championshipName = championshipName; this.eventDate = eventDate; nextPage(); }
        void ensure(float height) { if (y - height < BOTTOM) nextColumn(); }
        void down(float amount) { y -= amount; }
        void text(float size, boolean bold, String value) { page.text(COLUMNS[column], y, size, bold, value); }
        void nextColumn() { if (column == 0) { column = 1; y = 760; } else nextPage(); }
        void nextPage() { column = 0; y = 760; page = new Page(); pages.add(page); page.text(36, 806, 14, true, "AELB · Listado de inscritos"); page.text(36, 788, 10, false, championshipName); page.text(36, 774, 8, false, "Fecha del campeonato: " + eventDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); page.line(36, 768, 559, 768); }
    }

    private static final class Page {
        private final StringBuilder content = new StringBuilder();
        void text(float x, float y, float size, boolean bold, String value) { content.append("BT /").append(bold ? "F2" : "F1").append(' ').append(size).append(" Tf ").append(x).append(' ').append(y).append(" Td (").append(escape(value)).append(") Tj ET\n"); }
        void line(float x1, float y1, float x2, float y2) { content.append("0.75 w ").append(x1).append(' ').append(y1).append(" m ").append(x2).append(' ').append(y2).append(" l S\n"); }
        byte[] bytes() { return content.toString().getBytes(StandardCharsets.ISO_8859_1); }
        private static String escape(String value) { return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[^\\x20-\\xFF]", "?"); }
    }

    private static final class PdfWriter {
        private final List<Page> pages; private final ByteArrayOutputStream out = new ByteArrayOutputStream(); private final List<Integer> offsets = new ArrayList<>();
        PdfWriter(List<Page> pages) { this.pages = pages; offsets.add(0); }
        byte[] write() {
            raw("%PDF-1.4\n%âãÏÓ\n");
            object(1, "<< /Type /Catalog /Pages 2 0 R >>");
            StringBuilder kids = new StringBuilder("<< /Type /Pages /Kids ["); for (int i = 0; i < pages.size(); i++) kids.append(5 + i * 2).append(" 0 R "); kids.append("] /Count ").append(pages.size()).append(" >>"); object(2, kids.toString());
            // Sin /Encoding explícito los lectores usan la codificación interna de la fuente
            // (StandardEncoding), que no coincide con Latin-1 en el rango acentuado — tildes y
            // "ñ" saldrían mal. WinAnsiEncoding sí coincide con los bytes ISO-8859-1 que escribimos.
            object(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"); object(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
            for (int i = 0; i < pages.size(); i++) { int pageObject = 5 + i * 2; int contentObject = pageObject + 1; object(pageObject, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObject + " 0 R >>"); stream(contentObject, pages.get(i).bytes()); }
            int xref = out.size(); raw("xref\n0 " + (offsets.size()) + "\n0000000000 65535 f \n"); for (int i = 1; i < offsets.size(); i++) raw(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i))); raw("trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"); return out.toByteArray();
        }
        void object(int number, String body) { offsets.add(out.size()); raw(number + " 0 obj\n" + body + "\nendobj\n"); }
        void stream(int number, byte[] bytes) { offsets.add(out.size()); raw(number + " 0 obj\n<< /Length " + bytes.length + " >>\nstream\n"); out.writeBytes(bytes); raw("\nendstream\nendobj\n"); }
        void raw(String value) { out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1)); }
    }
}
