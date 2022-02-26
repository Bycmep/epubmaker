import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class epubmaker {


    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new Exception("Must specify a file to read.");
//        String currentDir = System.getProperty("user.dir");
//        System.out.println("Hello world! " + currentDir);

        List<String> input = readFileInList(args[0]);
//        System.out.println(input.get(0));

        List<Integer> marker = new ArrayList<>();
        for (int i = 0; i < input.size(); i++)
            if (input.get(i).startsWith("<!--"))
                marker.add(i);

        File toc = new File("toc.ncx");
        if (!toc.createNewFile()) System.out.println("toc.ncx: file already exists.");

        for (int p = 1; p < marker.size(); p++) {
            String fName = String.format("%04d.html", p);
            try {
                File part = new File(fName);
                if (!part.createNewFile()) System.out.println(fName + ": file already exists.");
                FileWriter partWriter = new FileWriter(fName);

                for (int i = 0; i < marker.get(0); i++ )
                    partWriter.write(input.get(i));
                for (int i = marker.get(p - 1) + 1; i < marker.get(p); i++ )
                    partWriter.write(input.get(i));
                for (int i = marker.get(marker.size() - 1) + 1; i < input.size(); i++ )
                    partWriter.write(input.get(i));
                partWriter.close();
                System.out.println(fName);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }



    }

    public static List<String> readFileInList(String fileName) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static String readMarker(String s) {
        return s.substring(4, s.indexOf("-->"));
    }

}
/*
        List l = readFileInList("C:\\Users\\pankaj\\Desktop\\test.java");

        Iterator<String> itr = l.iterator();
        while (itr.hasNext())
            System.out.println(itr.next());
*/