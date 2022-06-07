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

package com.patrickzedler.pallax.fragment.dialog;

import android.Manifest.permission;
import android.app.WallpaperManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.patrickzedler.pallax.Constants;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.databinding.FragmentBottomsheetOverwriteBinding;
import com.patrickzedler.pallax.drawable.WallpaperDrawable;
import com.patrickzedler.pallax.util.BitmapUtil;
import com.patrickzedler.pallax.util.SystemUiUtil;

public class OverwriteBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private static final String TAG = "OverwriteBottomSheet";

  private FragmentBottomsheetOverwriteBinding binding;
  private MainActivity activity;
  int previewWidth, previewHeight;
  float scaleRatio;
  float scale, offset, dimming;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
    binding = FragmentBottomsheetOverwriteBinding.inflate(inflater, container, false);
    activity = (MainActivity) requireActivity();

    binding.textOverwriteQuestion.setText(
        getString(
            R.string.msg_replace_or_keep,
            getString(activity.isWallpaperDarkMode() ? R.string.label_dark : R.string.label_light)
        )
    );

    int screenWidth = SystemUiUtil.getDisplayWidth(activity);
    int screenHeight = SystemUiUtil.getDisplayHeight(activity);
    if (screenWidth > screenHeight) {
      int width = screenWidth;
      //noinspection SuspiciousNameCombination
      screenWidth = screenHeight;
      //noinspection SuspiciousNameCombination
      screenHeight = width;
    }
    float screenRatio = (float) screenHeight / screenWidth;
    previewWidth = SystemUiUtil.dpToPx(activity, 60);
    previewHeight = (int) (previewWidth * screenRatio);
    scaleRatio = ((float) previewHeight) / ((float) screenHeight);

    boolean isDarkMode = activity.isWallpaperDarkMode();
    String suffix = isDarkMode ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;

    scale = getSharedPrefs().getFloat(
        PREF.SCALE + suffix,
        WallpaperDrawable.getDefaultScale(activity, activity.isWallpaperDarkMode())
    );
    scale *= scaleRatio;

    offset = getSharedPrefs().getInt(PREF.STATIC_OFFSET + suffix, DEF.STATIC_OFFSET);
    offset *= scaleRatio;

    dimming = getSharedPrefs().getInt(
        PREF.DIMMING + suffix, isDarkMode ? DEF.DIMMING_DARK : DEF.DIMMING_LIGHT) / 10f;

    loadCurrent();

    if (ContextCompat.checkSelfPermission(activity,
        permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    ) {
      loadNew();
    } else {
      binding.linearOverwriteConnection.setVisibility(View.GONE);
      binding.cardOverwriteNew.setVisibility(View.GONE);
    }

    binding.buttonOverwriteKeep.setOnClickListener(v -> {
      activity.setWallpaper(false);
      dismiss();
    });

    binding.buttonOverwriteReplace.setOnClickListener(v -> {
      performHapticClick();
      activity.setWallpaper(true);
      dismiss();
    });

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  private void loadCurrent() {
    binding.cardOverwriteCurrent.setLayoutParams(
        new LinearLayout.LayoutParams(previewWidth, previewHeight)
    );
    String suffix = activity.getDarkSuffix();
    String base64 = getSharedPrefs().getString(PREF.WALLPAPER + suffix, DEF.WALLPAPER);
    if (base64 != null) {
      BitmapDrawable drawable = BitmapUtil.getBitmapDrawable(activity, base64);
      WallpaperDrawable wallpaper = new WallpaperDrawable(activity, drawable);
      wallpaper.setScale(scale);
      wallpaper.setStaticOffsetX(offset);
      wallpaper.setDimming(dimming);
      binding.wallpaperOverwriteCurrent.setWallpaper(wallpaper);
    } else {
      binding.linearOverwriteContainerPreviews.setVisibility(View.GONE);
    }
  }

  private void loadNew() {
    if (ActivityCompat.checkSelfPermission(activity, permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_DENIED) {
      return;
    }
    WallpaperManager manager = WallpaperManager.getInstance(activity);
    Drawable drawable = manager.getDrawable();
    if (drawable != null) {
      binding.cardOverwriteNew.setLayoutParams(
          new LinearLayout.LayoutParams(previewWidth, previewHeight)
      );
      WallpaperDrawable wallpaper = new WallpaperDrawable(activity, drawable);
      wallpaper.setScale(scale);
      wallpaper.setStaticOffsetX(offset);
      wallpaper.setDimming(dimming);
      binding.wallpaperOverwriteNew.setWallpaper(wallpaper);
    } else {
      binding.linearOverwriteConnection.setVisibility(View.GONE);
      binding.cardOverwriteNew.setVisibility(View.GONE);
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearOverwriteContainer.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
