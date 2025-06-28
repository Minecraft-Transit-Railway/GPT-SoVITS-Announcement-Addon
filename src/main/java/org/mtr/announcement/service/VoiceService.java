package org.mtr.announcement.service;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.Voice;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class VoiceService {

	private final Object2ObjectAVLTreeMap<String, Voice> languages = new Object2ObjectAVLTreeMap<>();

	public boolean addLanguage(Voice voice) {
		if (languages.put(voice.id(), voice) == null) {
			log.info("Added language [{}]", voice.id());
			return true;
		} else {
			log.info("Replaced language [{}]", voice.id());
			return false;
		}
	}

	@Nullable
	public Voice getLanguage(String code) {
		return languages.get(code);
	}
}
