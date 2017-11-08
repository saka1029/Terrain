package saka1029.terrain;

/**
 * 
 * Google Static Maps API におけるピクセル座標系と世界測地系（緯度、経度）の
 * 間の変換を行います。
 * 
 * 計算式は以下を使用している。
 * 
 * http://hosohashi.blog59.fc2.com/blog-entry-5.html
 * 
 * 変数およびメソッドの意味は以下の通り。
 * 
 * z
 * ズームレベルのことで、Google Static Maps APIのzoom引数に指定する。
 * ズームレベルを大きくするとより詳細な地図が表示される。
 * 
 * (xc,yc)
 * Google Static Maps APIの引数(center)で指定する経緯度で、
 * この点を中心とした地図が表示される。変換式においては、
 * この点が暗黙的な基準点になる。本記事では、
 * この点が画像座標の原点に変換されるように変換式を求める。
 * 
 * (x,y)
 * マーカーの経緯度。この点（任意）に対して世界測地系・ピクセル座標・画像座標
 * の間の変換式を導く。
 * 
 * (p,q)
 * マーカーのピクセル座標内での位置。世界測地系のマーカー(x,y)は
 * この点に変換（写像）される。
 * 
 * (X,Y)
 * マーカーの画像座標内での位置。この変数は画像座標の位置であるが、
 * 別の見方をすると、画像の中心からの何ピクセル数離れているかを
 * 意味する変位と解釈できることに注意。世界測地系の経緯度(x,y)が
 * 画像座標のこの変位に変換される。
 * 
 * L
 * Google Mapsで表示可能な緯度の最大値（定数）。
 * Lは約85度。Google Maps APIではこの地点より北極側は表示できない。
 * 南極も同様。
 *
 */
public class GoogleMaps {
	
    public static final int IMAGE_SIZE = 256;
	public static final double L = 85.05112877980669;
	public static final double PI = Math.PI;
	
	public static double sindeg(double degree) {
		return Math.sin(degree * PI / 180.0);
	}
	
	public static double asin(double radian) {
		return Math.asin(radian);
	}
	
	public static double tanh(double x) {
		return (Math.exp(x) - Math.exp(-x)) / (Math.exp(x) + Math.exp(-x));
	}
	
	public static double atanh(double x) {
		return Math.log((1.0 + x) / (1.0 - x)) * 0.5;
	}
	
	public static double pow2(long b) {
		return 1 << b;
	}
	
	public static long p(double x, int z) {
		return Math.round(pow2(z + 7) * (x / 180.0 + 1.0));
	}
	
	public static long q(double y, int z) {
		return Math.round((-atanh(sindeg(y)) + atanh(sindeg(L))) * pow2(z + 7) / PI );
	}
	
	public static double x(double p, int z) {
		return 180.0 * (p / pow2(z + 7) - 1.0);
	}
	
	public static double y(double q, int z) {
		return 180.0 / PI * asin(tanh(- PI / pow2(z + 7) * q + atanh(sindeg(L))));
	}
	
	/** 未テスト */
	public static double X(double xc, double x, int z) {
		return pow2(z + 7) * ((x - xc) / 180.0);
	}
	
	/** 未テスト */
	public static double Y(double yc, double y, int z) {
		return pow2(z + 7) / PI * (-atanh(sindeg(yc)) + atanh(sindeg(y)));
	}
	
	/** 未テスト */
	public static double x(double xc, double X, int z) {
		return 180.0 / pow2(z + 7) * X + xc;
	}
	
	/** 未テスト */
	public static double y(double yc, double Y, int z) {
		return 180.0 / PI * asin(tanh(PI / pow2(z + 7) * Y + atanh(sindeg(yc))));
	}
}
