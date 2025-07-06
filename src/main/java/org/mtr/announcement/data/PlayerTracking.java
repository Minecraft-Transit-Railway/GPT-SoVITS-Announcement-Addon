package org.mtr.announcement.data;

import java.util.List;

public record PlayerTracking(String serverUrl, int dimension, String player, List<PlayerTrackingAnnouncement> announcements) {
	public record PlayerTrackingAnnouncement(String voiceId, String nextStationNoInterchange, String nextStationInterchange, String joinLast) {
	}
}
