package com.allenarcher.guest.database

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StayService(
    private val stayRepository: StayRepository,
    private val guestRepository: GuestRepository
) {

    fun createStay(request: CreateStayRequest): StayResponse {
        val guests = guestRepository.findAllById(request.guestIds).toMutableList()
        if (guests.size != request.guestIds.size) {
            throw IllegalArgumentException("One or more guest IDs not found")
        }

        val stay = Stay(checkIn = request.checkIn, checkOut = request.checkOut, guests = guests)
        val invoice = Invoice(
            stay = stay,
            items = request.invoice.items.map { InvoiceItem(name = it.name, price = it.price) }.toMutableList(),
            stateTax = request.invoice.stateTax,
            countyTax = request.invoice.countyTax
        )
        stay.invoice = invoice

        return stayRepository.save(stay).toResponse()
    }

    @Transactional(readOnly = true)
    fun getStaysInRange(from: LocalDate, to: LocalDate): List<StayResponse> =
        stayRepository.findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(from, to)
            .map { it.toResponse() }
}

fun Stay.toResponse() = StayResponse(
    id = id!!,
    checkIn = checkIn,
    checkOut = checkOut,
    guests = guests.map { it.toResponse() },
    invoice = invoice!!.toResponse()
)

fun Invoice.toResponse() = InvoiceResponse(
    id = id!!,
    items = items.map { InvoiceItemResponse(name = it.name, price = it.price) },
    stateTax = stateTax,
    countyTax = countyTax,
    paid = paid
)