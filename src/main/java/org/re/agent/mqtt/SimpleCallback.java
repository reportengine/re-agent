package org.re.agent.mqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.re.agent.model.ControlMessage;
import org.re.agent.resourcemonitor.Runner;
import org.springframework.util.SerializationUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleCallback implements MqttCallback {

    public void connectionLost(Throwable throwable) {
        logger.error("Connection lost to the broker [{}], wait 10 seconds and reconnect",
                MqttUtils.config().getHostUrl());
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException ex) {
            logger.error("Exception,", ex);
        }
        MqttUtils.connect();
    }

    @SuppressWarnings("unchecked")
    public void messageArrived(String topic, MqttMessage message) {
        if (topic.startsWith(MqttUtils.TOPIC_RE_AGENT)) {
            Map<String, Object> data = (Map<String, Object>) SerializationUtils.deserialize(message.getPayload());
            controlMessage(data);
        } else {
            logger.info("Message received: [topic:{}, payload:{}, qos:{}]",
                    topic, new String(message.getPayload()), message.getQos());
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        StringBuilder topics = new StringBuilder();
        for (String topic : token.getTopics()) {
            topics.append(topic).append(", ");
        }
        logger.debug("delivery complete. [topics:{}]", topics);
    }

    private void controlMessage(Map<String, Object> data) {
        logger.debug("Data:{}", data);
        try {
            ControlMessage ctlMessage = ControlMessage.get(data);
            if (ctlMessage.isValid()) {
                Runner.execute(ctlMessage);                
            }else{
                logger.debug("Invalid or not for me! {}", ctlMessage);
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }
    

}
