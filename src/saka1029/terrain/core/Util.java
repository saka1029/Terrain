package saka1029.terrain.core;

import java.io.File;
import java.io.FileFilter;

public class Util {

	private Util() {
	}

	public static void delete(File file) {
		if (file.isDirectory())
			for (File c : file.listFiles())
				delete(c);
		file.delete();
	}

	public static void makeDir(File dir, boolean clear) {
		if (!dir.exists()) dir.mkdirs();
		if (clear)
            for (File f : dir.listFiles())
                delete(f);
	}

	public static File changeExt(File f, String ext) {
		String name = f.getName().replaceFirst("\\.[^\\.]*$", "." + ext);
		return new File(f.getParentFile(), name);
	}

	public static String changeExt(String name, String ext) {
		return name.replaceFirst("\\.[^\\.]*$", "." + ext);
	}

	public static FileFilter filter(final String ext) {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName().toLowerCase();
				return name.endsWith("." + ext.toLowerCase());
			}
		};
	}
}
