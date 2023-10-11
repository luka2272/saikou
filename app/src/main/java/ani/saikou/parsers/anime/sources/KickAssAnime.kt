package ani.saikou.parsers.anime.sources

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.extractors.*
import kotlinx.serialization.Serializable
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder

//couldnt fix this time, will try some day again..
class KickAssAnime : AnimeParser() {

    override val name: String = "KickAssAnime"
    override val saveName: String = "kick_ass_anime"
    override val hostUrl: String = "https://kickassanime.am"
    override val isDubAvailableSeparately: Boolean = true

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val apiUrl = "https://kickassanime.am/api/show/berserk-of-gluttony-e3be/episodes?ep=1&lang=ja-JP"
        val response = client.get(apiUrl)



        if (response.isSuccessful) {
            val json = response.body?.string() ?: ""

            try {
                val jsonObject = JSONObject(json)
                val resultArray = jsonObject.getJSONArray("result")

                val episodes = mutableListOf<Episode>()

                for (i in 0 until resultArray.length()) {
                    val episodeObject = resultArray.getJSONObject(i)
                    val title = episodeObject.getString("title")
                    val episodeNumber = episodeObject.getString("episode_string")
                    val link = "https://kickassanime.am/api/show/berserk-of-gluttony-e3be/episode/ep-" + episodeObject.getString("episode_number") + "-" + episodeObject.getString("slug")
                    episodes.add(Episode(episodeNumber, link, title))
                }
                return episodes.reversed()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return emptyList()
    }


    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if (selectDub) " (Dub)" else "")

        // Fetch JSON data from the API
        val apiResponse = client.get("https://api.misfitsdev.co/saikou.php?q=$encoded")

        // Extract the JSON data from the response
        val jsonResponse = apiResponse.toString()

        // Parse the JSON into the SearchResponse data class
        val json = Mapper.parse<SearchResponse>(jsonResponse)

        return json.animes.mapNotNull { anime ->
            // KA seems to have no way to differentiate between Dub and Sub
            // so we simply remove those with "(Dub)" in the title.
            if (!selectDub && anime.name.contains("(Dub)")) return@mapNotNull null
            ShowResponse(
                name = anime.name,
                link = anime.slug,
                coverUrl = FileUrl(anime.image, mapOf(
                    "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:101.0) Gecko/20100101 Firefox/101.0"
                )),
            )
        }
    }


//    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String,String>?): List<VideoServer> {
//
//        val (kaastLinks, externalServers) = this.getVideoPlayerLink(episodeLink)
//
//        val serverLinks = mutableListOf<VideoServer>()
//        kaastLinks.forEach { pageLink ->
//            if (pageLink == null || pageLink.isBlank()) return@forEach
//
//            // These are not yet implemented
//            if (pageLink.contains("mobile")) return@forEach
//
//            val tag =
//                client.get(pageLink).document.getElementsByTag("script").reversed()[7].toString()
//            val jsonSlice =
//                "{\"sources\":"
//                    .plus(tag.substringAfter("var sources = ").substringBefore("}];"))
//                    .plus("}]}")
//
//            val json = Mapper.parse<EpisodePage>(jsonSlice)
//
//            json.sources.forEach { server ->
//                serverLinks.add(videoServerTemplate(server.name, server.src))
//            }
//        }
//
//        externalServers.forEach { server ->
//            serverLinks.add(videoServerTemplate(server.name, server.link))
//        }
//        return serverLinks
//    }

//    override suspend fun loadVideoServers(
//        episodeLink: String,
//        extra: Map<String, String>?
//    ): List<VideoServer> {
//        val baseUrl = "https:"
//        return client.get(episodeLink)
//            .document
//            .select(".anime_muti_link a")
//            .map {
//                val dataVideo = it.attr("data-video")
//                val videoUrl = if (dataVideo.startsWith("http://") || dataVideo.startsWith("https://")) {
//                    dataVideo // URL already has a scheme
//                } else {
//                    baseUrl + dataVideo // Add the base URL if the scheme is missing
//                }
//                VideoServer(it.text(), videoUrl)
//            }
//    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String, String>?): List<VideoServer> {
        val apiUrl = "https://kickassanime.am/api/show/berserk-of-gluttony-e3be/episode/ep-1-fb4bbc"  // Replace with the actual API URL
        val response = client.get(apiUrl)

        if (response.isSuccessful) {
            val json = response.body?.string() ?: ""

            try {
                val jsonObject = JSONObject(json)
                val serversArray = jsonObject.getJSONArray("servers")
                val videoServers = mutableListOf<VideoServer>()

                for (i in 0 until serversArray.length()) {
                    val serverObject = serversArray.getJSONObject(i)
                    val serverName = serverObject.getString("name")
                    val videoUrl = serverObject.getString("src")

                    videoServers.add(VideoServer(serverName, videoUrl))
                }

                println("kick v server: $videoServers")
                return videoServers
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return emptyList()
    }


        override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: ""
            val header = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Referer" to "https://kaast1.com/",
                "Sec-Fetch-Dest" to "iframe"
            )
        val extractor: VideoExtractor? = when {
            "vidco" in domain    -> GogoCDN(server)
            "vidco" in domain  -> KickAssAnimeV2(server)
//            "awish" in domain  -> AWish(server)
//            "mp4upload" in domain -> Mp4Upload(server)
//            "alions" in domain -> ALions(server)
//            "dood" in domain    -> DoodStream(server)
            else                 -> null
        }
            println("vdo extract: $extractor")
        return extractor

    }


    private fun videoServerTemplate(name: String, src: String): VideoServer {
        return VideoServer(
            name = name,
            embed =
            FileUrl(
                url = URLDecoder.decode(src, "utf-8"),
                headers =
                mapOf(
                    "Accept" to
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Referer" to "https://kaast1.com/",
                    "Sec-Fetch-Dest" to "iframe"
                )
            ),
            extraData =
            mapOf(
                "id" to name.lowercase(),
                "data_segment" to
                        URLDecoder.decode(src.split("&data=")?.elementAtOrNull(1) ?: "", "utf-8")
            )
        )
    }

    @Serializable
    data class SearchResponseEpisodes(val result: List<EpisodeSearchResult>) {
        @Serializable
        data class EpisodeSearchResult(
            val epnum: String,
            val name: String?,
            val slug: String,
            val createddate: String,
            val num: String,
        )
    }

    @Serializable
    data class SearchResponse(val animes: List<AnimeSearchResult>) {
        @Serializable
        data class AnimeSearchResult(
            val name: String,
            val slug: String, // NOTE: This is just the SLUG, not the entire absolute url
            val poster: String, // The actual posterID (e.g 40319.jpg)
            val image:
            String, // The directory which contains poster images (e.g
            // www2.kickassanime.ro/uploads)
        )
    }

    @Serializable
    data class KAASTLink(val episode: KAASTObject, val ext_servers: MutableList<ExternalServer>) {

        @Serializable
        data class KAASTObject(

            // NOTE: name, title, slug, dub are also available from this object
            var link1: String?,
            var link2: String?,
            var link3: String?,
            var link4: String?,
        ) {

            // This allows us to do `episode.forEach { link -> ... }`
            suspend fun forEach(action: suspend (String) -> Unit) {
                listOfNotNull(link1, link2, link3, link4).forEach { action(it) }
            }
        }

        @Serializable
        data class ExternalServer(
            val name: String,
            val link: String,
        )
    }

    @Serializable
    data class EpisodePage(val sources: List<EpisodePageJSON>) {
        @Serializable
        data class EpisodePageJSON(
            val name: String,
            val src: String,
            val rawSrc: String,
        )
    }
}