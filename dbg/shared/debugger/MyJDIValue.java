import com.sun.jdi.Value;
import com.sun.jdi.Field;
import com.sun.jdi.Type;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.CharValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.VoidValue;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.ObjectOutputStream;
import java.lang.Boolean;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MyJDIValue {

    public Value xyznotakey;

    @JsonIgnore
    public ArrayList<Value> ancestors;

    @JsonIgnore
    public int depth;


    public MyJDIValue(Value v) {
        xyznotakey = v;
        ancestors = new ArrayList<Value>();
        depth = -1;
    }

    public MyJDIValue(Value v, int d) {
        xyznotakey = v;
        ancestors = new ArrayList<Value>();
        depth = d;
    }

    public MyJDIValue(Value v, ArrayList<Value> _ancestors) {
        xyznotakey = v;
        ancestors = _ancestors;
        depth = -1;
    }

    public MyJDIValue(Value v, ArrayList<Value> _ancestors, int d) {
        xyznotakey = v;
        ancestors = _ancestors;
        depth = d;
    }

    public void setDepth(int d) {
        depth = d;
    }

    public Object getXyznotakey() {
        if (xyznotakey == null) return null;
        Type tp = xyznotakey.type();
        if (tp == null) return "null";
        
        if (ancestors.size() > 0 && ancestors.get(ancestors.size() - 1) == xyznotakey) {
            return "this";
        }

        if (ancestors.contains(xyznotakey)) {
            return "backref";
        }

        if (depth > 0 && ancestors.size() > depth && xyznotakey instanceof ObjectReference && !(xyznotakey instanceof StringReference)) {
            return "tooDeep";
        }

        if (MyDebugger.stopped == true) {
            return "stopped";
        }

        if (xyznotakey instanceof ObjectReference) {
            if (xyznotakey instanceof ArrayReference) return new ArrayList<MyJDIValue>(arrayReferenceToArrayList((ArrayReference) xyznotakey));
            else if (xyznotakey instanceof StringReference) return xyznotakey.toString().substring(1, xyznotakey.toString().length() - 1);
            else if (xyznotakey instanceof ClassObjectReference) return classObjectReferenceToHashMap((ClassObjectReference) xyznotakey);
            else if (xyznotakey instanceof ClassLoaderReference) return ((ClassLoaderReference) xyznotakey).toString();
            else if (xyznotakey instanceof ThreadReference) return ((ThreadReference) xyznotakey).toString();
            else if (xyznotakey instanceof ThreadGroupReference) return ((ThreadGroupReference) xyznotakey).toString();
            else return objectReferenceToHashMap((ObjectReference) xyznotakey);
        } else if (xyznotakey instanceof PrimitiveValue) {
            if (xyznotakey instanceof BooleanValue) return ((PrimitiveValue) xyznotakey).booleanValue();
            else if (xyznotakey instanceof ByteValue) return ((PrimitiveValue) xyznotakey).byteValue();
            else if (xyznotakey instanceof CharValue) return ((PrimitiveValue) xyznotakey).charValue();
            else if (xyznotakey instanceof DoubleValue) return ((PrimitiveValue) xyznotakey).doubleValue();
            else if (xyznotakey instanceof FloatValue) return ((PrimitiveValue) xyznotakey).floatValue();
            else if (xyznotakey instanceof IntegerValue) return ((PrimitiveValue) xyznotakey).intValue();
            else if (xyznotakey instanceof LongValue) return ((PrimitiveValue) xyznotakey).longValue();
            else if (xyznotakey instanceof ShortValue) return ((PrimitiveValue) xyznotakey).shortValue();
            else if (xyznotakey instanceof VoidValue) return null;
            else return xyznotakey + "";
        } else {
            return null;
        }
    }

    public ArrayList<MyJDIValue> arrayReferenceToArrayList(ArrayReference v) {
        List<Value> vals = (List<Value>) v.getValues();
        ArrayList<MyJDIValue> myvals = new ArrayList<MyJDIValue>();
        ArrayList<Value> newAncestors = new ArrayList<Value>(ancestors);
        newAncestors.add(xyznotakey);
        for (Value _v : vals) {
            myvals.add(new MyJDIValue(_v, newAncestors, depth));
        }
        return myvals;
    }

    public HashMap<String, MyJDIValue> classObjectReferenceToHashMap(ClassObjectReference o) {
        Map<Field, Value> m = o.getValues(o.referenceType().fields());
        HashMap<String, MyJDIValue> mymap = new HashMap<String, MyJDIValue>();
        ArrayList<Value> newAncestors = new ArrayList<Value>(ancestors);
        newAncestors.add(xyznotakey);
        for ( Map.Entry<Field, Value> entry : m.entrySet()) {
            Field f = entry.getKey();
            Value v = entry.getValue();
            String name;
            try {
                name = f.type().name() + " " + f.name();
            } catch (Exception e) {
                name = "<Field could not be retrieved>";
            }
            mymap.put(name, new MyJDIValue(v, newAncestors, depth));
        }
        return mymap;
    }

    public HashMap<String, MyJDIValue> objectReferenceToHashMap(ObjectReference o) {
        Map<Field, Value> m = o.getValues(o.referenceType().fields());
        HashMap<String, MyJDIValue> mymap = new HashMap<String, MyJDIValue>();
        ArrayList<Value> newAncestors = new ArrayList<Value>(ancestors);
        newAncestors.add(xyznotakey);
        for ( Map.Entry<Field, Value> entry : m.entrySet()) {
            Field f = entry.getKey();
            Value v = entry.getValue();
            String name;
            try {
                name = f.type().name() + " " + f.name();
            } catch (Exception e) {
                name = "<Field could not be retrieved>";
            }
            mymap.put(name, new MyJDIValue(v, newAncestors, depth));
        }
        return mymap;
    }
    
}
