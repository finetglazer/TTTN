// src/main/java/com/graduation/integrationtests/operations/OrderOperations.java
package com.graduation.integrationtests.operations;


import com.graduation.integrationtests.model.OrderInputModel;
import com.graduation.integrationtests.utils.IntegrationTestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Operations class containing all order-related business logic for integration tests
 * This encapsulates the complex workflow steps into reusable methods
 */
@Component
public class OrderOperations {

    @Value("${orders.create.url}")
    private String createOrderUrl;

    @Value("${base.url}")
    private String baseUrl;

    /**
     * STEP 1: Navigate to order creation page
     * Why separate method: Allows reuse and individual testing of navigation
     */
    public void navigateToCreateOrderPage() {
        System.out.println("Step 1: Navigating to create order page...");
        IntegrationTestUtils.navigateToPage(createOrderUrl);

        // Wait for form to be loaded - verify key elements are present
        IntegrationTestUtils.isElementPresent("placeholder", "Enter customer name");
        System.out.println("✅ Successfully navigated to create order page");
    }

    /**
     * STEPS 2-3: Fill order form and add item
     * Why combined: These steps are logically connected - filling form then adding item
     */
    public void fillOrderFormAndAddItem(OrderInputModel orderData) throws Exception{
        System.out.println("Step 2: Filling customer information...");

        // Fill customer information using placeholder locators
        IntegrationTestUtils.fillInput("css", "input[placeholder='Enter customer name']",
                orderData.getCustomerName());

        IntegrationTestUtils.fillInput("css", "input[placeholder='customer@example.com']",
                orderData.getCustomerEmail());

        IntegrationTestUtils.fillInput("css", "input[placeholder='+1 (555) 123-4567']",
                orderData.getCustomerPhone());

        IntegrationTestUtils.fillInput("css", "input[placeholder='123 Main St, City, State']",
                orderData.getCustomerAddress());

        System.out.println("Step 2: Filling product information...");

        // Fill product information
        IntegrationTestUtils.fillInput("css", "input[placeholder='Product name']",
                orderData.getProductName());

        IntegrationTestUtils.fillInput("css", "input[placeholder='0.00']",
                orderData.getProductPrice());

        // Update quantity - clear and set new value
        IntegrationTestUtils.fillInput("css", "input.form-input[type='number'][min='1']",
                String.valueOf(orderData.getQuantity()));

        System.out.println("Step 3: Adding item to order...");

        // Click "Add Item" button
        IntegrationTestUtils.clickElement("css", ".btn-secondary.w-full");

        // Wait for item to be added - could verify item appears in list
        Thread.sleep(1000); // Brief pause for UI update

        System.out.println("✅ Order form filled and item added successfully");
    }

    /**
     * STEP 4: Create the order
     * Why separate: This is a critical action that triggers backend workflow
     */
    public void createOrder() throws Exception{
        System.out.println("Step 4: Creating order...");

        // Click "Create Order" button
        IntegrationTestUtils.clickElement("css", ".btn-primary.w-full.text-lg.py-4");

        // Wait for order creation to complete
        // This might redirect to a success page or dashboard
        Thread.sleep(2000); // Allow time for backend processing

        System.out.println("✅ Order creation initiated");
    }

    /**
     * STEP 5: Navigate to dashboard
     * Why separate: Dashboard navigation is a distinct user action
     */
    public void navigateToDashboard() {
        System.out.println("Step 5: Navigating to dashboard...");
        String dashboardButtonXpath = "//button[contains(text(), 'Go to Dashboard')]";


        // ✅ FIX: Add an explicit wait for the button to be clickable BEFORE the click action.
        // This replaces the unreliable Thread.sleep() from the previous step.
//        System.out.println("Waiting for dashboard button to be clickable...");
//        IntegrationTestUtils.waitForElementToBeClickable("xpath", dashboardButtonXpath);
//        System.out.println("Dashboard button is now clickable.");

        // Click "Go to Dashboard" button
        IntegrationTestUtils.clickElement("xpath", dashboardButtonXpath);

        // Wait for dashboard page to load
//        IntegrationTestUtils.waitForPageTransition("dashboard");

        System.out.println("✅ Successfully navigated to dashboard");
    }


//    /**
//     * STEP 6: Navigate to last page of dashboard
//     * Why separate: Pagination logic can be complex and reusable
//     */
//    public void navigateToLastPageOfDashboard() throws Exception {
//        System.out.println("Step 6: Navigating to the last page of the dashboard...");
//
//        // This XPath robustly finds the last page number button.
//        // It works by finding the 'Next' button, then selecting the container of page numbers
//        // that comes just before it (its preceding sibling div), and finally clicking the last button in that container.
//        String lastPageButtonXPath = "//button[text()='Next']/preceding-sibling::div/button[last()]";
//
//        IntegrationTestUtils.clickElement("xpath", lastPageButtonXPath);
//
//        // Wait for the page to load
//        Thread.sleep(2000);
//
//        System.out.println("✅ Navigated to the last page of the dashboard");
//    }

    /**
     * STEP 6: Navigate to search field and fill the username
     */
    public void checkExistingOrderByUserName(String customerName) throws Exception {
        System.out.println("Step 6: Checking existing order by username...");

        // Use a CSS selector to find the input with the 'search-input' class
        String searchInputCssSelector = ".form-input.search-input";
        IntegrationTestUtils.fillInput("css", searchInputCssSelector, customerName);

        // Wait for the search results to load
        Thread.sleep(2000);

        System.out.println("✅ Searched for customer: " + customerName);
    }

    /**
     * STEP 7: Find order with ORD_ prefix
     * Why separate: Order verification is crucial for test validation
     */
    public String findOrderWithPrefix() {
        System.out.println("Step 7: Finding order with ORD_ prefix...");

        // Look for elements containing "ORD_" text
        String orderId = IntegrationTestUtils.getElementText("xpath",
                "//*[contains(text(), 'ORD_')]");

        if (orderId != null && orderId.startsWith("ORD_")) {
            System.out.println("✅ Found order: " + orderId);
            return orderId;
        } else {
            throw new RuntimeException("❌ Order with ORD_ prefix not found on the page");
        }
    }

    /**
     * STEP 8: Navigate to oderDetails page
     * Why separate: This allows for detailed order inspection
     */
    public void navigateToOrderDetails(String orderId) {
        System.out.println("Step 8: Navigating to order details for " + orderId + "...");

        // Construct the URL for order details
        String orderDetailsUrl = baseUrl + "/orders/ORD_ " + orderId;

        // Navigate to the order details page
        IntegrationTestUtils.navigateToPage(orderDetailsUrl);

        // Wait for the page to load
        IntegrationTestUtils.waitForPageTransition("order-details");

        System.out.println("✅ Successfully navigated to order details for " + orderId);
    }

    /**
     * Complete end-to-end order creation workflow
     * This method orchestrates all the individual steps
     */
    public String executeCompleteOrderCreationWorkflow(OrderInputModel orderData) {
        System.out.println("🚀 Starting complete order creation workflow...");
        System.out.println("Order details: " + orderData.toString());

        try {
            // Execute all steps in sequence
            navigateToCreateOrderPage();
            fillOrderFormAndAddItem(orderData);
            createOrder();
            navigateToDashboard();
            checkExistingOrderByUserName(orderData.getCustomerName());
            String createdOrderId = findOrderWithPrefix();

            System.out.println("🎉 Order creation workflow completed successfully!");
            System.out.println("Created Order ID: " + createdOrderId);

            return createdOrderId;

        } catch (Exception e) {
            System.err.println("❌ Order creation workflow failed: " + e.getMessage());
            throw new RuntimeException("Order creation workflow failed", e);
        }
    }


    /**
     * Utility method to verify order appears in dashboard
     */
    public boolean verifyOrderInDashboard(String expectedOrderId) {
        System.out.println("Verifying order " + expectedOrderId + " appears in dashboard...");

        return IntegrationTestUtils.isElementPresent("xpath",
                "//*[contains(text(), '" + expectedOrderId + "')]");
    }

    /**
     * Performs a double-click on the 'Create Order' button.
     * Used to test idempotency and prevent duplicate submissions.
     */
    public void doubleClickCreateOrder() throws Exception {
        System.out.println("Step 4: Double-clicking create order button...");

        // Use the doubleClickElement utility
        IntegrationTestUtils.doubleClickElement("css", ".btn-primary.w-full.text-lg.py-4");

        // Wait for any backend processing to settle
        Thread.sleep(2000);

        System.out.println("✅ Order creation double-clicked");
    }

    /**
     * Counts the number of orders for a specific customer on the current dashboard view.
     * @param customerName The name of the customer to search for.
     * @return The number of table rows found containing the customer's name.
     */
    public int countOrdersOnDashboardPage(String customerName) {
        System.out.println("Counting orders for customer: " + customerName + " on the current page...");

        // FIX: Changed XPath from 'td' to 'span' to match the new HTML structure.
        // This now correctly finds the customer name within a <span> element.
        String orderSelector = String.format("//span[contains(text(), '%s')]", customerName);

        int count = IntegrationTestUtils.countElements("xpath", orderSelector);
        System.out.println("Found " + count + " orders for " + customerName);
        return count;
    }

    /**
     * Checks if a generic validation error message is visible on the page.
     * This is useful for negative tests where a form submission should fail.
     * We assume the error appears in an element with role="alert".
     * @return true if an error message is present, false otherwise.
     */
    public boolean isValidationErrorDisplayed() {
        // This is a robust way to check for feedback to the user.
        // It looks for any element with role="alert", which is a common
        // accessibility practice for error messages.
        return IntegrationTestUtils.isElementPresent("xpath", "//div[@role='alert']");
    }

}