package saka1029.gridscapes.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * 東京地形図(http://www.gridscapes.net/)
 * のKMLおよびイメージファイルを一括ダウンロードするプログラムです。
 *
 */
public class Main {

	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	static {
		factory.setIgnoringComments(true);
		factory.setNamespaceAware(true);
		factory.setValidating(false);
	}
	private static TransformerFactory outputFactory = TransformerFactory.newInstance();

	String base = "http://www.gridscapes.net/GI/TokyoTerrain/";
	String root = base + "index.kml";
	String out = "D:/gridscapes/";

	LinkedList<URL> que = new LinkedList<URL>();
	Set<URL> done = new HashSet<URL>();

	private URL removeRef(URL url) throws MalformedURLException {
		return new URL(url.toExternalForm().replaceAll("#.*", ""));
	}

	private String relativeURL(String src, String dst) {
		while (true) {
			// srcの先頭にある"/"を検索します。
			int pos = src.indexOf('/');
			// なければdstをそのまま返します。
			if (pos < 0) return dst;
			// srcの先頭の"/"までを削除します。
			src = src.substring(pos + 1);
			// dstの先頭に"../"を追加します。
			dst = "../" + dst;
		}
	}

	/**
	 * 元のURLから先のURLへの相対パスを求めます。
	 *
	 * 【処理例】
	 * src = http://host/a/b/c/d.kml
	 * dst = http://host/a/p/q/r.kml
	 * -> 共通部分の削除
	 * b/c/d.kml
	 * p/q/r.kml
	 * -> 相対化(1)
	 * c/d.kml
	 * ../p/q/r.kml
	 * -> 相対化(2)
	 * d.kml
	 * ../../p/q/r.kml
	 */
	private String relativeURL(URL src, URL dst) {
		String ss = src.toExternalForm();
		String ds = dst.toExternalForm();
		// 共通部分を求めます。
		int c;
		int min = Math.min(ss.length(), ds.length());
		for (c = min; c >= 0; --c)
			if (ss.substring(0, c).equals(ds.substring(0, c))) {
				// 共通部分以降を取り出します。
				ss = ss.substring(c);
				ds = ds.substring(c);
				break;
			}
		// 共通部分がなければ絶対パスを返します。
		if (c <= "http://".length()) return dst.toExternalForm();
		return relativeURL(ss, ds);
	}

	private String normalize(String file) {
		return file
			.replace(' ', '-')
			.replace('?', '-')
			.replace('&', '-')
			.replace('#', '-')
			.replace(':', '-');
	}

	/**
	 * URLから出力先ファイルを求めます。
	 * 他サイトの場合はnullを返します。
	 */
	private File outFile(URL url) {
		String u = url.toExternalForm();
		if (u.startsWith(base))
			u = u.substring(base.length());
		else
			return null;
		return new File(out, normalize(u));
	}

	private void parse(Node node, URL myUrl) throws DOMException, IOException {
		String name = node.getNodeName().toLowerCase();
		if (name.equals("href") || name.equals("icon") || name.indexOf("url") >= 0) {
			String s = node.getTextContent().trim();
			if (s.startsWith("http://")) {
				URL url = new URL(s);
				// 指定したURLが処理済でなくて自サイトの場合のみキューにURLを追加します。
				if (!done.contains(url) && outFile(url) != null) {
					done.add(url);
					que.add(url);
				}
				// 相対URLを求めます。
				String rel = relativeURL(myUrl, url);
				node.setTextContent(rel);
			}
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
			parse(child, myUrl);
	}

	private void saveKml(InputStream is, URL myUrl, File of)
			throws IOException, ParserConfigurationException, SAXException, TransformerException {
		// DOMの読込み
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(is);
		// DOMの解析
		parse(document.getDocumentElement(), myUrl);
		// すでにファイルがあれば書込みしません。
		if (of.exists()) return;
		// DOMの書込み
	    Transformer transformer = outputFactory.newTransformer();
	    transformer.transform(new DOMSource(document), new StreamResult(of));
	}

	private void saveStream(InputStream is, File of) throws IOException {
		OutputStream os = new FileOutputStream(of);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int size = is.read(buffer);
				if (size < 0) break;
				os.write(buffer, 0, size);
			}
		} finally {
			os.close();
		}
	}

	private boolean isKml(File file) {
		return file.getName().toLowerCase().endsWith(".kml");
	}

	private void run(URL url)
			throws IOException, ParserConfigurationException, SAXException, TransformerException {
		url = removeRef(url);
		File of = outFile(url);
		// 他のサイトの場合は何もしません。
		if (of == null) return;
		System.out.println("" + que.size() + " " + url + " -> " + of);
		// すでにファイルが存在してKML以外の場合は何もしません。
		if (of.exists() && !isKml(of)) return;
		// 出力先のディレクトリが存在しない場合は作成します。
		File parent = of.getParentFile();
		if (!parent.exists()) parent.mkdirs();
		InputStream is = url.openStream();
		try {
			if (isKml(of))
				saveKml(is, url, of);
			else
				saveStream(is, of);
		} finally {
			is.close();
		}
	}

	public void run()
			throws IOException, ParserConfigurationException, SAXException, TransformerException {
		File dir = new File(out);
		if (!dir.exists()) dir.mkdirs();
		run(new URL(root));
		while (que.size() > 0)
			run(que.remove());
	}

	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.run();
	}
}
