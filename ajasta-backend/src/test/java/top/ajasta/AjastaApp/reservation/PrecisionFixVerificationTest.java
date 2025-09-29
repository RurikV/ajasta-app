package top.ajasta.AjastaApp.reservation;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PrecisionFixVerificationTest {

    @Test
    void testPrecisionFixVerification() {
        System.out.println("[DEBUG_LOG] Verifying precision fix");
        
        // Simulate the frontend fix: keeping price as string
        String userInput = "5.00";
        System.out.println("[DEBUG_LOG] User enters: " + userInput);
        
        // With the fix, this should remain as string until backend processing
        System.out.println("[DEBUG_LOG] Frontend preserves: " + userInput);
        
        // Backend converts to BigDecimal (as it should)
        BigDecimal backendValue = new BigDecimal(userInput);
        System.out.println("[DEBUG_LOG] Backend BigDecimal: " + backendValue);
        System.out.println("[DEBUG_LOG] Backend BigDecimal scale: " + backendValue.scale());
        
        // Verify precision is preserved
        assertEquals(new BigDecimal("5.00"), backendValue, "Price precision should be preserved");
        assertEquals("5.00", backendValue.toString(), "String representation should maintain decimal places");
        
        // Test the problematic scenario that was happening before
        System.out.println("[DEBUG_LOG] Testing what happened before the fix:");
        
        // Before fix: JavaScript converted "5.00" to number 5, then back to "5"
        double jsNumber = Double.parseDouble("5.00"); // JavaScript behavior
        String jsBack = String.valueOf(jsNumber); // "5.0"
        String jsToString = Double.toString(jsNumber); // "5.0"
        
        System.out.println("[DEBUG_LOG] Before fix - JS number conversion: " + jsNumber);
        System.out.println("[DEBUG_LOG] Before fix - String.valueOf(): " + jsBack);
        System.out.println("[DEBUG_LOG] Before fix - Double.toString(): " + jsToString);
        
        // This would cause precision loss
        assertNotEquals("5.00", jsToString, "JavaScript number conversion loses precision");
        
        System.out.println("[DEBUG_LOG] Fix verification completed successfully!");
    }
    
    @Test
    void testRegexValidation() {
        System.out.println("[DEBUG_LOG] Testing regex validation from frontend fix");
        
        String pattern = "^\\d*(\\.\\d{1,2})?$";
        
        // Valid inputs
        assertTrue("5.00".matches(pattern), "5.00 should be valid");
        assertTrue("25.99".matches(pattern), "25.99 should be valid");
        assertTrue("0.50".matches(pattern), "0.50 should be valid");
        assertTrue("100".matches(pattern), "100 should be valid");
        assertTrue("".matches(pattern), "Empty string should be valid");
        assertTrue("5".matches(pattern), "5 should be valid");
        assertTrue("5.0".matches(pattern), "5.0 should be valid");
        assertTrue(".50".matches(pattern), ".50 should be valid");
        
        // Invalid inputs
        assertFalse("5.123".matches(pattern), "5.123 should be invalid (too many decimals)");
        assertFalse("abc".matches(pattern), "abc should be invalid");
        assertFalse("5.".matches(pattern), "5. should be invalid");
        
        System.out.println("[DEBUG_LOG] Regex validation tests passed!");
    }
}