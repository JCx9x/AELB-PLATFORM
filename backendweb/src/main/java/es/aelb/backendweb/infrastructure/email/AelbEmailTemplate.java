package es.aelb.backendweb.infrastructure.email;

import es.aelb.backendweb.application.email.EmailMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantilla corporativa de la AELB para todos los correos transaccionales.
 * El layout HTML vive en resources/email/aelb-email-layout.html y replica los colores y la tipografía de la web.
 */
public final class AelbEmailTemplate {
    static final String SUBJECT_PREFIX = "AELB · ";
    static final String AUTOMATIC_NOTICE = "Este es un correo generado automáticamente por la Asociación Española de Lucha de Brazos (AELB). Por favor, no respondas a este mensaje: esta dirección no está atendida.";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{[a-zA-Z]+}}");
    private static final String LAYOUT = load("/email/aelb-email-layout.html");

    public record Detail(String label, String value) {}
    public record Content(String subject, String eyebrow, String title, String name, List<String> paragraphs, List<Detail> details, String ctaLabel, String ctaUrl, String note) {}

    private final String siteUrl;

    public AelbEmailTemplate(String siteUrl) { this.siteUrl = siteUrl == null ? "" : siteUrl.replaceAll("/+$", ""); }

    public EmailMessage render(String recipient, Content c) {
        String subject = SUBJECT_PREFIX + c.subject();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("{{subject}}", esc(subject));
        values.put("{{preheader}}", esc(c.paragraphs().isEmpty() ? c.title() : c.paragraphs().get(0)));
        values.put("{{eyebrow}}", esc(c.eyebrow()));
        values.put("{{title}}", esc(c.title()));
        values.put("{{name}}", esc(c.name()));
        values.put("{{siteUrl}}", esc(siteUrl));
        values.put("{{siteLabel}}", esc(siteUrl.replaceFirst("^https?://", "")));
        values.put("{{year}}", String.valueOf(Year.now().getValue()));
        values.put("{{content}}", contentHtml(c));
        // Sustitución en una sola pasada: un valor nunca se reinterpreta como marcador.
        String html = PLACEHOLDER.matcher(LAYOUT).replaceAll(m -> Matcher.quoteReplacement(values.getOrDefault(m.group(), "")));
        return new EmailMessage(recipient, subject, plainText(c), html);
    }

    private String contentHtml(Content c) {
        StringBuilder b = new StringBuilder();
        for (String p : c.paragraphs()) b.append("<p style=\"margin:0 0 16px 0;\">").append(esc(p)).append("</p>");
        if (!c.details().isEmpty()) {
            b.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:8px 0 20px 0;border:1px solid #e4beba;border-radius:4px;\">");
            for (int i = 0; i < c.details().size(); i++) {
                Detail d = c.details().get(i);
                String border = i == 0 ? "" : "border-top:1px solid #e4beba;";
                b.append("<tr><td style=\"padding:12px 16px;").append(border).append("font-size:10px;line-height:14px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#8f6f6c;width:35%;\">")
                 .append(esc(d.label())).append("</td><td style=\"padding:12px 16px;").append(border).append("font-size:15px;line-height:20px;font-weight:700;color:#1a1c1c;\">")
                 .append(esc(d.value())).append("</td></tr>");
            }
            b.append("</table>");
        }
        if (c.ctaUrl() != null) {
            String url = esc(c.ctaUrl());
            b.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:8px 0 24px 0;\"><tr><td bgcolor=\"#d32f2f\" style=\"background-color:#d32f2f;border-radius:4px;\">")
             .append("<a href=\"").append(url).append("\" target=\"_blank\" style=\"display:inline-block;padding:14px 28px;font-family:'Lexend',Helvetica,Arial,sans-serif;font-size:13px;line-height:16px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#ffffff;text-decoration:none;border-radius:4px;\">")
             .append(esc(c.ctaLabel())).append("</a></td></tr></table>")
             .append("<p style=\"margin:0 0 16px 0;font-size:12px;line-height:18px;color:#5b403d;\">Si el botón no funciona, copia y pega este enlace en tu navegador:<br>")
             .append("<a href=\"").append(url).append("\" style=\"color:#d32f2f;word-break:break-all;\">").append(url).append("</a></p>");
        }
        if (c.note() != null) b.append("<p style=\"margin:0 0 16px 0;font-size:13px;line-height:20px;color:#5b403d;\">").append(esc(c.note())).append("</p>");
        return b.toString();
    }

    private String plainText(Content c) {
        StringBuilder b = new StringBuilder("ASOCIACIÓN ESPAÑOLA DE LUCHA DE BRAZOS (AELB)\n")
                .append(c.title()).append("\n\nHola ").append(c.name()).append(",\n\n");
        for (String p : c.paragraphs()) b.append(p).append("\n\n");
        for (Detail d : c.details()) b.append(d.label()).append(": ").append(d.value()).append('\n');
        if (!c.details().isEmpty()) b.append('\n');
        if (c.ctaUrl() != null) b.append(c.ctaLabel()).append(": ").append(c.ctaUrl()).append("\n\n");
        if (c.note() != null) b.append(c.note()).append("\n\n");
        return b.append("--\n").append(AUTOMATIC_NOTICE).append("\n\n© ").append(Year.now().getValue())
                .append(" Asociación Española de Lucha de Brazos · ").append(siteUrl).append('\n').toString();
    }

    static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    private static String load(String path) {
        try (InputStream in = AelbEmailTemplate.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("No se encuentra la plantilla de email " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
