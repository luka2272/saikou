package ani.saikou.parsers.manga

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.BaseParser
import ani.saikou.parsers.MangaReadSources
import ani.saikou.parsers.manga.sources.AllAnime
import ani.saikou.parsers.manga.sources.ComickFun
import ani.saikou.parsers.manga.sources.Manga4Life
import ani.saikou.parsers.manga.sources.MangaBuddy
import ani.saikou.parsers.manga.sources.MangaDex
import ani.saikou.parsers.manga.sources.MangaHub
import ani.saikou.parsers.manga.sources.MangaKakalot
import ani.saikou.parsers.manga.sources.MangaKatana
import ani.saikou.parsers.manga.sources.MangaPill
import ani.saikou.parsers.manga.sources.MangaRead
import ani.saikou.parsers.manga.sources.Manhwa18
import ani.saikou.parsers.manga.sources.NHentai
import ani.saikou.parsers.manga.sources.NineHentai

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "MangaKakalot" to ::MangaKakalot,
        "MangaBuddy" to ::MangaBuddy,
        "MangaPill" to ::MangaPill,
        "MangaDex" to ::MangaDex,
//        "MangaReaderTo" to ::MangaReaderTo,
        "AllAnime" to ::AllAnime,
        "Toonily" to ::Toonily,
//        "MangaHub" to ::MangaHub, cf blocked
        "MangaKatana" to ::MangaKatana,
        "Manga4Life" to ::Manga4Life,
        "MangaRead" to ::MangaRead,
//        "ComickFun" to ::ComickFun,

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
