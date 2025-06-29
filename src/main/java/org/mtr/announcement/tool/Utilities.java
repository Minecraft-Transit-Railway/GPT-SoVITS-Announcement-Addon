package org.mtr.announcement.tool;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class Utilities {

	@Nullable
	public static <T> T runWithRetry(ExceptionSupplier<T> exceptionSupplier, int retries) {
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
				if (i >= retries) {
					log.error("", e);
					return null;
				}
			}
		}
	}

	public static int findFreePort(int startingPort) {
		for (int i = Math.max(1024, startingPort); i <= 65535; i++) {
			// Start with port 80, then search from 1025 onwards
			try (final ServerSocket serverSocket = new ServerSocket(i == 1024 ? 80 : i)) {
				final int port = serverSocket.getLocalPort();
				log.info("Found available port: {}", port);
				return port;
			} catch (Exception ignored) {
			}
		}
		return 0;
	}

	@FunctionalInterface
	public interface ExceptionSupplier<T> {
		@Nullable
		T get() throws Exception;
	}
}
