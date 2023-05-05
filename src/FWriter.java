import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FWriter {

    public static boolean noCR = false;
    FileWriter writer;
    String fileName;

    public FWriter(String filename) {
        File file = new File(filename);
        fileName = filename;
        try {
            if (!file.createNewFile()) new Error().alreadyExists(filename).warning();
            writer = new FileWriter(fileName);
        } catch (SecurityException e) {
            new Error().cannotCreate(fileName).fatal();
        } catch (IOException e) {
            new Error().cannotWrite(fileName).fatal();
        }
    }

    public void write(String text) {
        try {
            writer.write(text.trim());
            if (!noCR) writer.write("\n");
        } catch (IOException e) {
            new Error().cannotWrite(fileName).fatal();
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            new Error().cannotWrite(fileName).fatal();
        }
    }
}
