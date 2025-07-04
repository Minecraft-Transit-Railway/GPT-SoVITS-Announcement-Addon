package org.mtr.announcement.service;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.data.PlayerTracking;
import org.mtr.announcement.data.Route;
import org.mtr.announcement.data.Station;
import org.mtr.announcement.data.SynthesisRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public final class PlayerTrackingService {

	@Nullable
	private volatile PlayerTracking playerTracking;

	@Nullable
	private ScheduledFuture<?> scheduledStationsAndRoutesTask;
	private ScheduledFuture<?> scheduledClientsTask;

	private final ConcurrentHashMap<String, Station> stations = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Route> routes = new ConcurrentHashMap<>();
	private final AtomicReference<String> pendingAnnouncementKey = new AtomicReference<>();
	private final AtomicReference<String> readyAnnouncementKey = new AtomicReference<>();
	private final Object dataLock = new Object();

	private final WebClient webClient;
	private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
	private final SynthesisService synthesisService;

	private static final int STATIONS_AND_ROUTES_REFRESH_SECONDS = 30;
	private static final int CLIENTS_REFRESH_SECONDS = 3;
	private static final int RETRIES = 3;

	public PlayerTrackingService(WebClient webClient, ThreadPoolTaskScheduler threadPoolTaskScheduler, SynthesisService synthesisService) {
		this.webClient = webClient;
		this.threadPoolTaskScheduler = threadPoolTaskScheduler;
		this.synthesisService = synthesisService;
	}

	public Mono<Boolean> update(PlayerTracking playerTracking) {
		return webClient.get().uri(String.format("https://mc-heads.net/json/get_user?search&u=%s", playerTracking.player())).retrieve().bodyToMono(PlayerLookup.class).map(playerLookup -> {
			this.playerTracking = new PlayerTracking(
					playerTracking.serverUrl() + (playerTracking.serverUrl().endsWith("/") ? "" : "/"),
					playerTracking.dimension(),
					playerLookup.uuid == null ? stripUuid(playerTracking.player()) : playerLookup.uuid,
					playerTracking.announcements()
			);
			return start();
		});
	}

	private boolean start() {
		shutdown();
		final PlayerTracking playerTrackingCopy = playerTracking;
		if (playerTrackingCopy == null) {
			return false;
		} else {
			log.info("Starting Minecraft Transit Railway player tracking for server [{}] and player [{}]", playerTrackingCopy.serverUrl(), playerTrackingCopy.player());
			scheduledStationsAndRoutesTask = threadPoolTaskScheduler.scheduleWithFixedDelay(this::fetchStationsAndRoutes, Instant.now(), Duration.ofSeconds(STATIONS_AND_ROUTES_REFRESH_SECONDS));
			scheduledClientsTask = threadPoolTaskScheduler.scheduleWithFixedDelay(this::fetchClients, Instant.now(), Duration.ofSeconds(CLIENTS_REFRESH_SECONDS));
			return true;
		}
	}

	private void fetchStationsAndRoutes() {
		try {
			final PlayerTracking playerTrackingCopy = playerTracking;
			if (playerTrackingCopy != null) {
				webClient.get().uri(String.format("%smtr/api/map/stations-and-routes?dimension=%s", playerTrackingCopy.serverUrl(), playerTrackingCopy.dimension())).retrieve().bodyToMono(new ParameterizedTypeReference<DataResponse<StationsAndRoutesResponse>>() {
				}).retry(RETRIES).onErrorResume(e -> {
					log.error("Error fetching Minecraft Transit Railway stations and routes", e);
					shutdown();
					return Mono.empty();
				}).subscribe(dataResponse -> {
					synchronized (dataLock) {
						stations.clear();
						routes.clear();

						dataResponse.data.stations.forEach(stationResponse -> stations.put(stationResponse.id, new Station(stationResponse.id, stationResponse.name, new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>())));
						dataResponse.data.routes.forEach(routeResponse -> {
							if (!routeResponse.hidden) {
								routes.put(routeResponse.id, new Route(routeResponse.id, routeResponse.name, routeResponse.color));
							}
						});

						dataResponse.data.stations.forEach(stationResponse -> {
							final Station station = stations.get(stationResponse.id);
							if (station != null) {
								stationResponse.connections.forEach(stationId -> {
									final Station connectedStation = stations.get(stationId);
									if (connectedStation != null) {
										station.connections().add(connectedStation);
									}
								});
							}
						});

						dataResponse.data.routes.forEach(routeResponse -> routeResponse.stations.forEach(routeStation -> {
							final Station station = stations.get(routeStation.id);
							if (station != null) {
								final Route route = routes.get(routeResponse.id);
								if (route != null) {
									station.routes().add(route);
								}
							}
						}));

						stations.values().forEach(station -> station.routes().sort(Comparator.comparingInt(Route::color)));
					}
				});
			}
		} catch (Exception e) {
			log.error("Scheduled task failed", e);
		}
	}

	private void fetchClients() {
		try {
			final PlayerTracking playerTrackingCopy = playerTracking;
			if (playerTrackingCopy != null) {
				webClient.get().uri(String.format("%smtr/api/map/clients?dimension=%s", playerTrackingCopy.serverUrl(), playerTrackingCopy.dimension())).retrieve().bodyToMono(new ParameterizedTypeReference<DataResponse<ClientsResponse>>() {
				}).retry(RETRIES).onErrorResume(e -> {
					log.error("Error fetching Minecraft Transit Railway clients", e);
					shutdown();
					return Mono.empty();
				}).subscribe(dataResponse -> {
					for (final ClientResponse clientResponse : dataResponse.data.clients) {
						if (stripUuid(clientResponse.id).equalsIgnoreCase(playerTrackingCopy.player())) {
							synthesizeAndPlay(clientResponse);
							return;
						}
					}
					pendingAnnouncementKey.set(null);
				});
			}
		} catch (Exception e) {
			log.error("Scheduled task failed", e);
		}
	}

	private void synthesizeAndPlay(ClientResponse clientResponse) {
		synchronized (dataLock) {
			final Route route = routes.get(clientResponse.routeId);
			final Station station1 = stations.get(clientResponse.routeStationId1);
			final Station station2 = stations.get(clientResponse.routeStationId2);
			final PlayerTracking playerTrackingCopy = playerTracking;

			if (route != null && station1 != null && station2 != null && playerTrackingCopy != null) {
				final String announcementKey = String.format("%s_%s_%s", route.id(), station1.id(), station2.id());

				if (!announcementKey.equals(pendingAnnouncementKey.getAndSet(announcementKey))) {
					final ObjectObjectImmutablePair<ObjectArrayList<ObjectArrayList<String>>, ObjectArrayList<ObjectArrayList<String>>> textGroupStationName = collectLanguages(ObjectArrayList.of(station2.name()));
					final ObjectArrayList<String> interchangeRouteNames = new ObjectArrayList<>();
					station2.routes().forEach(interchangeRoute -> {
						final String interchangeRouteName = interchangeRoute.name().split("\\|\\|")[0];
						if (!interchangeRouteName.equals(route.name().split("\\|\\|")[0]) && !interchangeRouteNames.contains(interchangeRouteName)) {
							interchangeRouteNames.add(interchangeRouteName);
						}
					});
					final ObjectObjectImmutablePair<ObjectArrayList<ObjectArrayList<String>>, ObjectArrayList<ObjectArrayList<String>>> textGroupInterchanges = collectLanguages(interchangeRouteNames);

					final ObjectArrayList<SynthesisRequest> synthesisRequests = new ObjectArrayList<>();

					int stationNameCjkIndex = 0;
					int stationNameNonCjkIndex = 0;
					int interchangeStationsCjkIndex = 0;
					int interchangeStationsNonCjkIndex = 0;
					for (final PlayerTracking.PlayerTrackingAnnouncement playerTrackingAnnouncement : playerTrackingCopy.announcements()) {
						final boolean cjkOnly = playerTrackingAnnouncement.match().equalsIgnoreCase("cjk");

						final ObjectArrayList<ObjectArrayList<String>> stationNameTextList = cjkOnly ? textGroupStationName.left() : textGroupStationName.right();
						final ObjectArrayList<ObjectArrayList<String>> interchangeStationsTextList = cjkOnly ? textGroupInterchanges.left() : textGroupInterchanges.right();

						final String stationName = joinStrings(stationNameTextList.get((cjkOnly ? stationNameCjkIndex : stationNameNonCjkIndex) % stationNameTextList.size()), playerTrackingAnnouncement.joinLast());
						if (interchangeStationsTextList.isEmpty()) {
							synthesisRequests.add(new SynthesisRequest(playerTrackingAnnouncement.voiceId(), String.format(playerTrackingAnnouncement.nextStationNoInterchange(), stationName)));
						} else {
							final String interchangeStations = joinStrings(interchangeStationsTextList.get((cjkOnly ? interchangeStationsCjkIndex : interchangeStationsNonCjkIndex) % interchangeStationsTextList.size()), playerTrackingAnnouncement.joinLast());
							synthesisRequests.add(new SynthesisRequest(playerTrackingAnnouncement.voiceId(), String.format(playerTrackingAnnouncement.nextStationInterchange(), stationName, interchangeStations)));

							if (cjkOnly) {
								interchangeStationsCjkIndex++;
							} else {
								interchangeStationsNonCjkIndex++;
							}
						}

						if (cjkOnly) {
							stationNameCjkIndex++;
						} else {
							stationNameNonCjkIndex++;
						}
					}

					synthesisService.synthesize(announcementKey, synthesisRequests, 1).subscribe(success -> {
						if (success) {
							readyAnnouncementKey.set(announcementKey);
						}
					});
				}

				if (announcementKey.equals(readyAnnouncementKey.get()) && !clientResponse.stationId.equals(clientResponse.routeStationId1)) {
					synthesisService.play(announcementKey);
					readyAnnouncementKey.set(null);
				}
			}
		}
	}

	@PreDestroy
	private void shutdown() {
		if (scheduledStationsAndRoutesTask != null) {
			if (scheduledStationsAndRoutesTask.cancel(true)) {
				log.info("Stopped Minecraft Transit Railway scheduled stations and routes task");
			}
		}
		if (scheduledClientsTask != null) {
			if (scheduledClientsTask.cancel(true)) {
				log.info("Stopped Minecraft Transit Railway scheduled clients task");
			}
		}
		synchronized (dataLock) {
			stations.clear();
			routes.clear();
		}
		pendingAnnouncementKey.set(null);
	}

	private static String stripUuid(String uuidString) {
		return uuidString.toLowerCase().replaceAll("[^a-f0-9]", "");
	}

	private static ObjectObjectImmutablePair<ObjectArrayList<ObjectArrayList<String>>, ObjectArrayList<ObjectArrayList<String>>> collectLanguages(ObjectArrayList<String> textList) {
		final ObjectArrayList<ObjectArrayList<String>> cjkTextList = new ObjectArrayList<>();
		final ObjectArrayList<ObjectArrayList<String>> nonCjkTextList = new ObjectArrayList<>();
		textList.forEach(text -> {
			int cjkIndex = 0;
			int nonCjkIndex = 0;
			for (final String textPart : text.split("\\|")) {
				final boolean isCjk = isCjk(textPart);
				final ObjectArrayList<ObjectArrayList<String>> tempTextList = (isCjk ? cjkTextList : nonCjkTextList);
				while (tempTextList.size() <= (isCjk ? cjkIndex : nonCjkIndex)) {
					tempTextList.add(new ObjectArrayList<>());
				}
				tempTextList.get(isCjk ? cjkIndex : nonCjkIndex).add(textPart);
				if (isCjk) {
					cjkIndex++;
				} else {
					nonCjkIndex++;
				}
			}
		});
		return new ObjectObjectImmutablePair<>(cjkTextList, nonCjkTextList);
	}

	private static String joinStrings(ObjectArrayList<String> textList, String joinLast) {
		if (textList.isEmpty()) {
			return "";
		} else if (textList.size() == 1) {
			return textList.get(0);
		} else {
			final ObjectArrayList<String> textListCopy = new ObjectArrayList<>(textList);
			final int lastIndex = textListCopy.size() - 1;
			textListCopy.set(lastIndex, String.format("%s %s", joinLast, textListCopy.get(lastIndex)));
			return String.join(", ", textListCopy);
		}
	}

	private static boolean isCjk(String text) {
		return text.codePoints().anyMatch(codePoint -> {
			final Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(codePoint);
			return Character.isIdeographic(codePoint) ||
					unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY ||
					unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS ||
					unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
					unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT ||
					unicodeBlock == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT ||
					unicodeBlock == Character.UnicodeBlock.CJK_STROKES ||
					unicodeBlock == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
					unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
					unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
					unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
					unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C ||
					unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D ||
					unicodeBlock == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS ||
					unicodeBlock == Character.UnicodeBlock.BOPOMOFO ||
					unicodeBlock == Character.UnicodeBlock.BOPOMOFO_EXTENDED ||
					unicodeBlock == Character.UnicodeBlock.HIRAGANA ||
					unicodeBlock == Character.UnicodeBlock.KATAKANA ||
					unicodeBlock == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS ||
					unicodeBlock == Character.UnicodeBlock.KANA_SUPPLEMENT ||
					unicodeBlock == Character.UnicodeBlock.KANBUN ||
					unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO ||
					unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_A ||
					unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_B ||
					unicodeBlock == Character.UnicodeBlock.HANGUL_SYLLABLES ||
					unicodeBlock == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO ||
					unicodeBlock == Character.UnicodeBlock.KANGXI_RADICALS ||
					unicodeBlock == Character.UnicodeBlock.TAI_XUAN_JING_SYMBOLS ||
					unicodeBlock == Character.UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS;
		});
	}

	private record PlayerLookup(String uuid) {
	}

	private record DataResponse<T>(T data) {
	}

	private record StationsAndRoutesResponse(CopyOnWriteArrayList<StationResponse> stations, CopyOnWriteArrayList<RouteResponse> routes) {
	}

	private record RouteResponse(String id, String name, int color, boolean hidden, CopyOnWriteArrayList<RouteStation> stations) {
	}

	private record RouteStation(String id) {
	}

	private record StationResponse(String id, String name, CopyOnWriteArrayList<String> connections) {
	}

	private record ClientsResponse(CopyOnWriteArrayList<ClientResponse> clients) {
	}

	private record ClientResponse(String id, String stationId, String routeId, String routeStationId1, String routeStationId2) {
	}
}
