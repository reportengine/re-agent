package org.re.agent.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.re.agent.ReConfig;
import org.re.agent.ReportEngineClient;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    private static String hostname = null;
    private static Integer id = -1;
    private static ReportEngineClient client = null;

    public static ReConfig reConfig() {
        return BeanUtil.getBean(ReConfig.class);
    }

    public static ReportEngineClient client() {
        if (client == null) {
            client = new ReportEngineClient(reConfig().getUrl());
        }
        return client;
    }

    public static String getHostname() {
        if (hostname == null) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getLocalHost();
                hostname = inetAddress.getHostName();
            } catch (UnknownHostException ex) {
                logger.error("Exception,", ex);
            }
        }
        return hostname;
    }

    public static String getReference() {
        return System.getenv().getOrDefault("REFERENCE", "global");
    }

    public static void updateMe(Map<String, Object> data) {
        String _hostname = (String) data.get("hostname");
        if (_hostname != null && _hostname.equals(hostname)) {
            if (data.get("id") != null) {
                id = (Integer) data.get("id");
            }
        }
    }

    public static Integer getId() {
        return id;
    }

}
