package com.example.myapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ProdConfigTemplateTest {

    @Test
    void prodConfigUsesSpringDatasourceEnvironmentKeys() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/application-prod.yml"));

        assertTrue(config.contains("url: ${SPRING_DATASOURCE_URL}"));
        assertTrue(config.contains("username: ${SPRING_DATASOURCE_USERNAME}"));
        assertTrue(config.contains("password: ${SPRING_DATASOURCE_PASSWORD}"));

        assertFalse(config.contains("url: ${DB_URL}"));
        assertFalse(config.contains("username: ${DB_USERNAME}"));
        assertFalse(config.contains("password: ${DB_PASSWORD}"));
    }
}
