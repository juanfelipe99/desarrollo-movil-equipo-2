package com.equipo2.vinilos.data.network

import android.content.Context
import android.os.Build
import com.equipo2.vinilos.R
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Cliente HTTP para descargar las portadas.
 *
 * Es aparte del de la API para que las pruebas E2E no esperen a las imágenes.
 */
fun imageHttpClient(context: Context): OkHttpClient {
    val builder = OkHttpClient.Builder()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
        val trustManager = trustManagerWithLetsEncrypt(context)
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
        builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    }
    return builder.build()
}

/**
 * Confía en las autoridades del sistema y además en ISRG Root X1 (Let's Encrypt).
 *
 * Android 7.0 y anteriores no traen esa raíz, así que no cargan portadas de sitios como cdn.shopify.com.
 */
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
