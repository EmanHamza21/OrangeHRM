package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class MyLeavePage extends BasePage {
    private static final String MY_LEAVE_URL = "/web/index.php/leave/viewMyLeaveList";
    private static final DateTimeFormatter ORANGEHRM_DATE = DateTimeFormatter.ofPattern("yyyy-dd-MM");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final By myLeaveTab = By.xpath("//a[normalize-space()='My Leave']");
    private final By searchButton = By.xpath("//button[normalize-space()='Search']");
    private final By fromDate = By.xpath("//label[normalize-space()='From Date']/following::input[1]");
    private final By statusFilter = By.xpath(
            "//label[contains(normalize-space(),'Show Leave with Status')]/following::div[contains(@class,'oxd-multiselect-wrapper')][1]");
    private final By rejectedStatusChip = statusChip("Rejected");
    private final By cancelledStatusChip = statusChip("Cancelled");
    private final By scheduledStatusChip = statusChip("Scheduled");
    private final By takenStatusChip = statusChip("Taken");
    private final By pendingApprovalStatusChip = statusChip("Pending Approval");
    private final By calendarWrapper = By.cssSelector(".oxd-calendar-wrapper");
    private final By calendarMonthSelector = By.cssSelector(".oxd-calendar-selector-month");
    private final By tableHeaders = By.cssSelector(".oxd-table-header .oxd-table-cell");
    private final By tableCells = By.cssSelector(".oxd-table-cell");
    private final By requestRows = By.cssSelector(".oxd-table-card");

    public MyLeavePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        String currentUrl = driver.getCurrentUrl();
        if (!isMyLeaveUrl(currentUrl)) {
            click(myLeaveTab);
            wait.until(currentDriver -> !currentDriver.getCurrentUrl().equals(currentUrl)
                    && isMyLeaveUrl(currentDriver.getCurrentUrl()));
        }
        wait.until(currentDriver -> isMyLeaveUrl(currentDriver.getCurrentUrl())
                && currentDriver.findElements(searchButton).stream().anyMatch(WebElement::isDisplayed));
    }

    public boolean isDisplayed() {
        return isMyLeaveUrl(driver.getCurrentUrl()) && visibleWithin(searchButton, 2);
    }

    public void searchFromDateWithPendingApproval(LocalDate date) {
        String expectedDate = date.format(ORANGEHRM_DATE);
        visible(fromDate).click();

        WebElement monthSelector = wait.until(
                ExpectedConditions.elementToBeClickable(calendarMonthSelector));
        monthSelector.click();

        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        WebElement monthOption = wait.until(currentDriver ->
                currentDriver.findElements(calendarWrapper).stream()
                        .filter(WebElement::isDisplayed)
                        .flatMap(calendar -> calendar.findElements(By.xpath(
                                ".//*[normalize-space()='" + monthName + "']")).stream())
                        .filter(WebElement::isDisplayed)
                        .filter(option -> !String.valueOf(option.getAttribute("class"))
                                .contains("oxd-calendar-selector-month"))
                        .findFirst()
                        .orElse(null));
        monthOption.click();

        String dayOfMonth = Integer.toString(date.getDayOfMonth());
        By calendarDay = By.xpath(".//*[contains(@class,'oxd-calendar-date') and normalize-space()='"
                + dayOfMonth + "' and not(contains(@class,'--other-month'))]");
        WebElement day = wait.until(currentDriver ->
                currentDriver.findElements(calendarWrapper).stream()
                        .filter(WebElement::isDisplayed)
                        .flatMap(calendar -> calendar.findElements(calendarDay).stream())
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null));
        day.click();

        wait.until(currentDriver -> expectedDate.equals(
                currentDriver.findElement(fromDate).getAttribute("value")));
        retainPendingApprovalStatus();
        click(searchButton);
    }

    private void retainPendingApprovalStatus() {
        removeStatusChip(rejectedStatusChip);
        removeStatusChip(cancelledStatusChip);
        removeStatusChip(scheduledStatusChip);
        removeStatusChip(takenStatusChip);

        List<String> selectedStatuses = visible(statusFilter)
                .findElements(By.cssSelector(
                        ".oxd-multiselect-chips-area .oxd-chip.oxd-multiselect-chips-selected"))
                .stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(status -> !status.isEmpty())
                .toList();
        if (!selectedStatuses.equals(List.of("Pending Approval"))
                || elementsWithin(statusFilter, pendingApprovalStatusChip).size() != 1) {
            throw new IllegalStateException(
                    "Expected only Pending Approval to be selected, but found: " + selectedStatuses);
        }
    }

    private void removeStatusChip(By chipLocator) {
        List<WebElement> chips = elementsWithin(statusFilter, chipLocator);
        if (chips.isEmpty()) {
            return;
        }

        WebElement clearIcon = chips.get(0).findElement(By.cssSelector("i.oxd-icon.bi-x.--clear"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", clearIcon);
        wait.until(ExpectedConditions.elementToBeClickable(clearIcon)).click();
        wait.until(currentDriver -> elementsWithin(statusFilter, chipLocator).isEmpty());
    }

    private List<WebElement> elementsWithin(By parentLocator, By childLocator) {
        return visible(parentLocator).findElements(childLocator);
    }

    private static By statusChip(String status) {
        return By.xpath(".//div[contains(@class,'oxd-multiselect-chips-area')]/span"
                + "[contains(@class,'oxd-multiselect-chips-selected') and normalize-space()='"
                + status + "']");
    }

    public boolean containsRequest(LocalDate date, String leaveType) {
        return matchingRequestRow(date, leaveType) != null;
    }

    public void waitForRequest(LocalDate date, String leaveType) {
        try {
            wait.until(currentDriver -> matchingRequestRow(date, leaveType) != null);
        } catch (TimeoutException exception) {
            List<String> visibleRows = elements(requestRows).stream()
                    .filter(WebElement::isDisplayed)
                    .map(WebElement::getText)
                    .toList();
            throw new AssertionError("No My Leave row found for date " + date + " and leave type "
                    + leaveType + ". Visible rows: " + visibleRows, exception);
        }
    }

    public boolean requestHasCancelButton(LocalDate date, String leaveType) {
        WebElement row = matchingRequestRow(date, leaveType);
        return row != null && row.findElements(By.xpath(".//button[normalize-space()='Cancel']")).stream()
                .anyMatch(WebElement::isDisplayed);
    }

    public boolean requestHasStatus(LocalDate date, String leaveType, String expectedStatus) {
        WebElement row = matchingRequestRow(date, leaveType);
        if (row == null) {
            return false;
        }

        List<WebElement> headers = elements(tableHeaders);
        int statusColumn = -1;
        for (int index = 0; index < headers.size(); index++) {
            if (headers.get(index).getText().trim().equalsIgnoreCase("Status")) {
                statusColumn = index;
                break;
            }
        }
        if (statusColumn < 0) {
            throw new IllegalStateException("My Leave table does not contain a Status column");
        }

        List<WebElement> cells = row.findElements(tableCells);
        if (statusColumn >= cells.size()) {
            throw new IllegalStateException("My Leave request row is missing its Status cell");
        }
        return cells.get(statusColumn).getText().trim().toLowerCase(Locale.ROOT)
                .startsWith(expectedStatus.toLowerCase(Locale.ROOT));
    }

    private WebElement matchingRequestRow(LocalDate date, String leaveType) {
        String orangeHrmDate = date.format(ORANGEHRM_DATE);
        String isoDate = date.format(ISO_DATE);
        return elements(requestRows).stream()
                .filter(WebElement::isDisplayed)
                .filter(row -> {
                    String text = row.getText();
                    return (text.contains(orangeHrmDate) || text.contains(isoDate))
                            && text.contains(leaveType);
                })
                .findFirst()
                .orElse(null);
    }

    private boolean isMyLeaveUrl(String url) {
        return url.contains(MY_LEAVE_URL);
    }
}
