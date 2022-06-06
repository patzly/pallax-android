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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.databinding.FragmentBottomsheetPermissionBinding;

public class PermissionBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private static final String TAG = "PermissionBottomSheet";

  private FragmentBottomsheetPermissionBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
    binding = FragmentBottomsheetPermissionBinding.inflate(inflater, container, false);

    binding.buttonPermissionCancel.setOnClickListener(v -> {
      performHapticClick();
      dismiss();
    });

    binding.buttonPermissionGrant.setOnClickListener(v -> {
      performHapticClick();
      ((MainActivity) requireActivity()).requestPermission();
      dismiss();
    });

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearPermissionContainer.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
