import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.lang.Iterable;
import java.lang.IndexOutOfBoundsException;

public class MyStack implements Iterable<MyStackFrame> {

    private List<MyStackFrame> sflist;
    private Method m;
    private String bpName;
    private Timestamp ts;
    private boolean broken;

    public MyStack(Method _m) {
        sflist = new ArrayList<MyStackFrame>();
        broken = false;
        ts = new Timestamp(System.currentTimeMillis());
        m = _m;
        if (m != null) {
            bpName = m.returnTypeName() + " " + m.declaringType().name() + " " + m.name() + "(" + String.join(",", m.argumentTypeNames()) + ")";
        } else {
            bpName = "<Breakpoint name could not be retrieved>";
        }
    }

    public String getTitle() {
        return bpName;
    }

    public void pushFrame(MyStackFrame f) {
        sflist.add(f);
    }

    public MyStackFrame getFrame(int i) throws IndexOutOfBoundsException {
        return sflist.get(i);
    }

    public int getFramesCount() {
        return sflist.size();
    }

    public void setBroken() {
        broken = true;
    }

    public boolean isBroken() {
        return broken;
    }

    public HashMap<Object, Object> toMap(HashMap<String, Object> inspection_strategy) {
        HashMap<Object, Object> ret = new HashMap<Object, Object>();
        ArrayList<Object> mylist = new ArrayList<Object>();
        if (!broken) {
            for (MyStackFrame sf : sflist) {
                String title = sf.getTitle();
                if (title.equals(""))
                    title = "<Method could not be retrieved>";
                HashMap<Object, Object> x = new HashMap<Object, Object>();
                x.put(title, sf.toList(inspection_strategy));
                mylist.add(x);
            }
        } else {
            HashMap<Object, Object> x = new HashMap<Object, Object>();
            x.put("<Stack could not be retrieved>", null);
            mylist.add(x);
        }
        ret.put(bpName, mylist);
        return ret;
    }

    public String getTime() {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(ts);
    }

    @Override
    public Iterator<MyStackFrame> iterator() {
        return sflist.iterator();
    }
}
