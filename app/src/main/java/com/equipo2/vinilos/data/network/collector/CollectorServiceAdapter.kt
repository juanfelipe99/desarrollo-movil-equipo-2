package com.equipo2.vinilos.data.network.collector

import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.network.ApiClient

class CollectorServiceAdapter(apiClient: ApiClient) {

    private val api = apiClient.create(CollectorApi::class.java)

    suspend fun getCollectors(): List<Collector> = api.getCollectors().map { it.toCollector() }
}
