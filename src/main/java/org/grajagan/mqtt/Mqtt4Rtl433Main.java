package org.grajagan.mqtt;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.HelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.grajagan.util.ConsoleParser;
import org.grajagan.util.TopicalMessage;
import org.grajagan.util.TopicalMessageReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.UUID;

import static java.util.Arrays.asList;

public class Mqtt4Rtl433Main {
    public static final String DEFAULT_BROKER = "test.mosquitto.org";
    public static final String DEFAULT_CLIENT_ID = "mqtt4rtl433_client";
    public static final String DEFAULT_TOPIC = "rtl433";
    public static final Integer DEFAULT_QOS = 2;
    public static final Integer DEFAULT_PORT = 1883;
    public static final String DEFAULT_CACHE_DIR = System.getProperty("java.io.tmpdir");
    public static final String DEFAULT_LOG_FILE =
            Paths.get("application.log").toAbsolutePath().toString();

    public static void main(String[] args) throws Exception {

        OptionParser optionParser = getOptionParser();
        OptionSet optionSet = optionParser.parse(args);

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        config.getAppender("FILE").stop();
        LoggerConfig ourConfig = config.getLoggerConfig("org.grajagan");
        ourConfig.removeAppender("FILE");

        File f = new File(DEFAULT_LOG_FILE);
        if (f.exists() && f.length() == 0) {
            f.delete();
        }

        PatternLayout patternLayout = PatternLayout.newBuilder().withConfiguration(config)
                .withPattern("%d{dd MMM yyyy HH:mm:ss} - %-5p (%F:%L): %m%n").build();

        if (optionSet.has("logfile")) {
            String logfile = (String) optionSet.valueOf("logfile");
            String path = FilenameUtils.getPath(logfile);
            String name = FilenameUtils.getBaseName(logfile);
            String ext = FilenameUtils.getExtension(logfile);

            String pattern = path + name + ".%d{yyyy-MM-dd}." + ext + ".gz";

            RollingFileAppender appender =
                    RollingFileAppender.newBuilder().setName("FILE").withFileName(logfile)
                            .withFilePattern(pattern).setLayout(patternLayout)
                            .setIgnoreExceptions(false)
                            .withPolicy(TimeBasedTriggeringPolicy.newBuilder().build())
                            .withStrategy(DefaultRolloverStrategy.newBuilder().build()).build();

            appender.start();
            ourConfig.addAppender(appender, optionSet.has("debug") ? Level.DEBUG : Level.INFO,
                    null);
        }

        boolean isDebug = optionSet.has("debug");
        if (!isDebug) {
            config.getRootLogger().setLevel(Level.INFO);
            ourConfig.setLevel(Level.INFO);
        }

        if (optionSet.has("quiet")) {
            ourConfig.removeAppender("CONSOLE");
            config.getRootLogger().removeAppender("CONSOLE");
            ConsoleAppender consoleAppender =
                    ConsoleAppender.newBuilder().setName("CONSOLE").setLayout(patternLayout)
                            .setTarget(ConsoleAppender.Target.SYSTEM_OUT).build();
            consoleAppender.start();

            config.getRootLogger().addAppender(consoleAppender, Level.ERROR, null);
            ourConfig.addAppender(consoleAppender, Level.ERROR, null);
        }

        ctx.updateLoggers();

        final Logger log = LogManager.getLogger(Mqtt4Rtl433Main.class);

        if (optionSet.has("help")) {
            optionParser.printHelpOn(System.err);
            return;
        }

        if (optionSet.has("file")) {
            try {
                FileInputStream is =
                        new FileInputStream(new File((String) optionSet.valueOf("file")));
                System.setIn(is);
            } catch (IOException e) {
                log.error("Cannot redirect STDIN!", e);
                System.exit(1);
            }
        }
        int port = (Integer) optionSet.valueOf("port");
        int qos = (Integer) optionSet.valueOf("q");
        boolean cleanSession = optionSet.has("clean");
        final String topic;
        final String broker;
        String clientId = (String) optionSet.valueOf("id");
        clientId = clientId.replace("-<UUID>", "-" + UUID.randomUUID());

        final String userName;
        final String password;
        String tmpDir = (String) optionSet.valueOf("dir");

        // Validate the provided arguments
        if (qos < 0 || qos > 2) {
            System.err.println("Invalid QoS: " + qos + "\n\n");
            optionParser.printHelpOn(System.err);
            return;
        }

        String url;

        if (optionSet.has("L")) {
            String s = "";
            try {
                s = (String) optionSet.valueOf("L");
                URI uri = new URI(s);
                port = uri.getPort();
                if (port <= 0) {
                    port = DEFAULT_PORT;
                }
                broker = uri.getHost();
                url = "tcp://" + broker + ":" + port;

                String[] userInfo = uri.getUserInfo().split(":");
                if (userInfo.length > 0) {
                    userName = userInfo[0];
                } else {
                    userName = null;
                }

                if (userInfo.length > 1) {
                    password = userInfo[1];
                } else {
                    password = null;
                }

                topic = uri.getPath().replace("/", "");
            } catch (URISyntaxException e) {
                System.err.println("\"" + s + "\" is not a valid URL\n\n");
                optionParser.printHelpOn(System.err);
                return;
            }
        } else {
            broker = (String) optionSet.valueOf("host");
            url = "tcp://" + broker + ":" + port;
            userName = (String) optionSet.valueOf("user");
            password = (String) optionSet.valueOf("password");
            topic = (String) optionSet.valueOf("topic");
        }

        log.info("Connecting to " + url + "/" + topic + " as " + clientId + " using QOS " + qos
                + " with cleanSession set to " + cleanSession);

        AsyncClient client = new AsyncClient(url, clientId, qos, tmpDir, topic);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.debug("The connection has been lost!");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                log.warn("We haven't subscribed but did receive a message! Weird!!");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                log.debug("Message " + token.getMessageId() + " has been delivered!");
            }
        });

        IMqttToken token = null;
        try {
            token = client.connect(userName, password, cleanSession);
            final IMqttToken t = token;
            Thread monitor = new Thread(() -> {
                try {
                    while (!t.isComplete()) {
                        t.waitForCompletion(1000);
                        log.debug("Connection still pending...");
                    }
                    log.debug("Successfully connected!");
                } catch (MqttSecurityException e) {
                    log.error("Unable to connect to " + broker + " as " + userName + " using "
                            + " password \"" + password + "\"");
                    System.exit(1);
                } catch (MqttException e) {
                    if (e.getCause() != null && e.getCause() instanceof UnknownHostException) {
                        log.error("Unknown host: \"" + broker + "\"");
                        System.exit(1);
                    } else {
                        log.error("Connection did not complete!", e);
                    }
                }
            });
            monitor.start();
        } catch (MqttException e) {
            log.error("Connection failed!", e);
            System.exit(1);
        }

        ConsoleParser parser = new ConsoleParser(client, (TopicalMessage message) -> {
            if (isDebug) {
                log.debug(topic + "/" + message);
            }
        });

        log.debug("Starting parser!");
        parser.start();

        log.debug("Emptying queue and disconnecting!");
        try {
            if (token != null) {
                token.waitForCompletion(5000);
            }

            int n = 0;
            while (client.getBufferedMessageCount() > 0 && n < 5) {
                Thread.sleep(1000);
                n++;
            }

            if (client.isConnected()) {
                client.disconnect(5000).waitForCompletion();
            }
        } catch (Exception e) {
            log.warn("Could not disconnect cleanly!", e);
        } finally {
            client.close();
        }

        log.debug("Finished!");
    }

    private static OptionParser getOptionParser() {

        OptionParser parser = new OptionParser();
        HelpFormatter formatter = new BuiltinHelpFormatter(200, 2);
        parser.formatHelpWith(formatter);

        parser.acceptsAll(asList("c", "clean"), "connect using a 'clean session'.");
        parser.acceptsAll(asList("D", "dir"), "directory for inflight storage").withRequiredArg()
                .defaultsTo(DEFAULT_CACHE_DIR);
        parser.acceptsAll(asList("d", "debug"), "enable debug messages.");
        parser.acceptsAll(asList("f", "file"), "read messages from file instead of STDIN")
                .withRequiredArg();
        parser.accepts("help", "print this help text and quit").forHelp();
        parser.acceptsAll(asList("h", "host"), "mqtt host to connect to.").withRequiredArg()
                .defaultsTo(DEFAULT_BROKER);
        parser.acceptsAll(asList("i", "id"), "id to use for this client.").withRequiredArg()
                .defaultsTo(DEFAULT_CLIENT_ID + "-<UUID>");
        parser.accepts("L", "specify connection URL in the form of "
                + "tcp://[username[:password]@]host[:port]/topic").withRequiredArg();

        parser.acceptsAll(asList("l", "logfile"), "log to this file.").withOptionalArg()
                .defaultsTo(DEFAULT_LOG_FILE);
        parser.acceptsAll(asList("P", "password"), "provide a password").withRequiredArg();
        parser.acceptsAll(asList("p", "port"), "network port to connect to.").withRequiredArg()
                .ofType(Integer.class).defaultsTo(DEFAULT_PORT);
        parser.accepts("q", "quality of service level to use for all messages.").withRequiredArg()
                .ofType(Integer.class).defaultsTo(DEFAULT_QOS);
        parser.accepts("quiet", "do not print any messages to the console except for errors.");
        parser.acceptsAll(asList("t", "topic"), "mqtt topic to publish below").withRequiredArg()
                .defaultsTo(DEFAULT_TOPIC);
        parser.acceptsAll(asList("u", "user"), "provide a username").withRequiredArg();
        return parser;
    }
}
