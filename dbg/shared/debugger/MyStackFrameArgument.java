import com.sun.jdi.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Base64;

public class MyStackFrameArgument {

    private MyJDIValue jdiValue;
    private Object myValue;
    private int myDepth = -2;
    public String stringValue;
    private boolean broken = false;

    public MyStackFrameArgument(Value v) {
        jdiValue = new MyJDIValue(v);
    }

    public void parse(int depth) {
        if (myDepth == -2 || ((depth > myDepth || depth == -1 || depth == 0) && myDepth > 0)) {
            try {
                jdiValue.setDepth(depth);
                ObjectMapper mapper = new ObjectMapper();
                stringValue = mapper.writeValueAsString(jdiValue);
                if (MyDebugger.stopped) {
                    myDepth = 1;
                }
                HashMap<Object, Object> mapping = new ObjectMapper().readValue(stringValue, HashMap.class);
                myValue = mapping;
                myDepth = depth;
                if (depth <= 0) myDepth = -1;
            } catch (Exception e) {
                e.printStackTrace();
                broken = true;
            }
        }
    }

    public Object getObject() {
        if (!broken)
            return myValue;
        else
            return "<Argument could not be retrieved>";
    }

    public String checkMarks(HashSet<String> marks) {
        parse(-1);
        for (String m : marks) {
            boolean b = stringValue.contains(m);
            b = b || stringValue.contains(m.toUpperCase());
            b = b || stringValue.contains(m.toLowerCase());
            b = b || stringValue.contains(Base64.getEncoder().encodeToString(m.getBytes()));
            b = b || stringValue.contains(new String(Base64.getDecoder().decode(m)));
            if (b) {
                return m;
            }
        }
        return null;
    }
}
