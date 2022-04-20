import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Epub {

    public static void parse(Book book, Notes notes, String dir) {
        Err.file = "main.xhtml";
        for (int part = 1; part < book.marker.size() - 1; part++) {
            FWriter htmlFile = new FWriter(dir + "\\" + String.format("%04d", part) + ".xhtml");

            // html file head
            for (int i = book.marker.get(0) + 1; i < book.marker.get(1); i++ )
                htmlFile.write(book.text.get(i));
            // html file body
            Kepub.kePara = 1;
            for (int i = book.marker.get(part) + 1; i < book.marker.get(part + 1); i++ ) {
                String text = book.text.get(i);
                Err.line = i + 1;
                if (text.contains("<note ")) text = addNotesLinks(text, part, notes);
                if (EpubMaker.kepub) text = Kepub.kepubify(text);
                htmlFile.write(text);
            }
            // html tail
            for (int i = book.marker.get(book.marker.size() - 1) + 1; i < book.text.size(); i++ )
                htmlFile.write(book.text.get(i));
            htmlFile.close();
        }
    }

    private static String addNotesLinks(String text, int id, Notes notes) {
        int pos = 0;
        int notePos;
        StringBuilder output = new StringBuilder();
        String ref = (id == -1) ? "" : String.format("x%04d.xhtml", id);

        while ((notePos = text.indexOf("<note id=\"", pos)) != -1) {
            int noteNumEnd = text.indexOf("\">", notePos + 10);
            if (noteNumEnd == -1) Err.fatal2("error in note tag");
            String noteLabel = text.substring(notePos + 10, noteNumEnd).trim();
            if (noteLabel.isEmpty()) Err.fatal2("note label not found");
            int noteEnd = text.indexOf("</note>", noteNumEnd + 1);
            if (noteEnd == -1) Err.fatal2("note tag is not closed");
            String noteLink = text.substring(noteNumEnd + 2, noteEnd);
            if (noteLink.isEmpty()) Err.fatal2("note link is empty");
            EpubMaker.Note note = notes.markers.get(noteLabel);
            if (note == null) Err.fatal2("no note with such label");
            else {
                if (note.isUsed) Err.fatal2("note was already referenced");
                note.isUsed = true;
                String s = noteLink.trim();
                if (note.title.isEmpty()) note.title = s.substring(0, 1).toUpperCase() + s.substring(1);
                notes.markers.replace(noteLabel, note);
            }

            output.append(text, pos, notePos).append("<a href=\"").append(ref).append("#n").
                    append(noteLabel).append("\" id=\"r").append(noteLabel).append("\" epub:type=\"noteref\">").
                    append(noteLink).append("</a>");
            pos = noteEnd + 7;

            // queue writing the note to notes file
            EpubMaker.QueuedNote qNote = new EpubMaker.QueuedNote();
            qNote.id = id;
            qNote.label = noteLabel;
            if (id == -1) {
                int p = 1;
                while (p < notes.queue.size() && notes.queue.get(p).id == -1) p++;
                notes.queue.add(p, qNote);
            }
            else notes.queue.add(qNote);
        }
        output.append(text.substring(pos));
        return output.toString();
    }

    public static void addNotes(Notes notes, String dir) {
        if(notes.queue.isEmpty()) return;
        EpubMaker.QueuedNote record;
        FWriter noteFile;
        int id = 0;
        Err.file = "notes.xhtml";
        do {
            record = notes.queue.get(0);
            noteFile = new FWriter(dir + String.format("\\x%04d.xhtml", record.id));
            for (int i = 0; i < notes.top; i++)
                noteFile.write(notes.text.get(i));
            do {
                if(record.id != -1) id = record.id;
                noteFile.write("<div class=\"note\" id=\"n" + record.label + "\" epub:type=\"footnote\">");
                String backref = (record.id == -1) ? "" : String.format("%04d.xhtml", record.id);
                EpubMaker.Note note = notes.markers.get(record.label);
                noteFile.write("<p class=\"link\"><a href=\"" + backref + "#r" + record.label +
                        "\">" + note.title + "</a></p>");
                for (int j = note.start + 1; j < note.end; j++) {
                    String text = notes.text.get(j);
                    if (text.contains("<note ")) {
                        Err.line = j;
                        noteFile.write(addNotesLinks(text, -1, notes));
                    } else noteFile.write(text);
                }
                noteFile.write("</div>");
                notes.queue.remove(0);
                if (notes.queue.isEmpty()) break;
                record = notes.queue.get(0);
            } while (record.id == id || record.id == -1);
            for (int i = notes.bottom + 1; i < notes.text.size(); i++)
                noteFile.write(notes.text.get(i));
            noteFile.close();
        } while(!notes.queue.isEmpty());

    }


    public static void mimetype(String dir) {
        FWriter mimetype = new FWriter(dir + "\\mimetype");
        mimetype.write("application/epub+zip");
        mimetype.close();
    }

    public static void appledisplayoptions(String dir) {
        FWriter applebook = new FWriter(dir + "\\com.apple.ibooks.display-options.xml");
        applebook.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        applebook.write("<display_options>");
        applebook.write("<platform name=\"*\">");
        applebook.write("<option name=\"specified-fonts\">true</option>");
        applebook.write("</platform>");
        applebook.write("</display_options>");
        applebook.close();
    }
    public static void container(String dir) {
        FWriter container = new FWriter(dir + "\\container.xml");
        container.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        container.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">");
        container.write("<rootfiles>");
        container.write("<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>");
        container.write("</rootfiles>");
        container.write("</container>");
        container.close();
    }

    public static void content(Book book, String inputDir, String outputDir)
    {
        Map<String, String> mediatype = Map.of("jpg", "image/jpeg", "png", "image/png",
                "otf", "application/vnd.ms-opentype", "ttf", "application/vnd.ms-opentype",
                "css", "text/css");
        List<String> dcs = List.of("title", "creator", "language", "publisher", "description");
        Map<String, String> contributors = Map.of("translator", "tril", "illustrator", "ill", "afterword", "aft");
        Map<String, String> metas = Map.of("series", "calibre:series", "number", "series_index");

        FWriter content = new FWriter(outputDir + "\\content.opf");
        content.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        content.write("<package version=\"3.0\" xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"bookid\" prefix=\"ibooks: http://vocabulary.itunes.apple.com/rdf/ibooks/vocabulary-extensions-1.0/\">");
        content.write("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        for (String attr: book.meta.keySet()) {
            if (dcs.contains(attr))
                content.write("<dc:" + attr + ">" + book.meta.get(attr) + "</dc:" + attr + ">");
            else if (contributors.containsKey(attr))
                content.write("<dc:contributor opf-role=\"" + contributors.get(attr) + "\">" +
                        book.meta.get(attr) + "</dc:contributor>");
            else if (metas.containsKey(attr))
                content.write("<meta name=\"" + metas.get(attr) + "\" content = \"" +
                        book.meta.get(attr) + "\"/>");
            else if (attr.equals("bookid"))
                content.write("<dc:identifier id=\"bookid\">" + book.meta.get(attr) + "</dc:identifier>");
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
        File dir = new File(outputDir);
        FileFilter htmlFiles = pathname -> {
            if (pathname.isDirectory()) return false;
            return pathname.getName().endsWith(".xhtml");
        };
        manifest = dir.listFiles(htmlFiles);
        List<String> textfiles = new ArrayList<>();
        if (manifest != null)
            for (File file : manifest) {
                String filename = file.getName();
                content.write("<item id=\"" + filename + "\" href=\"" + filename +
                        "\" media-type=\"application/xhtml+xml\"/>");
                textfiles.add(filename);
            }
        textfiles.sort(Comparator.naturalOrder());

        File inputDirFile = new File(inputDir);
        FileFilter noHtmlFiles = pathname -> {
            if (pathname.isDirectory()) return false;
            return !pathname.getName().endsWith(".xhtml");
        };
        manifest = inputDirFile.listFiles(noHtmlFiles);
        if (manifest != null)
            for (File file : manifest) {
                String filename = file.getName();
                String mtype = mediatype.get(Parse.extension(filename));
                if (mtype == null) {
                    Err.warning("media type not recognized: " + filename);
                } else {
                    String id = (filename.equalsIgnoreCase(book.meta.get("cover"))) ? "cover" : filename;
                    content.write("<item id=\"" + id + "\" href=\"" + filename +
                            "\" media-type=\"" + mtype + "\"/>");
                    String targetFile = outputDir + "\\" + filename;
                    try {
                        Files.copy(file.toPath(), new File(targetFile).toPath(),
                                new StandardCopyOption[]{REPLACE_EXISTING});
                    } catch (IOException e) {
                        Err.fatal("cannot write file: " + targetFile);
                    }
                }
            }
        content.write("</manifest>");

        content.write("<spine toc=\"ncx\">");
        for (String textfile: textfiles) {
            content.write("<itemref idref=\"" + textfile + "\"/>");
        }
        content.write("</spine>");
        content.write("<guide>");
        content.write("<reference type=\"cover\" href=\"0001.xhtml\" title=\"Cover\"/>");
        content.write("</guide>");
        content.write("</package>");
        content.close();
    }

    public static void toc(Book book, String outputDir) {
        FWriter toc = new FWriter(outputDir + "\\toc.ncx");
        int maxChapters = book.marker.size() - 2;
        String[] title = new String[maxChapters];
        int[] level = new int[maxChapters + 1];
        int[] fileNum = new int[maxChapters];
        int[] line = new int[maxChapters];
        int maxLevel = 1;
        int chapters = 0;
        Err.file = "main.xhtml";

        int currentLevel = 1;
        for (int part = 1; part <= maxChapters; part++) {
            int l = book.marker.get(part);
            String[] mark = Parse.marker(book.text.get(l));
            if (!mark[0].isEmpty()) {
                if (!mark[1].isEmpty()) {
                    try {
                        currentLevel = Integer.parseInt(mark[1]);
                        if (maxLevel < currentLevel) maxLevel = currentLevel;
                    } catch (NumberFormatException e) {
                        Err.line = l + 1;
                        Err.fatal2("cannot parse toc level");
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
        toc.write("<meta name=\"dtb:uid\" content=\"" + book.meta.get("bookid") + "\"/>");
        toc.write("<meta name=\"dtb:depth\" content=\"" + maxLevel + "\"/>");
        toc.write("<meta name=\"dtb:totalPageCount\" content=\"0\"/>");
        toc.write("<meta name=\"dtb:maxPageNumber\" content=\"0\"/>");
        toc.write("</head>");
        toc.write("<docTitle><text>" + book.meta.get("title") + "</text></docTitle>");
        toc.write("<docAuthor><text>" + book.meta.get("creator") + "</text></docAuthor>");
        toc.write("<navMap>");

        for (int i = 0; i < chapters; i++) {
            toc.write("<navPoint id=\"toc" + (i + 1) +
                    "\" playOrder=\"" + (i + 1) +
                    "\"><navLabel><text>" + title[i] +
                    "</text></navLabel><content src=\"" + String.format("%04d", fileNum[i]) + ".xhtml\"/>");
            if (level[i + 1] > level[i]) {
                if (level[i + 1] - level[i] > 1) {
                    Err.line = line[i] + 1;
                    Err.fatal2("toc level changed by more than 1");
                }
            } else
                for (int j = 0; j < level[i] - level[i + 1] + 1; j++)
                    toc.write("</navPoint>");
        }

        toc.write("</navMap></ncx>");
        toc.close();
    }

}
