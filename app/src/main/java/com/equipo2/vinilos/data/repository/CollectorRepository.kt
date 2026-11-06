package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.network.collector.CollectorServiceAdapter

interface CollectorRepository {
    suspend fun getCollectors(): Result<List<Collector>>
}

class CollectorRepositoryImpl(private val serviceAdapter: CollectorServiceAdapter) : CollectorRepository {

    override suspend fun getCollectors(): Result<List<Collector>> = resultOf { serviceAdapter.getCollectors() }
}
