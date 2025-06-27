package org.mtr.announcement.tool;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class Utilities {

	private static final int RETRIES = 5;

	@Nullable
	public static JsonElement getJson(String url) {
		return runWithRetry(() -> {
			final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setUseCaches(false);
			try (final InputStream inputStream = connection.getInputStream()) {
				return JsonParser.parseString(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
			} finally {
				connection.disconnect();
			}
		});
	}

	@Nullable
	public static <T> T runWithRetry(ExceptionSupplier<T> exceptionSupplier) {
		int i = 0;
		while (true) {
			try {
				return exceptionSupplier.get();
			} catch (Exception e) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException ignored) {
				}
				i++;
				if (i >= RETRIES) {
					log.error("", e);
					return null;
				}
			}
		}
	}

	@FunctionalInterface
	public interface ExceptionSupplier<T> {
		T get() throws Exception;
	}
}
