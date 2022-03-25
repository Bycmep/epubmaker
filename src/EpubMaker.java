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
    }

}
