package org.mtr.announcement.controller;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.service.SetupService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class SetupController {

	private final SetupService setupService;

	public SetupController(SetupService setupService) {
		this.setupService = setupService;
	}

	@GetMapping("/prepare")
	public boolean prepare() {
		return setupService.prepare();
	}

	@GetMapping("/download")
	public Mono<Boolean> download(@RequestParam int retries) {
		return setupService.download(retries);
	}

	@GetMapping("/unzip")
	public boolean unzip(@RequestParam int retries) {
		return setupService.unzip(retries);
	}

	@GetMapping("/finish")
	public boolean finish() {
		return setupService.finish();
	}

	@GetMapping("/version")
	public String version() {
		return setupService.getVersion();
	}
}
