package net.ogatomo.tomoyansblog.data

data class Article(
    val title: String,
    val link: String,
    val author: String,
    val publishedAt: String,
    val categories: List<String>,
    val description: String,
    val imageUrl: String?,
    val guid: String
)
