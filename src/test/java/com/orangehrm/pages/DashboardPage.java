package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage extends BasePage {
    private final By dashboardHeading = By.xpath("//h6[normalize-space()='Dashboard']");
    private final By loggedInUser = By.cssSelector(".oxd-userdropdown-name");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return visibleWithin(dashboardHeading, 10);
    }

    public String loggedInUser() {
        return visible(loggedInUser).getText();
    }

    public void openPim() {
        click(By.xpath("//span[normalize-space()='PIM']"));
    }

    public void openLeave() {
        click(By.xpath("//span[normalize-space()='Leave']"));
    }
}