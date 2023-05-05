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
            new Error().fileNotFound(filename).fatal();
        } catch (IOException e) {
            new Error().cannotRead(filename).fatal();
        }
        return list;
    }

    public static void createDirectory(String dirname) {
        try {
            boolean dirCreated = new File(dirname).mkdirs();
            if(!dirCreated) new Error().folderAlreadyExists(dirname).warning();
        } catch (SecurityException e) {
            new Error().cannotCreateFolder(dirname).fatal();
        }
    }

}
