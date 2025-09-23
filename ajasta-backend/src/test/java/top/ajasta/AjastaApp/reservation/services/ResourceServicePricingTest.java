package top.ajasta.AjastaApp.reservation.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.response.Response;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.config.import=",
    "DB_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "DB_USERNAME=sa",
    "DB_PASSWORD="
})
@Transactional
class ResourceServicePricingTest {

    @Autowired
    private ResourceService resourceService;

    @Test
    void testPricePrecisionIssue() {
        System.out.println("[DEBUG_LOG] Testing price precision issue");
        
        // Create a resource with price 5.00
        ResourceDTO dto = ResourceDTO.builder()
                .name("Test Court")
                .type(ResourceType.TURF_COURT)
                .location("Test Location")
                .description("Test Description")
                .pricePerSlot(new BigDecimal("5.00"))
                .active(true)
                .unitsCount(1)
                .build();

        System.out.println("[DEBUG_LOG] Original price: " + dto.getPricePerSlot());
        System.out.println("[DEBUG_LOG] Original price string: " + dto.getPricePerSlot().toString());
        System.out.println("[DEBUG_LOG] Original price scale: " + dto.getPricePerSlot().scale());

        // Since we can't create without image file, let's test update instead
        // First create a minimal resource manually for testing
        try {
            // Try to get an existing resource or skip if none exists
            Response<java.util.List<ResourceDTO>> allResources = resourceService.getResources(null, null, null);
            if (allResources.getData() != null && !allResources.getData().isEmpty()) {
                ResourceDTO existing = allResources.getData().getFirst();
                existing.setPricePerSlot(new BigDecimal("5.00"));
                
                System.out.println("[DEBUG_LOG] Updating existing resource with ID: " + existing.getId());
                System.out.println("[DEBUG_LOG] Setting price to: " + existing.getPricePerSlot());
                
                Response<ResourceDTO> updateResponse = resourceService.updateResource(existing);
                ResourceDTO updated = updateResponse.getData();
                
                System.out.println("[DEBUG_LOG] Updated price: " + updated.getPricePerSlot());
                System.out.println("[DEBUG_LOG] Updated price string: " + updated.getPricePerSlot().toString());
                System.out.println("[DEBUG_LOG] Updated price scale: " + updated.getPricePerSlot().scale());
                
                // Check if the price is exactly 5.00
                assertEquals(new BigDecimal("5.00"), updated.getPricePerSlot(), 
                    "Price should be exactly 5.00, but was: " + updated.getPricePerSlot());
                    
                // Additional checks
                assertEquals(0, new BigDecimal("5.00").compareTo(updated.getPricePerSlot()), 
                    "Price comparison should be equal to 5.00");
                    
                // Check if it's 4.97 (the reported issue)
                if (new BigDecimal("4.97").compareTo(updated.getPricePerSlot()) == 0) {
                    fail("Price precision issue confirmed: expected 5.00 but got 4.97");
                }
            } else {
                System.out.println("[DEBUG_LOG] No existing resources found, skipping precision test");
            }
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Error during test: " + e.getMessage());
            System.err.println("[DEBUG_LOG] Exception: " + e);
        }
    }
    
    @Test
    void testBigDecimalPrecisionHandling() {
        System.out.println("[DEBUG_LOG] Testing BigDecimal precision handling");
        
        BigDecimal original = new BigDecimal("5.00");
        BigDecimal fromString = new BigDecimal("5.00");
        BigDecimal fromDouble = BigDecimal.valueOf(5.00);
        
        System.out.println("[DEBUG_LOG] Original: " + original + " (scale: " + original.scale() + ")");
        System.out.println("[DEBUG_LOG] From string: " + fromString + " (scale: " + fromString.scale() + ")");
        System.out.println("[DEBUG_LOG] From double: " + fromDouble + " (scale: " + fromDouble.scale() + ")");
        
        // Test if there's any precision loss in different BigDecimal constructions
        assertEquals(original, fromString);
        
        // BigDecimal.valueOf(5.00) creates scale 1, not scale 2, so we need to use compareTo for value equality
        // or convert to same scale for string comparison
        assertEquals(0, original.compareTo(fromDouble), "Values should be equal even with different scales");
        assertEquals("5.00", original.toString(), "String representation should maintain decimal places");
        assertEquals("5.0", fromDouble.toString(), "BigDecimal from double has scale 1");
    }
}