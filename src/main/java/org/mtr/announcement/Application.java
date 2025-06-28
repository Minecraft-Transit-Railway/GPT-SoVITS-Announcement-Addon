package org.mtr.announcement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class Application {

	public static final Path APPLICATION_PATH = Paths.get(System.getProperty("user.dir"), "GPT-SoVITS-Announcement-Addon");
	public static final String NAMESPACE = "GPT-SoVITS";
	public static final Path SOURCE_DIRECTORY = Application.APPLICATION_PATH.resolve(NAMESPACE);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
