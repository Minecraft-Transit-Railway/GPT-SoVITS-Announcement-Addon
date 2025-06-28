package org.mtr.announcement.service;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.SynthesisRequest;
import org.mtr.announcement.data.Voice;
import org.mtr.announcement.tool.Utilities;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public final class SynthesisService {

	private int clipIndex = 0;
	private final ObjectArrayList<Clip> clips = new ObjectArrayList<>();

	private final RestTemplate restTemplate;
	private final VoiceService voiceService;
	private final RuntimeService runtimeService;

	public SynthesisService(RestTemplate restTemplate, VoiceService voiceService, RuntimeService runtimeService) {
		this.restTemplate = restTemplate;
		this.voiceService = voiceService;
		this.runtimeService = runtimeService;
	}

	public boolean synthesize(List<SynthesisRequest> synthesisRequests, int retries) {
		clips.forEach(Line::close);
		clips.clear();
		String previousVoiceId = null;

		for (final SynthesisRequest synthesisRequest : synthesisRequests) {
			final Voice voice = voiceService.getLanguage(synthesisRequest.voiceId());
			if (voice == null) {
				log.error("Voice with id [{}] not found", synthesisRequest.voiceId());
				return false;
			}

			if (!synthesisRequest.voiceId().equals(previousVoiceId) && !setLanguage(voice, retries)) {
				return false;
			}

			final Clip clip = synthesize(synthesisRequest, voice, retries);
			if (clip == null) {
				return false;
			}

			clips.add(clip);
			previousVoiceId = synthesisRequest.voiceId();
		}

		return true;
	}

	public boolean play() {
		if (clips.isEmpty()) {
			log.warn("No clips to play");
			return false;
		} else {
			log.info("Playing {} clip(s)", clips.size());
			clipIndex = clips.size();
			clips.forEach(DataLine::stop);
			clipIndex = 0;
			playInternal();
			return true;
		}
	}

	private boolean setLanguage(Voice voice, int retries) {
		if (runtimeService.isRunning()) {
			final RuntimeResponse runtimeResponse1 = Utilities.runWithRetry(() -> restTemplate.getForObject(String.format("http://localhost:%s/set_gpt_weights?weights_path=%s", runtimeService.port, voice.ckptPath()), RuntimeResponse.class), retries);
			if (runtimeResponse1 == null) {
				log.error("Failed to set GPT weights");
				return false;
			}
			log.info("Set GPT weights successful with message [{}]", runtimeResponse1.message);

			final RuntimeResponse runtimeResponse2 = Utilities.runWithRetry(() -> restTemplate.getForObject(String.format("http://localhost:%s/set_sovits_weights?weights_path=%s", runtimeService.port, voice.pthPath()), RuntimeResponse.class), retries);
			if (runtimeResponse2 == null) {
				log.error("Failed to set SoVITS weights");
				return false;
			}
			log.info("Set SoVITS weights successful with message [{}]", runtimeResponse2.message);

			return true;
		} else {
			log.error("Failed to set language; runtime not running");
			return false;
		}
	}

	@Nullable
	private Clip synthesize(SynthesisRequest synthesisRequest, Voice voice, int retries) {
		if (runtimeService.isRunning()) {
			try {
				final byte[] audioBytes = Utilities.runWithRetry(() -> restTemplate.execute(new URI(String.format(
						"http://localhost:%s/tts?text=%s&text_lang=%s&ref_audio_path=%s&prompt_lang=%s&prompt_text=%s",
						runtimeService.port,
						URLEncoder.encode(synthesisRequest.text(), StandardCharsets.UTF_8),
						URLEncoder.encode(voice.runtimeCode(), StandardCharsets.UTF_8),
						URLEncoder.encode(voice.voiceSamplePath(), StandardCharsets.UTF_8),
						URLEncoder.encode(voice.runtimeCode(), StandardCharsets.UTF_8),
						URLEncoder.encode(voice.voiceSampleText(), StandardCharsets.UTF_8)
				)), HttpMethod.GET, null, clientHttpResponse -> StreamUtils.copyToByteArray(clientHttpResponse.getBody())), retries);

				if (audioBytes == null || audioBytes.length == 0) {
					log.error("Failed to get audio input stream");
					return null;
				}

				try (final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(audioBytes)))) {
					final Clip clip = AudioSystem.getClip();
					clip.open(audioInputStream);
					clip.addLineListener(event -> {
						if (event.getType() == LineEvent.Type.STOP) {
							clipIndex++;
							playInternal();
						}
					});
					return clip;
				}
			} catch (Exception e) {
				log.error("Failed to synthesize", e);
				return null;
			}
		} else {
			log.error("Failed to synthesize; runtime not running");
			return null;
		}
	}

	private void playInternal() {
		if (clipIndex < clips.size()) {
			final Clip clip = clips.get(clipIndex);
			clip.setFramePosition(0);
			clip.start();
		}
	}

	private record RuntimeResponse(String message) {
	}
}
