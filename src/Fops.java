import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Fops {

    public static List<String> readFileToList(String filename) {
        List<String> list = new ArrayList<>();
        try {
            list = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            Err.fatal("file " + filename + " not found");
        } catch (IOException e) {
            Err.fatal("error reading file " + filename);
        }
        return list;
    }

    public static void createDirectory(String dirname) {
        try {
            boolean dirCreated = new File(dirname).mkdirs();
            if(!dirCreated) Err.warning("directory " + dirname + " already exists");
        } catch (SecurityException e) {
            Err.fatal("directory " + dirname + " could not be created");
        }
    }

}
