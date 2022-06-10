/*
 * This file is part of Pallax Android.
 *
 * Pallax Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Pallax Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pallax Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 by Patrick Zedler
 */

package com.patrickzedler.pallax;

import android.graphics.Color;

public final class Constants {

  private static final String TAG = Constants.class.getSimpleName();

  public static final String SUFFIX_LIGHT = "_light";
  public static final String SUFFIX_DARK = "_dark";

  public static String getDarkSuffix(boolean isDark) {
    return isDark ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;
  }

  public static final class PREF {

    // Appearance

    public static final String WALLPAPER = "wallpaper";
    public static final String WALLPAPER_DARK_MODE = "wallpaper_dark_mode";
    public static final String WALLPAPER_FOLLOW_SYSTEM = "wallpaper_follow_system";

    public static final String SCALE = "scale";
    public static final String STATIC_OFFSET = "static_offset";
    public static final String WALLPAPER_WIDTH = "wallpaper_width";
    public static final String WALLPAPER_HEIGHT = "wallpaper_height";

    public static final String COLOR_PRIMARY = "color_primary";
    public static final String COLOR_SECONDARY = "color_secondary";
    public static final String COLOR_TERTIARY = "color_tertiary";

    public static final String DIMMING = "dimming";
    public static final String USE_DARK_TEXT = "dark_text";
    public static final String FORCE_LIGHT_TEXT = "light_text";
    public static final String USE_DARK_LAUNCHER = "dark_launcher";

    // Parallax

    public static final String PARALLAX = "parallax";
    public static final String POWER_SAVE_SWIPE = "power_save_swipe";
    public static final String TILT = "tilt";
    public static final String REFRESH_RATE = "refresh_rate";
    public static final String DAMPING_TILT = "damping_tilt";
    public static final String THRESHOLD = "threshold";
    public static final String POWER_SAVE_TILT = "power_save_tilt";

    // Zoom

    public static final String ZOOM_INTENSITY = "zoom_intensity";
    public static final String POWER_SAVE_ZOOM = "power_save_zoom";
    public static final String ZOOM_LAUNCHER = "zoom_launcher";
    public static final String USE_ZOOM_DAMPING = "use_zoom_damping";
    public static final String DAMPING_ZOOM = "damping_zoom";
    public static final String ZOOM_SYSTEM = "zoom_system";
    public static final String ZOOM_UNLOCK = "zoom_unlock";
    public static final String ZOOM_DURATION = "zoom_duration";

    // Other

    public static final String LANGUAGE = "language";
    public static final String THEME = "app_theme";
    public static final String MODE = "mode";

    public static final String LAST_VERSION = "last_version";
    public static final String FEEDBACK_POP_UP_COUNT = "feedback_pop_up_count";
  }

  public static final class DEF {

    public static final String WALLPAPER = null;
    public static final boolean WALLPAPER_DARK_MODE = false;
    public static final boolean WALLPAPER_FOLLOW_SYSTEM = true;

    public static final float SCALE = 1;
    public static final int STATIC_OFFSET = 0;
    public static final int COLOR = Color.BLACK;
    public static final int DIMMING_LIGHT = 0;
    public static final int DIMMING_DARK = -2;
    public static final boolean USE_DARK_TEXT = false;
    public static final boolean FORCE_LIGHT_TEXT = false;
    public static final boolean USE_DARK_LAUNCHER = false;

    public static final int PARALLAX = 2;
    public static final boolean POWER_SAVE_SWIPE = false;
    public static final boolean TILT = false;
    public static final int REFRESH_RATE = 30000;
    public static final int DAMPING_TILT = 8;
    public static final int THRESHOLD = 5;
    public static final boolean POWER_SAVE_TILT = true;

    public static final int ZOOM_INTENSITY = 2;
    public static final boolean POWER_SAVE_ZOOM = false;
    public static final boolean ZOOM_LAUNCHER = true;
    public static final boolean USE_ZOOM_DAMPING = false;
    public static final int DAMPING_ZOOM = 12;
    public static final boolean ZOOM_SYSTEM = false;
    public static final boolean ZOOM_UNLOCK = true;
    public static final int ZOOM_DURATION = 1200;

    public static final String LANGUAGE = null;
    public static final String THEME = "";
    public static final int MODE = Constants.MODE.AUTO;
  }

  public static final class MODE {

    public static final int AUTO = -1;
    public static final int LIGHT = 1;
    public static final int DARK = 0;
  }

  public static final class USER_PRESENCE {

    public static final String LOCKED = "locked";
    public static final String OFF = "off";
    public static final String UNLOCKED = "unlocked";
  }

  public static final class REQUEST_SOURCE {

    public static final String ZOOM_LAUNCHER = "zoom_launcher";
    public static final String ZOOM_UNLOCK = "zoom_unlock";
    public static final String TILT = "tilt";
  }

  public static final class ACTION {

    public static final String THEME_CHANGED = "action_theme_changed";
    public static final String SETTINGS_CHANGED = "action_settings_changed";
  }

  public static final class EXTRA {

    public static final String RUN_AS_SUPER_CLASS = "run_as_super_class";
    public static final String INSTANCE_STATE = "instance_state";
    public static final String SHOW_FORCE_STOP_REQUEST = "show_force_stop_request";
    public static final String SCROLL_POSITION = "scroll_position";
  }

  public static final class THEME {

    public static final String DYNAMIC = "dynamic";
    public static final String RED = "red";
    public static final String YELLOW = "yellow";
    public static final String LIME = "lime";
    public static final String GREEN = "green";
    public static final String TURQUOISE = "turquoise";
    public static final String TEAL = "teal";
    public static final String BLUE = "blue";
    public static final String PURPLE = "purple";
    public static final String AMOLED = "amoled";
  }
}
