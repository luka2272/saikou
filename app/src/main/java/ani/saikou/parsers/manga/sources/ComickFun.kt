package ani.saikou.parsers.manga.sources

import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.manga.MangaChapter
import ani.saikou.parsers.manga.MangaImage
import ani.saikou.parsers.manga.MangaParser
import ani.saikou.parsers.ShowResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ComickFun : MangaParser() {

    override val name = "ComickFun"
    override val saveName = "comick_fun"
    override val hostUrl = "https://api.comick.app"

    override suspend fun search(query: String): List<ShowResponse> {
        val resp = Mapper.parse<List<SearchData>>(client.get("https://api.comick.app/v1.0/search?q=${encode(query)}").text)

        return resp.map { manga ->
            val coverimg = "https://meo.comick.pictures/llxWY.jpg"

            val mangaLink = "$hostUrl/comic/${manga.hid}/chapters"
            ShowResponse(manga.title,mangaLink,coverimg ,manga.md_titles.map { it.title })
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val resp = client.get(mangaLink).parsed<MangaChapterData>()
        return resp.chapters.reversed().map {
            val chapterLink = "$hostUrl/chapter/${it.hid}?tachiyomi=true"
            MangaChapter(number = it.chap.toString(), link = chapterLink, title = it.title)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val resp = client.get(chapterLink).parsed<MangaImageData>()
        return resp.chapter.images.map { MangaImage(url = it.url) }
    }

    @Serializable
    private data class SearchData(
        @SerialName("title") val title: String,
        @SerialName("id") val id: Int,
        @SerialName("hid") val hid: String,
        @SerialName("slug") val slug: String,
        @SerialName("md_titles") val md_titles: List<MdTitles>, // other titles
        @SerialName("md_covers") val md_covers: String,
        @SerialName("b2key") val b2key: String,
    ) {
        @Serializable
        data class MdTitles(
            @SerialName("title") val title: String,
        )
    }

    @Serializable
    private data class MangaChapterData(
        @SerialName("chapters") val chapters: List<Chap>
    )

    @Serializable
    private data class Chap(
        val chap: String? = null,
        val title: String? = null,
        val vol: String? = null,
        val lang: String? = null,
        val hid: String? = null,
    )

    @Serializable
    private data class MangaImageData(@SerialName("chapter") val chapter: Chapter) {

        @Serializable
        data class Chapter(@SerialName("images") val images: List<Image>) {

            @Serializable
            data class Image(@SerialName("url") val url: String)
        }
    }

}