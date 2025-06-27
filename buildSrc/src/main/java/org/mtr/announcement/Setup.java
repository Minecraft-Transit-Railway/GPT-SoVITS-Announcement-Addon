package org.mtr.announcement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Setup {

	private final Path path;

	private static final String NAMESPACE = "GPT-SoVITS";
	private static final String[] EXTRACT_LOCATIONS = {"api_v2.py", "GPT_SoVITS/*", "runtime/*", "tools/*"};
	private static final Logger LOGGER = LogManager.getLogger("Setup");

	public Setup(Project project) {
		path = project.getProjectDir().toPath();
	}

	public void downloadPython() throws IOException, InterruptedException {
		final File sourceDirectory = path.resolve("src/main/resources/source").toFile();
		final Path extractDirectory = path.resolve("build/extract");
		FileUtils.deleteDirectory(sourceDirectory);
		FileUtils.deleteDirectory(extractDirectory.toFile());

		final JsonObject jsonObject = getJson("https://api.github.com/repos/RVC-Boss/GPT-SoVITS/releases/latest").getAsJsonObject();
		final Matcher matcher = Pattern.compile("https://huggingface\\.co/lj1995/GPT-SoVITS-windows-package/resolve/main/[\\w-]+\\.7z\\?download=true").matcher(jsonObject.get("body").getAsString());

		if (matcher.find()) {
			final String url = matcher.group();
			final File zipFile = path.resolve("build").resolve(NAMESPACE + ".7z").toFile();
			while (true) {
				try {
					System.out.println("Downloading " + NAMESPACE + " from url [" + url + "]");
					FileUtils.copyURLToFile(new URL(url), zipFile);
					break;
				} catch (Exception e) {
					LOGGER.error("", e);
					System.out.println(NAMESPACE + " download failed, retrying");
				}
			}

			for (final String extractLocation : EXTRACT_LOCATIONS) {
				while (true) {
					final ProcessBuilder processBuilder = new ProcessBuilder(path.resolve("7z").toAbsolutePath().toString(), "x", zipFile.getAbsolutePath(), "-o" + extractDirectory.toAbsolutePath(), "-y", NAMESPACE + "*/" + extractLocation);
					System.out.println("Extracting [" + extractLocation + "] with command [" + String.join(" ", processBuilder.command()) + "]");
					processBuilder.inheritIO();
					final int exitCode = processBuilder.start().waitFor();
					if (exitCode == 0) {
						break;
					} else {
						System.out.println("[" + extractLocation + "] extraction failed with exit code " + exitCode + ", retrying");
					}
				}
			}

			System.out.println("Moving extracted files");
			try (final DirectoryStream<Path> stream = Files.newDirectoryStream(extractDirectory)) {
				for (final Path innerPath : stream) {
					if (Files.isDirectory(innerPath)) {
						FileUtils.moveDirectory(innerPath.toFile(), sourceDirectory);
						break;
					}
				}
			}
		}

		FileUtils.deleteDirectory(extractDirectory.toFile());
	}

	private static JsonElement getJson(String url, String... requestProperties) {
		for (int i = 0; i < 5; i++) {
			try {
				final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
				connection.setUseCaches(false);

				for (int j = 0; j < requestProperties.length / 2; j++) {
					connection.setRequestProperty(requestProperties[2 * j], requestProperties[2 * j + 1]);
				}

				try (final InputStream inputStream = connection.getInputStream()) {
					return JsonParser.parseString(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} catch (Exception e) {
				LOGGER.error("", e);
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}

		return new JsonObject();
	}
}
