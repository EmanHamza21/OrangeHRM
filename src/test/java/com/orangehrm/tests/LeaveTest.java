package com.orangehrm.tests;

import com.orangehrm.pages.LeavePage;
import com.orangehrm.pages.MyLeavePage;
import com.orangehrm.listeners.ExtentTestListener;
import com.orangehrm.utils.DateUtils;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.math.BigDecimal;
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
        leavePage.selectLeaveType(LEAVE_TYPE);
        String leaveType = LEAVE_TYPE;

        LocalDate today = DateUtils.today();
        LocalDate start = DateUtils.addWorkingDays(today, 4);
        String startText = start.format(ORANGEHRM_DATE);
        leavePage.enterFromDate(startText);
        leavePage.enterToDate(startText);
        Assert.assertEquals(leavePage.toDate(), startText,
                "The one-day test range should use the same start and end date");
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
        myLeavePage.searchFromDateWithPendingApproval(start);
        myLeavePage.waitForRequest(start, leaveType);
        Assert.assertTrue(myLeavePage.containsRequest(start, leaveType),
                "Submitted leave request should be listed with its date and type");
        Assert.assertTrue(myLeavePage.requestHasStatus(start, leaveType, "Pending Approval"),
                "The submitted leave request should have Pending Approval status");
        Assert.assertTrue(myLeavePage.requestHasCancelButton(start, leaveType),
                "The submitted request row should have a visible Cancel button");
        Assert.assertTrue(myLeavePage.requestHasComment(start, leaveType, comment),
                "The submitted leave request should preserve its comment");
        Assert.assertEquals(new BigDecimal(myLeavePage.requestNumberOfDays(start, leaveType, comment)),
                BigDecimal.ONE,
                "A one-day request should be calculated as one leave day");
        captureCheckpoint("leave-request-verified");

        String cancellationFeedback = myLeavePage.cancelRequest(start, leaveType, comment);
        Assert.assertTrue(cancellationFeedback.toLowerCase(Locale.ROOT).contains("success"),
                "Leave cancellation failed: " + cancellationFeedback);
        myLeavePage.waitForRequestToBeAbsent(start, leaveType, comment);
    }

    @Test(groups = {"negative", "regression", "leave"})
    public void leaveRequestWithEndDateBeforeStartDateIsRejected() {
        login();
        dashboard.openLeave();
        LeavePage leavePage = new LeavePage(driver);
        leavePage.openApply();
        leavePage.selectLeaveType(LEAVE_TYPE);
        String leaveType = LEAVE_TYPE;

        LocalDate start = DateUtils.addWorkingDays(DateUtils.today(), 4);
        LocalDate end = start.minusDays(1);
        String comment = "Invalid leave request " + UUID.randomUUID();
        leavePage.enterFromDate(start.format(ORANGEHRM_DATE));
        leavePage.enterToDate(end.format(ORANGEHRM_DATE));
        leavePage.addComment(comment);
        leavePage.submitRequest();

        Assert.assertTrue(leavePage.hasDateValidation(),
                "An end date before the start date should be rejected with validation");
        captureCheckpoint("leave-invalid-date-validation");

        MyLeavePage myLeavePage = new MyLeavePage(driver);
        myLeavePage.open();
        myLeavePage.searchFromDateWithPendingApproval(start);
        Assert.assertFalse(myLeavePage.containsRequest(start, leaveType, comment),
                "An invalid date range must not create a leave request");
    }

}