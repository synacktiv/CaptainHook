import java.lang.Thread;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.InternalException;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.lang.InterruptedException;
import java.lang.System;
import java.lang.Runnable;


public class BreakpointHandler implements Runnable {

    private BreakpointEvent event;
    private Semaphore sem;
    private Method m;
    private static Object syncObj = new Object();


    public BreakpointHandler(BreakpointEvent ev, Semaphore _sem, Method _m) {
        event = ev;
        sem = _sem;
        m = _m;
    }

    public void run() {
        if (MyDebugger.shellCmds.size() > 0) {
            List<BreakpointRequest> bpReqs = MyDebugger.manager.breakpointRequests();
            ArrayList<BreakpointRequest> disabledTemp = new ArrayList<BreakpointRequest>();
            for (BreakpointRequest br : bpReqs) {
                if ( "java.lang.Runtime".equals(br.location().declaringType().name()) && br.isEnabled()) {
                    br.disable();
                    disabledTemp.add(br);
                }
            }
            MyDebugger.runShell(event);
            for (BreakpointRequest br : disabledTemp) {
                br.enable();
            }
        }
        int id = 0;
        MyStack stackInfos = new MyStack(m);
        MySession sess;
        boolean interesting = false;
        HashMap<String, String> marks = new HashMap<String, String>();
        String base64encoded = "";
        try {
            List<StackFrame> stack = new ArrayList<StackFrame>();
            try {
                stack = event.thread().frames();
            } catch (Exception e) {
                stackInfos.setBroken();
                e.printStackTrace();
            }
            
            // Generate our own stack
            if (!stackInfos.isBroken()) {
                for (int i = 0; i < stack.size(); i++)
                    stackInfos.pushFrame(new MyStackFrame(stack.get(i), i));
            }

            // VM can resume
            sem.release();

            for (int i = 0; i < stackInfos.getFrame(0).getArgc(); i++) {
                String m = stackInfos.getFrame(0).getArg(i).checkMarks(MyDebugger.getMarks());
                if (m != null) {
                    marks.put(String.valueOf(i), m);
                }
            }

            for (int i = 0; i < stackInfos.getFramesCount(); i++) {
                for (String m : MyDebugger.getMarks()) {
                    boolean b = stackInfos.getFrame(i).getTitle().contains(m);
                    b = b || stackInfos.getFrame(i).getTitle().contains(m.toUpperCase());
                    b = b || stackInfos.getFrame(i).getTitle().contains(m.toLowerCase());
                    b = b || stackInfos.getFrame(i).getTitle().contains(Base64.getEncoder().encodeToString(m.getBytes()));
                    b = b || stackInfos.getFrame(i).getTitle().contains(new String(Base64.getDecoder().decode(m)));
                    if (b) {
                        marks.put(String.valueOf(0 - i), m);
                    }
                }
            }

            interesting = (marks.size() > 0) || !(MyDebugger.marksEnabled);
            
            if (interesting) {
                // Save it in session ArrayList
                synchronized (syncObj) {
                    System.out.println("Session interessante");
                    id = MyDebugger.sessionIdCounter++;
                    System.out.println(id);
                    sess = new MySession(stackInfos, id, marks);
                    MyDebugger.sessionsList.add(sess);
                }
                base64encoded = MyDebugger.javaObjectToBase64("stacktrace", sess.toNotif(), stackInfos.getTime());
            }

            
            
        } catch (Exception e) {
            System.out.println("Unhandled exception during stack analysis");
            e.printStackTrace();
        }
        
        if (interesting) {
            MyDebugger.sendMessage("{\"id\": -1, \"msg\": \"" + base64encoded + "\"}");

        }
    }
}
