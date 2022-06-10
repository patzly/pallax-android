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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.behavior.ScrollBehavior;
import com.patrickzedler.pallax.behavior.SystemBarBehavior;
import com.patrickzedler.pallax.databinding.FragmentOverviewBinding;
import com.patrickzedler.pallax.util.ViewUtil;

public class OverviewFragment extends BaseFragment implements OnClickListener {

  private static final String TAG = OverviewFragment.class.getSimpleName();

  private FragmentOverviewBinding binding;
  private MainActivity activity;
  private ViewUtil viewUtilLogo;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentOverviewBinding.inflate(inflater, container, false);
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

    viewUtilLogo = new ViewUtil(500);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarOverview);
    systemBarBehavior.setScroll(binding.scrollOverview, binding.constraintOverview);
    systemBarBehavior.setAdditionalBottomInset(activity.getFabTopEdgeDistance());
    systemBarBehavior.setUp();

    new ScrollBehavior(activity).setUpScroll(
        binding.appBarOverview, binding.scrollOverview, true
    );

    binding.toolbarOverview.setOnMenuItemClickListener(getOnMenuItemClickListener());
    MenuItem itemHelp = binding.toolbarOverview.getMenu().findItem(R.id.action_help);
    if (itemHelp != null) {
      itemHelp.setVisible(false);
    }

    boolean shouldLogoBeVisible = activity.shouldLogoBeVisibleOnOverviewPage();

    binding.frameOverviewClose.setVisibility(shouldLogoBeVisible ? View.GONE : View.VISIBLE);
    binding.frameOverviewLogo.setVisibility(shouldLogoBeVisible ? View.VISIBLE : View.GONE);
    if (activity.shouldLogoBeVisibleOnOverviewPage()) {
      binding.appBarOverview.setOnClickListener(v -> {
        if (viewUtilLogo.isClickEnabled(v.getId())) {
          ViewUtil.startIcon(binding.imageOverviewLogo);
          performHapticClick();
        }
      });
    } else {
      binding.frameOverviewClose.setOnClickListener(v -> {
        if (getViewUtil().isClickEnabled(v.getId())) {
          performHapticClick();
          activity.finish();
        }
      });
    }

    ViewUtil.setOnClickListeners(
        this,
        binding.buttonOverviewInfo,
        binding.buttonOverviewHelp,
        binding.linearOverviewAppearance,
        binding.linearOverviewParallax,
        binding.linearOverviewSize,
        binding.linearOverviewOther,
        binding.linearOverviewAbout
    );
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (getViewUtil().isClickDisabled(id)) {
      return;
    }
    performHapticClick();

    if (id == R.id.button_overview_info) {
      activity.showTextBottomSheet(R.raw.howto, R.string.action_howto);
    } else if (id == R.id.button_overview_help) {
      activity.showTextBottomSheet(R.raw.help, R.string.action_help);
    } else if (id == R.id.linear_overview_appearance) {
      navigate(OverviewFragmentDirections.actionOverviewToAppearance());
    } else if (id == R.id.linear_overview_parallax) {
      navigate(OverviewFragmentDirections.actionOverviewToParallax());
    } else if (id == R.id.linear_overview_size) {
      navigate(OverviewFragmentDirections.actionOverviewToZoom());
    } else if (id == R.id.linear_overview_other) {
      navigate(OverviewFragmentDirections.actionOverviewToOther());
    } else if (id == R.id.linear_overview_about) {
      navigate(OverviewFragmentDirections.actionOverviewToAbout());
    }
  }
}