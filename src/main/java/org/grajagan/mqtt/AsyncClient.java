package org.grajagan.mqtt;

import lombok.experimental.Delegate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.grajagan.util.TopicalMessage;
import org.grajagan.util.TopicalMessageReceiver;

public class AsyncClient implements TopicalMessageReceiver, IMqttAsyncClient {

    private static final Logger log = LogManager.getLogger(AsyncClient.class);

    private AsyncPublisher publisher;
    private String mainTopic;

    @Delegate
    private IMqttAsyncClient delegateClient;

    /**
     * Constructs an instance of the delegateClient wrapper
     *
     * @param brokerUrl the url to connect to
     * @param clientId  the delegateClient id to connect with
     * @param clean     clear state at end of connection or not (durable or non-durable subscriptions)
     * @param qos       the quality of service to delivery the message at (0,1,2)
     * @param userName  the username to connect with
     * @param password  the password for the user
     * @param tmpDir    the temporary directory to use for caching of inflight messages
     * @throws MqttException
     */
    public AsyncClient(String brokerUrl, String clientId, boolean clean, int qos, String userName,
            String password, String tmpDir, String mainTopic) {

        log.debug("Storing in-flight messages in " + tmpDir);
        MqttClientPersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        this.mainTopic = mainTopic;

        try {
            delegateClient = createDelegateClient(brokerUrl, clientId, dataStore);

            log.trace("Connecting to " + brokerUrl + " as " + clientId);
            connect(userName, password, clean);

            publisher = new AsyncPublisher(this, qos);

        } catch (MqttException e) {
            log.error("Unable to set up delegateClient!", e);
            System.exit(1);
        }
    }

    @Override
    public void consume(TopicalMessage message) {
        try {
            publisher.doPublish(mainTopic + "/" + message.getTopic(),
                    message.getMessage().getBytes());
        } catch (MqttException e) {
            log.error("Could not publish!");
            // Display full details of any exception that occurs
            log.error("reason " + e.getReasonCode());
            log.error("msg " + e.getMessage());
            log.error("loc " + e.getLocalizedMessage());
            log.error("cause " + e.getCause());
            log.error("excep " + e);
            log.error(e);
        }
    }

    protected AsyncClient() {
        // testing only
    }

    protected AsyncClient(IMqttAsyncClient client) {
        this.delegateClient = client;
    }

    protected IMqttAsyncClient createDelegateClient(String brokerUrl, String clientId,
            MqttClientPersistence dataStore) throws MqttException {
        MqttAsyncClient client = new MqttAsyncClient(brokerUrl, clientId, dataStore);

        DisconnectedBufferOptions options = new DisconnectedBufferOptions();
        options.setBufferEnabled(true);
        options.setPersistBuffer(true);
        client.setBufferOpts(options);

        return client;
    }

    protected IMqttToken connect(String userName, String password, boolean clean)
            throws MqttException {
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(clean);
        conOpt.setAutomaticReconnect(true);

        if (userName != null && !userName.equals("")) {
            conOpt.setUserName(userName);
        }

        if (password != null && !password.equals("")) {
            conOpt.setPassword(password.toCharArray());
        }

        return connect(conOpt);
    }
}
