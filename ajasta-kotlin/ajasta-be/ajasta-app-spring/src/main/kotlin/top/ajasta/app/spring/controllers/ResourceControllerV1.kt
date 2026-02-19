package top.ajasta.app.spring.controllers

import org.springframework.web.bind.annotation.*
import top.ajasta.api.v1.models.*
import top.ajasta.app.common.controllerHelper
import top.ajasta.app.spring.config.AjastaAppSettings
import top.ajasta.api.v1.mappers.fromTransport
import top.ajasta.api.v1.mappers.toTransportCreateResource
import top.ajasta.api.v1.mappers.toTransportReadResource
import top.ajasta.api.v1.mappers.toTransportUpdateResource
import top.ajasta.api.v1.mappers.toTransportDeleteResource
import top.ajasta.api.v1.mappers.toTransportSearchResources
import top.ajasta.api.v1.mappers.toTransportAvailability
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource

@RestController
@RequestMapping("v1/resources")
class ResourceControllerV1(
    private val appSettings: AjastaAppSettings,
    private val repoBooking: IRepoBooking,
    private val repoResource: IRepoResource
) {

    @PostMapping("create")
    suspend fun create(@RequestBody request: ResourceCreateRequest): ResourceCreateResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportCreateResource() },
            "resource-create",
            repoBooking,
            repoResource
        )

    @PostMapping("read")
    suspend fun read(@RequestBody request: ResourceReadRequest): ResourceReadResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportReadResource() },
            "resource-read",
            repoBooking,
            repoResource
        )

    @PostMapping("update")
    suspend fun update(@RequestBody request: ResourceUpdateRequest): ResourceUpdateResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportUpdateResource() },
            "resource-update",
            repoBooking,
            repoResource
        )

    @PostMapping("delete")
    suspend fun delete(@RequestBody request: ResourceDeleteRequest): ResourceDeleteResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportDeleteResource() },
            "resource-delete",
            repoBooking,
            repoResource
        )

    @PostMapping("search")
    suspend fun search(@RequestBody request: ResourceSearchRequest): ResourceSearchResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportSearchResources() },
            "resource-search",
            repoBooking,
            repoResource
        )

    @PostMapping("availability")
    suspend fun availability(@RequestBody request: AvailabilityRequest): AvailabilityResponse =
        appSettings.controllerHelper(
            { fromTransport(request) },
            { toTransportAvailability() },
            "resource-availability",
            repoBooking,
            repoResource
        )
}
