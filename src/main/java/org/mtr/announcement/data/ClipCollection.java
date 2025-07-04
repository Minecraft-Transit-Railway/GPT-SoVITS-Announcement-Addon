package org.mtr.announcement.data;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import java.util.List;

@Slf4j
public final class ClipCollection {

	private int clipIndex;

	public final String key;
	public final ObjectImmutableList<Clip> clips;

	public ClipCollection(String key, List<Clip> clips) {
		this.key = key;
		this.clips = new ObjectImmutableList<>(clips);
		clips.forEach(clip -> clip.addLineListener(event -> {
			if (event.getType() == LineEvent.Type.STOP) {
				synchronized (this) {
					clipIndex++;
					playInternal();
				}
			}
		}));
	}

	public synchronized void play() {
		log.info("Playing {} clip(s)", clips.size());
		clipIndex = clips.size();
		clips.forEach(DataLine::stop);
		clipIndex = 0;
		playInternal();
	}

	private synchronized void playInternal() {
		if (clipIndex < clips.size()) {
			final Clip clip = clips.get(clipIndex);
			clip.setFramePosition(0);
			clip.start();
		}
	}
}
