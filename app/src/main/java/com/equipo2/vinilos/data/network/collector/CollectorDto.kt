package com.equipo2.vinilos.data.network.collector

import com.equipo2.vinilos.data.model.Collector
import kotlinx.serialization.Serializable

@Serializable
data class CollectorDto(
    val id: Int,
    val name: String,
    val email: String = "",
    val collectorAlbums: List<CollectorAlbumDto> = emptyList(),
)

// Del álbum en la colección solo se necesita contarlo; precio y estado se ignoran
@Serializable
data class CollectorAlbumDto(val id: Int)

internal fun CollectorDto.toCollector() = Collector(
    id = id,
    name = name,
    email = email,
    albumCount = collectorAlbums.size,
)
