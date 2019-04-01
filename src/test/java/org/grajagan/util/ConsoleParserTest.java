package org.grajagan.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConsoleParserTest {

    private static final String TEST_MESSAGE = "{\"time\" : \"2019-04-01 16:18:17\", \"model\" :"
            + " \"Acurite 5n1 sensor\", \"sensor_id\" : 839, \"channel\" : \"A\", "
            + "\"sequence_num\" : 0, \"battery\" : \"OK\", \"message_type\" : 49, "
            + "\"wind_speed_kph\" : 0.000, \"wind_dir_deg\" : 225.000, \"rain_inch\" : 13.000}";

    private static final String TEST_STRING = "hello world";

    @Test
    public void testParse() {
        ConsoleParser parser = new ConsoleParser(new TopicalMessageReceiver() {
            @Override
            public void consume(TopicalMessage message) {
                // no-op
            }
        });
        TopicalMessage message = parser.parse(TEST_MESSAGE);
        assertEquals("Acurite_5n1_sensor", message.getTopic());
        assertEquals(TEST_MESSAGE, message.getMessage());

        message = parser.parse(TEST_STRING);

        JSONObject jo = null;
        try {
            jo = new JSONObject(message.getMessage());
        } catch (JSONException e) {
            throw new AssertionError(e);
        }

        assertNotNull(jo);
        assertEquals(TEST_STRING, jo.getString("message"));
    }
}