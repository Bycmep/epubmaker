import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Kepub {

    public static boolean on = false;
    public static int paragraph = 1;
    static final int MIN_SPAN = 32;

    public static String kepubify(String s) {
        int span = 1;

        String[] replace   = { "([A-Z])\\.", "([B-D,F-H,J-N,P-T,V-Z][b-d,f-h,j-n,p-t,v-z]+)\\." };   // initials, abbreviations
        String[] replaceTo = { "$1 ", "$1 " };

        List<Character> eos = Stream.of('.', '?', '!', '…').collect(Collectors.toList());

        int end = s.length(); s += "   ";

        String s1 = s + "     ";
        for (int i = 0; i < replace.length; i++) s1 = s1.replaceAll(replace[i], replaceTo[i]);

        char[] ss = s1.toCharArray();
        StringBuilder out = new StringBuilder();

        int spanStart = -1, pos = 0, tags;
        boolean pFound = false, pFinished = true;

        while (pos < end) {
            // look for <p
            while (pos < end && (ss[pos] != '<' || (ss[pos + 1] != 'p' && ss[pos + 1] != 'P')
                    || (ss[pos + 2] != '>' && ss[pos + 2] != ' '))) pos++;
            if (pos >= end) break;
            pos++;
            while (pos < end && ss[pos] != '>') pos++;
            if (pos >= end) break;
            pos++;
            if (spanStart == -1) out.append(s, 0, pos);
            spanStart = pos;
            pFound = true;
            pFinished = false;
            boolean endS = false;
            tags = 0;

            while (true) {
                if (ss[pos] == '<' && ss[pos + 1] == '/' &&
                        (ss[pos + 2] == 'p' || ss[pos + 2] == 'P') && ss[pos + 3] == '>' ) {
                    out.append("<span class=\"koboSpan\" id=\"kobo.").append(paragraph).append(".").append(span).
                            append("\">").append(s, spanStart, pos).append("</span></p>");
                    pFinished = true;
                    pos += 4;
                    break;
                }

                else if (eos.contains(ss[pos]))
                {
                    endS = true; pos++;
                }

                else if (ss[pos] == ' ' || ss[pos] == '”' || ss[pos] == '’' || ss[pos] == '\"') pos++;

                else if (endS) {
                    if (pos - spanStart < MIN_SPAN) endS = false;
                    else {
                        out.append("<span class=\"koboSpan\" id=\"kobo.").append(paragraph).append(".").append(span).
                                append("\">").append(s, spanStart, pos).append("</span>");
                        spanStart = pos;
                        endS = false;
                        span++;
                    }
                }

                else if (ss[pos] == '<') {
                    do {
                        pos++;
                        if (ss[pos - 1] == '<')
                            if (ss[pos] == '/') tags--;
                            else tags++;
                        else if (ss[pos - 1] == '/' && ss[pos] == '>') tags--;
                    } while ((tags > 0 || ss[pos - 1] != '>') && pos < end);
                    if (pos >= end) break;

                    if (tags < 0) new Error("tags are messed up").verbose().fatal();
                }

                else pos++;

            }
        }
        if(!pFound) return s;
        else paragraph++;
        if (!pFinished) new Error("for kepub, paragraph must be contained in one line").verbose().fatal();
        return out.toString();
    }

}
