package com.tradingbot.springbot;

import com.tradingbot.springbot.model.RiskConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RiskConfig.class)
public class SpringbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbotApplication.class, args);
	}

}
