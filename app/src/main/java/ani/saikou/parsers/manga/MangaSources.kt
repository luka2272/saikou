package ani.saikou.parsers.manga

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.BaseParser
import ani.saikou.parsers.MangaReadSources
import ani.saikou.parsers.manga.sources.MangaDummy

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "MangaKakalot" to ::MangaKakalot,
        "MangaBuddy" to ::MangaBuddy,
        "MangaPill" to ::MangaPill,
        "MangaDex" to ::MangaDex,
//        "MangaReaderTo" to ::MangaReaderTo,
        "AllAnime" to ::AllAnime,
        "Toonily" to ::Toonily,
        "MangaHub" to ::MangaHub,
        "MangaKatana" to ::MangaKatana,
        "Manga4Life" to ::Manga4Life,
        "MangaRead" to ::MangaRead,
        "ComickFun" to ::ComickFun,

    )
}

object HMangaSources : MangaReadSources() {
    private val aList: List<Lazier<BaseParser>> = lazyList(
        "NineHentai" to ::NineHentai,
        "Manhwa18" to ::Manhwa18,
        "NHentai" to ::NHentai,
    )
    override val list = listOf(aList, MangaSources.list).flatten()
}
