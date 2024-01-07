/*
 * KurobaEx - *chan browser https://github.com/K1rakishou/Kuroba-Experimental/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.k1rakishou.chan.core.site.sites.ylilauta

import android.text.TextUtils
import com.github.k1rakishou.chan.core.site.ChunkDownloaderSiteProperties
import com.github.k1rakishou.chan.core.site.Site
import com.github.k1rakishou.chan.core.site.SiteAuthentication
import com.github.k1rakishou.chan.core.site.Site.SiteFeature
import com.github.k1rakishou.chan.core.site.SiteIcon
import com.github.k1rakishou.chan.core.site.common.CommonClientException
import com.github.k1rakishou.chan.core.site.common.CommonSite
import com.github.k1rakishou.chan.core.site.common.MultipartHttpCall
import com.github.k1rakishou.chan.core.site.http.DeleteRequest
import com.github.k1rakishou.chan.core.site.http.DeleteResponse
import com.github.k1rakishou.chan.core.site.http.ReplyResponse
import com.github.k1rakishou.chan.core.site.limitations.ConstantAttachablesCount
import com.github.k1rakishou.chan.core.site.limitations.ConstantMaxTotalSizeInfo
import com.github.k1rakishou.chan.core.site.limitations.SitePostingLimitation
import com.github.k1rakishou.chan.core.site.parser.CommentParserType
import com.github.k1rakishou.chan.features.reply.data.ReplyFileMeta
import com.github.k1rakishou.common.DoNotStrip
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.model.data.board.ChanBoard
import com.github.k1rakishou.model.data.descriptor.BoardDescriptor
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Pattern

@DoNotStrip
class Ylilauta : CommonSite() {
  private val chunkDownloaderSiteProperties = ChunkDownloaderSiteProperties(
    enabled = true,
    siteSendsCorrectFileSizeInBytes = true
  )

  override fun setup() {
    setEnabled(true)
    setName(SITE_NAME)
    //setIcon(SiteIcon.fromFavicon(imageLoaderV2, "https://static.ylilauta.org/img/seal_of_ylilauta-icon.svg".toHttpUrl()))
    setIcon(SiteIcon.fromFavicon(imageLoaderDeprecated, "https://static.ylilauta.org/img/pwa/ylilauta/any.png".toHttpUrl()))

    setBoards(
      // Ajankohtaista
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "bigbrother"), "Big Brother"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "juorut"), "Julkkisjuorut"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "koronavirus"), "Koronavirus"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "rikokset"), "Rikokset"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "tori"), "Tori"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "tubetus"), "Tubetus ja striimit"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "uutiset"), "Uutiset"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "ww3"), "WW3"),

      // Elämä ja ihmissuhteet
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "suhteet"), "Ihmissuhteet"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "seksuaalisuus"), "Keho ja seksi"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "koti"), "Koti ja rakentaminen"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "mielenterveys"), "Mielenterveys ja psykologia"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "opiskelu"), "Opiskelu"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "perhe"), "Perhe ja arki"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "terveys"), "Terveys"),

      // Erotiikka
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "ansat"), "Ansat"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "homoilu"), "Homoilu"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "kasityot"), "Käsityöt"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "rule34"), "Rule34"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "turri"), "Turri"),

      // Harrastukset
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "elektroniikka"), "Elektroniikka"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "esports"), "Elektroninen urheilu"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "jorma"), "Eräjorma"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "harrastukset"), "Harrastukset ja liikunta"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "japanijutut"), "Japanijutut"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "kuntosali"), "Kehonrakennus ja fitness"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "kirjallisuus"), "Kirjallisuus, lehdet ja sarjakuvat"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "luonto"), "Luonto ja eläimet"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "matkustus"), "Matkustaminen"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "muoti"), "Muoti ja pukeutuminen"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "musiikki"), "Musiikki"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "nikotiinipussit"), "Nikotiinipussit"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "pelit"), "Pelit"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "penkkiurheilu"), "Penkkiurheilu"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "ruokajajuoma"), "Ruoka ja juoma"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "televisio"), "Sarjat ja elokuvat"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "taide"), "Taide, valo- ja videokuvaus"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "diy"), "Tee se itse + korjailu"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "uhkapelit"), "Uhkapelit ja vedonlyönti"),

      // International
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "eesti"), "Eesti"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "international"), "International"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "scandinavia"), "Scandinavia"),

      // Juttelu
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "aihevapaa"), "Aihe vapaa"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "akuankka"), "Aku Ankka"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "bilderberg"), "Bilderberg-kerhohuone"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "casinofoorumi"), "Casinofoorumi"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "meemivala"), "Meemivala"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "naistenhuone"), "Naistenhuone"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "pub"), "Pub Pilkku"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "satunnainen"), "Satunnainen"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "videot"), "Videot"),

      // Teknologia
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "ajoneuvot"), "Ajoneuvot ja liikenne"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "kryptovaluutat"), "Kryptovaluutat"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "mobiili"), "Kännykät ja mobiililaitteet"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "ohjelmointi"), "Ohjelmointi"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "tekoaly"), "Tekoäly"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "masiinat"), "Tietotekniikka"),

      // Yhteiskunta ja talous
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "laki"), "Laki"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "paranormaali"), "Paranormaali, salaliitot ja mysteerit"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "sota"), "Sota ja armeija"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "talous"), "Talous ja sijoittaminen"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "tiede"), "Tiede, historia ja filosofia"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "tyo"), "Työ ja yrittäminen"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "uskonnot"), "Uskonnot"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "yhteiskunta"), "Yhteiskunta ja politiikka"),

      // Ylilauta
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "arkisto"), "Arkisto"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "joulukalenteri"), "Joulukalenteri"),
      ChanBoard.create(BoardDescriptor.create(siteDescriptor().siteName, "palaute"), "Palaute ja kehitysideat"),
    )

    setResolvable(URL_HANDLER)
    setConfig(object : CommonConfig() {
      override fun siteFeature(siteFeature: SiteFeature): Boolean {
        return super.siteFeature(siteFeature) || siteFeature === SiteFeature.POSTING
      }
    })

    setEndpoints(YlilautaEndpoints(this))
    setActions(YlilautaActions(this))
    setApi(YlilautaApi(moshi, siteManager, boardManager, this))
    setParser(YlilautaCommentParser())
    setPostingLimitationInfo(
      postingLimitationInfoLazy = lazy {
        SitePostingLimitation(
          postMaxAttachables = ConstantAttachablesCount(3),
          postMaxAttachablesTotalSize = ConstantMaxTotalSizeInfo(20 * (1024 * 1024)) // 20MB
        )
      }
    )
  }

  override fun commentParserType(): CommentParserType {
    return CommentParserType.YlilautaParser
  }

  override fun getChunkDownloaderSiteProperties(): ChunkDownloaderSiteProperties {
    return chunkDownloaderSiteProperties
  }

  inner class YlilautaEndpoints(
    private val ylilauta: Ylilauta,
    rootUrl: String = "https://ylilauta.org/",
    sysUrl: String = "https://ylilauta.org/"
  ) : CommonSite.CommonEndpoints(
    ylilauta
  ) {
    protected val root: CommonSite.SimpleHttpUrl = CommonSite.SimpleHttpUrl(rootUrl)
    protected val sys: CommonSite.SimpleHttpUrl = CommonSite.SimpleHttpUrl(sysUrl)

    val siteHost: String
      get() = DEFAULT_DOMAIN.toString()

    override fun imageUrl(boardDescriptor: BoardDescriptor, arg: Map<String, String>): HttpUrl {
      val path = requireNotNull(arg["path"]) { "\"path\" parameter not found" }

      return root.builder().s(path).url()
    }

    override fun thumbnailUrl(
      boardDescriptor: BoardDescriptor,
      spoiler: Boolean,
      customSpoilers: Int,
      arg: Map<String, String>
    ): HttpUrl {
      val thumbnail = requireNotNull(arg["thumbnail"]) { "\"thumbnail\" parameter not found" }

      return root.builder().s(thumbnail).url()
    }

    override fun boards(): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment("api")
        .addPathSegment("mobile")
        .addPathSegment("v2")
        .addPathSegment("boards")
        .build()
    }

    override fun catalog(boardDescriptor: BoardDescriptor): HttpUrl {
        return root.builder()
                .s(boardDescriptor.boardCode)
                .s("catalog.json").url()
    }

    // /api/mobile/v2/after/{board}/{thread}/{num}
    override fun threadPartial(fromPostDescriptor: PostDescriptor): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment("api")
        .addPathSegment("mobile")
        .addPathSegment("v2")
        .addPathSegment("after")
        .addPathSegment(fromPostDescriptor.boardDescriptor().boardCode)
        .addPathSegment(fromPostDescriptor.getThreadNo().toString())
        .addPathSegment(fromPostDescriptor.postNo.toString())
        .build()
    }

    // https://2ch.hk/board_code/arch/res/thread_no.json
    override fun threadArchive(threadDescriptor: ChanDescriptor.ThreadDescriptor): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment(threadDescriptor.boardCode())
        .addPathSegment("arch")
        .addPathSegment("res")
        .addPathSegment("${threadDescriptor.threadNo}.json")
        .build()
    }

    override fun pages(board: ChanBoard): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment(board.boardCode())
        .addPathSegment("catalog.json")
        .build()
    }

    override fun reply(chanDescriptor: ChanDescriptor): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment("user")
        .addPathSegment("posting")
        .build()
    }

    override fun login(): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment("user")
        .addPathSegment("passlogin")
        .addQueryParameter("json", "1")
        .build()
    }

    override fun passCodeInfo(): HttpUrl? {
      if (!actions().isLoggedIn()) {
        return null
      }

      val passcode = ""
      if (passcode.isEmpty()) {
        return null
      }

      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment("makaba")
        .addPathSegment("makaba.fcgi")
        .addQueryParameter("task", "auth")
        .addQueryParameter("usercode", passcode)
        .addQueryParameter("json", "1")
        .build()
    }

    override fun search(): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment("user")
        .addPathSegment("search")
        .addQueryParameter("json", "1")
        .build()
    }

    override fun boardArchive(boardDescriptor: BoardDescriptor, page: Int?): HttpUrl {
      val builder = HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment(boardDescriptor.boardCode)
        .addPathSegment("arch")

      if (page != null) {
        builder.addPathSegment("${page}.html")
      }

      return builder.build()
    }

    override fun icon(icon: String, arg: Map<String, String>?): HttpUrl {
      return HttpUrl.Builder()
        .scheme("https")
        .host(siteHost)
        .addPathSegment(requireNotNull(arg?.get("icon")) { "Bad arg map: $arg" })
        .build()
    }
  }

  private inner class YlilautaActions(
    private val commonSite: CommonSite
  ) : CommonActions(commonSite) {
    override fun setupPost(replyChanDescriptor: ChanDescriptor, call: MultipartHttpCall): ModularResult<Unit> {
      return ModularResult.Try {
        replyManager.get().readReply(replyChanDescriptor) { reply ->
          call.parameter("board", reply.chanDescriptor.boardCode())

          if (reply.chanDescriptor is ChanDescriptor.ThreadDescriptor) {
            call.parameter("thread", reply.chanDescriptor.threadNo.toString())
          }

          // Added with VichanAntispam.
          // call.parameter("post", "Post");
          call.parameter("password", reply.password)
          call.parameter("name", reply.postName)
          call.parameter("email", reply.options)

          if (!TextUtils.isEmpty(reply.subject)) {
            call.parameter("subject", reply.subject)
          }

          call.parameter("body", reply.comment)

          val replyFile = reply.firstFileOrNull()
          if (replyFile != null) {
            val replyFileMetaResult = replyFile.getReplyFileMeta()
            if (replyFileMetaResult is ModularResult.Error<*>) {
              throw IOException((replyFileMetaResult as ModularResult.Error<ReplyFileMeta>).error)
            }

            val replyFileMetaInfo = (replyFileMetaResult as ModularResult.Value).value
            call.fileParameter("file", replyFileMetaInfo.fileName, replyFile.fileOnDisk)

            if (replyFileMetaInfo.spoiler) {
              call.parameter("spoiler", "on")
            }
          }
        }
      }
    }

    override fun requirePrepare(): Boolean {
      return true
    }

    override suspend fun prepare(
      call: MultipartHttpCall,
      replyChanDescriptor: ChanDescriptor,
      replyResponse: ReplyResponse
    ): ModularResult<Unit> {
      val siteDescriptor = replyChanDescriptor.siteDescriptor()

      val site = siteManager.bySiteDescriptor(siteDescriptor)
        ?: return ModularResult.error(CommonClientException("Site ${siteDescriptor} is disabled or not active"))

      val desktopUrl = site.resolvable().desktopUrl(replyChanDescriptor, null)?.toHttpUrl()
        ?: return ModularResult.error(CommonClientException("Failed to get desktopUrl by chanDescriptor: $replyChanDescriptor"))

      /*
      val antispam = VichanAntispam(proxiedOkHttpClient, desktopUrl)

      val antiSpamFieldsResult = antispam.get()
      if (antiSpamFieldsResult is ModularResult.Error) {
        Logger.e(TAG, "Antispam failure", antiSpamFieldsResult.error)
        return ModularResult.error(antiSpamFieldsResult.error)
      }

      antiSpamFieldsResult as ModularResult.Value

      for ((key, value) in antiSpamFieldsResult.value) {
        call.parameter(key, value)
      }
      */

      return ModularResult.value(Unit)
    }

    override fun handlePost(replyResponse: ReplyResponse, response: Response, result: String) {
      val authMatcher = AUTH_PATTERN.matcher(result)
      val errorMatcher = errorPattern().matcher(result)

      when {
        authMatcher.find() -> {
          replyResponse.requireAuthentication = true
          replyResponse.errorMessage = result
        }
        errorMatcher.find() -> {
          replyResponse.errorMessage = Jsoup.parse(errorMatcher.group(1)).body().text()
        }
        else -> {
          val url = response.request.url
          val threadNoMatcher = THREAD_NO_PATTERN.matcher(url.encodedPath)

          try {
            if (!threadNoMatcher.find()) {
              replyResponse.errorMessage = "Failed to find threadNo pattern in server response"
              return
            }

            replyResponse.threadNo = threadNoMatcher.group(1).toLong()
            val fragment = url.encodedFragment
            if (fragment != null) {
              replyResponse.postNo = fragment.toLong()
            } else {
              replyResponse.postNo = replyResponse.threadNo
            }

            replyResponse.posted = true
          } catch (ignored: NumberFormatException) {
            replyResponse.errorMessage = "Error posting: could not find posted thread."
          }
        }
      }
    }

    override fun setupDelete(deleteRequest: DeleteRequest, call: MultipartHttpCall) {
      call.parameter("board", deleteRequest.post.boardDescriptor.boardCode)
      call.parameter("delete", "Delete")
      call.parameter("delete_" + deleteRequest.post.postNo(), "on")
      call.parameter("password", deleteRequest.savedReply.passwordOrEmptyString())

      if (deleteRequest.imageOnly) {
        call.parameter("file", "on")
      }
    }

    override fun handleDelete(response: DeleteResponse, httpResponse: Response, responseBody: String) {
      val err = errorPattern().matcher(responseBody)
      if (err.find()) {
        response.errorMessage = Jsoup.parse(err.group(1)).body().text()
      } else {
        response.deleted = true
      }
    }

    fun errorPattern(): Pattern {
      return ERROR_PATTERN
    }

    override fun postAuthenticate(): SiteAuthentication {
      return SiteAuthentication.fromNone()
    }
  }

  companion object {
    private const val TAG = "Ylilauta"
    private val DEFAULT_DOMAIN = "https://ylilauta.org".toHttpUrl()
    private val ERROR_PATTERN = Pattern.compile("<h1[^>]*>Error</h1>.*<h2[^>]*>(.*?)</h2>")
    private val THREAD_NO_PATTERN = Pattern.compile("\\/res\\/(\\d+)\\.html")
    private val AUTH_PATTERN = Pattern.compile("\"captcha\": ?true")

    const val SITE_NAME = "Ylilauta"

    val URL_HANDLER: CommonSiteUrlHandler = object : CommonSiteUrlHandler() {
      private val ROOT = "https://ylilauta.org/"

      override fun getSiteClass(): Class<out Site> {
        return Ylilauta::class.java
      }

      override val url: HttpUrl
        get() = ROOT.toHttpUrl()
      override val mediaHosts: Array<HttpUrl>
        get() = arrayOf(url)
      override val names: Array<String>
        get() = arrayOf("Ylilauta, ylilauta, Ylilauta")

      override fun desktopUrl(chanDescriptor: ChanDescriptor, postNo: Long?): String? {
        return when (chanDescriptor) {
          is ChanDescriptor.CatalogDescriptor -> {
            url.newBuilder()
              .addPathSegment(chanDescriptor.boardCode())
              .toString()
          }
          is ChanDescriptor.ThreadDescriptor -> {
            url.newBuilder()
              .addPathSegment(chanDescriptor.boardCode())
              .addPathSegment("res")
              .addPathSegment(chanDescriptor.threadNo.toString())
              .toString()
          }
          else -> null
        }
      }
    }
  }

}
