package com.github.k1rakishou.chan.features.remote_image_search

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.compose.AsyncData
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.helper.DialogFactory
import com.github.k1rakishou.chan.core.image.ImageLoaderDeprecated
import com.github.k1rakishou.chan.features.bypass.CookieResult
import com.github.k1rakishou.chan.features.bypass.SiteFirewallBypassController
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeErrorMessage
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeProgressIndicator
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeTextField
import com.github.k1rakishou.chan.ui.compose.components.kurobaClickable
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequest
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequestData
import com.github.k1rakishou.chan.ui.compose.image.KurobaComposeImage
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.lazylist.LazyVerticalGridWithFastScroller
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalContentPaddings
import com.github.k1rakishou.chan.ui.controller.FloatingListMenuController
import com.github.k1rakishou.chan.ui.controller.base.BaseComposeController
import com.github.k1rakishou.chan.ui.controller.base.DeprecatedNavigationFlags
import com.github.k1rakishou.chan.ui.view.floating_menu.FloatingListMenuItem
import com.github.k1rakishou.chan.ui.view.floating_menu.HeaderFloatingListMenuItem
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.findControllerOrNull
import com.github.k1rakishou.common.FirewallType
import com.github.k1rakishou.common.isNotNullNorEmpty
import com.github.k1rakishou.common.resumeValueSafe
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.util.ChanPostUtils
import com.github.k1rakishou.persist_state.ImageSearchInstanceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl
import javax.inject.Inject

class ImageSearchController(
  context: Context,
  private val onImageSelected: (HttpUrl) -> Unit
) : BaseComposeController<ImageSearchControllerViewModel, Nothing>(
  context = context,
  viewModelClass = ImageSearchControllerViewModel::class.java
) {

  @Inject
  lateinit var imageLoaderDeprecated: ImageLoaderDeprecated
  @Inject
  lateinit var dialogFactory: DialogFactory

  override fun injectActivityDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun viewModelParams(): Nothing? = null

  override fun setupNavigation() {
    updateNavigationFlags(
      newNavigationFlags = DeprecatedNavigationFlags()
    )

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { requireNavController().popController() }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.Id(R.string.image_search_controller_title),
        subtitle = null
      ),
      menuBuilder = {
        withMenuItem(
          id = ACTION_RELOAD,
          drawableId = R.drawable.ic_refresh_white_24dp,
          onClick = {
            val currentCaptchaController = findControllerOrNull { controller ->
              return@findControllerOrNull controller is SiteFirewallBypassController &&
                controller.firewallType == FirewallType.YandexSmartCaptcha
            }

            currentCaptchaController?.stopPresenting()
            controllerViewModel.reload()
          }
        )
      }
    )
  }

  override fun onPrepare() {
    controllerScope.launch {
      controllerViewModel.searchErrorToastFlow
        .debounce(350L)
        .collect { errorMessage -> showToast(errorMessage) }
    }

    controllerScope.launch {
      controllerViewModel.solvingCaptcha.collect { urlToOpen ->
        if (urlToOpen == null) {
          return@collect
        }

        val alreadyPresenting = isAlreadyPresenting { controller -> controller is SiteFirewallBypassController }
        if (alreadyPresenting) {
          return@collect
        }

        try {
          val cookieResult = suspendCancellableCoroutine<CookieResult> { continuation ->
            val controller = SiteFirewallBypassController(
              context = context,
              firewallType = FirewallType.YandexSmartCaptcha,
              headerTitleText = AppModuleAndroidUtils.getString(
                R.string.firewall_check_header_title,
                FirewallType.YandexSmartCaptcha.name
              ),
              urlToOpen = urlToOpen,
              onResult = { cookieResult -> continuation.resumeValueSafe(cookieResult) }
            )

            presentController(controller)
          }

          // Wait a second for the controller to get closed so that we don't end up in a loop
          delay(1000)

          if (cookieResult !is CookieResult.CookieValue) {
            Logger.e(TAG, "Failed to bypass YandexSmartCaptcha, cookieResult: ${cookieResult}")
            controllerViewModel.reloadCurrentPage()
            return@collect
          }

          Logger.d(TAG, "Get YandexSmartCaptcha cookies, cookieResult: ${cookieResult}")
          controllerViewModel.updateYandexSmartCaptchaCookies(cookieResult.cookie)
          controllerViewModel.reloadCurrentPage()
        } finally {
          controllerViewModel.finishedSolvingCaptcha()
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    controllerViewModel.cleanup()
  }

  @Composable
  override fun ScreenContent() {
    val chanTheme = LocalChanTheme.current
    val focusManager = LocalFocusManager.current
    val contentPaddings = LocalContentPaddings.current

    val lastUsedSearchInstanceMut by controllerViewModel.lastUsedSearchInstance
    val lastUsedSearchInstance = lastUsedSearchInstanceMut
    if (lastUsedSearchInstance == null) {
      return
    }

    val searchInstanceMut = controllerViewModel.searchInstances[lastUsedSearchInstance]
    val searchInstance = searchInstanceMut
    if (searchInstance == null) {
      return
    }

    var baseUrl by controllerViewModel.baseUrl
    var searchQuery by controllerViewModel.searchQuery
    val baseUrlError by controllerViewModel.baseUrlError

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(chanTheme.backColorCompose)
        .padding(horizontal = 8.dp)
    ) {
      val topPadding = remember(contentPaddings) {
        contentPaddings.calculateTopPadding() + 4.dp
      }

      Spacer(modifier = Modifier.height(topPadding))

      SearchInstanceSelector(
        searchInstance = searchInstance,
        onSelectorItemClicked = { showImageSearchInstances() }
      )

      Spacer(modifier = Modifier.height(8.dp))

      KurobaComposeTextField(
        value = baseUrl,
        modifier = Modifier
          .wrapContentHeight()
          .fillMaxWidth(),
        onValueChange = { newValue ->
          baseUrl = newValue
          controllerViewModel.onBaseUrlChanged(newValue)
        },
        singleLine = true,
        maxLines = 1,
        label = {
          val baseUrlErrorLocal = baseUrlError
          if (baseUrlErrorLocal.isNullOrBlank()) {
            KurobaComposeText(
              text = stringResource(id = R.string.image_search_controller_base_url_hint),
              color = chanTheme.textColorHintCompose
            )
          } else {
            KurobaComposeText(
              text = baseUrlErrorLocal,
              color = chanTheme.errorColorCompose
            )
          }
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      KurobaComposeTextField(
        value = searchQuery,
        modifier = Modifier
          .wrapContentHeight()
          .fillMaxWidth(),
        onValueChange = { newValue ->
          searchQuery = newValue
          controllerViewModel.onSearchQueryChanged(newValue)
        },
        singleLine = true,
        maxLines = 1,
        label = {
          KurobaComposeText(
            text = stringResource(id = R.string.search_query_hint),
            color = chanTheme.textColorHintCompose
          )
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      BuildImageSearchResults(
        lastUsedSearchInstance = lastUsedSearchInstance,
        onImageClicked = { searxImage ->
          focusManager.clearFocus(force = true)

          if (searxImage.fullImageUrls.isEmpty()) {
            return@BuildImageSearchResults
          }

          if (searxImage.fullImageUrls.size == 1) {
            onImageSelected(searxImage.fullImageUrls.first())
            requireNavController().popController()

            return@BuildImageSearchResults
          }

          showOptions(searxImage.fullImageUrls)
        }
      )
    }
  }

  @Composable
  private fun SearchInstanceSelector(
    searchInstance: ImageSearchInstance,
    onSelectorItemClicked: () -> Unit
  ) {
    val chanTheme = LocalChanTheme.current

    KurobaComposeText(
      text = stringResource(id = R.string.image_search_controller_current_instance),
      fontSize = 12.ktu,
      color = chanTheme.textColorHintCompose
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .kurobaClickable(
          bounded = true,
          onClick = { onSelectorItemClicked() }
        )
        .padding(vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Image(
        modifier = Modifier.size(24.dp),
        painter = painterResource(id = searchInstance.icon),
        contentDescription = null
      )

      Spacer(modifier = Modifier.width(12.dp))

      KurobaComposeText(
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight(),
        text = searchInstance.type.name
      )
    }
  }

  @Composable
  private fun BuildImageSearchResults(
    lastUsedSearchInstance: ImageSearchInstanceType,
    onImageClicked: (ImageSearchResult) -> Unit
  ) {
    val chanTheme = LocalChanTheme.current

    val searchInstance = controllerViewModel.searchInstances[lastUsedSearchInstance]
      ?: return
    val searchResults = controllerViewModel.searchResults[lastUsedSearchInstance]
      ?: return

    val imageSearchResults = when (val result = searchResults) {
      AsyncData.NotInitialized -> {
        return
      }
      AsyncData.Loading -> {
        KurobaComposeProgressIndicator()
        return
      }
      is AsyncData.Error -> {
        KurobaComposeErrorMessage(
          error = result.throwable
        )

        return
      }
      is AsyncData.Data -> result.data
    }

    val state = rememberLazyGridState(
      initialFirstVisibleItemIndex = searchInstance.rememberedFirstVisibleItemIndex,
      initialFirstVisibleItemScrollOffset = searchInstance.rememberedFirstVisibleItemScrollOffset
    )

    DisposableEffect(
      key1 = Unit,
      effect = {
        onDispose {
          controllerViewModel.updatePrevLazyListState(
            firstVisibleItemIndex = state.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset
          )
        }
      }
    )

    LazyVerticalGridWithFastScroller(
      modifier = Modifier
        .fillMaxSize(),
      state = state,
      columns = GridCells.Adaptive(minSize = 128.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      draggableScrollbar = true
    ) {
      val images = imageSearchResults.results

      items(
        count = images.size,
        contentType = { "image_item" }
      ) { index ->
        val imageSearchResult = images.get(index)

        BuildImageSearchResult(
          imageSearchResult = imageSearchResult,
          onImageClicked = onImageClicked
        )
      }

      if (!imageSearchResults.endReached) {
        item(
          span = { GridItemSpan(maxLineSpan) },
          contentType = { "loading_indicator" }
        ) {
          Box(
            modifier = Modifier.size(128.dp)
          ) {
            KurobaComposeProgressIndicator(
              modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .align(Alignment.Center)
            )
          }

          LaunchedEffect(key1 = images.lastIndex) {
            controllerViewModel.onNewPageRequested(page = searchInstance.currentPage + 1)
          }
        }
      } else {
        item(
          span = { GridItemSpan(maxLineSpan) },
          contentType = { "end_reached_indicator" }
        ) {
          KurobaComposeText(
            modifier = Modifier
              .wrapContentSize()
              .padding(horizontal = 32.dp, vertical = 16.dp),
            text = "End reached"
          )
        }
      }
    }
  }

  @Composable
  private fun BuildImageSearchResult(
    imageSearchResult: ImageSearchResult,
    onImageClicked: (ImageSearchResult) -> Unit
  ) {
    val chanTheme = LocalChanTheme.current

    val request = remember(imageSearchResult.thumbnailUrl) {
      ImageLoaderRequest(
        data = ImageLoaderRequestData.Url(
          httpUrl = imageSearchResult.thumbnailUrl,
          cacheFileType = CacheFileType.Other
        )
      )
    }

    val imageInfo = remember(key1 = imageSearchResult) {
      if (!imageSearchResult.hasImageInfo()) {
        return@remember null
      }

      return@remember buildString {
        if (imageSearchResult.extension.isNotNullNorEmpty()) {
          append(imageSearchResult.extension.uppercase())
        }

        if (imageSearchResult.width != null && imageSearchResult.height != null) {
          if (length > 0) {
            append(" ")
          }

          append(imageSearchResult.width)
          append("x")
          append(imageSearchResult.height)
        }

        if (imageSearchResult.sizeInByte != null) {
          if (length > 0) {
            append(" ")
          }

          append(ChanPostUtils.getReadableFileSize(imageSearchResult.sizeInByte))
        }
      }
    }

    val bgColor = remember { Color.Black.copy(alpha = 0.6f) }

    Box(
      modifier = Modifier
        .size(128.dp)
        .background(chanTheme.backColorSecondaryCompose)
        .clickable { onImageClicked(imageSearchResult) }
    ) {
      KurobaComposeImage(
        modifier = Modifier.fillMaxSize(),
        controllerKey = null,
        request = request,
        contentScale = ContentScale.Crop
      )

      if (imageInfo != null) {
        KurobaComposeText(
          modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .align(Alignment.BottomEnd),
          text = imageInfo,
          fontSize = 11.ktu,
          color = Color.White
        )
      }
    }
  }

  private fun showImageSearchInstances() {
    val menuItems = mutableListOf<FloatingListMenuItem>()

    menuItems += HeaderFloatingListMenuItem("header", "Select image search instance")

    ImageSearchInstanceType.entries.forEach { imageSearchInstanceType ->
      menuItems += FloatingListMenuItem(
        key = imageSearchInstanceType,
        name = imageSearchInstanceType.name,
        value = imageSearchInstanceType
      )
    }

    val floatingListMenuController = FloatingListMenuController(
      context = context,
      constraintLayoutBias = globalWindowInsetsManager.lastTouchCoordinatesAsConstraintLayoutBias(),
      items = menuItems,
      itemClickListener = { clickedItem ->
        val selectedImageSearchInstanceType = (clickedItem.value as? ImageSearchInstanceType)
          ?: return@FloatingListMenuController

        controllerViewModel.changeSearchInstance(selectedImageSearchInstanceType)
      }
    )

    presentController(floatingListMenuController)
  }

  private fun showOptions(fullImageUrls: List<HttpUrl>) {
    val menuItems = mutableListOf<FloatingListMenuItem>()

    menuItems += HeaderFloatingListMenuItem("header", "Select source url")

    fullImageUrls.forEach { httpUrl ->
      menuItems += FloatingListMenuItem(httpUrl, httpUrl.toString(), httpUrl)
    }

    val floatingListMenuController = FloatingListMenuController(
      context = context,
      constraintLayoutBias = globalWindowInsetsManager.lastTouchCoordinatesAsConstraintLayoutBias(),
      items = menuItems,
      itemClickListener = { clickedItem ->
        val clickedItemUrl = (clickedItem.value as? HttpUrl)
          ?: return@FloatingListMenuController

        onImageSelected(clickedItemUrl)
        requireNavController().popController()
      }
    )

    presentController(floatingListMenuController)
  }

  companion object {
    private const val TAG = "ImageSearchController"

    private const val ACTION_RELOAD = 0
  }

}