package ani.saikou.parsers.anime.sources

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.anime.Video
import ani.saikou.parsers.anime.VideoContainer
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.VideoType
import ani.saikou.parsers.anime.extractors.FileMoon
import ani.saikou.parsers.anime.extractors.Mp4Upload
import ani.saikou.parsers.anime.extractors.StreamTape
import ani.saikou.tryWithSuspend
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.net.URL

class AniWave : AnimeParser() {

    override val name = "AniWave"
    override val saveName = "aniwave_to"
    override val hostUrl = "https://aniwave.to"
    override val malSyncBackupName = "9anime"
    override val isDubAvailableSeparately = true

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val animeId = client.get(animeLink).document.select("#watch-main").attr("data-id")
        val body = client.get("$hostUrl/ajax/episode/list/$animeId?vrf=${encodeVrf(animeId)}").parsed<Response>().result
        return Jsoup.parse(body).body().select("ul > li > a").mapNotNull {
            val id = it.attr("data-ids").split(",")
                .getOrNull(if (selectDub) 1 else 0) ?: return@mapNotNull null
            val num = it.attr("data-num")
            val title = it.selectFirst("span.d-title")?.text()
            println(title)
            val filler = it.hasClass("filler")
            Episode(num, id, title, isFiller = filler)
        }
    }

    private val embedHeaders = mapOf("referer" to "$hostUrl/")

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?
    ): List<VideoServer> {
        val body = client.get("$hostUrl/ajax/server/list/$episodeLink?vrf=${encodeVrf(episodeLink)}").parsed<Response>().result
        val document = Jsoup.parse(body)
        return document.select("li").mapNotNull {
            val name = it.text()
            val encodedStreamUrl = getEpisodeLinks(it.attr("data-link-id"))?.result?.url ?: return@mapNotNull null
            val realLink = FileUrl(decodeVrf(encodedStreamUrl), embedHeaders)
            VideoServer(name, realLink)
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val extractor: VideoExtractor? = when (server.name) {
            "vidplay"     -> Extractor(server)
            "Vidstream"     -> Extractor(server)
            "MyCloud"       -> Extractor(server)
            "Streamtape"    -> StreamTape(server)
            "Filemoon"    -> FileMoon(server)
            "Mp4upload"     -> Mp4Upload(server)
            else            -> null
        }
        return extractor
    }

    class Extractor(override val server: VideoServer) : VideoExtractor() {

        @Serializable
        data class Data(
            val result: Media? = null
        ) {
            @Serializable
            data class Media(
                val sources: List<Source>? = null
            ) {
                @Serializable
                data class Source(
                    val file: String? = null
                )
            }
        }
        @Serializable
        data class Response (
            val rawURL: String? = null
        )

        override suspend fun extract(): VideoContainer {
            val slug = URL(server.embed.url).path.substringAfter("e/")
            val isMyCloud = server.name == "MyCloud"
            val server = if (isMyCloud) "Mcloud" else "Vizcloud"
            val url = "https://9anime.eltik.net/raw$server?query=$slug&apikey=saikou"
            println("bal url: $url")
            val apiUrl = client.get(url).parsed<Response>().rawURL
            var videos: List<Video> = emptyList()

            if(apiUrl != null) {
                val referer = if (isMyCloud) "https://mcloud.to/" else "https://9anime.to/"
                videos =  client.get(apiUrl, referer = referer).parsed<Data>().result?.sources?.mapNotNull { s ->
                    s.file?.let { Video(null,VideoType.M3U8,it) }
                } ?: emptyList()
            }
            return  VideoContainer(videos)
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val vrf = encodeVrf(query)
        val searchLink =
            "$hostUrl/filter?language%5B%5D=${if (selectDub) "dub" else "sub"}&keyword=${encode(query)}&vrf=${vrf}&page=1"
        println("The value of the variable is: $searchLink")
        return client.get(searchLink).document.select("#list-items div.ani.poster.tip > a").map {
            val link = hostUrl + it.attr("href")
            println(link)
            val img = it.select("img")
            val title = img.attr("alt")
            val cover = img.attr("src")
            ShowResponse(title, link, cover)
        }
    }

//    override suspend fun loadByVideoServers(episodeUrl: String, extra: Map<String, String>?, callback: (VideoExtractor) -> Unit) {
//        tryWithSuspend {
//            val servers = loadVideoServers(episodeUrl, extra).map { getVideoExtractor(it) }
//            val mutex = Mutex()
//            servers.asyncMap {
//                tryWithSuspend {
//                    it?.apply {
//                        if (this is VizCloud) {
//                            mutex.withLock {
//                                load()
//                                callback.invoke(this)
//                            }
//                        } else {
//                            load()
//                            callback.invoke(this)
//                        }
//                    }
//                }
//            }
//        }
//    }

    @Serializable
    private data class Links(val result: Url?) {
        @Serializable
        data class Url(val url: String?)
    }

    @Serializable
    data class Response(val result: String)

    private suspend fun getEpisodeLinks(id: String): Links? {
        return tryWithSuspend { client.get("$hostUrl/ajax/server/$id?vrf=${encodeVrf(id)}").parsed() }
    }

    @Serializable
    data class SearchData (
        val url: String
    )
    private suspend fun encodeVrf(text: String): String {
        return client.get("https://9anime.eltik.net/vrf?query=$text&apikey=saikou").parsed<SearchData>().url
    }

    private suspend fun decodeVrf(text: String): String {
        return client.get("https://9anime.eltik.net/decrypt?query=$text&apikey=saikou").parsed<SearchData>().url
    }
}
