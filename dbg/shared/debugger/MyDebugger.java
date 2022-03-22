import com.sun.jdi.VirtualMachine;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.InternalException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Type;
import com.sun.jdi.ClassType;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Base64;
import java.lang.Thread;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.InterruptedException;
import java.lang.System;
import java.lang.Thread;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MyDebugger {
    public static final int STRATEGY_MAXDEPTH = 0;
    public static final int STRATEGY_SMART = 1;

    public static VirtualMachine vm;
    public static EventRequestManager manager;
    public static ArrayList<MySession> sessionsList = new ArrayList<MySession>();
    private static HashMap<Method, BreakpointRequest> breakpointRequests = new HashMap<Method, BreakpointRequest>();
    private static int counter = 0;
    public static int sessionIdCounter = 0;
    private static HashSet<String> marks = new HashSet<String>();
    public static volatile boolean stopped = false;
    public static volatile boolean marksEnabled = true;
    public static ArrayList<ArrayList<String>> bpList = new ArrayList<ArrayList<String>>();
    public static SocketServer socketServer; 
    public static ArrayList<String[]> shellCmds;
    public static String bbPath = new String();


    private static VirtualMachine connectVM() throws Exception {
        return connectVM("localhost", "1000");
    }

    private static VirtualMachine connectVM(String h, String p) throws Exception {
        List<AttachingConnector> attConList = Bootstrap.virtualMachineManager().attachingConnectors();
        AttachingConnector attachConn = attConList.get(1); // the socketconnector
        Map<String, Connector.Argument> argz = attachConn.defaultArguments();
        argz.get("hostname").setValue(h);
        argz.get("port").setValue(p);
        vm = attachConn.attach(argz);
        clearBreakpoints();
        return vm;
    }

    public static void main(String[] args) throws Exception {
        socketServer = new SocketServer("/workdir/socket_j2p", "/workdir/socket_p2j");
        shellCmds = new ArrayList<String[]>();
        try {
            if (args.length == 2)
                connectVM(args[0], args[1]);
            else
                connectVM();
        } catch (Exception e) {
            System.out.println("Connection to the main JVM failed...");
            return;
        }
        
        manager = vm.eventRequestManager();
        
        // setObjectInspectionStrategy("{}");

        System.out.println("Successfully attached to main program");
        eventLoop();
    }

    private static void eventLoop() throws InterruptedException {
        EventSet eventSet = null;
        while ((eventSet = vm.eventQueue().remove()) != null) {
            for (Event event : eventSet) {
                if (event instanceof BreakpointEvent) {
                    try {
                        Semaphore sem = new Semaphore(0);
                        BreakpointRequest req = (BreakpointRequest) event.request();
                        Method m = getBreakpointRequestMethod(req);
                        BreakpointHandler bph = new BreakpointHandler((BreakpointEvent) event, sem, m);
                        Thread thr = new Thread(bph);
                        thr.start();
                        sem.acquire();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event instanceof ClassPrepareEvent) {
                    for (ArrayList<String> x : MyDebugger.bpList) {
                        if (x.get(0).equals(((ClassPrepareEvent) event).referenceType().name())) {
                            // Apply the breakpoint
                            if (x.size() == 1) {
                                addBreakpoint(x.get(0), false);
                            } else if (x.size() == 2) {
                                addBreakpoint(x.get(0), x.get(1), false);
                            } else if (x.size() == 3) {
                                addBreakpoint(x.get(0), x.get(1), x.get(2), false);
                            }
                        }
                    }
                } else if (event instanceof ClassUnloadEvent) {
                    for (ArrayList<String> x : MyDebugger.bpList) {
                        if (x.get(0).equals(((ClassUnloadEvent) event).className())) {
                            // Delete the breakpoint
                            if (x.size() == 1) {
                                deleteBreakpoint(x.get(0), false);
                            } else if (x.size() == 2) {
                                deleteBreakpoint(x.get(0), x.get(1), false);
                            } else if (x.size() == 3) {
                                deleteBreakpoint(x.get(0), x.get(1), x.get(2), false);
                            }
                        }
                    }
                }
            }
            vm.resume();
        }
    }

    private static ReferenceType findClassRef(String clazzName) {
        List<ReferenceType> allRefTypes = vm.classesByName(clazzName);
        int indexClazz = -1;
        for (int i = 0; i < allRefTypes.size(); i++) {
            if (allRefTypes.get(i).name().equals(clazzName)) {
                indexClazz = i;
                break;
            }
        }

        if (indexClazz == -1) {
            return null;
        }
        return allRefTypes.get(indexClazz);
    }

    private static List<Method> findMethodRef(String clazzName, String methodName) {
        ReferenceType clazz = findClassRef(clazzName);
        if (clazz == null)
            return null;
        List<Method> meths = clazz.methodsByName(methodName);
        if (meths.size() == 0)
            return null;
        return meths;
    }

    private static Method findOverloadRef(String clazzName, String methodName, String ol) {
        List<Method> overloads = findMethodRef(clazzName, methodName);
        if (overloads == null)
            return null;
        if (overloads.size() == 0) {
            return null;
        }
        for (Method m : overloads) {
            if (ol.equals(String.join(",", m.argumentTypeNames()))) {
                return m;
            }
        }
        return null;
    }

    private static void addBPtoList(String clazzName) {
        ArrayList<String> b = new ArrayList<String>();
        b.add(clazzName);
        bpList.add(b);
    }

    private static void removeFromList(String clazzName) {
        for (int i = 0; i < bpList.size(); i++) {
            ArrayList<String> bp = (ArrayList<String>) bpList.get(i);
            if (bp.size() == 1 && bp.get(0).equals(clazzName)) {
                bpList.remove(i);
                i--;
            }
        }
    }

    private static void addBPtoList(String clazzName, String methodName) {
        ArrayList<String> b = new ArrayList<String>();
        b.add(clazzName);
        b.add(methodName);
        bpList.add(b);
    }

    private static void removeFromList(String clazzName, String methodName) {
        for (int i = 0; i < bpList.size(); i++) {
            ArrayList<String> bp = (ArrayList<String>) bpList.get(i);
            if (bp.size() == 2 && bp.get(0).equals(clazzName) && bp.get(1).equals(methodName)) {
                bpList.remove(i);
                i--;
            }
        }
    }

    private static void addBPtoList(String clazzName, String methodName, String overload) {
        ArrayList<String> b = new ArrayList<String>();
        b.add(clazzName);
        b.add(methodName);
        b.add(overload);
        bpList.add(b);
    }

    private static void removeFromList(String clazzName, String methodName, String overload) {
        for (int i = 0; i < bpList.size(); i++) {
            ArrayList<String> bp = (ArrayList<String>) bpList.get(i);
            if (bp.size() == 3 && bp.get(0).equals(clazzName) && bp.get(1).equals(methodName) && bp.get(2).equals(overload)) {
                bpList.remove(i);
                i--;
            }
        }
    }

    public static void addBreakpoint(String clazzName, String methodName, String overload) {
        addBreakpoint(clazzName, methodName, overload, true);
    }

    public static void addBreakpoint(String clazzName, String methodName) {
        addBreakpoint(clazzName, methodName, true);
    }

    public static void addBreakpoint(String clazzName) {
        addBreakpoint(clazzName, true);
    }

    public static void addBreakpoint(String clazzName, String methodName, String overload, Boolean addToList) {
        List<Method> overloads = findMethodRef(clazzName, methodName);
        if (overloads == null)
            return;
        if (overloads.size() == 0 && addToList) {
            addBPtoList(clazzName, methodName, overload);
        } else if (overloads.size() > 0) {
            removeFromList(clazzName, methodName, overload);
        }
        for (Method m : overloads) {
            if (overload.equals(String.join(",", m.argumentTypeNames()))) {
                addBreakpoint(m);
            }
        }
        return;
    }

    public static void addBreakpoint(String clazzName, String methodName, Boolean addToList) {
        List<Method> overloads = findMethodRef(clazzName, methodName);
        if (overloads == null)
            return;
        if (overloads.size() == 0 && addToList) {
            addBPtoList(clazzName, methodName);
        } else if (overloads.size() > 0) {
            removeFromList(clazzName, methodName);
        }
        for (Method m : overloads)
            addBreakpoint(m);
        return;
    }

    public static void addBreakpoint(String clazzName, Boolean addToList) {
        ReferenceType clazz = findClassRef(clazzName);
        if (clazz == null)
            return;
        List<Method> allMeths = clazz.methods();
        if (allMeths == null)
            return;
        if (allMeths.size() == 0 && addToList) {
            addBPtoList(clazzName);
        } else if (allMeths.size() > 0) {
            removeFromList(clazzName);
        }
        for (Method m : allMeths)
            addBreakpoint(m);
        return;
    }

    private static void addBreakpoint(Method m) {
        if (m == null)
            return;
        if (breakpointRequests.containsKey(m))
            return;
        if (manager == null)
            return;
        try {
            BreakpointRequest bpReq = manager.createBreakpointRequest(m.location());
            bpReq.enable();
            breakpointRequests.put(m, bpReq);
        } catch (Exception e) {
            return;
        }
    }

    public static void deleteBreakpoint(String clazzName, String methodName, String overload) {
        deleteBreakpoint(clazzName, methodName, overload, true);
    }

    public static void deleteBreakpoint(String clazzName, String methodName) {
        deleteBreakpoint(clazzName, methodName, true);
    }

    public static void deleteBreakpoint(String clazzName) {
        deleteBreakpoint(clazzName, true);
    }

    public static void deleteBreakpoint(String clazzName, String methodName, String overload, Boolean rmList) {
        if (rmList) removeFromList(clazzName, methodName, overload);
        List<Method> overloads = findMethodRef(clazzName, methodName);
        if (overloads == null)
            return;
        for (Method m : overloads) {
            if (overload.equals(String.join(",", m.argumentTypeNames()))) {
                deleteBreakpoint(m);
            }
        }            
    }

    public static void deleteBreakpoint(String clazzName, String methodName, Boolean rmList) {
        if (rmList) removeFromList(clazzName, methodName);
        List<Method> overloads = findMethodRef(clazzName, methodName);
        if (overloads == null)
            return;
        for (Method m : overloads)
            deleteBreakpoint(m);
    }

    public static void deleteBreakpoint(String clazzName, Boolean rmList) {
        if (rmList) removeFromList(clazzName);
        ReferenceType clazz = findClassRef(clazzName);
        if (clazz == null)
            return;
        List<Method> allMeths = clazz.methods();
        if (allMeths == null)
            return;
        for (Method m : allMeths)
            deleteBreakpoint(m);
        return;
    }

    private static void deleteBreakpoint(Method m) {
        if (m == null)
            return;
        BreakpointRequest bpReq = breakpointRequests.get(m);
        if (bpReq == null)
            return;
        breakpointRequests.remove(m);
        bpReq.disable();
        manager.deleteEventRequest(bpReq);
    }

    public static void clearBreakpoints() {
        for (Method m : breakpointRequests.keySet()) {
            BreakpointRequest bpReq = breakpointRequests.get(m);
            if (bpReq == null)
                return;
            bpReq.disable();
            manager.deleteEventRequest(bpReq);
        }
        breakpointRequests.clear();
    }

    private static Method getBreakpointRequestMethod(BreakpointRequest r) {
        Method ret = null;
        for ( Map.Entry<Method, BreakpointRequest> entry : breakpointRequests.entrySet()) {
            if (entry.getValue().equals(r)) {
                ret = entry.getKey();
                break;
            }
        }
        return ret;
    }

    public static void toggleMarks() {
        MyDebugger.marksEnabled = !(MyDebugger.marksEnabled);
    }

    public static String getMarksStatus() {
        HashMap<String, Boolean> m = new HashMap<String, Boolean>();
        m.put("enabled", MyDebugger.marksEnabled);
        return javaObjectToBase64("marks_status", m);
    }

    public static String listClasses() {
        List<ReferenceType> allRefTypes = vm.allClasses();
        ArrayList<String> m = new ArrayList<String>();
        for (ReferenceType rt : allRefTypes)
            m.add(rt.name());
        String base64encoded = javaObjectToBase64("classes", m);
        return base64encoded;
    }

    public static String listMethods(String clazzName) {
        ReferenceType clazz = findClassRef(clazzName);
        if (clazz == null)
            return "e30=";
        List<Method> meths = clazz.methods();
        ArrayList<String> m = new ArrayList<String>();
        for (Method meth : meths)
            m.add(meth.returnTypeName() + " " + meth.declaringType().name() + " " + meth.name() + "(" + String.join(",", meth.argumentTypeNames()) + ")");
        String base64encoded = javaObjectToBase64("methods", m);
        return base64encoded;
    }

    public static String listOverloads(String clazzName, String methodName) {
        List<Method> meths = findMethodRef(clazzName, methodName);
        if (meths == null) {
            return "e30=";
        }
        ArrayList<String> m = new ArrayList<String>();
        for (Method meth : meths) {
            if (meth != null && meth.name().equals(methodName)) {
                m.add(meth.returnTypeName() + " " + meth.declaringType().name() + " " + meth.name() + "(" + String.join(",", meth.argumentTypeNames()) + ")");
            }
        }
        String base64encoded = javaObjectToBase64("overloads", m);
        return base64encoded;
    }

    public static String listOverloadsRaw(String clazzName, String methodName) {
        List<Method> meths = findMethodRef(clazzName, methodName);
        if (meths == null) {
            return "e30=";
        }
        ArrayList<String> m = new ArrayList<String>();
        for (Method meth : meths)
            if (meth != null && meth.name().equals(methodName)) {
                try {
                    List<Type> argst = meth.argumentTypes();
                    ArrayList<String> argsstringtypes = new ArrayList<String>();
                    for (Type t : argst) {
                        argsstringtypes.add(t.signature());
                    }
                    m.add(meth.returnTypeName() + " " + meth.declaringType().name() + " " + meth.name() + "(" + String.join(",", argsstringtypes) + ")");
                } catch (ClassNotLoadedException e) {return "e30=";}
            }
        String base64encoded = javaObjectToBase64("overloads", m);
        return base64encoded;
    }

    public static String listBreakpoints() {
        ArrayList<String> mess = new ArrayList<String>();
        for (Method m : breakpointRequests.keySet())
            mess.add(m.returnTypeName() + " " + m.declaringType().name() + " " + m.name() + "(" + String.join(",", m.argumentTypeNames()) + ")");
        String base64encoded = javaObjectToBase64("breakpoints", mess);
        return base64encoded;
    }

    public static void addMark(String _mark) {
        marks.add(_mark);
    }

    public static HashSet<String> getMarks() {
        return marks;
    }

    public static String javaObjectToBase64(String typ, Object obj) {
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put(typ, obj);
        String jsonFormatted = JSONFormatter.format(hm);
        return Base64.getEncoder().encodeToString(jsonFormatted.getBytes());
    }

    public static String javaObjectToBase64(String typ, Object obj, String t) {
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put(typ, obj);
        hm.put("time", t);
        String jsonFormatted = JSONFormatter.format(hm);
        MyDebugger.stopped = false;
        return Base64.getEncoder().encodeToString(jsonFormatted.getBytes());
    }

    public static MySession getSession(int sess_id) {
        return sessionsList.get(sess_id);
    }

    public static void sendMessage(String m) {
        socketServer.sendMessage(m);
    }

    public static void runShell(BreakpointEvent e) {
        ThreadReference tr = e.thread();
        ClassType runtimeClass, inputStreamReaderClass, bufferedReaderClass;
        ArrayType arrayClass;
        Method getRuntime, exec, getIS, inputStreamReaderConstructor, bufferedReaderConstructor, readLine, waitFor, toArray, arrayListConstructor, addArray;

        try {
            runtimeClass = (ClassType) findClassRef("java.lang.Runtime");
            arrayClass = (ArrayType) findClassRef("java.lang.String[]");
            inputStreamReaderClass = (ClassType) findClassRef("java.io.InputStreamReader");
            bufferedReaderClass = (ClassType) findClassRef("java.io.BufferedReader");
            toArray = findMethodRef("java.util.ArrayList", "toArray").get(0);
            arrayListConstructor = findOverloadRef("java.util.ArrayList", "<init>", "");
            addArray = findOverloadRef("java.util.ArrayList", "add", "java.lang.Object");
            getRuntime = findMethodRef("java.lang.Runtime", "getRuntime").get(0);
            exec = findOverloadRef("java.lang.Runtime", "exec", "java.lang.String[]");
        } catch (Exception exp) {
            System.out.println("A class is missing, cannot run shell commands");
            return;
        }
        

        while (MyDebugger.shellCmds.size() > 0) {
            String[] c = MyDebugger.shellCmds.get(0);
            try {
                final ObjectReference runtime = (ObjectReference) runtimeClass.invokeMethod(tr, getRuntime, new ArrayList<Value>(), 0);
                List<ObjectReference> argz = new ArrayList<ObjectReference>();

                ArrayReference myArray = arrayClass.newInstance(c.length);
                for (int i =0; i < c.length; i++) {
                    myArray.setValue(i, vm.mirrorOf(c[i]));
                }

                argz.add(myArray);
                ObjectReference resultProc = (ObjectReference) runtime.invokeMethod(tr, exec, argz, ObjectReference.INVOKE_SINGLE_THREADED);
                int resCode = 0;

                try {
                    waitFor = findMethodRef("java.lang.Process", "waitFor").get(0);
                    IntegerValue v = (IntegerValue) resultProc.invokeMethod(tr, waitFor, new ArrayList<Value>(), 0);
                    resCode = v.value();
                } catch (Exception exp) {
                    System.out.println("Pas pu waitFor le process :/");
                }
    
                String base64cmd = Base64.getEncoder().encodeToString(("[" + String.join(", ", c) + "] -> return code : " + Integer.toString(resCode)).getBytes());

                MyDebugger.sendMessage("{\"id\": -2, \"msg\": \"" + base64cmd + "\"}");
            } catch (Exception exc) {
                System.out.println("La commande suivante a crash : [" + String.join(", ", c) + "]");
                exc.printStackTrace();
            }
            MyDebugger.shellCmds.remove(0);
        }
    }

    public static void stopTasks() {
        MyDebugger.stopped = true;
    }
}
