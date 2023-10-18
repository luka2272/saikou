package ani.saikou.parsers.anime.extractors

import ani.saikou.client
import ani.saikou.parsers.anime.Video
import ani.saikou.parsers.anime.VideoContainer
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.VideoType
import org.json.JSONObject

class Animeflix(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val response = client.get(server.embed.url,
            headers = mapOf("accept-language" to "en-US,en;q=0.9")
        )
        val responseBody = response.body?.string()
        val json = JSONObject(responseBody)
        val source = json.getString("source")

        println("vajha vahj as : $source")

        val player = client.get(source, headers = mapOf("accept-language" to "en-US,en;q=0.9")).document.html()
        val url = Regex("const source = `([^`]+)`").find(player)!!.groups[1]!!.value
        return VideoContainer(listOf(Video(null, VideoType.M3U8, url)))
    }

}