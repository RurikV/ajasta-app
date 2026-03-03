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
class BookingControllerV1Test {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    private val stubDebug = Debug(
        mode = RequestDebugMode.STUB,
        stub = RequestDebugStubs.SUCCESS
    )

    @Test
    fun `create booking should return stub response`() {
        val request = BookingCreateRequest(
            requestType = "createBooking",
            debug = stubDebug,
            booking = BookingCreateObject(
                resourceId = "resource-123",
                title = "Test Booking",
                slots = listOf(
                    BookingSlot(
                        slotStart = "2025-03-01T10:00:00Z",
                        slotEnd = "2025-03-01T11:00:00Z",
                        price = 30.0
                    )
                )
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/bookings/create")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("createBooking")
            .jsonPath("$.booking.id").exists()
            .jsonPath("$.paymentLink").exists()
    }

    @Test
    fun `read booking should return stub response`() {
        val request = BookingReadRequest(
            requestType = "readBooking",
            debug = stubDebug,
            booking = BookingReadObject(
                id = "booking-001"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/bookings/read")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("readBooking")
            .jsonPath("$.booking.id").exists()
    }

    @Test
    fun `update booking should return stub response`() {
        val request = BookingUpdateRequest(
            requestType = "updateBooking",
            debug = stubDebug,
            booking = BookingUpdateObject(
                id = "booking-001",
                title = "Updated Title"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/bookings/update")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("updateBooking")
    }

    @Test
    fun `delete booking should return stub response`() {
        val request = BookingDeleteRequest(
            requestType = "deleteBooking",
            debug = stubDebug,
            booking = BookingDeleteObject(
                id = "booking-001"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/bookings/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("deleteBooking")
            .jsonPath("$.booking.id").exists()
    }

    @Test
    fun `search bookings should return stub response`() {
        val request = BookingSearchRequest(
            requestType = "searchBookings",
            debug = stubDebug,
            bookingFilter = BookingFilter(
                resourceId = "resource-tennis-001"
            )
        )

        webTestClient.mutateWith(mockJwt()).post()
            .uri("/v1/bookings/search")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.responseType").isEqualTo("searchBookings")
            .jsonPath("$.bookings").isArray
    }
}
