package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class EmployeeFormPage extends BasePage {
    private final By firstNameField = By.cssSelector("input[placeholder='First Name']");
    private final By middleNameField = By.cssSelector("input[placeholder='Middle Name']");
    private final By lastNameField = By.cssSelector("input[placeholder='Last Name']");
    private final By employeeIdField = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Id']]//input");
    private final By saveButton = By.xpath("//button[normalize-space()='Save']");
    private final By toastContainer = By.cssSelector(".oxd-toast");
    private final By formLoader = By.cssSelector(".oxd-form-loader");
    private final By pageLoader = By.cssSelector(".oxd-loading-spinner-container");

    public EmployeeFormPage(WebDriver driver) {
        super(driver);
    }

    public void waitUntilDisplayed() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(pageLoader));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(formLoader));
        visible(firstNameField);
        visible(lastNameField);
    }

    public void waitUntilEmployeeDetailsLoaded(String expectedEmployeeId) {
        waitUntilDisplayed();
        wait.until(currentDriver -> {
            String actualEmployeeId = currentDriver.findElement(employeeIdField).getAttribute("value");
            String actualFirstName = currentDriver.findElement(firstNameField).getAttribute("value");
            String actualLastName = currentDriver.findElement(lastNameField).getAttribute("value");
            return expectedEmployeeId.equals(actualEmployeeId)
                    && !actualFirstName.isBlank()
                    && !actualLastName.isBlank();
        });
    }

    public void createEmployee(String firstName, String middleName, String lastName, String employeeId) {
        replaceFormField(firstNameField, firstName);
        replaceFormField(middleNameField, middleName);
        replaceFormField(lastNameField, lastName);
        replaceFormField(employeeIdField, employeeId);
        saveWhenFormReady();
    }

    public void submitEmptyEmployeeForm() {
        saveWhenFormReady();
    }

    public void submitWithMissingLastName(String firstName) {
        replaceFormField(firstNameField, firstName);
        replaceFormField(middleNameField, "Test");
        replaceFormField(lastNameField, "");
        saveWhenFormReady();
    }

    public String waitForSaveFeedback() {
        return wait.until(currentDriver -> {
            String notificationText = currentDriver.findElements(toastContainer).stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed)
                    .map(org.openqa.selenium.WebElement::getText)
                    .filter(text -> text.contains("Successfully Saved")
                            || text.contains("Successfully Updated")
                            || text.contains("Failed")
                            || text.contains("Error"))
                    .findFirst()
                    .orElse("");
            if (!notificationText.isEmpty()) {
                return notificationText;
            }

            return currentDriver.findElements(FIELD_ERROR).stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed)
                    .map(org.openqa.selenium.WebElement::getText)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse(null);
        });
    }

    public boolean isDisplayed() {
        return visibleWithin(firstNameField, 5)
                && visibleWithin(lastNameField, 5);
    }

    public boolean detailsAreAccessible() {
        return isDisplayed() && visibleWithin(employeeIdField, 5);
    }

    public String employeeId() {
        return value(employeeIdField);
    }

    public String firstName() {
        return value(firstNameField);
    }

    public String waitForFirstName(String expectedFirstName) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(pageLoader));
        return wait.until(currentDriver -> {
            String currentFirstName = currentDriver.findElement(firstNameField).getAttribute("value");
            return expectedFirstName.equals(currentFirstName) ? currentFirstName : null;
        });
    }

    public String lastName() {
        return value(lastNameField);
    }

    public void updateFirstName(String firstName) {
        replaceFormField(firstNameField, firstName);
        saveWhenFormReady();
    }

    private void replaceFormField(By locator, String text) {
        waitForFormReady();
        replaceText(locator, text);
    }

    private void saveWhenFormReady() {
        waitForFormReady();
        click(saveButton);
    }

    private void waitForFormReady() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(formLoader));
    }

}
