package org.mtr.announcement.configuration;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.Application;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class AppConfiguration {

	private final int AUDIO_BUFFER_SIZE = 10 * 1024 * 1024; // 10 MB

	@Bean
	public WebClient webClient() {
		return WebClient.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(AUDIO_BUFFER_SIZE)).build();
	}

	@Bean
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setThreadNamePrefix("MTR-Scheduler-");
		threadPoolTaskScheduler.setRemoveOnCancelPolicy(true);
		threadPoolTaskScheduler.initialize();
		return threadPoolTaskScheduler;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationReady() {
		log.info("Angular application ready [http://localhost:{}]", Application.SERVER_PORT);
	}
}
