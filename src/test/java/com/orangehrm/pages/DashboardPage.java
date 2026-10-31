package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage extends BasePage {
    private final By dashboardHeading = By.xpath("//h6[normalize-space()='Dashboard']");
    private final By loggedInUser = By.cssSelector(".oxd-userdropdown-name");
    private final By leaveNavigation = By.xpath("//span[normalize-space()='Leave']");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return visibleWithin(dashboardHeading, 10);
    }

    public String loggedInUser() {
        return visible(loggedInUser).getText();
    }

    public void openLeave() {
        click(leaveNavigation);
    }
}