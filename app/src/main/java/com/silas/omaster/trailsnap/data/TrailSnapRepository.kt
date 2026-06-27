package com.silas.omaster.trailsnap.data

import android.content.Context
import android.net.Uri
import com.silas.omaster.trailsnap.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import kotlin.random.Random

class TrailSnapRepository private constructor(context: Context) {

    private val _photos = MutableStateFlow<List<TrailPhoto>>(emptyList())
    val photos: StateFlow<List<TrailPhoto>> = _photos.asStateFlow()

    private val _albums = MutableStateFlow<List<TrailAlbum>>(emptyList())
    val albums: StateFlow<List<TrailAlbum>> = _albums.asStateFlow()

    private val _faces = MutableStateFlow<List<FaceCluster>>(emptyList())
    val faces: StateFlow<List<FaceCluster>> = _faces.asStateFlow()

    private val _locations = MutableStateFlow<List<LocationPin>>(emptyList())
    val locations: StateFlow<List<LocationPin>> = _locations.asStateFlow()

    private val _tickets = MutableStateFlow<List<TravelTicket>>(emptyList())
    val tickets: StateFlow<List<TravelTicket>> = _tickets.asStateFlow()

    private val _dashboardStats = MutableStateFlow<DashboardStats?>(null)
    val dashboardStats: StateFlow<DashboardStats?> = _dashboardStats.asStateFlow()

    private val _annualReport = MutableStateFlow<AnnualReport?>(null)
    val annualReport: StateFlow<AnnualReport?> = _annualReport.asStateFlow()

    init {
        seedData()
    }

    private fun seedData() {
        val now = LocalDateTime.now()
        val baseDate = now.toLocalDate()

        val cities = listOf(
            "杭州" to "浙江",
            "苏州" to "江苏",
            "黄山" to "安徽",
            "厦门" to "福建",
            "成都" to "四川",
            "西安" to "陕西",
            "北京" to "北京",
            "上海" to "上海"
        )

        val scenes = listOf("人像", "风景", "美食", "夜景", "宠物", "建筑", "街拍", "自然")
        val tagsPool = listOf("旅行", "家庭", "朋友", "工作", "节日", "日常", "户外", "室内")

        val seededPhotos = (1..80).map { index ->
            val dayOffset = Random.nextInt(0, 365)
            val date = baseDate.minusDays(dayOffset.toLong())
            val time = LocalDateTime.of(date, java.time.LocalTime.of(Random.nextInt(6, 22), Random.nextInt(0, 59)))
            val (city, province) = cities.random()
            val scene = scenes.random()
            val tags = tagsPool.shuffled().take(Random.nextInt(1, 4))
            TrailPhoto(
                id = "photo_$index",
                filename = "IMG_${20250000 + index}.jpg",
                photoTime = time,
                width = if (index % 2 == 0) 3000 else 4000,
                height = if (index % 2 == 0) 4000 else 3000,
                mediaType = if (index % 7 == 0) MediaType.VIDEO else MediaType.IMAGE,
                metadata = PhotoMetadata(
                    longitude = 120.0 + Random.nextDouble(-5.0, 5.0),
                    latitude = 30.0 + Random.nextDouble(-5.0, 5.0),
                    city = city,
                    province = province,
                    country = "中国",
                    address = "$city · 某街道",
                    make = if (index % 3 == 0) "OPPO" else if (index % 3 == 1) "Hasselblad" else "Sony",
                    model = if (index % 3 == 0) "Find X9 Pro" else if (index % 3 == 1) "X2D 100C" else "A7M4",
                    shootingParams = ShootingParams(
                        iso = listOf(100, 200, 400, 800, 1600).random(),
                        shutterSpeed = listOf("1/125", "1/250", "1/60", "1/500").random(),
                        aperture = listOf("f/1.8", "f/2.8", "f/4.0", "f/5.6").random(),
                        focalLength = listOf("23mm", "35mm", "50mm", "85mm").random()
                    ),
                    scene = scene
                ),
                tags = tags
            )
        }

        val seededAlbums = listOf(
            TrailAlbum(id = "album_1", name = "2025 春节旅行", coverPhotoId = seededPhotos.firstOrNull()?.id, photoIds = seededPhotos.take(20).map { it.id }),
            TrailAlbum(id = "album_2", name = "杭州记忆", coverPhotoId = seededPhotos.getOrNull(5)?.id, type = AlbumType.CONDITIONAL, condition = AlbumCondition(location = "杭州"), photoIds = seededPhotos.filter { it.metadata?.city == "杭州" }.map { it.id }),
            TrailAlbum(id = "album_3", name = "美食合集", coverPhotoId = seededPhotos.getOrNull(12)?.id, type = AlbumType.SMART, condition = AlbumCondition(scene = "美食"), photoIds = seededPhotos.filter { it.metadata?.scene == "美食" }.map { it.id }),
            TrailAlbum(id = "album_4", name = "家人", coverPhotoId = seededPhotos.getOrNull(30)?.id, type = AlbumType.CONDITIONAL, condition = AlbumCondition(tag = "家庭"), photoIds = seededPhotos.filter { "家庭" in it.tags }.map { it.id })
        )

        val seededFaces = listOf(
            FaceCluster(id = "face_1", name = "我", photoIds = seededPhotos.map { it.id }.take(30)),
            FaceCluster(id = "face_2", name = "妈妈", photoIds = seededPhotos.map { it.id }.drop(30).take(20)),
            FaceCluster(id = "face_3", name = "朋友A", photoIds = seededPhotos.map { it.id }.drop(50).take(15))
        )

        val cityGroups = seededPhotos.groupBy { it.metadata?.city }.filter { it.key != null }
        val seededLocations = cityGroups.map { (city, photos) ->
            LocationPin(
                name = city ?: "未知",
                latitude = photos.firstOrNull()?.metadata?.latitude ?: 0.0,
                longitude = photos.firstOrNull()?.metadata?.longitude ?: 0.0,
                photoCount = photos.size,
                coverPhotoId = photos.firstOrNull()?.id,
                level = LocationLevel.CITY
            )
        }

        val seededTickets = listOf(
            TravelTicket(
                id = "ticket_1",
                type = TicketType.TRAIN,
                departure = "杭州东",
                arrival = "黄山北",
                departureTime = LocalDateTime.of(2025, Month.FEBRUARY, 10, 8, 30),
                arrivalTime = LocalDateTime.of(2025, Month.FEBRUARY, 10, 11, 15),
                ticketNo = "G1234",
                seatInfo = "二等座 07A",
                price = "¥156.0",
                isRecognized = true
            ),
            TravelTicket(
                id = "ticket_2",
                type = TicketType.FLIGHT,
                departure = "上海浦东",
                arrival = "成都双流",
                departureTime = LocalDateTime.of(2025, Month.MAY, 1, 14, 20),
                arrivalTime = LocalDateTime.of(2025, Month.MAY, 1, 17, 45),
                ticketNo = "MU5103",
                seatInfo = "经济舱 23K",
                price = "¥820.0",
                isRecognized = true
            ),
            TravelTicket(
                id = "ticket_3",
                type = TicketType.SCENIC,
                departure = "黄山风景区",
                arrival = "",
                departureTime = LocalDateTime.of(2025, Month.FEBRUARY, 11, 7, 0),
                ticketNo = "HS2025021101",
                price = "¥190.0",
                isRecognized = true
            )
        )

        _photos.value = seededPhotos
        _albums.value = seededAlbums
        _faces.value = seededFaces
        _locations.value = seededLocations
        _tickets.value = seededTickets

        _dashboardStats.value = DashboardStats(
            totalPhotos = seededPhotos.count { it.mediaType == MediaType.IMAGE },
            totalVideos = seededPhotos.count { it.mediaType == MediaType.VIDEO },
            totalAlbums = seededAlbums.size,
            locationCount = seededLocations.size,
            peopleCount = seededFaces.size,
            ticketCount = seededTickets.size,
            earliestPhotoDate = seededPhotos.minOfOrNull { it.photoTime.toLocalDate() },
            latestPhotoDate = seededPhotos.maxOfOrNull { it.photoTime.toLocalDate() }
        )

        val monthly = (1..12).associateWith { month -> seededPhotos.count { it.photoTime.monthValue == month } }
        _annualReport.value = AnnualReport(
            year = 2025,
            totalPhotos = seededPhotos.size,
            totalCities = seededLocations.size,
            totalScenics = Random.nextInt(3, 12),
            totalTrips = seededTickets.size,
            farthestCity = "成都",
            favoriteCity = "杭州",
            seasonStats = mapOf("春" to 22, "夏" to 18, "秋" to 24, "冬" to 16),
            monthlyStats = monthly,
            cityStats = seededLocations.map { CityStat(it.name, null, it.photoCount) }.sortedByDescending { it.photoCount }.take(5),
            highlightPhotoIds = seededPhotos.shuffled().take(9).map { it.id }
        )
    }

    suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        delay(600)
    }

    fun getPhotoById(id: String): TrailPhoto? = _photos.value.find { it.id == id }

    fun getTimelineSections(): List<TimelineSection> {
        return _photos.value
            .sortedByDescending { it.photoTime }
            .groupBy { it.photoTime.toLocalDate() }
            .map { TimelineSection(it.key, it.value.sortedByDescending { p -> p.photoTime }) }
    }

    fun getPhotosByAlbum(albumId: String): List<TrailPhoto> {
        val album = _albums.value.find { it.id == albumId } ?: return emptyList()
        return _photos.value.filter { it.id in album.photoIds }.sortedByDescending { it.photoTime }
    }

    fun getPhotosByLocation(city: String): List<TrailPhoto> {
        return _photos.value.filter { it.metadata?.city == city }.sortedByDescending { it.photoTime }
    }

    fun getPhotosByFace(faceId: String): List<TrailPhoto> {
        val face = _faces.value.find { it.id == faceId } ?: return emptyList()
        return _photos.value.filter { it.id in face.photoIds }.sortedByDescending { it.photoTime }
    }

    fun getToolboxItems(): List<ToolboxItem> {
        val duplicates = _photos.value.groupBy { it.metadata?.city to it.metadata?.scene }.count { it.value.size > 1 }
        return listOf(
            ToolboxItem(ToolboxTool.DUPLICATE_CLEANUP, "重复照片清理", "扫描并合并相似照片", "clean", duplicates),
            ToolboxItem(ToolboxTool.SIMILAR_PHOTOS, "相似照片整理", "按时间线自动归类", "layers", 0),
            ToolboxItem(ToolboxTool.ORGANIZE_BY_DATE, "按日期整理", "一键归档到年月文件夹", "calendar", 0),
            ToolboxItem(ToolboxTool.RENAME_BATCH, "批量重命名", "统一照片命名规则", "edit_3", 0),
            ToolboxItem(ToolboxTool.TIME_FROM_FILENAME, "从文件名恢复时间", "修复缺失的拍摄时间", "clock", 0),
            ToolboxItem(ToolboxTool.RECYCLE_BIN, "回收站", "恢复或彻底删除照片", "trash", _photos.value.count { it.isDeleted })
        )
    }

    companion object {
        @Volatile
        private var instance: TrailSnapRepository? = null

        fun getInstance(context: Context): TrailSnapRepository {
            return instance ?: synchronized(this) {
                instance ?: TrailSnapRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
