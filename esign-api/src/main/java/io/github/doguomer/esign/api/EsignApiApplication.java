package io.github.doguomer.esign.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** esign REST API giriş noktası. REST API entry point. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EsignApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsignApiApplication.class, args);
    }
}
