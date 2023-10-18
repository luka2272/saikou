package ani.saikou.parsers.anime.sources

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.extractors.ALions
import ani.saikou.parsers.anime.extractors.AWish
import ani.saikou.parsers.anime.extractors.DoodStream
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.Mp4Upload
import ani.saikou.parsers.anime.extractors.StreamSB

class SFlix : AnimeParser() {
    override val name = "SFlix"
    override val saveName = "sflix"
    override val hostUrl = "https://sflix.to"
    override val malSyncBackupName = "SFlix"
    override val isDubAvailableSeparately = false


    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {

        val lastNumbers = animeLink.replaceFirst(".*-(\\d+)$".toRegex(), "$1")
        val pageBody = client.get("$hostUrl/ajax/season/list/$lastNumbers").document
        val dataIds = pageBody.select("[data-id]")

        val dataIdList = mutableListOf<String>()
        for (element in dataIds) {
            val dataId = element.attr("data-id")
            dataIdList.add(dataId)
        }

        val list = mutableListOf<Episode>()
        for (dataId in dataIdList) {
            val episodeDetailPage = client.get("$hostUrl/ajax/season/episodes/$dataId").document
            val episodeElements =
                episodeDetailPage.select(".flw-item.film_single-item.episode-item.eps-item")
            for (episodeElement in episodeElements) {
                val imgSrc = episodeElement.select(".film-poster-img").attr("src")
                val episodeNumber =
                    episodeElement.select(".episode-number").text().replace("Episode", "").trim()
                        .replace(":", "")
                val filmName = episodeElement.select(".film-name a").attr("title")

                list.add(Episode(episodeNumber, imgSrc, filmName))
            }
        }
        return list
    }

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?
    ): List<VideoServer> {
        val baseUrl = "https:"
        return client.get("https://sflix.to/ajax/v2/episode/servers/943619")
            .document
            .select(".ulclear a")
            .map {
                val dataVideo = it.attr("data-id")
                val videoUrl = "https://sflix.to/ajax/sources/$dataVideo"
                VideoServer(it.text(), videoUrl)
            }
    }

    private fun httpsIfy(text: String): String {
        return if (text.take(2) == "//") "https:$text"
        else text
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val serverUrl = Uri.parse(server.embed.url)
        val domain = serverUrl.host ?: return null
        val path = serverUrl.path ?: return null
        val extractor: VideoExtractor? = when {
            "taku" in domain -> GogoCDN(server)
            "sb" in domain -> StreamSB(server)
            "dood" in domain -> DoodStream(server)
            "mp4" in domain -> Mp4Upload(server)
            else -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = (query).replace(" ", "-")
        val list = mutableListOf<ShowResponse>()
        val document = client.get("$hostUrl/search/$encoded").document
        val showElements = document.select(".film_list-wrap .flw-item")
        for (showElement in showElements) {
            val titleElement = showElement.select(".film-name a")
            val link = titleElement.attr("href").removePrefix("/tv/free-")
            val title = titleElement.attr("title")
            val cover = showElement.select(".film-poster-img").attr("data-src")
            list.add(ShowResponse(title, link, cover))
        }

        return list
    }

}