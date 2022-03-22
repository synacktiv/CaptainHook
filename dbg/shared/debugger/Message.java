import java.util.concurrent.Semaphore;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.ArrayList;


public class Message {
    public int id;
    private String cmd;
    private ArrayList<Object> args;
    public ArrayDeque<String> returnQueue;
    private Semaphore parentSem;
    public boolean finished;
    public Thread handleThread = null;

    public Message(String m) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(m, Map.class);
            this.id = (Integer) map.get("id");
            this.cmd = (String) map.get("cmd");
            this.args = new ArrayList<Object>();
            for (String s : (ArrayList<String>) map.get("args")) {
                ObjectMapper mapper_ = new ObjectMapper();
                Map<String, Object> map_ = mapper.readValue(s, Map.class);
                this.args.add(map_.get("val"));
        }
        } catch (Exception e) {
            return;
        }
        
        this.returnQueue = new ArrayDeque<String>();
        this.finished = false;
    }

    public Message(int id, String command, ArrayList<String> args) {
        this.id = id;
        this.cmd = command;
        this.args = new ArrayList<Object>();
        for (String s : args) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(s, Map.class);
                this.args.add(map.get("val"));
            } catch (Exception e) {
                return;
            }
        }
        this.returnQueue = new ArrayDeque<String>();
        this.finished = false;
    }

    public void attachSemaphore(Semaphore s) {
        this.parentSem = s;
    }

    private void addToQueue(String s) {
        // Adds to returnQueue and signals to parent
        this.returnQueue.add(s);
        this.signalParent();
    }

    private void signalParent() {
        // Signals the end of the message handling to the parent
        this.parentSem.release();
    }

    private void handle_sync() {
        switch (this.cmd) {
            case "clear_breakpoints" -> {
                MyDebugger.clearBreakpoints();
                addToQueue("Breakpoints cleared");
            }
            case "list_classes" -> {
                addToQueue(MyDebugger.listClasses());
            }
            case "list_methods" -> {
                String result = MyDebugger.listMethods((String) this.args.get(0));
                addToQueue(result);
            }
            case "list_overloads" -> {
                String result = MyDebugger.listOverloads((String) this.args.get(0), (String) this.args.get(1));
                addToQueue(result);
            }
            case "list_overloads_raw" -> {
                String result = MyDebugger.listOverloadsRaw((String) this.args.get(0), (String) this.args.get(1));
                addToQueue(result);
            }
            case "list_breakpoints" -> {
                String result = MyDebugger.listBreakpoints();
                addToQueue(result);
            }
            case "add_breakpoint" -> {
                // Overload, method & class
                switch (this.args.size()) {
                    case 1 -> {
                        MyDebugger.addBreakpoint((String) this.args.get(0));
                    }
                    case 2 -> {
                        MyDebugger.addBreakpoint((String) this.args.get(0), (String) this.args.get(1));
                    }
                    case 3 -> {
                        MyDebugger.addBreakpoint((String) this.args.get(0), (String) this.args.get(1), (String) this.args.get(2));
                    }
                }
            }
            case "delete_breakpoint" -> {
                // Overload, method & class
                switch (this.args.size()) {
                    case 1 -> {
                        MyDebugger.deleteBreakpoint((String) this.args.get(0));
                    }
                    case 2 -> {
                        MyDebugger.deleteBreakpoint((String) this.args.get(0), (String) this.args.get(1));
                    }
                    case 3 -> {
                        MyDebugger.deleteBreakpoint((String) this.args.get(0), (String) this.args.get(1), (String) this.args.get(2));
                    }
                }
            }
            case "set_stack_inspection_strategy" -> {
                MyDebugger.getSession((Integer) this.args.get(0)).setStackInspectionStrategy((String) this.args.get(1));
            }
            case "set_max_inspection_depth" -> {
                MyDebugger.getSession((Integer) this.args.get(0)).setMaxInspectionDepth((Integer) this.args.get(1));
            }
            case "set_object_inspection_strategy" -> {
                MyDebugger.getSession((Integer) this.args.get(0)).setObjectInspectionStrategy((String) this.args.get(1));
            }
            case "get_stack_inspection_strategy" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).getStackInspectionStrategy();
                addToQueue(result);
            }
            case "get_max_inspection_depth" -> {
                String x = MyDebugger.getSession((Integer) this.args.get(0)).getStackInspectionStrategy();
                addToQueue(x);
            }
            case "get_object_inspection_strategy" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).getObjectInspectionStrategy();
                addToQueue(result);
            }
            case "dump_stack" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).dump();
                addToQueue(result);
            }
            case "list_stackframes" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).listStackframes();
                addToQueue(result);
            }
            case "inspect_stackframe" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).inspectStackframe((Integer) this.args.get(1));
                addToQueue(result);
            }
            case "inspect_argument" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).inspectArgument((Integer) this.args.get(1), (Integer) this.args.get(2));
                addToQueue(result);
            }
            case "stop_tasks" -> {
                MyDebugger.stopTasks();
            }
            case "toggle_marks" -> {
                MyDebugger.toggleMarks();
                addToQueue("e30=");
            }
            case "get_marks_status" -> {
                addToQueue(MyDebugger.getMarksStatus());
            }
            case "list_marks" -> {
                String result = MyDebugger.getSession((Integer) this.args.get(0)).listMarks();
                addToQueue(result);
            }
            case "add_mark" -> {
                MyDebugger.addMark((String) this.args.get(0));
            }
            case "find_frst_mark_occurence" -> {
                int x = MyDebugger.getSession((Integer) this.args.get(0)).findFirstMarkOccurence();
                addToQueue(String.valueOf(x));
            }
            case "add_shell_cmd" -> {
                Object[] tmpArr = this.args.toArray();
                String[] strArr = new String[tmpArr.length];
                for (int i = 0; i < tmpArr.length; i++) {
                    strArr[i] = (String) tmpArr[i];
                }
                MyDebugger.shellCmds.add(strArr);
                addToQueue("e30=");
            }
        };
    }

    public void handle() {
        handleThread = new Thread(() -> {
            this.handle_sync();
            this.finished = true;
            signalParent();
        });
        handleThread.start();
    }
}
