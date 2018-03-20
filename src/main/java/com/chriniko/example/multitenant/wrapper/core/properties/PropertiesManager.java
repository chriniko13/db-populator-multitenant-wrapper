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

            Properties dbPopulatorPropertiesFile = new Properties();

            dbPopulatorPropertiesFile.load(inputStream);

            return dbPopulatorPropertiesFile;
        }
    }

    public String getJdbcUrl(Properties dbPopulatorPropertiesFile) {
        return (String) dbPopulatorPropertiesFile.get(JDBC_URL);
    }

}
