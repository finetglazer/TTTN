// src/main/java/utils/IntegrationTestUtils.java
package com.graduation.integrationtests.utils;

import com.graduation.integrationtests.utils.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import java.util.List;

public class IntegrationTestUtils {

    /**
     * Navigate to a specific page and wait for it to load
     */
    public static void navigateToPage(String url) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        driver.get(url);
        wait.until(ExpectedConditions.urlContains(extractPathFromUrl(url)));
    }

    /**
     * Fill input field by various locator strategies
     */
    public static void fillInput(String locatorType, String locatorValue, String inputValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(inputValue);
    }

    /**
     * Click element with wait
     */
    public static void clickElement(String locatorType, String locatorValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
    }

    /**
     * Select dropdown option
     */
    public static void selectDropdownOption(String locatorType, String locatorValue, String optionText) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        Select dropdown = new Select(element);
        dropdown.selectByVisibleText(optionText);
    }

    /**
     * Get text from element
     */
    public static String getElementText(String locatorType, String locatorValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        return element.getText();
    }

    /**
     * Verify element is present
     */
    public static boolean isElementPresent(String locatorType, String locatorValue) {
        try {
            WebDriver driver = WebDriverUtils.getDriver();
            By locator = getLocator(locatorType, locatorValue);
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for page transition by URL change
     */
    public static void waitForPageTransition(String expectedUrlPart) {
        WebDriverWait wait = WebDriverUtils.getWait();
        wait.until(ExpectedConditions.urlContains(expectedUrlPart));
    }

    /**
     * Get current page URL
     */
    public static String getCurrentUrl() {
        return WebDriverUtils.getDriver().getCurrentUrl();
    }

    /**
     * Extract order ID from URL (for order detail pages)
     */
    public static String extractOrderIdFromUrl() {
        String currentUrl = getCurrentUrl();
        // Extract ORD_XXXXX pattern from URL
        if (currentUrl.contains("ORD_")) {
            return currentUrl.substring(currentUrl.lastIndexOf("ORD_"));
        }
        return null;
    }

    // Helper method to create locators
    private static By getLocator(String locatorType, String locatorValue) {
        switch (locatorType.toLowerCase()) {
            case "id":
                return By.id(locatorValue);
            case "name":
                return By.name(locatorValue);
            case "class":
                return By.className(locatorValue);
            case "css":
                return By.cssSelector(locatorValue);
            case "xpath":
                return By.xpath(locatorValue);
            case "tag":
                return By.tagName(locatorValue);
            default:
                throw new IllegalArgumentException("Locator type not supported: " + locatorType);
        }
    }

    private static String extractPathFromUrl(String url) {
        try {
            return url.substring(url.indexOf('/', 8)); // Skip protocol and domain
        } catch (Exception e) {
            return url;
        }
    }
}