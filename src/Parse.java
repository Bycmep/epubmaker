public class Parse {

    public static String[] marker(String s) {
        String[] result = new String[2];
        int end = s.indexOf("-->");
        if (end == -1) Err.fatal2("invalid marker");
        int colon = s.indexOf(":");
        result[0] = (colon == -1) ? s.substring(4, end).trim() : s.substring(4, colon).trim();
        result[1] = (colon == -1) ? "" : s.substring(colon + 1, end).trim();
        return result;
    }

    public static String extension(String s) {
        return s.substring(s.lastIndexOf('.') + 1).toLowerCase();
    }

}
