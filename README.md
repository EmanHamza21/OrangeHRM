# OrangeHRM QA Assessment

Java 25, Maven, Selenium 4, and TestNG UI automation for the OrangeHRM public demo at <https://opensource-demo.orangehrmlive.com/>. The test code uses Page Object Model: test classes express user flows while page classes own locators and browser interactions.

## Prerequisites

- Java JDK 25 or newer (`java -version`)
- Apache Maven 3.9 or newer (`mvn -version`)
- Chrome or Firefox installed
- Network access to the OrangeHRM demo and WebDriverManager/Maven Central on first run

WebDriverManager resolves the matching browser driver. Chrome is the default browser. The demo is shared and may change its data, permissions, available leave types, or availability without notice.

## Configuration

Runtime settings are loaded from `src/test/resources/config.properties`. Copy `src/test/resources/config.properties.example` to `src/test/resources/config.properties`, then enter your username and password in that local file. The real config file is Git-ignored to prevent credentials from being committed. Override values with a JVM property or an environment variable prefixed with `ORANGEHRM_`. `baseUrl` must be configured; it has no code-level fallback.

| Setting | Default | Example override |
| --- | --- | --- |
| `baseUrl` | `https://opensource-demo.orangehrmlive.com/` | `ORANGEHRM_BASEURL=https://...` |
| `browser` | `chrome` | `ORANGEHRM_BROWSER=firefox` |
| `headless` | `false` | `ORANGEHRM_HEADLESS=true` |
| `explicitWaitSeconds` | `15` | `-DexplicitWaitSeconds=25` |
| `username` | Required in `config.properties` | `username=...` |
| `password` | Required in `config.properties` | `password=...` |
| `reportPath` | `target/extent-report.html` | `-DreportPath=target/run.html` |

Fill in both credential values in `config.properties`. Do not commit this file or share its contents.

## Framework Layout

```text
src/test/java/com/orangehrm/
	config/       Property and environment configuration
	driver/       Thread-local WebDriver lifecycle
	listeners/    Extent HTML report and failure evidence
	pages/        Login, dashboard, employee, and leave Page Objects
	tests/        TestNG smoke, regression, and negative scenarios
	utils/        Screenshot utilities
src/test/resources/
	config.properties
	testng.xml
```

The driver uses explicit waits and zero implicit wait. Employee results are located by meaningful row content, not row index. Test data uses a unique timestamp suffix. Employee cleanup is attempted after the lifecycle scenario; because the public demo is shared and permissions can vary, cleanup is best-effort. The leave scenario submits a real request and does not automatically cancel it, since cancellation permissions and leave balance availability are account-dependent. Use a dedicated test account where possible and inspect/cancel pending demo requests manually.

## Execute Tests

Run all discovered tests:

```powershell
mvn test
```

Run one class:

```powershell
mvn test -Dtest=EmployeeTest
```

Run the explicit TestNG suite:

```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

Run a TestNG group:

```powershell
mvn test -Dgroups=smoke
mvn test -Dgroups=negative
```

Groups include `smoke`, `regression`, `employee`, `leave`, and `negative`. Individual methods can be selected with Surefire's `-Dtest=ClassName#methodName` syntax.

## Reports and Evidence

ExtentReports writes `target/extent-report.html` with test names, status, duration, failure details, and attached screenshots. Maven Surefire also writes XML/text results under `target/surefire-reports/`. Checkpoint images and automatic failure screenshots are stored in `target/screenshots/`; the report links to them. The generated `target/` artifacts are excluded from Git, so a report and evidence set should be generated from the candidate's actual execution before submission rather than represented by fabricated sample results.

The suite captures evidence at dashboard/login, employee creation, employee search, employee update, required-field validation, and leave submission/negative validation checkpoints. Selenium screenshots require a working graphical/headless browser session.

## Automated Coverage

- Login controls, valid login, dashboard, and logged-in user display.
- Employee create with generated data, generated ID, dynamic search, details, edit, refresh persistence, return-navigation persistence, combined name/ID filters, and filter reset.
- Required-field employee validation; the invalid record is not saved.
- Leave request with a future weekday range, comment, success notification, listed dates, and pending approval status.
- Invalid leave range where the end precedes the start date.
- Best-effort cleanup of the generated employee.

The public demo may not have a usable leave type/balance for the configured account. Such environment-dependent failures should be recorded with the report and not mistaken for a product defect without confirming the account setup.

## Manual QA Assessment: Employee Leave Request

### Questions and assumptions to resolve

- Are leave dates inclusive, and how are weekends, public holidays, work schedules, half-days, and cross-midnight shifts counted?
- Which leave types are available to each employee, and which require an existing balance or supporting document?
- Is a request allowed when its dates overlap another pending or approved request? Can employees request past dates or dates outside the current leave year?
- At what point is balance reserved or deducted: submit, approval, or a later payroll event? How is balance restored after rejection or cancellation?
- Who is the approver, how are missing/inactive approvers handled, and what are the expected approve/reject notifications and audit records?
- Can an employee edit a pending request, cancel it, or cancel only before a cutoff? What happens if an approver acts concurrently?
- What are the timezone, date-format, maximum-range, comment-length, and attachment constraints? Are accessibility and localization requirements in scope?
- What should the employee see when the service fails, the session expires, or a submission is retried after a timeout? How is duplicate submission prevented?

Assumptions for the initial test design: dates are interpreted in the employee's configured work location; a normal full-day request includes both endpoints; a valid new request enters Pending Approval; an unsuccessful submission must not change the balance or create a request. Product owners should confirm these before sign-off.

### Manual scenarios and test cases

| ID | Scenario / data | Expected result |
| --- | --- | --- |
| LV-01 | Open Leave Request as an authenticated eligible employee | Form and available leave types are displayed |
| LV-02 | Submit a valid one-day request with a positive balance | Request is created and appears as Pending Approval |
| LV-03 | Submit a valid multi-day request spanning weekdays | Inclusive/exclusive day count follows the confirmed calendar rules |
| LV-04 | Select each available leave type | Type-specific eligibility, balance, and required fields are correct |
| LV-05 | Enter From Date after To Date | Validation is shown; request and balance remain unchanged |
| LV-06 | Leave a required type/date empty | Field-level validation is shown; no request is created |
| LV-07 | Request weekend and public-holiday dates | Calculated days match the employee work schedule and holiday calendar |
| LV-08 | Request a date in the past or outside allowed period | Defined policy is enforced with a clear message |
| LV-09 | Request more days than available balance | Submission is blocked; balance is unchanged |
| LV-10 | Request dates overlapping pending and approved leave | Conflict policy is enforced; no unintended duplicate is created |
| LV-11 | Submit with a comment at and beyond the length limit | Boundary is accepted; over-limit input is prevented or validated |
| LV-12 | Double-click Submit or retry after a delayed response | Only one request is created and balance is affected once |
| LV-13 | View own request list and open the submitted record | Dates, type, calculated days, comment, and status are correct |
| LV-14 | Cancel a pending request | Status changes per policy and reserved balance is restored once |
| LV-15 | Attempt to cancel approved/rejected request | Action follows permissions and lifecycle rules; no incorrect balance change |
| LV-16 | Approver approves a pending request | Status and balance reflect approval; employee sees the result |
| LV-17 | Approver rejects with/without a reason | Status, reason, notification, and balance follow policy |
| LV-18 | Employee has no active approver or loses permission | Clear recoverable error; no partial request/balance update |
| LV-19 | Refresh/navigate away after successful submit | Request and status remain persisted |
| LV-20 | Session expires or service returns an error during submit | User receives actionable feedback; no duplicate or partial request |

### Defect Report

**Title:** Leave balance is reduced by four days for a three-day request (10-12 August)

**Environment:** OrangeHRM demo/application under test; record build/version, browser, operating system, employee, leave type, work schedule, and configured holiday calendar during reproduction. The year was not supplied in the reported observation and must be confirmed.

**Severity / priority:** Major / High. The displayed duration and balance ledger disagree, risking incorrect employee entitlements and downstream approval/payroll decisions.

**Preconditions:** Employee is signed in, has a known leave balance, and can request the selected leave type. Record the balance before the test and verify the three calendar days are valid working days under the configured schedule.

**Steps to reproduce:**

1. Open Leave Request and select a leave type with sufficient balance.
2. Set From Date to 10 August and To Date to 12 August in the same year.
3. Confirm the form calculates three leave days.
4. Submit the request and inspect the resulting balance/transaction history.

**Expected:** If the dates represent three chargeable workdays, the balance is reduced by three days at the defined lifecycle point. The displayed duration, request record, and balance ledger agree.

**Actual:** The request displays three leave days, but the employee's balance is reduced by four days.

**Frequency / impact:** Reported once; repeat with a controlled test account and capture the before/after balance, transaction history, and request status. Potentially affects every request matching the calendar/leave-type condition.

**Evidence and investigation:** Attach screenshots of the date range and displayed duration, request details/status, and before/after balance; include timestamp, employee ID, leave type, date year, timezone, schedule, and network/request correlation ID. Check whether weekends/holidays are excluded in the duration calculation but included in the balance deduction, whether both endpoints are counted twice, and whether retries or approval transitions apply the debit more than once. Verify ledger/audit entries and compare the calculation and deduction services. Do not repair balances manually until the transaction history is preserved.
