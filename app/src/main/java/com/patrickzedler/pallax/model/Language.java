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

package com.patrickzedler.pallax.model;

import com.patrickzedler.pallax.util.LocaleUtil;

public class Language {

  private final String code;
  private final String translators;
  private final String name;

  public Language(String codeTranslators) {
    String[] parts = codeTranslators.split("\n");
    code = parts[0];
    translators = parts[1];
    name = LocaleUtil.getLocaleFromCode(code).getDisplayName(LocaleUtil.getDeviceLocale());
  }

  public String getCode() {
    return code;
  }

  public String getTranslators() {
    return translators;
  }

  public String getName() {
    return name;
  }
}
