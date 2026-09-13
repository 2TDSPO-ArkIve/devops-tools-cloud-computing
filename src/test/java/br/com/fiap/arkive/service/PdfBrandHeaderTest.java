package br.com.fiap.arkive.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfBrandHeaderTest {
	@Test
	void simboloAzulPreservaTransparenciaSemBlocoDeFundo() throws Exception {
		try (PDDocument document = new PDDocument()) {
			var image = PdfBrandHeader.carregarLogo(document).getImage();
			assertTrue(image.getColorModel().hasAlpha());
			assertEquals(0, image.getRGB(0, 0) >>> 24);
			int transparent = 0, colored = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int pixel = image.getRGB(x, y);
					if ((pixel >>> 24) == 0) transparent++;
					else {
						colored++;
						assertEquals(0x3F51B5, pixel & 0xFFFFFF);
					}
				}
			}
			assertTrue(colored > 0);
			assertTrue(transparent > colored, "Symbol should not be an opaque square");
		}
	}
}
