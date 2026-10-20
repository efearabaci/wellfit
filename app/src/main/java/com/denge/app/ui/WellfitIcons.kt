package com.denge.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Wellfit's 24 dp symbol family: one optical size, rounded ends, 1.7 dp strokes. */
object WellfitIcons {
    private fun symbol(name: String, data: String, mirrored: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirrored).apply {
            addPath(PathParser().parsePathString(data).toNodes(), fill = null,
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }.build()

    val Brand = symbol("Wellfit", "M3 6L6.5 18L12 9L17.5 18L21 6M9 6L12 9L15 6")
    val Today = symbol("Today", "M5 4H19Q21 4 21 6V19Q21 21 19 21H5Q3 21 3 19V6Q3 4 5 4ZM7 2V6M17 2V6M3 9H21M8 14L11 17L16 12")
    val Chart = symbol("Chart", "M4 3V20H21M8 15L12 10L16 13L21 6M17 6H21V10")
    val Progress = symbol("Progress", "M3 20V15M9 20V11M15 20V14M21 20V6M3 9L9 4L15 7L21 2")
    val Period = symbol("Period", "M6 4H18Q21 4 21 7V18Q21 21 18 21H6Q3 21 3 18V7Q3 4 6 4ZM8 2V6M16 2V6M3 10H21M7 15H17M7 18H12")
    val Down = symbol("Expand", "M6 9L12 15L18 9")
    val Up = symbol("Collapse", "M6 15L12 9L18 15")
    val Activity = symbol("Activity", "M7 9H17V15H7V9ZM3 7H7V17H3V7ZM17 7H21V17H17V7ZM1 12H3M21 12H23")
    val Nutrition = symbol("Nutrition", "M17 12A5 5 0 1 1 7 12A5 5 0 1 1 17 12M2 3V8Q2 10 4 10M4 3V21M6 3V8Q6 10 4 10M21 21V3Q18 6 18 11H21")
    val WeightCurve = symbol("WeightCurve", "M5 20H19Q21 20 21 18V6Q21 4 19 4H5Q3 4 3 6V18Q3 20 5 20ZM7 9Q12 5 17 9M12 10L15 8M7 16L10 13L13 16L17 12")
    val Compare = symbol("Compare", "M3 7H20M16 3L20 7L16 11M21 17H4M8 13L4 17L8 21")
    val Rising = symbol("Rising", "M5 17L19 5M10 5H19V14")
    val Falling = symbol("Falling", "M5 7L19 19M10 19H19V10")
    val Flat = symbol("Unchanged", "M5 12H19")
    val Memory = symbol("Memory", "M6 3H18Q20 3 20 5V21H6Q3 21 3 18V6Q3 3 6 3ZM6 17H20M7 7H16M7 11H13M6 17Q3 17 3 19")
    val Library = symbol("Library", "M7 3H19Q21 3 21 5V17Q21 19 19 19H7Q5 19 5 17V5Q5 3 7 3ZM2 7V19Q2 22 5 22H17M10 3V10L13 8L16 10V3")
    val Review = symbol("Review", "M14 3H6Q4 3 4 5V19Q4 21 6 21H18Q20 21 20 19V9L14 3ZM14 3V9H20M8 14L10 16L14 12M8 19H16")
    val History = symbol("History", "M3 10A9 9 0 1 1 5 18M3 4V10H9M12 7V12L15 14")
    val Bookmark = symbol("Bookmark", "M7 3H17Q19 3 19 5V22L12 17L5 22V5Q5 3 7 3ZM9 8H15M9 12H13")
    val Export = symbol("Export", "M5 15V19Q5 21 7 21H19Q21 21 21 19V7Q21 5 19 5H15M3 3H11V11M11 3L2 12")
    val Alert = symbol("Alert", "M10 4Q12 1 14 4L22 18Q24 21 20 21H4Q0 21 2 18L10 4ZM12 8V13M12 17H12.01")
    val Ai = symbol("AI", "M6 3H18Q21 3 21 6V18Q21 21 18 21H6Q3 21 3 18V6Q3 3 6 3ZM6.5 16L9 8L11.5 16M7.5 13H10.5M16 8V16")
    val Settings = symbol("Settings", "M4 6H9M15 6H20M4 18H9M15 18H20M4 12H14M20 12H20M15 6A3 3 0 1 1 9 6A3 3 0 1 1 15 6M20 12A3 3 0 1 1 14 12A3 3 0 1 1 20 12M15 18A3 3 0 1 1 9 18A3 3 0 1 1 15 18")
    val Back = symbol("Back", "M15 5L8 12L15 19M8 12H21", true)
    val Left = symbol("Previous", "M14 6L8 12L14 18", true)
    val Right = symbol("Next", "M10 6L16 12L10 18", true)
    val Add = symbol("Add", "M12 5V19M5 12H19")
    val Close = symbol("Close", "M6 6L18 18M18 6L6 18")
    val Check = symbol("Check", "M5 12L10 17L20 7")
    val Complete = symbol("Complete", "M21 11V12A9 9 0 1 1 15.5 3.7M8 11L12 15L21 5")
    val Edit = symbol("Edit", "M15 5L19 9M4 20L9 19L20 8Q22 6 20 4Q18 2 16 4L5 15L4 20ZM13 20H21")
    val Note = symbol("Note", "M13 3H5Q3 3 3 5V19Q3 21 5 21H19Q21 21 21 19V11M8 8H11M8 12H10M13 15L14 11L20 5Q22 3 20 2Q19 1 18 2L12 8M7 17H10")
    val Meals = symbol("Meals", "M5 3V8Q5 11 8 11Q11 11 11 8V3M8 3V21M17 21V3Q21 6 21 12H17")
    val Sun = symbol("Sun", "M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12M12 2V4M12 20V22M2 12H4M20 12H22M5 5L6.5 6.5M17.5 17.5L19 19M5 19L6.5 17.5M17.5 6.5L19 5")
    val Moon = symbol("Moon", "M20.8 13A9 9 0 0 1 11 3.2A9 9 0 1 0 20.8 13Z")
    val Coffee = symbol("Coffee", "M4 8H17V15Q17 19 13 19H8Q4 19 4 15V8ZM17 9H19A3 3 0 0 1 19 15H17M3 22H19M8 2V4M13 2V4")
    val Steps = symbol("Steps", "M8 3Q11 3 11 6V10Q11 12 8 12Q5 12 5 10V6Q5 3 8 3ZM5 15H11V18Q11 20 8 20Q5 20 5 18V15ZM17 7Q20 7 20 10V14Q20 16 17 16Q14 16 14 14V10Q14 7 17 7ZM14 19H20V21Q20 23 17 23Q14 23 14 21V19Z")
    val Weight = symbol("Weight", "M6 3H18Q21 3 21 6V18Q21 21 18 21H6Q3 21 3 18V6Q3 3 6 3ZM7 8Q12 4 17 8L15 12H9L7 8ZM12 9L14 7")
    val Workout = symbol("Workout", "M7 7L17 17M4 4L2 6L6 10L10 6L6 2L4 4ZM14 18L18 14L22 18L18 22L14 18ZM8 4L11 7M4 8L7 11M17 13L20 16M13 17L16 20")
    val Sync = symbol("Sync", "M20 8A8 8 0 0 0 6 5L3 8M3 3V8H8M4 16A8 8 0 0 0 18 19L21 16M16 16H21V21")
    val Rest = symbol("Rest", "M4 11V17H20V11M7 11V5Q7 3 9 3H15Q17 3 17 5V11M4 14H20M6 17V21M18 17V21M2 9Q4 9 4 11M22 9Q20 9 20 11")
    val Shield = symbol("Shield", "M12 2L21 6V12Q21 18 12 22Q3 18 3 12V6L12 2ZM8 12L11 15L16 9")
    val Download = symbol("Download", "M12 3V15M7 10L12 15L17 10M3 16V20Q3 21 5 21H19Q21 21 21 20V16")
    val Cloud = symbol("Cloud", "M7 18A5 5 0 0 1 5 8A7 7 0 0 1 18 7A5.5 5.5 0 0 1 18 18H7Z")
    val Code = symbol("Code", "M7 6L2 12L7 18M17 6L22 12L17 18M14 3L10 21")
    val Link = symbol("Link", "M10 13A4 4 0 0 0 6 7L3 10A4 4 0 0 0 9 16L11 14M14 11A4 4 0 0 0 18 17L21 14A4 4 0 0 0 15 8L13 10M8 16L16 8")
    val External = symbol("External", "M14 3H21V10M21 3L10 14M10 4H5Q3 4 3 6V19Q3 21 5 21H18Q20 21 20 19V14")
    val Arrow = symbol("Arrow", "M4 12H20M14 6L20 12L14 18", true)
    val Calendar = symbol("Calendar", "M5 4H19Q21 4 21 6V19Q21 21 19 21H5Q3 21 3 19V6Q3 4 5 4ZM7 2V6M17 2V6M3 9H21M7 13H9M15 13H17M7 17H9M15 17H17")
    val Week = symbol("Week", "M4 4H20Q22 4 22 6V18Q22 20 20 20H4Q2 20 2 18V6Q2 4 4 4ZM8 4V20M16 4V20")
    val Timer = symbol("Timer", "M20 14A8 8 0 1 1 4 14A8 8 0 1 1 20 14M12 10V14L15 16M9 2H15M12 2V6M18 7L20 5")
    val Clock = symbol("Clock", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M12 6V12L16 14")
    val Bars = symbol("Bars", "M5 20V12M12 20V4M19 20V8")
    val Trend = symbol("Trend", "M3 17L9 11L13 15L21 5M15 5H21V11")
    val Flag = symbol("Flag", "M5 22V3Q10 0 14 3Q18 6 21 3V14Q17 17 13 14Q9 11 5 14")
    val Energy = symbol("Energy", "M13 2L4 14H11L10 22L20 9H13L13 2Z")
    val Award = symbol("Award", "M17 8A5 5 0 1 1 7 8A5 5 0 1 1 17 8M8 12L6 22L12 19L18 22L16 12")
}
