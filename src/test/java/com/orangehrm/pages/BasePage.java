package com.orangehrm.pages;

import com.orangehrm.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.getInt("explicitWaitSeconds", 15)));
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void forceClick(By locator) {
        WebElement element = visible(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void replaceText(By locator, String value) {
        WebElement element = visible(locator);
        element.clear();
        element.sendKeys(value);
    }

    protected String value(By locator) {
        return visible(locator).getAttribute("value");
    }

    protected boolean visibleWithin(By locator, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (org.openqa.selenium.TimeoutException exception) {
            return false;
        }
    }

    protected List<WebElement> elements(By locator) {
        return driver.findElements(locator);
    }

    protected By fieldByLabel(String label) {
        return By.xpath("//label[normalize-space()='" + label + "']/following::input[1]");
    }

    public String notificationText() {
        By notification = By.cssSelector(".oxd-toast-content-text");
        return visible(notification).getText();
    }

    public boolean hasFieldError() {
        return visibleWithin(By.cssSelector(".oxd-input-field-error-message"), 3);
    }

    public String fieldErrorText() {
        return visible(By.cssSelector(".oxd-input-field-error-message")).getText();
    }
}