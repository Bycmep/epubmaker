public class EpubMaker {

    static class Note {
        public int start;
        public int end;
        public String title;
        public boolean isUsed = false;
    }

    static class QueuedNote {
        public String label;
        public int id;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Must specify a directory to read.");
            System.exit(-1);
        }
        String inputDir = args[0];
        String inputFilename = args[0] + "\\main.xhtml";
        String notesFilename = args[0] + "\\notes.xhtml";
        String[] outputDir = new String[3];
        outputDir[0] = args[0] + "_out";
        outputDir[1] = args[0] + "_out\\META-INF";
        outputDir[2] = args[0] + "_out\\OEBPS";

        Book book = new Book(inputFilename);
        Notes notes = new Notes(notesFilename);
        for (int i = 0; i < 3; i++) Fops.createDirectory(outputDir[i]);

        Epub.parse(book, notes, outputDir[2]);
        Epub.addNotes(notes, outputDir[2]);

        Epub.mimetype(outputDir[0]);
        Epub.appledisplayoptions(outputDir[1]);
        Epub.container(outputDir[1]);
        Epub.content(book, inputDir, outputDir[2]);
        Epub.toc(book, outputDir[2]);
/*

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
*/
    }



}
