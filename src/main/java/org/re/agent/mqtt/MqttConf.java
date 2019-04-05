package org.re.agent.mqtt;

import lombok.ToString;

import lombok.Data;

@Data
@ToString(exclude = { "password" })
public class MqttConf {
    private String host;
    private Integer port;
    private String user;
    private String password;

    public boolean isValid() {
        if (host == null) {
            return false;
        }
        if (port == null) {
            return false;
        }
        return true;
    }

    public String getHostUrl() {
        return String.format("tcp://%s:%d", this.host, this.port);
    }
}
