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

package com.patrickzedler.pallax.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.databinding.RowLanguageBinding;
import com.patrickzedler.pallax.model.Language;
import com.patrickzedler.pallax.util.LocaleUtil;
import com.patrickzedler.pallax.util.ViewUtil;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {

  private static final String TAG = LanguageAdapter.class.getSimpleName();

  private final List<Language> languages;
  private final String selectedCode;
  private final LanguageAdapterListener listener;
  private final HashMap<String, Language> languageHashMap;

  public LanguageAdapter(
      List<Language> languages, String selectedCode, LanguageAdapterListener listener
  ) {
    this.languages = languages;
    this.selectedCode = selectedCode;
    this.listener = listener;
    this.languageHashMap = new HashMap<>();
    for (Language language : languages) {
      languageHashMap.put(language.getCode(), language);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowLanguageBinding binding;

    public ViewHolder(RowLanguageBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        RowLanguageBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
    holder.binding.frameLanguageContainer.setBackground(
        ViewUtil.getRippleBgListItemSurfaceRecyclerItem(holder.binding.getRoot().getContext())
    );

    if (position == 0) {
      Locale localeSystem = LocaleUtil.getNearestSupportedLocale(
          languageHashMap, LocaleUtil.getDeviceLocale()
      );
      Language language = languageHashMap.get(localeSystem.toString());
      if (language == null) {
        language = languageHashMap.get(localeSystem.getLanguage());
      }

      String translators = language != null
          ? language.getTranslators()
          : holder.binding.getRoot().getContext().getString(
              R.string.other_language_not_available
          );

      holder.binding.textLanguageName.setText(
          holder.binding.textLanguageName.getContext().getString(
              R.string.other_language_system,
              localeSystem.getDisplayName(localeSystem)
          )
      );
      holder.binding.textLanguageTranslators.setText(translators);

      holder.binding.imageLanguageSelected.setVisibility(
          selectedCode != null ? View.INVISIBLE : View.VISIBLE
      );
      holder.binding.frameLanguageContainer.setOnClickListener(
          view -> listener.onItemRowClicked(null)
      );
      return;
    }

    Language language = languages.get(holder.getAdapterPosition() - 1);
    holder.binding.textLanguageName.setText(language.getName());
    holder.binding.textLanguageTranslators.setText(language.getTranslators());

    // SELECTED

    holder.binding.imageLanguageSelected.setVisibility(
        language.getCode().equals(selectedCode) ? View.VISIBLE : View.INVISIBLE
    );

    // CONTAINER

    holder.binding.frameLanguageContainer.setOnClickListener(
        view -> listener.onItemRowClicked(language)
    );
  }

  @Override
  public int getItemCount() {
    return languages.size() + 1;
  }

  public interface LanguageAdapterListener {

    void onItemRowClicked(Language language);
  }
}
