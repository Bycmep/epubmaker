import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Book {
    String inDir, outDir, outMetaInf, outOEBPS;
    List<String> text;
    List<Integer> markers;
    Map<String, String> meta;
    List<String> noteText;
    List<Integer> noteMarkers;
    Map<String, Integer> noteLabels;
    List<Note> noteQueue;
    int imgScale = 0;

    static class Note {
        int marker;
        int part;
        boolean local;
        String label;
        String title;
        Note(String label, String title, int marker, int part, boolean local) {
            this.label = label; this.title = title; this.marker = marker; this.part = part; this.local = local;
        }
    }

    static class Img {
        String src;
        String cls;
        String style;
        String alt;
        int part;
        Img(String src, String cls, String style, String alt, int part) {
            this.src = src; this.cls = cls; this.style = style; this.alt = alt; this.part = part;
        }
    }
    List<Img> imgQueue;

    final String htmlFile = "main.xhtml";
    final String noteFile = "notes.xhtml";

    public Book(String directory) {
        inDir = directory;
        outDir = directory + "_out";
        outMetaInf = outDir + "\\META-INF";
        outOEBPS = outDir + "\\OEBPS";

        text = Fops.readFileToList(directory + "\\" + htmlFile);
        markers = new ArrayList<>();
        for (int i = 0; i < text.size(); i++)
            if (text.get(i).startsWith("<!--"))
                markers.add(i);

        noteMarkers = new ArrayList<>();
        noteLabels = new HashMap<>();
        noteQueue = new ArrayList<>();
        imgQueue = new ArrayList<>();
        if (new File(directory + "\\" + noteFile).exists()) {
            noteText = Fops.readFileToList(directory + "\\" + noteFile);
            Error.file = noteFile;
            for (int i = 0; i < noteText.size(); i++) {
                if (noteText.get(i).startsWith("<!--")) {
                    noteMarkers.add(i);
                    Error.line = i;
                    String[] s = parseMarker(noteText.get(i));
                    if (!s[0].isEmpty()) {
                        if (noteLabels.containsKey(s[0]))
                            new Error("note label is not unique").verbose().fatal();
                        noteLabels.put(s[0], noteMarkers.size() - 1);
                    }
                }
            }
        }

        Error.file = htmlFile;
        meta = new HashMap<>();
        String[] keywords = new String[] { "title", "creator", "language", "translator", "illustrator", "afterword",
                "publisher", "bookid", "description", "cover", "series", "number", "imgscale" };
        int mark = 0;
        while (mark < markers.size() - 1 && markers.get(mark) + 1 == markers.get(mark + 1)) mark++;
        for (int i = 0; i <= mark; i++) {
            Error.line = markers.get(i);
            String[] pair = parseMarker(text.get(markers.get(i)));
            if (pair[0].isEmpty()) new Error("meta attribute not found").verbose().warning();
            else {
                String found = "";
                for (String keyword: keywords)
                    if (pair[0].equalsIgnoreCase(keyword)) {
                        found = keyword; break;
                    }
                if (found.isEmpty()) {
                    new Error("meta attribute " + pair[0] + " is not recognized").verbose().warning();
                } else if (meta.containsKey(found)) {
                    new Error("meta attribute " + pair[0] + " is already defined").verbose().warning();
                } else if (pair[1].isEmpty()) {
                    new Error("meta attribute " + pair[0] + " has no value").verbose().warning();
                } else {
                    meta.put(found, pair[1]);
                }
            }
        }
        if (meta("title").isEmpty())
            new Error("a book must have a title").fatal();
        if (meta("creator").isEmpty())
            new Error("a book must have a creator").fatal();
        if (meta("bookid").isEmpty())
            new Error("a book must have an ID").fatal();
        if (!meta("imgscale").isEmpty()) {
            try {
                imgScale = Integer.parseInt(meta("imgscale"));
            } catch (NumberFormatException e) {
                new Error("cannot parse image scale: " + meta("imgscale")).verbose().fatal();
            }
            if (imgScale <= 0)
                new Error("invalid image scale: " + imgScale).verbose().fatal();
        }
        if (mark > 0) markers.subList(0, mark).clear();

        // PARSING BODY TEXT
        for (int part = 1; part < markers.size() - 1; part++) {
            // html file body
            Kepub.paragraph = 1;
            for (int i = markers.get(part) + 1; i < markers.get(part + 1); i++ ) {
                boolean changed = false;
                String txt = text.get(i);
                Error.line = i;
                if (txt.contains("<note ")) {
                    changed = true;
                    txt = addNotesLinks(txt, part, false);
                }
                if (txt.contains("<img ")) {
                    changed = true;
                    txt = addImgLinks(txt, part);
                }
                if (Kepub.on) {
                    changed = true;
                    txt = Kepub.kepubify(txt);
                }
                if (changed) text.set(i, txt);
            }
        }

        // PARSING NOTES TEXT
        int part = -1;
        for (Note n : noteQueue) {
            if (part != n.part) {
                part = n.part;
                Kepub.paragraph = 1;
            }
            for (int j = noteMarkers.get(n.marker); j < noteMarkers.get(n.marker + 1); j++) {
                boolean changed = false;
                String txt = noteText.get(j);
                Error.line = j + 1;
                if (txt.contains("<note ")) {
                    changed = true;
                    txt = addNotesLinks(txt, part, true);
                }
                if (txt.contains("<img ")) {
                    changed = true;
                    txt = addImgLinks(txt, part);
                }
                if (Kepub.on) {
                    changed = true;
                    txt = Kepub.kepubify(txt);
                }
                if (changed) noteText.set(j, txt);
            }
        }
    }

    String addImgLinks(String s, int part) {
        int pos = 0;
        int imgPos;
        StringBuilder output = new StringBuilder();

        while ((imgPos = s.indexOf("<img ", pos)) != -1) {
            int imgEnd = s.indexOf(">", imgPos + 5);
            if (imgEnd == -1) new Error("error in img tag").verbose().fatal();
            Map <String,String> imgProperties = getProperties(s.substring(imgPos + 5, imgEnd - 1));

            if (imgProperties.get("target") != null) {
                String num = String.format("%04d", imgQueue.size());
                output.append(s, pos, imgPos).append("<a href=\"y").append(num).append(".xhtml\" id=\"img").append(num).
                        append("\"><img");
                if (imgProperties.get("src") == null) new Error("img with no src").verbose().fatal();
                else output.append(" src=\"").append(imgProperties.get("src")).append("\"");
                if (imgProperties.get("class") != null)
                    output.append(" class=\"").append(imgProperties.get("class")).append("\"");
                if (imgProperties.get("style") != null)
                    output.append(" style=\"").append(imgProperties.get("style")).append("\"");
                output.append("/></a>");
                Img img = new Img(imgProperties.get("target"),
                        imgProperties.get("target-class"),
                        imgProperties.get("target-style"),
                        imgProperties.get("alt"), part);
                imgQueue.add(img);
            } else if (imgScale > 0 && imgProperties.get("width") != null && imgProperties.get("width").equalsIgnoreCase("auto")) {
                imgProperties.remove("width");
                if (imgProperties.get("src") == null) new Error("img with no src").verbose().fatal();
                String filename = inDir + "\\" + imgProperties.get("src");
                float width = 0;
                try {
                    BufferedImage image = ImageIO.read(new File(filename));
                    width = (float)(image.getWidth());
                } catch (IOException e) {
                    new Error().cannotRead(filename).fatal();
                }
                width = width * 100 / imgScale;
                if (width > 100) width = 100;
                String widthPC = String.format("width:%.2f%%;", width);
                String style = imgProperties.get("style");
                if (style == null) imgProperties.put("style", widthPC);
                else imgProperties.replace("style", widthPC + style);

                output.append(s, pos, imgPos).append("<img");
                imgProperties.forEach( (k, v) -> output.append(" ").append(k).append("=\"").append(v).append("\"") );
                output.append("/>");
            } else {
                output.append(s, pos, imgEnd + 1);
            }

            pos = imgEnd + 1;
        }
        output.append(s.substring(pos));
        return output.toString();
    }

    Map<String, String> getProperties(String s) {
        Map<String, String> properties = new HashMap<>();
        char[] s2 = s.toCharArray();
        char[] s3 = new char[s2.length + 1];

        boolean quoted = false;
        char q = ' ';
        int l = 0;
        for (char c : s2) {
            if (quoted) {
                s3[l++] = c;
                if (c == q) quoted = false;
            }
            else if (c == '\'' || c == '\"') {
                s3[l++] = c;
                q = c; quoted = true;
            }
            else if (c > ' ') {
                if (l > 0 && c == '=' && s3[l - 1] == ' ') l--;
                s3[l++] = c;
            } else {
                if (l > 0 && s3[l - 1] != ' ' && s3[l - 1] != '=') s3[l++] = ' ';
            }
        }
        if (s3[l - 1] > ' ') s3[l++] = ' ';

        boolean property = true;
        quoted = false;
        StringBuilder ss = new StringBuilder();
        String pro = "";
        for (int i = 0; i < l; i++) {
            char c = s3[i];
            if (property) {
                if (c == '\"' || c == '\'') new Error("invalid property syntax").verbose().fatal();
                else if (c == '=') {
                    property = false;
                    pro = ss.toString();
                    ss = new StringBuilder();
                    if (pro.isEmpty()) new Error("invalid property syntax").verbose().fatal();
                } else if (c == ' ') {
                    properties.put(ss.toString(), "");
                    ss = new StringBuilder();
                } else ss.append(c);
            } else {
                if (quoted) {
                    if (c == q) quoted = false;
                    else ss.append(c);
                } else if (c == '\"' || c == '\'') {
                    quoted = true;
                    q = c;
                } else if (c == ' ') {
                    properties.put(pro, ss.toString());
                    property = true;
                    ss = new StringBuilder();
                } else ss.append(c);
            }
        }
    return properties;
    }

    String addNotesLinks(String s, int part, boolean local) {
        int pos = 0;
        int notePos;
        StringBuilder output = new StringBuilder();
        String ref = (local) ? "" : String.format("x%04d.xhtml", part);

        while ((notePos = s.indexOf("<note id=\"", pos)) != -1) {
            int noteNumEnd = s.indexOf("\">", notePos + 10);
            if (noteNumEnd == -1) new Error("error in note tag").verbose().fatal();
            String label = s.substring(notePos + 10, noteNumEnd).trim();
            if (label.isEmpty()) new Error("note label not found").verbose().fatal();
            int noteEnd = s.indexOf("</note>", noteNumEnd + 1);
            if (noteEnd == -1) new Error("note tag is not closed").verbose().fatal();
            String link = s.substring(noteNumEnd + 2, noteEnd).trim();
            if (link.isEmpty()) new Error("note link is empty").verbose().fatal();
            if (noteLabels.get(label) == null)  new Error("no note with such label").verbose().fatal();
            int marker = noteLabels.get(label);
            if (marker == -1) new Error("note was already referenced").verbose().fatal();
            noteLabels.put(label, -1);
            String[] title = parseMarker(noteText.get(noteMarkers.get(marker)));
            if (title[1].isEmpty()) title[1] = link.substring(0, 1).toUpperCase() + link.substring(1);

            output.append(s, pos, notePos).append("<a href=\"").append(ref).append("#n").
                    append(label).append("\" id=\"r").append(label).append("\">").
                    append(link).append("</a>");
            pos = noteEnd + 7;

            // queue writing the note to notes file
            Note q = new Note(label, title[1], marker, part, local);
            if (local) {
                int i = 0;
                while (i < noteQueue.size() && noteQueue.get(i).part != part) i++;
                while (i < noteQueue.size() && noteQueue.get(i).part == part) i++;
                noteQueue.add(i, q);
            } else noteQueue.add(q);
        }
        output.append(s.substring(pos));
        return output.toString();
    }

    public String meta(String attr) {
        String r = meta.get(attr);
        if (r == null) return "";
        return r;
    }

    String[] parseMarker(String s) {
        String[] result = new String[2];
        int end = s.indexOf("-->");
        if (end == -1) new Error("invalid marker").verbose().fatal();
        int colon = s.indexOf(":");
        result[0] = (colon == -1) ? s.substring(4, end).trim() : s.substring(4, colon).trim();
        result[1] = (colon == -1) ? "" : s.substring(colon + 1, end).trim();
        return result;
    }

    public void createFolders() {
        Fops.createDirectory(outDir);
        Fops.createDirectory(outMetaInf);
        Fops.createDirectory(outOEBPS);
    }

    FWriter startXhtmlFile(String filename) {
        FWriter writer = new FWriter(outOEBPS + "\\" + filename + ".xhtml");
        for (int i = markers.get(0) + 1; i < markers.get(1); i++ )
            writer.write(text.get(i));
        return writer;
    }

    void endXhtmlFile(FWriter writer) {
        for (int i = markers.get(markers.size() - 1) + 1; i < text.size(); i++)
            writer.write(text.get(i));
        writer.close();
    }

    public void writeText() {
        for (int part = 1; part < markers.size() - 1; part++) {
            FWriter htmlFile = startXhtmlFile(String.format("%04d", part));
            for (int i = markers.get(part) + 1; i < markers.get(part + 1); i++)
                htmlFile.write(text.get(i));
            endXhtmlFile(htmlFile);
        }
    }

    public void writeNotes() {
        if (noteQueue.isEmpty()) return;
        int n = 0;
        Note note;

        do {
            note = noteQueue.get(n);
            FWriter noteFile = startXhtmlFile(String.format("x%04d", note.part));

            do {
                note = noteQueue.get(n);
                noteFile.write("<div class=\"note\" id=\"n" + note.label + "\">");
                String backref = (note.local) ? "" : String.format("%04d.xhtml", note.part);
                noteFile.write("<p class=\"link\"><a href=\"" + backref + "#r" + note.label +
                        "\">" + note.title + "</a></p>");
                for (int i = noteMarkers.get(note.marker) + 1; i < noteMarkers.get(note.marker + 1); i++ )
                    noteFile.write(noteText.get(i));
                noteFile.write("</div>");
                n++;
            } while (n < noteQueue.size() && note.part == noteQueue.get(n).part);

            endXhtmlFile(noteFile);
        } while (n < noteQueue.size());
    }

    public void writeImages() {
        int n = 0;
        for (Img img: imgQueue) {
            FWriter imgFile = startXhtmlFile(String.format("y%04d", n));

            StringBuilder s = new StringBuilder();
            s.append(String.format("<a href=\"%04d.xhtml#img%04d\"><img src=\"%s\"",
                    img.part, n, img.src));
            if (img.cls != null) s.append(" class=\"").append(img.cls).append("\"");
            if (img.style != null) s.append(" style=\"").append(img.style).append("\"");
            s.append("/></a>");
            imgFile.write(s.toString());
            if (img.alt!=null) imgFile.write(String.format("<p class=\"imgdesc\">%s</p>", img.alt));

            endXhtmlFile(imgFile);
            n++;
        }
    }

    static String extension(String s) {
        return s.substring(s.lastIndexOf('.') + 1).toLowerCase();
    }

    public void createMimetype() {
        FWriter mimetype = new FWriter(outDir + "\\mimetype");
        mimetype.write("application/epub+zip");
        mimetype.close();
    }

    public void createAppleDisplayOptions() {
        FWriter appleBook = new FWriter(outMetaInf + "\\com.apple.ibooks.display-options.xml");
        appleBook.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        appleBook.write("<display_options>");
        appleBook.write("<platform name=\"*\">");
        appleBook.write("<option name=\"specified-fonts\">true</option>");
        appleBook.write("</platform>");
        appleBook.write("</display_options>");
        appleBook.close();
    }
    public void createContainer() {
        FWriter container = new FWriter(outMetaInf + "\\container.xml");
        container.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        container.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">");
        container.write("<rootfiles>");
        container.write("<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>");
        container.write("</rootfiles>");
        container.write("</container>");
        container.close();
    }

    public void createContent()
    {
        Map<String, String> mediaType = Stream.of(new String[][] {
                { "jpg", "image/jpeg" },
                { "png", "image/png" },
                { "otf", "application/vnd.ms-opentype" },
                { "ttf", "application/vnd.ms-opentype" },
                { "css", "text/css" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        Map<String, String> contributors = Stream.of(new String[][] {
                { "translator", "tril" },
                { "illustrator", "ill" },
                { "afterword", "aft" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        Map<String, String> metas = Stream.of(new String[][] {
                { "series", "calibre:series" },
                { "number", "series_index" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        List<String> dcs = Stream.of("title", "creator", "language", "publisher", "description").collect(Collectors.toList());

        FWriter content = new FWriter(outOEBPS + "\\content.opf");
        content.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        content.write("<package version=\"3.0\" xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"bookid\" prefix=\"ibooks: http://vocabulary.itunes.apple.com/rdf/ibooks/vocabulary-extensions-1.0/\">");
        content.write("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        for (String attr: meta.keySet()) {
            if (dcs.contains(attr))
                content.write("<dc:" + attr + ">" + meta.get(attr) + "</dc:" + attr + ">");
            else if (contributors.containsKey(attr))
                content.write("<dc:contributor opf-role=\"" + contributors.get(attr) + "\">" +
                        meta.get(attr) + "</dc:contributor>");
            else if (metas.containsKey(attr))
                content.write("<meta name=\"" + metas.get(attr) + "\" content = \"" +
                        meta.get(attr) + "\"/>");
            else if (attr.equals("bookid"))
                content.write("<dc:identifier id=\"bookid\">" + meta.get(attr) + "</dc:identifier>");
        }
        content.write("<meta name=\"cover\" content=\"cover\"/>");
        SimpleDateFormat zformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        zformat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = zformat.format(new Date());
        content.write("<meta property=\"dcterms:modified\">" + time + "</meta>");
        content.write("</metadata>");

        content.write("<manifest>");
        content.write("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>");
        File[] manifest;
        File dir = new File(outOEBPS);
        FileFilter htmlFiles = pathname -> {
            if (pathname.isDirectory()) return false;
            return pathname.getName().endsWith(".xhtml");
        };
        manifest = dir.listFiles(htmlFiles);
        List<String> textFiles = new ArrayList<>();
        if (manifest != null)
            for (File file : manifest) {
                String filename = file.getName();
                content.write("<item id=\"" + filename + "\" href=\"" + filename +
                        "\" media-type=\"application/xhtml+xml\"/>");
                textFiles.add(filename);
            }
        textFiles.sort(Comparator.naturalOrder());

        File inputDirFile = new File(inDir);
        FileFilter noHtmlFiles = pathname -> {
            if (pathname.isDirectory()) return false;
            return !pathname.getName().endsWith(".xhtml");
        };
        manifest = inputDirFile.listFiles(noHtmlFiles);
        if (manifest != null)
            for (File file : manifest) {
                String filename = file.getName();
                String mtype = mediaType.get(extension(filename));
                if (mtype == null) {
                    new Error("media type not recognized: " + filename).warning();
                } else {
                    String id = (filename.equalsIgnoreCase(meta.get("cover"))) ? "cover" : filename;
                    content.write("<item id=\"" + id + "\" href=\"" + filename +
                            "\" media-type=\"" + mtype + "\"/>");
                    String targetFile = outOEBPS + "\\" + filename;
                    try {
                        Files.copy(file.toPath(), new File(targetFile).toPath(),
                                new StandardCopyOption[]{REPLACE_EXISTING});
                    } catch (IOException e) {
                        new Error("cannot write file: " + targetFile).fatal();
                    }
                }
            }
        content.write("</manifest>");

        content.write("<spine toc=\"ncx\">");
        for (String textFile: textFiles) {
            content.write("<itemref idref=\"" + textFile + "\"/>");
        }
        content.write("</spine>");
        content.write("<guide>");
        content.write("<reference type=\"cover\" href=\"0001.xhtml\" title=\"Cover\"/>");
        content.write("</guide>");
        content.write("</package>");
        content.close();
    }

    public void createTOC() {
        FWriter toc = new FWriter(outOEBPS + "\\toc.ncx");
        int maxChapters = markers.size() - 2;
        String[] title = new String[maxChapters];
        int[] level = new int[maxChapters + 1];
        int[] fileNum = new int[maxChapters];
        int[] line = new int[maxChapters];
        int maxLevel = 1;
        int chapters = 0;
        Error.file = htmlFile;

        int currentLevel = 1;
        for (int part = 1; part <= maxChapters; part++) {
            int l = markers.get(part);
            String[] mark = parseMarker(text.get(l));
            if (!mark[0].isEmpty()) {
                if (!mark[1].isEmpty()) {
                    try {
                        currentLevel = Integer.parseInt(mark[1]);
                        if (maxLevel < currentLevel) maxLevel = currentLevel;
                    } catch (NumberFormatException e) {
                        Error.line = l + 1;
                        new Error("cannot parse toc level").verbose().fatal();
                    }
                }
                level[chapters] = currentLevel;
                title[chapters] = mark[0];
                fileNum[chapters] = part;
                line[chapters] = l;
                chapters++;
            }
        }
        level[chapters] = 1;

        toc.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        toc.write("<!DOCTYPE ncx PUBLIC \"-//NISO//DTD ncx 2005-1//EN\"");
        toc.write("\"http://www.daisy.org/z3986/2005/ncx-2005-1.dtd\">");
        toc.write("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">");
        toc.write("<head>");
        toc.write("<meta name=\"dtb:uid\" content=\"" + meta.get("bookid") + "\"/>");
        toc.write("<meta name=\"dtb:depth\" content=\"" + maxLevel + "\"/>");
        toc.write("<meta name=\"dtb:totalPageCount\" content=\"0\"/>");
        toc.write("<meta name=\"dtb:maxPageNumber\" content=\"0\"/>");
        toc.write("</head>");
        toc.write("<docTitle><text>" + meta.get("title") + "</text></docTitle>");
        toc.write("<docAuthor><text>" + meta.get("creator") + "</text></docAuthor>");
        toc.write("<navMap>");

        for (int i = 0; i < chapters; i++) {
            toc.write("<navPoint id=\"toc" + (i + 1) +
                    "\" playOrder=\"" + (i + 1) +
                    "\"><navLabel><text>" + title[i] +
                    "</text></navLabel><content src=\"" + String.format("%04d", fileNum[i]) + ".xhtml\"/>");
            if (level[i + 1] > level[i]) {
                if (level[i + 1] - level[i] > 1) {
                    Error.line = line[i] + 1;
                    new Error("toc level increased by more than 1").verbose().fatal();
                }
            } else
                for (int j = 0; j < level[i] - level[i + 1] + 1; j++)
                    toc.write("</navPoint>");
        }

        toc.write("</navMap></ncx>");
        toc.close();
    }
}
