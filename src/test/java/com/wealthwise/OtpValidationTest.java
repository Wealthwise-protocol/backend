package com.wealthwise;

import java.util.regex.Pattern;

public class OtpValidationTest {

    private static final Pattern OTP_PATTERN = Pattern.compile("^[0-9]{6}$");

    public static void main(String[] args) {
        System.out.println("=== OTP Validation Test ===\n");
        
        // Test valid OTPs
        System.out.println("Test 1: Valid OTPs (should PASS)");
        testOtp("123456", true);
        testOtp("000000", true);
        testOtp("999999", true);
        testOtp("000123", true);
        testOtp("847293", true);
        
        // Test invalid OTPs
        System.out.println("\nTest 2: Invalid OTPs (should FAIL)");
        testOtp("12345", false);   // Too short
        testOtp("1234567", false); // Too long
        testOtp("abc123", false);  // Contains letters
        testOtp("12-456", false);  // Contains special char
        testOtp("", false);        // Empty
        testOtp(" 123456", false); // Leading space
        testOtp("123456 ", false); // Trailing space
        testOtp("12 3456", false); // Space in middle
        
        System.out.println("\n=== All Validation Tests Completed ===");
    }
    
    private static void testOtp(String otp, boolean shouldBeValid) {
        boolean isValid = OTP_PATTERN.matcher(otp).matches();
        String status = (isValid == shouldBeValid) ? "✅ PASS" : "❌ FAIL";
        String expected = shouldBeValid ? "VALID" : "INVALID";
        String actual = isValid ? "VALID" : "INVALID";
        
        System.out.printf("%s | OTP: %-10s | Expected: %-7s | Actual: %-7s%n", 
            status, "\"" + otp + "\"", expected, actual);
    }
}
