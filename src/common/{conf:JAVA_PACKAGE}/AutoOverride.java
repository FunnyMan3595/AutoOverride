package %conf:JAVA_PACKAGE%;

import cpw.mods.fml.relauncher.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

@IFMLLoadingPlugin.TransformerExclusions("%conf:JAVA_PACKAGE%.")
public class AutoOverride implements IFMLLoadingPlugin, IClassTransformer {
    public static File zip;
    public static String name = "(uninitialized)";
    private Map<String, Data> classes = null;

    public String[] getLibraryRequestClass() {
        return new String[0];
    }

    public String[] getASMTransformerClass() {
        return new String[] {"%conf:JAVA_PACKAGE%.AutoOverride"};
    }

    public String getModContainerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> data) {
        zip = (File) data.get("coremodLocation");
        name = zip.getName();
    }


    public byte[] transform(String name, byte[] bytes) {
        try {
            if (classes == null) {
                if (zip != null) {
                    initClasses();
                } else {
                    return bytes;
                }
            }

            Data data = classes.get(name);
            if (data != null) {
                System.out.println(this.name + ": Replacing class " + name + ".");
                return data.array;
            } else {
                return bytes;
            }
        } catch (Exception e) {
            System.out.println(this.name + ": Unable to transform " + name + ":");
            e.printStackTrace();
            return bytes;
        }
    }

    private void initClasses() {
        classes = new HashMap<String, Data>();

        try {
            ZipFile my_zip = new ZipFile(zip);

            Enumeration<? extends ZipEntry> entries = my_zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String name = entry.getName();
                if (name.endsWith(".class")) {
                    String clsName = name.substring(0, name.length() - 6);

                    int len = (int) entry.getSize();

                    if (len != -1) {
                        byte[] bytes = new byte[len];
                        InputStream input = my_zip.getInputStream(entry);
                        int read = input.read(bytes);

                        while (read > 0 && read < len) {
                            read += input.read(bytes, read, len-read);
                        }

                        if (read != len) {
                            System.out.println("Size mismatch while loading " + name + " from " + zip.getName() + "; skipping this file.");
                            continue;
                        }

                        classes.put(clsName, new Data(bytes));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + name + ": " + e.getMessage());
        }
    }

    private class Data {
        public byte[] array = new byte[0];
        public Data(byte[] array) {
            this.array = array;
        }
    }
}
