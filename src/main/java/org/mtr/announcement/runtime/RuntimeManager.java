package org.mtr.announcement.runtime;

import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.controller.SetupController;
import org.mtr.announcement.tool.Utilities;
import org.springframework.stereotype.Component;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public final class RuntimeManager {

	@Nullable
	private Process process;
	private final int port = findFreePort();
	private final ProcessBuilder processBuilder = new ProcessBuilder(SetupController.SOURCE_DIRECTORY.resolve("runtime/python").toAbsolutePath().toString(), "api_v2.py", "-p", String.valueOf(port));

	public boolean start() {
		if (isRunning()) {
			log.warn("Runtime already running");
			return false;
		}

		try {
			processBuilder.directory(SetupController.SOURCE_DIRECTORY.toFile());
			processBuilder.inheritIO();
			log.info("Starting runtime with command [{}]", String.join(" ", processBuilder.command()));
			process = processBuilder.start();
			return true;
		} catch (Exception e) {
			log.error("Failed to start runtime", e);
			return false;
		}
	}

	@PreDestroy
	public boolean stop() {
		if (isRunning()) {
			return Utilities.runWithRetry(() -> {
				if (process != null) {
					process.destroy();
					if (!process.waitFor(5, TimeUnit.SECONDS)) {
						process.destroyForcibly();
						process.waitFor();
					}
				}
				return 0;
			}) != null;
		} else {
			log.warn("Cannot stop; runtime not running");
			return false;
		}
	}

	private boolean isRunning() {
		return process != null && process.isAlive();
	}

	private static int findFreePort() {
		for (int i = 9880; i <= 65535; i++) {
			// Start with port 80, then search from 1025 onwards
			try (final ServerSocket serverSocket = new ServerSocket(i)) {
				final int port = serverSocket.getLocalPort();
				log.info("Found available port: {}", port);
				return port;
			} catch (Exception ignored) {
			}
		}
		return 0;
	}
}
