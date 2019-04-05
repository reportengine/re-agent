package org.re.agent.mqtt;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.re.agent.utils.Utils;
import org.springframework.util.SerializationUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MqttUtils {
    public static final String TOPIC_RE_AGENT = "re/agent";

    private static MqttClient CLIENT;

    public static MqttConf config() {
        return Utils.client().getMqttBrokerSettings();
    }

    public static MqttClient client() {
        isConnected();
        return CLIENT;
    }

    private static boolean isConnected() {
        if (CLIENT == null || !CLIENT.isConnected()) {
            connect();
        }
        if (!CLIENT.isConnected()) {
            logger.error("MQTT client is not connected!");
        }
        return CLIENT.isConnected();
    }

    private static String clientId() {
        return Utils.getHostname() + RandomStringUtils.randomAlphabetic(5);
    }

    public static void connect() {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttConf conf = config();
        logger.debug("Connecting to the MQTT broker, {}", conf);
        try {
            CLIENT = new MqttClient(conf.getHostUrl(), clientId(), persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(conf.getUser());
            connOpts.setPassword(conf.getPassword().toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(30000); // 30 seconds
            CLIENT.setCallback(new SimpleCallback());
            CLIENT.connectWithResult(connOpts);
            logger.debug("Connected to the MQTT broker. Connection status:{}", CLIENT.isConnected());
            subscribe(TOPIC_RE_AGENT + "/#");
        } catch (MqttException ex) {
            logger.error("Exception,", ex);
        }
    }

    public static void subscribe(String... topics) {
        if (isConnected()) {
            try {
                CLIENT.subscribe(topics);
            } catch (MqttException ex) {
                logger.error("Exception,", ex);
            }
        }
    }

    public static void publish(String topic, Object payload, int qos) {
        if (isConnected()) {
            MqttMessage message = new MqttMessage();
            message.setPayload(SerializationUtils.serialize(payload));
            message.setQos(qos);
            try {
                CLIENT.publish(topic, message);
            } catch (MqttException ex) {
                logger.error("Exception,", ex);
            }
        }
    }
}
