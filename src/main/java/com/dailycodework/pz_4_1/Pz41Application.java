package com.dailycodework.pz_4_1;

import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@SpringBootApplication
@EnableJpaAuditing
public class Pz41Application {

    @Autowired
    private Flyway flyway; // Autowire Flyway bean

    public static void main(String[] args) {
        SpringApplication.run(Pz41Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            // If you want to run repair ONCE, then remove this code.
            // Or you can run it via a specific profile.
            if (Arrays.asList(args).contains("flyway-repair")) {
                System.out.println("Running Flyway repair...");
                flyway.repair();
                System.out.println("Flyway repair complete. Exiting.");
                System.exit(0); // Exit after repair
            }
        };
    }
}