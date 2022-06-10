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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.snackbar.Snackbar;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.EXTRA;
import com.patrickzedler.pallax.Constants.MODE;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.Constants.THEME;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.behavior.ScrollBehavior;
import com.patrickzedler.pallax.behavior.SystemBarBehavior;
import com.patrickzedler.pallax.databinding.FragmentOtherBinding;
import com.patrickzedler.pallax.model.Language;
import com.patrickzedler.pallax.service.LiveWallpaperService;
import com.patrickzedler.pallax.util.LocaleUtil;
import com.patrickzedler.pallax.util.ResUtil;
import com.patrickzedler.pallax.util.SystemUiUtil;
import com.patrickzedler.pallax.util.ViewUtil;
import com.patrickzedler.pallax.view.SelectionCardView;
import java.util.Locale;

public class OtherFragment extends BaseFragment implements OnClickListener {

  private static final String TAG = OtherFragment.class.getSimpleName();

  private FragmentOtherBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentOtherBinding.inflate(inflater, container, false);
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
    systemBarBehavior.setAppBar(binding.appBarOther);
    systemBarBehavior.setScroll(binding.scrollOther, binding.constraintOther);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarOther, binding.scrollOther, true
    );

    ViewUtil.centerToolbarTitleOnLargeScreens(binding.toolbarOther);
    binding.toolbarOther.setNavigationOnClickListener(v -> {
      if (getViewUtil().isClickEnabled(v.getId())) {
        performHapticClick();
        navigateUp();
      }
    });
    binding.toolbarOther.setOnMenuItemClickListener(item -> {
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

    binding.textOtherLanguage.setText(getLanguage());

    setUpThemeSelection();

    int id;
    switch (getSharedPrefs().getInt(PREF.MODE, DEF.MODE)) {
      case MODE.LIGHT:
        id = R.id.button_other_theme_light;
        break;
      case MODE.DARK:
        id = R.id.button_other_theme_dark;
        break;
      default:
        id = R.id.button_other_theme_auto;
        break;
    }
    binding.toggleOtherTheme.check(id);
    binding.toggleOtherTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      int pref;
      if (checkedId == R.id.button_other_theme_light) {
        pref = MODE.LIGHT;
      } else if (checkedId == R.id.button_other_theme_dark) {
        pref = MODE.DARK;
      } else {
        pref = MODE.AUTO;
      }
      getSharedPrefs().edit().putInt(PREF.MODE, pref).apply();
      performHapticClick();
      activity.restartToApply(
          0, getInstanceState(), false, true
      );
    });

    ViewUtil.setOnClickListeners(
        this,
        binding.linearOtherLanguage,
        binding.linearOtherReset
    );
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_other_language && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageOtherLanguage);
      performHapticClick();
      navigate(OtherFragmentDirections.actionOtherToLanguagesDialog());
    } else if (id == R.id.linear_other_reset && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageOtherReset);
      performHapticClick();
      activity.showSnackbar(
          activity.getSnackbar(
              R.string.msg_reset, Snackbar.LENGTH_LONG
          ).setAction(
              getString(R.string.action_reset), view -> {
                performHapticHeavyClick();
                activity.reset();
                activity.restartToApply(
                    100,
                    getInstanceState(),
                    LiveWallpaperService.isMainEngineRunning(),
                    false
                );
              }
          )
      );
    }
  }

  public void setLanguage(Language language) {
    Locale locale = language != null
        ? LocaleUtil.getLocaleFromCode(language.getCode())
        : LocaleUtil.getNearestSupportedLocale(activity, LocaleUtil.getDeviceLocale());
    binding.textOtherLanguage.setText(
        language != null
            ? locale.getDisplayName()
            : getString(R.string.other_language_system, locale.getDisplayName())
    );
  }

  public String getLanguage() {
    String code = getSharedPrefs().getString(PREF.LANGUAGE, DEF.LANGUAGE);
    Locale locale = code != null
        ? LocaleUtil.getLocaleFromCode(code)
        : LocaleUtil.getNearestSupportedLocale(activity, LocaleUtil.getDeviceLocale());
    return code != null
        ? locale.getDisplayName()
        : getString(R.string.other_language_system, locale.getDisplayName());
  }

  private void setUpThemeSelection() {
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    ViewGroup container = binding.linearOtherThemeContainer;
    int colorsCount = 8;
    for (int i = hasDynamic ? -1 : 0; i <= colorsCount; i++) {
      String name;
      int resId;
      if (i == -1) {
        name = THEME.DYNAMIC;
        resId = -1;
      } else if (i == 0) {
        name = THEME.RED;
        resId = R.style.Theme_Pallax_Red;
      } else if (i == 1) {
        name = THEME.YELLOW;
        resId = R.style.Theme_Pallax_Yellow;
      } else if (i == 2) {
        name = THEME.LIME;
        resId = R.style.Theme_Pallax_Lime;
      } else if (i == 3) {
        name = THEME.GREEN;
        resId = R.style.Theme_Pallax_Green;
      } else if (i == 4) {
        name = THEME.TURQUOISE;
        resId = R.style.Theme_Pallax_Turquoise;
      } else if (i == 5) {
        name = THEME.TEAL;
        resId = R.style.Theme_Pallax_Teal;
      } else if (i == 6) {
        name = THEME.BLUE;
        resId = R.style.Theme_Pallax_Blue;
      } else if (i == 7) {
        name = THEME.PURPLE;
        resId = R.style.Theme_Pallax_Purple;
      } else if (i == 8) {
        name = THEME.AMOLED;
        resId = R.style.Theme_Pallax_Amoled;
      } else {
        name = THEME.BLUE;
        resId = R.style.Theme_Pallax_Blue;
      }

      SelectionCardView card = new SelectionCardView(activity);
      card.setScrimEnabled(false, false);
      int color;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && i == -1) {
        color = ContextCompat.getColor(
            activity,
            SystemUiUtil.isDarkModeActive(activity)
                ? android.R.color.system_accent1_700
                : android.R.color.system_accent1_100
        );
      } else if (i == colorsCount) {
        // Amoled theme selection card
        color = SystemUiUtil.isDarkModeActive(activity) ? 0x484848 : 0xe3e3e3;
      } else {
        color = ResUtil.getColorAttr(
            new ContextThemeWrapper(activity, resId), R.attr.colorPrimaryContainer
        );
      }
      card.setCardBackgroundColor(color);
      card.setOnClickListener(v -> {
        if (!card.isChecked()) {
          card.startCheckedIcon();
          ViewUtil.startIcon(binding.imageOtherTheme);
          performHapticClick();
          ViewUtil.uncheckAllChildren(container);
          card.setChecked(true);
          getSharedPrefs().edit().putString(PREF.THEME, name).apply();
          activity.restartToApply(
              100, getInstanceState(), false, true
          );
        }
      });

      String selected = getSharedPrefs().getString(PREF.THEME, DEF.THEME);
      boolean isSelected;
      if (selected.isEmpty()) {
        isSelected = hasDynamic ? name.equals(THEME.DYNAMIC) : name.equals(THEME.BLUE);
      } else {
        isSelected = selected.equals(name);
      }
      card.setChecked(isSelected);
      container.addView(card);
    }

    Bundle bundleInstanceState = activity.getIntent().getBundleExtra(EXTRA.INSTANCE_STATE);
    if (bundleInstanceState != null) {
      binding.scrollHorizOtherTheme.scrollTo(
          bundleInstanceState.getInt(EXTRA.SCROLL_POSITION, 0),
          0
      );
    }
  }

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(EXTRA.SCROLL_POSITION, binding.scrollHorizOtherTheme.getScrollX());
    }
    return bundle;
  }
}