package com.silas.omaster.trailsnap.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.silas.omaster.trailsnap.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/**
 * 行影集数据仓库
 *
 * 真实数据源：Android MediaStore
 * - 读取系统图库全部图片/视频
 * - 提取 EXIF 经纬度、拍摄参数
 * - 按城市聚类生成足迹（支持地理反编码获取真实地名）
 * - 按日期/位置/场景自动生成相册
 * - 使用 ML Kit 进行真实人脸检测与票据 OCR
 */
class TrailSnapRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _favorites = MutableStateFlow<List<TrailPhoto>>(emptyList())
    val favorites: StateFlow<List<TrailPhoto>> = _favorites.asStateFlow()

    private val faceDetectionProcessed = mutableSetOf<String>()

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .build()
    )

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        repositoryScope.launch {
            loadLocalMedia()
        }
    }

    /**
     * 刷新：重新扫描 MediaStore
     */
    suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        loadLocalMedia()
    }

    /**
     * 从 MediaStore 加载本地照片和视频
     */
    suspend fun loadLocalMedia(): Unit = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.BUCKET_ID
            )

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
            val loadedPhotos = mutableListOf<TrailPhoto>()

            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                // DATA 列在 Android 10+ 已废弃且可能为空，使用 getColumnIndex 兜底
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

                var index = 0
                while (cursor.moveToNext() && index < 2000) {
                    if (idCol < 0) continue
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "IMG_$id.jpg" else "IMG_$id.jpg"
                    val dateTaken = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Camera" else "Camera"

                    val mediaType = when {
                        mimeType.startsWith("video/") -> MediaType.VIDEO
                        mimeType.contains("live") || name.contains("LIVE") -> MediaType.LIVE_PHOTO
                        else -> MediaType.IMAGE
                    }

                    val photoTime = if (dateTaken > 0) {
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTaken), ZoneId.systemDefault())
                    } else {
                        LocalDateTime.now()
                    }

                    val metadata = extractMetadataFromExif(contentUri, dataPath)

                    loadedPhotos.add(
                        TrailPhoto(
                            id = id.toString(),
                            uri = contentUri,
                            thumbnailUri = contentUri,
                            filename = name,
                            photoTime = photoTime,
                            width = width,
                            height = height,
                            size = size,
                            mediaType = mediaType,
                            metadata = metadata,
                            tags = listOf(bucketName)
                        )
                    )
                    index++
                }
            }

            _photos.value = loadedPhotos
            rebuildDerivedData(loadedPhotos)

            // 异步执行真实 ML Kit 人脸检测与票据 OCR
            detectFacesAndCluster(loadedPhotos)
            recognizeTickets(loadedPhotos)
        } catch (e: SecurityException) {
            _error.value = "缺少存储权限，请在设置中开启"
        } catch (e: Exception) {
            _error.value = "加载失败：${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 从 EXIF 提取元数据（优先通过 ContentUri 读取，兼容 Android 10+ 作用域存储）
     */
    private suspend fun extractMetadataFromExif(uri: Uri?, dataPath: String?): PhotoMetadata? {
        val filename = dataPath?.substringAfterLast('/') ?: uri?.toString() ?: "unknown"
        val exif = try {
            when {
                uri != null -> {
                    val stream = try {
                        contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to open input stream for $filename", e)
                        null
                    }
                    stream?.use { ExifInterface(it) }
                }
                dataPath != null -> {
                    val file = File(dataPath)
                    if (file.exists()) ExifInterface(file) else null
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF for $filename", e)
            null
        } ?: return null

        val latLong = try {
            val arr = FloatArray(2)
            if (exif.getLatLong(arr)) arr else null
        } catch (_: Exception) { null }
        val make = try { exif.getAttribute(ExifInterface.TAG_MAKE) } catch (_: Exception) { null }
        val model = try { exif.getAttribute(ExifInterface.TAG_MODEL) } catch (_: Exception) { null }
        val iso = try { exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.toIntOrNull() } catch (_: Exception) { null }
        val focalLength = try { exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) } catch (_: Exception) { null }
        val aperture = try { exif.getAttribute(ExifInterface.TAG_F_NUMBER) } catch (_: Exception) { null }
        val exposureTime = try { exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) } catch (_: Exception) { null }

        val locationInfo = latLong?.let {
            try { resolveLocationName(it[0].toDouble(), it[1].toDouble()) } catch (_: Exception) { null }
        }

        val scene = try { inferScene(exif) } catch (_: Exception) { null }

        return PhotoMetadata(
            longitude = latLong?.get(1)?.toDouble(),
            latitude = latLong?.get(0)?.toDouble(),
            city = locationInfo?.city,
            district = locationInfo?.district,
            province = locationInfo?.province,
            country = locationInfo?.country,
            address = locationInfo?.address,
            make = make,
            model = model,
            shootingParams = ShootingParams(
                iso = iso,
                shutterSpeed = exposureTime?.let { formatExposureTime(it) },
                aperture = aperture?.let { "f/$it" },
                focalLength = focalLength?.let { "${it}mm" }
            ),
            scene = scene
        )
    }

    private data class LocationInfo(
        val city: String?,
        val district: String?,
        val province: String?,
        val country: String?,
        val address: String?
    )

    private suspend fun resolveLocationName(latitude: Double, longitude: Double): LocationInfo? {
        return try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用异步 GeocodeListener
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val info = addresses.firstOrNull()?.let {
                            LocationInfo(
                                city = it.locality ?: it.subAdminArea,
                                district = it.subLocality,
                                province = it.adminArea,
                                country = it.countryName,
                                address = it.getAddressLine(0)
                            )
                        }
                        continuation.resume(info, onCancellation = null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let {
                    LocationInfo(
                        city = it.locality ?: it.subAdminArea,
                        district = it.subLocality,
                        province = it.adminArea,
                        country = it.countryName,
                        address = it.getAddressLine(0)
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatExposureTime(exposureTime: String): String {
        return try {
            val seconds = exposureTime.toDouble()
            when {
                seconds >= 1.0 -> "${seconds.toInt()}s"
                seconds > 0 -> "1/${(1.0 / seconds).toInt()}"
                else -> exposureTime
            }
        } catch (_: Exception) {
            exposureTime
        }
    }

    private fun inferScene(exif: ExifInterface): String? {
        return when {
            exif.getAttribute(ExifInterface.TAG_FLASH)?.contains("1") == true -> "夜景"
            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.toFloatOrNull()?.let { it < 30 } == true -> "街拍"
            else -> null
        }
    }

    /**
     * 使用 ML Kit Face Detection 检测人脸并简单聚类
     *
     * 实现说明：
     * - 对每张照片进行人脸检测
     * - 将检测到的人脸区域裁剪并保存为缓存头像
     * - 使用简单特征（宽高比、平均肤色）进行聚类分组
     * - 返回真实 FaceCluster，avatarUri 指向真实缓存图片
     */
    private suspend fun detectFacesAndCluster(photos: List<TrailPhoto>) {
        val clusters = mutableListOf<FaceCluster>()
        val faceFeatures = mutableListOf<FaceFeature>()

        photos.filter { it.mediaType == MediaType.IMAGE }.take(300).forEach { photo ->
            if (photo.id in faceDetectionProcessed) return@forEach
            if (faceDetectionProcessed.size >= FACE_DETECTION_MAX_PER_SESSION) return@forEach
            faceDetectionProcessed.add(photo.id)
            var bitmap: Bitmap? = null
            try {
                bitmap = loadBitmap(photo.uri ?: return@forEach) ?: return@forEach
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val faces = faceDetector.process(inputImage).await()

                faces.forEachIndexed { index, face ->
                    val avatarUri = cropAndSaveFaceAvatar(bitmap, face.boundingBox, photo.id, index)
                        ?: return@forEachIndexed
                    val avgColor = computeAverageColor(bitmap, face.boundingBox)
                    val feature = FaceFeature(
                        photoId = photo.id,
                        avatarUri = avatarUri,
                        aspectRatio = face.boundingBox.width().toFloat() / face.boundingBox.height().coerceAtLeast(1),
                        avgColor = avgColor
                    )
                    faceFeatures.add(feature)
                }
            } catch (_: Exception) {
                // 单张图片检测失败不影响整体流程
            } finally {
                // 及时回收 Bitmap，避免大图批量检测导致 OOM
                bitmap?.recycle()
            }
        }

        // 简单聚类：将人脸按宽高比和平均颜色距离分组
        val grouped = mutableListOf<MutableList<FaceFeature>>()
        faceFeatures.forEach { feature ->
            val matched = grouped.find { group ->
                val representative = group.first()
                abs(representative.aspectRatio - feature.aspectRatio) < 0.25f &&
                    colorDistance(representative.avgColor, feature.avgColor) < 60f
            }
            if (matched != null) {
                matched.add(feature)
            } else {
                grouped.add(mutableListOf(feature))
            }
        }

        grouped.filter { it.size >= 2 }.forEachIndexed { index, group ->
            val representative = group.maxByOrNull { it.photoId } ?: group.first()
            clusters.add(
                FaceCluster(
                    id = "face_cluster_$index",
                    name = "人物 ${index + 1}",
                    avatarUri = representative.avatarUri,
                    photoIds = group.map { it.photoId }.distinct()
                )
            )
        }

        _faces.value = clusters
        rebuildDerivedData(_photos.value)
    }

    private data class FaceFeature(
        val photoId: String,
        val avatarUri: Uri,
        val aspectRatio: Float,
        val avgColor: Triple<Int, Int, Int>
    )

    private fun colorDistance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Float {
        return kotlin.math.sqrt(
            (a.first - b.first).toFloat().pow(2) +
                (a.second - b.second).toFloat().pow(2) +
                (a.third - b.third).toFloat().pow(2)
        )
    }

    private fun Float.pow(exp: Int): Float {
        var result = 1f
        repeat(exp) { result *= this }
        return result
    }

    private suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(appContext)
                .data(uri)
                .allowHardware(false)
                .build()
            val drawable = appContext.imageLoader.execute(request).drawable
            drawable?.toBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun cropAndSaveFaceAvatar(
        source: Bitmap,
        boundingBox: android.graphics.Rect,
        photoId: String,
        faceIndex: Int
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val width = source.width
            val height = source.height
            val left = boundingBox.left.coerceIn(0, width)
            val top = boundingBox.top.coerceIn(0, height)
            val right = boundingBox.right.coerceIn(left, width)
            val bottom = boundingBox.bottom.coerceIn(top, height)
            if (right - left < 8 || bottom - top < 8) return@withContext null

            val cropped = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
            val avatar = Bitmap.createScaledBitmap(cropped, 256, 256, true)
            cropped.recycle()

            val dir = File(appContext.cacheDir, "face_avatars").apply { mkdirs() }
            val file = File(dir, "face_${photoId}_${faceIndex}.jpg")
            FileOutputStream(file).use { out ->
                avatar.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            avatar.recycle()
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun computeAverageColor(bitmap: Bitmap, boundingBox: android.graphics.Rect): Triple<Int, Int, Int> {
        val left = boundingBox.left.coerceIn(0, bitmap.width)
        val top = boundingBox.top.coerceIn(0, bitmap.height)
        val right = boundingBox.right.coerceIn(left, bitmap.width)
        val bottom = boundingBox.bottom.coerceIn(top, bitmap.height)
        if (right <= left || bottom <= top) return Triple(128, 128, 128)

        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0
        for (y in top until bottom step 4) {
            for (x in left until right step 4) {
                val pixel = bitmap.getPixel(x, y)
                r += android.graphics.Color.red(pixel)
                g += android.graphics.Color.green(pixel)
                b += android.graphics.Color.blue(pixel)
                count++
            }
        }
        return if (count > 0) Triple((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
        else Triple(128, 128, 128)
    }

    /**
     * 使用 ML Kit Text Recognition 识别票据信息
     *
     * 支持：火车票、机票、景区门票、电影票、酒店入住单等
     */
    private suspend fun recognizeTickets(photos: List<TrailPhoto>) {
        val recognized = mutableListOf<TravelTicket>()

        photos.filter { it.mediaType == MediaType.IMAGE }.take(200).forEach { photo ->
            try {
                val bitmap = loadBitmap(photo.uri ?: return@forEach) ?: return@forEach
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = textRecognizer.process(inputImage).await()
                val text = visionText.text
                if (text.isBlank()) return@forEach

                val ticket = parseTicketFromText(text, photo.id)
                if (ticket != null) {
                    recognized.add(ticket)
                }
            } catch (_: Exception) {
                // 单张识别失败不影响整体流程
            }
        }

        _tickets.value = recognized
        rebuildDerivedData(_photos.value)
    }

    private fun parseTicketFromText(text: String, photoId: String): TravelTicket? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return null

        // 火车票：包含"车次"、"G/D/C/Z/T/K"、"站"、"出发/到达"
        val trainPatterns = listOf(
            Regex("([GDCZTK]\\d{1,4})"),
            Regex("(\\d{1,2}:\\d{2})"),
            Regex("(出发|到达|始发|终到|开|到)")
        )
        val isTrain = lines.any { it.contains("车次") || it.contains("火车站") || it.contains("铁路") }
            || trainPatterns.all { pattern -> lines.any { pattern.containsMatchIn(it) } }

        // 机票：包含"航班号"、"Flight"、"机场"
        val isFlight = text.contains("航班号", ignoreCase = true)
            || text.contains("Flight No", ignoreCase = true)
            || text.contains("机场", ignoreCase = true)
            || Regex("[A-Z]{2}\\d{3,4}").containsMatchIn(text)

        // 景区门票
        val isScenic = text.contains("门票", ignoreCase = true)
            || text.contains("景区", ignoreCase = true)
            || text.contains("入园", ignoreCase = true)
            || text.contains("Scenic", ignoreCase = true)

        // 酒店
        val isHotel = text.contains("酒店", ignoreCase = true)
            || text.contains("入住", ignoreCase = true)
            || text.contains("Hotel", ignoreCase = true)
            || text.contains("房号", ignoreCase = true)

        // 电影票
        val isMovie = text.contains("电影票", ignoreCase = true)
            || text.contains("影院", ignoreCase = true)
            || text.contains("Cinema", ignoreCase = true)
            || text.contains("放映", ignoreCase = true)

        val (type, departure, arrival) = when {
            isTrain -> Triple(TicketType.TRAIN, extractStation(lines) ?: "未知站", extractArrivalStation(lines) ?: "")
            isFlight -> Triple(TicketType.FLIGHT, extractAirport(lines) ?: "未知机场", extractArrivalAirport(lines) ?: "")
            isScenic -> Triple(TicketType.SCENIC, extractScenicName(lines) ?: "未知景区", "")
            isHotel -> Triple(TicketType.HOTEL, extractHotelName(lines) ?: "未知酒店", "")
            isMovie -> Triple(TicketType.MOVIE, extractCinemaName(lines) ?: "未知影院", "")
            else -> return null
        }

        return TravelTicket(
            type = type,
            departure = departure,
            arrival = arrival,
            departureTime = extractDateTime(lines),
            ticketNo = extractTicketNo(lines),
            seatInfo = extractSeatInfo(lines),
            price = extractPrice(lines),
            photoIds = listOf(photoId),
            isRecognized = true
        )
    }

    private fun extractStation(lines: List<String>): String? {
        val stationPattern = Regex("([\\u4e00-\\u9fa5]{2,6})(?:站|火车站|高铁站)")
        return lines.mapNotNull { stationPattern.find(it)?.groupValues?.get(1) }.firstOrNull()
    }

    private fun extractArrivalStation(lines: List<String>): String? {
        val stationPattern = Regex("([\\u4e00-\\u9fa5]{2,6})(?:站|火车站|高铁站)")
        val matches = lines.mapNotNull { stationPattern.find(it)?.groupValues?.get(1) }
        return matches.getOrNull(1)
    }

    private fun extractAirport(lines: List<String>): String? {
        val pattern = Regex("([\\u4e00-\\u9fa5]{2,6})(?:机场|国际机场)")
        return lines.mapNotNull { pattern.find(it)?.groupValues?.get(1) }.firstOrNull()
    }

    private fun extractArrivalAirport(lines: List<String>): String? {
        val pattern = Regex("([\\u4e00-\\u9fa5]{2,6})(?:机场|国际机场)")
        val matches = lines.mapNotNull { pattern.find(it)?.groupValues?.get(1) }
        return matches.getOrNull(1)
    }

    private fun extractScenicName(lines: List<String>): String? {
        val scenicKeywords = listOf("景区", "门票", "入园")
        return lines.find { line -> scenicKeywords.any { line.contains(it) } }
            ?.let { line ->
                // 提取关键词前面的景区名称（如 "西湖景区" -> "西湖"）
                val match = Regex("([\\u4e00-\\u9fa5]{2,8})(?:景区|门票|入园)").find(line)
                match?.groupValues?.get(1) ?: line.replace(Regex(".*?(景区|门票|入园)"), "").take(12)
            }
    }

    private fun extractHotelName(lines: List<String>): String? {
        return lines.find { it.contains("酒店") || it.contains("Hotel") }
            ?.take(15)
    }

    private fun extractCinemaName(lines: List<String>): String? {
        return lines.find { it.contains("影院") || it.contains("影城") || it.contains("Cinema") }
            ?.take(15)
    }

    private fun extractTicketNo(lines: List<String>): String? {
        val patterns = listOf(
            Regex("(?:票号|订单号|编号|No)[：:]?\\s*([A-Z0-9]{6,20})", RegexOption.IGNORE_CASE),
            Regex("([A-Z0-9]{10,20})")
        )
        patterns.forEach { pattern ->
            lines.forEach { line ->
                pattern.find(line)?.groupValues?.get(1)?.let { return it }
            }
        }
        return null
    }

    private fun extractDateTime(lines: List<String>): LocalDateTime? {
        val datePattern = Regex("(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})[日]?\\s*(\\d{1,2}):\\s*(\\d{2})")
        lines.forEach { line ->
            datePattern.find(line)?.let { match ->
                return try {
                    LocalDateTime.of(
                        match.groupValues[1].toInt(),
                        match.groupValues[2].toInt(),
                        match.groupValues[3].toInt(),
                        match.groupValues[4].toInt(),
                        match.groupValues[5].toInt()
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun extractSeatInfo(lines: List<String>): String? {
        val seatPattern = Regex("(?:座位|车厢|席别|舱位|登机口|Seat)[：:]?\\s*([\\u4e00-\\u9fa5A-Z0-9\\-]{1,10})")
        return lines.mapNotNull { seatPattern.find(it)?.groupValues?.get(1) }.firstOrNull()
    }

    private fun extractPrice(lines: List<String>): String? {
        val pricePattern = Regex("(?:¥|￥|价格|Price)[：:]?\\s*([\\d.,]+)")
        return lines.mapNotNull { pricePattern.find(it)?.groupValues?.get(1) }.firstOrNull()
            ?.let { "¥$it" }
    }

    /**
     * 基于真实照片重建派生数据：相册、位置、年度报告
     */
    private fun rebuildDerivedData(photos: List<TrailPhoto>) {
        // 1. 相册：按月份 + 城市自动分组
        val albums = mutableListOf<TrailAlbum>()

        val monthlyGroups = photos.groupBy {
            "${it.photoTime.year}年${it.photoTime.monthValue}月"
        }
        // 月份分组按时间倒序排列（最新优先）
        monthlyGroups.toSortedMap(compareByDescending<String> { label ->
            val regex = Regex("(\\d+)年(\\d+)月")
            val match = regex.find(label)
            if (match != null) {
                val year = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                year * 12 + month
            } else 0
        }).forEach { (monthLabel, monthPhotos) ->
            if (monthPhotos.size >= 3) {
                albums.add(
                    TrailAlbum(
                        id = "album_month_${monthLabel}",
                        name = monthLabel,
                        description = "${monthPhotos.size} 张照片",
                        coverPhotoId = monthPhotos.firstOrNull()?.id,
                        type = AlbumType.SMART,
                        photoIds = monthPhotos.map { it.id }
                    )
                )
            }
        }

        val cityGroups = photos.filter { it.metadata?.city != null }
            .groupBy { it.metadata?.city }
        // 城市分组按照片数倒序排列，并去除重复城市名
        cityGroups.entries
            .filter { it.key != null }
            .distinctBy { it.key }
            .sortedByDescending { it.value.size }
            .forEach { (city, cityPhotos) ->
                if (city != null && cityPhotos.size >= 3) {
                    albums.add(
                        TrailAlbum(
                            id = "album_city_$city",
                            name = "$city",
                            description = "${cityPhotos.size} 张照片",
                            coverPhotoId = cityPhotos.firstOrNull()?.id,
                            type = AlbumType.CONDITIONAL,
                            condition = AlbumCondition(location = city),
                            photoIds = cityPhotos.map { it.id }
                        )
                    )
                }
            }

        albums.add(
            0, TrailAlbum(
                id = "album_all",
                name = "所有照片",
                description = "全部照片和视频",
                coverPhotoId = photos.firstOrNull()?.id,
                type = AlbumType.USER,
                photoIds = photos.map { it.id }
            )
        )

        _albums.value = albums

        // 2. 位置：按经纬度聚合
        val locationPins = photos.filter {
            (it.metadata?.latitude ?: 0.0) != 0.0 && (it.metadata?.longitude ?: 0.0) != 0.0
        }.groupBy { photo ->
            val lat = photo.metadata?.latitude ?: 0.0
            val lon = photo.metadata?.longitude ?: 0.0
            "${(lat * 20).toInt()}_${(lon * 20).toInt()}"
        }.map { (_, groupPhotos) ->
            val first = groupPhotos.first()
            val name = first.metadata?.city
                ?: first.metadata?.district
                ?: "地点 ${first.metadata?.latitude?.toString()?.take(5)},${first.metadata?.longitude?.toString()?.take(5)}"
            LocationPin(
                name = name,
                latitude = first.metadata?.latitude ?: 0.0,
                longitude = first.metadata?.longitude ?: 0.0,
                photoCount = groupPhotos.size,
                coverPhotoId = first.id,
                level = if (first.metadata?.city != null) LocationLevel.CITY else LocationLevel.SCENIC
            )
        }

        _locations.value = locationPins

        // 4. 收藏列表
        _favorites.value = photos.filter { it.isFavorite }.sortedByDescending { it.photoTime }

        // 5. 统计
        _dashboardStats.value = DashboardStats(
            totalPhotos = photos.count { it.mediaType == MediaType.IMAGE },
            totalVideos = photos.count { it.mediaType == MediaType.VIDEO },
            totalAlbums = albums.size,
            locationCount = locationPins.size,
            peopleCount = _faces.value.size,
            ticketCount = _tickets.value.size,
            favoriteCount = photos.count { it.isFavorite },
            earliestPhotoDate = photos.minOfOrNull { it.photoTime.toLocalDate() },
            latestPhotoDate = photos.maxOfOrNull { it.photoTime.toLocalDate() }
        )

        // 6. 年度报告
        val currentYear = LocalDate.now().year
        val yearPhotos = photos.filter { it.photoTime.year == currentYear }
        if (yearPhotos.isNotEmpty()) {
            val monthly = (1..12).associateWith { month ->
                yearPhotos.count { it.photoTime.monthValue == month }
            }
            _annualReport.value = AnnualReport(
                year = currentYear,
                totalPhotos = yearPhotos.size,
                totalCities = locationPins.size,
                totalScenics = photos.mapNotNull { it.metadata?.scene }.distinct().size,
                totalTrips = _tickets.value.count { it.type == TicketType.TRAIN || it.type == TicketType.FLIGHT },
                farthestCity = locationPins.maxByOrNull { kotlin.math.abs(it.latitude) }?.name,
                favoriteCity = locationPins.maxByOrNull { it.photoCount }?.name,
                seasonStats = mapOf(
                    "春" to yearPhotos.count { it.photoTime.monthValue in 3..5 },
                    "夏" to yearPhotos.count { it.photoTime.monthValue in 6..8 },
                    "秋" to yearPhotos.count { it.photoTime.monthValue in 9..11 },
                    "冬" to yearPhotos.count { it.photoTime.monthValue in listOf(12, 1, 2) }
                ),
                monthlyStats = monthly,
                cityStats = locationPins.map { CityStat(it.name, null, it.photoCount) }
                    .sortedByDescending { it.photoCount }
                    .take(5),
                highlightPhotoIds = yearPhotos
                    .map { photo ->
                        val score = (if (photo.metadata?.city != null) 2 else 0) +
                            (if (photo.metadata?.scene != null) 1 else 0) +
                            (photo.width * photo.height) / 1_000_000
                        photo to score
                    }
                    .sortedByDescending { it.second }
                    .take(9)
                    .map { it.first.id }
            )
        }
    }

    fun getPhotoById(id: String): TrailPhoto? = _photos.value.find { it.id == id }

    fun getTimelineSections(): List<TimelineSection> {
        return _photos.value
            .filter { !it.isDeleted }
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

    /**
     * 工具箱：基于真实照片统计生成可执行项
     */
    fun getToolboxItems(): List<ToolboxItem> {
        val photos = _photos.value
        val deletedCount = photos.count { it.isDeleted }

        val duplicateGroups = photos.groupBy { Triple(it.size, it.width, it.height) }
            .filter { it.value.size > 1 }
        val duplicateCount = duplicateGroups.size

        val missingTimeCount = photos.count { photo ->
            val parsed = parseTimeFromFilename(photo.filename)
            parsed != null && kotlin.math.abs(Duration.between(photo.photoTime, parsed).toDays()) > 1
        }

        return listOf(
            ToolboxItem(
                ToolboxTool.DUPLICATE_CLEANUP,
                "重复照片清理",
                "扫描并合并相似照片",
                "clean",
                duplicateCount
            ),
            ToolboxItem(
                ToolboxTool.SIMILAR_PHOTOS,
                "相似照片整理",
                "按时间线自动归类",
                "layers",
                0
            ),
            ToolboxItem(
                ToolboxTool.ORGANIZE_BY_DATE,
                "按日期整理",
                "一键归档到年月文件夹",
                "calendar",
                0
            ),
            ToolboxItem(
                ToolboxTool.RENAME_BATCH,
                "批量重命名",
                "统一照片命名规则",
                "edit_3",
                0
            ),
            ToolboxItem(
                ToolboxTool.TIME_FROM_FILENAME,
                "从文件名恢复时间",
                "修复缺失的拍摄时间",
                "clock",
                missingTimeCount
            ),
            ToolboxItem(
                ToolboxTool.RECYCLE_BIN,
                "回收站",
                "恢复或彻底删除照片",
                "trash",
                deletedCount
            )
        )
    }

    /**
     * 获取相似/重复照片分组（基于综合相似度评分）
     *
     * 评分维度：
     * - 分辨率相似度（相同尺寸满分，按面积差异递减）
     * - 拍摄时间接近度（5秒内为连拍，按时间差递减）
     * - 文件名前缀相似度（相同前缀满分）
     * - 文件大小相似度
     */
    fun getSimilarPhotoGroups(): List<List<TrailPhoto>> {
        val candidates = _photos.value.filter { !it.isDeleted }
        if (candidates.size < 2) return emptyList()

        val visited = mutableSetOf<String>()
        val groups = mutableListOf<List<TrailPhoto>>()

        for (i in candidates.indices) {
            val a = candidates[i]
            if (a.id in visited) continue
            val group = mutableListOf(a)

            for (j in (i + 1) until candidates.size) {
                val b = candidates[j]
                if (b.id in visited) continue

                val score = computeSimilarityScore(a, b)
                if (score >= SIMILAR_SCORE_THRESHOLD) {
                    group.add(b)
                }
            }

            if (group.size > 1) {
                visited.addAll(group.map { it.id })
                groups.add(group.sortedByDescending { it.width * it.height })
            }
        }

        return groups
    }

    /**
     * 计算两张照片的综合相似度评分（0.0~1.0）
     */
    private fun computeSimilarityScore(a: TrailPhoto, b: TrailPhoto): Float {
        var score = 0f

        // 1. 分辨率相似度（权重 0.3）
        val sameDimensions = a.width == b.width && a.height == b.height
        val areaA = a.width * a.height
        val areaB = b.width * b.height
        val resolutionScore = if (sameDimensions) 1.0f
        else if (areaA > 0 && areaB > 0) {
            val ratio = minOf(areaA, areaB).toFloat() / maxOf(areaA, areaB).toFloat()
            if (ratio > 0.8f) ratio else 0f
        } else 0f
        score += resolutionScore * 0.3f

        // 2. 拍摄时间接近度（权重 0.3）
        val timeDiffSeconds = kotlin.math.abs(
            Duration.between(a.photoTime, b.photoTime).seconds
        )
        val timeScore = when {
            timeDiffSeconds <= SIMILAR_TIME_THRESHOLD_SECONDS -> 1.0f  // 连拍
            timeDiffSeconds <= 30 -> 0.7f
            timeDiffSeconds <= 300 -> 0.3f
            timeDiffSeconds <= 3600 -> 0.1f
            else -> 0f
        }
        score += timeScore * 0.3f

        // 3. 文件名前缀相似度（权重 0.25）
        val prefixA = extractFilenamePrefix(a.filename)
        val prefixB = extractFilenamePrefix(b.filename)
        val nameScore = when {
            prefixA.isNotBlank() && prefixA == prefixB -> 1.0f
            a.filename == b.filename -> 1.0f
            else -> 0f
        }
        score += nameScore * 0.25f

        // 4. 文件大小相似度（权重 0.15）
        val sizeScore = if (a.size > 0 && b.size > 0) {
            val ratio = minOf(a.size, b.size).toFloat() / maxOf(a.size, b.size).toFloat()
            if (ratio > 0.9f) 1.0f else if (ratio > 0.7f) 0.5f else 0f
        } else 0f
        score += sizeScore * 0.15f

        return score
    }

    /**
     * 提取文件名前缀（如 IMG_20240101 从 IMG_20240101_123045.jpg 中）
     */
    private fun extractFilenamePrefix(filename: String): String {
        val name = filename.substringBeforeLast('.')
        val separator = name.indexOfFirst { it == '_' || it == '-' || it == ' ' }
        return if (separator > 0) name.substring(0, separator + 9) else name.take(12)
    }

    /**
     * 执行重复照片清理：基于综合相似度识别重复项，保留最大的一张
     */
    fun cleanupDuplicates(): Int {
        val duplicateGroups = getSimilarPhotoGroups()
        var removed = 0
        duplicateGroups.forEach { group ->
            // 保留尺寸最大（或文件最大）的一张作为原图
            val keeper = group.maxWithOrNull(compareBy({ it.width * it.height }, { it.size })) ?: group.first()
            group.filter { it.id != keeper.id }.forEach { photo ->
                markPhotoDeleted(photo.id)
                removed++
            }
        }
        return removed
    }

    /**
     * 批量按日期重命名照片
     */
    fun batchRenameByDate(): BatchRenameResult {
        val preview = getBatchRenamePreview()
        return applyBatchRename(preview)
    }

    /**
     * 应用批量重命名：通过 MediaStore 更新 DISPLAY_NAME
     * 单个文件失败不影响整个批次，返回详细结果
     */
    fun applyBatchRename(preview: Map<String, String>): BatchRenameResult {
        if (preview.isEmpty()) return BatchRenameResult(0, emptyList())

        var successCount = 0
        val failures = mutableListOf<RenameFailure>()
        val updated = _photos.value.map { photo ->
            val newName = preview[photo.filename]
            if (newName != null && photo.uri != null) {
                try {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                    }
                    val rows = contentResolver.update(photo.uri, values, null, null)
                    if (rows > 0) {
                        successCount++
                        photo.copy(filename = newName)
                    } else {
                        failures.add(RenameFailure(photo.filename, "MediaStore update returned 0 rows"))
                        photo
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Batch rename failed for ${photo.filename}: permission denied", e)
                    failures.add(RenameFailure(photo.filename, "权限不足：${e.message}"))
                    photo
                } catch (e: Exception) {
                    Log.e(TAG, "Batch rename failed for ${photo.filename}", e)
                    failures.add(RenameFailure(photo.filename, e.message ?: "未知错误"))
                    photo
                }
            } else photo
        }
        _photos.value = updated
        rebuildDerivedData(updated)
        return BatchRenameResult(successCount, failures)
    }

    /**
     * 标记照片为删除（软删除）
     */
    fun markPhotoDeleted(id: String) {
        _photos.value = _photos.value.map {
            if (it.id == id) it.copy(isDeleted = true) else it
        }
        rebuildDerivedData(_photos.value)
    }

    /**
     * 恢复已删除照片
     */
    fun restorePhoto(id: String) {
        _photos.value = _photos.value.map {
            if (it.id == id) it.copy(isDeleted = false) else it
        }
        rebuildDerivedData(_photos.value)
    }

    /**
     * 彻底删除照片：先尝试从 MediaStore 删除，再从应用状态移除，并清理相关缓存
     */
    fun permanentlyDelete(id: String): Boolean {
        val photo = _photos.value.find { it.id == id } ?: return false
        val uri = photo.uri
        val deletedFromSystem = if (uri != null) {
            try {
                contentResolver.delete(uri, null, null) > 0
            } catch (_: Exception) {
                false
            }
        } else false

        // 从内存中移除照片
        _photos.value = _photos.value.filter { it.id != id }

        // 清理人脸检测缓存
        faceDetectionProcessed.remove(id)

        // 清理引用该照片的 FaceCluster 条目
        _faces.value = _faces.value.map { cluster ->
            val updatedPhotoIds = cluster.photoIds.filter { it != id }
            cluster.copy(photoIds = updatedPhotoIds)
        }.filter { it.photoIds.isNotEmpty() }

        // 删除关联的人脸头像缓存文件
        val avatarDir = File(appContext.cacheDir, "face_avatars")
        if (avatarDir.exists()) {
            avatarDir.listFiles()?.filter { it.name.contains("face_${id}_") }?.forEach {
                it.delete()
            }
        }

        rebuildDerivedData(_photos.value)
        return deletedFromSystem
    }

    /**
     * 获取已删除照片列表（回收站）
     */
    fun getDeletedPhotos(): List<TrailPhoto> = _photos.value.filter { it.isDeleted }
        .sortedByDescending { it.photoTime }

    /**
     * 从文件名解析日期并修复缺失的拍摄时间
     * 支持格式：IMG_20240101_123045.jpg、2024-01-01_12-30-45.jpg 等
     *
     * 真实实现：将解析到的时间通过 MediaStore 写回系统图库
     */
    fun fixTimeFromFilename(): Int {
        var fixed = 0
        val updated = _photos.value.map { photo ->
            val parsed = parseTimeFromFilename(photo.filename)
            if (parsed != null && kotlin.math.abs(Duration.between(photo.photoTime, parsed).toDays()) > 1) {
                val uri = photo.uri
                if (uri != null) {
                    try {
                        val epochMillis = parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DATE_TAKEN, epochMillis)
                        }
                        contentResolver.update(uri, values, null, null)
                        fixed++
                        photo.copy(photoTime = parsed)
                    } catch (e: Exception) {
                        Log.w(TAG, "修复时间失败: ${photo.filename}", e)
                        photo
                    }
                } else photo
            } else photo
        }
        if (fixed > 0) {
            _photos.value = updated
            rebuildDerivedData(updated)
        }
        return fixed
    }

    private fun parseTimeFromFilename(filename: String): LocalDateTime? {
        val patterns = listOf(
            Regex("\\D(20\\d{2})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})(\\d{2})\\D") to listOf(1, 2, 3, 4, 5, 6),
            Regex("(20\\d{2})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2})") to listOf(1, 2, 3, 4, 5, 6),
            Regex("(20\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})") to listOf(1, 2, 3, 4, 5, 6)
        )
        for ((regex, groups) in patterns) {
            val match = regex.find(filename) ?: continue
            return try {
                LocalDateTime.of(
                    match.groupValues[groups[0]].toInt(),
                    match.groupValues[groups[1]].toInt(),
                    match.groupValues[groups[2]].toInt(),
                    match.groupValues[groups[3]].toInt(),
                    match.groupValues[groups[4]].toInt(),
                    match.groupValues[groups[5]].toInt()
                )
            } catch (_: Exception) { null }
        }
        return null
    }

    /**
     * 按日期整理方案：按年月分组
     */
    fun getOrganizeByDatePlan(): Map<String, List<TrailPhoto>> {
        return _photos.value
            .filter { !it.isDeleted }
            .groupBy { "${it.photoTime.year}年${it.photoTime.monthValue}月" }
            .toSortedMap(compareBy { it })
    }

    /**
     * 执行按日期整理：通过 MediaStore 更新照片的 BUCKET_DISPLAY_NAME，实现按年月文件夹归档
     * 返回实际整理（移动）的照片数量
     */
    fun applyOrganizeByDate(plan: Map<String, List<TrailPhoto>>): Int {
        if (plan.isEmpty()) return 0
        var organizedCount = 0
        plan.forEach { (monthLabel, photos) ->
            photos.forEach { photo ->
                val uri = photo.uri ?: return@forEach
                try {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, monthLabel)
                    }
                    val rows = contentResolver.update(uri, values, null, null)
                    if (rows > 0) organizedCount++
                } catch (e: SecurityException) {
                    Log.e(TAG, "按日期整理失败（权限不足）: ${photo.filename}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "按日期整理失败: ${photo.filename}", e)
                }
            }
        }
        // 刷新以确保相册数据与系统图库同步
        repositoryScope.launch { loadLocalMedia() }
        return organizedCount
    }

    /**
     * 批量重命名预览：按日期生成新文件名
     */
    fun getBatchRenamePreview(): Map<String, String> {
        val grouped = _photos.value
            .filter { !it.isDeleted }
            .groupBy { "${it.photoTime.year}${String.format("%02d", it.photoTime.monthValue)}" }
        val preview = mutableMapOf<String, String>()
        grouped.forEach { (month, photos) ->
            photos.sortedBy { it.photoTime }.forEachIndexed { index, photo ->
                preview[photo.filename] = "OMaster_${month}_${String.format("%03d", index + 1)}.jpg"
            }
        }
        return preview
    }

    /**
     * 切换照片收藏状态：通过 MediaStore 写入 IS_FAVORITE，并同步更新内存状态
     */
    fun toggleFavorite(id: String): Boolean {
        val photo = _photos.value.find { it.id == id } ?: return false
        val newFavorite = !photo.isFavorite
        val uri = photo.uri ?: return false
        return try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.IS_FAVORITE, if (newFavorite) 1 else 0)
            }
            val rows = contentResolver.update(uri, values, null, null)
            if (rows > 0) {
                _photos.value = _photos.value.map {
                    if (it.id == id) it.copy(isFavorite = newFavorite) else it
                }
                rebuildDerivedData(_photos.value)
            }
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "切换收藏失败: ${photo.filename}", e)
            false
        }
    }

    fun getFavoritePhotos(): List<TrailPhoto> = _favorites.value

    /**
     * 创建自定义相册：将选中的照片通过 MediaStore 添加到新相册（Bucket）
     */
    fun createAlbum(name: String, photoIds: List<String>): TrailAlbum? {
        if (name.isBlank() || photoIds.isEmpty()) return null
        val selectedPhotos = _photos.value.filter { it.id in photoIds }
        if (selectedPhotos.isEmpty()) return null

        var successCount = 0
        selectedPhotos.forEach { photo ->
            val uri = photo.uri ?: return@forEach
            try {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, name)
                }
                val rows = contentResolver.update(uri, values, null, null)
                if (rows > 0) successCount++
            } catch (e: Exception) {
                Log.w(TAG, "创建相册时更新照片失败: ${photo.filename}", e)
            }
        }

        return if (successCount > 0) {
            val album = TrailAlbum(
                id = "album_user_${name.hashCode()}",
                name = name,
                description = "$successCount 张照片",
                coverPhotoId = selectedPhotos.firstOrNull()?.id,
                type = AlbumType.USER,
                photoIds = selectedPhotos.map { it.id }
            )
            _albums.value = _albums.value + album
            album
        } else null
    }

    /**
     * 释放仓库资源：取消所有协程、关闭 ML Kit 检测器。
     * 应在 Application.onTerminate() 或进程退出前调用。
     */
    fun close() {
        try {
            repositoryScope.coroutineContext[Job]?.cancel()
            faceDetector.close()
            textRecognizer.close()
        } catch (e: Exception) {
            Log.w(TAG, "关闭 TrailSnapRepository 资源异常", e)
        }
    }

    companion object {
        private const val TAG = "TrailSnapRepository"
        private const val FACE_DETECTION_MAX_PER_SESSION = 100
        private const val SIMILAR_TIME_THRESHOLD_SECONDS = 5L
        private const val SIMILAR_SCORE_THRESHOLD = 0.6f

        @Volatile
        private var instance: TrailSnapRepository? = null

        fun getInstance(context: Context): TrailSnapRepository {
            return instance ?: synchronized(this) {
                instance ?: TrailSnapRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
