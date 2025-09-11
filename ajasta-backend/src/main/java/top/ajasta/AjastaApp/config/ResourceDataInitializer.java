package top.ajasta.AjastaApp.config;

import top.ajasta.AjastaApp.reservation.entity.Resource;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.reservation.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceDataInitializer implements CommandLineRunner {

    private final ResourceRepository resourceRepository;

    @Override
    public void run(String... args) throws Exception {
        if (resourceRepository.count() == 0) {
            List<Resource> resources = Arrays.asList(
                    Resource.builder().name("City Turf Court A").type(ResourceType.TURF_COURT).location("Downtown Complex").description("5-a-side turf court with lights").active(true).unitsCount(3).openTime(LocalTime.of(8,0)).closeTime(LocalTime.of(22,0)).build(),
                    Resource.builder().name("Beach Volleyball Court 1").type(ResourceType.VOLLEYBALL_COURT).location("Seaside Park").description("Standard sand court").active(true).unitsCount(2).openTime(LocalTime.of(8,0)).closeTime(LocalTime.of(20,0)).build(),
                    Resource.builder().name("Community Playground").type(ResourceType.PLAYGROUND).location("Greenwood Center").description("Playground area for kids' events").active(true).unitsCount(1).openTime(LocalTime.of(9,0)).closeTime(LocalTime.of(18,0)).build(),
                    Resource.builder().name("Salon Chair 3").type(ResourceType.HAIRDRESSING_CHAIR).location("Main Street Salon").description("Hair styling chair with mirror").active(true).unitsCount(3).openTime(LocalTime.of(10,0)).closeTime(LocalTime.of(19,0)).build()
            );
            resourceRepository.saveAll(resources);
            log.info("Seeded {} resources for booking", resources.size());
        } else {
            log.info("Resources already present, skipping seeding");
        }
    }
}
