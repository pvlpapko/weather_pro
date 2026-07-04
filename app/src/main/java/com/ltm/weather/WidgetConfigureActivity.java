package com.ltm.weather;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class WidgetConfigureActivity extends Activity {
    public static final String EXTRA_FROM_WIDGET_SETTINGS = "from_widget_settings";
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean fromWidgetSettings;
    private boolean dark;
    private boolean widgetDark;
    private int opacity;
    private int clockScale;
    private int weatherScale;
    private int bgColor;
    private int cardColor;
    private int textColor;
    private int subTextColor;
    private int borderColor;
    private LinearLayout previewSurface;
    private LinearLayout previewRoot;
    private LinearLayout previewCanvas;
    private TextView opacityLabel;
    private TextView clockScaleLabel;
    private TextView weatherScaleLabel;
    private TextView previewTime;
    private TextView previewDate;
    private TextView previewHint;
    private TextView previewCity;
    private TextView previewTemp;
    private TextView previewCondition;
    private TextView previewDayNight;
    private TextView previewIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        dark = WeatherRepository.isDarkTheme(this);
        if (dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        initColors();
        applySystemBars();
        widgetDark = WeatherRepository.isWidgetDark(this);
        opacity = WeatherRepository.getWidgetTransparency(this);
        clockScale = WeatherRepository.getWidgetClockScale(this);
        weatherScale = WeatherRepository.getWidgetWeatherScale(this);
        Bundle extras = getIntent() == null ? null : getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            fromWidgetSettings = extras.getBoolean(EXTRA_FROM_WIDGET_SETTINGS, false);
        }
        buildUi();
    }

    private void initColors() {
        bgColor = dark ? Color.rgb(2, 6, 23) : Color.rgb(239, 246, 255);
        cardColor = dark ? Color.rgb(15, 23, 42) : Color.argb(238, 255, 255, 255);
        textColor = dark ? Color.rgb(248, 250, 252) : Color.rgb(15, 23, 42);
        subTextColor = dark ? Color.rgb(203, 213, 225) : Color.rgb(100, 116, 139);
        borderColor = dark ? Color.rgb(51, 65, 85) : Color.rgb(191, 215, 255);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor);
        LinearLayout content = column();
        content.setPadding(dp(12), dp(12) + statusBarHeight(), dp(12), dp(18) + navigationBarHeight());
        scroll.addView(content);
        setContentView(scroll);

        content.addView(text(tr("widget_setup"), 24, true, textColor));
        content.addView(text(tr("widget_setup_hint"), 13, false, subTextColor));
        content.addView(card(previewSection()));
        content.addView(card(transparencySection()));
        content.addView(card(clockSizeSection()));
        content.addView(card(weatherSizeSection()));
        content.addView(card(modeSection()));
        content.addView(card(backgroundSection()));
        content.addView(button(tr("done"), true, v -> finishSetup()), margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 12, 0, 0));
        updatePreview();
    }

    private LinearLayout previewSection() {
        LinearLayout box = column();
        box.addView(text(tr("preview"), 17, true, textColor));
        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(this);

        previewCanvas = column();
        previewCanvas.setPadding(0, 0, 0, 0);
        previewCanvas.setMinimumHeight(dp(150));
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        canvasParams.setMargins(0, dp(10), 0, 0);
        previewCanvas.setLayoutParams(canvasParams);

        previewRoot = row();
        previewRoot.setPadding(dp(14), dp(10), dp(14), dp(10));
        previewRoot.setMinimumHeight(dp(138));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        previewRoot.setLayoutParams(pp);

        LinearLayout left = column();
        left.setGravity(Gravity.CENTER_VERTICAL);
        previewTime = text("13:07", 40, true, widgetTextColor());
        previewTime.setSingleLine(true);
        previewDate = text(new java.text.SimpleDateFormat("EEE, d MMM", L10n.locale(this)).format(new java.util.Date()), 11, false, widgetSubTextColor());
        previewHint = text(tr("alarm_hint"), 9, false, widgetSubTextColor());
        left.addView(previewTime);
        left.addView(previewDate);
        left.addView(previewHint);
        previewRoot.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f));

        LinearLayout right = column();
        right.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        previewCity = text(WeatherWidgetUpdater.formatWidgetLocation(cache.city), 10, false, widgetSubTextColor());
        previewCity.setGravity(Gravity.RIGHT);
        previewCity.setSingleLine(true);
        previewIcon = text("☀", 28, false, widgetTextColor());
        previewIcon.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        previewTemp = text(cache.temp, 38, true, widgetTextColor());
        previewTemp.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout weatherLine = row();
        weatherLine.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        weatherLine.addView(previewIcon);
        LinearLayout.LayoutParams tempParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tempParams.setMargins(dp(5), 0, 0, 0);
        weatherLine.addView(previewTemp, tempParams);
        previewCondition = text(WeatherWidgetUpdater.shortCondition(cache.condition), 11, false, widgetTextColor());
        previewCondition.setGravity(Gravity.RIGHT);
        previewDayNight = text(cache.todayDayNight == null || cache.todayDayNight.trim().isEmpty() ? "Д:18° · Н:10°" : cache.todayDayNight.replace("Дн.", "Д:").replace("Ноч.", "Н:"), 10, false, widgetSubTextColor());
        previewDayNight.setGravity(Gravity.RIGHT);
        right.addView(previewCity);
        right.addView(weatherLine);
        right.addView(previewCondition);
        right.addView(previewDayNight);
        previewRoot.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.92f));

        previewCanvas.addView(previewRoot);
        box.addView(previewCanvas);
        return box;
    }

    private LinearLayout transparencySection() {
        LinearLayout box = column();
        opacityLabel = text(tr("opacity") + opacity + "%", 17, true, textColor);
        box.addView(opacityLabel);
        box.addView(text(tr("transparency_hint"), 12, false, subTextColor));
        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(opacity);
        seek.setPadding(0, dp(6), 0, 0);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacity = progress;
                WeatherRepository.setWidgetTransparency(WidgetConfigureActivity.this, progress);
                updatePreview();
                WeatherWidgetUpdater.updateAllWidgets(WidgetConfigureActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(WidgetConfigureActivity.this, tr("opacity") + opacity + "%", Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(seek, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private LinearLayout clockSizeSection() {
        LinearLayout box = column();
        clockScaleLabel = text(tr("clock_size") + clockScale + "%", 17, true, textColor);
        box.addView(clockScaleLabel);
        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(clockScale - 70);
        seek.setPadding(0, dp(6), 0, 0);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                clockScale = progress + 70;
                WeatherRepository.setWidgetClockScale(WidgetConfigureActivity.this, clockScale);
                updatePreview();
                WeatherWidgetUpdater.updateAllWidgets(WidgetConfigureActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(seek, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private LinearLayout weatherSizeSection() {
        LinearLayout box = column();
        weatherScaleLabel = text(tr("weather_size") + weatherScale + "%", 17, true, textColor);
        box.addView(weatherScaleLabel);
        SeekBar seek = new SeekBar(this);
        seek.setMax(140);
        seek.setProgress(weatherScale - 80);
        seek.setPadding(0, dp(6), 0, 0);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                weatherScale = progress + 80;
                WeatherRepository.setWidgetWeatherScale(WidgetConfigureActivity.this, weatherScale);
                updatePreview();
                WeatherWidgetUpdater.updateAllWidgets(WidgetConfigureActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(seek, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private LinearLayout modeSection() {
        LinearLayout box = column();
        box.addView(text(tr("info_density"), 17, true, textColor));
        String mode = WeatherRepository.getWidgetMode(this);
        Button chooser = button(modeLabel(mode) + "  ▾", false, v -> showModeMenu((Button) v));
        box.addView(chooser, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)), 0, 8, 0, 0));
        return box;
    }

    private LinearLayout backgroundSection() {
        LinearLayout box = column();
        box.addView(text(tr("widget_color"), 17, true, textColor));
        Button chooser = button(themeLabel(WeatherRepository.getWidgetThemeMode(this)) + "  ▾", false, v -> showBackgroundMenu((Button) v));
        box.addView(chooser, margin(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)), 0, 8, 0, 0));
        return box;
    }

    private void showModeMenu(Button anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(tr("compact")).setOnMenuItemClickListener(item -> { setMode(WeatherRepository.WIDGET_MODE_COMPACT); return true; });
        menu.getMenu().add(tr("normal")).setOnMenuItemClickListener(item -> { setMode(WeatherRepository.WIDGET_MODE_NORMAL); return true; });
        menu.getMenu().add(tr("detailed")).setOnMenuItemClickListener(item -> { setMode(WeatherRepository.WIDGET_MODE_DETAILED); return true; });
        menu.show();
    }

    private void showBackgroundMenu(Button anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(tr("follow_system")).setOnMenuItemClickListener(item -> { setWidgetThemeMode(WeatherRepository.THEME_SYSTEM); return true; });
        menu.getMenu().add(tr("light")).setOnMenuItemClickListener(item -> { setWidgetThemeMode(WeatherRepository.THEME_LIGHT); return true; });
        menu.getMenu().add(tr("dark")).setOnMenuItemClickListener(item -> { setWidgetThemeMode(WeatherRepository.THEME_DARK); return true; });
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

    private void setMode(String value) {
        WeatherRepository.setWidgetMode(this, value);
        WeatherWidgetUpdater.updateAllWidgets(this);
        recreate();
    }

    private void setWidgetDark(boolean value) {
        WeatherRepository.setWidgetDark(this, value);
        widgetDark = WeatherRepository.isWidgetDark(this);
        WeatherWidgetUpdater.updateAllWidgets(this);
        recreate();
    }

    private void setWidgetThemeMode(String mode) {
        WeatherRepository.setWidgetThemeMode(this, mode);
        widgetDark = WeatherRepository.isWidgetDark(this);
        WeatherWidgetUpdater.updateAllWidgets(this);
        recreate();
    }

    private void updatePreview() {
        if (opacityLabel != null) opacityLabel.setText(tr("opacity") + opacity + "%");
        if (clockScaleLabel != null) clockScaleLabel.setText(tr("clock_size") + clockScale + "%");
        if (weatherScaleLabel != null) weatherScaleLabel.setText(tr("weather_size") + weatherScale + "%");
        if (previewCanvas != null) previewCanvas.setBackgroundColor(Color.TRANSPARENT);
        if (previewRoot != null) previewRoot.setBackground(round(WeatherWidgetUpdater.widgetBackgroundColor(widgetDark, opacity), dp(24), Color.TRANSPARENT));
        if (previewTime != null) {
            previewTime.setTextColor(widgetTextColor());
            previewTime.setTextSize(previewClockSize());
        }
        if (previewDate != null) previewDate.setTextColor(widgetSubTextColor());
        if (previewHint != null) previewHint.setTextColor(widgetSubTextColor());
        if (previewCity != null) previewCity.setTextColor(widgetSubTextColor());
        if (previewIcon != null) {
            previewIcon.setTextColor(widgetTextColor());
            previewIcon.setTextSize(Math.max(20, Math.min(54, Math.round(28 * previewWeatherScale()))));
        }
        if (previewTemp != null) {
            previewTemp.setTextColor(widgetTextColor());
            previewTemp.setTextSize(Math.max(30, Math.min(76, Math.round(38 * previewWeatherScale()))));
        }
        if (previewCondition != null) {
            previewCondition.setTextColor(widgetTextColor());
            previewCondition.setTextSize(Math.max(9, Math.min(13, Math.round(11 * previewWeatherScale()))));
        }
        if (previewDayNight != null) {
            previewDayNight.setTextColor(widgetSubTextColor());
            previewDayNight.setTextSize(Math.max(9, Math.min(12, Math.round(10 * previewWeatherScale()))));
        }
    }

    private int previewStrokeColor() {
        return widgetDark ? Color.argb(150, 226, 232, 240) : Color.argb(150, 15, 23, 42);
    }

    private float previewWeatherScale() {
        return Math.max(0.80f, Math.min(2.00f, weatherScale / 100f));
    }

    private int previewClockSize() {
        String mode = WeatherRepository.getWidgetMode(this);
        int base = WeatherRepository.WIDGET_MODE_COMPACT.equals(mode) ? 42 : WeatherRepository.WIDGET_MODE_DETAILED.equals(mode) ? 60 : 54;
        return Math.max(28, Math.min(78, Math.round(base * clockScale / 100f)));
    }

    private int widgetTextColor() { return widgetDark ? Color.WHITE : Color.rgb(15, 23, 42); }
    private int widgetSubTextColor() { return widgetDark ? Color.rgb(226, 232, 240) : Color.rgb(71, 85, 105); }

    private void finishSetup() {
        WeatherWidgetUpdater.refreshAndUpdate(this);
        WeatherWidgetUpdater.updateAllWidgets(this);
        WeatherWidgetUpdater.scheduleClockTick(this);
        WeatherWidgetUpdater.scheduleWeatherRefresh(this);
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        if (fromWidgetSettings) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        }
        finish();
    }


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
    private TextView text(String v, int sp, boolean bold, int color) { TextView t = new TextView(this); t.setText(v); t.setTextSize(sp); t.setTextColor(color); t.setIncludeFontPadding(true); if (bold) t.setTypeface(Typeface.DEFAULT_BOLD); return t; }
    private Button button(String label, boolean primary, View.OnClickListener listener) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(12); b.setSingleLine(true); b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(6), 0, dp(6), 0); b.setTextColor(primary ? Color.WHITE : textColor); b.setBackground(round(primary ? Color.rgb(37, 99, 235) : (dark ? Color.rgb(30, 41, 59) : Color.rgb(226, 238, 255)), dp(14), primary ? Color.rgb(37, 99, 235) : Color.TRANSPARENT)); b.setOnClickListener(listener); return b; }
    private LinearLayout card(View child) { LinearLayout box = column(); box.setPadding(dp(14), dp(12), dp(14), dp(12)); box.setBackground(round(cardColor, dp(22), borderColor)); box.addView(child); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.setMargins(0, dp(12), 0, 0); box.setLayoutParams(p); return box; }
    private GradientDrawable round(int color, int radius, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d; }
    private LinearLayout.LayoutParams margin(LinearLayout.LayoutParams p, int l, int t, int r, int b) { p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private LinearLayout.LayoutParams chip() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(38), 1); p.setMargins(0, 0, dp(6), 0); return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int statusBarHeight() { int res = getResources().getIdentifier("status_bar_height", "dimen", "android"); return res > 0 ? getResources().getDimensionPixelSize(res) : 0; }
    private int navigationBarHeight() { int res = getResources().getIdentifier("navigation_bar_height", "dimen", "android"); return res > 0 ? getResources().getDimensionPixelSize(res) : 0; }
    private String tr(String key) { return L10n.t(this, key); }
}
