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
package com.github.k1rakishou.chan.ui.settings;

import android.view.View;

import com.github.k1rakishou.chan.ui.controller.settings.SettingsController;

public abstract class SettingView implements DisposableSetting {
    public SettingsController settingsController;
    public final String name;
    public View view;
    public View divider;
    public SettingNotificationType settingNotificationType = SettingNotificationType.Default;

    public SettingView(SettingsController settingsController, String name) {
        this.settingsController = settingsController;
        this.name = name;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void setEnabled(boolean enabled) {
    }

    public void setSettingNotificationType(SettingNotificationType type) {
        this.settingNotificationType = type;
    }

    public String getTopDescription() {
        return name;
    }

    public String getBottomDescription() {
        return null;
    }

    public SettingNotificationType getSettingNotificationType() {
        return settingNotificationType;
    }

    @Override
    public void dispose() {
        // no-op
    }
}