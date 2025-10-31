package com.justine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@SpringBootApplication(proxyBeanMethods = false)
public class HotelManagementApplication {
	public static void main(String[] args) {
		SpringApplication.run(HotelManagementApplication.class, args);
	}
}

