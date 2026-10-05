package ru.fsp.jobsearcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JobsearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobsearcherApplication.class, args);
    }
}
