package ani.saikou.parsers.manga.sources

import ani.saikou.connections.anilist.Anilist
import ani.saikou.client
import ani.saikou.parsers.manga.MangaChapter
import ani.saikou.parsers.manga.MangaImage
import ani.saikou.parsers.manga.MangaParser
import ani.saikou.parsers.ShowResponse
import ani.saikou.tryWithSuspend
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.DecimalFormat

class AllAnime : MangaParser() {
    override val name = "AllAnime"
    override val saveName = "all_anime_manga"
    override val hostUrl = "https://allmanga.to"

    private val ytAnimeCoversHost = "https://wp.youtube-anime.com/aln.youtube-anime.com"
    private val idRegex = Regex("${hostUrl}/manga/(\\w+)")
    private val epNumRegex = Regex("/[sd]ub/(\\d+)")

    private val idHash = "affde0163d56435e1c5378d50b3ef0b3aa614c83936a6833e6e5f0fd142055b4"
    private val episodeInfoHash = "c8f3ac51f598e630a1d09d7f7fb6924cff23277f354a23e473b962a367880f7d"
    private val searchHash = "a27e57ef5de5bae714db701fb7b5cf57e13d57938fc6256f7d5c70a975d11f3d"
    private val chapterHash = "d877ecac37a54bd0599836d275acbb30a53d6ff50780c5da1f6c76048eebb388"

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val showId = idRegex.find(mangaLink)?.groupValues?.get(1)!!
        val episodeInfos = getEpisodeInfos(showId)!!
        val format = DecimalFormat("#####.#####")

        return episodeInfos.sortedBy { it.episodeIdNum }.map { epInfo ->
            val link = """${hostUrl}/manga/$showId/chapters/sub/${epInfo.episodeIdNum}"""
            val epNum = format.format(epInfo.episodeIdNum).toString()
            MangaChapter(epNum, link, epInfo.notes)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val showId = idRegex.find(chapterLink)?.groupValues?.get(1)!!
        val episodeNum = epNumRegex.find(chapterLink)?.groupValues?.get(1)!!
        val variables = """{"mangaId":"$showId","translationType":"sub","chapterString":"$episodeNum","limit":1000000}"""
        val chapterPages = graphqlQuery(variables, chapterHash)?.data?.chapterPages?.edges
        // For future reference: If pictureUrlHead is null then the link provided is a relative link of the "apivtwo" variety, but it doesn't seem to contain useful images
        val chapter = chapterPages?.filter { !it.pictureUrlHead.isNullOrEmpty() }?.get(0)!!
        return chapter.pictureUrls.sortedBy { it.num }
            .map { MangaImage("""${chapter.pictureUrlHead}${it.url}""") }

    }

    override suspend fun search(query: String): List<ShowResponse> {

        val variables =
            """{"search":{"query":"$query","isManga":true},"limit":26,"page":1,"translationType":"sub","countryOrigin":"ALL"}"""
    val edges =
            graphqlQuery(variables, searchHash)?.data?.mangas?.edges!!

        return edges.map { show ->
            val link = """${hostUrl}/manga/${show.id}"""
            val otherNames = mutableListOf<String>()
            show.englishName?.let { otherNames.add(it) }
            show.nativeName?.let { otherNames.add(it) }
            show.altNames?.forEach { otherNames.add(it) }
            var thumbnail = show.thumbnail
            if (thumbnail.startsWith("mcovers")) {
                thumbnail = "$ytAnimeCoversHost/$thumbnail"
            }
            ShowResponse(show.name, link, thumbnail, otherNames, show.availableChapters.sub)
        }
    }

    private suspend fun graphqlQuery(variables: String, persistHash: String): Query? {
        val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"$persistHash"}}"""
        val graphqlUrl = ("https://api.allanime.day/api").toHttpUrl().newBuilder().addQueryParameter("variables", variables)
            .addQueryParameter("extensions", extensions).build()
        val headers = mutableMapOf<String, String>()
        headers["Host"] = "api.allanime.day"
        headers["origin"] = "https://allmanga.to"
        return tryWithSuspend {
            client.get(graphqlUrl.toString(), headers).parsed()
        }
    }

    private suspend fun getEpisodeInfos(showId: String): List<EpisodeInfo>? {
        val variables = """{"ids": "$showId"}"""

        val manga = graphqlQuery(variables, idHash)?.data?.manga
        println("manga info : $manga")
        if (manga != null) {
            val epCount = manga.availableChapters.sub
            val epVariables = """{"showId":"manga@$showId","episodeNumStart":0,"episodeNumEnd":${epCount}}"""
            return graphqlQuery(
                epVariables,
                episodeInfoHash
            )?.data?.episodeInfos
        }

        return null
    }

    @Serializable
    private data class Query(
        @SerialName("data") var data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("manga") val manga: Manga?,
            @SerialName("mangas") val mangas: MangasConnection?,
            @SerialName("episodeInfos") val episodeInfos: List<EpisodeInfo>?,
            @SerialName("chapterPages") val chapterPages: ChapterConnection?,
        )

        @Serializable
        data class MangasConnection(
            @SerialName("edges") val edges: List<Manga>
        )

        @Serializable
        data class Manga(
            @SerialName("_id") val id: String,
            @SerialName("name") val name: String,
            @SerialName("description") val description: String?,
            @SerialName("englishName") val englishName: String?,
            @SerialName("nativeName") val nativeName: String?,
            @SerialName("thumbnail") val thumbnail: String,
            @SerialName("availableChapters") val availableChapters: AvailableChapters,
            @SerialName("altNames") val altNames: List<String>?
        )

        @Serializable
        data class AvailableChapters(
            @SerialName("sub") val sub: Int,
            @SerialName("raw") val raw: Int
        )

        @Serializable
        data class ChapterConnection(
            @SerialName("edges") val edges: List<Chapter>
        ) {
            @Serializable
            data class Chapter(
                @SerialName("pictureUrls") val pictureUrls: List<PictureUrl>,
                @SerialName("pictureUrlHead") val pictureUrlHead: String?
            )

            @Serializable
            data class PictureUrl(
                @SerialName("num") val num: Int,
                @SerialName("url") val url: String

            )
        }
    }

    @Serializable
    private data class EpisodeInfo(
        // Episode "numbers" can have decimal values, hence float
        @SerialName("episodeIdNum") val episodeIdNum: Float,
        @SerialName("notes") val notes: String?,
        @SerialName("thumbnails") val thumbnails: List<String>?,
    )

}
