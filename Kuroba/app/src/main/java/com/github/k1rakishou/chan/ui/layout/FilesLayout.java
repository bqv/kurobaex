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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.k1rakishou.chan.Chan;
import com.github.k1rakishou.chan.R;
import com.github.k1rakishou.chan.core.saver.FileWatcher;
import com.github.k1rakishou.chan.ui.adapter.FilesAdapter;
import com.github.k1rakishou.chan.ui.theme.ThemeEngine;
import com.github.k1rakishou.chan.utils.RecyclerUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class FilesLayout
        extends LinearLayout
        implements FilesAdapter.Callback, View.OnClickListener {

    @Inject
    ThemeEngine themeEngine;

    private ViewGroup backLayout;
    private ImageView backImage;
    private TextView backText;
    private RecyclerView recyclerView;

    private LinearLayoutManager layoutManager;
    private FilesAdapter filesAdapter;

    private Map<String, FileItemHistory> history = new HashMap<>();
    private FileItemHistory currentHistory;
    private FileWatcher.FileItems currentFileItems;

    private Callback callback;

    public FilesLayout(Context context) {
        this(context, null);
        init();
    }

    public FilesLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public FilesLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            Chan.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        backLayout = findViewById(R.id.back_layout);
        backImage = backLayout.findViewById(R.id.back_image);
        backImage.setImageDrawable(DrawableCompat.wrap(backImage.getDrawable()));
        backText = backLayout.findViewById(R.id.back_text);
        recyclerView = findViewById(R.id.recycler);

        backLayout.setOnClickListener(this);
    }

    public void initialize() {
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        filesAdapter = new FilesAdapter(this);
        recyclerView.setAdapter(filesAdapter);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setFiles(FileWatcher.FileItems fileItems) {
        // Save the associated list position
        if (currentFileItems != null) {
            RecyclerUtils.IndexAndTop indexTop = RecyclerUtils.getIndexAndTop(recyclerView);
            currentHistory.index = indexTop.getIndex();
            currentHistory.top = indexTop.getTop();
            history.put(currentFileItems.path.getAbsolutePath(), currentHistory);
        }

        filesAdapter.setFiles(fileItems);
        currentFileItems = fileItems;

        // Restore any previous list position
        currentHistory = history.get(fileItems.path.getAbsolutePath());
        if (currentHistory != null) {
            layoutManager.scrollToPositionWithOffset(currentHistory.index, currentHistory.top);
            filesAdapter.setHighlightedItem(currentHistory.clickedItem);
        } else {
            currentHistory = new FileItemHistory();
            filesAdapter.setHighlightedItem(null);
        }

        boolean enabled = fileItems.canNavigateUp;
        backLayout.setEnabled(enabled);
        Drawable wrapped = DrawableCompat.wrap(backImage.getDrawable());
        backImage.setImageDrawable(wrapped);

        int color = enabled
                ? themeEngine.getChanTheme().getTextPrimaryColor()
                : themeEngine.getChanTheme().getTextColorHint();

        DrawableCompat.setTint(wrapped, color);
        backText.setEnabled(enabled);
        backText.setTextColor(color);
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public ViewGroup getBackLayout() {
        return backLayout;
    }

    @Override
    public void onFileItemClicked(FileWatcher.FileItem fileItem) {
        currentHistory.clickedItem = fileItem;
        callback.onFileItemClicked(fileItem);
    }

    @Override
    public void onClick(View view) {
        if (view == backLayout) {
            currentHistory.clickedItem = null;
            callback.onBackClicked();
        }
    }

    private class FileItemHistory {
        int index, top;
        FileWatcher.FileItem clickedItem;
    }

    public interface Callback {
        void onBackClicked();

        void onFileItemClicked(FileWatcher.FileItem fileItem);
    }
}
