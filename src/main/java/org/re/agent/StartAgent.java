package org.re.agent;

import java.io.File;

import org.re.agent.model.SigarVersion;
import org.re.agent.mqtt.MqttUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class StartAgent {

    public static void main(String[] args) {
        SpringApplication.run(StartAgent.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomeWorkOnStartup() {
        loadSigarLibs();
        logger.info("{}", SigarVersion.get());
        MqttUtils.connect();

    }

    public void loadSigarLibs() {
        String[] dirLocations = { "/app/sigar-libs/lib", "./sigar-libs/lib" };
        for (String location : dirLocations) {
            if (new File(location).exists()) {
                System.setProperty("java.library.path", location);
                logger.debug("sigar lib location: {}", location);
            }
        }
    }

}
