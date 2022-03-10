import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Book {
    public List<String> text;
    List<Integer> marker;
    Map<String, String> meta;

    public Book(String filename) {
        text = Fops.readFileToList(filename);
        marker = new ArrayList<>();
        for (int i = 0; i < text.size(); i++)
            if (text.get(i).startsWith("<!--"))
                marker.add(i);
        meta = new HashMap<>();

        Err.file = "main.html";
        String[] keywords = new String[] { "title", "creator", "language", "translator", "illustrator", "afterword",
                "publisher", "bookid", "description", "cover", "series", "number" };
        int mark = 0;
        while (mark < marker.size() - 1 && marker.get(mark) + 1 == marker.get(mark + 1)) mark++;
        for (int i = 0; i <= mark; i++) {
            Err.line = marker.get(i);
            String[] pair = Parse.marker(text.get(marker.get(i)));
            if (pair[0].isEmpty()) Err.warning2("meta attribute not found");
            else {
                String found = "";
                for (String keyword: keywords)
                    if (pair[0].equalsIgnoreCase(keyword)) {
                        found = keyword; break;
                    }
                if (found.isEmpty()) {
                    Err.warning2("meta attribute " + pair[0] + " is not recognized");
                } else if (meta.containsKey(found)) {
                    Err.warning2("meta attribute " + pair[0] + " is already defined");
                } else if (pair[1].isEmpty()) {
                    Err.warning2("meta attribute " + pair[0] + " has no value");
                } else {
                    meta.put(found, pair[1]);
                }
            }
        }
        if (meta("title").isEmpty() || meta("creator").isEmpty() || meta("bookid").isEmpty())
            Err.fatal("a book must have a title, a creator and an ID");
        if (mark > 0) marker.subList(0, mark).clear();
    }

    public String meta(String attr) {
        String r = meta.get(attr);
        if (r == null) return "";
        return r;
    }

}
