package com.dailycodework.pz_4_1;

import org.flywaydb.core.Flyway; // Залиште імпорт, якщо він потрібен для CommandLineRunner
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Arrays;

@SpringBootApplication
@EnableJpaAuditing
public class Pz41Application {

    // Залиште @Autowired Flyway лише якщо він абсолютно необхідний
    // для прямого використання в Pz41Application (наприклад, для CommandLineRunner)
    // Якщо CommandLineRunner не викликає flyway.repair() в коді, то @Autowired можна видалити
    @Autowired(required = false)
    private Flyway flyway; // Spring тепер знайде Flyway bean з FlywayConfig.java

    public static void main(String[] args) {
        SpringApplication.run(Pz41Application.class, args);
    }

    // ВИДАЛИТИ: Цей метод @Bean тепер знаходиться у FlywayConfig.java
    // public Flyway flyway(DataSource dataSource) { ... }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            if (Arrays.asList(args).contains("flyway-repair")) {
                if (flyway != null) {
                    System.out.println("Running Flyway repair...");
                    flyway.repair();
                    System.out.println("Flyway repair complete. Exiting.");
                    System.exit(0);
                } else {
                    System.err.println("Flyway bean not available for repair. Check configuration.");
                }
            }
        };
    }
}