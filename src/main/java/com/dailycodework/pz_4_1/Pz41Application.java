package com.dailycodework.pz_4_1;

import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
public class Pz41Application {

    public static void main(String[] args) {
        SpringApplication.run(Pz41Application.class, args);


    }

}
