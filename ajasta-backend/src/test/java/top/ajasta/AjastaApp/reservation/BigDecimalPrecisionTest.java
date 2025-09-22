package top.ajasta.AjastaApp.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.entity.Resource;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalPrecisionTest {

    @Test
    void testBigDecimalPrecisionIssue() {
        System.out.println("[DEBUG_LOG] Testing BigDecimal precision issue - 5.00 becomes 4.97");
        
        // Test different ways of creating BigDecimal with value 5.00
        BigDecimal fromString = new BigDecimal("5.00");
        BigDecimal fromDouble = BigDecimal.valueOf(5.0);
        BigDecimal fromDoubleWithScale = new BigDecimal("5.0").setScale(2, RoundingMode.HALF_UP);
        
        System.out.println("[DEBUG_LOG] From string '5.00': " + fromString + " (scale: " + fromString.scale() + ")");
        System.out.println("[DEBUG_LOG] From double 5.0: " + fromDouble + " (scale: " + fromDouble.scale() + ")");
        System.out.println("[DEBUG_LOG] From double with scale: " + fromDoubleWithScale + " (scale: " + fromDoubleWithScale.scale() + ")");
        
        // Test potential precision loss scenarios
        BigDecimal problematic = new BigDecimal("4.97"); // Simulate some calculation that might cause 4.97
        System.out.println("[DEBUG_LOG] Problematic calculation (5.0 * 0.994): " + problematic);
        
        // Test if JSON serialization/deserialization causes precision loss
        testJsonSerialization();
        
        // Test database column precision simulation
        testDatabasePrecisionSimulation();
    }
    
    @Test
    void testJsonSerialization() {
        System.out.println("[DEBUG_LOG] Testing JSON serialization precision");
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            ResourceDTO dto = ResourceDTO.builder()
                    .name("Test Resource")
                    .type(ResourceType.TURF_COURT)
                    .pricePerSlot(new BigDecimal("5.00"))
                    .build();
            
            System.out.println("[DEBUG_LOG] Original DTO price: " + dto.getPricePerSlot());
            
            // Serialize to JSON
            String json = mapper.writeValueAsString(dto);
            System.out.println("[DEBUG_LOG] JSON: " + json);
            
            // Deserialize back
            ResourceDTO deserialized = mapper.readValue(json, ResourceDTO.class);
            System.out.println("[DEBUG_LOG] Deserialized price: " + deserialized.getPricePerSlot());
            
            assertEquals(new BigDecimal("5.00"), deserialized.getPricePerSlot(), 
                "Price should remain 5.00 after JSON round-trip");
                
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] JSON test error: " + e.getMessage());
            fail("JSON serialization test failed: " + e.getMessage());
        }
    }
    
    @Test
    void testDatabasePrecisionSimulation() {
        System.out.println("[DEBUG_LOG] Testing database precision simulation");
        
        // Simulate database column with precision=10, scale=2
        BigDecimal original = new BigDecimal("5.00");
        
        // Simulate what might happen in database storage/retrieval
        BigDecimal scaled = original.setScale(2, RoundingMode.HALF_UP);
        System.out.println("[DEBUG_LOG] After setScale(2): " + scaled);
        
        // Test potential conversion issues
        double doubleValue = original.doubleValue();
        BigDecimal fromDoubleBack = BigDecimal.valueOf(doubleValue);
        System.out.println("[DEBUG_LOG] Original -> double -> BigDecimal: " + fromDoubleBack);
        
        // Test if converting to string and back causes issues
        String stringValue = original.toString();
        BigDecimal fromStringBack = new BigDecimal(stringValue);
        System.out.println("[DEBUG_LOG] Original -> string -> BigDecimal: " + fromStringBack);
        
        assertEquals(original, fromStringBack, "String round-trip should preserve precision");
    }
    
    @Test 
    void testEntityToDTO() {
        System.out.println("[DEBUG_LOG] Testing Entity to DTO conversion");
        
        Resource entity = Resource.builder()
                .name("Test Resource")
                .type(ResourceType.TURF_COURT)
                .pricePerSlot(new BigDecimal("5.00"))
                .build();
        
        System.out.println("[DEBUG_LOG] Entity price: " + entity.getPricePerSlot());
        
        // Simulate the toDTO conversion from ResourceServiceImpl
        ResourceDTO dto = ResourceDTO.builder()
                .name(entity.getName())
                .type(entity.getType())
                .pricePerSlot(entity.getPricePerSlot())
                .build();
        
        System.out.println("[DEBUG_LOG] DTO price after conversion: " + dto.getPricePerSlot());
        
        assertEquals(new BigDecimal("5.00"), dto.getPricePerSlot(), 
            "Entity to DTO conversion should preserve precision");
    }
    
    @Test
    void testProblematicScenarios() {
        System.out.println("[DEBUG_LOG] Testing scenarios that might cause 4.97");
        
        // Test various calculations that might result in 4.97
        BigDecimal scenario1 = new BigDecimal("5.00").multiply(new BigDecimal("0.994"));
        BigDecimal scenario2 = new BigDecimal("4.97");
        BigDecimal scenario3 = BigDecimal.valueOf(4.970000000000001).setScale(2, RoundingMode.HALF_UP);
        
        System.out.println("[DEBUG_LOG] Scenario 1 (5.00 * 0.994): " + scenario1);
        System.out.println("[DEBUG_LOG] Scenario 2 (5.0 - 0.03): " + scenario2);  
        System.out.println("[DEBUG_LOG] Scenario 3 (4.970000000000001 scaled): " + scenario3);
        
        // Check if any of these equals 4.97
        BigDecimal expected497 = new BigDecimal("4.97");
        if (scenario2.compareTo(expected497) == 0) {
            System.out.println("[DEBUG_LOG] Found the issue! Scenario 2 produces 4.97");
        }
    }
}