package org.re.agent.resourcemonitor;

import lombok.AccessLevel;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Resource {
    private String name;
    private String identifier;

    public static Resource get(String rawData) {
        // cpu
        // memory
        // disk,/tmp/re
        Resource rData = new Resource();
        String[] data = rawData.split(",", 2);
        rData.name = data[0].trim().toLowerCase();
        if (data.length > 1) {
            rData.identifier = data[1].trim();
        }
        return rData;
    }
}
