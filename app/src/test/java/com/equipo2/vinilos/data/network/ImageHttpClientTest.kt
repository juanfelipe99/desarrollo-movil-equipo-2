package com.equipo2.vinilos.data.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageHttpClientTest {

    private val thumb = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Ruben_Blades_by_Gage_Skidmore.jpg/%dpx-Ruben_Blades_by_Gage_Skidmore.jpg"

    @Test
    fun miniaturaDeWikipediaPasaAlTamanoPermitidoMayor() {
        assertEquals(thumb.format(960), withWikimediaSize(thumb.format(800).toHttpUrl()).toString())
        assertEquals(thumb.format(500), withWikimediaSize(thumb.format(400).toHttpUrl()).toString())
        assertEquals(thumb.format(250), withWikimediaSize(thumb.format(250).toHttpUrl()).toString())
        assertEquals(thumb.format(960), withWikimediaSize(thumb.format(2000).toHttpUrl()).toString())
    }

    @Test
    fun lasDemasUrlNoCambian() {
        val original = "https://upload.wikimedia.org/wikipedia/en/4/4d/Queen_A_Night_At_The_Opera.png"
        val otra = "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg"
        assertEquals(original, withWikimediaSize(original.toHttpUrl()).toString())
        assertEquals(otra, withWikimediaSize(otra.toHttpUrl()).toString())
    }

    @Test
    fun anteUn429_seEsperaLoQuePideElServidor_entreUnoYQuinceSegundos() {
        assertEquals(11, retryAfterSeconds("11"))
        assertEquals(1, retryAfterSeconds("0"))
        assertEquals(15, retryAfterSeconds("120"))
        assertEquals(2, retryAfterSeconds(null))
        assertEquals(2, retryAfterSeconds("Wed, 21 Oct 2026 07:28:00 GMT"))
    }
}
