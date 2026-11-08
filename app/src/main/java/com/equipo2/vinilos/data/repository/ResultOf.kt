package com.equipo2.vinilos.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Los ViewModels reciben un Result con un DataError en lugar de atrapar excepciones.
 * El trabajo corre en el despachador de entrada y salida, así la red y la conversión de los datos no ocupan el hilo principal.
 */
internal suspend fun <T> resultOf(dispatcher: CoroutineDispatcher = Dispatchers.IO, block: suspend () -> T): Result<T> = try {
    Result.success(withContext(dispatcher) { block() })
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toDataError())
}
