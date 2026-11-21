package com.equipo2.vinilos.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WikimediaThumbnailsTest {

    private val who = "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/960px-Who_-_1975.jpg" +
        "?utm_source=en.wikipedia.org&utm_campaign=api&utm_content=thumbnail"

    @Test
    fun unaMiniaturaGrande_sePideDelAnchoPermitidoQueAlcanzaParaLaVista() {
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/250px-Who_-_1975.jpg",
            wikimediaThumbnail(who, 146),
        )
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/500px-Who_-_1975.jpg",
            wikimediaThumbnail(who, 442),
        )
    }

    @Test
    fun unaVistaMasAnchaQueLaMiniaturaMasGrande_pideLaDe960() {
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/960px-Who_-_1975.jpg",
            wikimediaThumbnail(who, 1400),
        )
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
    fun unaMiniaturaQueYaTieneElAnchoJusto_noCambia() {
        val justa = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Who_-_1975.jpg/250px-Who_-_1975.jpg"
        assertNull(wikimediaThumbnail(justa, 200))
    }
}
