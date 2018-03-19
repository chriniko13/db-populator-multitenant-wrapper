package com.chriniko.example.multitenant.wrapper;

import com.chriniko.example.multitenant.wrapper.configuration.AppConfiguration;
import com.chriniko.example.multitenant.wrapper.core.WrapperKickstart;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Application {

    public static void main(String[] args) {

        final ApplicationContext context = new AnnotationConfigApplicationContext(AppConfiguration.class);
        context.getBean(WrapperKickstart.class).run();
    }
}
