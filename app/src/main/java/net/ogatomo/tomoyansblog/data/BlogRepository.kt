package net.ogatomo.tomoyansblog.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class BlogRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val parser: RssFeedParser = RssFeedParser()
) {

    suspend fun fetchLatestArticles(): List<Article> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(FEED_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        return@withContext body.byteStream().use { stream ->
                            parser.parse(stream).take(MAX_ARTICLES)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        return@withContext emptyList()
    }

    companion object {
        const val FEED_URL = "https://ogatomo.net/feed"
        const val MORE_URL = "https://ogatomo.net/page/3/"
        private const val MAX_ARTICLES = 30
    }
}
