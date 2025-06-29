package org.mtr.announcement.service;

import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mtr.announcement.Application;
import org.mtr.announcement.tool.Utilities;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public final class RuntimeService {

	@Nullable
	private Process process;

	public final int port = Utilities.findFreePort(9880);
	private final ProcessBuilder processBuilder = new ProcessBuilder(Application.SOURCE_DIRECTORY.resolve("runtime/python").toAbsolutePath().toString(), "api_v2.py", "-p", String.valueOf(port));

	public boolean start() {
		if (isRunning()) {
			log.warn("Runtime already running");
			return false;
		}

		try {
			processBuilder.directory(Application.SOURCE_DIRECTORY.toFile());
			processBuilder.inheritIO();
			processBuilder.environment().put("PYTHONUTF8", "1");
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
				log.info("Runtime stopped");
				return 0;
			}, Integer.MAX_VALUE) != null;
		} else {
			log.info("No need to stop; runtime not running");
			return false;
		}
	}

	public boolean isRunning() {
		return process != null && process.isAlive();
	}
}
