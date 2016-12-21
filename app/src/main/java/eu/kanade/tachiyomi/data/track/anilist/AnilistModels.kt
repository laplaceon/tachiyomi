package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.injectLazy

data class ALManga(
        val id: Int,
        val title_romaji: String,
        val type: String,
        val total_chapters: Int) {

    fun toTrack() = Track.create(TrackManager.ANILIST).apply {
        remote_id = this@ALManga.id
        title = title_romaji
        total_chapters = this@ALManga.total_chapters
    }
}

data class ALUserManga(
        val id: Int,
        val list_status: String,
        val score_raw: Int,
        val chapters_read: Int,
        val manga: ALManga) {

    fun toTrack() = Track.create(TrackManager.ANILIST).apply {
        remote_id = manga.id
        status = toTrackStatus()
        score = score_raw.toFloat()
        last_chapter_read = chapters_read
    }

    fun toTrackStatus() = when (list_status) {
        "reading" -> Anilist.READING
        "completed" -> Anilist.COMPLETED
        "on-hold" -> Anilist.ON_HOLD
        "dropped" -> Anilist.DROPPED
        "plan to read" -> Anilist.PLAN_TO_READ
        else -> throw NotImplementedError("Unknown status")
    }
}

data class ALUserLists(val lists: Map<String, List<ALUserManga>>) {

    fun flatten() = lists.values.flatten()
}

fun Track.toAnilistStatus() = when (status) {
    Anilist.READING -> "reading"
    Anilist.COMPLETED -> "completed"
    Anilist.ON_HOLD -> "on-hold"
    Anilist.DROPPED -> "dropped"
    Anilist.PLAN_TO_READ -> "plan to read"
    else -> throw NotImplementedError("Unknown status")
}

private val preferences: PreferencesHelper by injectLazy()

fun Track.toAnilistScore(): String = when (preferences.anilistScoreType().getOrDefault()) {
    // 10 point
    0 -> Math.floor(score.toDouble() / 10).toInt().toString()
    // 100 point
    1 -> score.toInt().toString()
    // 5 stars
    2 -> when {
        score == 0f -> "0"
        score < 30 -> "1"
        score < 50 -> "2"
        score < 70 -> "3"
        score < 90 -> "4"
        else -> "5"
    }
    // Smiley
    3 -> when {
        score == 0f -> "0"
        score <= 30 -> ":("
        score <= 60 -> ":|"
        else -> ":)"
    }
    // 10 point decimal
    4 -> (score / 10).toString()
    else -> throw Exception("Unknown score type")
}