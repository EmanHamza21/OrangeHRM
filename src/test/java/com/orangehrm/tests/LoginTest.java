package com.orangehrm.tests;

import com.orangehrm.config.Config;
import com.orangehrm.pages.DashboardPage;
import com.orangehrm.pages.LoginPage;
import com.orangehrm.listeners.ExtentTestListener;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(ExtentTestListener.class)
public class LoginTest extends BaseTest {
    @Test(groups = {"smoke", "regression"})
    public void validUserCanLoginAndSeeDashboard() {
        LoginPage loginPage = new LoginPage(driver)
                .open(Config.getRequired("baseUrl"));
        Assert.assertTrue(loginPage.isLoginFormDisplayed(), "Login heading and controls should be available");

        DashboardPage dashboardPage = loginPage.login(
                Config.getRequired("username"), Config.getRequired("password"));

        Assert.assertTrue(dashboardPage.isDisplayed(), "Dashboard should be displayed after login");
        Assert.assertFalse(dashboardPage.loggedInUser().isBlank(), "Logged-in user information should be visible");
        captureCheckpoint("dashboard");
    }
}