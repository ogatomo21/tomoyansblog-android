package net.ogatomo.tomoyansblog.data

import android.os.Build
import android.text.Html
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.regex.Pattern

class RssFeedParser {

    fun parse(inputStream: InputStream): List<Article> {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser().apply {
            setInput(inputStream, null)
        }

        val articles = mutableListOf<Article>()
        var eventType = parser.eventType

        var title = ""
        var link = ""
        var author = ""
        var publishedAt = ""
        var categories = mutableListOf<String>()
        var description = ""
        var imageUrl: String? = null
        var guid = ""
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when {
                    parser.name == "item" -> {
                        insideItem = true
                        title = ""
                        link = ""
                        author = ""
                        publishedAt = ""
                        categories = mutableListOf()
                        description = ""
                        imageUrl = null
                        guid = ""
                    }
                    insideItem && parser.name == "title" -> title = parser.nextText().trim()
                    insideItem && parser.name == "link" -> link = parser.nextText().trim()
                    insideItem && isTag(parser, "creator", "dc") -> author = parser.nextText().trim()
                    insideItem && parser.name == "pubDate" -> {
                        publishedAt = formatPublishedAt(parser.nextText().trim())
                    }
                    insideItem && parser.name == "category" -> categories += parser.nextText().trim()
                    insideItem && parser.name == "description" -> {
                        val rawDescription = parser.nextText()
                        description = htmlToText(rawDescription)
                        if (imageUrl.isNullOrBlank()) {
                            imageUrl = extractFirstImageUrl(rawDescription)
                        }
                    }
                    insideItem && parser.name == "guid" -> guid = parser.nextText().trim()
                    insideItem && isTag(parser, "content", "media") -> {
                        imageUrl = parser.getAttributeValue(null, "url")
                            ?: parser.getAttributeValue(MEDIA_NAMESPACE, "url")
                    }
                    insideItem && isTag(parser, "thumbnail", "media") -> {
                        imageUrl = parser.getAttributeValue(null, "url")
                            ?: parser.getAttributeValue(MEDIA_NAMESPACE, "url")
                    }
                    insideItem && parser.name == "enclosure" -> {
                        val type = parser.getAttributeValue(null, "type").orEmpty()
                        if (type.startsWith("image/")) {
                            imageUrl = parser.getAttributeValue(null, "url")
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        articles += Article(
                            title = title,
                            link = link,
                            author = author,
                            publishedAt = publishedAt,
                            categories = categories.filter { it.isNotBlank() },
                            description = description,
                            imageUrl = imageUrl,
                            guid = guid.ifBlank { link }
                        )
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }

        return articles
    }

    @Suppress("DEPRECATION")
    private fun htmlToText(html: String): String {
        val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(html)
        }

        return spanned
            .toString()
            .replace('\u00A0', ' ')
            .trim()
    }

    private fun extractFirstImageUrl(html: String): String? {
        val matcher = IMAGE_TAG_PATTERN.matcher(html)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun isTag(parser: XmlPullParser, localName: String, prefix: String): Boolean {
        return parser.name == localName && (parser.prefix == prefix || parser.namespace == namespaceFor(prefix))
    }

    private fun namespaceFor(prefix: String): String? = when (prefix) {
        "media" -> MEDIA_NAMESPACE
        "dc" -> DC_NAMESPACE
        else -> null
    }

    private fun formatPublishedAt(raw: String): String {
        return try {
            OffsetDateTime.parse(raw, RSS_DATE_FORMATTER)
                .atZoneSameInstant(TOKYO_ZONE_ID)
                .format(DISPLAY_DATE_FORMATTER)
        } catch (_: DateTimeParseException) {
            raw
        }
    }

    private companion object {
        val IMAGE_TAG_PATTERN: Pattern = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        const val MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/"
        const val DC_NAMESPACE = "http://purl.org/dc/elements/1.1/"
        val RSS_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.JAPAN)
        val TOKYO_ZONE_ID: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}
