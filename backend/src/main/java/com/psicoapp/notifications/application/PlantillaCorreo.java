package com.psicoapp.notifications.application;

import com.psicoapp.notifications.domain.ContenidoCorreo;
import java.util.List;
import org.springframework.web.util.HtmlUtils;

final class PlantillaCorreo {
    record Dato(String etiqueta, String valor) { }

    private PlantillaCorreo() { }

    static ContenidoCorreo preparar(String titulo, String introduccion, List<Dato> datos, String nota, String enlace) {
        return preparar(titulo, introduccion, "Datos de la sesión", datos, nota, enlace,
                "Ver actividad en Psicoapp", "Ingresa con tu correo institucional y tu contraseña.");
    }

    static ContenidoCorreo preparar(String titulo, String introduccion, String tituloDatos, List<Dato> datos,
            String nota, String enlace, String accion, String ayuda) {
        var texto = new StringBuilder(titulo).append("\n\n").append(introduccion).append('\n');
        if (!datos.isEmpty()) texto.append('\n').append(tituloDatos).append('\n');
        var filas = new StringBuilder();
        for (Dato dato : datos) {
            texto.append(dato.etiqueta()).append(": ").append(dato.valor()).append('\n');
            filas.append("""
                    <tr>
                      <th scope="row" style="padding:9px 14px;text-align:left;vertical-align:top;color:#405962;font-size:14px;font-weight:normal;">%s</th>
                      <td style="padding:9px 14px;vertical-align:top;color:#14374d;font-size:15px;font-weight:bold;overflow-wrap:anywhere;">%s</td>
                    </tr>
                    """.formatted(escapar(dato.etiqueta()), escapar(dato.valor())));
        }
        String panelDatos = datos.isEmpty() ? "" : """
                <h2 style="margin:0 0 8px;color:#14374d;font-size:17px;">%s</h2>
                <table width="100%%" cellpadding="0" cellspacing="0" bgcolor="#edf3ee" style="border-radius:8px;">%s</table>
                """.formatted(escapar(tituloDatos), filas);
        String accesoHtml = "";
        if (enlace != null) {
            texto.append('\n').append(accion).append('\n').append(enlace)
                    .append("\n\n").append(ayuda).append('\n');
            accesoHtml = """
                    <table role="presentation" cellpadding="0" cellspacing="0" style="margin:24px 0 12px;">
                      <tr><td bgcolor="#14374d" style="border-radius:8px;">
                        <a href="%s" style="display:inline-block;padding:13px 20px;color:#ffffff;font-size:15px;font-weight:bold;text-decoration:none;">%s</a>
                      </td></tr>
                    </table>
                    <p style="margin:0;color:#405962;font-size:14px;line-height:1.5;">%s</p>
                    """.formatted(escapar(enlace), escapar(accion), escapar(ayuda));
        }
        String notaHtml = "";
        if (nota != null && !nota.isBlank()) {
            texto.append("\nMensaje del docente\n").append(nota).append('\n');
            notaHtml = """
                    <h2 style="margin:24px 0 8px;color:#14374d;font-size:17px;">Mensaje del docente</h2>
                    <p style="margin:0;color:#405962;font-size:15px;line-height:1.6;overflow-wrap:anywhere;">%s</p>
                    """.formatted(escapar(nota).replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<br>"));
        }
        String html = """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                </head>
                <body style="margin:0;background:#f3f6f4;font-family:Arial,Helvetica,sans-serif;color:#14374d;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#f3f6f4">
                    <tr><td align="center" style="padding:24px 12px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;background:#ffffff;border:1px solid #dce6e0;border-radius:12px;">
                        <tr><td bgcolor="#14374d" style="padding:16px 22px;color:#ffffff;font-size:20px;font-weight:bold;border-radius:11px 11px 0 0;">Psicoapp</td></tr>
                        <tr><td style="padding:24px 22px;">
                          <h1 style="margin:0 0 12px;color:#14374d;font-size:26px;line-height:1.2;">%s</h1>
                          <p style="margin:0 0 20px;color:#405962;font-size:15px;line-height:1.6;">%s</p>
                          %s
                          %s
                          %s
                        </td></tr>
                        <tr><td style="padding:13px 22px;border-top:1px solid #dce6e0;color:#405962;font-size:12px;">Mensaje automático de Psicoapp</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapar(titulo), escapar(titulo), escapar(introduccion), panelDatos, accesoHtml, notaHtml);
        return new ContenidoCorreo(texto.append("\nMensaje automático de Psicoapp\n").toString(), html);
    }

    private static String escapar(String contenido) {
        return HtmlUtils.htmlEscape(contenido, "UTF-8");
    }
}
