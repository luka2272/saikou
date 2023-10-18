package ani.saikou.parsers.manga.sources

import ani.saikou.client
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.manga.MangaChapter
import ani.saikou.parsers.manga.MangaImage
import ani.saikou.parsers.manga.MangaParser

class MmScans : MangaParser() {

    override val name = "MmScans"
    override val saveName = "Mmscans"
    override val hostUrl = "https://mm-scans.org/"

    val headers = mapOf("referer" to hostUrl)

    override suspend fun loadChapters(
        mangaLink: String, extra: Map<String, String>?
    ): List<MangaChapter> {
        return client.get(mangaLink).document.select(".chapter-li a").map { element ->
            val chap = element.select(".chapter-title-date p").text().replace("Chapter ", "")
            val href = element.attr("href")
            MangaChapter(chap, href)
        }.reversed()
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        return client.get(chapterLink).document.select(".reading-content img[data-src]")
            .mapNotNull { element ->
                val imageUrl = element.attr("data-src").trim()
                if (imageUrl.isNotEmpty()) {
                    MangaImage(url = imageUrl)
                } else {
                    null
                }
            }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val doc = client.get("$hostUrl/?s=${encode(query)}&post_type=wp-manga").document
        val data = doc.select(".tab-content-wrap  a")
        return data.zip(data).map {
            ShowResponse(
                name = it.first.attr("title"),
                link = it.first.attr("href"),
                coverUrl = it.second.select(".img-responsive").attr("data-src")
            )
        }
    }


}
