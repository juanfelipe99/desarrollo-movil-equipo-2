package com.equipo2.vinilos.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WikimediaThumbnailsTest {

    private val who = "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/960px-Who_-_1975.jpg" +
        "?utm_source=en.wikipedia.org&utm_campaign=api&utm_content=thumbnail"

    @Test
    fun unaMiniaturaGrande_sePideDelAnchoPermitidoMasCercanoAlDeLaVista() {
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/120px-Who_-_1975.jpg",
            wikimediaThumbnail(who, 146),
        )
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/500px-Who_-_1975.jpg",
            wikimediaThumbnail(who, 540),
        )
    }

    @Test
    fun unaMiniaturaNoSeAgranda_porquePodriaSuperarAlOriginal() {
        assertNull(wikimediaThumbnail(who, 1400))
        assertNull(wikimediaThumbnail(who, 800))
    }

    @Test
    fun unaFotoOriginalDeCommons_pasaASuMiniatura() {
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Ruben_Blades.jpg/330px-Ruben_Blades.jpg",
            wikimediaThumbnail("https://upload.wikimedia.org/wikipedia/commons/b/bb/Ruben_Blades.jpg", 300),
        )
    }

    @Test
    fun lasPortadasOriginalesDeLaWikipediaEnIngles_losPngYLasDemasImagenes_noCambian() {
        assertNull(wikimediaThumbnail("https://upload.wikimedia.org/wikipedia/en/9/98/TheDoorsTheDoorsalbumcover.jpg", 442))
        assertNull(wikimediaThumbnail("https://upload.wikimedia.org/wikipedia/commons/3/3b/Dark_Side_of_the_Moon.png", 442))
        assertNull(wikimediaThumbnail("https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg", 442))
        assertNull(wikimediaThumbnail("no es una url", 442))
    }

    @Test
    fun entreDosAnchosPermitidos_eligeElMasCercano() {
        val base = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/%dpx-Who_-_1975.jpg"
        assertEquals(base.format(250), wikimediaThumbnail(who, 200))
        assertEquals(base.format(330), wikimediaThumbnail(who, 300))
    }

    @Test
    fun unaMiniaturaQueYaTieneElAnchoJusto_noCambia() {
        val justa = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/250px-Who_-_1975.jpg"
        assertNull(wikimediaThumbnail(justa, 200))
    }
}
