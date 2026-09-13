package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.PrescricaoResumoResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class PdfRenderer implements AutoCloseable {
	private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
	private static final float MARGIN = 54;
	private static final float FOOTER_Y = 36;
	private static final float CONTENT_BOTTOM = 72;
	private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - (MARGIN * 2);
	private static final Color PRIMARY = new Color(0x3F51B5);
	private static final Color TEXT = new Color(0x1F2937);
	private static final Color MUTED = new Color(0x6B7280);
	private static final Color LIGHT = new Color(0xEE, 0xF0, 0xFB);
	private static final Color SEPARATOR = new Color(0xDD, 0xE2, 0xEA);

	private final PDDocument document;
	private final PDImageXObject logo;
	private final String title;
	private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

	private PDPageContentStream content;
	private float y;
	private int pageNumber;

	PdfRenderer(PDDocument document, String title) throws IOException {
		this.document = document;
		this.logo = PdfBrandHeader.carregarLogo(document);
		this.title = title;
		addPage();
	}

	private void addPage() throws IOException {
		if (content != null) {
			content.close();
		}
		PDPage page = new PDPage(PAGE_SIZE);
		document.addPage(page);
		content = new PDPageContentStream(document, page);
		pageNumber++;
		y = PAGE_SIZE.getHeight() - MARGIN;
		header();
		footer();
	}

	private void header() throws IOException {
		PdfBrandHeader.render(content, logo, bold, MARGIN, y);
		text(title, MARGIN, y - 72, bold, 16, TEXT);
		content.setStrokingColor(PRIMARY);
		content.setLineWidth(2);
		content.moveTo(MARGIN, y - 86);
		content.lineTo(PAGE_SIZE.getWidth() - MARGIN, y - 86);
		content.stroke();
		y -= 114;
	}

	private void footer() throws IOException {
		content.setStrokingColor(SEPARATOR);
		content.setLineWidth(0.6f);
		content.moveTo(MARGIN, FOOTER_Y + 18);
		content.lineTo(PAGE_SIZE.getWidth() - MARGIN, FOOTER_Y + 18);
		content.stroke();
		text("Documento gerado pelo ArkIve a partir dos registros do atendimento veterinário.", MARGIN, FOOTER_Y, regular, 8, MUTED);
		String pageText = "Página " + pageNumber;
		float width = textWidth(pageText, regular, 8);
		text(pageText, PAGE_SIZE.getWidth() - MARGIN - width, FOOTER_Y, regular, 8, MUTED);
	}

	void section(String title) throws IOException {
		ensureSpace(48);
		y -= 6;
		content.setNonStrokingColor(LIGHT);
		content.addRect(MARGIN, y - 17, CONTENT_WIDTH, 24);
		content.fill();
		text(title, MARGIN + 12, y - 9, bold, 12, PRIMARY);
		y -= 34;
	}

	void field(String label, String value) throws IOException {
		value = nuloSeVazio(value);
		if (value == null) {
			return;
		}
		float valueX = MARGIN + 154;
		List<String> lines = wrap(value, regular, 10.5f, PAGE_SIZE.getWidth() - MARGIN - valueX);
		ensureSpace(18 + (lines.size() * 14));
		text(label, MARGIN, y, bold, 9, MUTED);
		float lineY = y;
		for (String line : lines) {
			text(line, valueX, lineY, regular, 10.5f, TEXT);
			lineY -= 14;
		}
		y = lineY - 4;
	}

	void paragraph(String label, String value) throws IOException {
		value = nuloSeVazio(value);
		if (value == null) {
			return;
		}
		ensureSpace(44);
		text(label, MARGIN, y, bold, 9, MUTED);
		y -= 16;
		for (String paragraph : value.split("\\R", -1)) {
			List<String> lines = wrap(paragraph, regular, 10.5f, CONTENT_WIDTH);
			if (lines.isEmpty()) {
				y -= 8;
				continue;
			}
			for (String line : lines) {
				ensureSpace(16);
				text(line, MARGIN, y, regular, 10.5f, TEXT);
				y -= 14;
			}
			y -= 4;
		}
		y -= 8;
	}

	void prescriptions(List<PrescricaoResumoResponse> prescricoes) throws IOException {
		if (prescricoes.isEmpty()) return;
		ensureSpace(128);
		section("Medica\u00e7\u00f5es Registradas");
		for (var prescricao : prescricoes) prescription(prescricao);
	}

	private void prescription(PrescricaoResumoResponse prescricao) throws IOException {
		ensureSpace(88);
		content.setStrokingColor(PRIMARY);
		content.setLineWidth(2);
		content.moveTo(MARGIN, y + 5);
		content.lineTo(MARGIN, y - 76);
		content.stroke();
		String medicamento = Objects.requireNonNullElse(nuloSeVazio(prescricao.medicamento()), "Medicação");
		for (String line : wrap(medicamento, bold, 12, CONTENT_WIDTH - 14)) {
			ensureSpace(18);
			text(line, MARGIN + 14, y, bold, 12, TEXT);
			y -= 15;
		}
		y -= 7;
		field("Dosagem", prescricao.dosagem());
		field("Frequência", prescricao.frequencia());
		field("Via de administração", prescricao.viaAdministracao());
		field("Data de início", prescricao.dataInicio() == null ? null : DATA_FORMATTER.format(prescricao.dataInicio()));
		field("Data de término", prescricao.dataFim() == null ? null : DATA_FORMATTER.format(prescricao.dataFim()));
		paragraph("Instruções", prescricao.instrucoes());
		y -= 8;
	}

	private void ensureSpace(float required) throws IOException {
		if (y - required < CONTENT_BOTTOM) {
			addPage();
		}
	}

	private void text(String value, float x, float textY, PDFont font, float fontSize, Color color) throws IOException {
		content.beginText();
		content.setFont(font, fontSize);
		content.setNonStrokingColor(color);
		content.newLineAtOffset(x, textY);
		content.showText(value.replace('\t', ' '));
		content.endText();
	}

	private List<String> wrap(String value, PDFont font, float fontSize, float maxWidth) throws IOException {
		String normalized = value == null ? "" : value.replace('\t', ' ').trim();
		if (normalized.isBlank()) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : normalized.split("\\s+")) {
			if (current.isEmpty()) {
				appendWrappedWord(lines, current, word, font, fontSize, maxWidth);
				continue;
			}
			String candidate = current + " " + word;
			if (textWidth(candidate, font, fontSize) <= maxWidth) {
				current.append(' ').append(word);
			} else {
				lines.add(current.toString());
				current.setLength(0);
				appendWrappedWord(lines, current, word, font, fontSize, maxWidth);
			}
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines;
	}

	private void appendWrappedWord(
			List<String> lines,
			StringBuilder current,
			String word,
			PDFont font,
			float fontSize,
			float maxWidth
	) throws IOException {
		if (textWidth(word, font, fontSize) <= maxWidth) {
			current.append(word);
			return;
		}
		StringBuilder chunk = new StringBuilder();
		for (int i = 0; i < word.length(); i++) {
			String candidate = chunk.toString() + word.charAt(i);
			if (!chunk.isEmpty() && textWidth(candidate, font, fontSize) > maxWidth) {
				lines.add(chunk.toString());
				chunk.setLength(0);
			}
			chunk.append(word.charAt(i));
		}
		current.append(chunk);
	}

	private float textWidth(String value, PDFont font, float fontSize) throws IOException {
		return font.getStringWidth(value) / 1000 * fontSize;
	}

	@Override
	public void close() throws IOException {
		if (content != null) {
			content.close();
			content = null;
		}
	}

	private static String nuloSeVazio(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}
}
