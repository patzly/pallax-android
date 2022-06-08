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
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.behavior.ScrollBehavior;
import com.patrickzedler.pallax.behavior.SystemBarBehavior;
import com.patrickzedler.pallax.databinding.FragmentParallaxBinding;
import com.patrickzedler.pallax.util.ResUtil;
import com.patrickzedler.pallax.util.SensorUtil;
import com.patrickzedler.pallax.util.ViewUtil;

public class ParallaxFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnChangeListener {

  private static final String TAG = ParallaxFragment.class.getSimpleName();

  private FragmentParallaxBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentParallaxBinding.inflate(inflater, container, false);
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
    systemBarBehavior.setAppBar(binding.appBarParallax);
    systemBarBehavior.setScroll(binding.scrollParallax, binding.constraintParallax);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarParallax, binding.scrollParallax, true
    );

    ViewUtil.centerToolbarTitleOnLargeScreens(binding.toolbarParallax);
    binding.toolbarParallax.setNavigationOnClickListener(v -> {
      if (getViewUtil().isClickEnabled(v.getId())) {
        performHapticClick();
        navigateUp();
      }
    });
    binding.toolbarParallax.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();

      if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_help) {
        activity.showTextBottomSheet(R.raw.help, R.string.action_help);
      } else if (id == R.id.action_share) {
        ResUtil.share(activity, R.string.msg_share);
      }
      return true;
    });

    binding.sliderParallaxIntensity.setValue(getSharedPrefs().getInt(PREF.PARALLAX, DEF.PARALLAX));
    binding.sliderParallaxIntensity.addOnChangeListener(this);
    binding.sliderParallaxIntensity.setLabelFormatter(value -> {
      if (value == 0) {
        return getString(R.string.parallax_none);
      } else {
        return String.valueOf((int) value);
      }
    });

    binding.switchParallaxSwipePowerSave.setChecked(
        getSharedPrefs().getBoolean(PREF.POWER_SAVE_SWIPE, DEF.POWER_SAVE_SWIPE)
    );

    binding.linearParallaxTiltContainer.setVisibility(
        SensorUtil.hasAccelerometer(activity) ? View.VISIBLE : View.GONE
    );

    binding.switchParallaxTilt.setChecked(getSharedPrefs().getBoolean(PREF.TILT, DEF.TILT));

    binding.switchParallaxTiltPowerSave.setChecked(
        getSharedPrefs().getBoolean(PREF.POWER_SAVE_TILT, DEF.POWER_SAVE_TILT)
    );

    ViewUtil.setEnabledAlpha(
        binding.switchParallaxTilt.isChecked(),
        false,
        binding.linearParallaxRefreshRate,
        binding.linearParallaxDamping,
        binding.linearParallaxThreshold,
        binding.linearParallaxTiltPowerSave
    );
    ViewUtil.setEnabled(
        binding.switchParallaxTilt.isChecked(),
        binding.sliderParallaxRefreshRate,
        binding.sliderParallaxDamping,
        binding.sliderParallaxThreshold,
        binding.switchParallaxTiltPowerSave
    );

    binding.sliderParallaxRefreshRate.setValue(
        getSharedPrefs().getInt(PREF.REFRESH_RATE, DEF.REFRESH_RATE)
    );
    binding.sliderParallaxRefreshRate.addOnChangeListener(this);
    binding.sliderParallaxRefreshRate.setLabelFormatter(
        value -> getString(
            R.string.label_ms,
            String.format(activity.getLocale(), "%.0f", value / 1000)
        )
    );

    binding.sliderParallaxDamping.setValue(
        getSharedPrefs().getInt(PREF.DAMPING_TILT, DEF.DAMPING_TILT)
    );
    binding.sliderParallaxDamping.addOnChangeListener(this);
    binding.sliderParallaxDamping.setLabelFormatter(
        value -> String.format(activity.getLocale(), "%.0f", value)
    );

    binding.sliderParallaxThreshold.setValue(
        getSharedPrefs().getInt(PREF.THRESHOLD, DEF.THRESHOLD)
    );
    binding.sliderParallaxThreshold.addOnChangeListener(this);
    binding.sliderParallaxThreshold.setLabelFormatter(
        value -> String.format(activity.getLocale(), "%.0f", value)
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearParallaxSwipePowerSave,
        binding.linearParallaxTilt,
        binding.linearParallaxTiltPowerSave
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchParallaxSwipePowerSave,
        binding.switchParallaxTilt,
        binding.switchParallaxTiltPowerSave
    );
  }

  @Override
  public void onResume() {
    super.onResume();

    binding.cardParallaxTouchWiz.setVisibility(activity.isTouchWiz() ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_parallax_swipe_power_save) {
      binding.switchParallaxSwipePowerSave.setChecked(
          !binding.switchParallaxSwipePowerSave.isChecked()
      );
    } else if (id == R.id.linear_parallax_tilt) {
      binding.switchParallaxTilt.setChecked(!binding.switchParallaxTilt.isChecked());
    } else if (id == R.id.linear_parallax_tilt_power_save) {
      binding.switchParallaxTiltPowerSave.setChecked(
          !binding.switchParallaxTiltPowerSave.isChecked()
      );
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_parallax_swipe_power_save) {
      getSharedPrefs().edit().putBoolean(PREF.POWER_SAVE_SWIPE, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageParallaxSwipePowerSave);
    } else if (id == R.id.switch_parallax_tilt) {
      getSharedPrefs().edit().putBoolean(PREF.TILT, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageParallaxTilt);
      ViewUtil.setEnabledAlpha(
          isChecked,
          true,
          binding.linearParallaxRefreshRate,
          binding.linearParallaxDamping,
          binding.linearParallaxThreshold,
          binding.linearParallaxTiltPowerSave
      );
      ViewUtil.setEnabled(
          isChecked,
          binding.sliderParallaxRefreshRate,
          binding.sliderParallaxDamping,
          binding.sliderParallaxThreshold,
          binding.switchParallaxTiltPowerSave
      );
    } else if (id == R.id.switch_parallax_tilt_power_save) {
      getSharedPrefs().edit().putBoolean(PREF.POWER_SAVE_TILT, isChecked).apply();
      activity.requestSettingsRefresh();
      performHapticClick();
      ViewUtil.startIcon(binding.imageParallaxTiltPowerSave);
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_parallax_intensity) {
      getSharedPrefs().edit().putInt(PREF.PARALLAX, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxIntensity);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_parallax_refresh_rate) {
      getSharedPrefs().edit().putInt(PREF.REFRESH_RATE, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxRefreshRate);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_parallax_damping) {
      getSharedPrefs().edit().putInt(PREF.DAMPING_TILT, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxDamping);
      activity.requestSettingsRefresh();
      performHapticClick();
    } else if (id == R.id.slider_parallax_threshold) {
      getSharedPrefs().edit().putInt(PREF.THRESHOLD, (int) value).apply();
      ViewUtil.startIcon(binding.imageParallaxThreshold);
      activity.requestSettingsRefresh();
      performHapticClick();
    }
  }
}