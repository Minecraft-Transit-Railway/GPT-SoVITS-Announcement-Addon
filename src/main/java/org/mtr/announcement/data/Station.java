package org.mtr.announcement.data;

import java.util.concurrent.CopyOnWriteArrayList;

public record Station(String id, String name, CopyOnWriteArrayList<Station> connections, CopyOnWriteArrayList<Route> routes) {
}
