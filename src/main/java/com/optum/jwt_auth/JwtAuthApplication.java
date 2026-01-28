package com.optum.jwt_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class JwtAuthApplication {

	public static void main(String[] args) {


        String encodeStr = new BCryptPasswordEncoder().encode("admin123");
        System.out.println("Encoded password for 'admin123': " + encodeStr);
        SpringApplication.run(JwtAuthApplication.class, args);
	}

}
