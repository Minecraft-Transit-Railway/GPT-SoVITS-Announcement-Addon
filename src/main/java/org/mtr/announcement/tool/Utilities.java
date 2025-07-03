package org.mtr.announcement.tool;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class Utilities {

	private static final int MIN_PORT = 1025;
	private static final int MAX_PORT = 65535;

	/**
	 * Run a value-returning task. If the task threw an exception, retry it.
	 *
	 * @param exceptionSupplier the task
	 * @param retries           the number of times to retry upon exception or unlimited times if {@code 0}
	 * @param <T>               the return type of the task
	 * @return the return value of the task or {@code null} if the task failed after retrying
	 */
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
				if (retries > 0 & i >= retries) {
					log.error("", e);
					return null;
				}
			}
		}
	}

	/**
	 * Find a free port (for hosting a server). Any previously used ports will be invalid.
	 *
	 * @param startingPort the port to start the search from
	 * @return a free port
	 */
	public static int findFreePort(int startingPort) {
		for (int i = Math.max(MIN_PORT, startingPort); i <= MAX_PORT; i++) {
			try (final ServerSocket serverSocket = new ServerSocket(i)) {
				final int port = serverSocket.getLocalPort();
				log.info("Found available port: {}", port);
				return port;
			} catch (Exception ignored) {
			}
		}
		return 0;
	}

	public static int findRandomFreePort() {
		return findFreePort(new Random().nextInt(MIN_PORT, (MAX_PORT + MIN_PORT) / 2));
	}

	@FunctionalInterface
	public interface ExceptionSupplier<T> {
		@Nullable
		T get() throws Exception;
	}
}
