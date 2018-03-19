package com.chriniko.example.multitenant.wrapper.configuration;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration

@ComponentScan("com.chriniko.example.multitenant.wrapper")

@PropertySource("classpath:application.properties")

public class AppConfiguration {

}
