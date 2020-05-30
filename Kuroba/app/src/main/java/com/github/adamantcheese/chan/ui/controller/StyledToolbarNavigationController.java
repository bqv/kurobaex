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
package com.github.adamantcheese.chan.ui.controller;

import android.content.Context;

import com.github.adamantcheese.chan.R;
import com.github.adamantcheese.chan.controller.Controller;
import com.github.adamantcheese.chan.controller.ControllerTransition;
import com.github.adamantcheese.chan.controller.ui.NavigationControllerContainerLayout;
import com.github.adamantcheese.chan.core.settings.ChanSettings;
import com.github.adamantcheese.chan.features.drawer.DrawerController;
import com.github.adamantcheese.chan.ui.theme.ThemeHelper;

import javax.inject.Inject;

import static com.github.adamantcheese.chan.Chan.inject;
import static com.github.adamantcheese.chan.utils.AndroidUtils.inflate;

public class StyledToolbarNavigationController extends ToolbarNavigationController {

    @Inject
    ThemeHelper themeHelper;

    public StyledToolbarNavigationController(Context context) {
        super(context);
        inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        view = inflate(context, R.layout.controller_navigation_toolbar);
        container = (NavigationControllerContainerLayout) view.findViewById(R.id.container);

        NavigationControllerContainerLayout nav = (NavigationControllerContainerLayout) container;
        nav.setNavigationController(this);
        nav.setSwipeEnabled(ChanSettings.controllerSwipeable.get());

        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(themeHelper.getTheme().primaryColor.color);
        toolbar.setCallback(this);
    }

    @Override
    public boolean popController(ControllerTransition controllerTransition) {
        return !toolbar.isTransitioning() && super.popController(controllerTransition);
    }

    @Override
    public boolean pushController(Controller to, ControllerTransition controllerTransition) {
        return !toolbar.isTransitioning() && super.pushController(to, controllerTransition);
    }

    @Override
    public void transition(Controller from, Controller to, boolean pushing, ControllerTransition controllerTransition) {
        super.transition(from, to, pushing, controllerTransition);

        if (to != null) {
            DrawerController drawerController = getDrawerController();
            if (drawerController != null) {
                drawerController.setDrawerEnabled(to.navigation.hasDrawer);
            }
        }
    }

    @Override
    public void endSwipeTransition(Controller from, Controller to, boolean finish) {
        super.endSwipeTransition(from, to, finish);

        if (finish) {
            DrawerController drawerController = getDrawerController();
            if (drawerController != null) {
                drawerController.setDrawerEnabled(to.navigation.hasDrawer);
            }
        }
    }

    @Override
    public boolean onBack() {
        if (super.onBack()) {
            return true;
        } else if (parentController instanceof PopupController && childControllers.size() == 1) {
            ((PopupController) parentController).dismiss();
            return true;
        } else if (doubleNavigationController != null && childControllers.size() == 1) {
            if (doubleNavigationController.getRightController() == this) {
                doubleNavigationController.setRightController(null);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onMenuClicked() {
        DrawerController drawerController = getDrawerController();
        if (drawerController != null) {
            drawerController.onMenuClicked();
        }
    }

    private DrawerController getDrawerController() {
        if (parentController instanceof DrawerController) {
            return (DrawerController) parentController;
        } else if (doubleNavigationController != null) {
            Controller doubleNav = (Controller) doubleNavigationController;
            if (doubleNav.parentController instanceof DrawerController) {
                return (DrawerController) doubleNav.parentController;
            }
        }
        return null;
    }
}
