import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.*;
import java.util.*;
import java.text.*;

public class Server {
    // Date format to put in responses
    public static DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM YYYY HH:mm:ss zzz");

    // Target file for log messages to be written to
    public static File logFile = null;
    public static FileOutputStream fos = null;
    public static PrintStream ps = null;

    /**
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

    /**
    * Return 501 response string
    */
    public static String get501String() {
        StringBuilder result = new StringBuilder();
        Date date = new Date();

        result.append("\r\nHTTP/1.1 Status-Code 501: Not Implemented\r\n");
        result.append("Date: ");
        result.append(dateFormat.format(date));
        result.append("\r\n");
        result.append("Content-Type: html");
        result.append("\r\n");
        result.append("Content-Length: 208");
        result.append("\r\n");

        print("\nResponse sent:\n" + result.toString());

        result.append("\r\n");
        result.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>501 Not Implemented</title></head><body><h1>Not Implemented</h1><p>The requested OP was not implemented on this server.</p></body></html>");
        result.append("\r\n");

        return result.toString();
    }

    /**
    * Return 404 response string
    */
    public static String get404String() {
        StringBuilder result = new StringBuilder();
        Date date = new Date();

        result.append("HTTP/1.1 Status-Code 404: Not Found\r\n");
        result.append("Date: ");
        result.append(dateFormat.format(date));
        result.append("\r\n");
        result.append("Content-Type: html");
        result.append("\r\n");
        result.append("Content-Length: 190");
        result.append("\r\n");

        print("\nResponse sent:\n" + result.toString());

        result.append("\r\n");
        result.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>404 Not Found</title></head><body><h1>Not Found</h1><p>The requested URL was not found on this server.</p></body></html>");
        result.append("\n");

        return result.toString();
    }

    /**
    * Return 304 response string
    */
    public static String get304String() {
        StringBuilder result = new StringBuilder();
        Date date = new Date();

        result.append("HTTP/1.1 Status-Code 304: Not Modified\r\n");
        result.append("Date: ");
        result.append(dateFormat.format(date));
        result.append("\r\n");

        print("\nResponse sent:\n" + result.toString());

        result.append("\n");

        return result.toString();
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
                    print("File set to " + rootDir.getName());

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
        }

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.bind(new InetSocketAddress(port));

        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        print("Server initialized\n");

        // Create shutdown hook to close channels/buffers
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutting down");

                if (logFile != null) {
                    try {
                        fos.close();
                        ps.close();
                        serverSocketChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        while (true) {
            int readyChannels = selector.select(2000);

            if (readyChannels == 0) {
                // If there are more keys than 1 (the server channel),
                // those channels have timed out, close all of them except
                // the server
                if (selector.keys().size() > 1) {
                    for (SelectionKey key : selector.keys()) {
                        if (key.channel() != serverSocketChannel)
                            key.channel().close();
                    }
                }
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyItr = selectedKeys.iterator();

            while (keyItr.hasNext()) {
                SelectionKey key = keyItr.next();

                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

                    SocketChannel clientChannel = serverSocketChannel.accept();

                    if (clientChannel == null)
                        continue;

                    clientChannel.configureBlocking(false);
                    clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                    clientChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                    SocketChannel clientChannel = (SocketChannel) key.channel();

                    ByteBuffer recvBuffer = ByteBuffer.allocate(1024);
                    recvBuffer.clear();
                    int read = 0;

                    StringBuilder messageBuilder = new StringBuilder();

                    while ((read = clientChannel.read(recvBuffer)) > 0) {
                        recvBuffer.flip();
                        byte[] bytes = new byte[recvBuffer.limit()];
                        recvBuffer.get(bytes);
                        messageBuilder.append(new String(bytes));
                        recvBuffer.clear();
                    }

                    if (read < 0) {
                        clientChannel.close();
                        continue;
                    } else {
                        String message = messageBuilder.toString();
                        print("Request from Client " + clientChannel.getRemoteAddress() + ":\n" + message.toString());

                        String[] lines = message.split("\n");
                        String response = "";
                        boolean closeAfter = false;

                        byte[] data = null;

                        if (!lines[0].substring(0, 3).toUpperCase().equals("GET")) {
                            response = get501String();
                            //closeAfter = true;
                        } else {
                            String getTokens[] = lines[0].split(" ");
                            String resource = getTokens[1].substring(1).toLowerCase();
                            print("Searching for file " + resource);
                            boolean foundFile = false;
                            Date ifModifiedSince = null;

                            // Scan through headers, find important ones
                            for (String line : lines) {
                                String lineTokens[] = line.split(" ");

                                // Get ifModifiedSince date if exists
                                if (lineTokens[0].toLowerCase().equals("if-modified-since:")) {
                                    try {
                                        ifModifiedSince = dateFormat.parse(line.substring(19));
                                    } catch (ParseException e) {
                                        print("If-modified-since date parse exception");
                                        ifModifiedSince = null;
                                    }
                                    print("If-modified-since date found: " + ifModifiedSince);
                                }

                                if (lineTokens[0].toLowerCase().equals("connection:")) {
                                    if (lineTokens[1].toLowerCase().equals("close")) {
                                        closeAfter = true;
                                    }
                                }
                            }

                            // Check if file exists in this or subdirectories
                            File fileList[] = rootDir.listFiles();
                            for (File file : fileList) {

                                if (!file.isDirectory() && file.getName().toLowerCase().equals(resource)) {

                                    print("File found");
                                    foundFile = true;

                                    // Check if it's been modified since
                                    if (ifModifiedSince != null && ifModifiedSince.compareTo(new Date(file.lastModified())) < 0) {

                                        // If file hasn't been modified since...
                                        print("File has not been modified since");
                                        data = null;
                                        response = get304String();

                                    // Or send the file with 200
                                    } else {

                                        print("Building file packet");
                                        StringBuilder result = new StringBuilder();
                                        Date date = new Date();

                                        result.append("\r\nHTTP/1.1 200 OK\r\n");

                                        result.append("Date: ");
                                        result.append(dateFormat.format(date));
                                        result.append("\r\n");

                                        result.append("Last-Modified: ");
                                        result.append(dateFormat.format(new Date(file.lastModified())));
                                        result.append("\r\n");

                                        result.append("Content-Type: ");
                                        result.append(file.getName().substring(file.getName().lastIndexOf('.') + 1));
                                        result.append("\r\n");

                                        result.append("Content-Length: ");
                                        result.append(file.length());
                                        result.append("\r\n");

                                        print("Response sent:\n");
                                        print(result.toString());

                                        result.append("\r\n");

                                        // Get file bytes
                                        data = new byte[(int) file.length()];
                                        try {
                                            FileInputStream fileInputStream = new FileInputStream(file);
                                            fileInputStream.read(data);
                                        } catch (FileNotFoundException e) {
                                            print("File Not Found.");
                                        }

                                        response = result.toString();

                                    }

                                    break;
                                }
                            }

                            if (!foundFile) {
                                print("File not found");
                                response = get404String();
                            }

                        }

                        ByteBuffer buffer = ByteBuffer.allocate(500000);
                        buffer.clear();
                        buffer.put(response.getBytes());

                        if (data != null) {
                            buffer.put(data);
                        }

                        buffer.flip();

                        while (buffer.hasRemaining()) {
                            clientChannel.write(buffer);
                        }

                        buffer.clear();

                        if (closeAfter) {
                            clientChannel.close();
                        }
                    }

                }

                keyItr.remove();
            }
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
    }
}
