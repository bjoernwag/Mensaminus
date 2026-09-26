package com.bjwag.mensaminus.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import android.content.Context
import com.bjwag.mensaminus.R

enum class StatusColor { GREEN, ORANGE, RED, GRAY }
data class CanteenStatus(val text: String, val color: StatusColor)

object OpeningHoursHelper {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    private data class TimeSlot(val start: LocalTime, val end: LocalTime, val isEvening: Boolean = false)

    private val DEFAULT_SLOT = listOf(TimeSlot(LocalTime.of(11, 0), LocalTime.of(14, 30)))

    // hardcoded slots for specific canteens
    // this will break sooner or later and is awful
    // but then again neue mensa took 14 yrs so we good
    private val CANTEEN_SLOTS = mapOf(
        1 to listOf(TimeSlot(LocalTime.of(11, 0), LocalTime.of(15, 0))), // Neue Mensa
        9 to listOf(
            TimeSlot(LocalTime.of(11, 0), LocalTime.of(14, 30)), // Siedepunkt Mittag
            TimeSlot(LocalTime.of(17, 30), LocalTime.of(19, 45), isEvening = true) // Siedepunkt Abend
        ),
    )

    private fun getSlotsForCanteen(canteenId: Int): List<TimeSlot> {
        return CANTEEN_SLOTS[canteenId] ?: DEFAULT_SLOT
    }

    fun getStatus(context: Context, canteenId: Int, date: LocalDate, category: String? = null): CanteenStatus? {
        if (date != LocalDate.now()) return null

        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return CanteenStatus(context.getString(R.string.closed), StatusColor.GRAY)
        }

        val isEveningRequested = category?.contains("Abend", ignoreCase = true) == true
        val allSlots = getSlotsForCanteen(canteenId)

        val slots = if (category != null) allSlots.filter { it.isEvening == isEveningRequested } else allSlots

        if (slots.isEmpty()) return CanteenStatus(context.getString(R.string.closed), StatusColor.GRAY)

        val now = LocalTime.now()

        val currentSlot = slots.find { !now.isBefore(it.start) && now.isBefore(it.end) }
        if (currentSlot != null) {
            val minsToClose = ChronoUnit.MINUTES.between(now, currentSlot.end)
            return when {
                minsToClose <= 30 -> CanteenStatus(context.getString(R.string.closes_in, minsToClose), StatusColor.RED)
                minsToClose <= 60 -> CanteenStatus(context.getString(R.string.closes_at, currentSlot.end.format(formatter)), StatusColor.ORANGE)
                else -> CanteenStatus(context.getString(R.string.closes_at, currentSlot.end.format(formatter)), StatusColor.GREEN)
            }
        }

        val nextSlot = slots.filter { now.isBefore(it.start) }.minByOrNull { it.start }
        if (nextSlot != null) {
            val minsToOpen = ChronoUnit.MINUTES.between(now, nextSlot.start)
            return when {
                minsToOpen <= 30 -> CanteenStatus(context.getString(R.string.opens_in, minsToOpen), StatusColor.ORANGE)
                else -> CanteenStatus(context.getString(R.string.opens_at, nextSlot.start.format(formatter)), StatusColor.GRAY)
            }
        }

        return CanteenStatus(context.getString(R.string.closed), StatusColor.GRAY)
    }
}
