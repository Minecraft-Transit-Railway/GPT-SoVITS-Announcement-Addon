package org.mtr.announcement.data;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.mtr.announcement.Application;
import org.mtr.announcement.tool.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class PythonRuntime {

	public final int port = Utilities.findRandomFreePort();
	private final Process process;

	public PythonRuntime() throws IOException {
		final File configFile = File.createTempFile("tts_infer_", "yaml");
		configFile.deleteOnExit();
		FileUtils.copyFile(Application.SOURCE_DIRECTORY.resolve("GPT_SoVITS/configs/tts_infer.yaml").toFile(), configFile);

		final ProcessBuilder processBuilder = new ProcessBuilder(Application.SOURCE_DIRECTORY.resolve("runtime/python").toAbsolutePath().toString(), "api_v2.py", "-p", String.valueOf(port), "-c", configFile.getAbsolutePath());
		processBuilder.directory(Application.SOURCE_DIRECTORY.toFile());
		processBuilder.inheritIO();
		processBuilder.environment().put("PYTHONUTF8", "1");

		log.info("Starting runtime with command [{}]", String.join(" ", processBuilder.command()));
		process = processBuilder.start();
	}

	/**
	 * Stop the Python runtime.
	 */
	public synchronized void stop() {
		if (isRunning()) {
			Utilities.runWithRetry(() -> {
				process.destroy();
				if (!process.waitFor(5, TimeUnit.SECONDS)) {
					process.destroyForcibly();
					process.waitFor();
				}
				log.info("Runtime stopped for port [{}]", port);
				return 0;
			}, 0);
		} else {
			log.info("No need to stop; runtime not running");
		}
	}

	public boolean isRunning() {
		return process.isAlive();
	}
}
