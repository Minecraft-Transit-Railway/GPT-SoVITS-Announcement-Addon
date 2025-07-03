package org.mtr.announcement.service;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.ClipCollection;
import org.mtr.announcement.data.SynthesisRequest;
import org.mtr.announcement.data.Voice;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public final class SynthesisService {

	private final List<ClipCollection> clipCollections = Collections.synchronizedList(new ObjectArrayList<>());

	private final WebClient webClient;
	private final VoiceService voiceService;

	private final int MAX_CLIP_COLLECTIONS = 10;

	public SynthesisService(WebClient webClient, VoiceService voiceService) {
		this.webClient = webClient;
		this.voiceService = voiceService;
	}

	public Mono<Boolean> synthesize(String key, List<SynthesisRequest> synthesisRequests, int retries) {
		clipCollections.removeIf(clipCollection -> {
			if (clipCollection.key.equals(key)) {
				clipCollection.clips.forEach(Line::close);
				return true;
			}
			return false;
		});

		return Flux.fromIterable(synthesisRequests).flatMapSequential(synthesisRequest -> {
			final ObjectIntImmutablePair<Voice> voiceAndRuntimePort = voiceService.getVoiceAndRuntimePort(synthesisRequest.voiceId());
			if (voiceAndRuntimePort == null) {
				log.error("Voice with id [{}] not found", synthesisRequest.voiceId());
				return Mono.error(new RuntimeException("Voice not found"));
			}

			return webClient.get().uri(String.format(
					"http://localhost:%s/tts?text=%s&text_lang=%s&ref_audio_path=%s&prompt_lang=%s&prompt_text=%s",
					voiceAndRuntimePort.rightInt(),
					synthesisRequest.text(),
					voiceAndRuntimePort.left().runtimeCode(),
					voiceAndRuntimePort.left().voiceSamplePath(),
					voiceAndRuntimePort.left().runtimeCode(),
					voiceAndRuntimePort.left().voiceSampleText()
			)).retrieve().bodyToMono(byte[].class).retry(retries).flatMap(audioBytes -> Mono.fromCallable(() -> {
				final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(audioBytes)));
				final Clip clip = AudioSystem.getClip();
				clip.open(audioInputStream);
				log.info("Text synthesis successful for [{}]", synthesisRequest.text());
				return clip;
			}).subscribeOn(Schedulers.boundedElastic()));
		}).collectList().map(clips -> {
			while (clipCollections.size() >= MAX_CLIP_COLLECTIONS) {
				clipCollections.remove(0).clips.forEach(Line::close);
			}
			clipCollections.add(new ClipCollection(key, clips));
			return true;
		}).onErrorResume(e -> {
			log.error("Synthesis failed", e);
			return Mono.just(false);
		});
	}

	public boolean play(String key) {
		for (final ClipCollection clipCollection : clipCollections) {
			if (clipCollection.key.equals(key)) {
				clipCollection.play();
				return true;
			}
		}

		log.warn("Clips for key [{}] not found", key);
		return false;
	}
}
