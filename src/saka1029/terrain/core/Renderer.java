package saka1029.terrain.core;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class Renderer {

    private static final Logger logger = Logger.getLogger(Renderer.class.getName());

    private static final int IMAGE_SIZE = GoogleMaps.IMAGE_SIZE;
    private static final String IMAGE_EXT = "png";

    // 色の選択
//     private static final HeightColor HEIGHT_COLOR = HeightColor.ORIGIN;

    static final double CELL_SIZE = 5; // メッシュサイズ(5m)
    static final double K = CELL_SIZE * Math.sqrt(2F) / 2F; // 傾斜時の水平距離

    // 陰影比率（0から1）大きいほど影が暗くなる。
    static final double SHADE_RATE = 1.0D; // 明るさの増減比率
    // static final double SHADE_RATE = 0.5D; // 明るさの増減比率

    // 補間時の参照セル範囲（対象セル±INTERPOLATE_RANGEの範囲で計算）
    static final int INTERPOLATE_RANGE = 2;

    // 傾斜度計算時の参照セル範囲（対象セル±SHADING_RANGEの範囲で計算）
    static final int SHADING_RANGE = 2;

    BinaryStorage storage;

    static int maxType(int[] typeCount) {
        int max = Integer.MIN_VALUE;
        int maxi = -1;
        for (int i = 0, size = typeCount.length; i < size; ++i)
            if (typeCount[i] > max) {
                max = typeCount[i];
                maxi = i;
            }
        return maxi;
    }

    double[][] weight;
    {
        weight = new double[INTERPOLATE_RANGE * 2 + 1][INTERPOLATE_RANGE * 2 + 1];
        for (int i = -INTERPOLATE_RANGE ; i <= INTERPOLATE_RANGE ; ++i)
            for (int j = -INTERPOLATE_RANGE ; j <= INTERPOLATE_RANGE ; ++j)
                if (i != 0 && j != 0)
                  weight[i + INTERPOLATE_RANGE][j + INTERPOLATE_RANGE] = 1D / Math.sqrt(i * i + j * j);
//                    weight[i + range][j + range] = 1D / (i * i + j * j);
    }
    double[] heightWeight = new double[TypeHeight.TYPE_SIZE];
    double[] heightSum = new double[TypeHeight.TYPE_SIZE];

    void interpolate(int xx, int yy, int x, int y, short[][] area) throws IOException {
        Arrays.fill(heightWeight, 0);
        Arrays.fill(heightSum, 0);
        for (int i = -INTERPOLATE_RANGE; i <= INTERPOLATE_RANGE; ++i)
            for (int j = -INTERPOLATE_RANGE; j <= INTERPOLATE_RANGE; ++j) {
                if (i == 0 && j == 0)
                    continue;
                int xi = x + i;
                int yj = y + j;
                int typeHeight = xi >= 0 && xi < IMAGE_SIZE && yj >= 0 && yj < IMAGE_SIZE
                    ? (int) area[xi][yj]
                    : storage.get(xx + xi, yy + yj);
                int type = TypeHeight.type(typeHeight);
                int height = TypeHeight.height(typeHeight);
                double w = weight[i + INTERPOLATE_RANGE][j + INTERPOLATE_RANGE];
                heightWeight[type] += w;
                heightSum[type] += w * height;
            }
//        if (x ==31)
//            info(logger, "x=%d y=%d heightWeight=%s", x, y, Arrays.toString(heightWeight));
        int maxType = 0;
        double maxWeight = heightWeight[0];
        for (int i = 1; i < TypeHeight.TYPE_SIZE; ++i)
            if (heightWeight[i] > maxWeight) {
                maxType = i;
                maxWeight = heightWeight[i];
            }
        area[x][y] = (short) TypeHeight.typeHeight(
            maxType,
            (int) Math.round(heightSum[maxType] / heightWeight[maxType]));
    }

    void interpolate(int xx, int yy, short[][] area, int range) throws IOException {
        for (int x = 0; x < IMAGE_SIZE; ++x)
            for (int y = 0; y < IMAGE_SIZE; ++y) {
                int typeHeight = (int) area[x][y];
                if (TypeHeight.type(typeHeight) == TypeHeight.EMPTY)
                    interpolate(xx, yy, x, y, area);
            }
    }

    double slope(int xx, int yy, int x, int y, short[][] area) throws IOException {
        double center = TypeHeight.height((int) area[x][y]);
        double height = 0;
        double distance = 0;
        for (int i = -SHADING_RANGE; i <= SHADING_RANGE; ++i)
            for (int j = -SHADING_RANGE; j <= SHADING_RANGE; ++j) {
                int ij = i + j;
                if (ij == 0)
                    continue;
                int xi = x + i;
                int yj = y + j;
                int typeHeight = xi >= 0 && xi < IMAGE_SIZE && yj >= 0 && yj < IMAGE_SIZE ? (int) area[xi][yj]
                    : storage.get(xx + xi, yy + yj);
                int t = TypeHeight.type(typeHeight);
                int h = TypeHeight.height(typeHeight);
                switch (t) {
                case TypeHeight.地表面:
                case TypeHeight.表層面:
                case TypeHeight.その他:
                    height += ij < 0 ? center - h : h - center;
                    distance += Math.abs(ij) * K;
                    break;
                }
            }
        return Math.atan(height / distance);
    }

    static final double SIN45 = Math.sin(Math.PI / 4.0);

    void render(int xx, int yy, short[][] area, Graphics g) throws IOException {
        for (int x = 0; x < IMAGE_SIZE; ++x)
            for (int y = 0; y < IMAGE_SIZE; ++y) {
                int heightType = area[x][y];
                int type = TypeHeight.type(heightType);
                int height = TypeHeight.height(heightType);
                switch (type) {
//                case TypeHeight.EMPTY:
//                case TypeHeight.データなし:
//                case TypeHeight.海水面:
//                case TypeHeight.内水面:
//                    break;
                case TypeHeight.地表面:
                case TypeHeight.表層面:
                case TypeHeight.その他:
//                    double shading = slope(xx, yy, x, y, area) / (Math.PI / 2D) * SHADE_RATE + 1.0D;
                    double slope = slope(xx, yy, x, y, area);
                    double shading = Math.max(0.0, Math.sin(slope * 2D + SIN45));
                     Color org = HeightColor.HEIGHT_COLOR.get(height);
                    Color c = HeightColor.HEIGHT_COLOR.get(height);
                    float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                    Color cc = Color.getHSBColor(hsv[0], hsv[1], (float)(hsv[2] * shading));
                    g.setColor(cc);
                    g.fillRect(x, y, 1, 1);
                    break;
                }
            }
    }

    void render(int xx, int yy, File outFile) throws IOException {
        short[][] area = new short[IMAGE_SIZE][IMAGE_SIZE];
        storage.get(xx, yy, area);
        interpolate(xx, yy, area, INTERPOLATE_RANGE);
        interpolate(xx, yy, area, INTERPOLATE_RANGE);
        BufferedImage img = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(HeightColor.TRANSPARENT_COLOR);
        g.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);
        render(xx, yy, area, g);
        ImageIO.write(img, IMAGE_EXT, outFile);
        logger.info(String.format("file=%s", outFile));
    }

    void render(File outDir, String file) throws IOException {
            String[] f = file.split("-");
            if (f.length < 2) return;
            int xx = Integer.parseInt(f[0]) * IMAGE_SIZE;
            int yy = Integer.parseInt(f[1]) * IMAGE_SIZE;
            File outFile = new File(outDir, Util.changeExt(file, IMAGE_EXT));
            render(xx, yy, outFile);
    }

    public void render(File inDir, File outDir, String[] files) throws IOException {
        try (BinaryStorage storage = new BinaryStorage(inDir, 100)) {
            this.storage = storage;
            for (String file : files)
                render(outDir, file);
        }
    }

    public void render(File inDir, File outDir, boolean clear) throws IOException {
        Util.makeDir(outDir, clear);
        try (BinaryStorage storage = new BinaryStorage(inDir, 100)) {
            this.storage = storage;
            FileFilter filter = Util.filter("bin");
            for (File file : inDir.listFiles(filter))
                render(outDir, file.getName());
        }
    }
}
