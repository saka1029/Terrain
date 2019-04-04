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

class TestTerrain {

    static Logger logger = Logger.getLogger(TestTerrain.class.getName());

    static String str(int n) { return String.valueOf(n); }

    @Test
    void test() throws ZipException, IOException, SAXException, ParserConfigurationException {
        File BASE = new File("D:/JPGIS/height5m/");
        int Z = 15;
        File in = new File(BASE, "test/GML");
        File bin = new File(BASE, "test/BIN");
        File out = new File(BASE, "test/OUT");
        new Parser().parse(in, bin, Z, true);
        File outBase = new File(out, str(Z));
        new Renderer().render(bin, outBase, true);
		for (int z = 16; z <= 16; ++z)
			new Zoomer().zoom(outBase, new File(out, str(z)), z, true);
		for (int z = 14; z >= 10; --z)
			new Zoomer().zoom(outBase, new File(out, str(z)), z, true);
    }

}
