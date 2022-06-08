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

package com.patrickzedler.pallax.drawable;

import android.app.WallpaperColors;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import com.patrickzedler.pallax.Constants;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.util.PrefsUtil;
import com.patrickzedler.pallax.util.SystemUiUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class WallpaperDrawable {

  private static final String TAG = WallpaperDrawable.class.getSimpleName();

  private final Drawable drawable;
  private WallpaperColors colors;
  private float offsetX;
  private float offsetStaticX;
  private float offsetY;
  private float scale;
  private float zoom;
  private int dimmingColor;
  private final float pixelUnit;
  private final Rect rect;
  private Point point;

  public WallpaperDrawable(Context context, @NonNull Drawable drawable) {
    this.drawable = drawable;

    pixelUnit = getPixelUnit(context);
    scale = 1;

    dimmingColor = Color.BLACK;

    rect = new Rect();
    point = new Point();
  }

  public void setWallpaperColors(WallpaperColors colors) {
    this.colors = colors;
  }

  @RequiresApi(api = VERSION_CODES.O_MR1)
  public WallpaperColors getWallpaperColors(
      boolean darkText, boolean lightText, boolean darkLauncher) {
    if (colors == null) {
      return WallpaperColors.fromBitmap(((BitmapDrawable) drawable).getBitmap());
    } else if (VERSION.SDK_INT >= 31) {
      int hints = 0;
      if (darkText) {
        hints |= WallpaperColors.HINT_SUPPORTS_DARK_TEXT;
      } else if (darkLauncher) {
        hints |= WallpaperColors.HINT_SUPPORTS_DARK_THEME;
      }
      return new WallpaperColors(
          colors.getPrimaryColor(),
          colors.getSecondaryColor(),
          colors.getTertiaryColor(),
          hints
      );
    } else {
      int primary = colors.getPrimaryColor().toArgb();
      if (lightText) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(primary, hsl);
        hsl[2] = 0.6f;
        primary = ColorUtils.HSLToColor(hsl);
      } else if (darkLauncher) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(primary, hsl);
        hsl[2] = 0.1f;
        primary = ColorUtils.HSLToColor(hsl);
      }

      // Fix required for older versions, color constructor only calculates dark theme support
      // We need a way to set dark text support, the bitmap method calls the calculation method
      // Bitmap is more efficient than Drawable here because Drawable would be converted to Bitmap
      Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
      new Canvas(bitmap).drawColor(primary);
      return WallpaperColors.fromBitmap(bitmap);
    }
  }

  private static float getPixelUnit(Context context) {
    return SystemUiUtil.dpToPx(context, 1) * 0.33f;
  }

  /**
   * The final offset is calculated with the elevation
   */
  public void setOffset(float offsetX, float offsetY) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
  }

  /**
   * Higher is to the right side
   */
  public void setStaticOffsetX(float offsetX) {
    offsetStaticX = offsetX;
  }

  public void setScale(float scale) {
    this.scale = scale;
  }

  /**
   * Set how much should be zoomed out. The final value is calculated with the elevation of each
   * object. An object with elevation of 1 (nearest) is zoomed out much more than an object with the
   * elevation 0.1 (almost no parallax/zoom effect).
   *
   * @param zoom value from 0-1: 0 = original size; 1 = max zoomed out (depending on the elevation)
   */
  public void setZoom(float zoom) {
    this.zoom = zoom;
  }

  /**
   * dimming -1 to 1 (-1 = black, 0 = none, 1 = white)
   */
  public void setDimming(float dimming) {
    if (drawable != null) {
      drawable.setAlpha((int) ((1 - Math.abs(dimming)) * 255));
    }
    dimmingColor = dimming > 0 ? Color.WHITE : Color.BLACK;
  }

  public static float getDefaultScale(Context context, boolean isDarkMode) {
    try {
      String suffix = isDarkMode ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;
      SharedPreferences sharedPrefs = new PrefsUtil(context).getSharedPrefs();
      float wallpaperWidth = sharedPrefs.getInt(PREF.WALLPAPER_WIDTH + suffix, 0);
      float wallpaperHeight = sharedPrefs.getInt(PREF.WALLPAPER_HEIGHT + suffix, 0);
      int screenWidth = SystemUiUtil.getDisplayWidth(context);
      int screenHeight = SystemUiUtil.getDisplayHeight(context);

      if (wallpaperWidth == 0 || wallpaperHeight == 0) {
        float displayWidth = Math.min(screenWidth, screenHeight);
        float circleWidth = 300 * getPixelUnit(context);
        float currentRatio = circleWidth / displayWidth;
        float originalRatio = 0.2777f;
        float scale = (1 - (currentRatio / originalRatio)) + 1.2f;
        return BigDecimal.valueOf(scale).setScale(1, RoundingMode.HALF_UP).floatValue();
      } else {
        float screenRatio = (float) screenWidth / screenHeight;
        float wallpaperRatio = wallpaperWidth / wallpaperHeight;
        int zoom = sharedPrefs.getInt(PREF.ZOOM_INTENSITY, DEF.ZOOM_INTENSITY);

        float outputHeight;

        if (screenRatio > wallpaperRatio) {
          //fit in width (width scale is 1.0)
          outputHeight = screenWidth * (wallpaperHeight / wallpaperWidth);
        } else if (screenRatio < wallpaperRatio) {
          //fit in height (height scale is 1.0)
          outputHeight = screenHeight;
        } else {
          outputHeight = screenHeight;
        }

        float intensity = zoom / 10f;
        float scale = outputHeight / wallpaperHeight + intensity + 0.2f;

        return BigDecimal.valueOf(scale).setScale(1, RoundingMode.HALF_UP).floatValue();
      }
    } catch (Exception e) {
      return DEF.SCALE;
    }
  }

  public void draw(Canvas canvas) {
    if (drawable == null) {
      return;
    }
    canvas.drawColor(dimmingColor);

    float scale = this.scale - zoom;
    point = getFinalCenter(canvas);
    rect.set(
        (int) (point.x - (drawable.getIntrinsicWidth() * scale) / 2),
        (int) (point.y - (drawable.getIntrinsicHeight() * scale) / 2),
        (int) (point.x + (drawable.getIntrinsicWidth() * scale) / 2),
        (int) (point.y + (drawable.getIntrinsicHeight() * scale) / 2)
    );

    drawable.setBounds(rect);
    drawable.draw(canvas);
  }

  private Point getFinalCenter(Canvas canvas) {
    float cx = 0.5f * canvas.getWidth() - offsetX + offsetStaticX * pixelUnit * 30;
    float cy = 0.5f * canvas.getHeight() - offsetY;

    float centerX = canvas.getWidth() / 2f;
    if (cx < centerX) {
      float dist = centerX - cx;
      cx += dist * zoom;
    } else {
      float dist = cx - centerX;
      cx -= dist * zoom;
    }

    float centerY = canvas.getHeight() / 2f;
    if (cy < centerY) {
      float dist = centerY - cy;
      cy += dist * zoom;
    } else {
      float dist = cy - centerY;
      cy -= dist * zoom;
    }
    return new Point((int) cx, (int) cy);
  }
}
