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

package com.patrickzedler.pallax.activity;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions.Builder;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import com.google.android.material.snackbar.Snackbar;
import com.patrickzedler.pallax.BuildConfig;
import com.patrickzedler.pallax.Constants;
import com.patrickzedler.pallax.Constants.ACTION;
import com.patrickzedler.pallax.Constants.DARK_MODE;
import com.patrickzedler.pallax.Constants.DEF;
import com.patrickzedler.pallax.Constants.EXTRA;
import com.patrickzedler.pallax.Constants.PREF;
import com.patrickzedler.pallax.Constants.THEME;
import com.patrickzedler.pallax.NavMainDirections;
import com.patrickzedler.pallax.R;
import com.patrickzedler.pallax.behavior.SystemBarBehavior;
import com.patrickzedler.pallax.databinding.ActivityMainBinding;
import com.patrickzedler.pallax.fragment.AppearanceFragment;
import com.patrickzedler.pallax.fragment.BaseFragment;
import com.patrickzedler.pallax.fragment.dialog.ApplyBottomSheetDialogFragment;
import com.patrickzedler.pallax.service.LiveWallpaperService;
import com.patrickzedler.pallax.util.BitmapUtil;
import com.patrickzedler.pallax.util.HapticUtil;
import com.patrickzedler.pallax.util.LocaleUtil;
import com.patrickzedler.pallax.util.PrefsUtil;
import com.patrickzedler.pallax.util.ResUtil;
import com.patrickzedler.pallax.util.SystemUiUtil;
import com.patrickzedler.pallax.util.ViewUtil;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  private ActivityMainBinding binding;
  private NavController navController;
  private SharedPreferences sharedPrefs;
  private ViewUtil viewUtil;
  private HapticUtil hapticUtil;
  private ActivityResultLauncher<Intent> wallpaperPickerLauncher;
  private ActivityResultLauncher<String> requestPermissionLauncher;
  private NavHostFragment navHost;
  private Locale localeUser;
  private boolean isServiceRunning;
  private int fabTopEdgeDistance;
  private int bottomInset;
  private boolean runAsSuperClass;

  @SuppressLint("AppBundleLocaleChanges")
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    runAsSuperClass = savedInstanceState != null
        && savedInstanceState.getBoolean(EXTRA.RUN_AS_SUPER_CLASS, false);

    if (runAsSuperClass) {
      super.onCreate(savedInstanceState);
      return;
    }

    sharedPrefs = new PrefsUtil(this).checkForMigrations().getSharedPrefs();

    // LANGUAGE

    localeUser = LocaleUtil.getUserLocale(this, sharedPrefs);
    Locale.setDefault(localeUser);

    // DARK MODE

    int modeDark = sharedPrefs.getInt(PREF.MODE, DEF.MODE);
    int uiMode = getResources().getConfiguration().uiMode;
    switch (modeDark) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        uiMode = Configuration.UI_MODE_NIGHT_NO;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        uiMode = Configuration.UI_MODE_NIGHT_YES;
        break;
    }
    AppCompatDelegate.setDefaultNightMode(modeDark);

    // APPLY CONFIG TO RESOURCES

    // base
    Resources resBase = getBaseContext().getResources();
    Configuration configBase = resBase.getConfiguration();
    configBase.setLocale(localeUser);
    configBase.uiMode = uiMode;
    resBase.updateConfiguration(configBase, resBase.getDisplayMetrics());
    // app
    Resources resApp = getApplicationContext().getResources();
    Configuration configApp = resApp.getConfiguration();
    configApp.setLocale(localeUser);
    // Don't set uiMode here, won't let FOLLOW_SYSTEM apply correctly
    resApp.updateConfiguration(configApp, getResources().getDisplayMetrics());

    switch (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
      case THEME.RED:
        setTheme(R.style.Theme_Pallax_Red);
        break;
      case THEME.YELLOW:
        setTheme(R.style.Theme_Pallax_Yellow);
        break;
      case THEME.LIME:
        setTheme(R.style.Theme_Pallax_Lime);
        break;
      case THEME.GREEN:
        setTheme(R.style.Theme_Pallax_Green);
        break;
      case THEME.TURQUOISE:
        setTheme(R.style.Theme_Pallax_Turquoise);
        break;
      case THEME.TEAL:
        setTheme(R.style.Theme_Pallax_Teal);
        break;
      case THEME.BLUE:
        setTheme(R.style.Theme_Pallax_Blue);
        break;
      case THEME.PURPLE:
        setTheme(R.style.Theme_Pallax_Purple);
        break;
      case THEME.AMOLED:
        setTheme(R.style.Theme_Pallax_Amoled);
        break;
      default:
        if (DynamicColors.isDynamicColorAvailable()) {
          DynamicColors.applyToActivityIfAvailable(
              this,
              new Builder().setOnAppliedCallback(
                  activity -> HarmonizedColors.applyToContextIfAvailable(
                      this, HarmonizedColorsOptions.createMaterialDefaults()
                  )
              ).build()
          );
        } else {
          setTheme(R.style.Theme_Pallax_Blue);
        }
        break;
    }

    Bundle bundleInstanceState = getIntent().getBundleExtra(EXTRA.INSTANCE_STATE);
    super.onCreate(bundleInstanceState != null ? bundleInstanceState : savedInstanceState);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    viewUtil = new ViewUtil();
    hapticUtil = new HapticUtil(binding.getRoot());

    wallpaperPickerLauncher = registerForActivityResult(
        new StartActivityForResult(),
        result -> setFabVisibility(
            result.getResultCode() != Activity.RESULT_OK, true
        )
    );

    requestPermissionLauncher = registerForActivityResult(
        new RequestPermission(), isGranted -> {
          if (isGranted) {
            showSnackbar(getSnackbar(R.string.msg_permission_granted, Snackbar.LENGTH_SHORT));
          } else {
            Snackbar snackbar = getSnackbar(R.string.msg_permission_denied, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.action_retry, v -> {
              if (VERSION.SDK_INT >= VERSION_CODES.M
                  && shouldShowRequestPermissionRationale(permission.READ_EXTERNAL_STORAGE)) {
                navigate(NavMainDirections.actionGlobalPermissionDialog());
              } else {
                requestPermission();
              }
            });
            showSnackbar(snackbar);
          }
        });

    // Calculate FAB top edge distance to bottom (excluding bottom inset)
    int height = SystemUiUtil.dpToPx(this, 32) + SystemUiUtil.spToPx(
        this, 16
    );
    int margin = SystemUiUtil.dpToPx(this, 40);
    fabTopEdgeDistance = height + margin;

    navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(
        R.id.fragment_main_nav_host
    );
    assert navHost != null;
    navController = navHost.getNavController();

    SystemBarBehavior.applyBottomInset(binding.fabMain);

    isServiceRunning = LiveWallpaperService.isMainEngineRunning();
    if (isServiceRunning) {
      binding.fabMain.setVisibility(View.INVISIBLE);
    }

    if (getIntent() != null) {
      if (getIntent().getBooleanExtra(EXTRA.SHOW_FORCE_STOP_REQUEST, false)) {
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> showForceStopRequest(null), 200
        );
      }
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
      bottomInset = insets.getInsets(Type.systemBars()).bottom;
      setFabVisibility(!isServiceRunning, false);
      return insets;
    });

    binding.fabMain.setRippleColor(
        ColorStateList.valueOf(
            ResUtil.getColorAttr(this, R.attr.colorOnPrimaryContainer, 0.07f)
        )
    );

    binding.fabMain.setOnClickListener(v -> {
      if (viewUtil.isClickEnabled()) {
        performHapticHeavyClick();
        String base64 = sharedPrefs.getString(PREF.WALLPAPER + getDarkSuffix(), null);
        if (base64 != null) {
          // Not working correctly, same wallpaper seems to return different base64
          /*if (ActivityCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED) {
            WallpaperManager manager = WallpaperManager.getInstance(this);
            if (base64.equals(BitmapUtil.getBase64((BitmapDrawable) manager.getDrawable()))) {
              setWallpaper(false);
              return;
            }
          }*/
          navigate(NavMainDirections.actionGlobalOverwriteDialog());
        } else {
          setWallpaper(true);
        }
      }
    });

    if (savedInstanceState == null && bundleInstanceState == null) {
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showInitialBottomSheets, VERSION.SDK_INT >= 31 ? 950 : 0
      );
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (runAsSuperClass) {
      return;
    }

    boolean isServiceRunningNew = LiveWallpaperService.isMainEngineRunning();
    if (isServiceRunning != isServiceRunningNew) {
      isServiceRunning = isServiceRunningNew;
      setFabVisibility(!isServiceRunning, true);
    }

    hapticUtil.setEnabled(HapticUtil.areSystemHapticsTurnedOn(this));
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (runAsSuperClass) {
      super.attachBaseContext(base);
    } else {
      SharedPreferences sharedPrefs = new PrefsUtil(base).checkForMigrations().getSharedPrefs();
      // Language
      Locale userLocale = LocaleUtil.getUserLocale(base, sharedPrefs);
      Locale.setDefault(userLocale);
      // Dark mode
      int modeDark = sharedPrefs.getInt(PREF.MODE, DEF.MODE);
      int uiMode = base.getResources().getConfiguration().uiMode;
      switch (modeDark) {
        case AppCompatDelegate.MODE_NIGHT_NO:
          uiMode = Configuration.UI_MODE_NIGHT_NO;
          break;
        case AppCompatDelegate.MODE_NIGHT_YES:
          uiMode = Configuration.UI_MODE_NIGHT_YES;
          break;
      }
      AppCompatDelegate.setDefaultNightMode(modeDark);
      // Apply config to resources
      Resources resources = base.getResources();
      Configuration config = resources.getConfiguration();
      config.setLocale(userLocale);
      config.uiMode = uiMode;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
      super.attachBaseContext(base.createConfigurationContext(config));
    }
  }

  @Override
  public void applyOverrideConfiguration(Configuration overrideConfiguration) {
    if (!runAsSuperClass && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      overrideConfiguration.setLocale(LocaleUtil.getUserLocale(this, sharedPrefs));
    }
    super.applyOverrideConfiguration(overrideConfiguration);
  }

  @NonNull
  public BaseFragment getCurrentFragment() {
    return (BaseFragment) navHost.getChildFragmentManager().getFragments().get(0);
  }

  public void requestPermission() {
    requestPermissionLauncher.launch(permission.READ_EXTERNAL_STORAGE);
  }

  public void setWallpaper(boolean shouldSave) {
    if (shouldSave) {
      if (ContextCompat.checkSelfPermission(this,
          permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
      ) {
        saveWallpaper();
      } else {
        navigate(NavMainDirections.actionGlobalPermissionDialog());
        return;
      }
    }
    try {
      Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
      intent.putExtra(
          WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
          new ComponentName(getPackageName(), LiveWallpaperService.class.getCanonicalName())
      );
      intent.putExtra("SET_LOCKSCREEN_WALLPAPER", true);
      wallpaperPickerLauncher.launch(intent);
    } catch (ActivityNotFoundException e) {
      showSnackbar(R.string.msg_preview_missing);
    }
  }

  private void saveWallpaper() {
    if (ActivityCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_DENIED) {
      return;
    }
    WallpaperManager manager = WallpaperManager.getInstance(this);
    Drawable drawable = manager.getDrawable();
    if (drawable != null) {
      boolean isDarkMode = isWallpaperDarkMode();
      String suffix = isDarkMode ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;

      SharedPreferences.Editor editor = sharedPrefs.edit();
      editor.putString(PREF.WALLPAPER + suffix, BitmapUtil.getBase64((BitmapDrawable) drawable));

      BaseFragment current = getCurrentFragment();
      if (current instanceof AppearanceFragment) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
          ((AppearanceFragment) current).loadPreview(isDarkMode);
          ((AppearanceFragment) current).updatePreview(isDarkMode);
        }, 1000);
      }

      if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
        WallpaperColors colors = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        editor.putInt(PREF.COLOR_PRIMARY + suffix, colors.getPrimaryColor().toArgb());
        if (colors.getSecondaryColor() != null) {
          editor.putInt(PREF.COLOR_SECONDARY + suffix, colors.getSecondaryColor().toArgb());
        } else {
          editor.remove(PREF.COLOR_SECONDARY + suffix);
        }
        if (colors.getTertiaryColor() != null) {
          editor.putInt(PREF.COLOR_TERTIARY + suffix, colors.getTertiaryColor().toArgb());
        } else {
          editor.remove(PREF.COLOR_TERTIARY + suffix);
        }
      }
      editor.apply();
    }
  }

  public int getFabTopEdgeDistance() {
    return binding.fabMain.getTranslationY() == 0
        && binding.fabMain.getVisibility() == View.VISIBLE ? fabTopEdgeDistance : 0;
  }

  private void setFabVisibility(boolean visible, boolean animated) {
    if (binding == null) {
      return;
    }
    binding.fabMain.setVisibility(View.VISIBLE);
    if (animated) {
      binding.fabMain.animate()
          .translationY(visible ? 0 : fabTopEdgeDistance + bottomInset)
          .setDuration(400)
          .setStartDelay(200)
          .setInterpolator(new FastOutSlowInInterpolator())
          .start();
    } else {
      binding.fabMain.setTranslationY(visible ? 0 : fabTopEdgeDistance + bottomInset);
    }
  }

  public boolean shouldLogoBeVisibleOnOverviewPage() {
    return true;
  }

  public void showSnackbar(@StringRes int resId) {
    showSnackbar(
        Snackbar.make(binding.coordinatorMain, getString(resId), Snackbar.LENGTH_LONG)
    );
  }

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.fabMain).show();
  }

  public Snackbar getSnackbar(@StringRes int resId, int duration) {
    return Snackbar.make(binding.coordinatorMain, getString(resId), duration);
  }

  public void navigate(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigateUp() {
    if (navController != null) {
      navController.navigateUp();
    } else {
      Log.e(TAG, "navigateUp: controller is null");
    }
  }

  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  public Locale getLocale() {
    return localeUser != null ? localeUser : Locale.getDefault();
  }

  public void requestSettingsRefresh() {
    Intent intent = new Intent();
    intent.setAction(ACTION.SETTINGS_CHANGED);
    sendBroadcast(intent);
  }

  public void requestThemeRefresh() {
    Intent intent = new Intent();
    intent.setAction(ACTION.THEME_CHANGED);
    sendBroadcast(intent);
  }

  public boolean isWallpaperDarkMode() {
    int darkMode = getSharedPrefs().getInt(PREF.DARK_MODE, DEF.DARK_MODE);
    if (darkMode == DARK_MODE.ON) {
      return true;
    }
    int flags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return darkMode == DARK_MODE.AUTO && flags == Configuration.UI_MODE_NIGHT_YES;
  }

  public String getDarkSuffix() {
    return isWallpaperDarkMode() ? Constants.SUFFIX_DARK : Constants.SUFFIX_LIGHT;
  }

  public void reset() {
    sharedPrefs.edit().clear().apply();
    requestSettingsRefresh();
    requestThemeRefresh();

    boolean isLauncherIconDisabled = getPackageManager().getComponentEnabledSetting(
        new ComponentName(this, LauncherActivity.class)
    ) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    if (isLauncherIconDisabled) {
      getPackageManager().setComponentEnabledSetting(
          new ComponentName(this, LauncherActivity.class),
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
          0
      );
    }
  }

  public void restartToApply(long delay) {
    restartToApply(delay, new Bundle(), false, true);
  }

  public void restartToApply(
      long delay, @NonNull Bundle bundle, boolean showForceStopRequest, boolean restoreState
  ) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      if (restoreState) {
        onSaveInstanceState(bundle);
      }
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        finish();
      }
      Intent intent = new Intent(this, MainActivity.class);
      if (restoreState) {
        intent.putExtra(EXTRA.INSTANCE_STATE, bundle);
      }
      if (showForceStopRequest) {
        intent.putExtra(EXTRA.SHOW_FORCE_STOP_REQUEST, true);
      }
      startActivity(intent);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        finish();
      }
      overridePendingTransition(R.anim.fade_in_restart, R.anim.fade_out_restart);
    }, delay);
  }

  public void showForceStopRequest(NavDirections directions) {
    if (!LiveWallpaperService.isMainEngineRunning() || binding == null) {
      return;
    }
    showSnackbar(
        getSnackbar(
            R.string.msg_force_stop, Snackbar.LENGTH_LONG
        ).setAction(
            getString(R.string.action_continue), view -> {
              performHapticHeavyClick();
              if (directions != null && navController != null) {
                navigate(directions);
              } else {
                ViewUtil.showBottomSheet(this, new ApplyBottomSheetDialogFragment());
              }
            }
        )
    );
  }

  private void showInitialBottomSheets() {
    // Changelog
    int versionNew = BuildConfig.VERSION_CODE;
    int versionOld = sharedPrefs.getInt(PREF.LAST_VERSION, 0);
    if (versionOld == 0) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
    } else if (versionOld != versionNew) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
      showChangelogBottomSheet();
    }

    // Feedback
    int feedbackCount = sharedPrefs.getInt(PREF.FEEDBACK_POP_UP_COUNT, 1);
    if (feedbackCount > 0) {
      if (feedbackCount < 5) {
        sharedPrefs.edit().putInt(PREF.FEEDBACK_POP_UP_COUNT, feedbackCount + 1).apply();
      } else {
        showFeedbackBottomSheet();
      }
    }
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title) {
    showTextBottomSheet(file, title, 0);
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    NavMainDirections.ActionGlobalTextDialog action
        = NavMainDirections.actionGlobalTextDialog();
    action.setTitle(title);
    action.setFile(file);
    if (link != 0) {
      action.setLink(link);
    }
    navigate(action);
  }

  public void showFeedbackBottomSheet() {
    navigate(NavMainDirections.actionGlobalFeedbackDialog());
  }

  public void showChangelogBottomSheet() {
    navigate(NavMainDirections.actionGlobalChangelogDialog());
  }

  public boolean isTouchWiz() {
    PackageManager localPackageManager = getPackageManager();
    Intent intent = new Intent("android.intent.action.MAIN");
    intent.addCategory("android.intent.category.HOME");
    String launcher = localPackageManager.resolveActivity(
        intent, PackageManager.MATCH_DEFAULT_ONLY
    ).activityInfo.packageName;
    return launcher != null && launcher.equals("com.sec.android.app.launcher");
  }

  public void performHapticClick() {
    hapticUtil.click();
  }

  public void performHapticHeavyClick() {
    hapticUtil.heavyClick();
  }
}
