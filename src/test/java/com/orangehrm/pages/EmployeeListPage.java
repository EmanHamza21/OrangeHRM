package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class EmployeeListPage extends BasePage {
    private final By pimNavigation = By.xpath("//span[normalize-space()='PIM']");
    private final By employeeListTab = By.xpath("//a[normalize-space()='Employee List']");
    private final By addButton = By.xpath("//button[normalize-space()='Add']");
    private final By searchButton = By.xpath("//button[normalize-space()='Search']");
    private final By resetButton = By.xpath("//button[normalize-space()='Reset']");
    private final By employeeNameFilter = By.cssSelector("input[placeholder='Type for hints...']");
    private final By employeeIdFilter = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Id']]//input");
    private final By tableRows = By.cssSelector(".oxd-table-card");
    private final By tableHeaders = By.cssSelector(".oxd-table-header-cell");
    private final By tableCells = By.cssSelector(".oxd-table-cell");
    private final By tableLoader = By.cssSelector(".oxd-table-loader");
    private final By autocompleteOptions = By.cssSelector(".oxd-autocomplete-option");
    private final By rowActionButtons = By.cssSelector("button.oxd-icon-button");
    private final By noRecordsMessage = By.xpath("//*[contains(normalize-space(), 'No Records Found')]");
    private final By confirmDeleteButton = By.xpath("//button[normalize-space()='Yes, Delete']");

    public EmployeeListPage(WebDriver driver) {
        super(driver);
    }

    public EmployeeListPage open() {
        if (!visibleWithin(searchButton, 1)) {
            if (elements(employeeListTab).stream().anyMatch(WebElement::isDisplayed)) {
                click(employeeListTab);
            } else {
                click(pimNavigation);
                click(employeeListTab);
            }
        }
        waitForSearchButton();
        return this;
    }

    public EmployeeFormPage openAddEmployee() {
        click(addButton);
        EmployeeFormPage employeeForm = new EmployeeFormPage(driver);
        employeeForm.waitUntilDisplayed();
        return employeeForm;
    }

    public EmployeeFormPage openEmployee(String expectedName) {
        WebElement row = findEmployeeRow(expectedName);
        List<WebElement> buttons = row.findElements(rowActionButtons);
        if (buttons.isEmpty()) {
            row.click();
        } else {
            buttons.get(0).click();
        }

        EmployeeFormPage employeeForm = new EmployeeFormPage(driver);
        employeeForm.waitUntilEmployeeDetailsLoaded(expectedName);
        return employeeForm;
    }

    public void searchByName(String name) {
        replaceText(employeeNameFilter, name);
        chooseFirstSuggestionIfPresent();
        click(searchButton);
        waitForSearchResults(name);
    }

    public void searchByNameAndId(String name, String id) {
        replaceText(employeeNameFilter, name);
        chooseFirstSuggestionIfPresent();
        replaceText(employeeIdFilter, id);
        click(searchButton);
        waitForSearchResults(id);
    }

    public void searchById(String id) {
        replaceText(employeeIdFilter, id);
        click(searchButton);
        waitForSearchResults(id);
    }

    public void searchAll() {
        click(searchButton);
        waitForSearchResults();
    }

    public EmployeeSummary firstEmployee() {
        WebElement row = wait.until(currentDriver ->
                currentDriver.findElements(tableRows).stream().findFirst().orElse(null));
        List<WebElement> headers = elements(tableHeaders);
        List<WebElement> cells = row.findElements(tableCells);
        String firstName = cellValue(headers, cells, header -> header.contains("first"));
        String lastName = cellValue(headers, cells, header -> header.equals("last name"));
        String employeeId = cellValue(headers, cells,
                header -> header.equals("employee id") || header.equals("id"));
        return new EmployeeSummary(firstName, lastName, employeeId);
    }

    public boolean hasEmployeeResult(String expectedText) {
        String normalizedExpected = normalize(expectedText);
        return wait.until(currentDriver -> {
            try {
                return currentDriver.findElements(tableRows).stream()
                        .map(WebElement::getText)
                        .map(EmployeeListPage::normalize)
                        .anyMatch(rowText -> rowText.contains(normalizedExpected));
            } catch (StaleElementReferenceException exception) {
                return false;
            }
        });
    }

    public String matchingRowText(String expectedText) {
        String normalizedExpected = normalize(expectedText);
        return wait.until(currentDriver -> {
            try {
                return currentDriver.findElements(tableRows).stream()
                        .map(WebElement::getText)
                        .filter(rowText -> normalize(rowText).contains(normalizedExpected))
                        .findFirst()
                        .orElse(null);
            } catch (StaleElementReferenceException exception) {
                return null;
            }
        });
    }

    public void resetFilters() {
        click(resetButton);
    }

    public boolean hasAnyEmployeeResults() {
        return visibleWithin(tableRows, 5);
    }

    public boolean hasNoEmployeeResults() {
        return visibleWithin(noRecordsMessage, 3);
    }

    public void deleteEmployee(String expectedName) {
        WebElement row = findEmployeeRow(expectedName);
        deleteEmployeeRow(row, expectedName);
    }

    public boolean deleteEmployeeIfPresent(String expectedName) {
        WebElement row = findEmployeeRowIfPresent(expectedName);
        if (row == null) {
            return false;
        }
        deleteEmployeeRow(row, expectedName);
        return true;
    }

    private void deleteEmployeeRow(WebElement row, String expectedName) {
        List<WebElement> buttons = row.findElements(rowActionButtons);
        if (buttons.size() < 2) {
            throw new IllegalStateException("Employee row does not contain a delete action: " + expectedName);
        }

        buttons.get(buttons.size() - 1).click();
        click(confirmDeleteButton);
        visible(NOTIFICATION);
    }

    private void chooseFirstSuggestionIfPresent() {
        List<WebElement> suggestions;
        try {
            suggestions = new WebDriverWait(driver, Duration.ofSeconds(3)).until(currentDriver -> {
                List<WebElement> available = currentDriver.findElements(autocompleteOptions).stream()
                        .filter(WebElement::isDisplayed)
                        .toList();
                return available.isEmpty() ? null : available;
            });
        } catch (org.openqa.selenium.TimeoutException exception) {
            visible(employeeNameFilter).sendKeys(Keys.TAB);
            return;
        }

        String enteredName = value(employeeNameFilter).trim().toLowerCase(Locale.ROOT);
        WebElement matchingSuggestion = suggestions.stream()
                .filter(suggestion -> suggestion.getText().trim().toLowerCase(Locale.ROOT)
                        .contains(enteredName))
                .findFirst()
                .orElse(suggestions.get(0));
        matchingSuggestion.click();
    }

    private void waitForSearchResults() {
        wait.until(currentDriver -> {
            try {
                boolean loading = currentDriver.findElements(tableLoader).stream()
                        .anyMatch(WebElement::isDisplayed);
                boolean hasResults = !currentDriver.findElements(tableRows).isEmpty()
                        || currentDriver.findElements(noRecordsMessage).stream()
                        .anyMatch(WebElement::isDisplayed);
                return !loading && hasResults;
            } catch (StaleElementReferenceException exception) {
                return false;
            }
        });
    }

    private void waitForSearchButton() {
        wait.until(currentDriver -> currentDriver.findElements(searchButton).stream()
                .anyMatch(WebElement::isDisplayed));
    }

    private void waitForSearchResults(String expectedResult) {
        String normalizedExpected = normalize(expectedResult);
        wait.until(currentDriver -> {
            try {
                boolean loading = currentDriver.findElements(tableLoader).stream()
                        .anyMatch(WebElement::isDisplayed);
                boolean expectedRow = currentDriver.findElements(tableRows).stream()
                        .map(WebElement::getText)
                        .map(EmployeeListPage::normalize)
                        .anyMatch(rowText -> rowText.contains(normalizedExpected));
                boolean noRecords = currentDriver.findElements(noRecordsMessage).stream()
                        .anyMatch(WebElement::isDisplayed);
                return !loading && (expectedRow || noRecords);
            } catch (StaleElementReferenceException exception) {
                return false;
            }
        });
    }

    private WebElement findEmployeeRow(String expectedText) {
        WebElement row = findEmployeeRowIfPresent(expectedText);
        if (row == null) {
            throw new AssertionError("Employee not found in results: " + expectedText);
        }
        return row;
    }

    private WebElement findEmployeeRowIfPresent(String expectedText) {
        String normalizedExpected = normalize(expectedText);
        return elements(tableRows).stream()
                .filter(row -> normalize(row.getText()).contains(normalizedExpected))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String cellValue(
            List<WebElement> headers,
            List<WebElement> cells,
            java.util.function.Predicate<String> headerMatcher) {
        for (int index = 0; index < headers.size(); index++) {
            if (headerMatcher.test(normalize(headers.get(index).getText()))) {
                if (index >= cells.size()) {
                    throw new IllegalStateException("Employee row is missing a table cell");
                }
                return cells.get(index).getText().trim();
            }
        }
        throw new IllegalStateException(
                "Employee table is missing a required column. Available columns: "
                        + headers.stream().map(WebElement::getText).toList());
    }

    public record EmployeeSummary(String firstName, String lastName, String employeeId) {
        public String fullName() {
            return (firstName + " " + lastName).trim();
        }
    }
}
