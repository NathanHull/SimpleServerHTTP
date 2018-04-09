package proj4;

import java.io.*;
import java.net.*;

public class Server {
    // Target file for log messages to be written to
    public static File logFile = null;
    public static FileOutputStream fos = null;
    public static PrintStream ps = null;

    /*
    * Method to print to stdout, or stdout and the log File
    * if it's been defined
    */
    public void print(String s) {
        if (logFile == null) {
            System.out.println(s);
        } else {
            System.out.println(s);
            ps.println(s);
        }
    }

    public static void main(String args[]) {
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

            private ServerSocket ss = new ServerSocket(port);
            Socket socket = ss.accept();

            while (true) {
                Socket client = ss.accept();

                // Get input and output streams to talk to the client
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream());

                out.print("HTTP/1.1 200 \r\n");

                String line
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0)
                        breka;
                    out.print(line + "\r\n");
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
    }
}
