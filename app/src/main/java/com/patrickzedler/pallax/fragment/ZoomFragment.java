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

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.NavMainDirections;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.behavior.ScrollBehavior;
import com.patrickzedler.pallax.behavior.SystemBarBehavior;
import com.patrickzedler.pallax.databinding.FragmentZoomBinding;
import com.patrickzedler.pallax.util.ViewUtil;

public class ZoomFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnChangeListener {

  private FragmentZoomBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentZoomBinding.inflate(inflater, container, false);
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
    systemBarBehavior.setAppBar(binding.appBarZoom);
    systemBarBehavior.setScroll(binding.scrollZoom, binding.constraintZoom);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarZoom, binding.scrollZoom, true
    );

    ViewUtil.centerToolbarTitleOnLargeScreens(binding.toolbarZoom);
    binding.toolbarZoom.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarZoom.setOnMenuItemClickListener(getOnMenuItemClickListener());

    binding.sliderZoomIntensity.setValue(
        getSharedPrefs().getInt(PREF.ZOOM_INTENSITY, DEF.ZOOM_INTENSITY)
    );
    binding.sliderZoomIntensity.addOnChangeListener(this);
    binding.sliderZoomIntensity.setLabelFormatter(
        value -> String.format(activity.getLocale(), "%.0f", value)
    );

    binding.switchZoomPowerSave.setChecked(
        getSharedPrefs().getBoolean(PREF.POWER_SAVE_ZOOM, DEF.POWER_SAVE_ZOOM)
    );

    int launcherZoom = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? View.VISIBLE : View.GONE;
    binding.linearZoomLauncher.setVisibility(launcherZoom);
    binding.switchZoomLauncher.setChecked(
        getSharedPrefs().getBoolean(PREF.ZOOM_LAUNCHER, DEF.ZOOM_LAUNCHER)
    );

    boolean useZoomDamping = getSharedPrefs().getBoolean(
        PREF.USE_ZOOM_DAMPING, DEF.USE_ZOOM_DAMPING
    );
    binding.switchZoomDamping.setChecked(useZoomDamping);
    binding.sliderZoomDamping.setValue(
        getSharedPrefs().getInt(PREF.DAMPING_ZOOM, DEF.DAMPING_ZOOM)
    );
    binding.sliderZoomDamping.addOnChangeListener(this);
    binding.sliderZoomDamping.setLabelFormatter(
        value -> String.format(activity.getLocale(), "%.0f", value)
    );

    boolean systemZoomAvailable = Build.VERSION.SDK_INT == Build.VERSION_CODES.R;
    if (!systemZoomAvailable && getSharedPrefs().getBoolean(PREF.ZOOM_SYSTEM, DEF.ZOOM_SYSTEM)) {
      // Only available on Android 11, turn off previously enabled
      getSharedPrefs().edit().putBoolean(PREF.ZOOM_SYSTEM, false).apply();
    }
    binding.linearZoomSystem.setVisibility(systemZoomAvailable ? View.VISIBLE : View.GONE);
    if (systemZoomAvailable) {
      setZoomSystemEnabled(binding.switchZoomLauncher.isChecked(), false);
      binding.switchZoomSystem.setChecked(
          getSharedPrefs().getBoolean(PREF.ZOOM_SYSTEM, DEF.ZOOM_SYSTEM)
      );
    }

    setZoomDampingEnabled(
        binding.switchZoomLauncher.isChecked()
            && !binding.switchZoomSystem.isChecked(),
        false
    );
    binding.sliderZoomDamping.setEnabled(
        binding.linearZoomDamping.isEnabled() && useZoomDamping
    );

    binding.switchZoomUnlock.setChecked(
        getSharedPrefs().getBoolean(PREF.ZOOM_UNLOCK, DEF.ZOOM_UNLOCK)
    );

    binding.sliderZoomDuration.setValue(
        getSharedPrefs().getInt(PREF.ZOOM_DURATION, DEF.ZOOM_DURATION)
    );
    binding.sliderZoomDuration.addOnChangeListener(this);
    binding.sliderZoomDuration.setLabelFormatter(
        value -> getString(
            R.string.label_ms, String.format(activity.getLocale(), "%.0f", value)
        )
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearZoomPowerSave,
        binding.linearZoomLauncher,
        binding.linearZoomDamping,
        binding.linearZoomSystem,
        binding.linearZoomUnlock
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchZoomPowerSave,
        binding.switchZoomLauncher,
        binding.switchZoomDamping,
        binding.switchZoomSystem,
        binding.switchZoomUnlock
    );
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_zoom_power_save) {
      binding.switchZoomPowerSave.setChecked(
          !binding.switchZoomPowerSave.isChecked()
      );
    } else if (id == R.id.linear_zoom_launcher) {
      binding.switchZoomLauncher.setChecked(!binding.switchZoomLauncher.isChecked());
    } else if (id == R.id.linear_zoom_damping) {
      binding.switchZoomDamping.setChecked(!binding.switchZoomDamping.isChecked());
    } else if (id == R.id.linear_zoom_system && binding.switchZoomLauncher.isChecked()) {
      binding.switchZoomSystem.setChecked(!binding.switchZoomSystem.isChecked());
    } else if (id == R.id.linear_zoom_unlock) {
      binding.switchZoomUnlock.setChecked(!binding.switchZoomUnlock.isChecked());
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_zoom_power_save) {
      getSharedPrefs().edit().putBoolean(PREF.POWER_SAVE_ZOOM, isChecked).apply();
      activity.requestSettingsRefresh();
      ViewUtil.startIcon(binding.imageZoomPowerSave);
      performHapticClick();
    } else if (id == R.id.switch_zoom_launcher) {
      getSharedPrefs().edit().putBoolean(PREF.ZOOM_LAUNCHER, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
      setZoomDampingEnabled(
          isChecked && !binding.switchZoomSystem.isChecked(), true
      );
      setZoomSystemEnabled(isChecked, true);
    } else if (id == R.id.switch_zoom_damping) {
      getSharedPrefs().edit().putBoolean(PREF.USE_ZOOM_DAMPING, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
      binding.sliderZoomDamping.setEnabled(isChecked);
      ViewUtil.startIcon(binding.imageZoomDamping);
    } else if (id == R.id.switch_zoom_system) {
      getSharedPrefs().edit().putBoolean(PREF.ZOOM_SYSTEM, isChecked).apply();
      performHapticClick();
      setZoomDampingEnabled(!isChecked, true);
      activity.showForceStopRequest(NavMainDirections.actionGlobalApplyDialog());
    } else if (id == R.id.switch_zoom_unlock) {
      getSharedPrefs().edit().putBoolean(PREF.ZOOM_UNLOCK, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    int id = slider.getId();
    if (id == R.id.slider_zoom_intensity) {
      getSharedPrefs().edit().putInt(PREF.ZOOM_INTENSITY, (int) value).apply();
      ViewUtil.startIcon(binding.imageZoomIntensity);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_zoom_damping) {
      getSharedPrefs().edit().putInt(PREF.DAMPING_ZOOM, (int) value).apply();
      ViewUtil.startIcon(binding.imageZoomDamping);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_zoom_duration) {
      getSharedPrefs().edit().putInt(PREF.ZOOM_DURATION, (int) value).apply();
      ViewUtil.startIcon(binding.imageZoomDuration);
      activity.requestSettingsRefresh();
      performHapticClick();
    }
  }

  private void setZoomDampingEnabled(boolean enabled, boolean animated) {
    ViewUtil.setEnabledAlpha(enabled, animated, binding.linearZoomDamping);
    binding.switchZoomDamping.setEnabled(enabled);
    binding.sliderZoomDamping.setEnabled(enabled && binding.switchZoomDamping.isChecked());
  }

  private void setZoomSystemEnabled(boolean enabled, boolean animated) {
    ViewUtil.setEnabledAlpha(enabled, animated, binding.linearZoomSystem);
    binding.switchZoomSystem.setEnabled(enabled);
  }
}