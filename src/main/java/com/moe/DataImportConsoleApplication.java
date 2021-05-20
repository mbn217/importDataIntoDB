package com.moe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.moe.service.DataImportService;

@SpringBootApplication
@EnableAutoConfiguration
public class DataImportConsoleApplication implements CommandLineRunner {

    @Autowired
    private DataImportService dataImportService;

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(DataImportConsoleApplication.class);
        app.run(args);

    }

    public void run(String... args) throws Exception {
    	dataImportService.importData();
    }

}