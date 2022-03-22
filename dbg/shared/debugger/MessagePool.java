import java.util.HashMap;
import java.util.concurrent.Semaphore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.lang.Thread.State;


public class MessagePool {

    private ArrayList<Message> mList;
    private Semaphore sem;
    private SocketServer server;

    public MessagePool(SocketServer s) {
        this.mList = new ArrayList<Message>();
        this.sem = new Semaphore(0);
        this.server = s;
        this.responseLoop();
    }

    public void add(Message m) {
        m.attachSemaphore(this.sem);
        mList.add(m);
        m.handle();
    }

    private void responseLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    this.sem.acquire();
                    // Check if processing is done
                    int i = 0;
                    while (i < this.mList.size()) {
                        if (this.mList.get(i).finished && this.mList.get(i).returnQueue.isEmpty()) {
                            this.mList.remove(i);
                        } else {
                            i++;
                        }
                    }
                    // Check if queue is empty for the remaining ones
                    for (Message m : this.mList) {
                        if (!m.returnQueue.isEmpty()) {
                            // Format message
                            ObjectMapper mapper = new ObjectMapper();
                            HashMap<String, Object> hm = new HashMap<String, Object>();
                            hm.put("id", m.id);
                            hm.put("msg", m.returnQueue.remove());
                            String msg = "";
                            try {
                                msg = mapper.writeValueAsString(hm);
                            } catch (Exception e) {
                                continue;
                            }
    
                            // Send message
                            this.server.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void killMessages() {
        for (Message m : this.mList) {
            if (m.handleThread != null) {
                if (m.handleThread.isAlive()) {
                    // TODO Kill the thread
                }
            }
        }
    }
    
}
