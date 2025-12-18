package com.example.pixelplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
        "com.example.pixelplace",
        "org.axonframework.eventhandling.tokenstore.jpa",
        "org.axonframework.eventsourcing.eventstore.jpa",
        "org.axonframework.modelling.saga.repository.jpa"
})
public class PixelPlaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixelPlaceApplication.class, args);
    }
}