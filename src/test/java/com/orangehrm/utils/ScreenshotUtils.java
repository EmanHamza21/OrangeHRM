package com.orangehrm.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Reporter;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ScreenshotUtils {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ScreenshotUtils() {
    }

    public static String capture(WebDriver driver, String label) {
        return capture(driver, label, Reporter.getCurrentTestResult());
    }

    public static String capture(WebDriver driver, String label, ITestResult result) {
        try {
            Path directory = Path.of("target", "screenshots");
            Files.createDirectories(directory);
            String safeLabel = label.replaceAll("[^A-Za-z0-9._-]", "_");
            Path destination = directory.resolve(safeLabel + "-" + LocalDateTime.now().format(TIMESTAMP) + ".png");
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), destination);

            if (result != null) {
                @SuppressWarnings("unchecked")
                List<String> paths = (List<String>) result.getAttribute("screenshots");
                if (paths == null) {
                    paths = new ArrayList<>();
                    result.setAttribute("screenshots", paths);
                }
                paths.add(destination.toAbsolutePath().toString());
            }
            return destination.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save screenshot", exception);
        }
    }
}