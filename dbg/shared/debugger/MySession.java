import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.lang.Thread;
import java.util.Random;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MySession {
    private MyStack stack;
    private int id;
    private HashMap<String, String> marks;
    private int max_depth = 3;
    private HashMap<String, Object> sf_inspection_strategy;
    private HashMap<String, Object> object_inspection_strategy;
    private HashMap<Integer, String> messages = new HashMap<Integer, String>();
    private HashMap<Integer, Thread> tasks = new HashMap<Integer, Thread>();
    

    public MySession(MyStack _stack, int _id, HashMap<String, String> _marks) {
        stack = _stack;
        id = _id;
        marks = _marks;
        setStackInspectionStrategy("{}");
    }

    public String listStackframes() {
        ArrayList<String> titles = new ArrayList<String>();
        for (MyStackFrame sf : stack) {
            titles.add(sf.getTitle());
        }
        return MyDebugger.javaObjectToBase64("stackframes", titles);
    }

    public String inspectStackframe(int sf_id) {
        try {
            stack.getFrame(sf_id).inspectForFrida(1);
            return MyDebugger.javaObjectToBase64(stack.getFrame(sf_id).getTitle(), stack.getFrame(sf_id).toList(new ObjectMapper().readValue("{\"idMin\":-1}", HashMap.class)));
        } catch (Exception e) {
            return "e30=";
        }
        
    }

    public String inspectArgument(int sf_id, int arg_id) {
        try {
            MyStackFrame sf = stack.getFrame(sf_id);
            MyStackFrameArgument arg = sf.getArg(arg_id);
            arg.parse(-1);
            return MyDebugger.javaObjectToBase64(sf.getTitle(), arg.getObject());
        } catch (Exception e) {
            System.out.println("Argument not found");
            return "e30=";
        }
    }

    public String getMessage(int mess_id) {
        if (messages.containsKey(mess_id)) {
            return messages.get(mess_id);
        } else {
            return "";
        }
    }

    public HashMap<String, Object> toNotif() {
        HashMap<String, Object> h = new HashMap<String, Object>();
        h.put("method", stack.getTitle());
        h.put("id", id);
        h.put("marks", marks);
        return h;
    }

    public void setMaxInspectionDepth(int depth) {
        max_depth = depth;
    }

    public void setStackInspectionStrategy(String jsonStrat) {
        if (jsonStrat.equals("{}")) {
            // Inspect *
            try {
                sf_inspection_strategy = new ObjectMapper().readValue("{\"idMin\":-1}", HashMap.class);
            } catch (Exception e) {e.printStackTrace();}
        } else {
            try {
                sf_inspection_strategy = new ObjectMapper().readValue(jsonStrat, HashMap.class);
            } catch (Exception e) {e.printStackTrace();}
        }
        
    }

    public void setObjectInspectionStrategy(String jsonStrat) {
        if (jsonStrat.equals("{}")) {
            // Inspect *
            try {
                object_inspection_strategy = new ObjectMapper().readValue("{\"idMin\":-1}", HashMap.class);
            } catch (Exception e) {e.printStackTrace();}
        } else {
            try {
                object_inspection_strategy = new ObjectMapper().readValue(jsonStrat, HashMap.class);
            } catch (Exception e) {e.printStackTrace();}
        }
    }

    public String getObjectInspectionStrategy() {
        return MyDebugger.javaObjectToBase64("objectInspectionStrategy", object_inspection_strategy);
    }

    public String getMaxInspectionDepth() {
        return MyDebugger.javaObjectToBase64("maxInspectionDepth", Integer.toString(max_depth));
    }

    public String getStackInspectionStrategy() {
        return MyDebugger.javaObjectToBase64("stackInspectionStrategy", sf_inspection_strategy);
    }
    
    public int findFirstMarkOccurence() {
        // THIS DOES NOT WORK
        // build the HashSet
        HashSet<String> hs = new HashSet<String>();
        for (Map.Entry<String, String> entry : this.marks.entrySet()) {
            hs.add(entry.getValue());
        }
        // binary search to find first occurence
        int a = 0;
        int b = this.stack.getFramesCount() - 1;
        while (b - a > 1) {
            int c = (b + a) / 2;
            // inspect stackframe c
            ArrayList<Integer> marksArgsC = stack.getFrame(c).checkMarks(hs);
            if (marksArgsC.size() > 0) {
                a = c;
            } else {
                b = c;
            }
        }
        return a;
    }

    public String dump() {
        try {
            if (!stack.isBroken()) {
                for (MyStackFrame sf : stack) {
                    if (!sf.isBroken() && sf.goDeep(sf_inspection_strategy)) {
                        for (MyStackFrameArgument v : sf.getArgs())
                            v.parse(max_depth);
                    }
                }
            }
            return MyDebugger.javaObjectToBase64("stacktrace", stack.toMap(sf_inspection_strategy), stack.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        
    }

    public MyStackFrame getStackframe(int sf_id) {
        return stack.getFrame(sf_id);
    }

    public String listMarks() {
        return MyDebugger.javaObjectToBase64("marks", marks);
    }
}
