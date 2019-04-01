package org.grajagan.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.grajagan.util.ConsoleParser;

public class Mqtt4Rtl433Main {
    private static final Logger log = LogManager.getLogger(Mqtt4Rtl433Main.class);

    public static final String DEFAULT_BROKER = "m2m.eclipse.org";
    public static final String DEFAULT_CLIENT_ID = "mqtt4rtl433_client";
    public static final String DEFAULT_TOPIC = "rtl433";

    IMqttDeliveryToken token = null;

    public static void main(String[] args) {

        // Default settings:
        String topic = "rtl433";
        int qos = 2;
        String broker = DEFAULT_BROKER;
        int port = 1883;
        String clientId = null;
        boolean cleanSession = true;
        String password = null;
        String userName = null;
        String tmpDir = System.getProperty("java.io.tmpdir");

        // TODO: replace with option parser
        for (int i = 0; i < args.length; i++) {
            // Check this is a valid argument
            if (args[i].length() == 2 && args[i].startsWith("-")) {
                char arg = args[i].charAt(1);
                // Handle arguments that take no-value
                switch (arg) {
                    case 'h':
                    case '?':
                        printHelp();
                        return;
                }

                if (i == args.length - 1 || args[i + 1].charAt(0) == '-') {
                    System.err.println("Missing value for argument: " + args[i]);
                    printHelp();
                    return;
                }
                switch (arg) {
                    case 't':
                        topic = args[++i];
                        break;
                    case 'd':
                        tmpDir = args[++i];
                    case 's':
                        qos = Integer.parseInt(args[++i]);
                        break;
                    case 'b':
                        broker = args[++i];
                        break;
                    case 'p':
                        port = Integer.parseInt(args[++i]);
                        break;
                    case 'i':
                        clientId = args[++i];
                        break;
                    case 'c':
                        cleanSession = Boolean.valueOf(args[++i]);
                        break;
                    case 'u':
                        userName = args[++i];
                        break;
                    case 'P':
                        password = args[++i];
                        break;
                    default:
                        System.out.println("Unrecognised argument: " + args[i]);
                        printHelp();
                        return;
                }
            } else {
                System.err.println("Unrecognised argument: " + args[i]);
                printHelp();
                return;
            }
        }

        // Validate the provided arguments
        if (qos < 0 || qos > 2) {
            System.err.println("Invalid QoS: " + qos);
            printHelp();
            return;
        }

        String protocol = "tcp://";

        String url = protocol + broker + ":" + port;

        if (clientId == null || clientId.equals("")) {
            clientId = DEFAULT_CLIENT_ID;
        }

        AsyncClient client =
                new AsyncClient(url, clientId, cleanSession, qos, userName, password, tmpDir,
                        topic);
        Runnable parser = new ConsoleParser(client);
        Thread parserThread = new Thread(parser);

        log.trace("Starting parser thread!");
        parserThread.start();
    }

    private static void printHelp() {
        System.err.println("Syntax:\n\n"
                + "    AsyncClient [-h] [-t <topic>] [-s 0|1|2] -b <hostname|IP address>]\n"
                + "            [-p <brokerport>] [-i <clientID>] [-d <directory>]\n\n"
                + "    -h  Print this help text and quit\n"
                + "    -t  Publish below the <topic> instead of the default \"" + DEFAULT_TOPIC
                + "\"\n" + "    -s  Use this QoS instead of the default (2)\n"
                + "    -b  Use this name/IP address instead of the default \"" + DEFAULT_BROKER
                + "\"\n" + "    -p  Use this port instead of the default (1883)\n\n"
                + "    -i  Use this client ID instead of \"" + DEFAULT_CLIENT_ID + "\"\n"
                + "    -d  Use this directory for inflight storage instead of the system's temp. dir\n"
                + "    -c  Connect to the server with a clean session (default is false)\n"
                + "     \n\n Security Options \n" + "     -u Username \n" + "     -P Password \n\n"
                + "Delimit strings containing spaces with \"\"\n\n");
    }
}
