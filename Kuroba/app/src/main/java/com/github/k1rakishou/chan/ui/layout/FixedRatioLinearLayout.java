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
package com.github.k1rakishou.chan.ui.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class FixedRatioLinearLayout
        extends LinearLayout {
    private float ratio;
    private boolean enabled = false;

    public FixedRatioLinearLayout(Context context) {
        super(context);
    }

    public FixedRatioLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedRatioLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!enabled) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                && (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST)) {
            int width = MeasureSpec.getSize(widthMeasureSpec);

            super.onMeasure(
                    widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec((int) (width / ratio), MeasureSpec.EXACTLY)
            );
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
