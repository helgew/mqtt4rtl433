package org.grajagan.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static java.util.Arrays.asList;

public class ConsoleParser {

    private static final Logger log = LogManager.getLogger(ConsoleParser.class);

    private final List<TopicalMessageReceiver> receivers;

    private final Map<String, LocalDateTime> lastDataMap = new HashMap<>();
    private static final DateTimeFormatter PATTERN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ConsoleParser(TopicalMessageReceiver... receivers) {
        this.receivers = asList(receivers);
    }

    public void start() {
        Scanner console = new Scanner(System.in);
        log.debug("Parsing input!");
        while (console.hasNextLine()) {
            String message = console.nextLine();
            TopicalMessage topicalMessage = parse(message);
            if (topicalMessage != null) {
                for (TopicalMessageReceiver receiver : receivers) {
                    receiver.consume(topicalMessage);
                }
            }
        }
        log.debug("Finished parsing!");
    }

    public TopicalMessage parse(String message) {
        JSONObject jo;
        LocalDateTime time = null;
        LocalDateTime prevTime;
        String model = "unknown";

        try {
            jo = new JSONObject(message);
            model = jo.getString("model");
            time = LocalDateTime.from(PATTERN.parse(jo.getString("time")));
        } catch (JSONException e) {
            log.debug("Message did not contain valid JSON and/or \"model\" and \"time\" "
                    + "parameters. Converting to JSON.");
            log.trace("Caught exception: ", e);

            jo = new JSONObject();
            jo.put("message", message);
            message = jo.toString();
        }

        prevTime = lastDataMap.get(model);
        if (prevTime != null && time != null && prevTime
                .isAfter(time.minus(2, ChronoUnit.SECONDS))) {
            log.debug("Skipping duplicate message!");
            return null;
        }

        lastDataMap.put(model, time);

        TopicalMessage topicalMessage = new TopicalMessage();
        topicalMessage.setTopic(model.replaceAll(" ", "_"));
        topicalMessage.setMessage(message);

        return topicalMessage;
    }
}