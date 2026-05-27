package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.*
import com.allenarcher.guest.database.services.BackupService
import com.allenarcher.guest.database.services.GuestService
import com.allenarcher.guest.database.services.StayService
import org.springframework.context.annotation.Profile
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class Controller(
    private val stayService: StayService,
    private val guestService: GuestService,
    private val backupService: BackupService
) {
    @PostMapping("/guests")
    fun createGuest(@RequestBody request: CreateGuestRequest): GuestResponse =
        guestService.createGuest(request)

    @GetMapping("/guests/{externalId}/history")
    fun getGuestHistory(@PathVariable externalId: Long): GuestHistoryResponse =
        guestService.getGuestHistory(externalId)

    @PostMapping("/stays")
    fun createStay(@RequestBody request: CreateStayRequest): StayResponse =
        stayService.createStay(request)

    @GetMapping("/stays")
    fun getStays(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<StayResponse> = stayService.getStaysInRange(from, to)

    @GetMapping("/stays/without-guest")
    fun getStaysWithoutGuest(): List<StayResponse> = stayService.getStaysWithoutGuest()

    @PostMapping("/stays/enrich")
    fun enrichStays(@RequestBody requests: List<EnrichStayRequest>): List<StayResponse> =
        stayService.enrichStays(requests)

    @PostMapping("/stays/{externalId}/cancel")
    fun cancelStay(@PathVariable externalId: Long): StayResponse =
        stayService.cancelStay(externalId)

    @PostMapping("/database/backup")
    fun backup(): String = backupService.backup()
}

@Profile("test")
@RestController
class TestController(
    private val stayService: StayService,
    private val guestService: GuestService
) {
    @DeleteMapping("/database/clear")
    fun clear(): String {
        stayService.clear()
        guestService.clear()
        return "Cleared"
    }
}