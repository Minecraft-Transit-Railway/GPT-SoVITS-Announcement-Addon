package org.mtr.announcement.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.mtr.announcement.Application;
import org.mtr.announcement.tool.Utilities;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public final class SetupService {

	private final WebClient webClient;

	private static final Path EXTRACT_DIRECTORY = Application.APPLICATION_PATH.resolve("extract");
	private static final Path ZIP_FILE = Application.APPLICATION_PATH.resolve(Application.NAMESPACE + ".7z");
	private static final Path VERSION_FILE = Application.APPLICATION_PATH.resolve("version.txt");
	private static final String[] EXTRACT_LOCATIONS = {"api_v2.py", "GPT_SoVITS/*", "runtime/*", "tools/*"};
	private static final Pattern DOWNLOAD_URL_PATTERN = Pattern.compile("https://huggingface\\.co/lj1995/GPT-SoVITS-windows-package/resolve/main/GPT-SoVITS-[\\w-]+\\.7z\\?download=true");

	public SetupService(WebClient webClient) {
		this.webClient = webClient;
	}

	public boolean prepare() {
		try {
			log.info("Setting up in [{}]", Application.APPLICATION_PATH);
			FileUtils.deleteDirectory(EXTRACT_DIRECTORY.toFile());
			FileUtils.deleteDirectory(Application.SOURCE_DIRECTORY.toFile());
			log.info("Cleaning complete");
			return true;
		} catch (Exception e) {
			log.error("Failed to prepare", e);
			return false;
		}
	}

	public Mono<Boolean> download(int retries) {
		log.info("Finding latest release from GitHub");
		return webClient.get().uri("https://api.github.com/repos/RVC-Boss/GPT-SoVITS/releases/latest").retrieve().bodyToMono(GitHubLatestRelease.class).retry(retries).onErrorResume(e -> {
			log.error("Failed to fetch latest release", e);
			return Mono.empty();
		}).flatMap(gitHubLatestRelease -> {
			try {
				FileUtils.write(VERSION_FILE.toFile(), gitHubLatestRelease.name, StandardCharsets.UTF_8);
			} catch (IOException e) {
				log.warn("Failed to write version file", e);
			}

			final Matcher matcher = DOWNLOAD_URL_PATTERN.matcher(gitHubLatestRelease.body);

			if (matcher.find()) {
				final String url = matcher.group();
				log.info("Downloading {} from [{}]", Application.NAMESPACE, url);

				return Mono.fromCallable(() -> Utilities.runWithRetry(() -> {
					FileUtils.copyURLToFile(new URL(url), ZIP_FILE.toFile());
					log.info("Download complete");
					return 0;
				}, retries) != null).onErrorResume(e -> {
					log.error("Failed to download", e);
					return Mono.just(false);
				}).subscribeOn(Schedulers.boundedElastic());
			} else {
				log.error("Failed to find download URL");
				return Mono.just(false);
			}
		}).defaultIfEmpty(false);
	}

	public boolean unzip(int retries) {
		try {
			log.info("Extracting 7z.exe");
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
					final ProcessBuilder processBuilder = new ProcessBuilder(unzipFile.getAbsolutePath(), "x", ZIP_FILE.toFile().getAbsolutePath(), "-o" + EXTRACT_DIRECTORY.toAbsolutePath(), "-y", Application.NAMESPACE + "*/" + extractLocation);
					processBuilder.inheritIO();
					final int exitCode = processBuilder.start().waitFor();
					if (exitCode != 0) {
						throw new Exception(String.format("[%s] extraction failed for command [%s] with exit runtimeCode %s", extractLocation, String.join(" ", processBuilder.command()), exitCode));
					}
					return 0;
				}, retries) == null) {
					return false;
				}
			}

			log.info("Unzip complete");
			return true;
		} catch (Exception e) {
			log.error("Failed to unzip", e);
			return false;
		}
	}

	public boolean finish() {
		try {
			log.info("Moving extracted files");
			try (final DirectoryStream<Path> stream = Files.newDirectoryStream(EXTRACT_DIRECTORY)) {
				for (final Path innerPath : stream) {
					if (Files.isDirectory(innerPath)) {
						FileUtils.moveDirectory(innerPath.toFile(), Application.SOURCE_DIRECTORY.toFile());
						break;
					}
				}
			}

			FileUtils.deleteDirectory(EXTRACT_DIRECTORY.toFile());
			log.info("Setup complete");
			return true;
		} catch (Exception e) {
			log.error("Failed to finish", e);
			return false;
		}
	}

	public String getVersion() {
		try {
			return Files.readString(VERSION_FILE);
		} catch (Exception e) {
			return "";
		}
	}

	private record GitHubLatestRelease(String name, String body) {
	}
}
