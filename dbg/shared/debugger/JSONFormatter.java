import java.util.List;
import java.util.Map;

public class JSONFormatter {
    public static String format(Object o) {
        String m = "";
        if (o instanceof Map) m = format((Map<Object, Object>) o);
        else if (o instanceof List) m = format((List<Object>) o);
        else if (o instanceof String) m = format((String) o);
        else if (o == null) m = "null";
        else m = o.toString();
        return m;
    }

    private static String format(Map<Object, Object> hm) {
        if (hm.containsKey("xyznotakey")) {
            return format(hm.get("xyznotakey"));
        } else {
            String m = "{";
            for ( Map.Entry<Object, Object> entry : hm.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                if (key instanceof Integer) key = String.valueOf(key);
                m += format(key);
                m += " : ";
                m += format(val);
                m += ", ";
            }
            if (m.length() > 1) m = m.substring(0, m.length() - 2);
            m += "}";
            return m;
        }
    }

    private static String format(List<Object> l) {
        String m = "[";
        for (Object o : l) {
            m += format(o);
            m += ", ";
        }
        if (m.length() > 1) m = m.substring(0, m.length() - 2);
        m += "]";
        return m;
    }

    private static String format(String s) {
        if (s == null) return "null";
        return quote(s);
    }

    public static String quote(String s) {
        String ret = "\"" + escape(s) + "\"";
        return ret;
    }

    private static String escape(String input) {
        StringBuilder buffer = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            if ((int) input.charAt(i) > 256) {
                buffer.append("\\\\u").append(Integer.toHexString((int) input.charAt(i)));
            } else {
                if (input.charAt(i) == '\n') {
                    buffer.append("\\n");
                } else if(input.charAt(i) == '\t'){
                    buffer.append("\\t");
                } else if(input.charAt(i) == '\r'){
                    buffer.append("\\r");
                } else if(input.charAt(i) == '\b'){
                    buffer.append("\\b");
                } else if(input.charAt(i) == '\f'){
                    buffer.append("\\f");
                } else if(input.charAt(i) == '\"'){
                    buffer.append("\\\"");
                } else if(input.charAt(i) == '\\'){
                    buffer.append("\\\\");
                } else if (((int)input.charAt(i)) < 0x20 || ((int)input.charAt(i)) >= 0x7f) {
                    buffer.append("\\\\x" + Integer.toHexString((int) input.charAt(i)));
                } else {
                    buffer.append(input.charAt(i));
                }
            }
        }
        return buffer.toString();
    }

}
