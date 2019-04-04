package saka1029.terrain.test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import saka1029.terrain.core.Parser;
import saka1029.terrain.core.Renderer;
import saka1029.terrain.core.Zoomer;

class Tokyo {

    static Logger logger = Logger.getLogger(Tokyo.class.getName());
    static File BASE = new File("D:/JPGIS/height5m/");
    static File REGION = new File(BASE, "tokyo");
    static int ZOOM_BASE = 15;
    static File IN = new File(REGION, "GML");
    static File BIN = new File(REGION, "BIN");
    static File OUT = new File(BASE, "tokyo-height/image/theme1");
    static String str(int z) { return String.valueOf(z); }
    static File OUT_ZOOM_BASE = new File(OUT, str(ZOOM_BASE));

    @Test
    void run() throws ZipException, IOException, SAXException, ParserConfigurationException {
        new Parser().parse(IN, BIN, ZOOM_BASE, true);
        new Renderer().render(BIN, OUT_ZOOM_BASE, true);
		for (int z = 16; z <= 16; ++z)
			new Zoomer().zoom(OUT_ZOOM_BASE, new File(OUT, str(z)), z, true);
		for (int z = 14; z >= 10; --z)
			new Zoomer().zoom(OUT_ZOOM_BASE, new File(OUT, str(z)), z, true);
    }

}
