package fr.roadguard.drive.net

import fr.roadguard.drive.link.LinkClient
import fr.roadguard.core.net.Net
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.maplibre.android.module.http.HttpRequestUtil

/**
 * Tuiles et styles de la carte : quand le poste n'a pas Internet, MapLibre passe par le téléphone
 * (ROADGUARD Link), sans rien changer au reste de la carte.
 */
class LinkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val link = LinkClient.current
        if (Net.hasDirectInternet() || link == null || !link.isConnected()) return chain.proceed(request)
        val body = request.body?.let { b -> Buffer().also { b.writeTo(it) }.readByteArray() }
        val r = runBlocking { link.http(request.url.toString(), request.method, body, request.body?.contentType()?.toString(), true) }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(r.code)
            .message(if (r.code in 200..299) "OK" else "Erreur")
            .body(r.body.toResponseBody(r.contentType?.toMediaTypeOrNull()))
            .build()
    }

    companion object {
        /** À appeler une fois au démarrage, avant le chargement de la carte. */
        fun install() {
            HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder().addInterceptor(LinkInterceptor()).build())
        }
    }
}
