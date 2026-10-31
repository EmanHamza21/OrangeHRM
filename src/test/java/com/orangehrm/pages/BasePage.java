package com.orangehrm.pages;

import com.orangehrm.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public abstract class BasePage {
    protected static final By NOTIFICATION = By.cssSelector(".oxd-toast-content-text");
    protected static final By FIELD_ERROR = By.cssSelector(".oxd-input-field-error-message");
    protected static final By ERROR_TOAST = By.cssSelector(".oxd-toast--error");

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
        element.click();
        element.sendKeys(Keys.CONTROL, "a");
        element.sendKeys(Keys.BACK_SPACE);
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

    public String notificationText() {
        return visible(NOTIFICATION).getText();
    }

    public boolean hasFieldError() {
        return visibleWithin(FIELD_ERROR, 3);
    }

    public String fieldErrorText() {
        return visible(FIELD_ERROR).getText();
    }
}