package com.github.k1rakishou.chan.core.site.sites.foolfuuka.sites

import com.github.k1rakishou.chan.core.site.Site
import com.github.k1rakishou.chan.core.site.SiteIcon
import com.github.k1rakishou.chan.core.site.sites.foolfuuka.FoolFuukaActions
import com.github.k1rakishou.chan.core.site.sites.foolfuuka.FoolFuukaApi
import com.github.k1rakishou.chan.core.site.sites.foolfuuka.FoolFuukaCommentParser
import com.github.k1rakishou.chan.core.site.sites.foolfuuka.FoolFuukaEndpoints
import com.github.k1rakishou.common.DoNotStrip
import com.github.k1rakishou.common.data.ArchiveType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

@DoNotStrip
class DesuArchive : BaseFoolFuukaSite() {

  override fun rootUrl(): HttpUrl = ROOT_URL

  override fun setup() {
    setEnabled(true)
    setName(SITE_NAME)
    setIcon(SiteIcon.fromFavicon(imageLoaderV2, FAVICON_URL))
    setBoardsType(Site.BoardsType.DYNAMIC)
    setResolvable(URL_HANDLER)
    setConfig(object : CommonConfig() {})
    setEndpoints(FoolFuukaEndpoints(this, rootUrl()))
    setActions(FoolFuukaActions(this))
    setApi(FoolFuukaApi(this))
    setParser(FoolFuukaCommentParser(archivesManager))
  }

  companion object {
    val FAVICON_URL: HttpUrl = "https://desuarchive.org/favicon.ico".toHttpUrl()
    val ROOT: String = "https://desuarchive.org/"
    val ROOT_URL: HttpUrl =  ROOT.toHttpUrl()
    val SITE_NAME: String = ArchiveType.DesuArchive.domain
    val NAMES: Array<String> = arrayOf("desuarchive")
    val CLASS: Class<out Site> = DesuArchive::class.java

    val MEDIA_HOSTS: Array<HttpUrl> = arrayOf(
      ROOT_URL,
      "https://s1.desu-usergeneratedcontent.xyz/".toHttpUrl(),
      "https://s2.desu-usergeneratedcontent.xyz/".toHttpUrl(),
    )

    val URL_HANDLER = BaseFoolFuukaUrlHandler(ROOT_URL, MEDIA_HOSTS, NAMES, CLASS)
  }

}