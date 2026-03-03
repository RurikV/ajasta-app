package top.ajasta.app.spring.controllers

import org.springframework.web.bind.annotation.*
import top.ajasta.api.v1.models.*
import top.ajasta.app.common.controllerHelper
import top.ajasta.app.spring.config.AjastaAppSettings
import top.ajasta.api.v1.mappers.fromTransport
import top.ajasta.api.v1.mappers.toTransportCreateBooking
import top.ajasta.api.v1.mappers.toTransportReadBooking
import top.ajasta.api.v1.mappers.toTransportUpdateBooking
import top.ajasta.api.v1.mappers.toTransportDeleteBooking
import top.ajasta.api.v1.mappers.toTransportSearchBookings
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource

@RestController
@RequestMapping("v1/bookings")
class BookingControllerV1(
    private val appSettings: AjastaAppSettings,
    private val repoBooking: IRepoBooking,
    private val repoResource: IRepoResource
) {

    @PostMapping("create")
    suspend fun create(@RequestBody request: BookingCreateRequest): BookingCreateResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportCreateBooking() },
            "booking-create",
            repoBooking,
            repoResource
        )

    @PostMapping("read")
    suspend fun read(@RequestBody request: BookingReadRequest): BookingReadResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportReadBooking() },
            "booking-read",
            repoBooking,
            repoResource
        )

    @PostMapping("update")
    suspend fun update(@RequestBody request: BookingUpdateRequest): BookingUpdateResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportUpdateBooking() },
            "booking-update",
            repoBooking,
            repoResource
        )

    @PostMapping("delete")
    suspend fun delete(@RequestBody request: BookingDeleteRequest): BookingDeleteResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportDeleteBooking() },
            "booking-delete",
            repoBooking,
            repoResource
        )

    @PostMapping("search")
    suspend fun search(@RequestBody request: BookingSearchRequest): BookingSearchResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportSearchBookings() },
            "booking-search",
            repoBooking,
            repoResource
        )
}
