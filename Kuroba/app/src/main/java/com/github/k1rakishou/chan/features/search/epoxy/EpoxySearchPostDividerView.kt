package com.github.k1rakishou.chan.features.search.epoxy

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.github.k1rakishou.chan.Chan
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.ui.theme.ThemeEngine
import com.github.k1rakishou.common.updateMargins
import javax.inject.Inject

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class EpoxySearchPostDividerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  @Inject
  lateinit var themeEngine: ThemeEngine

  private val divider: View

  init {
    Chan.inject(this)
    View.inflate(context, R.layout.epoxy_divider_view, this)

    divider = findViewById<View>(R.id.divider)
    divider.setBackgroundColor(themeEngine.chanTheme.dividerColor)
  }

  @JvmOverloads
  @ModelProp
  fun updateMargins(margins: NewMargins? = null) {
    if (margins == null) {
      divider.updateMargins(
        top = 0,
        bottom = 0,
        left = 0,
        right = 0
      )

      return
    }

    divider.updateMargins(
      top = margins.top,
      bottom = margins.bottom,
      left = margins.left,
      right = margins.right
    )
  }

  data class NewMargins(
    val left: Int? = null,
    val right: Int? = null,
    val top: Int? = null,
    val bottom: Int? = null
  )

}