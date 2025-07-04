package org.mtr.announcement.data;

import java.util.Locale;

public record Route(String id, String name, int color) implements Comparable<Route> {

	private String combineNameColorId() {
		return (name + color + id).toLowerCase(Locale.ENGLISH);
	}

	@Override
	public int compareTo(Route route) {
		return combineNameColorId().compareTo(route.combineNameColorId());
	}
}
