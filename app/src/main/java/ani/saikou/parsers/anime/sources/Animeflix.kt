package ani.saikou.parsers.anime.sources

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.anime.Video
import ani.saikou.parsers.anime.VideoContainer
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.VideoType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject

//All most completed but the apisode links getting expired. need to use selinum but dunno how to use it.

class Animeflix : AnimeParser() {
    override val name = "Animeflix"
    override val saveName = "Animeflix"
    override val hostUrl = "https://api.animeflix.live"
    override val malSyncBackupName = "Animeflix"
    override val isDubAvailableSeparately = true

    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {
        val qhash = client.get("https://saik0u.repl.co/aniflixHash?text=${animeLink}").toString()
        val animeid = animeLink.replace(" ", "-")

        val response = client.get(
            "https://api.animeflix.live/episodes",
            headers = mapOf("accept-language" to "en-US,en;q=0.9"),
            params = mapOf(
                "id" to animeid,
                "dub" to if (selectDub) "true" else "false",
                "c" to qhash
            )
        )
        val responseBody = response.body?.string()
        val json = JSONObject(responseBody)
        val episodesArray = json.getJSONArray("episodes")

        val episodes = mutableListOf<Episode>()
        val qhash2 = client.get("https://saik0u.repl.co/aniflixHash?text=${animeLink}").toString()

        for (i in 0 until episodesArray.length()) {
            val episodeObj = episodesArray.getJSONObject(i)
            val number = episodeObj.getString("number")
            val title = episodeObj.getString("title")
            val image = episodeObj.getString("image")
            val link = "$hostUrl/watch/$animeid-episode-$number?server=&c=$qhash2"
            episodes.add(Episode(number, link, title, image))
        }
        return episodes
    }

    private suspend fun getSourceUrl(server: String): String {
        val response = client.get(server, headers = mapOf("accept-language" to "en-US,en;q=0.9"))
        val responseBody = response.body?.string()

        if (response.isSuccessful && responseBody != null) {
            val json = JSONObject(responseBody)
            return json.optString("source", "")
        } else {
            return ""
        }
    }

    override suspend fun loadVideoServers(
        server: String,
        extra: Map<String, String>?
    ): List<VideoServer> {

        val source = getSourceUrl(server)
        println("wow loa res4: $source")

        val html = source?.let {
            client.get(
                it,
                headers = mapOf("accept-language" to "en-US,en;q=0.9")
            ).text
        }

        val serverNames =
            html?.let { extractServerNamesFromJavascript(it) } // Extract server names from JavaScript code
        val sourceUrl = html?.let { extractSourceUrlFromJavascript(it) }

        println("Source name : $serverNames")
        println("Source : $sourceUrl")

        return serverNames!!.map { serverName ->
            sourceUrl?.let {
                VideoServer(
                    name = serverName,
                    embedUrl = source,
                    extraData = mapOf(
                        "referer" to hostUrl,
                        "source" to source // Add the source URL to the extraData
                    )
                )
            }!!
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val url = server.embed.url
        val extractor = when {
            "animeflix" in url -> AnimeflixPlayer(server)
            else -> null
        }
        return extractor
    }

    private class AnimeflixPlayer(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {

            val html = server.embed.url?.let {
                client.get(
                    it,
                    headers = mapOf("accept-language" to "en-US,en;q=0.9")
                ).text
            }

            val sourceUrl = extractSourceUrlFromJavascript(html)

            println("Source : $sourceUrl")

            val link = sourceUrl  // Assign the extracted source URL to the 'link' variable

            return VideoContainer(listOf(
                FileUrl(link).let {
                    Video(null, VideoType.M3U8, it, getSize(it))
                }
            ))
        }

        private fun extractSourceUrlFromJavascript(script: String?): String {
            if (script == null) {
                return ""  // Handle the case where the script is null
            }

            val regex =
                Regex("""const source\s*=\s*`([^`]+)`""") // Matches the source variable assignment
            val match = regex.find(script)
            return match?.groupValues?.get(1) ?: ""
        }
    }


    private suspend fun getPlayerUrl(server: VideoServer): String {
        val response =
            client.get(server.toString(), headers = mapOf("accept-language" to "en-US,en;q=0.9"))
        val responseBody = response.body?.string()
        val json = JSONObject(responseBody)
        val source = json.getString("source")
        return source
    }

    private fun extractServerNamesFromJavascript(script: String): List<String> {
        val serverNames = mutableListOf<String>()
        val regex = Regex("""case\s+"([^"]+)""") // Matches case statements with server names
        val matches = regex.findAll(script)
        matches.forEach { match ->
            val serverName = match.groupValues[1] // Group 1 contains the server name
            serverNames.add(serverName)
        }
        return serverNames
    }

    fun extractSourceUrlFromJavascript(script: String): String {
        val regex =
            Regex("""const source\s*=\s*`([^`]+)`""") // Matches the source variable assignment
        val match = regex.find(script)
        return match?.groupValues?.get(1) ?: ""
    }


    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if (selectDub) " (Dub)" else "")
        val qhash = client.get("https://saik0u.repl.co/aniflixHash?text=${query.substring(0, 3)}")
            .toString()
        val apiResponse = client.get(
            "https://api.animeflix.live/info/",
            headers = mapOf("accept-language" to "en-US,en;q=0.9"),
            params = mapOf(
                "query" to query,
                "limit" to "15",
                "k" to qhash
            )
        )

        val jsonResponse =
            apiResponse.toString() // Use content to get the response body as a String
        val json = Json.decodeFromString<List<JsonObject>>(jsonResponse)
        val extractedData = json.mapNotNull { anime ->
            val title = anime.jsonObject["title"]?.jsonObject
            val english = title?.get("english")?.jsonPrimitive?.content
            val slug = anime["slug"]?.jsonPrimitive?.content
            val images = anime.jsonObject["images"]?.jsonObject
            val imageMedium = images?.get("medium")?.jsonPrimitive?.content
            if (english != null && slug != null && imageMedium != null) {
                ShowResponse(english, slug, imageMedium)
            } else {
                null
            }
        }
        return extractedData
    }


}