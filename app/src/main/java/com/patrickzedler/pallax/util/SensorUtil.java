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
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class SensorUtil {

  public static boolean hasAccelerometer(Context context) {
    return getAccelerometer(context) != null;
  }

  public static Sensor getAccelerometer(Context context) {
    SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
  }
}
