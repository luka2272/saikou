package ani.saikou.parsers.manga.sources

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.manga.MangaChapter
import ani.saikou.parsers.manga.MangaImage
import ani.saikou.parsers.manga.MangaParser
import ani.saikou.parsers.ShowResponse
import ani.saikou.printIt

class MangaPill : MangaParser() {

    override val name = "MangaPill"
    override val saveName = "manga_pill"
    override val hostUrl = "https://mangapill.com"

    val headers = mapOf("referer" to hostUrl)

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        return client.get(mangaLink).document.select("#chapters > div > a").reversed().map {
            val chap = it.text().replace("Chapter ", "")
            MangaChapter(chap, hostUrl + it.attr("href"))
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        return client.get(chapterLink).document.select("img.js-page").map {
            MangaImage(FileUrl(it.attr("data-src"), headers))
        }

    }

    override suspend fun search(query: String): List<ShowResponse> {
        val link = "$hostUrl/quick-search?q=${encode(query)}"
        return client.get(link).document.select(".bg-card").map {
            ShowResponse(
                it.select(".font-black").text(),
                hostUrl + it.attr("href"),
                FileUrl(it.select("img").attr("src"), headers)
            )
        }

    }

}
