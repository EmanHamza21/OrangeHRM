package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {
    private final By username = By.name("username");
    private final By password = By.name("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By loginHeading = By.cssSelector(".orangehrm-login-title");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl);
        visible(username);
        return this;
    }

    public boolean isLoginFormDisplayed() {
        return visibleWithin(username, 5) && visibleWithin(password, 5)
                && visibleWithin(loginButton, 5) && visibleWithin(loginHeading, 5);
    }

    public DashboardPage login(String user, String secret) {
        replaceText(username, user);
        replaceText(password, secret);
        click(loginButton);
        return new DashboardPage(driver);
    }
}