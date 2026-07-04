package com.ltm.weather;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    public static final String EXTRA_SECTION = "open_section";
    public static final String SECTION_HOURLY = "hourly";
    public static final String SECTION_DAILY = "daily";
    public static final String SECTION_MAP = "map";

    private static final int REQUEST_LOCATION = 40;
    private static final int REQUEST_NOTIFICATIONS = 41;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ScrollView scrollView;
    private LinearLayout content;
    private LinearLayout hourlyContainer;
    private LinearLayout dailyContainer;
    private Button forecastRangeButton;
    private LinearLayout indicatorsContainer;
    private LinearLayout precipitationContainer;
    private TextView cityText;
    private TextView updatedText;
    private TextView tempText;
    private TextView conditionText;
    private TextView detailsText;
    private TextView hourlyTitle;
    private TextView dailyTitle;
    private TextView mapTitle;
    private Button refreshButton;
    private Button locateButton;
    private WeatherSceneView sceneView;
    private int selectedDays = 7;
    private WeatherRepository.SavedLocation currentLocation;
    private WeatherRepository.WeatherSnapshot lastSnapshot;
    private boolean dark;
    private int bgColor;
    private int cardColor;
    private int textColor;
    private int subTextColor;
    private int borderColor;
    private String pendingSection;
    private boolean waitingForLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = WeatherRepository.isDarkTheme(this);
        if (dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        initColors();
        applySystemBars();
        selectedDays = WeatherRepository.getForecastDays(this);
        currentLocation = WeatherRepository.getSavedLocation(this);
        pendingSection = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_SECTION);
        buildUi();
        renderCached();
        updateWeather();
        if (WeatherRepository.rainAlertsEnabled(this)) RainAlertReceiver.schedule(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        pendingSection = intent == null ? null : intent.getStringExtra(EXTRA_SECTION);
        scrollToSectionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentLocation = WeatherRepository.getSavedLocation(this);
        if (cityText != null) cityText.setText(WeatherRepository.displayLocationName(currentLocation.name));
        WeatherWidgetUpdater.updateAllWidgets(this);
        WeatherWidgetUpdater.scheduleClockTick(this);
        WeatherWidgetUpdater.scheduleWeatherRefresh(this);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void initColors() {
        bgColor = dark ? Color.rgb(2, 6, 23) : Color.rgb(239, 246, 255);
        cardColor = dark ? Color.rgb(15, 23, 42) : Color.argb(242, 255, 255, 255);
        textColor = dark ? Color.rgb(248, 250, 252) : Color.rgb(15, 23, 42);
        subTextColor = dark ? Color.rgb(203, 213, 225) : Color.rgb(100, 116, 139);
        borderColor = dark ? Color.rgb(38, 52, 78) : Color.rgb(207, 226, 255);
    }

    private void buildUi() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(bgColor);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10) + statusBarHeight(), dp(10), dp(16) + navigationBarHeight());
        scrollView.addView(content, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleWrap = column();
        titleWrap.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleWrap.addView(text(tr("app_title"), 22, true, textColor));
        titleWrap.addView(text(tr("app_subtitle"), 11, false, subTextColor));
        header.addView(titleWrap);
        header.addView(smallButton("⚙", true, v -> startActivity(new Intent(this, SettingsActivity.class))), new LinearLayout.LayoutParams(dp(44), dp(38)));
        content.addView(header);

        content.addView(card(currentCard()));
        content.addView(card(daysCard()));

        indicatorsContainer = column();
        indicatorsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(section(tr("useful_indicators"), null));
        content.addView(indicatorsContainer);

        hourlyTitle = section(tr("hourly"), null);
        content.addView(hourlyTitle);
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hourlyContainer = row();
        hsv.addView(hourlyContainer);
        content.addView(hsv);

        dailyTitle = section(tr("forecast_for") + selectedDays + tr("days"), null);
        content.addView(dailyTitle);
        dailyContainer = column();
        content.addView(dailyContainer);

        mapTitle = section(tr("precipitation"), null);
        content.addView(mapTitle);
        precipitationContainer = column();
        content.addView(card(precipitationContainer));

        TextView footer = text(tr("footer"), 11, false, subTextColor);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.setMargins(0, dp(14), 0, 0);
        content.addView(footer, fp);
    }

    private LinearLayout currentCard() {
        LinearLayout box = column();
        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleCol = column();
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        cityText = text(tr("loading"), 16, true, textColor);
        cityText.setMaxLines(2);
        cityText.setEllipsize(TextUtils.TruncateAt.END);
        cityText.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        titleCol.addView(cityText);
        TextView hint = text(tr("tap_city_hint"), 10, false, subTextColor);
        titleCol.addView(hint);
        top.addView(titleCol);
        locateButton = smallButton("📍", false, v -> useDeviceLocation());
        locateButton.setTextSize(18);
        top.addView(locateButton, new LinearLayout.LayoutParams(dp(44), dp(38)));
        box.addView(top);

        updatedText = text("", 11, false, subTextColor);
        sceneView = new WeatherSceneView(this);
        tempText = text("--°", 46, true, textColor);
        tempText.setIncludeFontPadding(false);
        conditionText = text("", 16, false, textColor);
        detailsText = text("", 12, false, dark ? Color.rgb(226, 232, 240) : Color.rgb(51, 65, 85));
        detailsText.setLineSpacing(dp(1), 1.0f);
        refreshButton = smallButton(tr("refresh"), true, v -> updateWeather());
        box.addView(updatedText, margins(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 2, 0, 0));
        box.addView(sceneView, margins(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)), 0, 6, 0, 0));
        box.addView(tempText, margins(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 5, 0, 0));
        box.addView(conditionText);
        box.addView(detailsText, margins(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT), 0, 3, 0, 0));
        box.addView(refreshButton, margins(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)), 0, 8, 0, 0));
        return box;
    }

    private LinearLayout daysCard() {
        LinearLayout box = column();
        box.addView(text(tr("forecast_range"), 15, true, textColor));
        forecastRangeButton = smallButton(forecastRangeLabel(selectedDays), true, v -> showForecastRangeMenu(forecastRangeButton));
        box.addView(forecastRangeButton, margins(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)), 0, 8, 0, 0));
        return box;
    }

    private void renderDayButtons() {
        if (forecastRangeButton != null) forecastRangeButton.setText(forecastRangeLabel(selectedDays));
    }

    private String forecastRangeLabel(int days) {
        return days + tr("days") + " ▾";
    }

    private void showForecastRangeMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        int[] values = {3, 5, 7, 10};
        for (int value : values) menu.getMenu().add(0, value, 0, value + tr("days"));
        menu.setOnMenuItemClickListener(item -> {
            setSelectedDays(item.getItemId());
            return true;
        });
        menu.show();
    }

    private void renderCached() {
        WeatherRepository.CachedSnapshot cached = WeatherRepository.readCachedSnapshot(this);
        cityText.setText(WeatherRepository.displayLocationName(cached.city));
        tempText.setText(cached.temp);
        conditionText.setText(cached.condition);
        if (cached.updated != null && !cached.updated.isEmpty()) updatedText.setText(tr("updated") + cached.updated);
    }

    private void updateWeather() {
        setBusy(true);
        WeatherRepository.SavedLocation location = currentLocation;
        executor.execute(() -> {
            try {
                WeatherRepository.WeatherSnapshot snapshot = WeatherRepository.fetchWeather(this, location.latitude, location.longitude, WeatherRepository.displayLocationName(location.name));
                WeatherRepository.saveSnapshot(this, snapshot);
                runOnUiThread(() -> {
                    lastSnapshot = snapshot;
                    renderWeather(snapshot);
                    setBusy(false);
                    WeatherWidgetUpdater.updateAllWidgets(this);
                    scrollToSectionIfNeeded();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, tr("failed_update") + shortError(e), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void useDeviceLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        Toast.makeText(this, tr("finding_location"), Toast.LENGTH_SHORT).show();
        setBusy(true);
        Location last = bestLastLocation();
        if (last != null && System.currentTimeMillis() - last.getTime() < 180000L) {
            applyDeviceLocation(last);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            fallbackDeviceLocation(last);
            return;
        }
        waitingForLocation = true;
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (!waitingForLocation) return;
                waitingForLocation = false;
                try { manager.removeUpdates(this); } catch (SecurityException ignored) {}
                applyDeviceLocation(location);
            }
            @Override public void onProviderDisabled(String provider) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        };
        boolean requested = false;
        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper());
                requested = true;
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
                requested = true;
            }
        } catch (SecurityException ignored) {}
        if (!requested) {
            fallbackDeviceLocation(last);
            return;
        }
        Location fallback = last;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!waitingForLocation) return;
            waitingForLocation = false;
            try { manager.removeUpdates(listener); } catch (SecurityException ignored) {}
            fallbackDeviceLocation(fallback == null ? bestLastLocation() : fallback);
        }, 6500L);
    }

    private void fallbackDeviceLocation(Location location) {
        if (location == null) {
            setBusy(false);
            Toast.makeText(this, tr("location_failed"), Toast.LENGTH_LONG).show();
            return;
        }
        applyDeviceLocation(location);
    }

    private void applyDeviceLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        executor.execute(() -> {
            try {
                String name = resolveAddressName(lat, lon, location.getAccuracy());
                WeatherRepository.SavedLocation saved = new WeatherRepository.SavedLocation(WeatherRepository.displayLocationName(name), lat, lon);
                WeatherRepository.saveLocation(this, saved);
                WeatherRepository.WeatherSnapshot snapshot = WeatherRepository.fetchWeather(this, saved.latitude, saved.longitude, saved.name);
                WeatherRepository.saveSnapshot(this, snapshot);
                runOnUiThread(() -> {
                    currentLocation = saved;
                    lastSnapshot = snapshot;
                    renderWeather(snapshot);
                    setBusy(false);
                    WeatherWidgetUpdater.updateAllWidgets(this);
                    Toast.makeText(this, tr("location") + name, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, tr("location_weather_failed") + shortError(e), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String resolveAddressName(double lat, double lon, float accuracy) {
        try {
            String networkLabel = WeatherRepository.reverseGeocode(lat, lon);
            if (hasUsefulSettlement(networkLabel)) return WeatherRepository.displayLocationName(networkLabel);
        } catch (Exception ignored) {}
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String label = addressToLabel(addresses.get(0));
                if (label != null && !label.trim().isEmpty()) return WeatherRepository.displayLocationName(label);
            }
        } catch (Exception ignored) {}
        return "Моё место";
    }

    private String addressToLabel(Address a) {
        String street = join(a.getThoroughfare(), a.getSubThoroughfare());
        String feature = a.getFeatureName() == null ? "" : a.getFeatureName().trim();
        String city = firstNonEmpty(a.getLocality(), a.getSubAdminArea(), a.getAdminArea());
        String district = firstNonEmpty(a.getSubLocality(), streetLike(feature) ? "" : feature);
        StringBuilder out = new StringBuilder();
        appendDistinct(out, city);
        appendDistinct(out, district);
        appendDistinct(out, street);
        return out.toString();
    }

    private boolean hasUsefulSettlement(String value) {
        String clean = WeatherRepository.displayLocationName(value);
        if (clean == null || clean.trim().isEmpty()) return false;
        String first = clean.split("[,·]", 2)[0].trim();
        return !streetLike(first);
    }

    private boolean streetLike(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase(Locale.getDefault());
        return v.startsWith("улица ") || v.startsWith("ул. ") || v.startsWith("проспект ") || v.startsWith("пр-т ") || v.startsWith("переулок ") || v.startsWith("шоссе ") || v.startsWith("площадь ") || v.contains(" улица") || v.contains(" проспект");
    }

    private String join(String a, String b) {
        if (a == null || a.trim().isEmpty()) return b == null ? "" : b.trim();
        if (b == null || b.trim().isEmpty()) return a.trim();
        return a.trim() + " " + b.trim();
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private void appendDistinct(StringBuilder out, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.isEmpty()) return;
        String lowerOut = out.toString().toLowerCase(Locale.getDefault());
        if (lowerOut.contains(clean.toLowerCase(Locale.getDefault()))) return;
        if (out.length() > 0) out.append(", ");
        out.append(clean);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            boolean ok = false;
            for (int result : grantResults) ok = ok || result == PackageManager.PERMISSION_GRANTED;
            if (ok) useDeviceLocation();
        }
        if (requestCode == REQUEST_NOTIFICATIONS && WeatherRepository.rainAlertsEnabled(this)) {
            RainAlertReceiver.schedule(this);
        }
    }

    private Location bestLastLocation() {
        try {
            LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (manager == null) return null;
            Location best = null;
            for (String provider : manager.getProviders(true)) {
                Location loc = manager.getLastKnownLocation(provider);
                if (loc != null && (best == null || loc.getAccuracy() < best.getAccuracy())) best = loc;
            }
            return best;
        } catch (SecurityException e) {
            return null;
        }
    }

    private void renderWeather(WeatherRepository.WeatherSnapshot s) {
        cityText.setText(WeatherRepository.displayLocationName(s.locationName));
        updatedText.setText(tr("updated") + s.updatedReadable);
        tempText.setText(WeatherRepository.formatTemp(this, s.temperature));
        conditionText.setText(WeatherRepository.weatherEmoji(s.weatherCode) + " " + capitalize(WeatherRepository.weatherLabel(this, s.weatherCode)));
        if (sceneView != null) sceneView.setWeather(s.weatherCode, s.isDay == 1, s.temperature);
        WeatherRepository.DailyItem today = s.daily.isEmpty() ? null : s.daily.get(0);
        String dayNight = today == null ? "" : "\n" + WeatherRepository.formatDayNight(this, today);
        detailsText.setText(String.format(L10n.locale(this),
                "%s %s · %s %s · %s %s\n%s %s, %s · %s %s · %s %.1f %s%s",
                tr("feels"),
                WeatherRepository.formatTemp(this, s.apparentTemperature),
                tr("humidity"),
                s.humidity >= 0 ? s.humidity + "%" : "--",
                tr("pressure"),
                WeatherRepository.formatPressure(this, s.pressure),
                tr("wind"),
                WeatherRepository.formatWind(this, s.windSpeed),
                WeatherRepository.windDirectionLabel(s.windDirection),
                tr("clouds"),
                s.cloudCover >= 0 ? s.cloudCover + "%" : "--",
                tr("precipitation"),
                Double.isNaN(s.precipitation) ? 0.0 : s.precipitation,
                tr("mm"),
                dayNight));
        renderIndicators(s);
        renderHourly(s);
        renderDaily(s);
        renderPrecipitation(s);
    }

    private void renderIndicators(WeatherRepository.WeatherSnapshot s) {
        indicatorsContainer.removeAllViews();
        WeatherRepository.DailyItem today = s.daily.isEmpty() ? null : s.daily.get(0);
        LinearLayout r1 = row();
        LinearLayout r2 = row();
        r1.addView(indicator(tr("sunrise"), today == null ? "--" : today.sunrise, "🌅"), cell());
        r1.addView(indicator(tr("sunset"), today == null ? "--" : today.sunset, "🌇"), cellLast());
        r2.addView(indicator(tr("daylight"), today == null ? "--" : WeatherRepository.daylightLabel(this, today.daylightDurationSeconds), "☀️"), cell());
        r2.addView(indicator("UV", today == null ? "--" : (s.isDay == 1 ? WeatherRepository.formatOneDecimal(today.uvIndexMax, "") : L10n.t(this, "no_uv_night")), "🧴"), cellLast());
        indicatorsContainer.addView(r1);
        indicatorsContainer.addView(r2);
        LinearLayout r3 = row();
        r3.addView(indicator(tr("gusts"), today == null ? "--" : WeatherRepository.formatWind(this, today.windGustsMax), "💨"), cell());
        r3.addView(indicator(tr("rain"), today == null || today.precipitationProbabilityMax < 0 ? "--" : today.precipitationProbabilityMax + "%", "🌧"), cellLast());
        indicatorsContainer.addView(r3);
    }

    private LinearLayout indicator(String label, String value, String icon) {
        LinearLayout box = column();
        box.setPadding(dp(9), dp(7), dp(9), dp(7));
        box.setBackground(round(cardColor, dp(14), borderColor));
        box.addView(text(icon + " " + label, 10, false, subTextColor));
        box.addView(text(value, 14, true, textColor));
        return box;
    }

    private void renderHourly(WeatherRepository.WeatherSnapshot s) {
        hourlyContainer.removeAllViews();
        int count = Math.min(24, s.hourly.size());
        for (int i = 0; i < count; i++) hourlyContainer.addView(hourCard(s.hourly.get(i)));
    }

    private LinearLayout hourCard(WeatherRepository.HourlyItem item) {
        LinearLayout box = column();
        box.setPadding(dp(9), dp(8), dp(9), dp(8));
        box.setBackground(round(cardColor, dp(16), borderColor));
        box.addView(text(item.time, 12, true, textColor));
        box.addView(text(WeatherRepository.weatherEmoji(item.weatherCode), 22, false, textColor));
        box.addView(text(WeatherRepository.formatTemp(this, item.temperature), 15, true, textColor));
        box.addView(text("💧 " + Math.max(0, item.precipitationProbability) + "%", 11, false, subTextColor));
        box.addView(text(WeatherRepository.formatWind(this, item.windSpeed), 11, false, subTextColor));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, dp(8), 0);
        box.setLayoutParams(p);
        return box;
    }

    private void renderDaily(WeatherRepository.WeatherSnapshot s) {
        dailyTitle.setText(tr("forecast_for") + selectedDays + tr("days"));
        dailyContainer.removeAllViews();
        int limit = Math.min(selectedDays, s.daily.size());
        for (int i = 0; i < limit; i++) dailyContainer.addView(card(dailyRow(s.daily.get(i))));
    }

    private LinearLayout dailyRow(WeatherRepository.DailyItem item) {
        LinearLayout box = column();
        LinearLayout top = row();
        TextView day = text(WeatherRepository.dailyLabelWithDate(item), 14, true, textColor);
        top.addView(day, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView cond = text(WeatherRepository.weatherEmoji(item.weatherCode) + " " + WeatherRepository.weatherLabel(this, item.weatherCode), 13, false, subTextColor);
        cond.setGravity(Gravity.CENTER);
        top.addView(cond, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView temp = text(WeatherRepository.formatDayNightCompact(this, item), 14, true, textColor);
        temp.setGravity(Gravity.END);
        top.addView(temp, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(top);
        box.addView(text(tr("sunrise") + " " + item.sunrise + " · " + tr("sunset") + " " + item.sunset + " · " + tr("precipitation") + " " + (item.precipitationProbabilityMax >= 0 ? item.precipitationProbabilityMax + "%" : "--") + " · UV " + WeatherRepository.formatOneDecimal(item.uvIndexMax, "") + " · " + tr("wind") + " " + WeatherRepository.formatWind(this, item.windSpeedMax), 11, false, subTextColor));
        return box;
    }

    private void renderPrecipitation(WeatherRepository.WeatherSnapshot s) {
        precipitationContainer.removeAllViews();
        precipitationContainer.addView(text(tr("precipitation") + " · " + tr("hourly"), 15, true, textColor));
        int count = Math.min(10, s.hourly.size());
        for (int i = 0; i < count; i++) precipitationContainer.addView(precipRow(s.hourly.get(i)));
    }

    private LinearLayout precipRow(WeatherRepository.HourlyItem item) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, 0);
        TextView label = text(item.time, 12, true, subTextColor);
        row.addView(label, new LinearLayout.LayoutParams(dp(46), LinearLayout.LayoutParams.WRAP_CONTENT));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(Math.max(0, item.precipitationProbability));
        row.addView(bar, new LinearLayout.LayoutParams(0, dp(7), 1));
        TextView val = text(item.precipitationProbability >= 0 ? item.precipitationProbability + "%" : "--", 12, true, subTextColor);
        val.setGravity(Gravity.END);
        row.addView(val, new LinearLayout.LayoutParams(dp(50), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void setSelectedDays(int days) {
        selectedDays = days;
        WeatherRepository.setForecastDays(this, days);
        renderDayButtons();
        if (lastSnapshot != null) renderDaily(lastSnapshot);
        Toast.makeText(this, tr("selected_forecast") + days + tr("days"), Toast.LENGTH_SHORT).show();
    }

    private void openMap() {
        Toast.makeText(this, tr("precipitation") + ": " + tr("hourly"), Toast.LENGTH_SHORT).show();
        if (mapTitle != null) scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, mapTitle.getTop() - dp(10))));
    }

    private void setBusy(boolean busy) {
        if (refreshButton != null) {
            refreshButton.setEnabled(!busy);
            refreshButton.setText(busy ? tr("updating") : tr("refresh"));
        }
        if (locateButton != null) locateButton.setEnabled(!busy);
    }

    private void scrollToSectionIfNeeded() {
        if (pendingSection == null || pendingSection.isEmpty()) return;
        View target = SECTION_DAILY.equals(pendingSection) ? dailyTitle : hourlyTitle;
        if (target != null) scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, target.getTop() - dp(10))));
        pendingSection = null;
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

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView section(String label, View.OnClickListener listener) {
        TextView t = text(label, 16, true, textColor);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(12), 0, dp(5));
        t.setLayoutParams(p);
        if (listener != null) t.setOnClickListener(listener);
        return t;
    }

    private Button smallButton(String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setSingleLine(true);
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
        p.setMargins(0, dp(9), 0, 0);
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

    private LinearLayout.LayoutParams margins(LinearLayout.LayoutParams p, int l, int t, int r, int b) {
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private LinearLayout.LayoutParams cell() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(0, 0, dp(7), dp(7));
        return p;
    }

    private LinearLayout.LayoutParams cellLast() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(0, 0, 0, dp(7));
        return p;
    }

    private Space space(int w, int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(w), dp(h)));
        return s;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int statusBarHeight() {
        int res = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return res > 0 ? getResources().getDimensionPixelSize(res) : 0;
    }

    private int navigationBarHeight() {
        int res = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return res > 0 ? getResources().getDimensionPixelSize(res) : 0;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private String capitalize(String v) {
        if (v == null || v.isEmpty()) return "";
        return v.substring(0, 1).toUpperCase(Locale.getDefault()) + v.substring(1);
    }

    private String shortError(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private String tr(String key) {
        return L10n.t(this, key);
    }

    public void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }
}
