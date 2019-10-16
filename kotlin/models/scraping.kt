package models

import java.time.Duration

data class CatalogEntry(
    val courseName: String,
    val subject: String,
    val courseId: Int,
    val deptCourseNumber: Int,
    val sections: List<CatalogSectionEntry>
)

data class CatalogSectionEntry(
    val registrationNumber: Int,
    val sectionNumber: Int,
    val type: SectionType,
    val meetings: List<Meeting>
)

data class Meeting(
    val instructor: String,
    val begin: DateTime, // Begin date; contains date and time of first event.
    val duration: Duration, // Duration of meeting
    val activeDuration: Duration, // How long after the begin that this event can start. Meetings implicitly meet weekly.
    val days: Days // The days that this meeting happens on
)

data class SectionResult(
  val description: String
)