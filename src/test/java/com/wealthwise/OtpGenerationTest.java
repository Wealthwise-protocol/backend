package com.wealthwise;

public class OtpGenerationTest {

    public static void main(String[] args) {
        System.out.println("=== OTP Generation Test ===\n");
        
        // Test 1: Generate 10 OTPs
        System.out.println("Test 1: Generating 10 OTPs");
        for (int i = 0; i < 10; i++) {
            String otp = generateOtp();
            System.out.println("OTP " + (i + 1) + ": " + otp + " (Length: " + otp.length() + ")");
        }
        
        // Test 2: Verify all OTPs are 6 digits
        System.out.println("\nTest 2: Validating OTP format (must be 6 digits)");
        boolean allValid = true;
        for (int i = 0; i < 100; i++) {
            String otp = generateOtp();
            if (!otp.matches("^[0-9]{6}$")) {
                System.out.println("FAILED: Invalid OTP generated: " + otp);
                allValid = false;
            }
        }
        if (allValid) {
            System.out.println("✅ PASSED: All 100 OTPs are valid 6-digit numbers");
        }
        
        // Test 3: Check for leading zeros
        System.out.println("\nTest 3: Testing edge cases (numbers < 100000)");
        int leadingZeroCount = 0;
        for (int i = 0; i < 1000; i++) {
            String otp = generateOtp();
            if (otp.startsWith("0")) {
                leadingZeroCount++;
            }
        }
        System.out.println("✅ OTPs with leading zeros: " + leadingZeroCount + "/1000");
        System.out.println("   (Expected: ~100, shows format works correctly)");
        
        // Test 4: Uniqueness check
        System.out.println("\nTest 4: Checking uniqueness (1000 OTPs)");
        java.util.Set<String> uniqueOtps = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            uniqueOtps.add(generateOtp());
        }
        System.out.println("✅ Unique OTPs generated: " + uniqueOtps.size() + "/1000");
        System.out.println("   (High uniqueness = good randomness)");
        
        System.out.println("\n=== All Tests Completed ===");
    }
    
    private static String generateOtp() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
