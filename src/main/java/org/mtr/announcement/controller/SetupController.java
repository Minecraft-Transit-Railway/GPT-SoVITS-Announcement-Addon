package org.mtr.announcement.controller;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.service.SetupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
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
	public boolean download(@RequestParam int retries) {
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
