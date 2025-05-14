package com.andremunay.hobbyhub;

import org.springframework.boot.SpringApplication;

public class TestHobbyhubApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(HobbyhubApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
