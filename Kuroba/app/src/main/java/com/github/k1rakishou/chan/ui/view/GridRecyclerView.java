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
package com.github.k1rakishou.chan.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.k1rakishou.chan.Chan;
import com.github.k1rakishou.chan.ui.theme.ThemeEngine;

import javax.inject.Inject;

/**
 * A RecyclerView with a GridLayoutManager that manages the span count by dividing the width of the
 * view with the value set by {@link #setSpanWidth(int)}.
 */
public class GridRecyclerView
        extends RecyclerView {

    @Inject
    ThemeEngine themeEngine;

    private GridLayoutManager gridLayoutManager;
    private int spanWidth;
    private int realSpanWidth;

    public GridRecyclerView(Context context) {
        super(context);
        init();
    }

    public GridRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            Chan.inject(this);
        }
    }

    public void setLayoutManager(GridLayoutManager gridLayoutManager) {
        this.gridLayoutManager = gridLayoutManager;
        super.setLayoutManager(gridLayoutManager);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setBackgroundColor(themeEngine.getChanTheme().getPrimaryColor());
    }

    /**
     * Set the width of each span in pixels.
     *
     * @param spanWidth width of each span in pixels.
     */
    public void setSpanWidth(int spanWidth) {
        this.spanWidth = spanWidth;
    }

    public int getRealSpanWidth() {
        return realSpanWidth;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        int spanCount = Math.max(1, getMeasuredWidth() / spanWidth);
        gridLayoutManager.setSpanCount(spanCount);
        int oldRealSpanWidth = realSpanWidth;
        realSpanWidth = getMeasuredWidth() / spanCount;
        if (realSpanWidth != oldRealSpanWidth) {
            getAdapter().notifyDataSetChanged();
        }
    }
}
