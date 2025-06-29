package org.mtr.announcement.configuration;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.Application;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class AppConfiguration {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationReady() {
		log.info("Angular application ready [http://localhost:{}]", Application.SERVER_PORT);
	}
}
