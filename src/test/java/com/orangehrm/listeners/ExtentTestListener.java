package com.orangehrm.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.orangehrm.config.Config;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExtentTestListener implements ITestListener {
    private static final ExtentReports REPORTS = createReport();
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    private static ExtentReports createReport() {
        String reportPath = Config.get("reportPath", "target/extent-report.html");
        try {
            Path parent = Path.of(reportPath).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create report directory", exception);
        }
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setDocumentTitle("OrangeHRM UI Automation");
        sparkReporter.config().setReportName("Senior QA Assessment Execution");
        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        return reports;
    }

    @Override
    public void onTestStart(ITestResult result) {
        CURRENT_TEST.set(REPORTS.createTest(result.getMethod().getMethodName()));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = CURRENT_TEST.get();
        test.pass("Passed in " + duration(result) + " ms");
        attachScreenshots(test, result);
        CURRENT_TEST.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = CURRENT_TEST.get();
        test.fail(result.getThrowable());
        test.info("Failed in " + duration(result) + " ms");
        attachScreenshots(test, result);
        CURRENT_TEST.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = CURRENT_TEST.get();
        if (test != null) {
            test.skip(result.getThrowable());
            CURRENT_TEST.remove();
        }
    }

    @Override
    public void onFinish(org.testng.ITestContext context) {
        REPORTS.flush();
    }

    private void attachScreenshots(ExtentTest test, ITestResult result) {
        Object value = result.getAttribute("screenshots");
        if (value instanceof List<?> paths) {
            for (Object path : paths) {
                test.addScreenCaptureFromPath(path.toString());
            }
        }
    }

    private long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}