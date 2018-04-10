import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server {
    // Target file for log messages to be written to
    public static File logFile = null;
    public static FileOutputStream fos = null;
    public static PrintStream ps = null;

    /*
    * Method to print to stdout, or stdout and the log File
    * if it's been defined
    */
    public static void print(String s) {
        if (logFile == null) {
            System.out.println(s);
        } else {
            System.out.println(s);
            ps.println(s);
        }
    }

    public static void main(String args[]) {
        try {
        // Port to run server on (defaults t0 8080, without root access,
        // can't use standard port 80)
        int port = 8080;

        // Root directory server should run on, defaulting to the directory
        // the server was run from
        File rootDir = new File(".");

        // Parse flags
        if (args.length > 0) {
            for (int x = 0; x < args.length; x++) {
                if (args[x].equals("-p") && x + 1 < args.length) {
                    // Set port
                    port = Integer.parseInt(args[x+1]);

                } else if (args[x].equals("-docroot") && x + 1 < args.length) {
                    // Set root directory
                    rootDir = new File(args[x+1]);

                } else if (args[x].equals("-logfile") && x + 1 < args.length) {
                    // Set log file and it's output/print streams
                    try {
                        logFile = new File(args[x+1]);
                        fos = new FileOutputStream(logFile);
                        ps = new PrintStream(fos, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.configureBlocking(false);

            ServerSocket serverSocket = channel.socket();
            serverSocket.bind(new InetSocketAddress(port));

            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_ACCEPT);
            print("Server initialized");

            while (true) {
                int readyChannels = selector.select();

                if (readyChannels == 0)
                    continue;

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyItr = selectedKeys.iterator();

                while (keyItr.hasNext()) {
                    SelectionKey key = keyItr.next();

                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        Socket newClient = serverSocket.accept();
                        SocketChannel clientChannel = newClient.getChannel();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        ByteBuffer buffer = ByteBuffer.allocate(512);
                        buffer.clear();
                        int read = 0;

                        StringBuilder message = new StringBuilder();

                        while ((read = clientChannel.read(buffer)) > 0) {
                            buffer.flip();
                            byte[] bytes = new byte[buffer.limit()];
                            buffer.get(bytes);
                            message.append(new String(bytes));
                            buffer.clear();
                        }

                        if (read < 0) {
                            print("Client " + clientChannel + " disconnected.");
                            clientChannel.close();
                        } else {
                            print("Client " + clientChannel + ":\n" + message.toString());
                        }
                    }

                    keyItr.remove();
                }
            }
        }

        if (logFile != null) {
            try {
                fos.close();
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    }
}
