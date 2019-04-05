package org.re.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.ToString;

import lombok.Data;

@Component
@ConfigurationProperties("report.engine")
@Data
@ToString
public class ReConfig {
    private String url;
}
