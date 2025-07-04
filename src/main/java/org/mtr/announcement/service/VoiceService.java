package org.mtr.announcement.service;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.PythonRuntime;
import org.mtr.announcement.data.Voice;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public final class VoiceService {

	private final ConcurrentHashMap<String, ObjectObjectImmutablePair<Voice, PythonRuntime>> voiceWithRuntimeByVoiceId = new ConcurrentHashMap<>();

	private final WebClient webClient;

	public VoiceService(WebClient webClient) {
		this.webClient = webClient;
	}

	/**
	 * Add a new voice to the system. A new process will be started for every voice added.
	 *
	 * @param voice   the new voice to add
	 * @param retries how many times to retry if the operation fails
	 * @return {@link Mono} with {@code true} if successful, {@link Mono} with {@code false} otherwise
	 */
	public Mono<Boolean> addVoice(Voice voice, int retries) {
		clean();
		final ObjectObjectImmutablePair<Voice, PythonRuntime> existingVoiceWithRuntime = voiceWithRuntimeByVoiceId.get(voice.id());
		final PythonRuntime pythonRuntime;
		try {
			pythonRuntime = new PythonRuntime();
			voiceWithRuntimeByVoiceId.put(voice.id(), new ObjectObjectImmutablePair<>(voice, pythonRuntime));
		} catch (Exception e) {
			log.error("Failed to add voice [{}]", voice.id(), e);
			return Mono.just(false);
		}

		if (existingVoiceWithRuntime == null) {
			log.info("Added voice [{}]", voice.id());
		} else {
			existingVoiceWithRuntime.right().stop();
			log.info("Replaced voice [{}]", voice.id());
		}

		return webClient.get().uri(String.format("http://localhost:%s", pythonRuntime.port)).exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)).retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2)).filter(e -> pythonRuntime.isRunning())).flatMap(statusResponse -> {
			log.info("Runtime is available with response [{}]", statusResponse);
			return webClient.get().uri(String.format("http://localhost:%s/set_gpt_weights?weights_path=%s", pythonRuntime.port, voice.ckptPath())).retrieve().bodyToMono(RuntimeResponse.class).retry(retries).onErrorResume(e -> {
				log.error("Failed to set GPT weights", e);
				return Mono.empty();
			}).flatMap(runtimeResponse1 -> {
				log.info("Set GPT weights successful with message [{}]", runtimeResponse1.message);
				return webClient.get().uri(String.format("http://localhost:%s/set_sovits_weights?weights_path=%s", pythonRuntime.port, voice.pthPath())).retrieve().bodyToMono(RuntimeResponse.class).retry(retries).onErrorResume(e -> {
					log.error("Failed to set SoVITS weights", e);
					return Mono.empty();
				}).flatMap(runtimeResponse2 -> {
					log.info("Set SoVITS weights successful with message [{}]", runtimeResponse2.message);
					return Mono.just(true);
				});
			});
		}).defaultIfEmpty(false);
	}

	@Nullable
	public ObjectIntImmutablePair<Voice> getVoiceAndRuntimePort(String code) {
		clean();
		final ObjectObjectImmutablePair<Voice, PythonRuntime> voiceWithRuntime = voiceWithRuntimeByVoiceId.get(code);
		return voiceWithRuntime == null ? null : new ObjectIntImmutablePair<>(voiceWithRuntime.left(), voiceWithRuntime.right().port);
	}

	public ObjectArrayList<String> getVoiceIds() {
		clean();
		final ObjectArrayList<String> voices = new ObjectArrayList<>(voiceWithRuntimeByVoiceId.keySet());
		voices.sort(String::compareTo);
		return voices;
	}

	private void clean() {
		if (voiceWithRuntimeByVoiceId.entrySet().removeIf(entry -> !entry.getValue().right().isRunning())) {
			log.info("Removed all stopped voice runtimes");
		}
	}

	@PreDestroy
	private void shutdown() {
		voiceWithRuntimeByVoiceId.values().forEach(voiceWithRuntime -> voiceWithRuntime.right().stop());
		voiceWithRuntimeByVoiceId.clear();
	}

	private record RuntimeResponse(String message) {
	}
}
