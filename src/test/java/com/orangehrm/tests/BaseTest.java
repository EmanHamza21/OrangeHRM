package com.orangehrm.tests;

import com.orangehrm.config.Config;
import com.orangehrm.driver.DriverFactory;
import com.orangehrm.pages.DashboardPage;
import com.orangehrm.pages.LoginPage;
import com.orangehrm.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    protected WebDriver driver;
    protected DashboardPage dashboard;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.createDriver();
    }

    protected DashboardPage login() {
        LoginPage loginPage = new LoginPage(driver).open(Config.getRequired("baseUrl"));
        dashboard = loginPage.login(
                Config.getRequired("username"), Config.getRequired("password"));
        return dashboard;
    }

    protected void captureCheckpoint(String checkpoint) {
        ScreenshotUtils.capture(driver, getClass().getSimpleName() + "-" + checkpoint);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            ScreenshotUtils.capture(driver, getClass().getSimpleName() + "-failure", result);
        }
        DriverFactory.quitDriver();
    }
}