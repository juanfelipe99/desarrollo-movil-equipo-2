package com.equipo2.vinilos.data.model

/** Coleccionista con cuántos álbumes tiene en su colección. */
data class Collector(
    val id: Int,
    val name: String,
    val email: String,
    val albumCount: Int,
)
