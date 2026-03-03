package top.ajasta.app.spring

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.test.web.reactive.server.WebTestClient
import top.ajasta.api.v1.models.*

@SpringBootTest
@AutoConfigureWebTestClient
class ResourceControllerV1Test {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    private val stubDebug = Debug(
        mode = RequestDebugMode.STUB,
        stub = RequestDebugStubs.SUCCESS
    )

    @Test
    fun `create resource should return stub response`() {
        val request = ResourceCreateRequest(
            requestType = "createResource",
            debug = stubDebug,
            resource = ResourceCreateObject(
                name = "Tennis Court A",
                type = ResourceType.TURF_COURT,
                pricePerSlot = 30.0
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/resources/create")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("createResource")
            .jsonPath("$.resource.id").exists()
    }

    @Test
    fun `read resource should return stub response`() {
        val request = ResourceReadRequest(
            requestType = "readResource",
            debug = stubDebug,
            resource = ResourceReadObject(
                id = "resource-001"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/resources/read")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("readResource")
            .jsonPath("$.resource.id").exists()
    }

    @Test
    fun `update resource should return stub response`() {
        val request = ResourceUpdateRequest(
            requestType = "updateResource",
            debug = stubDebug,
            resource = ResourceUpdateObject(
                id = "resource-001",
                name = "Updated Resource"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/resources/update")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("updateResource")
    }

    @Test
    fun `delete resource should return stub response`() {
        val request = ResourceDeleteRequest(
            requestType = "deleteResource",
            debug = stubDebug,
            resource = ResourceDeleteObject(
                id = "resource-001"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/resources/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("deleteResource")
            .jsonPath("$.resource.id").exists()
    }

    @Test
    fun `search resources should return stub response`() {
        val request = ResourceSearchRequest(
            requestType = "searchResources",
            debug = stubDebug,
            resourceFilter = ResourceFilter(
                type = ResourceType.TURF_COURT
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/resources/search")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("searchResources")
            .jsonPath("$.resources").isArray
    }

    @Test
    fun `availability should return stub response`() {
        val request = AvailabilityRequest(
            requestType = "getAvailability",
            debug = stubDebug,
            resourceId = "resource-001",
            dateFrom = "2025-03-01T00:00:00Z",
            dateTo = "2025-03-01T23:59:59Z"
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/resources/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("getAvailability")
            .jsonPath("$.slots").isArray
    }
}
