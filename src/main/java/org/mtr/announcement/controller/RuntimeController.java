package org.mtr.announcement.controller;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.runtime.RuntimeManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class RuntimeController {

	private final RuntimeManager runtimeManager;

	public RuntimeController(RuntimeManager runtimeManager) {
		this.runtimeManager = runtimeManager;
	}

	@GetMapping("/start")
	public boolean start() {
		return runtimeManager.start();
	}

	@GetMapping("/stop")
	public boolean stop() {
		return runtimeManager.stop();
	}
}
