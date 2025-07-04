package org.mtr.announcement.controller;

import org.mtr.announcement.data.PlayerTracking;
import org.mtr.announcement.service.PlayerTrackingService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class PlayerTrackingController {

	private final PlayerTrackingService playerTrackingService;

	public PlayerTrackingController(PlayerTrackingService playerTrackingService) {
		this.playerTrackingService = playerTrackingService;
	}

	@PostMapping("/setPlayerTracking")
	public Mono<Boolean> setPlayerTracking(@RequestBody PlayerTracking playerTracking) {
		return playerTrackingService.update(playerTracking);
	}
}
