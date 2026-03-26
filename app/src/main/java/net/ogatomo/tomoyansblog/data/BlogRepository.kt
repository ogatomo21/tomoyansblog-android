package net.ogatomo.tomoyansblog.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ogatomo.tomoyansblog.data.database.ArticleDao
import net.ogatomo.tomoyansblog.data.database.ArticleEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class BlogRepository(
    private val articleDao: ArticleDao,
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
                        val newArticles = body.byteStream().use { stream ->
                            parser.parse(stream).take(MAX_ARTICLES)
                        }

                        val existingArticles = articleDao.getAllArticles().associateBy { it.guid }
                        val entities = newArticles.map { article ->
                            val bodyHtml = existingArticles[article.guid]?.bodyHtml
                            ArticleEntity.fromArticle(article, bodyHtml)
                        }

                        articleDao.insertArticles(entities)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If offline and DB is empty, throw so the UI shows an error
            val cached = articleDao.getAllArticles()
            if (cached.isEmpty()) {
                throw e
            }
        }

        return@withContext articleDao.getAllArticles().map { it.toArticle() }
    }

    suspend fun getArticleHtml(guid: String, url: String): String? = withContext(Dispatchers.IO) {
        val cached = articleDao.getArticleByGuid(guid)
        if (cached?.bodyHtml != null) {
            return@withContext cached.bodyHtml
        }

        val html = fetchCleanArticleHtml(url)
        if (html != null) {
            articleDao.updateArticleBody(guid, html)
        }
        return@withContext html
    }

    suspend fun syncAllMissingArticleBodies() = withContext(Dispatchers.IO) {
        val articles = articleDao.getAllArticles()
        for (article in articles) {
            if (article.bodyHtml == null) {
                val html = fetchCleanArticleHtml(article.link)
                if (html != null) {
                    articleDao.updateArticleBody(article.guid, html)
                }
            }
        }
    }

    suspend fun fetchCleanArticleHtml(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val document = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8)")
                .get()

            val articleElement = document.selectFirst("article")
            if (articleElement != null) {
                articleElement.select(".adbox").remove()
                articleElement.select(".sns.st-sns-singular").remove()
                articleElement.select(".st-author-box").remove()
                articleElement.select(".tagst").remove()
                articleElement.select(".kanren").remove()
                articleElement.select("h4.point").remove()
                articleElement.select("#comments").remove()
                articleElement.select(".p-navi").remove()
                articleElement.select("hr.hrcss").remove()
                articleElement.select("script").remove()
                
                return@withContext articleElement.outerHtml()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    companion object {
        const val FEED_URL = "https://ogatomo.net/feed"
        const val MORE_URL = "https://ogatomo.net/page/3/"
        private const val MAX_ARTICLES = 30
    }
}
