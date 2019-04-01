package org.grajagan.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class AsyncPublisher {
    private static final Logger log = LogManager.getLogger(AsyncPublisher.class);

    private IMqttAsyncClient client;
    private int qos;

    public AsyncPublisher(IMqttAsyncClient client, int qos) {
        this.client = client;
        this.qos = qos;
    }

    public IMqttDeliveryToken doPublish(String topicName, byte[] payload) throws MqttException {

        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);

        log.trace("publishing to topic \"" + topicName + "\" qos " + qos);

        // Setup a listener object to be notified when the publish completes.
        IMqttActionListener pubListener = new IMqttActionListener() {
            public void onSuccess(IMqttToken asyncActionToken) {
                log.trace("published message " + asyncActionToken.getMessageId() + "!");
            }

            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log.error("publish failed for msg. " + asyncActionToken.getMessageId() + "!",
                        exception);
            }
        };

        return client.publish(topicName, message, "Mqtt4Rtl433 context", pubListener);
    }
}
