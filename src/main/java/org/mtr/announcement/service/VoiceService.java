package org.mtr.announcement.service;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.Voice;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class VoiceService {

	private final Object2ObjectAVLTreeMap<String, Voice> voices = new Object2ObjectAVLTreeMap<>();

	public boolean addVoice(Voice voice) {
		if (voices.put(voice.id(), voice) == null) {
			log.info("Added voice [{}]", voice.id());
			return true;
		} else {
			log.info("Replaced voice [{}]", voice.id());
			return false;
		}
	}

	@Nullable
	public Voice getVoice(String code) {
		return voices.get(code);
	}
}
