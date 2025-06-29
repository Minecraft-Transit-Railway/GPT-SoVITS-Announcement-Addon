package org.mtr.announcement.controller;

import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.SynthesisRequest;
import org.mtr.announcement.data.Voice;
import org.mtr.announcement.service.SynthesisService;
import org.mtr.announcement.service.VoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class SynthesisController {

	private final VoiceService voiceService;
	private final SynthesisService synthesisService;

	public SynthesisController(VoiceService voiceService, SynthesisService synthesisService) {
		this.voiceService = voiceService;
		this.synthesisService = synthesisService;
	}

	@GetMapping("/addVoice")
	public boolean addVoice(@RequestParam String id, @RequestParam String runtimeCode, @RequestParam String ckptPath, @RequestParam String pthPath, @RequestParam String voiceSamplePath, @RequestParam String voiceSampleText) {
		return voiceService.addVoice(new Voice(id, runtimeCode, ckptPath, pthPath, voiceSamplePath, voiceSampleText));
	}

	@PostMapping("/synthesize")
	public boolean synthesize(@RequestBody List<SynthesisRequest> synthesisRequests, @RequestParam int retries) {
		return synthesisService.synthesize(synthesisRequests, retries);
	}

	@GetMapping("/play")
	public boolean play() {
		return synthesisService.play();
	}
}
