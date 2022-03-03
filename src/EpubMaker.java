import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class EpubMaker {

    static class Note {
        public int start;
        public int end;
        public String title;
        public boolean isUsed = false;
    }

    static class ToAdd {
        public String label;
        public String backRef;
    }

    static class Notes {
        public List<String> input;
        public Map<String, Note> markers;
        public List<ToAdd> queue;
        public int top, bottom;

        public Notes() {
            input = new ArrayList<>();
            markers = new HashMap<>();
            queue = new ArrayList<>();
            top = bottom = -1;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Must specify a directory to read.");
            System.exit(-1);
        }
//        String currentDir = System.getProperty("user.dir");
        String inputDir = args[0];
        String inputFilename = args[0] + "\\main.html";
        String notesFilename = args[0] + "\\notes.html";
        String[] outputDir = new String[3];
        outputDir[0] = args[0] + "_out";
        outputDir[1] = args[0] + "_out\\META-INF";
        outputDir[2] = args[0] + "_out\\OEBPS";

        List<String> input = readFileToList(inputFilename);
        List<Integer> marker = new ArrayList<>();
        for (int i = 0; i < input.size(); i++)
            if (input.get(i).startsWith("<!--"))
                marker.add(i);

        // read notes.html and process it, if not done already
        Notes notes = new Notes();
        File notesHtml = new File(notesFilename);
        if (notesHtml.exists()) {
            notes.input = readFileToList(notesFilename);
            int previousNote = -1;
            for (int j = 0; j < notes.input.size(); j++) {
                if (notes.input.get(j).startsWith("<!--")) {
                    if (previousNote != -1) {
                        notes.bottom = j;
                        String s = readMarker(notes.input.get(previousNote), true, previousNote + 1);
                        String label = getAttribute(s);
                        Note note = new Note();
                        note.start = previousNote; note.end = j; note.title = getValue(s);
                        if (label.isEmpty() || note.title.isEmpty()) {
                            System.out.println("Note fatal: line " +
                                    (previousNote + 1) + ": note label or title is empty.");
                            System.exit(-7);
                        }
                        if (notes.markers.containsKey(label)) {
                            System.out.println("Note fatal: line " +
                                    (j + 1) + ": note label is not unique.");
                            System.exit(-7);
                        }
                        notes.markers.put(label, note);
                    } else {
                        notes.top = j;
                    }
                    previousNote = j;
                }
            }
        }



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
            String s = readMarker(input.get(marker.get(m)), false, marker.get(m) + 1);
            String attribute = getAttribute(s);
            String value = getValue(s);
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
                if (!file.isDirectory() && !extension(filename).equalsIgnoreCase("html")) {
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
            content.write("<item id=\"" + fName + "\" href=\"" + fName + ".html\" media-type=\"application/xhtml+xml\"/>");
        }

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
            String chapTitle = readMarker(input.get(marker.get(meta + p)), false, meta + p + 1);
            if (!chapTitle.isEmpty()) {
                toc.write("<navPoint id=\"toc" + playOrder +
                        "\" playOrder=\"" + playOrder +
                        "\"><navLabel><text>" + chapTitle +
                        "</text></navLabel><content src=\"" + String.format("%04d", p) + ".html\"/></navPoint>");
                playOrder++;
            }
        }
        toc.write("</navMap></ncx>");
        toc.close();

        for (int p = 1; p < marker.size() - meta - 1; p++) {
            String id = String.format("%04d", p);
            String htmlFilename = id + ".html";
            String noteFilename = "x" + htmlFilename;
            FWriter htmlFile = new FWriter(outputDir[2] + "\\" + htmlFilename);

            // html file head
            for (int i = marker.get(meta) + 1; i < marker.get(meta + 1); i++ )
                htmlFile.write(input.get(i));
            // html file body
            for (int i = marker.get(meta + p) + 1; i < marker.get(meta + p + 1); i++ ) {
                String text = input.get(i);
                // handle notes, if any
                if (text.contains("<note ")) {
                    htmlFile.write(addNotesLinks(text, "", i + 1, htmlFilename, noteFilename, notes));
                }
                else htmlFile.write(text);
            }
            for (int i = marker.get(marker.size() - 1) + 1; i < input.size(); i++ )
                htmlFile.write(input.get(i));
            htmlFile.close();
            writeNotesQueue(notes, outputDir[2], id, content);
        }

        content.write("</manifest>");
        content.write("<spine toc=\"ncx\">");
        for (int i = 1; i < marker.size() - meta - 1; i++) {
            String fName = String.format("%04d", i);
            content.write("<itemref idref=\"" + fName + "\"/>");
        }
        content.write("</spine>");
        content.write("<guide>");
        content.write("<reference type=\"cover\" href=\"0001.html\" title=\"Cover\"/>");
        content.write("</guide>");
        content.write("</package>");
        content.close();

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

    public static String readMarker(String s, boolean notes, int line) {
        int i = s.indexOf("-->");
        if (i == -1) {
            String fatal = notes ? "Notes fatal" : "Fatal";
            System.out.println(fatal + ": line " + line + ": invalid marker.");
            System.exit(-8);
        }
        return s.substring(4, s.indexOf("-->"));
    }

    public static String extension(String s) {
        return s.substring(s.lastIndexOf('.') + 1).toLowerCase();
    }

    public static String getAttribute(String s) {
        int i = s.indexOf(":");
        if (i == -1) return "";
        return s.substring(0, i).trim();
    }

    public static String getValue(String s) {
        int i = s.indexOf(":");
        if (i == -1) return "";
        return s.substring(i + 1).trim();
    }

    public static void fatal(String location, int line, String error) {
        System.out.println("Fatal: " + location + " line " + line + ": " + error + ".");
        System.exit(-1);
    }

    private static String addNotesLinks(String text, String location, int line,
                                        String htmlFilename, String noteFilename, Notes notes) {
        int pos = 0;
        int notePos;
        String output = "";
        while ((notePos = text.indexOf("<note ", pos)) != -1) {
            int noteNumEnd = text.indexOf('>', notePos + 6);
            if (noteNumEnd == -1) fatal(location, line, "error in note tag");
            String noteLabel = text.substring(notePos + 6, noteNumEnd).trim();
            if (noteLabel.isEmpty())
                fatal(location, line, "note label not found");
            Note note = notes.markers.get(noteLabel);
            if (note == null)
                fatal(location, line, "no note with such label");
            else {
                if (note.isUsed)
                    fatal(location, line, "note was already referenced");
                note.isUsed = true;
                notes.markers.replace(noteLabel, note);
            }
            int noteEnd = text.indexOf("</note>", noteNumEnd + 1);
            if (noteEnd == -1)
                fatal(location, line, "note tag is not closed");
            String noteLink = text.substring(noteNumEnd + 1, noteEnd);
            if (noteLink.isEmpty())
                fatal(location, line, "note link is empty");
            output += text.substring(pos, notePos) +
                    "<a href=\"" + noteFilename + "#n" + noteLabel + "\" id=\"r" +
                    noteLabel + "\" epub:type=\"noteref\">" + noteLink + "</a>";
            pos = noteEnd + 7;

            // queue writing the note to notes file
            ToAdd noteQ = new ToAdd();
            noteQ.backRef = htmlFilename;
            noteQ.label = noteLabel;
            notes.queue.add(noteQ);
        }
        output += text.substring(pos);
        return output;
    }

    private static void writeNotesQueue(Notes notes, String dir, String id, FWriter manifest) {
        if (notes.queue.isEmpty()) return;

        FWriter noteFile = new FWriter(dir + "\\x" + id + ".html");
        for (int j = 0; j < notes.top; j++)
            noteFile.write(notes.input.get(j));
        manifest.write("<item id=\"x" + id + "\" href=\"x" + id +
                    ".html\" media-type=\"application/xhtml+xml\"/>");
        while (!notes.queue.isEmpty()) {
            ToAdd n = notes.queue.get(0);
            notes.queue.remove(0);
            Note note = notes.markers.get(n.label);
            noteFile.write("<div class=\"note\" id=\"n" + n.label + "\" epub:type=\"footnote\">");
            noteFile.write("<p class=\"link\"><a href=\"" + n.backRef + "#r" + n.label +
                    "\">" + note.title + "</a></p>");
            for (int j = note.start + 1; j < note.end; j++) {
                String text = notes.input.get(j);
                // handle notes, if any
                if (text.contains("<note ")) {
                    noteFile.write(addNotesLinks(text, "notes ", j + 1, "", "", notes));
                }
                else noteFile.write(text);
            }
            noteFile.write("</div>");
        }
        for (int j = notes.bottom + 1; j < notes.input.size(); j++)
            noteFile.write(notes.input.get(j));
        noteFile.close();
    }

}
