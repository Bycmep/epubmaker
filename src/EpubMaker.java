public class EpubMaker {

    public static void main(String[] args) {
        String inputDir = "";

        System.out.println("EpubMaker 2023.5.5");
        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (arg.equals("-kepub")) Kepub.on = true;
                else if (arg.equals("-nocr")) FWriter.noCR = true;
                else new Error("unknown option: " + arg).warning();
            } else inputDir = arg;
        }
        if (inputDir.isEmpty()) new Error("must specify a directory to read").fatal();

        Book book = new Book(inputDir);
        book.createFolders();
        book.writeText();
        book.writeNotes();
        book.writeImages();

        book.createMimetype();
        book.createAppleDisplayOptions();
        book.createContainer();
        book.createContent();
        book.createTOC();

    }

}
