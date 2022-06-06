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

package com.patrickzedler.pallax.service;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.patrickzedler.pallax.Constants;
import com.patrickzedler.pallax.Constants.ACTION;
import com.patrickzedler.pallax.Constants.DARK_MODE;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.Constants.REQUEST_SOURCE;
import com.patrickzedler.pallax.Constants.USER_PRESENCE;
import com.patrickzedler.pallax.drawable.WallpaperDrawable;
import com.patrickzedler.pallax.util.BitmapUtil;
import com.patrickzedler.pallax.util.PrefsUtil;
import com.patrickzedler.pallax.util.SensorUtil;
import java.util.ArrayList;
import java.util.List;

public class LiveWallpaperService extends WallpaperService {

  private static final String TAG = LiveWallpaperService.class.getSimpleName();

  // All things where we need a context or the service's context are done in this Service class
  // All other things should be done in the inner Engine class

  private static LiveWallpaperService serviceInstance = null;
  private static UserAwareEngine nonPreviewEngineInstance = null;

  private SharedPreferences sharedPrefs;
  private WallpaperDrawable wallpaperDrawable;
  private int darkMode;
  private boolean isPowerSaveMode;
  private BroadcastReceiver receiver;
  private String presence;
  private boolean isReceiverRegistered = false;
  private UserPresenceListener userPresenceListener;
  private RefreshListener refreshListener;
  private SensorManager sensorManager;
  private PowerManager powerManager;

  @Override
  public void onCreate() {
    super.onCreate();

    serviceInstance = this;

    sharedPrefs = new PrefsUtil(this).checkForMigrations().getSharedPrefs();

    receiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
          case Intent.ACTION_USER_PRESENT:
            setUserPresence(USER_PRESENCE.UNLOCKED);
            break;
          case Intent.ACTION_SCREEN_OFF:
            setUserPresence(USER_PRESENCE.OFF);
            break;
          case Intent.ACTION_SCREEN_ON:
            setUserPresence(isKeyguardLocked() ? USER_PRESENCE.LOCKED : USER_PRESENCE.UNLOCKED);
            break;
          case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
            isPowerSaveMode = powerManager.isPowerSaveMode();
            break;
          case ACTION.THEME_CHANGED:
            if (refreshListener != null) {
              refreshListener.onRefreshTheme();
            }
            break;
          case ACTION.SETTINGS_CHANGED:
            if (refreshListener != null) {
              refreshListener.onRefreshSettings();
            }
            break;
        }
      }
    };
    registerReceiver();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    serviceInstance = null;
    unregisterReceiver();
  }

  @Override
  public Engine onCreateEngine() {
    setUserPresence(isKeyguardLocked() ? USER_PRESENCE.LOCKED : USER_PRESENCE.UNLOCKED);

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    isPowerSaveMode = powerManager.isPowerSaveMode();

    return new UserAwareEngine();
  }

  public static boolean isMainEngineRunning() {
    try {
      // If instance was not cleared but the service was destroyed an exception will be thrown
      if (serviceInstance != null && serviceInstance.ping()) {
        return nonPreviewEngineInstance != null
            && nonPreviewEngineInstance.ping();
      } else {
        return false;
      }
    } catch (Exception e) {
      // destroyed/not-started
      return false;
    }
  }

  private boolean ping() {
    return true;
  }

  private void registerReceiver() {
    if (!isReceiverRegistered) {
      IntentFilter filter = new IntentFilter();
      filter.addAction(Intent.ACTION_USER_PRESENT);
      filter.addAction(Intent.ACTION_SCREEN_OFF);
      filter.addAction(Intent.ACTION_SCREEN_ON);
      filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
      filter.addAction(ACTION.THEME_CHANGED);
      filter.addAction(ACTION.SETTINGS_CHANGED);
      registerReceiver(receiver, filter);
      isReceiverRegistered = true;
    }
  }

  private void unregisterReceiver() {
    if (isReceiverRegistered) {
      try {
        unregisterReceiver(receiver);
      } catch (Exception e) {
        Log.e(TAG, "unregisterReceiver", e);
      }
      isReceiverRegistered = false;
    }
  }

  private boolean isDarkMode() {
    if (darkMode == DARK_MODE.ON) {
      return true;
    }
    int flags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return darkMode == DARK_MODE.AUTO && flags == Configuration.UI_MODE_NIGHT_YES;
  }

  private boolean isKeyguardLocked() {
    return ((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked();
  }

  private void setUserPresence(final String presence) {
    if (presence.equals(this.presence)) {
      return;
    }
    this.presence = presence;
    if (userPresenceListener != null) {
      userPresenceListener.onPresenceChange(presence);
    }
  }

  private float getFrameRate() {
    WindowManager windowManager = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
    return windowManager != null ? windowManager.getDefaultDisplay().getRefreshRate() : 60;
  }

  private interface UserPresenceListener {

    void onPresenceChange(String presence);
  }

  private interface RefreshListener {

    void onRefreshTheme();

    void onRefreshSettings();
  }

  // ENGINE ------------------------------------------------------------

  class UserAwareEngine extends Engine implements UserPresenceListener, RefreshListener {

    private Context context;
    private boolean darkText, lightText;
    private boolean darkLauncher;
    private int dimming;
    private int zoomIntensity;
    private boolean isZoomLauncherEnabled, isZoomUnlockEnabled;
    private float zoomLauncher;
    private float zoomUnlock;
    private boolean useSystemZoom;
    private float scale;
    private int parallax;
    private int correction;
    private int zoomDuration;
    private int dampingTilt, dampingZoom;
    private boolean useZoomDamping;
    private boolean hasAccelerometer;
    private boolean isTiltEnabled;
    private float tiltX, tiltY;
    private int screenRotation;
    private int tiltThreshold;
    private float[] accelerationValues;
    private float offsetX;
    private long lastDrawZoomLauncher, lastDrawZoomUnlock, lastDrawTilt;
    private boolean isVisible;
    private boolean isDark;
    private boolean isListenerRegistered = false;
    private boolean isSurfaceAvailable = false;
    private boolean iconDropConsumed = true;
    private boolean isRtl = false;
    private boolean powerSaveSwipe, powerSaveTilt, powerSaveZoom;
    private float fps;
    private final TimeInterpolator zoomInterpolator = new FastOutSlowInInterpolator();
    private ValueAnimator zoomAnimator;
    private SensorEventListener sensorListener;
    private final List<Pair<Float, Float>> tiltHistory = new ArrayList<>();

    @Override
    public void onCreate(SurfaceHolder surfaceHolder) {
      super.onCreate(surfaceHolder);

      context = LiveWallpaperService.this;

      if (!isPreview()) {
        nonPreviewEngineInstance = this;
      }

      fps = getFrameRate();

      userPresenceListener = this;
      refreshListener = this;

      sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
          if (isVisible && isTiltEnabled && animTilt()) {
            accelerationValues = lowPassAcceleration(event.values, accelerationValues);
            tiltX = accelerationValues[0];
            tiltY = -accelerationValues[1];

            tiltHistory.add(new Pair<>(tiltX, tiltY));
            while (tiltHistory.size() > 30) {
              tiltHistory.remove(0);
            }

            float sumX = 0, sumY = 0;
            for (Pair<Float, Float> tilt : tiltHistory) {
              sumX += tilt.first;
              sumY += tilt.second;
            }
            float averageX = sumX / tiltHistory.size();
            float averageY = sumY / tiltHistory.size();
            float tolerance = tiltThreshold / 100f; // Allow small deviations caused by the sensor
            for (Pair<Float, Float> tilt : tiltHistory) {
              boolean isMovingX = averageX >= 0
                  ? tilt.first > averageX + tolerance
                  : tilt.first < averageX - tolerance;
              boolean isMovingY = averageY >= 0
                  ? tilt.second > averageY + tolerance
                  : tilt.second < averageY - tolerance;
              if (isMovingX || isMovingY) {
                updateOffset(false, REQUEST_SOURCE.TILT);
                return;
              }
            }
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
      };

      hasAccelerometer = SensorUtil.hasAccelerometer(context);

      loadSettings();
      loadTheme();

      zoomLauncher = 0;
      // This starts the zoom effect already in wallpaper preview
      zoomUnlock = useSystemZoom ? 0 : 1;
      if (!useSystemZoom) {
        animateZoom(0);
      }

      setTouchEventsEnabled(false);
      setOffsetNotificationsEnabled(true);
    }

    @Override
    public void onDestroy() {
      if (!isPreview()) {
        nonPreviewEngineInstance = null;
      }
      if (zoomAnimator != null) {
        zoomAnimator.cancel();
        zoomAnimator.removeAllUpdateListeners();
        zoomAnimator = null;
      }
      if (sensorManager != null && isListenerRegistered) {
        sensorManager.unregisterListener(sensorListener);
        isListenerRegistered = false;
      }
    }

    @Override
    public void onSurfaceCreated(SurfaceHolder holder) {
      isSurfaceAvailable = true;
    }

    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
      isSurfaceAvailable = false;
    }

    @Override
    public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
      WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
      int screenRotationOld = screenRotation;
      screenRotation = window.getDefaultDisplay().getRotation();
      if (screenRotation != screenRotationOld) {
        accelerationValues = null;
        updateOffset(true, null);
      } else {
        // Not necessarily needed but recommended
        drawFrame(true, null);
      }
    }

    @Override
    public WallpaperColors onComputeColors() {
      if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
        return wallpaperDrawable.getWallpaperColors(darkText, lightText, darkLauncher);
      } else {
        return super.onComputeColors();
      }
    }

    public boolean ping() {
      return true;
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
      isVisible = visible;
      if (!visible) {
        return;
      }

      if (isDark != isDarkMode()) {
        loadTheme();
      }

      isRtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

      updateOffset(true, null);
    }

    @Override
    public void onOffsetsChanged(
        float xOffset,
        float yOffset,
        float xStep,
        float yStep,
        int xPixels,
        int yPixels
    ) {
      if (isRtl && !isPreview()) {
        offsetX = xOffset - 1;
      } else {
        offsetX = xOffset;
      }
      if (animSwipe()) {
        updateOffset(true, null);
      }
    }

    @Override
    public Bundle onCommand(
        final String action, final int x, final int y, final int z, final Bundle extras,
        final boolean resultRequested
    ) {
      switch (action) {
        case WallpaperManager.COMMAND_DROP:
          iconDropConsumed = false;
          notifyIconDropped(x, y);
          break;
        case WallpaperManager.COMMAND_TAP:
        case "android.wallpaper.wakingup":
        case "android.wallpaper.goingtosleep":
        case "android.wallpaper.reapply":
          break;
      }
      return null;
    }

    private void notifyIconDropped(int x, int y) {
      if (!iconDropConsumed) {
        iconDropConsumed = true;
      }
    }

    private void loadSettings() {
      parallax = sharedPrefs.getInt(PREF.PARALLAX, DEF.PARALLAX);
      // disables zooming so this should not be disabled
      // setOffsetNotificationsEnabled(parallax != 0);

      isTiltEnabled = sharedPrefs.getBoolean(PREF.TILT, DEF.TILT);
      dampingTilt = sharedPrefs.getInt(PREF.DAMPING_TILT, DEF.DAMPING_TILT);
      dampingZoom = sharedPrefs.getInt(PREF.DAMPING_ZOOM, DEF.DAMPING_ZOOM);
      useZoomDamping = sharedPrefs.getBoolean(PREF.USE_ZOOM_DAMPING, DEF.USE_ZOOM_DAMPING);
      tiltThreshold = sharedPrefs.getInt(PREF.THRESHOLD, DEF.THRESHOLD);
      if (hasAccelerometer && isTiltEnabled && !isListenerRegistered) {
        sensorManager.registerListener(
            sensorListener,
            SensorUtil.getAccelerometer(context),
            // SENSOR_DELAY_GAME = 20000
            // SENSOR_DELAY_UI = 66667
            sharedPrefs.getInt(PREF.REFRESH_RATE, DEF.REFRESH_RATE)
        );
        isListenerRegistered = true;
      } else if (hasAccelerometer && isTiltEnabled) {
        sensorManager.unregisterListener(sensorListener);
        isListenerRegistered = false;
        sensorManager.registerListener(
            sensorListener,
            SensorUtil.getAccelerometer(context),
            // SENSOR_DELAY_GAME = 20000
            // SENSOR_DELAY_UI = 66667
            sharedPrefs.getInt(PREF.REFRESH_RATE, DEF.REFRESH_RATE)
        );
        isListenerRegistered = true;
      } else if (isListenerRegistered) {
        sensorManager.unregisterListener(sensorListener);
        isListenerRegistered = false;
      }

      loadDarkModeDependencies();
      if (wallpaperDrawable != null) {
        wallpaperDrawable.setScale(scale);
        wallpaperDrawable.setDimming(dimming / 10f);
        wallpaperDrawable.setStaticOffsetX(correction);
      }

      zoomIntensity = sharedPrefs.getInt(PREF.ZOOM_INTENSITY, DEF.ZOOM_INTENSITY);
      isZoomLauncherEnabled = sharedPrefs.getBoolean(PREF.ZOOM_LAUNCHER, DEF.ZOOM_LAUNCHER);
      isZoomUnlockEnabled = sharedPrefs.getBoolean(PREF.ZOOM_UNLOCK, DEF.ZOOM_UNLOCK);
      useSystemZoom = sharedPrefs.getBoolean(PREF.ZOOM_SYSTEM, DEF.ZOOM_SYSTEM);
      zoomDuration = sharedPrefs.getInt(PREF.ZOOM_DURATION, DEF.ZOOM_DURATION);

      powerSaveSwipe = sharedPrefs.getBoolean(PREF.POWER_SAVE_SWIPE, DEF.POWER_SAVE_SWIPE);
      powerSaveTilt = sharedPrefs.getBoolean(PREF.POWER_SAVE_TILT, DEF.POWER_SAVE_TILT);
      powerSaveZoom = sharedPrefs.getBoolean(PREF.POWER_SAVE_ZOOM, DEF.POWER_SAVE_ZOOM);
    }

    private void loadDarkModeDependencies() {
      darkMode = sharedPrefs.getInt(PREF.DARK_MODE, DEF.DARK_MODE);
      isDark = isDarkMode();
      String suffix = isDark ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;

      correction = sharedPrefs.getInt(PREF.STATIC_OFFSET + suffix, DEF.STATIC_OFFSET);
      dimming = sharedPrefs.getInt(PREF.DIMMING + suffix, DEF.DIMMING);
      darkText = sharedPrefs.getBoolean(PREF.USE_DARK_TEXT + suffix, DEF.USE_DARK_TEXT);
      lightText = sharedPrefs.getBoolean(PREF.FORCE_LIGHT_TEXT + suffix, DEF.FORCE_LIGHT_TEXT);
      darkLauncher = sharedPrefs.getBoolean(
          PREF.USE_DARK_LAUNCHER + suffix, DEF.USE_DARK_LAUNCHER
      );

      scale = sharedPrefs.getFloat(
          PREF.SCALE + suffix, WallpaperDrawable.getDefaultScale(context, isDarkMode())
      );
    }

    private void loadTheme() {
      loadDarkModeDependencies();
      loadWallpaper();
      if (wallpaperDrawable != null) {
        wallpaperDrawable.setScale(scale);
        wallpaperDrawable.setDimming(dimming / 10f);
        wallpaperDrawable.setStaticOffsetX(correction);
      }

      if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
        // NullPointerException on many devices!?
        try {
          notifyColorsChanged();
          if (VERSION.SDK_INT < VERSION_CODES.S) {
            notifyColorsChanged();
          }
        } catch (Exception e) {
          Log.e(TAG, "colorsHaveChanged", e);
        }
      }
    }

    private void loadWallpaper() {
      String suffix = isDark ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;

      String base64 = sharedPrefs.getString(PREF.WALLPAPER + suffix, DEF.WALLPAPER);
      if (base64 != null) {
        BitmapDrawable drawable = BitmapUtil.getBitmapDrawable(context, base64);
        wallpaperDrawable = new WallpaperDrawable(context, drawable);

        sharedPrefs.edit()
            .putInt(PREF.WALLPAPER_WIDTH + suffix, drawable.getIntrinsicWidth())
            .putInt(PREF.WALLPAPER_HEIGHT + suffix, drawable.getIntrinsicHeight())
            .apply();

        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
          Color primary = Color.valueOf(
              sharedPrefs.getInt(PREF.COLOR_PRIMARY + suffix, DEF.COLOR)
          );
          Color secondary = Color.valueOf(
              sharedPrefs.getInt(PREF.COLOR_SECONDARY + suffix, DEF.COLOR)
          );
          Color tertiary = Color.valueOf(
              sharedPrefs.getInt(PREF.COLOR_TERTIARY + suffix, DEF.COLOR)
          );

          WallpaperColors colors = new WallpaperColors(primary, secondary, tertiary);
          wallpaperDrawable.setWallpaperColors(colors);
        }
      }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void updateOffset(boolean force, String source) {
      float xOffset = parallax != 0 ? offsetX : 0;
      int tiltFactor = 18 * parallax * (isTiltEnabled ? 1 : 0);
      float finalTiltX, finalTiltY;
      switch (screenRotation) {
        case Surface.ROTATION_90:
          finalTiltX = tiltY;
          finalTiltY = -tiltX;
          break;
        case Surface.ROTATION_180:
          finalTiltX = -tiltX;
          finalTiltY = -tiltY;
          break;
        case Surface.ROTATION_270:
          finalTiltX = -tiltY;
          finalTiltY = tiltX;
          break;
        case Surface.ROTATION_0:
        default:
          finalTiltX = tiltX;
          finalTiltY = tiltY;
          break;
      }
      if (wallpaperDrawable != null) {
        wallpaperDrawable.setOffset(
            xOffset * parallax * 100 + finalTiltX * tiltFactor,
            finalTiltY * tiltFactor
        );
      }
      drawFrame(force, source);
    }

    /**
     * WallpaperService.Engine#shouldZoomOutWallpaper()
     */
    public boolean shouldZoomOutWallpaper() {
      // Return true and clear onZoomChanged if we don't want a custom zoom animation
      return isZoomLauncherEnabled && useSystemZoom && animZoom();
    }

    @Override
    public void onZoomChanged(float zoom) {
      if (!useSystemZoom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && zoomIntensity > 0 && animZoom()) {
          if (useZoomDamping) {
            zoomLauncher = lowPassZoom(zoom, zoomLauncher);
          } else {
            zoomLauncher = zoomInterpolator.getInterpolation(zoom);
          }
          drawFrame(false, REQUEST_SOURCE.ZOOM_LAUNCHER);
        }
      }
    }

    @Override
    public void onPresenceChange(String presence) {
      switch (presence) {
        case USER_PRESENCE.OFF:
          if (isZoomUnlockEnabled && animZoom()) {
            zoomUnlock = 1;
            zoomLauncher = 0; // 1 or 0?
          }
          if (isZoomUnlockEnabled && animZoom()) {
            drawFrame(true, null);
          }
          break;
        case USER_PRESENCE.LOCKED:
          if (isZoomUnlockEnabled && animZoom()) {
            zoomLauncher = 0;
            animateZoom(0.5f);
          }
          break;
        case USER_PRESENCE.UNLOCKED:
          if (isVisible && animZoom()) {
            animateZoom(0);
          } else {
            zoomUnlock = 0;
            drawFrame(true, null);
          }
          break;
      }
    }

    @Override
    public void onRefreshTheme() {
      loadTheme();
    }

    @Override
    public void onRefreshSettings() {
      loadSettings();
    }

    void drawFrame(boolean force, String source) {
      if (!isDrawingAllowed(force, source)) {
        // Cancel drawing request
        return;
      } else if (!isSurfaceAvailable || getSurfaceHolder().getSurface() == null) {
        // Cancel drawing request
        return;
      } else if (!getSurfaceHolder().getSurface().isValid()) {
        // Prevents IllegalStateException when surface is not ready
        return;
      }
      final SurfaceHolder surfaceHolder = getSurfaceHolder();
      Canvas canvas = null;
      try {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
          canvas = surfaceHolder.lockHardwareCanvas();
        } else {
          canvas = surfaceHolder.lockCanvas();
        }

        if (canvas != null) {
          canvas.drawColor(Color.BLACK);

          // ZOOM
          float intensity = zoomIntensity / 10f;
          double finalZoomLauncher = isZoomLauncherEnabled ? zoomLauncher * intensity : 0;
          double finalZoomUnlock = isZoomUnlockEnabled ? zoomUnlock * intensity : 0;

          if (wallpaperDrawable != null) {
            wallpaperDrawable.setZoom((float) (finalZoomLauncher + finalZoomUnlock));
            wallpaperDrawable.draw(canvas);
          }

          if (source != null) {
            switch (source) {
              case REQUEST_SOURCE.ZOOM_LAUNCHER:
                lastDrawZoomLauncher = SystemClock.elapsedRealtime();
                break;
              case REQUEST_SOURCE.ZOOM_UNLOCK:
                lastDrawZoomUnlock = SystemClock.elapsedRealtime();
                break;
              case REQUEST_SOURCE.TILT:
                lastDrawTilt = SystemClock.elapsedRealtime();
                break;
            }
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "drawFrame: unexpected exception", e);
      } finally {
        try {
          if (canvas != null && isSurfaceAvailable) {
            surfaceHolder.unlockCanvasAndPost(canvas);
          }
        } catch (Exception e) {
          Log.e(TAG, "drawFrame: unlocking canvas failed", e);
        }
      }
    }

    private boolean isDrawingAllowed(boolean force, String source) {
      if (force || zoomLauncher == 0 || zoomLauncher == 1 || zoomUnlock == 0 || zoomUnlock == 1) {
        return true;
      } else if (source != null) {
        if (source.equals(REQUEST_SOURCE.ZOOM_LAUNCHER)
            && SystemClock.elapsedRealtime() - lastDrawZoomLauncher < 1000 / fps) {
          return true;
        } else if (source.equals(REQUEST_SOURCE.ZOOM_UNLOCK)
            && SystemClock.elapsedRealtime() - lastDrawZoomUnlock < 1000 / fps) {
          return true;
        } else {
          return source.equals(REQUEST_SOURCE.TILT)
              && SystemClock.elapsedRealtime() - lastDrawTilt < 1000 / fps;
        }
      } else {
        return false;
      }
    }

    private float[] lowPassAcceleration(float[] input, float[] output) {
      if (output == null) {
        return input.clone();
      }
      for (int i = 0; i < input.length; i++) {
        output[i] = output[i] + (dampingTilt / 100f) * (input[i] - output[i]);
      }
      return output;
    }

    private float lowPassZoom(float input, float output) {
      return (output + (dampingZoom / 100f) * (input - output)) * input;
    }

    private void animateZoom(float valueTo) {
      if (zoomAnimator != null) {
        zoomAnimator.pause();
        zoomAnimator.cancel();
        zoomAnimator.removeAllUpdateListeners();
        zoomAnimator = null;
      }
      zoomAnimator = ValueAnimator.ofFloat(zoomUnlock, valueTo);
      zoomAnimator.addUpdateListener(animation -> {
        zoomUnlock = (float) animation.getAnimatedValue();
        drawFrame(false, REQUEST_SOURCE.ZOOM_UNLOCK);
      });
      zoomAnimator.setInterpolator(zoomInterpolator);
      zoomAnimator.setDuration(zoomDuration).start();
    }

    private boolean animSwipe() {
      return !(isPowerSaveMode && powerSaveSwipe);
    }

    private boolean animTilt() {
      return !(isPowerSaveMode && powerSaveTilt);
    }

    private boolean animZoom() {
      return !(isPowerSaveMode && powerSaveZoom);
    }
  }
}
