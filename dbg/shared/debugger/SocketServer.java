import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.lang.String;
import java.util.concurrent.Semaphore;
import java.nio.channels.ServerSocketChannel;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;
import java.nio.channels.SocketChannel;


public class SocketServer {

    private String address_j2p;
    private String address_p2j;
    private SocketChannel channel_j2p;
    private SocketChannel channel_p2j;
    private ServerSocketChannel serverChannel_j2p;
    private ServerSocketChannel serverChannel_p2j;
    private MessagePool mPool;
    private Semaphore semSocketConnected_j2p;
    private Semaphore semSocketConnected_p2j;
    private LinkedBlockingQueue<String> packets;


    public SocketServer(String address_j2p, String address_p2j) {
        semSocketConnected_j2p = new Semaphore(0);
        semSocketConnected_p2j = new Semaphore(0);
        this.address_j2p = address_j2p;
        this.address_p2j = address_p2j;
        Path socketFile_j2p = Paths.get(address_j2p);
        Path socketFile_p2j = Paths.get(address_p2j);
        UnixDomainSocketAddress udsaddress_j2p;
        UnixDomainSocketAddress udsaddress_p2j;
        try {
            Files.deleteIfExists(socketFile_j2p);
            Files.deleteIfExists(socketFile_p2j);
            udsaddress_j2p = UnixDomainSocketAddress.of(socketFile_j2p);
            udsaddress_p2j = UnixDomainSocketAddress.of(socketFile_p2j);
            serverChannel_j2p = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel_p2j = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel_j2p.bind(udsaddress_j2p);
            serverChannel_p2j.bind(udsaddress_p2j);
        } catch (Exception e) {
            return;
        }

        this.mPool = new MessagePool(this);
        this.packets = new LinkedBlockingQueue<String>();

        new Thread(() -> {
            this.acceptClientJ2P();
        }).start();
        new Thread(() -> {
            this.acceptClientP2J();
        }).start();
        new Thread(() -> {
            this.streamToPacketsLoop();
        }).start();
        new Thread(() -> {
            this.packetsToMessageLoop();
        }).start();
    }

    private void acceptClientJ2P() {
        while (true) {
            try {
                this.channel_j2p = this.serverChannel_j2p.accept();
                semSocketConnected_j2p.release();
            } catch (Exception e) {

            }
        }
    }

    private void acceptClientP2J() {
        while (true) {
            try {
                this.channel_p2j = this.serverChannel_p2j.accept();
            } catch (Exception e) {

            }
            semSocketConnected_p2j.release();
        }
    }

    private void packetsToMessageLoop() {
        while (true) {
            try {
                String message = this.packets.take();
                Message m = new Message(message);
                this.mPool.add(m);
            } catch (Exception e) {

            }
        }
    }
    
    private void streamToPacketsLoop() {
        String infiniteBuffer = "";
        try {
            semSocketConnected_p2j.acquire(); // for first connection
        } catch (Exception e) {
            return;
        }
        while (true) {
            String r = readSocket();
            if (r != null) {
                infiniteBuffer = infiniteBuffer + r;
                String[] arr = extractPacket(infiniteBuffer);
                while (arr != null) {
                    String p = arr[0];
                    infiniteBuffer = arr[1];
                    packets.add(p);
                    arr = extractPacket(infiniteBuffer);
                }
            }
        }

    }

    private String readSocket() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            int bytesRead = this.channel_p2j.read(buffer);
            String retval = null;
            if (bytesRead > 0) {
                // We get the message
                byte[] bytes = new byte[bytesRead];
                buffer.flip();
                buffer.get(bytes);
                retval = new String(bytes);
            }
            return retval;
        } catch (Exception e) {
            return "";
        }
    }

    private String[] extractPacket(String s) {
        // modify s in place
        // check next packet's size
        String[] retval = new String[2];
        int numLength = s.indexOf("_");
        if (numLength < 1) {
            return null;
        }
        int packLength = Integer.parseInt(s.substring(0, numLength));
        if (s.length() >= numLength + packLength + 1) {
            retval[0] = s.substring(numLength+1, numLength+packLength+1);
            retval[1] = s.substring(numLength+packLength+1);
            return retval;
        } else {
            return null;
        }
    }

    public void sendMessage(String message) {
        String newMess = String.valueOf(message.length()) + "_" + message;
        int step = 1000;
        String messChunk = newMess;
        for (int i=0; i < newMess.length(); i = i+step) {
            int upper = i+step;
            if (upper > newMess.length()) upper = newMess.length();
            messChunk = newMess.substring(i, upper);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            buffer.put(messChunk.getBytes());
            buffer.flip();
            try {
                while(buffer.hasRemaining()) {
                    this.channel_j2p.write(buffer);
                }
            } catch (Exception e) {
                return;
            }
        }
    }
}