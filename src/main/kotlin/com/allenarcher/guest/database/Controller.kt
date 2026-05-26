package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.CreateGuestRequest
import com.allenarcher.guest.database.models.CreateStayRequest
import com.allenarcher.guest.database.models.GuestResponse
import com.allenarcher.guest.database.models.StayResponse
import com.allenarcher.guest.database.services.GuestService
import com.allenarcher.guest.database.services.StayService
import org.springframework.context.annotation.Profile
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class Controller(
    private val stayService: StayService,
    private val guestService: GuestService
) {
    @PostMapping("/guests")
    fun createGuest(@RequestBody request: CreateGuestRequest): GuestResponse =
        guestService.createGuest(request)

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
}

@Profile("test")
@RestController
class TestController(
    private val stayService: StayService,
    private val guestService: GuestService
) {
    @DeleteMapping("/clear")
    fun clear(): String {
        stayService.clear()
        guestService.clear()
        return "Cleared"
    }
}