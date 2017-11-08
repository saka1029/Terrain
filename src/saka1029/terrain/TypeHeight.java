package saka1029.terrain;

import java.util.HashMap;
import java.util.Map;

public class TypeHeight {

    private TypeHeight() {}

    static final int TYPE_SHIFT = 13;

    public static final int TYPE_MASK = 0x7;
    public static final int EMPTY = 0;
    public static final int 地表面 = 1;
    public static final int 表層面 = 2;
    public static final int 海水面 = 3;
    public static final int 内水面 = 4;
    public static final int その他 = 5;
    public static final int データなし = 6;
    public static final int TYPE_SIZE = 7;
    
    static final Map<String, Integer> encode = new HashMap<>();

    static {
        encode.put("地表面", 地表面);
        encode.put("表層面", 表層面);
        encode.put("海水面", 海水面);
        encode.put("内水面", 内水面);
        encode.put("その他", その他);
        encode.put("データなし", データなし);
        encode.put("データ無し", データなし);
    }

    static int valueOf(String name) {
        Integer r = encode.get(name);
        if (r == null)
            throw new IllegalArgumentException("name");
        return r;
    }
    
    public static int type(int typeHeight) {
        return (typeHeight >>> TYPE_SHIFT) & TYPE_MASK;
    }
    
    static final int HEIGHT_MASK = 0x1FFF;
    static final int HEIGHT_SIGN = 0x1000;
    static final int HEIGHT_MINUS = 0xFFFFE000;

    public static int height(int typeHeight) {
        return (typeHeight & HEIGHT_SIGN )!= 0
            ? typeHeight | HEIGHT_MINUS
            : typeHeight & HEIGHT_MASK;
    }
    
    public static int typeHeight(int type, int height) {
        return ((type & TYPE_MASK) << TYPE_SHIFT) | height & HEIGHT_MASK;
    }
}
