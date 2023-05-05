public class Error {
    public static String file = "";
    public static int line = 0;
    private static String errorMessage;

    public Error() {
        errorMessage = "";
    }

    public Error(String message) {
        errorMessage = message;
    }

    public Error cannotWrite(String filename) {
        errorMessage = "cannot write file " + filename;
        return this;
    }

    public Error alreadyExists(String filename) {
        errorMessage = "file " + filename + " already exists";
        return this;
    }

    public Error cannotCreate(String filename) {
        errorMessage = "failed to create file " + filename;
        return this;
    }

    public Error fileNotFound(String filename) {
        errorMessage = "file " + filename + " not found";
        return this;
    }

    public Error cannotRead(String filename) {
        errorMessage = "file " + filename + " cannot be read";
        return this;
    }

    public Error folderAlreadyExists(String foldername) {
        errorMessage = "folder " + foldername + " already exists";
        return this;
    }

    public Error cannotCreateFolder(String foldername) {
        errorMessage = "cannot create folder " + foldername;
        return this;
    }

    public void fatal() {
        System.out.println("Fatal: " + errorMessage + ".");
        System.exit(-1);
    }

    public Error verbose() {
        errorMessage += " (file " + file + ", line " + (line + 1) + ")";
        return this;
    }

    public void warning() {
        System.out.println("Warning: " + errorMessage + ".");
    }

}
