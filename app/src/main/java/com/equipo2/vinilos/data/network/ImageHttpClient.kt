package com.equipo2.vinilos.data.network

import android.content.Context
import android.os.Build
import com.equipo2.vinilos.BuildConfig
import com.equipo2.vinilos.R
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** Cliente para las imágenes, aparte del de la API para que las pruebas E2E no esperen a que carguen. */
fun imageHttpClient(context: Context): OkHttpClient {
    val builder = OkHttpClient.Builder().addInterceptor(::forImageHosts)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
        val trustManager = trustManagerWithLetsEncrypt(context)
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
        builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    }
    return builder.build()
}

/** Agrega a las autoridades del sistema la raíz de Let's Encrypt, que Android 7.0 y anteriores no traen. */
fun trustManagerWithLetsEncrypt(context: Context): X509TrustManager {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }

    val system = KeyStore.getInstance("AndroidCAStore").apply { load(null, null) }
    system.aliases().toList().forEach { alias -> keyStore.setCertificateEntry(alias, system.getCertificate(alias)) }

    val isrgRootX1 = context.resources.openRawResource(R.raw.isrg_root_x1).use { input ->
        CertificateFactory.getInstance("X.509").generateCertificate(input) as X509Certificate
    }
    keyStore.setCertificateEntry("isrg_root_x1", isrgRootX1)

    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keyStore) }
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}

private const val USER_AGENT = "Vinilos/${BuildConfig.VERSION_NAME} (https://github.com/juanfelipe99/desarrollo-movil-equipo-2)"
private const val WIKIMEDIA_HOST = "upload.wikimedia.org"
private val WIKIMEDIA_SIZES = listOf(120, 250, 330, 500, 960)
private val THUMB_SIZE = Regex("/(\\d+)px-")

/** Wikipedia exige un User-Agent propio y solo tiene miniaturas de ciertos tamaños, así que se pide el siguiente permitido. */
private fun forImageHosts(chain: Interceptor.Chain): Response {
    val request = chain.request().newBuilder()
        .url(withWikimediaSize(chain.request().url))
        .header("User-Agent", USER_AGENT)
        .build()
    val response = chain.proceed(request)
    // Al desplazar rápido el catálogo se piden muchas imágenes seguidas y Wikimedia responde 429 a una parte, con
    // Retry-After de unos 11 s. Antes esas portadas quedaban sin imagen. Se espera lo que pide y se intenta una vez más,
    // en el hilo de la descarga, no en el principal
    if (response.code != HTTP_TOO_MANY_REQUESTS) return response
    val wait = retryAfterSeconds(response.header("Retry-After"))
    response.close()
    Thread.sleep(wait * 1000)
    return chain.proceed(request)
}

private const val HTTP_TOO_MANY_REQUESTS = 429

/** Segundos que pide esperar el servidor, entre 1 y 15. Sin el encabezado o con una fecha, 2. */
internal fun retryAfterSeconds(header: String?): Long = header?.trim()?.toLongOrNull()?.coerceIn(1, 15) ?: 2

internal fun withWikimediaSize(url: HttpUrl): HttpUrl {
    if (url.host != WIKIMEDIA_HOST || "/thumb/" !in url.encodedPath) return url
    val match = THUMB_SIZE.find(url.encodedPath) ?: return url
    val requested = match.groupValues[1].toInt()
    val size = WIKIMEDIA_SIZES.firstOrNull { it >= requested } ?: WIKIMEDIA_SIZES.last()
    return url.newBuilder().encodedPath(url.encodedPath.replaceRange(match.range, "/${size}px-")).build()
}
