package com.ltm.weather;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class SettingsActivity extends Activity {
    private LinearLayout content;
    private boolean dark;
    private int bgColor;
    private int cardColor;
    private int textColor;
    private int subTextColor;
    private int borderColor;
    private EditText cityInput;
    private LinearLayout widgetPreviewCanvas;
    private LinearLayout widgetPreviewRoot;
    private TextView widgetPreviewTime;
    private TextView widgetPreviewDate;
    private TextView widgetPreviewHint;
    private TextView widgetPreviewCity;
    private TextView widgetPreviewTemp;
    private TextView widgetPreviewCondition;
    private TextView widgetPreviewDayNight;
    private TextView widgetPreviewIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = WeatherRepository.isDarkTheme(this);
        if (dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        initColors();
        applySystemBars();
        buildUi();
    }

    private void initColors() {
        bgColor = dark ? Color.rgb(2, 6, 23) : Color.rgb(239, 246, 255);
        cardColor = dark ? Color.rgb(15, 23, 42) : Color.argb(242, 255, 255, 255);
        textColor = dark ? Color.rgb(248, 250, 252) : Color.rgb(15, 23, 42);
        subTextColor = dark ? Color.rgb(203, 213, 225) : Color.rgb(100, 116, 139);
        borderColor = dark ? Color.rgb(38, 52, 78) : Color.rgb(207, 226, 255);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12) + statusBarHeight(), dp(12), dp(18) + navigationBarHeight());
        scroll.addView(content);
        setContentView(scroll);

        LinearLayout header = row();
        TextView title = text(tr("settings"), 24, true, textColor);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(smallButton(tr("back"), false, v -> finish()), new LinearLayout.LayoutParams(dp(82), dp(38)));
        content.addView(header);

        content.addView(card(languageCard()));
        content.addView(card(citiesCard()));
        content.addView(card(themeCard()));
        content.addView(card(unitsCard()));
        content.addView(card(widgetCard()));
        content.addView(card(lockScreenCard()));
        content.addView(card(rainCard()));
    }

    private LinearLayout citiesCard() {
        LinearLayout box = column();
        box.addView(text(tr("places"), 17, true, textColor));
        WeatherRepository.SavedLocation current = WeatherRepository.getSavedLocation(this);
        box.addView(text(tr("current") + WeatherRepository.displayLocationName(current.name), 12, false, subTextColor));
        cityInput = new EditText(this);
        cityInput.setSingleLine(true);
        cityInput.setHint(tr("enter_city"));
        cityInput.setTextColor(textColor);
        cityInput.setHintTextColor(subTextColor);
        cityInput.setTextSize(14);
        box.addView(cityInput, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        LinearLayout searchRow = row();
        searchRow.addView(smallButton(tr("find_select"), true, v -> searchAndSelectCity()), new LinearLayout.LayoutParams(0, dp(38), 1));
        box.addView(searchRow, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 6, 0, 0));
        box.addView(text(tr("quick_pick"), 13, true, subTextColor), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 10, 0, 0));
        List<WeatherRepository.SavedLocation> savedCities = WeatherRepository.getSavedCities(this);
        Button quickPick = smallButton(shortLocation(current.name) + " ▾", false, v -> showCitiesMenu(v, savedCities));
        quickPick.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        box.addView(quickPick, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 5, 0, 0));
        box.addView(smallButton(tr("clear_saved"), false, v -> {
            WeatherRepository.clearSavedCities(this);
            Toast.makeText(this, tr("list_cleared"), Toast.LENGTH_SHORT).show();
            rebuild();
        }), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 10, 0, 0));
        return box;
    }

    private LinearLayout languageCard() {
        LinearLayout box = column();
        box.addView(text(tr("language"), 17, true, textColor));
        box.addView(text(tr("language_hint"), 12, false, subTextColor));
        String current = WeatherRepository.getLanguage(this);
        Button language = trayButton(L10n.languageName(current), v -> showLanguageMenu(v));
        box.addView(language, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)), 0, 8, 0, 0));
        return box;
    }

    private void searchAndSelectCity() {
        String query = cityInput == null ? "" : cityInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, tr("enter_city"), Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard(cityInput);
        Toast.makeText(this, tr("searching_city"), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                WeatherRepository.CityResult result = WeatherRepository.findCity(query);
                WeatherRepository.SavedLocation saved = new WeatherRepository.SavedLocation(result.name, result.latitude, result.longitude);
                WeatherRepository.saveLocation(this, saved);
                WeatherWidgetUpdater.refreshAndUpdate(this);
                runOnUiThread(() -> {
                    Toast.makeText(this, tr("selected") + WeatherRepository.displayLocationName(saved.name), Toast.LENGTH_SHORT).show();
                    rebuild();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, tr("city_not_found") + shortError(e), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void selectSavedCity(WeatherRepository.SavedLocation city) {
        WeatherRepository.saveLocation(this, city);
        WeatherWidgetUpdater.refreshAndUpdate(this);
        Toast.makeText(this, tr("selected") + WeatherRepository.displayLocationName(city.name), Toast.LENGTH_SHORT).show();
        rebuild();
    }

    private LinearLayout themeCard() {
        LinearLayout box = column();
        box.addView(text(tr("app_theme"), 17, true, textColor));
        box.addView(trayButton(themeLabel(WeatherRepository.getAppThemeMode(this)), v -> showThemeMenu(v)), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)), 0, 8, 0, 0));
        return box;
    }

    private LinearLayout unitsCard() {
        LinearLayout box = column();
        box.addView(text(tr("units"), 17, true, textColor));
        box.addView(text(tr("temperature"), 13, true, subTextColor), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        box.addView(trayButton(WeatherRepository.getTempUnit(this).equals(WeatherRepository.TEMP_F) ? "°F" : "°C", v -> showTempMenu(v)), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
        box.addView(text(tr("wind"), 13, true, subTextColor), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        box.addView(trayButton(WeatherRepository.WIND_MS.equals(WeatherRepository.getWindUnit(this)) ? "м/с" : "км/ч", v -> showWindMenu(v)), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
        box.addView(text(tr("pressure"), 13, true, subTextColor), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        box.addView(trayButton(WeatherRepository.PRESSURE_MMHG.equals(WeatherRepository.getPressureUnit(this)) ? "мм рт." : "гПа", v -> showPressureMenu(v)), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
        return box;
    }

    private LinearLayout widgetCard() {
        LinearLayout box = column();
        box.addView(text(tr("widgets"), 17, true, textColor));
        box.addView(widgetPreviewSection(), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 4));
        int current = WeatherRepository.getWidgetTransparency(this);
        TextView alphaLabel = text(tr("opacity") + current + "%", 14, true, subTextColor);
        box.addView(alphaLabel, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        SeekBar alpha = new SeekBar(this);
        alpha.setMax(100);
        alpha.setProgress(current);
        alpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                WeatherRepository.setWidgetTransparency(SettingsActivity.this, progress);
                alphaLabel.setText(tr("opacity") + progress + "%");
                WeatherWidgetUpdater.updateAllWidgets(SettingsActivity.this);
                updateWidgetPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(alpha, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        box.addView(text(tr("info_mode"), 13, true, subTextColor));
        String mode = WeatherRepository.getWidgetMode(this);
        box.addView(trayButton(modeLabel(mode), v -> showModeMenu(v)), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 6, 0, 0));

        int clockScale = WeatherRepository.getWidgetClockScale(this);
        TextView clockLabel = text(tr("clock_size") + clockScale + "%", 13, true, subTextColor);
        box.addView(clockLabel, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        SeekBar clock = new SeekBar(this);
        clock.setMax(100);
        clock.setProgress(clockScale - 70);
        clock.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 70;
                WeatherRepository.setWidgetClockScale(SettingsActivity.this, value);
                clockLabel.setText(tr("clock_size") + value + "%");
                WeatherWidgetUpdater.updateAllWidgets(SettingsActivity.this);
                updateWidgetPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(clock, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        int weatherScale = WeatherRepository.getWidgetWeatherScale(this);
        TextView weatherLabel = text(tr("weather_size") + weatherScale + "%", 13, true, subTextColor);
        box.addView(weatherLabel, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 8, 0, 0));
        SeekBar weather = new SeekBar(this);
        weather.setMax(140);
        weather.setProgress(weatherScale - 80);
        weather.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 80;
                WeatherRepository.setWidgetWeatherScale(SettingsActivity.this, value);
                weatherLabel.setText(tr("weather_size") + value + "%");
                WeatherWidgetUpdater.updateAllWidgets(SettingsActivity.this);
                updateWidgetPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(weather, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        box.addView(trayButton(themeLabel(WeatherRepository.getWidgetThemeMode(this)), v -> showWidgetColorMenu(v)), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 8, 0, 0));

        box.addView(text(tr("weather_refresh_interval"), 13, true, subTextColor), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 10, 0, 0));
        box.addView(text(tr("weather_refresh_hint"), 12, false, subTextColor));
        int refresh = WeatherRepository.getWeatherRefreshInterval(this);
        box.addView(trayButton(L10n.intervalLabel(this, refresh), v -> showRefreshIntervalMenu(v)), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 6, 0, 0));
        return box;
    }

    private LinearLayout widgetPreviewSection() {
        LinearLayout box = column();
        box.addView(text(tr("preview"), 13, true, subTextColor));
        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(this);

        widgetPreviewCanvas = column();
        widgetPreviewCanvas.setPadding(0, 0, 0, 0);
        widgetPreviewCanvas.setMinimumHeight(dp(128));
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        canvasParams.setMargins(0, dp(8), 0, 0);
        widgetPreviewCanvas.setLayoutParams(canvasParams);

        widgetPreviewRoot = row();
        widgetPreviewRoot.setPadding(dp(12), dp(10), dp(12), dp(10));
        widgetPreviewRoot.setMinimumHeight(dp(112));

        LinearLayout left = column();
        left.setGravity(Gravity.CENTER_VERTICAL);
        widgetPreviewTime = text("13:07", 36, true, widgetPreviewTextColor());
        widgetPreviewTime.setSingleLine(true);
        widgetPreviewDate = text(new java.text.SimpleDateFormat("EEE, d MMM", L10n.locale(this)).format(new java.util.Date()), 10, false, widgetPreviewSubTextColor());
        widgetPreviewHint = text(tr("alarm_hint"), 9, false, widgetPreviewSubTextColor());
        left.addView(widgetPreviewTime);
        left.addView(widgetPreviewDate);
        left.addView(widgetPreviewHint);
        widgetPreviewRoot.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f));

        LinearLayout right = column();
        right.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        widgetPreviewCity = text(WeatherWidgetUpdater.formatWidgetLocation(cache.city), 10, false, widgetPreviewSubTextColor());
        widgetPreviewCity.setGravity(Gravity.RIGHT);
        widgetPreviewCity.setSingleLine(true);
        widgetPreviewIcon = text(WeatherWidgetUpdater.shortCondition(cache.condition).contains("дожд") ? "☔" : WeatherWidgetUpdater.shortCondition(cache.condition).contains("снег") ? "❄" : (cache.isDay == 0 ? (cache.moonIcon == null || cache.moonIcon.trim().isEmpty() ? "🌙" : cache.moonIcon) : "☀"), 24, false, widgetPreviewTextColor());
        widgetPreviewIcon.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        widgetPreviewTemp = text(cache.temp, 34, true, widgetPreviewTextColor());
        widgetPreviewTemp.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout weatherLine = row();
        weatherLine.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        weatherLine.addView(widgetPreviewIcon);
        LinearLayout.LayoutParams tempParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tempParams.setMargins(dp(5), 0, 0, 0);
        weatherLine.addView(widgetPreviewTemp, tempParams);
        widgetPreviewCondition = text(WeatherWidgetUpdater.shortCondition(cache.condition), 10, false, widgetPreviewTextColor());
        widgetPreviewCondition.setGravity(Gravity.RIGHT);
        String dayNight = cache.todayDayNight == null || cache.todayDayNight.trim().isEmpty() ? "День 18° · Ночь 10°" : cache.todayDayNight.replace("Дн.", "День").replace("Ноч.", "Ночь").replace(": ", " ");
        widgetPreviewDayNight = text(dayNight, 9, false, widgetPreviewSubTextColor());
        widgetPreviewDayNight.setGravity(Gravity.RIGHT);
        right.addView(widgetPreviewCity);
        right.addView(weatherLine);
        right.addView(widgetPreviewCondition);
        right.addView(widgetPreviewDayNight);
        widgetPreviewRoot.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.92f));

        widgetPreviewCanvas.addView(widgetPreviewRoot);
        box.addView(widgetPreviewCanvas);
        updateWidgetPreview();
        return box;
    }

    private void updateWidgetPreview() {
        boolean widgetDark = WeatherRepository.isWidgetDark(this);
        int opacity = WeatherRepository.getWidgetTransparency(this);
        int clockScale = WeatherRepository.getWidgetClockScale(this);
        int weatherScale = WeatherRepository.getWidgetWeatherScale(this);
        String mode = WeatherRepository.getWidgetMode(this);
        if (widgetPreviewCanvas != null) widgetPreviewCanvas.setBackgroundColor(Color.TRANSPARENT);
        if (widgetPreviewRoot != null) widgetPreviewRoot.setBackground(round(WeatherWidgetUpdater.widgetBackgroundColor(widgetDark, opacity), dp(22), Color.TRANSPARENT));
        if (widgetPreviewTime != null) {
            int base = WeatherRepository.WIDGET_MODE_COMPACT.equals(mode) ? 42 : WeatherRepository.WIDGET_MODE_DETAILED.equals(mode) ? 58 : 52;
            widgetPreviewTime.setTextColor(widgetPreviewTextColor());
            widgetPreviewTime.setTextSize(Math.max(26, Math.min(74, Math.round(base * Math.max(0.70f, clockScale / 100f)))));
        }
        if (widgetPreviewDate != null) widgetPreviewDate.setTextColor(widgetPreviewSubTextColor());
        if (widgetPreviewHint != null) widgetPreviewHint.setTextColor(widgetPreviewSubTextColor());
        if (widgetPreviewCity != null) widgetPreviewCity.setTextColor(widgetPreviewSubTextColor());
        float weatherMul = Math.max(0.80f, Math.min(2.00f, weatherScale / 100f));
        if (widgetPreviewIcon != null) {
            widgetPreviewIcon.setTextColor(widgetPreviewTextColor());
            widgetPreviewIcon.setTextSize(Math.max(28, Math.min(68, Math.round(38 * weatherMul))));
        }
        if (widgetPreviewTemp != null) {
            widgetPreviewTemp.setTextColor(widgetPreviewTextColor());
            widgetPreviewTemp.setTextSize(Math.max(24, Math.min(68, Math.round(34 * weatherMul))));
        }
        if (widgetPreviewCondition != null) {
            widgetPreviewCondition.setTextColor(widgetPreviewTextColor());
            widgetPreviewCondition.setTextSize(Math.max(9, Math.min(13, Math.round(10 * weatherMul))));
        }
        if (widgetPreviewDayNight != null) {
            widgetPreviewDayNight.setTextColor(widgetPreviewSubTextColor());
            widgetPreviewDayNight.setTextSize(Math.max(8, Math.min(12, Math.round(9 * weatherMul))));
        }
    }

    private int widgetPreviewTextColor() { return WeatherRepository.isWidgetDark(this) ? Color.WHITE : Color.rgb(15, 23, 42); }
    private int widgetPreviewSubTextColor() { return WeatherRepository.isWidgetDark(this) ? Color.rgb(226, 232, 240) : Color.rgb(71, 85, 105); }
    private int widgetPreviewStrokeColor(boolean widgetDark) { return widgetDark ? Color.argb(150, 226, 232, 240) : Color.argb(150, 15, 23, 42); }

    private LinearLayout lockScreenCard() {
        LinearLayout box = column();
        box.addView(text(tr("lockscreen_weather"), 17, true, textColor));
        box.addView(text(tr("lockscreen_weather_hint"), 12, false, subTextColor));
        boolean enabled = WeatherRepository.lockScreenWeatherEnabled(this);
        box.addView(smallButton(enabled ? tr("enabled") : tr("disabled"), enabled, v -> toggleLockScreenWeather()), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 8, 0, 0));
        return box;
    }

    private LinearLayout rainCard() {
        LinearLayout box = column();
        box.addView(text(tr("rain_alerts"), 17, true, textColor));
        boolean enabled = WeatherRepository.rainAlertsEnabled(this);
        box.addView(smallButton(enabled ? tr("enabled") : tr("disabled"), enabled, v -> toggleRainAlerts()), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 8, 0, 0));
        int threshold = WeatherRepository.getRainThreshold(this);
        box.addView(trayButton(threshold + "%", v -> showRainThresholdMenu(v)), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 8, 0, 0));
        return box;
    }


    private void showLanguageMenu(View anchor) {
        String[] values = {L10n.LANG_RU, L10n.LANG_BE, L10n.LANG_EN, L10n.LANG_ZH, L10n.LANG_JA, L10n.LANG_KO, L10n.LANG_VI};
        String[] labels = new String[values.length];
        Runnable[] actions = new Runnable[values.length];
        for (int i = 0; i < values.length; i++) {
            final String value = values[i];
            labels[i] = L10n.languageName(value);
            actions[i] = () -> setLanguage(value);
        }
        showMenu(anchor, labels, actions);
    }

    private void showCitiesMenu(View anchor, List<WeatherRepository.SavedLocation> cities) {
        String[] labels = new String[cities.size()];
        Runnable[] actions = new Runnable[cities.size()];
        for (int i = 0; i < cities.size(); i++) {
            final WeatherRepository.SavedLocation city = cities.get(i);
            labels[i] = shortLocation(city.name);
            actions[i] = () -> selectSavedCity(city);
        }
        showMenu(anchor, labels, actions);
    }

    private void showThemeMenu(View anchor) {
        showMenu(anchor,
                new String[]{tr("follow_system"), tr("light"), tr("dark")},
                new Runnable[]{() -> setAppThemeMode(WeatherRepository.THEME_SYSTEM), () -> setAppThemeMode(WeatherRepository.THEME_LIGHT), () -> setAppThemeMode(WeatherRepository.THEME_DARK)});
    }

    private void showTempMenu(View anchor) {
        showMenu(anchor, new String[]{"°C", "°F"}, new Runnable[]{() -> setTemp(WeatherRepository.TEMP_C), () -> setTemp(WeatherRepository.TEMP_F)});
    }

    private void showWindMenu(View anchor) {
        showMenu(anchor, new String[]{"км/ч", "м/с"}, new Runnable[]{() -> setWind(WeatherRepository.WIND_KMH), () -> setWind(WeatherRepository.WIND_MS)});
    }

    private void showPressureMenu(View anchor) {
        showMenu(anchor, new String[]{"гПа", "мм рт."}, new Runnable[]{() -> setPressure(WeatherRepository.PRESSURE_HPA), () -> setPressure(WeatherRepository.PRESSURE_MMHG)});
    }

    private void showModeMenu(View anchor) {
        showMenu(anchor,
                new String[]{tr("compact"), tr("normal"), tr("detailed")},
                new Runnable[]{() -> setMode(WeatherRepository.WIDGET_MODE_COMPACT), () -> setMode(WeatherRepository.WIDGET_MODE_NORMAL), () -> setMode(WeatherRepository.WIDGET_MODE_DETAILED)});
    }

    private void showWidgetColorMenu(View anchor) {
        showMenu(anchor,
                new String[]{tr("follow_system"), tr("light"), tr("dark")},
                new Runnable[]{() -> setWidgetThemeMode(WeatherRepository.THEME_SYSTEM), () -> setWidgetThemeMode(WeatherRepository.THEME_LIGHT), () -> setWidgetThemeMode(WeatherRepository.THEME_DARK)});
    }

    private void showRefreshIntervalMenu(View anchor) {
        int[] values = {15, 30, 60, 180, 360, 720};
        String[] labels = new String[values.length];
        Runnable[] actions = new Runnable[values.length];
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            labels[i] = L10n.intervalLabel(this, value);
            actions[i] = () -> setWeatherRefreshInterval(value);
        }
        showMenu(anchor, labels, actions);
    }

    private void showRainThresholdMenu(View anchor) {
        showMenu(anchor, new String[]{"35%", "55%", "75%"}, new Runnable[]{() -> setThreshold(35), () -> setThreshold(55), () -> setThreshold(75)});
    }

    private void showMenu(View anchor, String[] labels, Runnable[] actions) {
        PopupMenu menu = new PopupMenu(this, anchor);
        for (int i = 0; i < labels.length; i++) menu.getMenu().add(0, i, i, labels[i]);
        menu.setOnMenuItemClickListener(item -> {
            int index = item.getItemId();
            if (index >= 0 && index < actions.length) actions[index].run();
            return true;
        });
        menu.show();
    }

    private String themeLabel(String mode) {
        if (WeatherRepository.THEME_LIGHT.equals(mode)) return tr("light");
        if (WeatherRepository.THEME_DARK.equals(mode)) return tr("dark");
        return tr("follow_system");
    }

    private String modeLabel(String mode) {
        if (WeatherRepository.WIDGET_MODE_COMPACT.equals(mode)) return tr("compact");
        if (WeatherRepository.WIDGET_MODE_DETAILED.equals(mode)) return tr("detailed");
        return tr("normal");
    }

    private boolean sameLocation(WeatherRepository.SavedLocation a, WeatherRepository.SavedLocation b) {
        return Math.abs(a.latitude - b.latitude) < 0.01 && Math.abs(a.longitude - b.longitude) < 0.01;
    }

    private String shortLocation(String value) {
        if (value == null) return "";
        String v = WeatherRepository.displayLocationName(value);
        if (v.length() > 70) return v.substring(0, 67).trim() + "…";
        return v;
    }

    private void setLanguage(String language) { WeatherRepository.setLanguage(this, language); WeatherWidgetUpdater.refreshAndUpdate(this); recreate(); }
    private void setDark(boolean enabled) { WeatherRepository.setDarkTheme(this, enabled); recreate(); }
    private void setAppThemeMode(String mode) { WeatherRepository.setAppThemeMode(this, mode); recreate(); }
    private void setTemp(String unit) { WeatherRepository.setTempUnit(this, unit); WeatherWidgetUpdater.refreshAndUpdate(this); rebuild(); }
    private void setWind(String unit) { WeatherRepository.setWindUnit(this, unit); WeatherWidgetUpdater.refreshAndUpdate(this); rebuild(); }
    private void setPressure(String unit) { WeatherRepository.setPressureUnit(this, unit); rebuild(); }
    private void setWidgetDark(boolean enabled) { WeatherRepository.setWidgetDark(this, enabled); WeatherWidgetUpdater.updateAllWidgets(this); rebuild(); }
    private void setWidgetThemeMode(String mode) { WeatherRepository.setWidgetThemeMode(this, mode); WeatherWidgetUpdater.updateAllWidgets(this); rebuild(); }
    private void setMode(String mode) { WeatherRepository.setWidgetMode(this, mode); WeatherWidgetUpdater.updateAllWidgets(this); rebuild(); }
    private void setWeatherRefreshInterval(int minutes) {
        WeatherRepository.setWeatherRefreshInterval(this, minutes);
        WeatherWidgetUpdater.scheduleWeatherRefresh(this);
        Toast.makeText(this, tr("weather_refresh_interval") + ": " + L10n.intervalLabel(this, minutes), Toast.LENGTH_SHORT).show();
        rebuild();
    }

    private void toggleLockScreenWeather() {
        boolean enabled = !WeatherRepository.lockScreenWeatherEnabled(this);
        WeatherRepository.setLockScreenWeatherEnabled(this, enabled);
        if (enabled) {
            requestNotificationPermission();
            WeatherWidgetUpdater.scheduleWeatherRefresh(this);
            WeatherWidgetUpdater.refreshAndUpdate(this);
            LockScreenWeatherNotifier.update(this);
            Toast.makeText(this, tr("lockscreen_weather_on"), Toast.LENGTH_SHORT).show();
        } else {
            LockScreenWeatherNotifier.cancel(this);
            WeatherWidgetUpdater.cancelWeatherRefreshIfNoWidgets(this);
            Toast.makeText(this, tr("lockscreen_weather_off"), Toast.LENGTH_SHORT).show();
        }
        rebuild();
    }

    private void toggleRainAlerts() {
        boolean enabled = !WeatherRepository.rainAlertsEnabled(this);
        WeatherRepository.setRainAlertsEnabled(this, enabled);
        if (enabled) {
            requestNotificationPermission();
            RainAlertReceiver.schedule(this);
            Toast.makeText(this, tr("rain_alerts_enabled"), Toast.LENGTH_SHORT).show();
        } else {
            RainAlertReceiver.cancel(this);
            Toast.makeText(this, tr("rain_alerts_disabled"), Toast.LENGTH_SHORT).show();
        }
        rebuild();
    }

    private void setThreshold(int value) {
        WeatherRepository.setRainThreshold(this, value);
        Toast.makeText(this, tr("rain_threshold") + value + "%", Toast.LENGTH_SHORT).show();
        rebuild();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 55);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 55 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (WeatherRepository.lockScreenWeatherEnabled(this)) {
                WeatherWidgetUpdater.refreshAndUpdate(this);
                LockScreenWeatherNotifier.update(this);
            }
            if (WeatherRepository.rainAlertsEnabled(this)) RainAlertReceiver.schedule(this);
        }
    }

    private void rebuild() { content.removeAllViews(); buildUi(); }


    private void applySystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button trayButton(String label, View.OnClickListener listener) {
        return smallButton(label + " ▾", false, listener);
    }

    private Button smallButton(String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setTextSize(12);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setTextColor(primary ? Color.WHITE : textColor);
        int bg = primary ? Color.rgb(37, 99, 235) : (dark ? Color.rgb(30, 41, 59) : Color.rgb(226, 238, 255));
        int stroke = primary ? Color.rgb(37, 99, 235) : Color.TRANSPARENT;
        b.setBackground(round(bg, dp(13), stroke));
        b.setOnClickListener(listener);
        return b;
    }

    private LinearLayout card(View child) {
        LinearLayout box = column();
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(round(cardColor, dp(20), borderColor));
        box.addView(child);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(10), 0, 0);
        box.setLayoutParams(p);
        return box;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams margin(LinearLayout.LayoutParams p, int l, int t, int r, int b) { p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private LinearLayout.LayoutParams chip() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(38), 1); p.setMargins(0, 0, dp(6), 0); return p; }
    private LinearLayout.LayoutParams chipLast() { return new LinearLayout.LayoutParams(0, dp(38), 1); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int statusBarHeight() { int res = getResources().getIdentifier("status_bar_height", "dimen", "android"); return res > 0 ? getResources().getDimensionPixelSize(res) : 0; }
    private int navigationBarHeight() { int res = getResources().getIdentifier("navigation_bar_height", "dimen", "android"); return res > 0 ? getResources().getDimensionPixelSize(res) : 0; }
    private void hideKeyboard(View view) { InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); if (imm != null && view != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0); }
    private String shortError(Exception e) { String m = e.getMessage(); return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m; }
    private String tr(String key) { return L10n.t(this, key); }
}
