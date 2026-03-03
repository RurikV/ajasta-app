package top.ajasta.biz.repo

import top.ajasta.biz.BizContext
import top.ajasta.common.helpers.fail
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.repo.DbBookingFilterRequest
import top.ajasta.repo.DbBookingIdRequest
import top.ajasta.repo.DbBookingRequest
import top.ajasta.repo.IDbBookingResponse
import top.ajasta.repo.IDbBookingsResponse

fun ICorChainDsl<BizContext>.repoBookingCreate(title: String) = worker {
    this.title = title
    description = "Adding booking to DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbBookingRequest(bookingRepoPrepare)
        when (val result = repoBooking.createBooking(request)) {
            is IDbBookingResponse.Ok -> bookingRepoDone = result.data
            is IDbBookingResponse.Err -> fail(result.errors)
            is IDbBookingResponse.ErrWithData -> {
                fail(result.errors)
                bookingRepoDone = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoBookingRead(title: String) = worker {
    this.title = title
    description = "Reading booking from DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbBookingIdRequest(bookingValidated.id)
        when (val result = repoBooking.readBooking(request)) {
            is IDbBookingResponse.Ok -> bookingRepoRead = result.data
            is IDbBookingResponse.Err -> fail(result.errors)
            is IDbBookingResponse.ErrWithData -> {
                fail(result.errors)
                bookingRepoRead = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoBookingUpdate(title: String) = worker {
    this.title = title
    description = "Updating booking in DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbBookingRequest(bookingRepoPrepare)
        when (val result = repoBooking.updateBooking(request)) {
            is IDbBookingResponse.Ok -> bookingRepoDone = result.data
            is IDbBookingResponse.Err -> fail(result.errors)
            is IDbBookingResponse.ErrWithData -> {
                fail(result.errors)
                bookingRepoDone = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoBookingDelete(title: String) = worker {
    this.title = title
    description = "Deleting booking from DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbBookingIdRequest(bookingValidated.id, lock = bookingValidated.lock)
        when (val result = repoBooking.deleteBooking(request)) {
            is IDbBookingResponse.Ok -> bookingRepoDone = result.data
            is IDbBookingResponse.Err -> fail(result.errors)
            is IDbBookingResponse.ErrWithData -> {
                fail(result.errors)
                bookingRepoDone = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoBookingSearch(title: String) = worker {
    this.title = title
    description = "Searching bookings in DB with pagination"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbBookingFilterRequest(
            resourceId = bookingFilterValidated.resourceId,
            userId = bookingFilterValidated.userId,
            status = bookingFilterValidated.status,
            dateFrom = bookingFilterValidated.dateFrom,
            dateTo = bookingFilterValidated.dateTo,
            page = page,
            pageSize = pageSize
        )
        when (val result = repoBooking.searchBookings(request)) {
            is IDbBookingsResponse.Ok -> {
                bookingsRepoDone.clear()
                bookingsRepoDone.addAll(result.data)
                total = result.total
            }
            is IDbBookingsResponse.Err -> fail(result.errors)
        }
    }
}
