package co.technius.signchestshop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class StringConfig
{

    private final File f;
    private PrintWriter pw;
    private final HashMap<String, String> map = new HashMap<String, String>();

    public StringConfig(final File file) {
        f = file;
    }

    public StringConfig(final String file) {
        f = new File(file);
    }

    public void close() throws IOException {
        if (pw == null)
            return;
        pw.flush();
        pw.close();
        pw = null;
    }

    public boolean getBoolean(final String k, final boolean def) {
        if (!map.containsKey(k))
            return def;
        return Boolean.parseBoolean(map.get(k));
    }

    public double getDouble(final String k, final double def) {
        if (!map.containsKey(k))
            return def;
        try {
            return Double.parseDouble(map.get(k));
        } catch (final Exception e) {
            return def;
        }
    }

    public double[] getDoubleList(final String k, final double[] def) {
        if (!map.containsKey(k))
            return def;
        final String[] s = map.get(k).split(",");
        final double[] a = new double[s.length];
        for (int i = 0; i < s.length; i++) {
            final String b = s[i];
            try {
                a[i] = Double.parseDouble(b.trim());
            } catch (final NumberFormatException nfe) {
                return def;
            }
        }
        return a;
    }

    public File getFile() {
        return f;
    }

    public int getInt(final String k, final int def) {
        if (!map.containsKey(k))
            return def;
        try {
            return Integer.parseInt(map.get(k));
        } catch (final Exception e) {
            return def;
        }
    }

    public int[] getIntList(final String k, final int[] def) {
        if (!map.containsKey(k))
            return def;
        final String[] s = map.get(k).split(",");
        final int[] a = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            final String b = s[i];
            try {
                a[i] = Integer.parseInt(b.trim());
            } catch (final NumberFormatException nfe) {
                return def;
            }
        }
        return a;
    }

    public String getString(final String k, final String def) {
        if (!map.containsKey(k))
            return def;
        return map.get(k);
    }

    public String[] getStringList(final String k, final String[] def) {
        if (!map.containsKey(k))
            return def;
        return map.get(k).split(",");
    }

    public void insertComment(final String s) {
        if (pw == null)
            return;
        pw.println("#" + s);
    }

    public void load() throws FileNotFoundException, IOException {
        final BufferedReader br = new BufferedReader(new FileReader(f));
        String l;
        map.clear();
        while ((l = br.readLine()) != null) {
            if (l.isEmpty() || l.startsWith("#")) {
                continue;
            }
            final String[] t = l.split(":", 2);
            if (t.length != 2) {
                continue;
            }
            map.put(t[0], t[1].trim());
        }
        br.close();
    }

    public void set(final String k, final String v) {
        map.put(k, v);
    }

    public void start() throws IOException {
        if (pw != null)
            return;
        if (f == null) {
            final FileOutputStream fos = new FileOutputStream(f);
            fos.flush();
            fos.close();
        }
        pw = new PrintWriter(f);
    }

    public void write(final String s) {
        if (pw == null)
            return;
        pw.println(s);
    }

    public void write(final String k, final String v) {
        if (pw == null)
            return;
        pw.println(k + ": " + v);
    }

    public void writeKey(final String k, final boolean def) {
        writeKey(k, Boolean.toString(def));
    }

    public void writeKey(final String k, final int def) {
        writeKey(k, Integer.toString(def));
    }

    public void writeKey(final String k, final String def) {
        if (pw == null)
            return;
        if (!map.containsKey(k)) {
            pw.println(k + ": " + def);
        } else {
            pw.println(k + ": " + map.get(k));
        }
    }

    public void writeLine() {
        if (pw == null)
            return;
        pw.println();
    }
}
