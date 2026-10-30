package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class LeavePage extends BasePage {
    private static final String APPLY_LEAVE_URL = "/leave/applyLeave";

    private final By applyTab = By.xpath("//a[normalize-space()='Apply']");
    private final By applyLeaveTitle = By.xpath("//h6[normalize-space()='Apply Leave']");
    private final By leaveTypeDropdown = By.xpath(
            "//label[normalize-space()='Leave Type']/following::div[contains(@class,'oxd-select-text')][1]");
    private final By leaveTypeOptions = By.cssSelector(".oxd-select-option:not([class*='disabled'])");
    private final By durationDropdown = By.xpath(
            "//label[normalize-space()='Duration']/following::div[contains(@class,'oxd-select-text')][1]");
    private final By fromDate = By.xpath("//label[normalize-space()='From Date']/following::input[1]");
    private final By toDate = By.xpath("//label[normalize-space()='To Date']/following::input[1]");
    private final By submitButton = By.xpath("//button[@type='submit' and normalize-space()='Apply']");

    public LeavePage(WebDriver driver) {
        super(driver);
    }

    public void openApply() {
        String currentUrl = driver.getCurrentUrl();
        if (!isAtApplyUrl(currentUrl)) {
            click(applyTab);
            wait.until(currentDriver -> !currentDriver.getCurrentUrl().equals(currentUrl)
                    && isAtApplyUrl(currentDriver.getCurrentUrl()));
        }
        wait.until(currentDriver -> isAtApplyUrl(currentDriver.getCurrentUrl())
                && currentDriver.findElements(applyLeaveTitle).stream().anyMatch(WebElement::isDisplayed));
    }

    public boolean isApplyPageDisplayed() {
        return isAtApplyUrl(driver.getCurrentUrl())
                && visibleWithin(applyLeaveTitle, 2);
    }

    private boolean isAtApplyUrl(String url) {
        return url.contains(APPLY_LEAVE_URL);
    }

    public boolean hasLeaveTypeOptions() {
        return !elements(leaveTypeDropdown).isEmpty();
    }

    public void selectLeaveType(String leaveType) {
        click(leaveTypeDropdown);
        WebElement option = elements(leaveTypeOptions).stream()
                .filter(candidate -> candidate.getText().trim().equals(leaveType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Leave type is not available: " + leaveType));
        option.click();
    }

    public void enterFromDate(String start) {
        enterDate(fromDate, start);
    }

    public void enterToDate(String end) {
        enterDate(toDate, end);
    }

    public String toDate() {
        return value(toDate);
    }

    public boolean isFullDayDurationSelected() {
        return visibleWithin(durationDropdown, 3)
                && visible(durationDropdown).getText().trim().equals("Full Day");
    }

    private void enterDate(By locator, String expectedDate) {
        WebElement dateInput = visible(locator);
        dateInput.click();
        dateInput.sendKeys(Keys.CONTROL, "a");
        dateInput.sendKeys(Keys.BACK_SPACE);
        dateInput.sendKeys(expectedDate);
        dateInput.sendKeys(Keys.TAB);

        String actualDate = dateInput.getAttribute("value");
        if (!expectedDate.equals(actualDate)) {
            throw new IllegalStateException(
                    "Date field did not accept the requested value. Expected: "
                            + expectedDate + ", actual: " + actualDate);
        }
    }

    public void addComment(String comment) {
        By commentField = By.xpath("//label[normalize-space()='Comments']/following::textarea[1]");
        replaceText(commentField, comment);
    }

    public void submitRequest() {
        click(submitButton);
    }

    public String waitForSubmissionFeedback() {
        By notification = By.cssSelector(".oxd-toast-content-text");
        By fieldErrors = By.cssSelector(".oxd-input-field-error-message");
        return wait.until(driver -> {
            String notificationText = driver.findElements(notification).stream()
                    .filter(WebElement::isDisplayed)
                    .map(WebElement::getText)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse("");
            if (!notificationText.isEmpty()) {
                return notificationText;
            }

            String validationText = driver.findElements(fieldErrors).stream()
                    .filter(WebElement::isDisplayed)
                    .map(WebElement::getText)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse("");
            return validationText.isEmpty() ? null : validationText;
        });
    }

    public boolean hasDateValidation() {
        return visibleWithin(By.cssSelector(".oxd-input-field-error-message"), 3)
            || visibleWithin(By.cssSelector(".oxd-toast--error"), 1);
    }

}