package com.chriniko.example.multitenant.wrapper.core.properties;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class PropertiesManager {

    private static final String JDBC_URL = "jdbc.url";

    public Properties loadProperties(String propertyFile) throws IOException {

        try (InputStream inputStream = new FileInputStream(propertyFile)) {

            Properties gameInjectorPropertiesFile = new Properties();

            gameInjectorPropertiesFile.load(inputStream);

            return gameInjectorPropertiesFile;
        }
    }

    public String getJdbcUrl(Properties gameInjectorPropertiesFile) {
        return (String) gameInjectorPropertiesFile.get(JDBC_URL);
    }

}
