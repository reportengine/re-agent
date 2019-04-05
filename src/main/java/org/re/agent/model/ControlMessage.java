package org.re.agent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.re.agent.utils.Utils;

import lombok.Builder.Default;
import lombok.extern.slf4j.Slf4j;
import lombok.ToString;
import lombok.Getter;
import lombok.Builder;

@Builder
@Getter
@ToString
@Slf4j
public class ControlMessage {
    private String agentReference;
    private String command; // start, stop
    private String suiteId;
    private String measurementSuffix;
    @Default
    private List<String> resources = new ArrayList<>(); // cpu, memory, disk
    @Default
    private String monitorInterval = "30s"; // 30s, 1m, 10m
    private String endTime;
    @Default
    private long startTime = System.currentTimeMillis();

    public boolean isValid() {
        if (command == null) {
            return false;
        }
        // start works only with suite id
        if (suiteId == null && command.equalsIgnoreCase("start")) {
            return false;
        }
        if (agentReference != null && !agentReference.equals(Utils.getReference())) {
            return false;
        }
        if (measurementSuffix != null && measurementSuffix.trim().length() == 0) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static ControlMessage get(Map<String, Object> data) {
        try {
            return ControlMessage.builder()
                    .agentReference((String) data.get("agentReference"))
                    .command((String) data.get("command"))
                    .suiteId((String) data.get("suiteId"))
                    .measurementSuffix((String) data.get("measurementSuffix"))
                    .resources((List<String>) data.get("resources"))
                    .monitorInterval((String) data.get("monitorInterval"))
                    .endTime((String) data.get("endTime"))
                    .build();
        } catch (Exception ex) {
            logger.error("Exception", ex);
            return ControlMessage.builder().build();
        }
    }

    public Long monitorIntervalInMillisecond() {
        return timeInMillisecond(monitorInterval, 60 * 1000L);
    }

    private Long timeInMillisecond(String time, Long defaultTime) {
        if (time == null) {
            return defaultTime;
        }
        Long number = Long.valueOf(time.replaceAll("[^0-9]", ""));
        Long duration = null;
        if (time.endsWith("s")) {
            duration = number * 1000L;
        } else if (time.endsWith("m")) {
            duration = number * 1000L * 60;
        } else if (time.endsWith("h")) {
            duration = number * 1000L * 60 * 60;
        } else if (time.endsWith("d")) {
            duration = number * 1000L * 60 * 60 * 24;
        } else {
            duration = number;
        }
        return duration;
    }

    public boolean isActive() {
        if (endTime == null || endTime.trim().length() == 0) {
            return true;
        }
        return System.currentTimeMillis() <= (startTime + timeInMillisecond(endTime, 10 * 60 * 1000L));
    }

}
