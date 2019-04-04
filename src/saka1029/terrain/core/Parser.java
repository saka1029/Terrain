package saka1029.terrain.core;

import static saka1029.terrain.core.Log.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Parser {

    private static Logger logger = Logger.getLogger(Parser.class.getName());

    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    static {
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        factory.setValidating(false);
    }

    static class HeightData {
        int divY;
        int divX;
        double minLon;
        double minLat;
        double maxLon;
        double maxLat;
        int startX;
        int startY;
        String tuple;
    }

    File file;
    BinaryStorage storage;
    int z;

    void write(int x, int y, double lon, double lat, int type, int height) throws IOException {
        long p = GoogleMaps.p(lon, z);
        long q = GoogleMaps.q(lat, z);
        int t = TypeHeight.type(storage.get(p, q));
        if (t == TypeHeight.EMPTY || t == TypeHeight.データなし)
            storage.put(p, q, TypeHeight.typeHeight(type, height));
    }

    void write(HeightData data) throws IOException {
        if (data.tuple == null)
            return;
        String[] ths = data.tuple.trim().split("[\\r\\n ]+");
        double unitLat = (data.maxLat - data.minLat) / data.divY;
        double unitLon = (data.maxLon - data.minLon) / data.divX;
        int i = 0;
        int startX = data.startX;
        int startY = data.startY;
        L: for (int y = startY; y <= data.divY; ++y) {
            double lat = data.maxLat - unitLat * y;
            for (int x = startX; x <= data.divX; ++x) {
                if (i >= ths.length)
                    break L;
                double lon = data.minLon + unitLon * x;
                String[] t = ths[i++].split(",");
                int type = TypeHeight.valueOf(t[0]);
                int height = (int) Math.round(Double.parseDouble(t[1]));
                write(x, y, lon, lat, type, height);
            }
            startX = 0;
        }
    }

    void parse(Node node, HeightData ie) throws IOException {
        String name = node.getNodeName();
        String[] t;
        switch (name) {
        case "gml:lowerCorner":
            t = node.getTextContent().trim().split(" ");
            ie.minLat = Double.parseDouble(t[0]);
            ie.minLon = Double.parseDouble(t[1]);
            break;
        case "gml:upperCorner":
            t = node.getTextContent().trim().split(" ");
            ie.maxLat = Double.parseDouble(t[0]);
            ie.maxLon = Double.parseDouble(t[1]);
            break;
        case "gml:high":
            t = node.getTextContent().trim().split(" ");
            ie.divX = Integer.parseInt(t[0]);
            ie.divY = Integer.parseInt(t[1]);
            break;
        case "gml:startPoint":
            t = node.getTextContent().trim().split(" ");
            ie.startX = Integer.parseInt(t[0]);
            ie.startY = Integer.parseInt(t[1]);
            break;
        case "gml:tupleList":
            ie.tuple = node.getTextContent();
            break;
        }
        if (!node.hasChildNodes())
            return;
        for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling())
            parse(c, ie);
    }

    void parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        HeightData data = new HeightData();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(is);
        parse(document.getDocumentElement(), data);
        write(data);
    }

    public void parse(File inDir, File outDir, int z, boolean clear)
        throws ZipException, IOException, SAXException, ParserConfigurationException {
        Util.makeDir(outDir, clear);
        this.z = z;
        try (BinaryStorage storage = new BinaryStorage(outDir, 100)) {
            this.storage = storage;
            File[] files = inDir.listFiles();
            int size = files.length;
            Arrays.sort(files);
            int count = 0;
            for (File file : files) {
                info(logger, "count=%d/%d file=%s", ++count, size, file.getName());
                if (!file.isFile())
                    continue;
                String name = file.getName().toLowerCase();
                if (name.endsWith(".zip"))
                    try (ZipFile zip = new ZipFile(file)) {
                        for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
                            ZipEntry e = en.nextElement();
                            info(logger, "  xml=%s", e.getName());
                            try (InputStream is = zip.getInputStream(e)) {
                                parse(is);
                            }
                        }
                    }
                else if (name.endsWith(".xml"))
                    try (InputStream is = new FileInputStream(file)) {
                        parse(is);
                    }
            }
        }
    }
}
