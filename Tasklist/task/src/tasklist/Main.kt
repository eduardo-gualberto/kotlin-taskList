package tasklist

import kotlinx.datetime.*
import java.text.SimpleDateFormat
import java.util.Scanner
import kotlin.Exception
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.nio.charset.Charset

enum class Action { ADD, PRINT, EDIT, DELETE, END, UNKNOWN }
enum class Priority { C, H, N, L, UNKNOWN }

data class Task (var priority: Priority, var strDate: String, var strTime: String, var text: String) {
    init {
        require(text.isNotBlank()) { "Task's text is blank." }
    }
}

class ApplicationBroker {
    private val inputReader = InputReader()
    private val outputPrinter = OutputPrinter()

    var tasks = mutableListOf<Task>()

    public fun waitForAction() : Action {
        return inputReader.readAction()
    }

    public fun add() {
        val priority = inputReader.readPriority()
        val date = inputReader.readDate()
        val time = inputReader.readTime()
        val text = inputReader.readText()

        try {
            val task = Task(priority, date, time, text)
            tasks.add(task)
        } catch (e: Exception) {
            return
        }
    }

    public fun print() {
        outputPrinter.outputTasks(tasks)
    }

    public fun end() {
        println("Tasklist exiting!")
    }

    public fun unknown() {
        outputPrinter.outputUnknownAction()
    }

    public fun delete() {
        outputPrinter.outputTasks(tasks, showErrorMessage = false)
        val taskToDelete = inputReader.readTaskToDeleteOrEdit(this.tasks.size)
        if (taskToDelete != null) {
            tasks.removeAt(taskToDelete - 1)
            println("The task is deleted")
        }
    }

    public fun edit() {
        outputPrinter.outputTasks(tasks, showErrorMessage = false)
        val taskToEdit = inputReader.readTaskToDeleteOrEdit(this.tasks.size) ?: return
        when(inputReader.readFieldToEdit()) {
            "priority" -> {
                val priority = inputReader.readPriority()
                tasks[taskToEdit - 1].priority = priority
            }
            "date" -> {
                val date = inputReader.readDate()
                tasks[taskToEdit - 1].strDate = date
            }
            "time" -> {
                val time = inputReader.readTime()
                tasks[taskToEdit - 1].strTime = time
            }
            "task" -> {
                val text = inputReader.readText()
                tasks[taskToEdit - 1].text = text
            }
        }

        println("The task is changed")
    }
}

class InputReader() {
    private val scanner = Scanner(System.`in`)

    public fun readAction() : Action {
        println("Input an action (add, print, edit, delete, end):")
        return when(readln()) {
            "add" -> Action.ADD
            "print" -> Action.PRINT
            "edit" -> Action.EDIT
            "delete" -> Action.DELETE
            "end" -> Action.END
            else -> Action.UNKNOWN
        }
    }


    public fun readText() : String {
        println("Input a new task (enter a blank line to end):")
        var resultTask = ""

        while (scanner.hasNextLine()) {
            val task = scanner.nextLine()

            if (task.isEmpty())
                return resultTask

            if (task.trim().isNotBlank())
                resultTask += "${task.trim()}\n"
            else {
                println("The task is blank")
                return ""
            }
        }
        return ""
    }

    public fun readPriority() : Priority {
        var priority: Priority
        do {
            println("Input the task priority (C, H, N, L):")
            val input = readln().uppercase()
            priority = mapPriority(input)
        } while (priority == Priority.UNKNOWN)
        return priority
    }

    private fun mapPriority(input: String) : Priority {
        val priority = when(input) {
            "C" -> Priority.C
            "H" -> Priority.H
            "N" -> Priority.N
            "L" -> Priority.L
            else -> Priority.UNKNOWN
        }
        return priority
    }

    public fun readDate() : String {
        println("Input the date (yyyy-mm-dd):")
        var date = readln()
        val dateValidator = "[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}".toRegex()
        var parsedDate = validAndParsedDate(date)
        while (!dateValidator.matches(date) || parsedDate == null) {
            println("The input date is invalid")
            println("Input the date (yyyy-mm-dd):")
            date = readln()
            parsedDate = validAndParsedDate(date)
        }
        return parsedDate
    }

    private fun validAndParsedDate(date: String) : String? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        dateFormat.isLenient = false
        return try {
            val formattedDate = dateFormat.parse(date)
            dateFormat.format(formattedDate)
        } catch (e: Exception) {
            null
        }
    }

    public fun readTime() : String {
        println("Input the time (hh:mm):")
        var time = readln()
        val timeValidator = "(0?[0-9]|1[0-9]|2[0-3]):(0?[0-9]|[0-5][0-9])".toRegex()
        while (!timeValidator.matches(time)) {
            println("The input time is invalid")
            println("Input the time (hh:mm):")
            time = readln()
        }

        val timeComponents = time.split(":")
        var formattedTime = ""
        if (timeComponents[0].length < 2)
            formattedTime += "0"
        formattedTime += timeComponents[0]
        formattedTime += ":"
        if (timeComponents[1].length < 2)
            formattedTime += "0"
        formattedTime += timeComponents[1]

        return formattedTime
    }

    public fun readTaskToDeleteOrEdit(totalTasks: Int) : Int? {
        var taskNumber: Int?
        if (totalTasks == 0) {
            println("No tasks have been input")
            return null
        }
        do {
            println("Input the task number (1-${totalTasks}):")

            taskNumber = try {
                readln().toInt()
            } catch (e: Exception) {
                0
            }

            if (taskNumber !in 1..totalTasks)
                println("Invalid task number")

        } while (taskNumber !in 1..totalTasks)

        return taskNumber
    }

    public fun readFieldToEdit() : String {
        val possibleFields = listOf<String>("priority", "date", "time", "task")
        var fieldToEdit: String
        do {
            println("Input a field to edit (priority, date, time, task):")
            fieldToEdit = readln()

            if (!possibleFields.contains(fieldToEdit))
                println("Invalid field")

        } while (!possibleFields.contains(fieldToEdit))
        return fieldToEdit
    }

}

class OutputPrinter() {

    public fun outputTasks(tasks: List<Task>, showErrorMessage: Boolean = true, completion: () -> Unit = {}) {
        if (tasks.isEmpty() && showErrorMessage) {
            println("No tasks have been input")
            return
        } else if (tasks.isEmpty()) {
            return
        }

        var output = ""

        output += printSeparatorLine()
        output += printTableHeader()
        output += printSeparatorLine()

        for ((i, task) in tasks.withIndex()) {
            val line = "|" + " ${i + 1}" + (if (i + 1 < 10) " " else "")
            val dueTag = getTaskDueTag(task)
            val priorityColor = getPriorityColor(task.priority)
            val dueTagColor = getDueTagColor(dueTag)
            output += "$line | ${task.strDate} | ${task.strTime} | $priorityColor | $dueTagColor |" + printTaskFixedSize(task.text)
            output += printSeparatorLine()
        }
        println(output)
        completion()
    }

    public fun outputUnknownAction() = println("The input action is invalid")

    private fun getTaskDueTag(task: Task) : String {
//        if (task.strDate == "2023-05-11") return "T"
        val taskDate = task.strDate.toLocalDate()
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+2")).date
        val numberOfDays = currentDate.daysUntil(taskDate)

        return if (numberOfDays == 0) "T" else if (numberOfDays > 0) "I" else "O"
    }

    private fun printSeparatorLine() : String {
        return "+----+------------+-------+---+---+--------------------------------------------+\n"
    }

    private fun printHeaderFillerLine() : String {
        return "|    |            |       |   |   |"
    }

    private fun printTableHeader() : String {
        return "| N  |    Date    | Time  | P | D |                   Task                     |\n"
    }

    private fun getPriorityColor(priority: Priority) : String {
        return when(priority) {
            Priority.C -> "\u001B[101m \u001B[0m"
            Priority.H -> "\u001B[103m \u001B[0m"
            Priority.N -> "\u001B[102m \u001B[0m"
            else -> "\u001B[104m \u001B[0m"
        }
    }

    private fun getDueTagColor(dueTag: String) : String {
        return when(dueTag) {
            "I" -> "\u001B[102m \u001B[0m"
            "T" -> "\u001B[103m \u001B[0m"
            else -> "\u001B[101m \u001B[0m"
        }
    }

    private fun printTaskFixedSize(taskText: String, size: Int = 44) : String {
        var isFirstOne = true
        var result = ""
        val taskTexts = taskText.split("\n")
        for (text in taskTexts) {
            val chunkedTaskText = text.chunked(size)
            for (chunkedText in chunkedTaskText) {
                if (isFirstOne) {
                    result += chunkedText.padEnd(44) + "|\n"
                    isFirstOne = false
                    continue
                }
                result += printHeaderFillerLine() + chunkedText.padEnd(44) + "|\n"
            }
        }

        return result
    }
}

fun main() {
    // write your code here
    val tasks = readJSONFile()

    val broker = ApplicationBroker()
    if (tasks != null)
        broker.tasks = tasks.toMutableList()

    while (true) {
        when(broker.waitForAction()) {
            Action.ADD -> broker.add()
            Action.PRINT -> broker.print()
            Action.EDIT -> broker.edit()
            Action.DELETE -> broker.delete()
            Action.UNKNOWN -> broker.unknown()
            Action.END -> {
                broker.end()
                writeToJSONFile(broker.tasks)
                return
            }
        }
    }
}

fun readJSONFile(jsonFile: String = "tasklist.json"): List<Task>? {
    val file = File(jsonFile)
    if (!file.exists())
        return null

    val taskListAdapter = moshiTaskListAdapter()
    val jsonString = file.inputStream().readBytes().toString(Charsets.UTF_8)

    if (jsonString.isEmpty())
        return null

    return taskListAdapter.fromJson(jsonString)
}

fun writeToJSONFile(taskList: List<Task>, jsonFile: String = "tasklist.json") {
    if (taskList.isEmpty())
        return

    val taskListAdapter = moshiTaskListAdapter()
    File(jsonFile).writeText(taskListAdapter.toJson(taskList))
}

fun moshiTaskListAdapter(): JsonAdapter<List<Task>> = Moshi.Builder()
                                                        .add(KotlinJsonAdapterFactory())
                                                        .build()
                                                        .adapter<List<Task>>(Types.newParameterizedType(List::class.java, Task::class.java))

