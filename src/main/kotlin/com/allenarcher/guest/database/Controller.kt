package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.*
import com.allenarcher.guest.database.services.BackupService
import com.allenarcher.guest.database.services.GuestService
import com.allenarcher.guest.database.services.StayService
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(Controller::class.java)

    @PostMapping("/guests")
    fun createGuest(@RequestBody request: CreateGuestRequest): GuestResponse =
        guestService.createGuest(request).also { logger.info("Created guest ${it.name}") }

    @GetMapping("/guests/{externalId}/history")
    fun getGuestHistory(@PathVariable externalId: Long): GuestHistoryResponse =
        guestService.getGuestHistory(externalId)

    @GetMapping("/guests/search")
    fun searchGuests(
        @RequestParam name: String?,
        @RequestParam email: String?,
        @RequestParam city: String?,
        @RequestParam state: String?,
        @RequestParam street: String?,
        @RequestParam phone: String?,
        @RequestParam zip: String?
    ): List<GuestSearchResponse> = guestService.searchGuests(name, email, city, state, street, phone, zip)

    @PostMapping("/stays/upsert")
    fun upsertStays(@RequestBody requests: List<UpsertStayRequest>): List<StayResponse> =
        stayService.upsertStays(requests).also { logger.info("Upserted stays with confirmationCodes = ${it.joinToString(", ") { stay -> "${stay.confirmationCode}" }}") }

    @PostMapping("/stays/upsert-by-confirmation")
    fun upsertByConfirmation(@RequestBody request: UpsertByConfirmationRequest): StayResponse =
        stayService.upsertByConfirmation(request)
            .also { logger.info("Upserted stay by confirmationCode = ${it.confirmationCode}") }

    @GetMapping("/stays")
    fun getStays(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<StayResponse> = stayService.getStaysInRange(from, to)

    @GetMapping("/stays/briefing")
    fun getStaysBriefing(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<StayBriefingResponse> = stayService.getStaysBriefing(from, to)

    @GetMapping("/stays/without-guest")
    fun getStaysWithoutGuest(): List<StayResponse> = stayService.getStaysWithoutGuest()

    @PostMapping("/stays/update-invoice")
    fun updateInvoice(@RequestBody request: UpdateInvoiceRequest): StayResponse =
        stayService.updateInvoice(request).also { logger.info("Updated invoice for stay ${it.confirmationCode}") }

    @PostMapping("/stays/cancel")
    fun cancelStay(@RequestBody rooms: List<RoomDateRequest>): StayResponse =
        stayService.cancelStay(rooms).also { logger.info("Cancelled stay with confirmationCode = ${it.confirmationCode}") }

    @GetMapping("/stays/last-by-room")
    fun getLastStaysByRoom(): List<LastByRoomResponse> = stayService.getLastStaysByRoom()

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