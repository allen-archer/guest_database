package com.allenarcher.guest.database

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
}