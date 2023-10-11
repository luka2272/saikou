package ani.saikou.parsers.anime.sources

import android.net.Uri
import ani.saikou.client
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.extractors.ALions
import ani.saikou.parsers.anime.extractors.AWish
import ani.saikou.parsers.anime.extractors.DoodStream
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.Mp4Upload
import ani.saikou.parsers.anime.extractors.VidStreaming

class AnimeDao : AnimeParser() {
    override val name = "AnimeDao"
    override val saveName = "anime_dao_bz"
    override val hostUrl = "https://animedao.bz"
    override val isDubAvailableSeparately = true

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val res = client.get(animeLink).document
        return res.select(".episode_well_link").map {
            Episode(
                it.select(".anime-title").text().substringAfter("Episode "),
                hostUrl + it.attr("href")
            )
        }.reversed()
    }

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?
    ): List<VideoServer> {
        val baseUrl = "https:"
        return client.get(episodeLink)
            .document
            .select(".anime_muti_link a")
            .map {
                val dataVideo = it.attr("data-video")
                val videoUrl = if (dataVideo.startsWith("http://") || dataVideo.startsWith("https://")) {
                    dataVideo // URL already has a scheme
                } else {
                    baseUrl + dataVideo // Add the base URL if the scheme is missing
                }
                VideoServer(it.text(), videoUrl)
            }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: ""
        val extractor: VideoExtractor? = when {
            "goone" in domain    -> GogoCDN(server)
            "goone" in domain  -> VidStreaming(server)
            "awish" in domain  -> AWish(server)
            "mp4upload" in domain -> Mp4Upload(server)
            "alions" in domain -> ALions(server)
            "dood" in domain    -> DoodStream(server)
            else                 -> null
        }
        return extractor

    }

    override suspend fun search(query: String): List<ShowResponse> {
        return client.get("$hostUrl/search.html?keyword=$query${if(selectDub) " (Dub)" else ""}").document
            .select(".col-lg-4 a").map {
                ShowResponse(
                    it.attr("title"),
                    hostUrl + it.attr("href"),
                    it.select("img").attr("src")
                )
            }
    }
}