package com.silas.omaster.trailsnap.model

import android.net.Uri
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class MediaType { IMAGE, VIDEO, LIVE_PHOTO }

enum class AlbumType { USER, SMART, CONDITIONAL }

enum class TicketType { TRAIN, FLIGHT, SCENIC, CONCERT, HOTEL, MOVIE, OTHER }

enum class ToolboxTool {
    DUPLICATE_CLEANUP,
    SIMILAR_PHOTOS,
    ORGANIZE_BY_DATE,
    RENAME_BATCH,
    TIME_FROM_FILENAME,
    RECYCLE_BIN
}

data class TrailPhoto(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val filename: String,
    val photoTime: LocalDateTime,
    val uploadTime: LocalDateTime = LocalDateTime.now(),
    val width: Int = 0,
    val height: Int = 0,
    val mediaType: MediaType = MediaType.IMAGE,
    val metadata: PhotoMetadata? = null,
    val thumbnailUri: Uri? = null,
    val isDeleted: Boolean = false,
    val tags: List<String> = emptyList(),
    val faces: List<FaceCluster> = emptyList()
)

data class PhotoMetadata(
    val longitude: Double? = null,
    val latitude: Double? = null,
    val city: String? = null,
    val district: String? = null,
    val province: String? = null,
    val country: String? = null,
    val address: String? = null,
    val make: String? = null,
    val model: String? = null,
    val shootingParams: ShootingParams? = null,
    val scene: String? = null
)

data class ShootingParams(
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null
)

data class TrailAlbum(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val coverPhotoId: String? = null,
    val type: AlbumType = AlbumType.USER,
    val createTime: LocalDateTime = LocalDateTime.now(),
    val photoIds: List<String> = emptyList(),
    val condition: AlbumCondition? = null,
    val numPhotos: Int = photoIds.size
)

data class AlbumCondition(
    val tag: String? = null,
    val scene: String? = null,
    val personId: String? = null,
    val location: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class FaceCluster(
    val id: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val avatarUri: Uri? = null,
    val photoIds: List<String> = emptyList(),
    val photoCount: Int = photoIds.size
)

data class LocationPin(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val photoCount: Int,
    val coverPhotoId: String? = null,
    val level: LocationLevel
)

enum class LocationLevel { CITY, DISTRICT, PROVINCE, COUNTRY, SCENIC }

data class TravelTicket(
    val id: String = UUID.randomUUID().toString(),
    val type: TicketType,
    val departure: String,
    val arrival: String,
    val departureTime: LocalDateTime? = null,
    val arrivalTime: LocalDateTime? = null,
    val ticketNo: String? = null,
    val seatInfo: String? = null,
    val price: String? = null,
    val photoIds: List<String> = emptyList(),
    val isRecognized: Boolean = false
)

data class TimelineSection(
    val date: LocalDate,
    val photos: List<TrailPhoto>
)

data class DashboardStats(
    val totalPhotos: Int,
    val totalVideos: Int,
    val totalAlbums: Int,
    val locationCount: Int,
    val peopleCount: Int,
    val ticketCount: Int,
    val earliestPhotoDate: LocalDate?,
    val latestPhotoDate: LocalDate?
)

data class AnnualReport(
    val year: Int,
    val totalPhotos: Int,
    val totalCities: Int,
    val totalScenics: Int,
    val totalTrips: Int,
    val farthestCity: String?,
    val favoriteCity: String?,
    val seasonStats: Map<String, Int>,
    val monthlyStats: Map<Int, Int>,
    val cityStats: List<CityStat>,
    val highlightPhotoIds: List<String>
)

data class CityStat(
    val city: String,
    val province: String?,
    val photoCount: Int,
    val firstVisit: LocalDate? = null
)

data class ToolboxItem(
    val tool: ToolboxTool,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val badgeCount: Int = 0
)
