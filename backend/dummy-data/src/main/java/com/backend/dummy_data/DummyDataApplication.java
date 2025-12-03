// dummy-data/src/main/java/com/backend/dummydata/DummyDataApplication.java
package com.backend.dummy_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.backend.dummy_data", "com.backend.common"})
public class DummyDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(DummyDataApplication.class, args);
    }
}