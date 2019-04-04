package saka1029.terrain.core;

import static saka1029.terrain.core.Log.*;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class Zoomer {

	private static final Logger logger = Logger.getLogger(Zoomer.class.getName());
	private static final String EXT = "png";
	private static final int MAX = 256;

	private File inDir;
	private File outDir;
	/** ターゲットのズームレベル */
	private int z;

	private String name(int x, int y, int z) {
		return String.format("%d-%d-%d.%s", x, y, z, EXT);
	}

	/**
	 * java.awt.ImageをBufferedImageに変換します。
	 */
	private static BufferedImage convert(Image image) {
		BufferedImage bimg = new BufferedImage(
			image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics g = bimg.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimg;
	}

	private void zoomOut(String name, int x, int y, int z0, int r) throws IOException {
		int size = MAX * r;
		BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		try {
			g.setColor(HeightColor.TRANSPARENT_COLOR);
			g.drawRect(0, 0, size, size);
			for (int i = 0; i < r; ++i)
				for (int j = 0; j < r; ++j) {
					String div = name(x * r + i, y * r + j, z0);
					File df = new File(inDir, div);
					if (!df.exists()) continue;
					BufferedImage im = ImageIO.read(df);
					g.drawImage(im, i * MAX, j * MAX, null);
				}
			ImageFilter filter = new AreaAveragingScaleFilter(MAX, MAX);
			ImageProducer p = new FilteredImageSource(bi.getSource(), filter);
			Image dstImage = Toolkit.getDefaultToolkit().createImage(p);
			BufferedImage newImage = convert(dstImage);
			info(logger, "zoomOut: %s", name);
			ImageIO.write(newImage, EXT, new File(outDir, name));
		} finally {
			g.dispose();
		}
	}

	private void zoomIn(File file, int x, int y, int r) throws IOException {
		int ssize = MAX / r;	// 元のイメージから切り出すサイズ
		BufferedImage iimg = ImageIO.read(file);
		for (int i = 0; i < r; ++i)
			for (int j = 0; j < r; ++j) {
				BufferedImage oimg = new BufferedImage(MAX, MAX, BufferedImage.TYPE_INT_ARGB);
				Graphics g = oimg.getGraphics();
				try {
					g.drawImage(iimg,
						0, 0, MAX, MAX,
						i * ssize, j * ssize, (i + 1) * ssize, (j + 1) * ssize, null);
				} finally {
					g.dispose();
				}
				String name = name(x + i, y + j, z);
                info(logger, "zoomIn: %s", name);
				ImageIO.write(oimg, EXT, new File(outDir, name));
			}
	}

	private void convert(File file) throws IOException {
		String org = file.getName();
		String[] f = org.split("[-\\.]");
		int x0 = Integer.parseInt(f[0]);
		int y0 = Integer.parseInt(f[1]);
		int z0 = Integer.parseInt(f[2]);
		if (z == z0)
			return;
		else if (z < z0) {
			int r = 1 << (z0 - z);
			int x = x0 / r;
			int y = y0 / r;
			String name = name(x, y, z);
			if (new File(outDir, name).exists()) return;
//			info(logger, name);
			zoomOut(name, x, y, z0, r);
		} else {
			int r = 1 << (z - z0);
			int x = x0 * r;
			int y = y0 * r;
			zoomIn(file, x, y, r);
		}
	}

	/**
	 *
	 * @param inDir 入力ファイルがあるディレクトリ
	 * @param outDir 出力ディレクトリ
	 * @param z ターゲットのズームレベル
	 * @throws IOException
	 */
	public void zoom(File inDir, File outDir, int z, boolean clear) throws IOException {
		if (outDir.equals(inDir)) return;
		Util.makeDir(outDir, clear);
		this.inDir = inDir;
		this.outDir = outDir;
		this.z = z;
		FileFilter filter = Util.filter(EXT);
		for (File e : inDir.listFiles(filter))
			convert(e);
	}
}
