package com.silas.omaster.trailsnap.model

import org.junit.Assert.*
import org.junit.Test

/**
 * TrailSnapModels 单元测试
 * 验证行影集数据模型的正确性
 */
class TrailSnapModelsTest {

    @Test
    fun `TrailPhoto 默认值应正确`() {
        val photo = TrailPhoto(
            id = 1L,
            displayName = "test.jpg",
            dateTaken = 1234567890L,
            uri = "content://media/1"
        )

        assertEquals(1L, photo.id)
        assertEquals("test.jpg", photo.displayName)
        assertEquals(1234567890L, photo.dateTaken)
        assertFalse(photo.isFavorite)
        assertFalse(photo.isVideo)
        assertTrue(photo.tags.isEmpty())
    }

    @Test
    fun `TrailAlbum 应正确统计照片数量`() {
        val photos = listOf(
            TrailPhoto(id = 1, displayName = "1.jpg", dateTaken = 1000, uri = "uri1"),
            TrailPhoto(id = 2, displayName = "2.jpg", dateTaken = 2000, uri = "uri2"),
            TrailPhoto(id = 3, displayName = "3.mp4", dateTaken = 3000, uri = "uri3", isVideo = true)
        )

        val album = TrailAlbum(
            id = "album-1",
            name = "测试相册",
            photoCount = photos.size,
            coverUri = "cover_uri"
        )

        assertEquals("album-1", album.id)
        assertEquals("测试相册", album.name)
        assertEquals(3, album.photoCount)
    }

    @Test
    fun `FaceCluster 应正确存储人脸信息`() {
        val facePhotos = listOf(
            TrailPhoto(id = 1, displayName = "p1.jpg", dateTaken = 1000, uri = "uri1"),
            TrailPhoto(id = 2, displayName = "p2.jpg", dateTaken = 2000, uri = "uri2")
        )

        val cluster = FaceCluster(
            id = "face-1",
            name = "小明",
            photoCount = facePhotos.size,
            coverUri = "cover",
            isNamed = true
        )

        assertEquals("face-1", cluster.id)
        assertEquals("小明", cluster.name)
        assertEquals(2, cluster.photoCount)
        assertTrue(cluster.isNamed)
    }

    @Test
    fun `LocationPin 应正确存储位置信息`() {
        val location = LocationPin(
            id = "loc-1",
            latitude = 39.9042,
            longitude = 116.4074,
            cityName = "北京",
            photoCount = 10
        )

        assertEquals("loc-1", location.id)
        assertEquals(39.9042, location.latitude, 0.0001)
        assertEquals(116.4074, location.longitude, 0.0001)
        assertEquals("北京", location.cityName)
        assertEquals(10, location.photoCount)
    }

    @Test
    fun `TravelTicket 应正确存储票据信息`() {
        val ticket = TravelTicket(
            id = "ticket-1",
            type = TicketType.TRAIN,
            from = "北京",
            to = "上海",
            date = "2024-01-01",
            price = 553.5f,
            photoUri = "uri"
        )

        assertEquals("ticket-1", ticket.id)
        assertEquals(TicketType.TRAIN, ticket.type)
        assertEquals("北京", ticket.from)
        assertEquals("上海", ticket.to)
        assertEquals("2024-01-01", ticket.date)
        assertEquals(553.5f, ticket.price, 0.01f)
    }

    @Test
    fun `TicketType 枚举应包含所有类型`() {
        val types = TicketType.values()
        assertTrue(types.size >= 3)
        assertTrue(TicketType.TRAIN in types)
        assertTrue(TicketType.PLANE in types)
        assertTrue(TicketType.SCENIC in types)
    }

    @Test
    fun `TrailPhoto 视频标识应正确`() {
        val photo = TrailPhoto(
            id = 1,
            displayName = "video.mp4",
            dateTaken = 1000,
            uri = "uri",
            isVideo = true,
            duration = 10000L
        )

        assertTrue(photo.isVideo)
        assertEquals(10000L, photo.duration)
    }
}
