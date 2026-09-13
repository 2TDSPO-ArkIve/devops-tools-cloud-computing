package br.com.fiap.arkive.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

final class PdfBrandHeader {
	private static final Color BLUE = new Color(0x3F51B5);

	private PdfBrandHeader() {
	}

	static PDImageXObject carregarLogo(PDDocument document) throws IOException {
		try (var input = new ClassPathResource("static/images/favicon.png").getInputStream()) {
			BufferedImage source = ImageIO.read(input);
			if (source == null || !source.getColorModel().hasAlpha()) {
				throw new IOException("Logo transparente indisponivel.");
			}
			int left = source.getWidth(), top = source.getHeight(), right = -1, bottom = -1;
			// The shared asset is a white symbol on transparency. Preserve its alpha mask, not its color.
			for (int y = 0; y < source.getHeight(); y++) {
				for (int x = 0; x < source.getWidth(); x++) {
					if ((source.getRGB(x, y) >>> 24) != 0) {
						left = Math.min(left, x); right = Math.max(right, x);
						top = Math.min(top, y); bottom = Math.max(bottom, y);
					}
				}
			}
			if (right < left) throw new IOException("Logo vazio.");
			BufferedImage blue = new BufferedImage(right - left + 1, bottom - top + 1, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < blue.getHeight(); y++) {
				for (int x = 0; x < blue.getWidth(); x++) {
					blue.setRGB(x, y, (source.getRGB(x + left, y + top) & 0xFF000000) | (BLUE.getRGB() & 0xFFFFFF));
				}
			}
			return LosslessFactory.createFromImage(document, blue);
		}
	}

	static void render(PDPageContentStream content, PDImageXObject logo, PDFont font,
			float x, float top) throws IOException {
		float height = 28;
		float width = height * logo.getWidth() / logo.getHeight();
		content.drawImage(logo, x, top - height, width, height);
		float fontSize = height * 1000 / font.getFontDescriptor().getCapHeight();
		content.beginText();
		content.setFont(font, fontSize);
		content.setNonStrokingColor(BLUE);
		content.newLineAtOffset(x + width + 12, top - height);
		content.showText("ArkIve");
		content.endText();
	}
}
