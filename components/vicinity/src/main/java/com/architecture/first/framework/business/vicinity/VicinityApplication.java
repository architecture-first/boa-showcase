package com.architecture.first.framework.business.vicinity;

import com.architecture.first.framework.business.vicinity.config.VicinityConfig;
import com.architecture.first.framework.technical.util.RuntimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@ComponentScan(basePackages = {"com.architecture.first"})
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class VicinityApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(VicinityApplication.class, args);
    }

    public static void close() {
        if (RuntimeUtils.isInDebugger()) {
            restart();
        }
        else {
            stop();
        }
    }

    /**
     * Ends the process after attempts to keep it alive.
     * This depends on the hosting environment, such as Kubernetes instantiating a new instance.
     * This prevents a hung pod running and using system resources.
     */
    public static void stop() {
        try {
            ApplicationArguments args = context.getBean(ApplicationArguments.class);
            context.close();
            SpringApplication.exit(context);
            System.exit(1);
        }
        catch (Exception e){
            log.error("Error shutting down: ", e);
        }
    }

    public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(VicinityApplication.class, args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }

}
