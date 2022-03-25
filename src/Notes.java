import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Notes {
    public List<String> text;
    public Map<String, EpubMaker.Note> markers;
    public List<EpubMaker.QueuedNote> queue;
    public List<String> localQueue;
    public int top, bottom;
    public List<String> files;

    public Notes(String notesFilename) {
        text = new ArrayList<>();
        markers = new HashMap<>();
        queue = new ArrayList<>();
        localQueue = new ArrayList<>();
        top = bottom = -1;
        files = new ArrayList<>();
        File notesHtml = new File(notesFilename);
        if (notesHtml.exists()) {
            text = Fops.readFileToList(notesFilename);
            Err.file = "notes.xhtml";
            int previousNote = -1;
            for (int j = 0; j < text.size(); j++) {
                if (text.get(j).startsWith("<!--")) {
                    if (previousNote != -1) {
                        bottom = j;
                        Err.line = previousNote + 1;
                        String[] s = Parse.marker(text.get(previousNote));
                        String label = s[0];
                        EpubMaker.Note note = new EpubMaker.Note();
                        note.start = previousNote;
                        note.end = j;
                        note.title = s[1];
                        if (label.isEmpty()) Err.fatal2("note label is empty");
                        if (markers.containsKey(label)) Err.fatal2("note label is not unique");
                        markers.put(label, note);
                    } else {
                        top = j;
                    }
                    previousNote = j;
                }
            }
        }
    }

}
