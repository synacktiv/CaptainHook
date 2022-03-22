import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.lang.Iterable;
import java.lang.Exception;

public class MyStackFrame {
    public String title = "";
    public List<MyStackFrameArgument> args;
    public boolean broken = false;
    public int id;
    public List<String> argstypes;

    public MyStackFrame() {
        setTitle("");
        args = new ArrayList<MyStackFrameArgument>();
    }

    public MyStackFrame(StackFrame sf, int _id) {
        try {
            Method m = sf.location().method();
            argstypes = m.argumentTypeNames();
            setTitle(m.returnTypeName() + " " + m.declaringType().name() + " " + m.name() +  "(" + String.join(",", argstypes) + ")");
        } catch (Exception e) {
            setBroken();
        }
        try {
            setArgs(sf.getArgumentValues());
        } catch (Exception e) {
            setBroken();
        }
        id = _id;
    }

    public MyStackFrame(String _title, List<Value> _args) {
        setTitle(_title);
        setArgs(_args);
    }

    public void setTitle(String _title) {
        title = _title;
    }

    public String getTitle() {
        return title;
    }

    public void setArgs(List<Value> _args) {
        args = new ArrayList<MyStackFrameArgument>();
        for (Value v : _args) {
            args.add(new MyStackFrameArgument(v));
        }
    }

    public List<MyStackFrameArgument> getArgs() {
        return args;
    }

    public void setBroken() {
        broken = true;
    }

    public boolean isBroken() {
        return broken;
    }

    public ArrayList<Object> toList(HashMap<String, Object> inspection_strategy) {
        ArrayList<Object> mylist = new ArrayList<Object>();
        if (!goDeep(inspection_strategy)) return mylist;
        if (!broken) {
            for (MyStackFrameArgument v : args) {
                mylist.add(v.getObject());
            }
        } else {
            mylist.add("<Arguments could not be retrieved>");
        }
        return mylist;
    }

    public boolean goDeep(HashMap<String, Object> inspection_strategy) {
        if (inspection_strategy == null) return true;
        HashMap<String, Object> descriptor = new HashMap<String, Object>();
        String nameParser[] = getTitle().split(" ");
        String argcString;
        try {
            argcString = nameParser[2].split("\\(")[1].split("\\)")[0];
        } catch (Exception e) {
            argcString = "";
        }
        String clazz = nameParser[1];
        int argc = argstypes.size();
        String name = nameParser[2].split("\\(")[0];

        if (argcString.length() < 2)
            argc = 0;
            
        descriptor.put("id", id);
        descriptor.put("argc", argc);
        descriptor.put("class", clazz);
        descriptor.put("name", name);
        descriptor.put("argstypes", argstypes);
        return StrategyAnalyzer.complies(descriptor, inspection_strategy);
    }

    public void inspectForFrida(int depth) {
        if (!isBroken()) {
            for (MyStackFrameArgument v : getArgs())
                v.parse(depth);
        }
    }

    public ArrayList<Integer> checkMarks(HashSet<String> marks) {
        if (!broken) {
            ArrayList<Integer> ret = new ArrayList<Integer>();
            for (int i = 0; i < this.getArgc(); i++) {
                MyStackFrameArgument arg = args.get(i);
                arg.parse(4);
                for (String m : marks) {
                    if (arg.stringValue.contains(m)) {
                        ret.add(i);
                    }
                }
            }
            return ret;
        } else {
            return new ArrayList<Integer>();
        }
    }

    public int getArgc() {
        return args.size();
    }

    public MyStackFrameArgument getArg(int id) {
        return args.get(id);
    }

}
