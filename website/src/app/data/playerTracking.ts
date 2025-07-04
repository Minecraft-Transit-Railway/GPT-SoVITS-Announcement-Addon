import {ValidatedDataBase} from "./validatedDataBase";

export class PlayerTracking extends ValidatedDataBase {
	public readonly serverUrl: string;
	public readonly dimension: number;
	public readonly player: string;
	public readonly announcements: PlayerTrackingAnnouncement[];

	constructor(playerTracking: PlayerTracking) {
		super();
		this.serverUrl = playerTracking.serverUrl;
		this.dimension = playerTracking.dimension;
		this.player = playerTracking.player;
		this.announcements = playerTracking.announcements;
	}

	public isValid() {
		return !!this.serverUrl && (this.dimension === 0 || !!this.dimension) && !!this.player && !!this.announcements && this.announcements.length > 0 && this.announcements.every(announcement => new PlayerTrackingAnnouncement(announcement).isValid());
	}
}

class PlayerTrackingAnnouncement extends ValidatedDataBase {
	public readonly voiceId: string;
	public readonly nextStationNoInterchange: string;
	public readonly nextStationInterchange: string;
	public readonly joinLast: string;
	public readonly match: string;

	constructor(playerTrackingFormat: PlayerTrackingAnnouncement) {
		super();
		this.voiceId = playerTrackingFormat.voiceId;
		this.nextStationNoInterchange = playerTrackingFormat.nextStationNoInterchange;
		this.nextStationInterchange = playerTrackingFormat.nextStationInterchange;
		this.joinLast = playerTrackingFormat.joinLast;
		this.match = playerTrackingFormat.match;
	}

	public isValid() {
		return !!this.voiceId && !!this.nextStationNoInterchange && !!this.nextStationInterchange && !!this.joinLast && !!this.match;
	}
}
