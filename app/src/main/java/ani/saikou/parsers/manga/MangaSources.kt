package ani.saikou.parsers.manga

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.BaseParser
import ani.saikou.parsers.MangaReadSources
import ani.saikou.parsers.manga.sources.AllAnime
import ani.saikou.parsers.manga.sources.AsuraScans
import ani.saikou.parsers.manga.sources.Manga4Life
import ani.saikou.parsers.manga.sources.MangaBuddy
import ani.saikou.parsers.manga.sources.MangaDex
import ani.saikou.parsers.manga.sources.MangaKakalot
import ani.saikou.parsers.manga.sources.MangaKatana
import ani.saikou.parsers.manga.sources.MangaPill
import ani.saikou.parsers.manga.sources.MangaRead
import ani.saikou.parsers.manga.sources.Manhwa18
import ani.saikou.parsers.manga.sources.MmScans
import ani.saikou.parsers.manga.sources.NineHentai
import ani.saikou.parsers.manga.sources.Toonily

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "AsuraScans" to ::AsuraScans,
        "MangaKakalot" to ::MangaKakalot,
        "MangaBuddy" to ::MangaBuddy,
        "MangaPill" to ::MangaPill,
        "MangaDex" to ::MangaDex,
        "AllAnime" to ::AllAnime,
        "Toonily" to ::Toonily,
        "MmScans" to ::MmScans,
        "MangaKatana" to ::MangaKatana,
        "Manga4Life" to ::Manga4Life,
        "MangaRead" to ::MangaRead

    )
}

object HMangaSources : MangaReadSources() {
    private val aList: List<Lazier<BaseParser>> = lazyList(
        "NineHentai" to ::NineHentai,
        "Manhwa18" to ::Manhwa18,
    )
    override val list = listOf(aList, MangaSources.list).flatten()
}
