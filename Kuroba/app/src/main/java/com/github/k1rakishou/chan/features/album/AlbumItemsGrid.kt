package com.github.k1rakishou.chan.features.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.compose.lazylist.LazyVerticalGridWithFastScroller
import com.github.k1rakishou.chan.ui.compose.providers.LocalContentPaddings
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.ui.helper.awaitWhile
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter

@Composable
fun AlbumItemsGrid(
  controllerKey: ControllerKey,
  controllerViewModel: AlbumViewControllerV2ViewModel,
  albumSpanCount: Int,
  onClick: (AlbumViewControllerV2ViewModel.AlbumItemData) -> Unit,
  onLongClick: (AlbumViewControllerV2ViewModel.AlbumItemData) -> Unit,
  clearDownloadingAlbumItemState: (AlbumViewControllerV2ViewModel.DownloadingAlbumItem) -> Unit
) {
  val contentPaddings = LocalContentPaddings.current
  val albumSelection by controllerViewModel.albumSelection.collectAsState()
  val albumItems = controllerViewModel.albumItems
  val downloadingAlbumItems = controllerViewModel.downloadingAlbumItems

  val state = rememberLazyGridState(
    initialFirstVisibleItemIndex = controllerViewModel.lastScrollPosition.intValue
  )

  LaunchedEffect(key1 = state) {
    controllerViewModel.scrollToPosition
      .filter { scrollToPosition -> scrollToPosition >= 0 }
      .collectLatest { scrollToPosition ->
        try {
          val success = awaitWhile(maxWaitTimeMs = 1_000L) { state.layoutInfo.totalItemsCount >= scrollToPosition }
          if (success) {
            awaitFrame()
            state.scrollToItem(scrollToPosition)
          }
        } catch (_: Throwable) {
          // no-op
        }
      }
  }

  LaunchedEffect(key1 = state) {
    // Give some time for scroll position restoration routine to do it's job
    delay(2000)

    snapshotFlow { state.firstVisibleItemIndex }
      .debounce(100)
      .filter { state.layoutInfo.totalItemsCount > 0 }
      .collectLatest { firstVisibleItemIndex -> controllerViewModel.updateLastScrollPosition(firstVisibleItemIndex) }
  }

  LazyVerticalGridWithFastScroller(
    modifier = Modifier.fillMaxSize(),
    columns = GridCells.Fixed(albumSpanCount),
    state = state,
    verticalArrangement = Arrangement.spacedBy(2.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    contentPadding = remember(contentPaddings, controllerKey) { contentPaddings.asPaddingValues(controllerKey) }
  ) {
    items(
      count = albumItems.size,
      key = { index -> albumItems.get(index).composeKey },
      contentType = { "album_item" },
      itemContent = { index ->
        val albumItemData = albumItems[index]

        AlbumItem(
          modifier = Modifier
            .fillMaxSize()
            .aspectRatio(3f / 4f),
          isInSelectionMode = albumSelection.isInSelectionMode,
          isSelected = albumItemData.id in albumSelection.selectedItems,
          albumItemData = albumItemData,
          downloadingAlbumItem = downloadingAlbumItems[albumItemData.id],
          onClick = onClick,
          onLongClick = onLongClick,
          clearDownloadingAlbumItemState = clearDownloadingAlbumItemState
        )
      }
    )
  }
}