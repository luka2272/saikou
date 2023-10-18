package ani.saikou.parsers.manga.sources

import ani.saikou.client
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.manga.MangaChapter
import ani.saikou.parsers.manga.MangaImage
import ani.saikou.parsers.manga.MangaParser

class AsuraScans : MangaParser() {

    override val name = "AsuraToon"
    override val saveName = "asuratoon"
    override val hostUrl = "https://asuratoon.com/"

    val headers = mapOf("referer" to hostUrl)

    override suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?
    ): List<MangaChapter> {
        return client.get(mangaLink).document.select(".eph-num a").map { element ->
            val chap = element.select(".chapternum").text().replace("Chapter ", "")
            val href = element.attr("href")
            MangaChapter(chap, href)
        }.reversed()
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        return client.get(chapterLink).document.select(".rdminimal img[decoding=async][width][height]")
            .mapNotNull { element ->
                val imageUrl = element.attr("src").trim()
                if (imageUrl.isNotEmpty()) {
                    MangaImage(url = imageUrl)
                } else {
                    null
                }
            }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val doc = client.get("$hostUrl/?s=${encode(query)}").document
        val data = doc.select("div.bs > div > a")
        val imgs = data.select("img")
        return data.zip(imgs).map {
            ShowResponse(
                name = it.first.attr("title"),
                link = it.first.attr("href"),
                coverUrl = it.second.attr("src")
            )
        }
    }


}
