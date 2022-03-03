import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FWriter {
    FileWriter w;
    String filename;

    public FWriter(String fname) {
        File file = new File(fname);
        filename = fname;
        try {
            if (!file.createNewFile()) System.out.println("Warning: file already exists: " + fname);
            w = new FileWriter(fname);
        } catch (SecurityException e) {
            System.out.println("Failed to create file: " + fname);
            System.exit(-4);
        } catch (IOException e) {
            System.out.println("IO error: " + fname);
            System.exit(-5);
        }
    }

    public void write(String s) {
        try {
            w.write(s.trim() + "\n");
        } catch (IOException e) {
            System.out.println("IO error: " + filename);
            System.exit(-5);
        }
    }

    public void close() {
        try {
            w.close();
        } catch (IOException e) {
            System.out.println("IO error: " + filename);
            System.exit(-5);
        }
    }
}
