import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Station} from "../data/station";
import {Route} from "../data/route";
import {Client} from "../data/client";
import {url} from "../utility/settings";

const dataInterval = 30000;
const clientInterval = 3000;

@Injectable({providedIn: "root"})
export class ServerService {
	private serverUrl = "";
	private dimension = -1;
	private dataTimeoutId = 0;
	private clientTimeoutId = 0;
	private lastAnnouncedKey = "";
	private stations: Record<string, Station> = {};
	private routes: Record<string, Route> = {};
	private client?: Client;

	constructor(private readonly httpClient: HttpClient) {
	}

	updateServer(serverUrl: string, dimension: number, playerUuid: string) {
		if (serverUrl !== this.serverUrl || dimension !== this.dimension) {
			clearTimeout(this.dataTimeoutId);
			this.sendDataRequest(serverUrl, dimension);
			clearTimeout(this.clientTimeoutId);
			this.sendClientsRequest(serverUrl, dimension, playerUuid);
		}

		this.serverUrl = serverUrl;
		this.dimension = dimension;
	}

	private sendDataRequest(serverUrl: string, dimension: number) {
		this.httpClient.get<{
			data: {
				stations: {
					id: string,
					name: string,
					connections: string[],
				}[],
				routes: {
					id: string,
					name: string,
					color: number,
					stations: { id: string }[],
				}[],
			},
		}>(`${serverUrl}/mtr/api/map/stations-and-routes?dimension=${dimension}`).subscribe(({data: {stations, routes}}) => {
			this.stations = {};
			this.routes = {};

			stations.forEach(station => this.stations[station.id] = {
				id: station.id,
				name: station.name,
				connections: [],
				routes: [],
			});

			routes.forEach(route => {
				this.routes[route.id] = route;
				route.stations.forEach(station => {
					const stationData = this.stations[station.id];
					if (stationData) {
						stationData.routes.push(route);
					}
				});
			});

			stations.forEach(station => {
				const stationData = this.stations[station.id];
				if (stationData) {
					station.connections.forEach(connection => {
						const connectionData = this.stations[connection];
						if (connectionData) {
							stationData.connections.push(connectionData);
						}
					});
				}
			});

			routes.forEach(route => route.stations.forEach(({id}) => {
				const routeData = this.routes[route.id];
				const stationData = this.stations[id];
				if (routeData && stationData) {
					stationData.routes.push(routeData);
				}
			}));

			this.dataTimeoutId = setTimeout(() => this.sendDataRequest(serverUrl, dimension), dataInterval) as unknown as number;
		});
	}

	private sendClientsRequest(serverUrl: string, dimension: number, playerUuid: string) {
		this.httpClient.get<{
			data: {
				clients: {
					id: string,
					stationId: string,
					routeId: string,
					routeStationId1: string,
					routeStationId2: string
				}[],
			},
		}>(`${serverUrl}/mtr/api/map/clients?dimension=${dimension}`).subscribe(({data: {clients}}) => {
			const tempClient = clients.find(client => client.id === playerUuid);
			if (tempClient) {
				this.client = {
					id: tempClient.id,
					atStation: !!tempClient.stationId,
					route: this.routes[tempClient.routeId],
					station1: this.stations[tempClient.routeStationId1],
					station2: this.stations[tempClient.routeStationId2],
				};
				this.prepareAnnouncement();
			}

			this.dataTimeoutId = setTimeout(() => this.sendClientsRequest(serverUrl, dimension, playerUuid), clientInterval) as unknown as number;
		});
	}

	private prepareAnnouncement() {
		if (this.client && this.client.route && this.client.station1 && this.client.station2) {
			const announcementRouteId = this.client.route.id;
			const announcementStationId = this.client.station2.id;
			const key = `${announcementRouteId}_${this.client.station1.id}_${announcementStationId}`;

			if (key !== this.lastAnnouncedKey) {
				this.lastAnnouncedKey = key;
				const currentColor = this.client.route.color;
				const synthesisRequests: { voiceId: string, text: string, isCjk: boolean, interchangeNames: string[] }[] = [];
				let cjkCount = 0;

				this.client.station2.name.split("|").forEach(namePart => {
					if (ServerService.isCjk(namePart)) {
						synthesisRequests.push({voiceId: cjkCount === 0 ? "mtr-canto" : "mtr-mandarin", text: `下一站：${namePart}。`, isCjk: true, interchangeNames: []});
						cjkCount++;
					} else {
						synthesisRequests.push({voiceId: "mtr-english", text: `Next station, ${namePart}.`, isCjk: false, interchangeNames: []});
					}
				});

				if (cjkCount === 1) {
					synthesisRequests.splice(1, 0, {voiceId: "mtr-mandarin", text: synthesisRequests[0].text, isCjk: true, interchangeNames: []});
				}

				this.client.station2.routes.filter(route => route.color !== currentColor).sort((route1, route2) => route1.color - route2.color).forEach(route => {
					const visitedIndices: number[] = [];
					let firstCjk: string | undefined;
					route.name.split("||")[0].split("|").forEach(routeNamePart => {
						for (let i = 0; i < synthesisRequests.length; i++) {
							if (!(i in visitedIndices) && synthesisRequests[i].isCjk === ServerService.isCjk(routeNamePart)) {
								visitedIndices.push(i);
								ServerService.pushIfNotExists(synthesisRequests[i].interchangeNames, routeNamePart);
								if (!firstCjk && synthesisRequests[i].isCjk) {
									firstCjk = routeNamePart;
								}
							}
						}
					});
					if (!(1 in visitedIndices) && firstCjk && synthesisRequests.length > 1) {
						ServerService.pushIfNotExists(synthesisRequests[1].interchangeNames, firstCjk);
					}
				});

				synthesisRequests.forEach(synthesisRequest => {
					if (synthesisRequest.interchangeNames.length > 0) {
						if (synthesisRequest.interchangeNames.length > 1) {
							synthesisRequest.interchangeNames[synthesisRequest.interchangeNames.length - 1] = (synthesisRequest.voiceId === "mtr-english" ? "and " : "或") + synthesisRequest.interchangeNames[synthesisRequest.interchangeNames.length - 1];
						}
						if (synthesisRequest.voiceId === "mtr-canto") {
							synthesisRequest.text += `乘客可以轉乘${synthesisRequest.interchangeNames.join("，")}`;
						} else if (synthesisRequest.voiceId === "mtr-mandarin") {
							synthesisRequest.text += `乘客可以換乘${synthesisRequest.interchangeNames.join("，")}`;
						} else {
							synthesisRequest.text += ` Interchange station for the ${synthesisRequest.interchangeNames.join(", ")}`;
						}
					}
				});

				this.httpClient.post<boolean>(`${url}/api/synthesize?key=test&retries=2`, synthesisRequests).subscribe(success => {
					if (success && this.client && this.client.route?.id === announcementRouteId && (this.client.atStation && this.client.station1?.id === announcementStationId || this.client.station2?.id === announcementStationId)) {
						this.httpClient.get<boolean>(`${url}/api/play?key=test`).subscribe();
					}
				});
			}
		} else {
			this.lastAnnouncedKey = "";
		}
	}

	private static isCjk(text: string) {
		return text.match(/[\u3000-\u303F\u3040-\u309F\u30A0-\u30FF\uFF00-\uFF9F\u4E00-\u9FAF\u3400-\u4DBF]/) !== null;
	}

	private static pushIfNotExists(array: string[], element: string) {
		if (!array.includes(element)) {
			array.push(element);
		}
	}
}
