package org.mtr.announcement.tool;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

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

	@FunctionalInterface
	public interface ExceptionSupplier<T> {
		@Nullable
		T get() throws Exception;
	}
}
