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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.Locale;
import java.util.Objects;
import com.patrickzedler.pallax.Constants;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.activity.MainActivity;
import com.patrickzedler.pallax.adapter.LanguageAdapter;
import com.patrickzedler.pallax.databinding.FragmentBottomsheetLanguagesBinding;
import com.patrickzedler.pallax.fragment.OtherFragment;
import com.patrickzedler.pallax.model.Language;
import com.patrickzedler.pallax.util.LocaleUtil;
import com.patrickzedler.pallax.util.SystemUiUtil;

public class LanguagesBottomSheetDialogFragment extends BaseBottomSheetDialogFragment
    implements LanguageAdapter.LanguageAdapterListener {

  private static final String TAG = "LanguagesBottomSheet";

  private FragmentBottomsheetLanguagesBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentBottomsheetLanguagesBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();
    String selectedCode = getSharedPrefs().getString(
        Constants.PREF.LANGUAGE, Constants.DEF.LANGUAGE
    );

    binding.textLanguagesTitle.setText(getString(R.string.action_language_select));
    binding.textLanguagesDescription.setText(getString(R.string.other_language_description));
    binding.textLanguagesDescription.setVisibility(View.VISIBLE);

    binding.recyclerLanguages.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recyclerLanguages.setAdapter(
        new LanguageAdapter(LocaleUtil.getLanguages(activity), selectedCode, this)
    );

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(Language language) {
    String previousCode = getSharedPrefs().getString(Constants.PREF.LANGUAGE, null);
    String selectedCode = language != null ? language.getCode() : null;

    if (Objects.equals(previousCode, selectedCode)) {
      return;
    } else if (previousCode == null || selectedCode == null) {
      Locale localeDevice = LocaleUtil.getNearestSupportedLocale(
          activity, LocaleUtil.getDeviceLocale()
      );
      String codeToCompare = previousCode == null ? selectedCode : previousCode;
      if (Objects.equals(localeDevice.toString(), codeToCompare)) {
        OtherFragment fragment = (OtherFragment) activity.getCurrentFragment();
        fragment.setLanguage(language);
        dismiss();
      } else {
        dismiss();
        activity.restartToApply(150);
      }
    } else {
      dismiss();
      activity.restartToApply(150);
    }

    getSharedPrefs().edit().putString(Constants.PREF.LANGUAGE, selectedCode).apply();
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerLanguages.setPadding(
        0, SystemUiUtil.dpToPx(requireContext(), 8),
        0, SystemUiUtil.dpToPx(requireContext(), 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
