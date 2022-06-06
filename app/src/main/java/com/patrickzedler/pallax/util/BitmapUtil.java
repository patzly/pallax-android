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

package com.patrickzedler.pallax.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import androidx.annotation.NonNull;
import java.io.ByteArrayOutputStream;

public class BitmapUtil {


  public static BitmapDrawable getBitmapDrawable(@NonNull Context context, @NonNull String base64) {
    byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    return new BitmapDrawable(context.getResources(), bitmap);
  }

  public static String getBase64(@NonNull BitmapDrawable drawable) {
    Bitmap bitmap = drawable.getBitmap();
    if (bitmap == null) {
      return "";
    }
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
    byte[] byteArray = byteStream.toByteArray();
    return Base64.encodeToString(byteArray, Base64.DEFAULT);
  }
}
