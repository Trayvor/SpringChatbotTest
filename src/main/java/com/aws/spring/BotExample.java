package com.aws.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

@SpringBootApplication
public class BotExample {

    public static void main(String[] args) throws SQLException {
        SpringApplication.run(BotExample.class, args);
    }
}