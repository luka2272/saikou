package ani.saikou.parsers.anime

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.BaseParser
import ani.saikou.parsers.WatchSources
import ani.saikou.parsers.anime.sources.AllAnime
import ani.saikou.parsers.anime.sources.AniWave
import ani.saikou.parsers.anime.sources.AnimeDao
import ani.saikou.parsers.anime.sources.AnimePahe
import ani.saikou.parsers.anime.sources.Gogo
import ani.saikou.parsers.anime.sources.Haho
import ani.saikou.parsers.anime.sources.HentaiFF
import ani.saikou.parsers.anime.sources.HentaiMama
import ani.saikou.parsers.anime.sources.HentaiStream
import ani.saikou.parsers.anime.sources.Kaido

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
//        "Dummy" to ::AnimeDummy,
        "Gogo" to ::Gogo,
//        "AniWave" to ::AniWave,
        "AllAnime" to ::AllAnime,
        "AnimeDao" to ::AnimeDao,
//        "AnimePahe" to ::AnimePahe,
//        "Kaido" to ::Kaido,

    )
}

object HAnimeSources : WatchSources() {
    private val aList: List<Lazier<BaseParser>>  = lazyList(
        "Haho" to ::Haho,
        "HentaiMama" to ::HentaiMama,
        "HentaiStream" to ::HentaiStream,
        "HentaiFF" to ::HentaiFF,

    )

    override val list = listOf(aList, AnimeSources.list).flatten()
}
