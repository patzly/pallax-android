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

package com.patrickzedler.pallax.fragment;

import android.graphics.drawable.BitmapDrawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.patrickzedler.pallax.Constants;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.behavior.ScrollBehavior;
import com.patrickzedler.pallax.behavior.SystemBarBehavior;
import com.patrickzedler.pallax.databinding.FragmentAppearanceBinding;
import com.patrickzedler.pallax.drawable.WallpaperDrawable;
import com.patrickzedler.pallax.util.BitmapUtil;
import com.patrickzedler.pallax.util.ResUtil;
import com.patrickzedler.pallax.util.SystemUiUtil;
import com.patrickzedler.pallax.util.ViewUtil;

public class AppearanceFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnChangeListener {

  private static final String TAG = AppearanceFragment.class.getSimpleName();

  private FragmentAppearanceBinding binding;
  private MainActivity activity;
  private boolean isDarkMode;
  private boolean areListenersActive;
  private String suffix;
  private WallpaperDrawable wallpaperDrawableLight, wallpaperDrawableDark;
  private float scaleRatio;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentAppearanceBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarAppearance);
    systemBarBehavior.setScroll(binding.scrollAppearance, binding.constraintAppearance);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarAppearance, binding.scrollAppearance, true
    );

    ViewUtil.centerToolbarTitleOnLargeScreens(binding.toolbarAppearance);
    binding.toolbarAppearance.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarAppearance.setOnMenuItemClickListener(getOnMenuItemClickListener());

    areListenersActive = true;

    // DARK MODE

    updateWallpaperSelection(activity.isWallpaperDarkMode(), false);

    binding.switchAppearanceFollowSystem.setChecked(
        getSharedPrefs().getBoolean(PREF.WALLPAPER_FOLLOW_SYSTEM, DEF.WALLPAPER_FOLLOW_SYSTEM)
    );

    // SCALE

    binding.sliderAppearanceScale.addOnChangeListener(this);
    binding.sliderAppearanceScale.setLabelFormatter(value -> {
      float scale = value / 10f;
      return String.format(
          activity.getLocale(), scale == 1 || scale == 2 ? "×%.0f" : "×%.1f", scale
      );
    });

    // STATIC OFFSET

    binding.sliderAppearanceStaticOffset.addOnChangeListener(this);
    binding.sliderAppearanceStaticOffset.setLabelFormatter(value -> {
      if (value == 0) {
        return getString(R.string.parallax_none);
      } else {
        return String.valueOf((int) value);
      }
    });

    // DIMMING

    binding.sliderAppearanceDimming.addOnChangeListener(this);
    binding.sliderAppearanceDimming.setLabelFormatter(value -> {
      if (value == 0) {
        return getString(R.string.parallax_none);
      } else {
        return getString(R.string.label_percent, String.valueOf((int) value * 10));
      }
    });

    // TEXT AND LAUNCHER COLOR

    if (VERSION.SDK_INT >= 31) {
      binding.linearAppearanceDarkText.setVisibility(View.VISIBLE);
      binding.linearAppearanceLightText.setVisibility(View.GONE);
    } else {
      binding.linearAppearanceDarkText.setVisibility(View.GONE);
      binding.linearAppearanceLightText.setVisibility(View.VISIBLE);
    }

    // Wallpaper previews
    loadPreview(false);
    updatePreview(false);
    loadPreview(true);
    updatePreview(true);

    ViewUtil.setOnClickListeners(
        this,
        binding.cardAppearanceWallpaperLight,
        binding.cardAppearanceWallpaperDark,
        binding.linearAppearanceFollowSystem,
        binding.frameAppearanceScaleReset,
        binding.linearAppearanceDarkText,
        binding.linearAppearanceLightText
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchAppearanceFollowSystem,
        binding.switchAppearanceDarkText,
        binding.switchAppearanceLightText
    );
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.card_appearance_wallpaper_light) {
      if (binding.cardAppearanceWallpaperLight.isChecked()) {
        return;
      }
      performHapticClick();
      getSharedPrefs().edit().putBoolean(PREF.WALLPAPER_DARK_MODE, false).apply();
      updateWallpaperSelection(false, true);
      if (!binding.switchAppearanceFollowSystem.isChecked()) {
        activity.requestThemeRefresh();
      }
    } else if (id == R.id.card_appearance_wallpaper_dark) {
      if (binding.cardAppearanceWallpaperDark.isChecked()) {
        return;
      }
      performHapticClick();
      getSharedPrefs().edit().putBoolean(PREF.WALLPAPER_DARK_MODE, true).apply();
      updateWallpaperSelection(true, true);
      if (!binding.switchAppearanceFollowSystem.isChecked()) {
        activity.requestThemeRefresh();
      }
    } else if (id == R.id.linear_appearance_follow_system) {
      binding.switchAppearanceFollowSystem.setChecked(
          !binding.switchAppearanceFollowSystem.isChecked()
      );
    } else if (id == R.id.frame_appearance_scale_reset) {
      float def = WallpaperDrawable.getDefaultScale(activity, isDarkMode);
      float scaleOld = binding.sliderAppearanceScale.getValue();
      float scaleNew = def * 10;
      binding.sliderAppearanceScale.setValue(scaleNew);
      getSharedPrefs().edit().putFloat(PREF.SCALE + suffix, def).apply();
      updatePreview(isDarkMode);
      if (scaleNew != scaleOld) {
        activity.requestSettingsRefresh();
      }
      performHapticClick();
      ViewUtil.startIcon(binding.imageAppearanceScaleReset);
    } else if (id == R.id.linear_appearance_dark_text) {
      binding.switchAppearanceDarkText.setChecked(
          !binding.switchAppearanceDarkText.isChecked()
      );
    } else if (id == R.id.linear_appearance_light_text) {
      binding.switchAppearanceLightText.setChecked(
          !binding.switchAppearanceLightText.isChecked()
      );
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (!areListenersActive) {
      return;
    }
    int id = buttonView.getId();
    if (id == R.id.switch_appearance_follow_system) {
      getSharedPrefs().edit().putBoolean(PREF.WALLPAPER_FOLLOW_SYSTEM, isChecked).apply();
      activity.requestThemeRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageAppearanceFollowSystem);
    } else if (id == R.id.switch_appearance_dark_text) {
      getSharedPrefs().edit().putBoolean(PREF.USE_DARK_TEXT + suffix, isChecked).apply();
      activity.requestThemeRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageAppearanceDarkText);
    } else if (id == R.id.switch_appearance_light_text) {
      getSharedPrefs().edit().putBoolean(PREF.FORCE_LIGHT_TEXT + suffix, isChecked).apply();
      activity.requestThemeRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageAppearanceLightText);
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser || !areListenersActive) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_appearance_scale) {
      getSharedPrefs().edit().putFloat(PREF.SCALE + suffix, value / 10).apply();
      updatePreview(isDarkMode);
      ViewUtil.startIcon(binding.imageSizeScale);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_appearance_static_offset) {
      getSharedPrefs().edit().putInt(PREF.STATIC_OFFSET + suffix, (int) value).apply();
      updatePreview(isDarkMode);
      ViewUtil.startIcon(binding.imageAppearanceStaticOffset);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_appearance_dimming) {
      getSharedPrefs().edit().putInt(PREF.DIMMING + suffix, (int) value).apply();
      updatePreview(isDarkMode);
      ViewUtil.startIcon(binding.imageAppearanceDimming);
      activity.requestSettingsRefresh();
      performHapticClick();
    }
  }

  private void updateWallpaperSelection(boolean isDarkMode, boolean animated) {
    this.isDarkMode = isDarkMode;
    MaterialCardView cardActive, cardInactive;
    if (isDarkMode) {
      cardActive = binding.cardAppearanceWallpaperDark;
      cardInactive = binding.cardAppearanceWallpaperLight;
    } else {
      cardActive = binding.cardAppearanceWallpaperLight;
      cardInactive = binding.cardAppearanceWallpaperDark;
    }
    cardActive.setStrokeColor(ResUtil.getColorAttr(activity, R.attr.colorPrimary));
    cardActive.setStrokeWidth(SystemUiUtil.dpToPx(activity, 2));
    cardActive.setChecked(true);
    cardInactive.setStrokeColor(ResUtil.getColorAttr(activity, R.attr.colorOutline));
    cardInactive.setStrokeWidth(SystemUiUtil.dpToPx(activity, 1));
    cardInactive.setChecked(false);

    if (animated) {
      binding.imageAppearanceWallpaperMode.setImageResource(
          isDarkMode
              ? R.drawable.ic_round_light_mode_to_dark_mode_anim
              : R.drawable.ic_round_dark_mode_to_light_mode_anim
      );
      ViewUtil.startIcon(binding.imageAppearanceWallpaper);
      ViewUtil.startIcon(binding.imageAppearanceWallpaperMode);
    } else {
      binding.imageAppearanceWallpaperMode.setImageResource(
          isDarkMode
              ? R.drawable.ic_round_dark_mode_to_light_mode_anim
              : R.drawable.ic_round_light_mode_to_dark_mode_anim
      );
    }

    updateDarkModeDependencies();
  }

  public void updateDarkModeDependencies() {
    suffix = Constants.getDarkSuffix(isDarkMode);

    areListenersActive = false;

    float scale = getSharedPrefs().getFloat(
        PREF.SCALE + suffix,
        WallpaperDrawable.getDefaultScale(activity, isDarkMode)
    );
    binding.sliderAppearanceScale.setValue(scale * 10);

    binding.sliderAppearanceStaticOffset.setValue(
        getSharedPrefs().getInt(PREF.STATIC_OFFSET + suffix, DEF.STATIC_OFFSET)
    );

    binding.sliderAppearanceDimming.setValue(
        getSharedPrefs().getInt(
            PREF.DIMMING + suffix, isDarkMode ? DEF.DIMMING_DARK : DEF.DIMMING_LIGHT
        )
    );

    binding.switchAppearanceDarkText.setChecked(
        getSharedPrefs().getBoolean(PREF.USE_DARK_TEXT + suffix, DEF.USE_DARK_TEXT)
    );
    binding.switchAppearanceLightText.setChecked(
        getSharedPrefs().getBoolean(PREF.FORCE_LIGHT_TEXT + suffix, DEF.FORCE_LIGHT_TEXT)
    );
    areListenersActive = true;
  }

  public void loadPreview(boolean isDarkMode) {
    int screenWidth = SystemUiUtil.getDisplayWidth(activity);
    int screenHeight = SystemUiUtil.getDisplayHeight(activity);
    if (screenWidth > screenHeight) {
      int width = screenWidth;
      //noinspection SuspiciousNameCombination
      screenWidth = screenHeight;
      //noinspection SuspiciousNameCombination
      screenHeight = width;
    }
    float screenRatio = (float) screenWidth / screenHeight;
    int previewHeight = SystemUiUtil.dpToPx(activity, 150);
    int previewWidth = (int) (previewHeight * screenRatio);
    scaleRatio = ((float) previewHeight) / ((float) screenHeight);

    if (isDarkMode) {
      binding.cardAppearanceWallpaperDark.setLayoutParams(
          new LinearLayout.LayoutParams(previewWidth, previewHeight)
      );
    } else {
      LinearLayout.LayoutParams paramsLight = new LinearLayout.LayoutParams(
          previewWidth, previewHeight
      );
      if (ResUtil.isLayoutRtl(activity)) {
        paramsLight.leftMargin = SystemUiUtil.dpToPx(activity, 8);
      } else {
        paramsLight.rightMargin = SystemUiUtil.dpToPx(activity, 8);
      }
      binding.cardAppearanceWallpaperLight.setLayoutParams(paramsLight);
    }

    String suffix = Constants.getDarkSuffix(isDarkMode);
    String base64 = getSharedPrefs().getString(PREF.WALLPAPER + suffix, DEF.WALLPAPER);
    if (base64 != null) {
      BitmapDrawable drawable = BitmapUtil.getBitmapDrawable(activity, base64);
      if (isDarkMode) {
        wallpaperDrawableDark = new WallpaperDrawable(activity, drawable);
        binding.wallpaperAppearanceDark.setWallpaper(wallpaperDrawableDark);
      } else {
        wallpaperDrawableLight = new WallpaperDrawable(activity, drawable);
        binding.wallpaperAppearanceLight.setWallpaper(wallpaperDrawableLight);
      }
    } else {
      if (isDarkMode) {
        wallpaperDrawableDark = null;
      } else {
        wallpaperDrawableLight = null;
      }
    }
  }

  public void updatePreview(boolean isDarkMode) {
    suffix = Constants.getDarkSuffix(isDarkMode);

    float scale = getSharedPrefs().getFloat(
        PREF.SCALE + suffix,
        WallpaperDrawable.getDefaultScale(activity, isDarkMode)
    );
    scale *= scaleRatio;

    float offset = getSharedPrefs().getInt(PREF.STATIC_OFFSET + suffix, DEF.STATIC_OFFSET);
    offset *= scaleRatio;

    int dimming = getSharedPrefs().getInt(
        PREF.DIMMING + suffix, isDarkMode ? DEF.DIMMING_DARK : DEF.DIMMING_LIGHT
    );

    if (isDarkMode) {
      if (wallpaperDrawableDark != null) {
        binding.wallpaperAppearanceDark.setVisibility(View.VISIBLE);
        wallpaperDrawableDark.setScale(scale);
        wallpaperDrawableDark.setStaticOffsetX(offset);
        wallpaperDrawableDark.setDimming(dimming / 10f);
        binding.wallpaperAppearanceDark.invalidate();
      } else {
        binding.wallpaperAppearanceDark.setVisibility(View.GONE);
      }
    } else {
      if (wallpaperDrawableLight != null) {
        binding.wallpaperAppearanceLight.setVisibility(View.VISIBLE);
        wallpaperDrawableLight.setScale(scale);
        wallpaperDrawableLight.setStaticOffsetX(offset);
        wallpaperDrawableLight.setDimming(dimming / 10f);
        binding.wallpaperAppearanceLight.invalidate();
      } else {
        binding.wallpaperAppearanceLight.setVisibility(View.GONE);
      }
    }
    if (wallpaperDrawableLight != null && wallpaperDrawableDark != null) {
      binding.imageAppearanceWallpaperMode.setVisibility(View.VISIBLE);
      binding.imageAppearanceWallpaper.setVisibility(View.GONE);
    } else {
      binding.imageAppearanceWallpaperMode.setVisibility(View.GONE);
      binding.imageAppearanceWallpaper.setVisibility(View.VISIBLE);
    }
  }
}