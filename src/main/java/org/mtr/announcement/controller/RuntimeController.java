package org.mtr.announcement.controller;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.service.RuntimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public final class RuntimeController {

	private final RuntimeService runtimeService;

	public RuntimeController(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	@GetMapping("/start")
	public boolean start() {
		return runtimeService.start();
	}

	@GetMapping("/stop")
	public boolean stop() {
		return runtimeService.stop();
	}
}
