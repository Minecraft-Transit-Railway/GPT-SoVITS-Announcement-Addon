package org.mtr.announcement.controller;

import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.mtr.announcement.Application;
import org.mtr.announcement.tool.Utilities;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api")
public final class SetupController {

	private static final String NAMESPACE = "GPT-SoVITS";
	private static final Path EXTRACT_DIRECTORY = Application.APPLICATION_PATH.resolve("extract");
	public static final Path SOURCE_DIRECTORY = Application.APPLICATION_PATH.resolve(NAMESPACE);
	private static final Path ZIP_FILE = Application.APPLICATION_PATH.resolve(NAMESPACE + ".7z");
	private static final Path VERSION_FILE = Application.APPLICATION_PATH.resolve("version.txt");
	private static final String[] EXTRACT_LOCATIONS = {"api_v2.py", "GPT_SoVITS/*", "runtime/*", "tools/*"};

	@GetMapping("/setup")
	public boolean setup() {
		try {
			log.info("Setting up in [{}]", Application.APPLICATION_PATH);
			FileUtils.deleteDirectory(EXTRACT_DIRECTORY.toFile());
			FileUtils.deleteDirectory(SOURCE_DIRECTORY.toFile());

			final JsonElement releasesElement = Utilities.getJson("https://api.github.com/repos/RVC-Boss/GPT-SoVITS/releases/latest");
			if (releasesElement == null) {
				return false;
			}
			final Matcher matcher = Pattern.compile("https://huggingface\\.co/lj1995/GPT-SoVITS-windows-package/resolve/main/GPT-SoVITS-[\\w-]+\\.7z\\?download=true").matcher(releasesElement.getAsJsonObject().get("body").getAsString());
			FileUtils.write(VERSION_FILE.toFile(), releasesElement.getAsJsonObject().get("name").getAsString(), StandardCharsets.UTF_8);

			if (matcher.find()) {
				final String url = matcher.group();
				log.info("Downloading {} from [{}]", NAMESPACE, url);
				if (Utilities.runWithRetry(() -> {
					FileUtils.copyURLToFile(new URL(url), ZIP_FILE.toFile());
					return 0;
				}) == null) {
					return false;
				}

				final File unzipFile = File.createTempFile("7z-", ".exe");
				try (final InputStream inputStream = getClass().getResourceAsStream("/7z.exe")) {
					if (inputStream == null) {
						log.error("7z.exe not found");
						return false;
					}
					unzipFile.deleteOnExit();
					FileUtils.copyInputStreamToFile(inputStream, unzipFile);
					if (!unzipFile.setExecutable(true)) {
						log.error("Failed to set 7z.exe executable");
						return false;
					}
				}

				for (final String extractLocation : EXTRACT_LOCATIONS) {
					log.info("Extracting [{}]", extractLocation);
					if (Utilities.runWithRetry(() -> {
						final ProcessBuilder processBuilder = new ProcessBuilder(unzipFile.getAbsolutePath(), "x", ZIP_FILE.toFile().getAbsolutePath(), "-o" + EXTRACT_DIRECTORY.toAbsolutePath(), "-y", NAMESPACE + "*/" + extractLocation);
						processBuilder.inheritIO();
						final int exitCode = processBuilder.start().waitFor();
						if (exitCode != 0) {
							throw new Exception(String.format("[%s] extraction failed for command [%s] with exit code %s", extractLocation, String.join(" ", processBuilder.command()), exitCode));
						}
						return 0;
					}) == null) {
						return false;
					}
				}

				log.info("Moving extracted files");
				try (final DirectoryStream<Path> stream = Files.newDirectoryStream(EXTRACT_DIRECTORY)) {
					for (final Path innerPath : stream) {
						if (Files.isDirectory(innerPath)) {
							FileUtils.moveDirectory(innerPath.toFile(), SOURCE_DIRECTORY.toFile());
							break;
						}
					}
				}

				FileUtils.deleteDirectory(EXTRACT_DIRECTORY.toFile());
				log.info("Setup complete");
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error("Failed to setup", e);
			return false;
		}
	}

	@GetMapping("/version")
	public String getVersion() {
		try {
			return Files.readString(VERSION_FILE);
		} catch (Exception e) {
			return "";
		}
	}
}
