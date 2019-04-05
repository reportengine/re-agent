package org.re.agent.model;

import java.util.HashMap;
import java.util.Map;

import lombok.ToString;
import lombok.Getter;
import lombok.Builder;
import lombok.Builder.Default;

@Builder
@Getter
@ToString
public class ReMetric {
    private String suiteId;
    private String measurementSuffix;
    private Long timestamp;
    @Default
    private Map<String, String> labels = new HashMap<>();
    @Default
    private Map<String, Object> data = new HashMap<>();
}
