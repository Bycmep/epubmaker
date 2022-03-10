public class Err {
    public static String file = "";
    public static int line = 0;

    public static void warning(String message) {
        System.out.println("Warning: " + message + ".");
    }

    public static void warning2(String message) {
        System.out.println("Warning: " + file + " (" + line + "): " + message + ".");
    }

    public static void fatal(String message) {
        System.out.println("Fatal: " + message + ".");
        System.exit(-1);
    }

    public static void fatal2(String error) {
        System.out.println("Fatal: " + file + " (" + line + "): " + error + ".");
        System.exit(-1);
    }


}
