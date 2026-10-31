package com.orangehrm.tests;

import com.orangehrm.listeners.ExtentTestListener;
import com.orangehrm.pages.EmployeeFormPage;
import com.orangehrm.pages.EmployeeListPage;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Listeners(ExtentTestListener.class)
public class EmployeeTest extends BaseTest {
    private static final String FIRST_NAME = "Automation";
    private static final String MIDDLE_NAME = "Test";

    private EmployeeRecord employeeUnderTest;

    @Test(groups = {"regression", "employee"})
    public void employeeCanBeCreated() {
        login();
        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeUnderTest = newEmployeeRecord();

        EmployeeFormPage employeeFormPage = employeeListPage.openAddEmployee();
        employeeFormPage.createEmployee(
                employeeUnderTest.firstName(),
                employeeUnderTest.middleName(),
                employeeUnderTest.lastName(),
                employeeUnderTest.employeeId());

        Assert.assertTrue(employeeFormPage.waitForSaveFeedback().contains("Successfully Saved"),
                "Employee creation should show a success notification");
        Assert.assertTrue(employeeFormPage.detailsAreAccessible(),
                "Created employee details should be accessible");
        Assert.assertEquals(employeeFormPage.firstName(), employeeUnderTest.firstName());
        Assert.assertEquals(employeeFormPage.lastName(), employeeUnderTest.lastName());

        Assert.assertEquals(employeeFormPage.employeeId(), employeeUnderTest.employeeId(),
                "The unique employee ID should be saved");
        captureCheckpoint("employee-created");
    }

    @Test(groups = {"regression", "employee"})
    public void employeeCanBeFoundByName() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchByName(employeeUnderTest.fullName());

        Assert.assertTrue(employeeListPage.hasEmployeeResult(employeeUnderTest.lastName()),
                "Name search should return the created employee");
        Assert.assertTrue(employeeListPage.matchingRowText(employeeUnderTest.lastName())
                        .contains(employeeUnderTest.employeeId()),
                "The matching result should include the generated employee ID");
        captureCheckpoint("employee-search-by-name");
    }

    @Test(groups = {"regression", "employee"})
    public void existingEmployeeCanBeFoundByName() {
        login();
        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchAll();
        if (!employeeListPage.hasAnyEmployeeResults()) {
            throw new SkipException("The demo currently has no existing employee records to search.");
        }
        EmployeeListPage.EmployeeSummary existingEmployee = employeeListPage.firstEmployee();

        Assert.assertFalse(existingEmployee.employeeId().isBlank(),
                "The existing employee should have an employee ID");
        Assert.assertFalse(existingEmployee.fullName().isBlank(),
                "The existing employee should have a name");

        employeeListPage.searchByName(existingEmployee.fullName());
        Assert.assertTrue(employeeListPage.hasEmployeeResult(existingEmployee.employeeId()),
                "Searching the existing employee by name should return the same employee ID");
        Assert.assertTrue(employeeListPage.matchingRowText(existingEmployee.employeeId())
                        .contains(existingEmployee.lastName()),
                "The matching result should show the existing employee's last name");
    }

    @Test(groups = {"regression", "employee"})
    public void employeeCanBeFoundById() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchById(employeeUnderTest.employeeId());

        Assert.assertTrue(employeeListPage.hasEmployeeResult(employeeUnderTest.employeeId()),
                "ID search should return the created employee");
        captureCheckpoint("employee-search-by-id");
    }

    @Test(groups = {"regression", "employee"})
    public void createdEmployeeCanBeReopenedWithPersistedDetails() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchById(employeeUnderTest.employeeId());
        EmployeeFormPage employeeFormPage = employeeListPage.openEmployee(employeeUnderTest.employeeId());

        Assert.assertEquals(employeeFormPage.waitForFirstName(employeeUnderTest.firstName()),
                employeeUnderTest.firstName());
        Assert.assertEquals(employeeFormPage.lastName(), employeeUnderTest.lastName());
        Assert.assertEquals(employeeFormPage.employeeId(), employeeUnderTest.employeeId());
        Assert.assertTrue(employeeFormPage.detailsAreAccessible(),
                "The reopened employee record should expose its details");
    }

    @Test(groups = {"regression", "employee"})
    public void employeeNameUpdatePersistsAfterRefresh() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchById(employeeUnderTest.employeeId());
        Assert.assertTrue(employeeListPage.hasEmployeeResult(employeeUnderTest.employeeId()),
                "The created employee should be available before opening its record");
        EmployeeFormPage employeeFormPage = employeeListPage.openEmployee(employeeUnderTest.employeeId());

        String updatedFirstName = "Updated" + employeeUnderTest.lastName().substring(2);
        employeeFormPage.updateFirstName(updatedFirstName);
        String updateFeedback = employeeFormPage.waitForSaveFeedback();
        Assert.assertTrue(updateFeedback.contains("Successfully Updated"),
                "Employee update failed: " + updateFeedback);
        Assert.assertEquals(employeeFormPage.waitForFirstName(updatedFirstName), updatedFirstName);
        employeeUnderTest = employeeUnderTest.withFirstName(updatedFirstName);
        captureCheckpoint("employee-updated");

        driver.navigate().refresh();
        employeeFormPage = new EmployeeFormPage(driver);
        Assert.assertEquals(employeeFormPage.waitForFirstName(updatedFirstName), updatedFirstName,
                "Updated employee name should remain after refreshing the details page");

        employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchById(employeeUnderTest.employeeId());
        employeeFormPage = employeeListPage.openEmployee(employeeUnderTest.employeeId());
        Assert.assertEquals(employeeFormPage.waitForFirstName(updatedFirstName), updatedFirstName,
                "Updated employee name should remain after navigating away and reopening the record");
    }

    @Test(groups = {"regression", "employee"})
    public void employeeCanBeFoundUsingCombinedNameAndIdFilters() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchByNameAndId(
                employeeUnderTest.fullName(), employeeUnderTest.employeeId());
        Assert.assertTrue(employeeListPage.hasEmployeeResult(employeeUnderTest.lastName()),
                "Matching name and ID filters should return the employee");
    }

    @Test(groups = {"regression", "employee"})
    public void mismatchedEmployeeIdReturnsNoResults() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchByNameAndId(employeeUnderTest.fullName(), "no-such-id");
        Assert.assertTrue(employeeListPage.hasNoEmployeeResults(),
                "A nonmatching employee ID should return no records");
    }

    @Test(groups = {"regression", "employee"})
    public void resettingEmployeeSearchFiltersRestoresResults() {
        employeeUnderTest = createEmployeeFixture();

        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchById("no-such-id");
        employeeListPage.resetFilters();
        Assert.assertTrue(employeeListPage.hasAnyEmployeeResults(),
                "Resetting filters should restore employee results");
        Assert.assertTrue(employeeListPage.hasEmployeeResult(employeeUnderTest.lastName()),
                "Resetting filters should restore the created employee to the results");
    }

    @Test(groups = {"negative", "regression", "employee"})
    public void requiredEmployeeFieldsAreValidated() {
        login();
        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        EmployeeFormPage employeeFormPage = employeeListPage.openAddEmployee();
        String uniqueFirstName = "Invalid" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        employeeFormPage.submitWithMissingLastName(uniqueFirstName);

        Assert.assertTrue(employeeFormPage.hasFieldError(),
                "Required employee fields should show validation");
        Assert.assertTrue(employeeFormPage.isDisplayed(),
                "The invalid form should remain on the add page");
        captureCheckpoint("employee-required-validation");

        employeeListPage = new EmployeeListPage(driver).open();
        employeeListPage.searchByName(uniqueFirstName);
        Assert.assertTrue(employeeListPage.hasNoEmployeeResults(),
                "An employee with a missing required last name must not be created");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupCreatedEmployee() {
        if (employeeUnderTest == null || employeeUnderTest.employeeId() == null
                || employeeUnderTest.employeeId().isBlank() || driver == null) {
            return;
        }

        try {
            EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
            employeeListPage.searchById(employeeUnderTest.employeeId());
            boolean deleted = employeeListPage.deleteEmployeeIfPresent(employeeUnderTest.lastName());
            if (!deleted) {
                Reporter.log("Employee cleanup found no matching row for last name "
                        + employeeUnderTest.lastName(), true);
            }
        } catch (RuntimeException exception) {
            Reporter.log("Employee test cleanup could not remove its test record: "
                    + exception.getMessage(), true);
        } finally {
            employeeUnderTest = null;
        }
    }

    private EmployeeRecord createEmployeeFixture() {
        login();
        EmployeeListPage employeeListPage = new EmployeeListPage(driver).open();
        employeeUnderTest = newEmployeeRecord();

        EmployeeFormPage employeeFormPage = employeeListPage.openAddEmployee();
        employeeFormPage.createEmployee(
                employeeUnderTest.firstName(),
                employeeUnderTest.middleName(),
                employeeUnderTest.lastName(),
                employeeUnderTest.employeeId());
        Assert.assertTrue(employeeFormPage.waitForSaveFeedback().contains("Successfully Saved"),
                "Test employee fixture should be created successfully");

        employeeUnderTest = employeeUnderTest.withEmployeeId(employeeFormPage.employeeId());
        Assert.assertFalse(employeeUnderTest.employeeId().isBlank(),
                "Test employee fixture should have a generated ID");
        return employeeUnderTest;
    }

    private EmployeeRecord newEmployeeRecord() {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String employeeId = Integer.toString(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        return new EmployeeRecord(FIRST_NAME, MIDDLE_NAME, "QA" + uniqueSuffix, employeeId);
    }

    private record EmployeeRecord(
            String firstName,
            String middleName,
            String lastName,
            String employeeId) {

        private String fullName() {
            return firstName + " " + middleName + " " + lastName;
        }

        private EmployeeRecord withFirstName(String updatedFirstName) {
            return new EmployeeRecord(updatedFirstName, middleName, lastName, employeeId);
        }

        private EmployeeRecord withEmployeeId(String generatedEmployeeId) {
            return new EmployeeRecord(firstName, middleName, lastName, generatedEmployeeId);
        }
    }
}
