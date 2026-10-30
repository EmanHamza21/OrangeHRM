package com.orangehrm.tests;

import com.orangehrm.pages.LeavePage;
import com.orangehrm.pages.MyLeavePage;
import com.orangehrm.listeners.ExtentTestListener;
import com.orangehrm.utils.DateUtils;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Listeners(ExtentTestListener.class)
public class LeaveTest extends BaseTest {
    private static final DateTimeFormatter ORANGEHRM_DATE = DateTimeFormatter.ofPattern("yyyy-dd-MM");
    private static final String LEAVE_TYPE = "US - Vacation";

    @Test(groups = {"regression", "leave"})
    public void validLeaveRequestCanBeSubmittedAndLocated() {
        login();
        dashboard.openLeave();
        LeavePage leavePage = new LeavePage(driver);
        leavePage.openApply();
        Assert.assertTrue(leavePage.isApplyPageDisplayed(),
                "Apply page URL and Apply Leave title should be present before form interaction");
        Assert.assertTrue(leavePage.hasLeaveTypeOptions(), "Leave type selector should be available");
        leavePage.selectLeaveType(LEAVE_TYPE);

        LocalDate today = DateUtils.today();
        LocalDate start = DateUtils.addWorkingDays(today, 4);
        String startText = start.format(ORANGEHRM_DATE);
        leavePage.enterFromDate(startText);
        String endText = leavePage.toDate();
        Assert.assertFalse(endText.isBlank(), "To Date should be populated by the system");
        Assert.assertTrue(leavePage.isFullDayDurationSelected(),
                "Duration should default to Full Day");
        String comment = "Automated assessment leave request " + UUID.randomUUID();
        leavePage.addComment(comment);
        leavePage.submitRequest();

        String feedback = leavePage.waitForSubmissionFeedback();
        Assert.assertTrue(feedback.toLowerCase(Locale.ROOT).contains("success"),
                "Leave submission was rejected: " + feedback);
        captureCheckpoint("leave-submitted");

        MyLeavePage myLeavePage = new MyLeavePage(driver);
        myLeavePage.open();
        Assert.assertTrue(myLeavePage.isDisplayed(),
                "My Leave URL and search controls should be present before searching");
        myLeavePage.searchFromDateWithPendingApproval(today);
        myLeavePage.waitForRequest(start, LEAVE_TYPE);
        Assert.assertTrue(myLeavePage.containsRequest(start, LEAVE_TYPE),
                "Submitted leave request should be listed with its date and type");
        Assert.assertTrue(myLeavePage.requestHasStatus(start, LEAVE_TYPE, "Pending Approval"),
                "The submitted leave request should have Pending Approval status");
        Assert.assertTrue(myLeavePage.requestHasCancelButton(start, LEAVE_TYPE),
                "The submitted request row should have a visible Cancel button");
    }

    @Test(groups = {"negative", "regression", "leave"})
    public void leaveRequestWithEndDateBeforeStartDateIsRejected() {
        login();
        dashboard.openLeave();
        LeavePage leavePage = new LeavePage(driver);
        leavePage.openApply();
        leavePage.selectLeaveType(LEAVE_TYPE);

        LocalDate start = DateUtils.addWorkingDays(DateUtils.today(), 4);
        LocalDate end = start.minusDays(1);
        leavePage.enterFromDate(start.format(ORANGEHRM_DATE));
        leavePage.enterToDate(end.format(ORANGEHRM_DATE));
        leavePage.submitRequest();

        Assert.assertTrue(leavePage.hasDateValidation(),
                "An end date before the start date should be rejected with validation");
        captureCheckpoint("leave-invalid-date-validation");
    }

}