import kotlinx.datetime.*
import kotlin.time.Duration

fun nextMonth(date: String): String {
    // Write your code here
    val instant = date.toInstant().plus(1, DateTimeUnit.MONTH, TimeZone.UTC)
    return instant.toString()
    //
}

fun main() {
    val date = readln()
    println(nextMonth(date))
}