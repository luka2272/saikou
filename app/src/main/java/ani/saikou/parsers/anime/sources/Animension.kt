package ani.saikou.parsers.anime.sources

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.extractors.ALions
import ani.saikou.parsers.anime.extractors.AWish
import ani.saikou.parsers.anime.extractors.DoodStream
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.VidStreaming
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Animension : AnimeParser() {
    override val name = "Animension"
    override val saveName = "Animension"
    override val hostUrl = "https://animension.to/public-api"
    override val malSyncBackupName = "Animension"
    override val isDubAvailableSeparately = true


    override suspend fun loadEpisodes(
        animeLink: String, extra: Map<String, String>?
    ): List<Episode> {
        val encodedAnimeLink = encode(animeLink)
        val apiResponse = client.get(
            "$hostUrl/episodes.php?id=$encodedAnimeLink",
            headers = mapOf("accept-language" to "en-US,en;q=0.9")
        )
        val jsonResponse = apiResponse.text
        val jsonArray = Json.decodeFromString<List<JsonElement>>(jsonResponse)
        val episodes = jsonArray.mapNotNull { element ->
            if (element is JsonArray && element.size >= 4) {
                val number = element[2].jsonPrimitive.content
                val title = "Episode $number"
                val image = null
                val link = "$hostUrl/episode.php?id=${element[1].jsonPrimitive.content}"

                Episode(number, link, title, image)
            } else {
                null
            }
        }
        return episodes.reversed()
    }

    override suspend fun loadVideoServers(
        episodeLink: String, extra: Map<String, String>?
    ): List<VideoServer> {
        val response = client.get("https://saik0u.repl.co/jsonfix?json=$episodeLink")
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = json.parseToJsonElement(response.toString()).jsonObject
        val videoServers = mutableListOf<VideoServer>()
        jsonObject.forEach { (name, url) ->
            val embed = FileUrl(url.jsonPrimitive.content, mapOf("referer" to hostUrl))
            videoServers.add(VideoServer(name.replace("-embed", ""), embed))
        }
        return videoServers
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "cdn" in domain -> VidStreaming(server)
            "goone" in domain -> GogoCDN(server)
            "goload" in domain -> GogoCDN(server)
            "dood" in domain -> DoodStream(server)
            "alions" in domain -> ALions(server)
            "awish" in domain -> AWish(server)
            else -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val apiResponse = client.get(
            hostUrl + "/search.php?search_text=${encode(query)}&season=${if (selectDub) "&dub=1" else "&dub=0"}&genres=&airing=&sort=popular-week&page=1/",
            headers = mapOf("accept-language" to "en-US,en;q=0.9")
        )
        val jsonResponse = apiResponse.text
        val jsonArray = Json.decodeFromString<List<JsonElement>>(jsonResponse)
        val extractedData = jsonArray.mapNotNull { element ->
            if (element is JsonArray && element.size >= 3) {
                val english = element[0].jsonPrimitive.content
                val slug = element[1].jsonPrimitive.content
                val imageMedium = element[2].jsonPrimitive.content
                ShowResponse(english, slug, imageMedium)
            } else {
                null
            }
        }
        return extractedData
    }

}