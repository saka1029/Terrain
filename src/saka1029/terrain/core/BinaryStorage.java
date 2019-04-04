package saka1029.terrain.core;

import static saka1029.terrain.core.Log.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class BinaryStorage implements Closeable {

    static final Logger logger = Logger.getLogger(BinaryStorage.class.getName());

    static final int IMAGE_SIZE = GoogleMaps.IMAGE_SIZE;
    static final int ENTRY_BYTES = 2;
    static final int AREA_SIZE = ENTRY_BYTES * IMAGE_SIZE * IMAGE_SIZE;

    static class Area {

        static final StandardOpenOption[] READ_MODE = {
            StandardOpenOption.READ,
        };

        static final StandardOpenOption[] WRITE_MODE = {
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
        };

        final File file;
        final ByteBuffer buffer;
        final ShortBuffer points;
        boolean dirty = false;

        Area(File file, Set<File> all) throws FileNotFoundException, IOException {
            this.file = file;
            this.buffer = ByteBuffer.allocateDirect(AREA_SIZE);
            this.points = buffer.asShortBuffer();
            if (!all.contains(file)) return;
            fine(logger, "Storage: read %s", file);
            try (FileChannel channel = FileChannel.open(file.toPath(), READ_MODE)) {
                buffer.clear();
                while (buffer.position() < buffer.limit())
                    channel.read(buffer);
            }
        }

        int get(long xx, long yy) {
            return points.get((int)(xx % IMAGE_SIZE * IMAGE_SIZE + yy % IMAGE_SIZE));
        }

        void put(long xx, long yy, int v) {
            points.put((int)(xx % IMAGE_SIZE * IMAGE_SIZE + yy % IMAGE_SIZE), (short)v);
            dirty = true;
        }

        void write(Set<File> all) throws FileNotFoundException, IOException {
            if (!dirty) return;
            fine(logger, "Storage: write %s", file);
            try (FileChannel channel = FileChannel.open(file.toPath(), WRITE_MODE)) {
                buffer.clear();
                channel.write(buffer);
            }
            all.add(file);
            dirty = false;
        }
    }

    private final File directory;
    private final Set<File> all;
    private final LinkedHashMap<Long, Area> map;

    public BinaryStorage(File directory, int cacheSize) {
        this.directory = directory;
        this.all = new HashSet<>(Arrays.asList(directory.listFiles()));
        this.map = new LinkedHashMap<Long, Area>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Area> eldest) {
                if (size() <= cacheSize) return false;
                try {
                    eldest.getValue().write(all);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        };
    }

    private File file(long xa, long ya) {
        return new File(directory, String.format("%d-%d-15.bin", xa, ya));
    }

    private Area area(long xx, long yy, boolean create) throws IOException {
        long xa = xx / IMAGE_SIZE;
        long ya = yy / IMAGE_SIZE;
        long key = xa << 32 ^ ya;
        Area value = map.get(key);
        if (value == null) {
            File file = file(xa, ya);
            if (create || all.contains(file))
                map.put(key, value = new Area(file, all));
        }
        return value;
    }

    public int get(long xx, long yy) throws IOException {
        Area area = area(xx, yy, false);
        return area == null ? 0 : area.get(xx, yy);
    }

    public void get(long xx, long yy, short[][] data) throws IOException {
        if (xx % IMAGE_SIZE != 0)
            throw new IllegalArgumentException("xx");
        if (xx % IMAGE_SIZE != 0)
            throw new IllegalArgumentException("yy");
        if (data.length != IMAGE_SIZE || data[0].length != IMAGE_SIZE)
            throw new IllegalArgumentException("data");
        Area area = area(xx, yy, false);
        if (area == null)
            throw new IndexOutOfBoundsException("xx, yy");
        for (int x = 0; x < IMAGE_SIZE; ++x)
            for (int y = 0; y < IMAGE_SIZE; ++y)
                data[x][y] = (short)area.get(xx + x, yy + y);
    }

    public void put(long xx, long yy, int point) throws IOException {
        area(xx, yy, true).put(xx, yy, point);
    }

    @Override
    public void close() throws IOException {
        for (Area a : map.values())
            a.write(all);
    }

}
