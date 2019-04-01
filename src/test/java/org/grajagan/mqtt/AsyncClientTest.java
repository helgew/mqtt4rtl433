package org.grajagan.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncClientTest {

    private static final String BROKER_URL = "tcp://" + Mqtt4Rtl433Main.DEFAULT_BROKER + ":1883";

    @Test
    public void testConstructor() {
        IMqttAsyncClient asyncClient =
                new AsyncClient(BROKER_URL, Mqtt4Rtl433Main.DEFAULT_CLIENT_ID, true, 0, "", "",
                        System.getProperty("java.io.tmpdir"),
                        Mqtt4Rtl433Main.DEFAULT_TOPIC + "/test");
        while (!asyncClient.isConnected()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.err.println("Interrupt while waiting for connection");
                e.printStackTrace();
            }
        }

        assertTrue("Client did not connect!", asyncClient.isConnected());
        IMqttDeliveryToken token = null;

        try {
            token = asyncClient
                    .publish(Mqtt4Rtl433Main.DEFAULT_TOPIC + "/test", "hello world".getBytes(), 0,
                            false);
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            throw new AssertionError(e);
        }

        Assume.assumeNotNull(token);
        assertTrue(token.isComplete());
    }

    @Test
    public void testCreateClient() {
        AsyncClient asyncClient = new AsyncClient();
        IMqttAsyncClient client = null;
        try {
            client = asyncClient
                    .createDelegateClient(BROKER_URL, Mqtt4Rtl433Main.DEFAULT_CLIENT_ID, null);
        } catch (MqttException e) {
            throw new AssertionError(e);
        }

        assertNotNull(client);
    }

    @Test
    public void testConnect() {
        IMqttAsyncClient client = null;
        try {
            client = new MqttAsyncClient(BROKER_URL, Mqtt4Rtl433Main.DEFAULT_CLIENT_ID, null);
        } catch (MqttException e) {
            System.err.println("Cannot create test client");
            e.printStackTrace();
        }

        Assume.assumeNotNull(client);

        AsyncClient asyncClient = new AsyncClient(client);
        IMqttToken token = null;
        try {
            token = asyncClient.connect("", "", true);
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            throw new AssertionError(e);
        }

        assertTrue(asyncClient.isConnected());
    }
}