package org.grajagan.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncPublisherTest {

    @Test
    public void testDoPublish() {
        String url = "tcp://" + Mqtt4Rtl433Main.DEFAULT_BROKER + ":1883";
        MqttAsyncClient client = null;
        try {
            client = new MqttAsyncClient(url, Mqtt4Rtl433Main.DEFAULT_CLIENT_ID, null);
            IMqttToken token = client.connect();
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            System.err.println("Cannot set up client!");
            e.printStackTrace();
        }

        Assume.assumeNotNull(client);

        AsyncPublisher publisher = new AsyncPublisher(client, 2);

        IMqttDeliveryToken token = null;
        try {
            token = publisher
                    .doPublish(Mqtt4Rtl433Main.DEFAULT_TOPIC + "/test", "test message".getBytes());
        } catch (MqttException e) {
            System.err.println("Cannot publish message!");
            e.printStackTrace();
        }

        assertNotNull(token);

        try {
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            System.err.println("Delivery did not complete!");
            e.printStackTrace();
        }

        assertTrue(token.isComplete());
    }

}