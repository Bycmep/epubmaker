import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class EpubMaker {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Must specify a directory to read.");
            System.exit(-1);
        }
//        String currentDir = System.getProperty("user.dir");
        String inputDir = args[0];
        String inputFile = args[0] + "\\main.xhtml";
        String[] outputDir = new String[3];
        outputDir[0] = args[0] + "_out";
        outputDir[1] = args[0] + "_out\\META-INF";
        outputDir[2] = args[0] + "_out\\OEBPS";

        List<String> input = readFileToList(inputFile);
        List<Integer> marker = new ArrayList<>();
        for (int i = 0; i < input.size(); i++)
            if (input.get(i).startsWith("<!--"))
                marker.add(i);

        for (int i = 0; i < 3; i++) createDirectory(outputDir[i]);

        FWriter mimetype = new FWriter(outputDir[0] + "\\mimetype");
        mimetype.write("application/epub+zip");
        mimetype.close();

        FWriter applebook = new FWriter(outputDir[1] + "\\com.apple.ibooks.display-options.xml");
        applebook.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        applebook.write("<display_options>");
        applebook.write("<platform name=\"*\">");
        applebook.write("<option name=\"specified-fonts\">true</option>");
        applebook.write("</platform>");
        applebook.write("</display_options>");
        applebook.close();

        FWriter container = new FWriter(outputDir[1] + "\\container.xml");
        container.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        container.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">");
        container.write("<rootfiles>");
        container.write("<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\" />");
        container.write("</rootfiles>");
        container.write("</container>");
        container.close();

        FWriter content = new FWriter(outputDir[2] + "\\content.opf");
        content.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        content.write("<package version=\"3.0\" xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"bookid\" prefix=\"ibooks: http://vocabulary.itunes.apple.com/rdf/ibooks/vocabulary-extensions-1.0/\">");
        content.write("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        content.write("<meta content=\"cover-image\" name=\"0001\"/>");

        int meta = -1;
        if (marker.size() > 1)
            if (marker.get(0) == 0)
                do {
                    meta++;
                } while (meta < marker.size() - 1 && marker.get(meta + 1) == marker.get(meta) + 1);

        String bookTitle = "", bookAuthor = "", bookID = "";
        for (int m = 0; m <= meta; m++) {
            String s = readMarker(input.get(marker.get(m)));
            String attribute = s.substring(0, s.indexOf(":")).trim();
            String value = s.substring(s.indexOf(":") + 1).trim();
            if (s.isEmpty()) System.out.println("Warning: line " + (m + 1) + ": meta attribute not found");
            else {
                if (attribute.equalsIgnoreCase("title")) {
                    content.write("<dc:title>" + value + "</dc:title>");
                    bookTitle = value;
                } else if (attribute.equalsIgnoreCase("author")) {
                    content.write("<dc:creator>" + value + "</dc:creator>");
                    bookAuthor = value;
                } else if (attribute.equalsIgnoreCase("language")) {
                    content.write("<dc:language>" + value + "</dc:language>");
                } else if (attribute.equalsIgnoreCase("translator")) {
                    content.write("<dc:contributor opf-role=\"trl\">" + value + "</dc:contributor>");
                } else if (attribute.equalsIgnoreCase("illustrator")) {
                    content.write("<dc:contributor opf-role=\"ill\">" + value + "</dc:contributor>");
                } else if (attribute.equalsIgnoreCase("afterword")) {
                    content.write("<dc:contributor opf-role=\"aft\">" + value + "</dc:contributor>");
                } else if (attribute.equalsIgnoreCase("publisher")) {
                    content.write("<dc:publisher>" + value + "</dc:publisher>");
                } else if (attribute.equalsIgnoreCase("bookid")) {
                    content.write("<dc:identifier id=\"bookid\">" + value + "</dc:identifier>");
                    bookID = value;
                } else if (attribute.equalsIgnoreCase("description")) {
                    content.write("<dc:description>" + value + "</dc:description>");
                } else {
                    System.out.println("Warning: line " + (m + 1) + ": meta attribute " + attribute + " not recognized");
                }
            }
        }
        if (bookTitle.isEmpty() || bookAuthor.isEmpty() || bookID.isEmpty()) {
           System.out.println("The book must have a title, an author and an ID. Aborting.");
           System.exit(-6);
        }
        SimpleDateFormat zformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        zformat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = zformat.format(new Date());
        content.write("<meta property=\"dcterms:modified\">" + time + "</meta>");
        content.write("</metadata>");

        content.write("<manifest>");
        content.write("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>");
        File[] manifest;
        File inputDirFile = new File(inputDir);
        manifest = inputDirFile.listFiles();
        if (manifest != null)
            for (File file : manifest) {
                String filename = file.getName();
                if (!file.isDirectory() && !filename.equalsIgnoreCase("main.xhtml")) {
                    boolean mediaRecognized = true;
                    String mediatype = "";
                    switch (extension(filename)) {
                        case "jpg" -> mediatype = "image/jpeg";
                        case "png" -> mediatype = "image/png";
                        case "otf", "ttf" -> mediatype = "application/vnd.ms-opentype";
                        case "css" -> mediatype = "text/css";
                        default -> mediaRecognized = false;
                    }
                    if (mediaRecognized) {
                        content.write("<item id=\"" + filename + "\" href=\"" + filename + "\" media-type=\"" + mediatype + "\"/>");
                        String targetFile = outputDir[2] + "\\" + filename;
                        try {
                            Files.copy(file.toPath(), new File(targetFile).toPath(),
                                    new StandardCopyOption[]{REPLACE_EXISTING});
                        } catch (IOException e) {
                            System.out.println("Cannot write file: " + targetFile);
                            System.exit(-5);
                        }
                    }
                    else
                        System.out.println("Warning: Media type not recognized: " + filename);

                }

            }
        for (int i = 1; i < marker.size() - meta - 1; i++) {
            String fName = String.format("%04d", i);
            content.write("<item id=\"" + fName + "\" href=\"" + fName + ".xhtml\" media-type=\"application/xhtml+xml\"/>");
        }
        content.write("</manifest>");
        content.write("<spine toc=\"ncx\">");
        for (int i = 1; i < marker.size() - meta - 1; i++) {
            String fName = String.format("%04d", i);
            content.write("<itemref idref=\"" + fName + "\"/>");
        }
        content.write("</spine>");
        content.write("<guide>");
        content.write("<reference type=\"cover\" href=\"0001.xhtml\" title=\"Cover\"/>");
        content.write("</guide>");
        content.write("</package>");
        content.close();

        FWriter toc = new FWriter(outputDir[2] + "\\toc.ncx");
        toc.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        toc.write("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">");
        toc.write("<head>");
        toc.write("<meta name=\"dtb:uid\" content=\"" + bookID + "\"/>");
        toc.write("<meta name=\"dtb:depth\" content=\"1\"/>");
        toc.write("<meta name=\"dtb:totalPageCount\" content=\"0\"/>");
        toc.write("<meta name=\"dtb:maxPageNumber\" content=\"0\"/>");
        toc.write("</head>");
        toc.write("<docTitle><text>" + bookTitle + "</text></docTitle>");
        toc.write("<docAuthor><text>" + bookAuthor + "</text></docAuthor>");
        toc.write("<navMap>");
        int playOrder = 1;
        for (int p = 1; p < marker.size() - meta - 1; p++) {
            String chapTitle = readMarker(input.get(marker.get(meta + p)));
            if (!chapTitle.isEmpty()) {
                toc.write("<navPoint id=\"toc" + playOrder +
                        "\" playOrder=\"" + playOrder +
                        "\"><navLabel><text>" + chapTitle +
                        "</text></navLabel><content src=\"" + String.format("%04d", p) + ".xhtml\"/></navPoint>");
                playOrder++;
            }
        }
        toc.write("</navMap></ncx>");
        toc.close();

        for (int p = 1; p < marker.size() - meta - 1; p++) {
            FWriter xhtml = new FWriter(String.format("%s\\%04d.xhtml", outputDir[2], p));

            for (int i = marker.get(meta) + 1; i < marker.get(meta + 1); i++ )
                xhtml.write(input.get(i));
            for (int i = marker.get(meta + p) + 1; i < marker.get(meta + p + 1); i++ )
                xhtml.write(input.get(i));
            for (int i = marker.get(marker.size() - 1) + 1; i < input.size(); i++ )
                xhtml.write(input.get(i));
            xhtml.close();
        }
    }

    private static List<String> readFileToList(String filename) {
        List<String> list = new ArrayList<>();

        try {
            list = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            System.out.println("File not found: " + filename);
            System.exit(-2);
        } catch (IOException e) {
            System.out.println("IO error: " + filename);
            System.exit(-5);
        }
        return list;
    }

    private static void createDirectory(String dirname) {
        try {
            boolean dirCreated = new File(dirname).mkdirs();
            if(!dirCreated) System.out.println("Warning: directory already exists: " + dirname);
        } catch (SecurityException e) {
            System.out.println("Directory could not be created: " + dirname);
            System.exit(-3);
        }
    }

    public static String readMarker(String s) {
        return s.substring(4, s.indexOf("-->"));
    }

    public static String extension(String s) {
        return s.substring(s.lastIndexOf('.') + 1).toLowerCase();
    }

}
