package com.github.k1rakishou.chan.features.media_viewer.media_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.core.mpv.MPVLib
import com.github.k1rakishou.chan.core.mpv.MPVView
import com.github.k1rakishou.chan.core.mpv.MpvUtils
import com.github.k1rakishou.chan.features.media_viewer.MediaLocation
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerControllerViewModel
import com.github.k1rakishou.chan.features.media_viewer.ViewableMedia
import com.github.k1rakishou.chan.features.media_viewer.helper.CloseMediaActionHelper
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableProgressBar
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getString
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.showToast
import com.github.k1rakishou.chan.utils.BackgroundUtils
import com.github.k1rakishou.chan.utils.setEnabledFast
import com.github.k1rakishou.chan.utils.setVisibilityFast
import com.github.k1rakishou.common.updateHeight
import com.github.k1rakishou.common.updatePaddings
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class MpvVideoMediaView(
  context: Context,
  initialMediaViewState: VideoMediaViewState,
  mediaViewContract: MediaViewContract,
  private val viewModel: MediaViewerControllerViewModel,
  private val onThumbnailFullyLoadedFunc: () -> Unit,
  private val isSystemUiHidden: () -> Boolean,
  override val viewableMedia: ViewableMedia.Video,
  override val pagerPosition: Int,
  override val totalPageItemsCount: Int,
) : MediaView<ViewableMedia.Video, MpvVideoMediaView.VideoMediaViewState>(
  context = context,
  attributeSet = null,
  mediaViewContract = mediaViewContract,
  mediaViewState = initialMediaViewState
), WindowInsetsListener, MPVLib.EventObserver {

  private val thumbnailMediaView: ThumbnailMediaView
  private val actualVideoPlayerView: MPVView
  private val bufferingProgressView: ColorizableProgressBar

  private val mpvVideoPosition: TextView
  private val mpvVideoProgress: AppCompatSeekBar
  private val mpvVideoDuration: TextView
  private val mpvMuteUnmute: ImageButton
  private val mpvHwSw: TextView
  private val mpvPlayPause: ImageButton
  private val mpvControlsRoot: LinearLayout
  private val mpvControlsBottomInset: FrameLayout

  private val closeMediaActionHelper: CloseMediaActionHelper
  private val gestureDetector: GestureDetector

  private var _firstLoadOccurred = false
  private var _hasContent = false
  private var _hasAudio = false
  private var userIsOperatingSeekbar = false

  private var showBufferingJob: Job? = null

  override val hasContent: Boolean
    get() = _hasContent

  init {
    AppModuleAndroidUtils.extractActivityComponent(context)
      .inject(this)

    inflate(context, R.layout.media_view_video_mpv, this)
    setWillNotDraw(false)

    thumbnailMediaView = findViewById(R.id.thumbnail_media_view)
    actualVideoPlayerView = findViewById(R.id.actual_video_view)
    bufferingProgressView = findViewById(R.id.buffering_progress_view)

    mpvVideoPosition = findViewById(R.id.mpv_position)
    mpvVideoProgress = findViewById(R.id.mpv_progress)
    mpvVideoDuration = findViewById(R.id.mpv_duration)
    mpvMuteUnmute = findViewById(R.id.mpv_mute_unmute)
    mpvHwSw = findViewById(R.id.mpv_hw_sw)
    mpvPlayPause = findViewById(R.id.mpv_play_pause)
    mpvControlsRoot = findViewById(R.id.mpv_controls_view_root)
    mpvControlsBottomInset = findViewById(R.id.mpv_controls_insets_view)

    mpvMuteUnmute.setOnClickListener {
      mediaViewContract.toggleSoundMuteState()

      if (mediaViewContract.isSoundCurrentlyMuted()) {
        actualVideoPlayerView.muteUnmute(true)
      } else {
        actualVideoPlayerView.muteUnmute(false)
      }
    }

    mpvHwSw.setOnClickListener {
      if (actualVideoPlayerView.hwdecActive) {
        showToast(context, "Switching to software decoding")
      } else {
        showToast(context, "Switching to hardware decoding")
      }

      actualVideoPlayerView.cycleHwdec()
    }
    mpvPlayPause.setOnClickListener { actualVideoPlayerView.cyclePause() }

    mpvVideoProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!fromUser) {
          return
        }

        actualVideoPlayerView.timePos = progress
        updatePlaybackPos(progress)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {
        userIsOperatingSeekbar = true
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        userIsOperatingSeekbar = false
      }
    })

    mpvMuteUnmute.setEnabledFast(false)
    mpvHwSw.setEnabledFast(false)
    mpvPlayPause.setEnabledFast(false)

    showBufferingJob = scope.launch {
      delay(125L)
      bufferingProgressView.setVisibilityFast(View.VISIBLE)
    }

    val movableContainer = actualVideoPlayerView.findViewById<View>(R.id.media_view_video_root)
      ?: actualVideoPlayerView

    closeMediaActionHelper = CloseMediaActionHelper(
      context = context,
      themeEngine = themeEngine,
      requestDisallowInterceptTouchEvent = { this.parent.requestDisallowInterceptTouchEvent(true) },
      onAlphaAnimationProgress = { alpha -> mediaViewContract.changeMediaViewerBackgroundAlpha(alpha) },
      movableContainerFunc = {
        if (actualVideoPlayerView.visibility == VISIBLE) {
          movableContainer
        } else {
          thumbnailMediaView
        }
      },
      invalidateFunc = { invalidate() },
      closeMediaViewer = { mediaViewContract.closeMediaViewer() },
      topPaddingFunc = { toolbarHeight() },
      bottomPaddingFunc = { 0 },
      topGestureInfo = createGestureAction(isTopGesture = true),
      bottomGestureInfo = createGestureAction(isTopGesture = false)
    )

    gestureDetector = GestureDetector(
      context,
      GestureDetectorListener(
        thumbnailMediaView = thumbnailMediaView,
        actualVideoView = actualVideoPlayerView,
        mediaViewContract = mediaViewContract,
        tryPreloadingFunc = { false },
        onMediaLongClick = { mediaViewContract.onMediaLongClick(this, viewableMedia) }
      )
    )

    thumbnailMediaView.setOnTouchListener { v, event ->
      if (thumbnailMediaView.visibility != View.VISIBLE) {
        return@setOnTouchListener false
      }

      // Always return true for thumbnails because otherwise gestures won't work with thumbnails
      gestureDetector.onTouchEvent(event)
      return@setOnTouchListener true
    }

    actualVideoPlayerView.setOnTouchListener { v, event ->
      if (actualVideoPlayerView.visibility != View.VISIBLE) {
        return@setOnTouchListener false
      }

      return@setOnTouchListener gestureDetector.onTouchEvent(event)
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    if (ev != null && closeMediaActionHelper.onInterceptTouchEvent(ev)) {
      return true
    }

    return super.onInterceptTouchEvent(ev)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (closeMediaActionHelper.onTouchEvent(event)) {
      return true
    }

    return super.onTouchEvent(event)
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    closeMediaActionHelper.onDraw(canvas)
  }

  override fun onInsetsChanged() {
    mpvControlsBottomInset.updateHeight(globalWindowInsetsManager.bottom())

    mpvControlsRoot.updatePaddings(
      left = globalWindowInsetsManager.left(),
      right = globalWindowInsetsManager.right()
    )
  }

  override fun preload() {
    thumbnailMediaView.bind(
      ThumbnailMediaView.ThumbnailMediaViewParameters(
        isOriginalMediaPlayable = true,
        viewableMedia = viewableMedia,
      ),
      onThumbnailFullyLoaded = {
        onThumbnailFullyLoadedFunc()
      }
    )

    onInsetsChanged()
  }

  override fun bind() {
    globalWindowInsetsManager.addInsetsUpdatesListener(this)
  }

  override fun show(isLifecycleChange: Boolean) {
    mediaViewToolbar?.updateWithViewableMedia(pagerPosition, totalPageItemsCount, viewableMedia)
    onSystemUiVisibilityChanged(isSystemUiHidden())

    actualVideoPlayerView.create(context.applicationContext)
    actualVideoPlayerView.addObserver(this)
    setFileToPlay(context)

    actualVideoPlayerView.setVisibilityFast(VISIBLE)
  }

  override fun hide(isLifecycleChange: Boolean) {
    actualVideoPlayerView.destroy()
    actualVideoPlayerView.removeObserver(this)

    thumbnailMediaView.setVisibilityFast(View.VISIBLE)
    actualVideoPlayerView.setVisibilityFast(GONE)

    // TODO(KurobaEx): mpv
//    mediaViewState.prevPosition = mainVideoPlayer.actualExoPlayer.currentPosition
//    mediaViewState.prevWindowIndex = mainVideoPlayer.actualExoPlayer.currentWindowIndex
//    mediaViewState.videoSoundDetected = videoSoundDetected
//
//    if (mediaViewState.prevPosition <= 0 && mediaViewState.prevWindowIndex <= 0) {
//      // Reset the flag because (most likely) the user swiped through the pages so fast that the
//      // player hasn't been able to start playing so it's still in some kind of BUFFERING state or
//      // something like that so mainVideoPlayer.isPlaying() will return false which will cause the
//      // player to appear paused if the user switches back to this page. We don't want that that's
//      // why we are resetting the "playing" to null here.
//      mediaViewState.playing = null
//    } else {
//      mediaViewState.playing = mainVideoPlayer.isPlaying()
//    }
//
//    mainVideoPlayer.pause()
  }

  override fun unbind() {
    thumbnailMediaView.unbind()
    closeMediaActionHelper.onDestroy()

    globalWindowInsetsManager.removeInsetsUpdatesListener(this)
    _hasContent = false
  }

  override fun eventProperty(property: String) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    BackgroundUtils.runOnMainThread { eventPropertyUi(property) }
  }

  override fun eventProperty(property: String, value: Boolean) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    BackgroundUtils.runOnMainThread { eventPropertyUi(property, value) }
  }

  override fun eventProperty(property: String, value: Long) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    BackgroundUtils.runOnMainThread { eventPropertyUi(property, value) }
  }

  override fun eventProperty(property: String, value: String) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    BackgroundUtils.runOnMainThread { eventPropertyUi(property, value) }
  }

  override fun event(eventId: Int) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    BackgroundUtils.runOnMainThread { eventUi(eventId) }
  }

  private fun eventUi(eventId: Int) {
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_IDLE -> {
        Logger.d(TAG, "onEvent MPV_EVENT_IDLE")

        // TODO(KurobaEx): mpv
//        actualVideoPlayerView.rewind()
      }
      MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {
        if (!_firstLoadOccurred) {
          _hasAudio = actualVideoPlayerView.audioCodec != null
        }

        Logger.d(TAG, "onEvent MPV_EVENT_PLAYBACK_RESTART " +
          "hwDecActive: '${actualVideoPlayerView.hwdecActive}', " +
          "videoCodec: '${actualVideoPlayerView.videoCodec}', " +
          "audioCodec: '${actualVideoPlayerView.audioCodec}', " +
          "aid: '${actualVideoPlayerView.aid}', " +
          "vid: '${actualVideoPlayerView.vid}'"
        )

        showBufferingJob?.cancel()
        showBufferingJob = null

        mpvHwSw.setEnabledFast(true)
        mpvPlayPause.setEnabledFast(true)

        if (_hasAudio) {
          mpvMuteUnmute.setEnabledFast(true)
        } else {
          mpvMuteUnmute.setEnabledFast(false)
        }

        if (actualVideoPlayerView.hwdecActive) {
          mpvHwSw.text = getString(R.string.mpv_hw_decoding)
        } else {
          mpvHwSw.text = getString(R.string.mpv_sw_decoding)
        }

        updateMuteUnmuteButtonState()

        bufferingProgressView.setVisibilityFast(View.INVISIBLE)
        thumbnailMediaView.setVisibilityFast(INVISIBLE)

        _hasContent = true
        _firstLoadOccurred = true
      }
      MPVLib.mpvEventId.MPV_EVENT_AUDIO_RECONFIG -> {
        Logger.d(TAG, "onEvent MPV_EVENT_AUDIO_RECONFIG")
        updateMuteUnmuteButtonState()
      }
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        Logger.d(TAG, "onEvent MPV_EVENT_FILE_LOADED")

        if (mediaViewContract.isSoundCurrentlyMuted()) {
          actualVideoPlayerView.muteUnmute(true)
        } else {
          actualVideoPlayerView.muteUnmute(false)
        }
      }
      MPVLib.mpvEventId.MPV_EVENT_NONE -> Logger.d(TAG, "onEvent MPV_EVENT_NONE")
      MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> Logger.d(TAG, "onEvent MPV_EVENT_SHUTDOWN")
      MPVLib.mpvEventId.MPV_EVENT_LOG_MESSAGE -> Logger.d(TAG, "onEvent MPV_EVENT_LOG_MESSAGE")
      MPVLib.mpvEventId.MPV_EVENT_GET_PROPERTY_REPLY -> Logger.d(TAG, "onEvent MPV_EVENT_GET_PROPERTY_REPLY")
      MPVLib.mpvEventId.MPV_EVENT_SET_PROPERTY_REPLY -> Logger.d(TAG, "onEvent MPV_EVENT_SET_PROPERTY_REPLY")
      MPVLib.mpvEventId.MPV_EVENT_COMMAND_REPLY -> Logger.d(TAG, "onEvent MPV_EVENT_COMMAND_REPLY")
      MPVLib.mpvEventId.MPV_EVENT_START_FILE -> Logger.d(TAG, "onEvent MPV_EVENT_START_FILE")
      MPVLib.mpvEventId.MPV_EVENT_END_FILE -> Logger.d(TAG, "onEvent MPV_EVENT_END_FILE")
      MPVLib.mpvEventId.MPV_EVENT_TICK -> Logger.d(TAG, "onEvent MPV_EVENT_TICK")
      MPVLib.mpvEventId.MPV_EVENT_CLIENT_MESSAGE -> Logger.d(TAG, "onEvent MPV_EVENT_CLIENT_MESSAGE")
      MPVLib.mpvEventId.MPV_EVENT_VIDEO_RECONFIG -> Logger.d(TAG, "onEvent MPV_EVENT_VIDEO_RECONFIG")
      MPVLib.mpvEventId.MPV_EVENT_SEEK -> Logger.d(TAG, "onEvent MPV_EVENT_SEEK")
      MPVLib.mpvEventId.MPV_EVENT_PROPERTY_CHANGE -> Logger.d(TAG, "onEvent MPV_EVENT_PROPERTY_CHANGE")
      MPVLib.mpvEventId.MPV_EVENT_QUEUE_OVERFLOW -> Logger.d(TAG, "onEvent MPV_EVENT_QUEUE_OVERFLOW")
      MPVLib.mpvEventId.MPV_EVENT_HOOK -> Logger.d(TAG, "onEvent MPV_EVENT_HOOK")
    }
  }

  private fun updateMuteUnmuteButtonState() {
    if (_firstLoadOccurred && !_hasAudio) {
      mpvMuteUnmute.setImageResource(R.drawable.ic_volume_off_white_24dp)
      return
    }

    if (actualVideoPlayerView.isMuted) {
      mpvMuteUnmute.setImageResource(R.drawable.ic_volume_off_white_24dp)
    } else {
      mpvMuteUnmute.setImageResource(R.drawable.ic_volume_up_white_24dp)
    }
  }


  private fun eventPropertyUi(property: String) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    when (property) {
      "video-params" -> {
//        updateOrientation()
      }
      "video-format" -> {
//        updateAudioUI()
      }
    }
  }

  private fun eventPropertyUi(property: String, value: Long) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    when (property) {
      "time-pos" -> {
        updatePlaybackPos(value.toInt())
      }
      "duration" -> {
        updatePlaybackDuration(value.toInt())
      }
    }
  }

  private fun eventPropertyUi(property: String, value: String) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    when (property) {
      "mute" -> updateMuteUnmuteButtonState()
    }

//    updateDisplayMetadata(property, value)
  }

  private fun eventPropertyUi(property: String, value: Boolean) {
    if (!viewModel.activityInForeground || !shown) {
      return
    }

    when (property) {
      "pause" -> updatePlaybackStatus(value)
    }
  }

  private fun setFileToPlay(context: Context) {
    val filePath = when (val mediaLocation = viewableMedia.mediaLocation) {
      is MediaLocation.Local -> {
        if (mediaLocation.isUri) {
          cancellableToast.showToast(
            context,
            "Cannot play local files by Uri"
          )

          return
        }

        mediaLocation.path
      }
      is MediaLocation.Remote -> mediaLocation.urlRaw
    }

    actualVideoPlayerView.playFile(filePath)
  }

  private fun updatePlaybackStatus(paused: Boolean) {
    val imageDrawable = if (paused) {
      R.drawable.exo_controls_play
    } else {
      R.drawable.exo_controls_pause
    }

    mpvPlayPause.setImageResource(imageDrawable)
  }

  private fun updatePlaybackPos(position: Int) {
    mpvVideoPosition.text = MpvUtils.prettyTime(position)

    if (!userIsOperatingSeekbar) {
      mpvVideoProgress.progress = position
    }

    updateDecoderButton()
  }

  private fun updatePlaybackDuration(duration: Int) {
    mpvVideoDuration.text = MpvUtils.prettyTime(duration)

    if (!userIsOperatingSeekbar) {
      mpvVideoProgress.max = duration
    }
  }

  private fun updateDecoderButton() {
    if (mpvHwSw.visibility != View.VISIBLE) {
      return
    }

    mpvHwSw.text = if (actualVideoPlayerView.hwdecActive) {
      getString(R.string.mpv_hw_decoding)
    } else {
      getString(R.string.mpv_sw_decoding)
    }
  }

  class GestureDetectorListener(
    private val thumbnailMediaView: ThumbnailMediaView,
    private val actualVideoView: MPVView,
    private val mediaViewContract: MediaViewContract,
    private val tryPreloadingFunc: () -> Boolean,
    private val onMediaLongClick: () -> Unit
  ) : GestureDetector.SimpleOnGestureListener() {

    override fun onDown(e: MotionEvent?): Boolean {
      return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
      if (actualVideoView.visibility == View.VISIBLE) {
        mediaViewContract.onTapped()
        return true
      } else if (thumbnailMediaView.visibility == View.VISIBLE) {
        if (tryPreloadingFunc()) {
          return true
        }

        mediaViewContract.onTapped()
        return true
      }

      return false
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
      if (e == null || actualVideoView.visibility != View.VISIBLE) {
        return false
      }

      // TODO(KurobaEx): mpv
//      val exoPlayer = actualVideoView.player
//        ?: return false
//
//      exoPlayer.playWhenReady = exoPlayer.playWhenReady.not()
      return true
    }

    override fun onLongPress(e: MotionEvent?) {
      onMediaLongClick()
    }
  }

  class VideoMediaViewState(
    var prevPosition: Long = -1,
    var prevWindowIndex: Int = -1,
    var videoSoundDetected: Boolean? = null,
    var playing: Boolean? = null
  ) : MediaViewState {

    fun resetPosition() {
      prevPosition = -1
      prevWindowIndex = -1
    }

    override fun clone(): MediaViewState {
      return VideoMediaViewState(prevPosition, prevWindowIndex, videoSoundDetected, playing)
    }

    override fun updateFrom(other: MediaViewState?) {
      if (other == null) {
        prevPosition = -1
        prevWindowIndex = -1
        videoSoundDetected = null
        playing = null
        return
      }

      if (other !is VideoMediaViewState) {
        return
      }

      this.prevPosition = other.prevPosition
      this.prevWindowIndex = other.prevWindowIndex
      this.videoSoundDetected = other.videoSoundDetected
      this.playing = other.playing
    }
  }

  companion object {
    private const val TAG = "MpvVideoMediaView"
  }

}