package saka1029.terrain;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class HeightColor {

	public static final int BASE = 20;
	public static final int MAX = 200;
	public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
	public static final HeightColor HEIGHT_COLOR = new HeightColor()
		.setMin(Color.getHSBColor(240F/360F, 1F, 1F))
		.set(0, Color.getHSBColor(180F/360F, 1F, 1F))
		.set(20, Color.getHSBColor(120F/360F, 1F, 1F))
		.set(40, Color.getHSBColor(60F/360F, 1F, 1F))
		.set(100, Color.getHSBColor(0F/360F, 1F, 1F))
		.setMax(Color.getHSBColor(300F/360F, 1F, 1F))
		.complete();

	private Color[] colors = new Color[MAX];

	public HeightColor set(int height, Color c) {
		colors[height + BASE] = c;
		return this;
	}

	public HeightColor setMin(Color c) {
	    return set(-HeightColor.BASE, c);
	}

	public HeightColor setMax(Color c) {
	    return set(HeightColor.MAX - HeightColor.BASE - 1, c);
	}

	public Color get(int height) {
		int i = height + BASE;
		if (i < 0) return colors[0];
		if (i >= MAX) return colors[MAX - 1];
		return colors[i];
	}

	HeightColor() {
	}

	private int complete(float s, float e, float w, float i) {
		float v = (e - s) * i / w + s;
		return (int) v;
	}

	private void complete(int start, int end) {
		if (end == start + 1) return;
		Color s = colors[start];
		Color e = colors[end];
		int width = end - start;
		for (int i = start + 1; i < end; ++i) {
			int r = complete(s.getRed(), e.getRed(), width, i - start);
			int g = complete(s.getGreen(), e.getGreen(), width, i - start);
			int b = complete(s.getBlue(), e.getBlue(), width, i - start);
			colors[i] = new Color(r, g, b);
		}
	}

	HeightColor complete() {
		if (colors[0] == null) 	throw new IllegalStateException();
		if (colors[MAX - 1] == null) 	throw new IllegalStateException();
		int start = 0;
		while (true) {
			while (start < MAX && colors[start] != null)
				++start;
			if (start >= MAX) break;
			--start;
			int end = start + 1;
			while (end < MAX && colors[end] == null)
				++end;
			complete(start, end);
			start = end;
		}
		return this;
	}

	public BufferedImage chart() {
		int max = HeightColor.MAX;
		int width = 100;
		int offset = 50;
		BufferedImage bi = new BufferedImage(width, max, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		try {
			for (int y = 0, h = MAX - BASE - 1; y < max; ++y, --h) {
				g.setColor(this.get(h));
				g.fillRect(0, y, width, y);
			}
			g.setColor(Color.WHITE);
			for (int y = 0, h = MAX - BASE - 1; y < max; ++y, --h) {
				if (h % 20 == 0) {
					g.drawLine(offset, y, offset + 10, y);
					int yy = y + 5;
					if (yy >= max) yy = y;
					g.drawString(String.format("%5dm", h), offset + 10, yy);
				}
			}

		} finally {
			g.dispose();
		}
		return bi;
	}

	public BufferedImage graph() {
		int max = HeightColor.MAX;
		int width = 256;
		int[] yy = new int[max];
		int[] xr = new int[max];
		int[] xg = new int[max];
		int[] xb = new int[max];
		for (int i = 0; i < max; ++i) {
			int h = i - BASE;
			Color c = this.get(h);
			xr[i] = c.getRed();
			xg[i] = c.getGreen();
			xb[i] = c.getBlue();
		}
		for (int i = 0; i < max; ++i)
			yy[i] = i;
		BufferedImage bi = new BufferedImage(width, max, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, max);
			g.setColor(Color.RED);
			g.drawPolyline(xr, yy, max);
			g.setColor(Color.GREEN);
			g.drawPolyline(xg, yy, max);
			g.setColor(Color.BLUE);
			g.drawPolyline(xb, yy, max);
		} finally {
			g.dispose();
		}
		return bi;
	}

	public static void write(BufferedImage img, File file) throws IOException {
		ImageIO.write(img, "png", file);
	}

	public static void main(String[] args) throws IOException {
		HeightColor hc = HEIGHT_COLOR;
		File base = new File("D:/JPGIS/height5m");
		HeightColor.write(hc.chart(), new File(base, "tokyo-height/image/HeightColor.png"));
		HeightColor.write(hc.graph(), new File(base, "tokyo-height/image/HeightGraph.png"));
	}

}
