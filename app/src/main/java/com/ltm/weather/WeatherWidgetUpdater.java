package com.ltm.weather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WeatherWidgetUpdater {
    public static final String ACTION_CLOCK_TICK = "com.ltm.weather.ACTION_CLOCK_TICK";
    public static final String ACTION_WEATHER_REFRESH = "com.ltm.weather.ACTION_WEATHER_REFRESH";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private WeatherWidgetUpdater() {}

    public static void refreshAndUpdate(Context context) {
        Context app = context.getApplicationContext();
        scheduleWeatherRefresh(app);
        updateAllWidgets(app);
        EXECUTOR.execute(() -> {
            try {
                WeatherRepository.SavedLocation loc = WeatherRepository.getSavedLocation(app);
                String locationName = WeatherRepository.displayLocationName(loc.name);
                WeatherRepository.WeatherSnapshot snapshot = WeatherRepository.fetchWeather(app, loc.latitude, loc.longitude, locationName);
                WeatherRepository.saveSnapshot(app, snapshot);
            } catch (Exception ignored) {}
            updateAllWidgets(app);
            LockScreenWeatherNotifier.update(app);
        });
    }

    public static void updateAllWidgets(Context context) {
        updateClockWeather(context);
        updateCenterClockWeather(context);
        updateHourly(context);
        updateForecast(context);
        scheduleClockTick(context);
        LockScreenWeatherNotifier.update(context);
    }

    public static void scheduleClockTick(Context context) {
        try {
            if (!hasClockWidgets(context)) {
                cancelClockTick(context);
                return;
            }
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            PendingIntent pi = clockTickIntent(context);
            long now = System.currentTimeMillis();
            long interval = 60000L;
            long nextTick = now + interval - (now % interval) + 500L;
            alarmManager.cancel(pi);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTick, pi);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTick, pi);
            }
        } catch (Exception ignored) {}
    }

    public static void scheduleWeatherRefresh(Context context) {
        try {
            Context app = context.getApplicationContext();
            if (!hasAnyWidgets(app) && !WeatherRepository.lockScreenWeatherEnabled(app)) {
                cancelWeatherRefresh(app);
                return;
            }
            AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            PendingIntent pi = weatherRefreshIntent(app);
            long interval = WeatherRepository.getWeatherRefreshInterval(app) * 60L * 1000L;
            long first = System.currentTimeMillis() + Math.max(15L * 60L * 1000L, interval);
            alarmManager.cancel(pi);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, first, pi);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, first, pi);
            }
        } catch (Exception ignored) {}
    }

    public static void cancelWeatherRefresh(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) alarmManager.cancel(weatherRefreshIntent(context.getApplicationContext()));
        } catch (Exception ignored) {}
    }

    public static void cancelWeatherRefreshIfNoWidgets(Context context) {
        if (!hasAnyWidgets(context) && !WeatherRepository.lockScreenWeatherEnabled(context)) cancelWeatherRefresh(context);
    }

    public static void cancelClockTick(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) alarmManager.cancel(clockTickIntent(context));
        } catch (Exception ignored) {}
    }

    private static PendingIntent clockTickIntent(Context context) {
        Intent intent = new Intent(context, ClockWeatherWidgetProvider.class);
        intent.setPackage(context.getPackageName());
        intent.setAction(ACTION_CLOCK_TICK);
        return PendingIntent.getBroadcast(context, 9101, intent, flags());
    }

    private static PendingIntent weatherRefreshIntent(Context context) {
        Intent intent = new Intent(context, ClockWeatherWidgetProvider.class);
        intent.setPackage(context.getPackageName());
        intent.setAction(ACTION_WEATHER_REFRESH);
        return PendingIntent.getBroadcast(context, 9102, intent, flags());
    }

    private static void updateClockWeather(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidgetProvider.class));
        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(context);
        String condition = cache.condition == null ? "" : cache.condition;
        for (int id : ids) {
            Bundle options = manager.getAppWidgetOptions(id);
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_clock_weather);
            applyBackground(context, v, R.id.widgetRootClock);
            applyClockTextColors(context, v);
            applyMode(context, v, true, options);
            v.setTextViewText(R.id.widgetCity, formatWidgetLocation(cache.city));
            v.setImageViewResource(R.id.widgetWeatherImage, weatherImageResource(cache));
            v.setTextViewText(R.id.widgetEffect, weatherEffectLine(condition));
            v.setTextViewText(R.id.widgetTemp, cache.temp);
            v.setTextViewText(R.id.widgetCondition, shortCondition(condition));
            v.setTextViewText(R.id.widgetUpdated, compactDayNight(context, cache.todayDayNight));
            v.setOnClickPendingIntent(R.id.widgetClockArea, openClockIntent(context));
            v.setOnClickPendingIntent(R.id.widgetWeatherArea, openAppIntent(context, null, 1001));
            v.setOnClickPendingIntent(R.id.widgetSettingsButton, openWidgetConfigIntent(context, id, 1099));
            v.setOnClickPendingIntent(R.id.widgetRootClock, openAppIntent(context, null, 1000));
            manager.updateAppWidget(id, v);
        }
    }

    private static void updateCenterClockWeather(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CenterClockWeatherWidgetProvider.class));
        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(context);
        String condition = cache.condition == null ? "" : cache.condition;
        for (int id : ids) {
            Bundle options = manager.getAppWidgetOptions(id);
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_center_clock_weather);
            applyBackground(context, v, R.id.centerWidgetRoot);
            int main = WeatherRepository.isWidgetDark(context) ? 0xFFF8FAFC : 0xFF0F172A;
            int sub = WeatherRepository.isWidgetDark(context) ? 0xFFE2E8F0 : 0xFF334155;
            v.setTextColor(R.id.centerClockTime, main);
            v.setTextColor(R.id.centerClockDate, sub);
            v.setTextColor(R.id.centerTemp, main);
            v.setTextColor(R.id.centerDayNight, sub);
            v.setTextColor(R.id.centerWidgetSettings, sub);
            applyCenterWidgetSizing(context, v, options);
            v.setImageViewResource(R.id.centerWeatherImage, weatherImageResource(cache));
            v.setTextViewText(R.id.centerTemp, cache.temp);
            v.setTextViewText(R.id.centerDayNight, compactDayNight(context, cache.todayDayNight));
            v.setOnClickPendingIntent(R.id.centerClockArea, openClockIntent(context));
            v.setOnClickPendingIntent(R.id.centerWeatherArea, openAppIntent(context, null, 4401));
            v.setOnClickPendingIntent(R.id.centerTempArea, openAppIntent(context, null, 4402));
            v.setOnClickPendingIntent(R.id.centerWidgetSettings, openWidgetConfigIntent(context, id, 4499));
            v.setOnClickPendingIntent(R.id.centerWidgetRoot, openAppIntent(context, null, 4403));
            manager.updateAppWidget(id, v);
        }
    }

    private static void updateHourly(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HourlyWidgetProvider.class));
        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(context);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_hourly_4x4);
            Bundle options = manager.getAppWidgetOptions(id);
            applyBackground(context, v, R.id.hourlyRoot);
            applyHourlyTextColors(context, v);
            applyHourlySizing(v, options);
            v.setTextViewText(R.id.hourlyWidgetTitle, L10n.t(context, "hourly"));
            v.setTextColor(R.id.hourlyWidgetSettings, WeatherRepository.isWidgetDark(context) ? 0xFFE2E8F0 : 0xFF334155);
            int[] lines = {R.id.hourlyLine1, R.id.hourlyLine2, R.id.hourlyLine3, R.id.hourlyLine4, R.id.hourlyLine5, R.id.hourlyLine6, R.id.hourlyLine7, R.id.hourlyLine8};
            for (int i = 0; i < lines.length; i++) v.setTextViewText(lines[i], cache.hourly[i]);
            v.setOnClickPendingIntent(R.id.hourlyWidgetSettings, openWidgetConfigIntent(context, id, 2099));
            v.setOnClickPendingIntent(R.id.hourlyRoot, openAppIntent(context, null, 2001));
            manager.updateAppWidget(id, v);
        }
    }

    private static void updateForecast(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ForecastWidgetProvider.class));
        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(context);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_forecast);
            Bundle options = manager.getAppWidgetOptions(id);
            applyBackground(context, v, R.id.forecastRoot);
            applyForecastTextColors(context, v);
            applyForecastSizing(v, options);
            v.setTextViewText(R.id.forecastWidgetTitle, L10n.t(context, "forecast") + " · 3" + L10n.t(context, "days"));
            v.setTextColor(R.id.forecastWidgetSettings, WeatherRepository.isWidgetDark(context) ? 0xFFE2E8F0 : 0xFF334155);
            v.setTextViewText(R.id.forecastWidgetCity, formatWidgetLocation(cache.city));
            v.setTextViewText(R.id.forecastDay1, cache.daily[0]);
            v.setTextViewText(R.id.forecastDay2, cache.daily[1]);
            v.setTextViewText(R.id.forecastDay3, cache.daily[2]);
            v.setOnClickPendingIntent(R.id.forecastWidgetSettings, openWidgetConfigIntent(context, id, 3099));
            v.setOnClickPendingIntent(R.id.forecastRoot, openAppIntent(context, null, 3001));
            manager.updateAppWidget(id, v);
        }
    }

    private static void applyBackground(Context context, RemoteViews views, int rootId) {
        boolean dark = WeatherRepository.isWidgetDark(context);
        int transparency = WeatherRepository.getWidgetTransparency(context);
        views.setInt(rootId, "setBackgroundColor", widgetBackgroundColor(dark, transparency));
    }

    public static int widgetBackgroundColor(boolean dark, int transparencyPercent) {
        int percent = Math.max(0, Math.min(100, transparencyPercent));
        int alpha = Math.round(255f * (100 - percent) / 100f);
        int red = dark ? 8 : 255;
        int green = dark ? 13 : 255;
        int blue = dark ? 25 : 255;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static String formatWidgetLocation(String raw) {
        String clean = WeatherRepository.settlementName(raw);
        if (clean == null || clean.trim().isEmpty()) return "Город";
        return limit(compressLocationLabel(clean.trim()), 18);
    }

    private static String compressLocationLabel(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return "Город";
        String lower = v.toLowerCase(Locale.getDefault());
        String[] stopPhrases = {"сельский совет", "сельсовет", "поселковый совет", "городской округ", "муниципальный округ", "район", "область", "district", "region"};
        for (String phrase : stopPhrases) {
            int idx = lower.indexOf(phrase);
            if (idx > 0) {
                v = v.substring(0, idx).trim();
                lower = v.toLowerCase(Locale.getDefault());
            }
        }
        v = v.replaceAll("(?i)\\b(аг\\.?|г\\.?|д\\.?|дер\\.?|пос\\.?|п\\.?\\s*г\\.?\\s*т\\.?|город|деревня|посёлок|поселок|село)\\s+", "").trim();
        v = v.replaceAll("[,:;\\-–—]+$", "").trim();
        if (v.isEmpty()) return "Город";
        return v;
    }


    private static int weatherImageResource(WeatherRepository.CachedSnapshot cache) {
        int code = cache == null ? -1 : cache.weatherCode;
        boolean isDay = cache == null || cache.isDay == 1;
        return weatherImageResource(code, isDay);
    }

    private static int weatherImageResource(int code, boolean isDay) {
        if (code >= 95) return R.drawable.weather_real_storm;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return R.drawable.weather_real_rain;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.drawable.weather_real_snow;
        if (code == 45 || code == 48) return R.drawable.weather_real_fog;
        if (code == 0) return isDay ? R.drawable.weather_real_sun : R.drawable.weather_real_moon;
        if (code == 1 || code == 2) return isDay ? R.drawable.weather_real_partly : R.drawable.weather_real_moon;
        if (code == 3) return R.drawable.weather_real_cloud;
        return isDay ? R.drawable.weather_real_partly : R.drawable.weather_real_moon;
    }

    private static String widgetWeatherIcon(WeatherRepository.CachedSnapshot cache, String condition) {
        if (cache != null && cache.isDay == 0) {
            return cache.moonIcon == null || cache.moonIcon.trim().isEmpty() ? "🌙" : cache.moonIcon;
        }
        return animatedWeatherSymbol(condition);
    }

    private static String compactDayNight(Context context, String value) {
        if (value == null || value.trim().isEmpty()) return L10n.t(context, "tap_for_forecast");
        String v = value.trim()
                .replace("Дн.", "День")
                .replace("Дз.", "День")
                .replace("Ноч.", "Ночь")
                .replace("Day", "Day")
                .replace("Night", "Night")
                .replace("Ngày", "Day")
                .replace("Đêm", "Night");
        v = v.replaceAll("\\s+", " ").replace(": ", " ");
        return limit(v, 28);
    }

    private static void addUniquePart(List<String> parts, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.isEmpty()) return;
        String lower = clean.toLowerCase(Locale.getDefault());
        for (String existing : parts) {
            String e = existing.toLowerCase(Locale.getDefault());
            if (e.contains(lower) || lower.contains(e)) return;
        }
        parts.add(clean);
    }

    public static String shortCondition(String value) {
        if (value == null || value.trim().isEmpty()) return "обновление...";
        String v = value.trim();
        if (v.length() > 22) return v.substring(0, 20).trim() + "…";
        return v;
    }

    private static String animatedLocationLine(String raw) {
        return formatWidgetLocation(raw);
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(1, max - 1)).trim() + "…";
    }

    private static String animatedWeatherSymbol(String condition) {
        String base = weatherSymbol(condition);
        int phase = (int) ((System.currentTimeMillis() / 20000L) % 4);
        if (condition != null) {
            String c = condition.toLowerCase(Locale.getDefault());
            if (isRain(c)) return new String[]{"☔", "🌧", "☔", "🌦"}[phase];
            if (isSnow(c)) return new String[]{"❄", "✻", "❄", "✦"}[phase];
            if (isThunder(c)) return new String[]{"☈", "⚡", "⛈", "⚡"}[phase];
            if (isFog(c)) return new String[]{"≋", "〰", "≋", "〰"}[phase];
            if (isCloud(c)) return new String[]{"☁", "☁", "☁", "☁"}[phase];
        }
        if ("☀".equals(base)) return new String[]{"☀", "◌", "☀", "◎"}[phase];
        if ("☾".equals(base)) return new String[]{"☾", "◐", "☾", "◑"}[phase];
        return base;
    }

    private static String weatherEffectLine(String condition) {
        if (condition == null || condition.trim().isEmpty()) return "";
        String c = condition.toLowerCase(Locale.getDefault());
        int phase = (int) ((System.currentTimeMillis() / 20000L) % 4);
        if (isThunder(c)) return new String[]{"⚡  ⋰⋱  ⚡", "⋱ ⚡ ⋰", "⚡  ⋱⋰  ⚡", "⋰ ⚡ ⋱"}[phase];
        if (isRain(c)) return new String[]{"⋰ ⋱ ⋰ ⋱", "⋱ ⋰ ⋱ ⋰", "╲ ╲ ╲", "╱ ╱ ╱"}[phase];
        if (isSnow(c)) return new String[]{"❄ · ❄ · ❄", "· ❄ · ❄ ·", "✻ · ✻ · ✻", "· ✻ · ✻ ·"}[phase];
        if (isFog(c)) return new String[]{"≋  ≋  ≋", "  ≋  ≋  ≋", "〰 〰 〰", " 〰 〰 〰"}[phase];
        if (isCloud(c)) return new String[]{"☁    ☁", "  ☁   ☁", "☁  ☁", "   ☁  ☁"}[phase];
        if (isNightNow()) return new String[]{"✦  ·  ✦", "·  ✦  ·", "✧  ·  ✧", "·  ✧  ·"}[phase];
        return new String[]{"◌  ◦  ◌", " ◦  ◌  ◦", "◎  ◦  ◎", " ◦  ◎  ◦"}[phase];
    }

    private static boolean isRain(String c) { return c.contains("дожд") || c.contains("даждж") || c.contains("морос") || c.contains("імжа") || c.contains("ливн") || c.contains("rain") || c.contains("drizzle") || c.contains("shower") || c.contains("降雨") || c.contains("雨") || c.contains("비") || c.contains("mưa") || c.contains("🌧") || c.contains("☔"); }
    private static boolean isSnow(String c) { return c.contains("снег") || c.contains("снеж") || c.contains("snow") || c.contains("雪") || c.contains("눈") || c.contains("tuyết") || c.contains("❄"); }
    private static boolean isThunder(String c) { return c.contains("гроз") || c.contains("наваль") || c.contains("thunder") || c.contains("雷") || c.contains("뇌우") || c.contains("dông") || c.contains("⛈") || c.contains("⚡"); }
    private static boolean isFog(String c) { return c.contains("туман") || c.contains("дым") || c.contains("fog") || c.contains("雾") || c.contains("霧") || c.contains("안개") || c.contains("sương") || c.contains("🌫"); }
    private static boolean isCloud(String c) { return c.contains("облач") || c.contains("воблач") || c.contains("пасмур") || c.contains("пахмур") || c.contains("cloud") || c.contains("overcast") || c.contains("云") || c.contains("曇") || c.contains("구름") || c.contains("mây") || c.contains("☁"); }

    private static String weatherSymbol(String condition) {
        if (condition == null) return isNightNow() ? "☾" : "☀";
        String c = condition.toLowerCase(Locale.getDefault());
        if (isThunder(c)) return "☈";
        if (isRain(c)) return "☔";
        if (isSnow(c)) return "❄";
        if (isFog(c)) return "≋";
        if (isCloud(c)) return "☁";
        return isNightNow() ? "☾" : "☀";
    }

    private static boolean isNightNow() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= 21 || h < 6;
    }

    private static void applyClockTextColors(Context context, RemoteViews views) {
        int main = WeatherRepository.isWidgetDark(context) ? 0xFFF8FAFC : 0xFF0F172A;
        int sub = WeatherRepository.isWidgetDark(context) ? 0xFFE2E8F0 : 0xFF334155;
        views.setTextColor(R.id.widgetClockTime, main);
        views.setTextColor(R.id.widgetClockDate, sub);
        views.setTextColor(R.id.widgetClockHint, sub);
        views.setTextColor(R.id.widgetCity, sub);
        views.setTextColor(R.id.widgetSettingsButton, sub);
        views.setTextColor(R.id.widgetEffect, sub);
        views.setTextColor(R.id.widgetTemp, main);
        views.setTextColor(R.id.widgetCondition, main);
        views.setTextColor(R.id.widgetUpdated, sub);
    }

    private static void applyHourlyTextColors(Context context, RemoteViews views) {
        int main = WeatherRepository.isWidgetDark(context) ? 0xFFF8FAFC : 0xFF0F172A;
        views.setTextColor(R.id.hourlyWidgetTitle, main);
        int[] lines = {R.id.hourlyLine1, R.id.hourlyLine2, R.id.hourlyLine3, R.id.hourlyLine4, R.id.hourlyLine5, R.id.hourlyLine6, R.id.hourlyLine7, R.id.hourlyLine8};
        for (int id : lines) views.setTextColor(id, main);
    }

    private static void applyForecastTextColors(Context context, RemoteViews views) {
        int main = WeatherRepository.isWidgetDark(context) ? 0xFFF8FAFC : 0xFF0F172A;
        int sub = WeatherRepository.isWidgetDark(context) ? 0xFFE2E8F0 : 0xFF334155;
        views.setTextColor(R.id.forecastWidgetTitle, main);
        views.setTextColor(R.id.forecastWidgetCity, sub);
        views.setTextColor(R.id.forecastDay1, main);
        views.setTextColor(R.id.forecastDay2, main);
        views.setTextColor(R.id.forecastDay3, main);
    }

    private static void applyCenterWidgetSizing(Context context, RemoteViews views, Bundle options) {
        int width = optionLargestDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320);
        int height = optionLargestDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 92);

        boolean tiny = width < 140 || height < 54;
        boolean low = height < 78;

        float clockUser = clamp(WeatherRepository.getWidgetClockScale(context) / 100f, 0.70f, 1.90f);
        float weatherUser = clamp(WeatherRepository.getWidgetWeatherScale(context) / 100f, 0.80f, 2.20f);
        float safe = tiny ? 0.78f : low ? 0.90f : 1.00f;
        float widthBoost = clamp(width / 320f, 0.96f, 1.16f);
        float heightBoost = clamp(height / 92f, 0.94f, 1.12f);
        float factor = Math.min(widthBoost, heightBoost) * safe;

        int clockSize = clamp(Math.round(64 * clockUser * factor), tiny ? 26 : 40, 112);
        int iconSize = clamp(Math.round(52 * weatherUser * factor), tiny ? 36 : 50, low ? 74 : 92);
        int tempSize = clamp(Math.round(32 * weatherUser * factor), tiny ? 20 : 26, low ? 52 : 66);
        int smallSize = clamp(Math.round(10 * factor), 8, 13);

        views.setTextViewTextSize(R.id.centerClockTime, TypedValue.COMPLEX_UNIT_SP, clockSize);
        views.setTextViewTextSize(R.id.centerClockDate, TypedValue.COMPLEX_UNIT_SP, smallSize);
        views.setTextViewTextSize(R.id.centerTemp, TypedValue.COMPLEX_UNIT_SP, tempSize);
        views.setTextViewTextSize(R.id.centerDayNight, TypedValue.COMPLEX_UNIT_SP, smallSize);
        applyImageViewSizeDp(views, R.id.centerWeatherImage, iconSize);

        int padH = clamp(Math.round(width / 42f), 4, 12);
        int padV = clamp(Math.round(height / 42f), 2, 6);
        views.setViewPadding(R.id.centerWidgetRoot, padH, padV, padH, padV);

        views.setViewVisibility(R.id.centerClockDate, (height < 58 || clockUser > 1.65f) ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.centerDayNight, (height < 58 || weatherUser > 1.95f) ? View.GONE : View.VISIBLE);
    }

    private static void applyMode(Context context, RemoteViews views, boolean clockWidget, Bundle options) {
        String mode = WeatherRepository.getWidgetMode(context);
        boolean compactByUser = WeatherRepository.WIDGET_MODE_COMPACT.equals(mode);
        boolean detailedByUser = WeatherRepository.WIDGET_MODE_DETAILED.equals(mode);
        if (clockWidget) {
            int width = optionLargestDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320);
            int height = optionLargestDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 92);

            boolean tiny = width < 135 || height < 52;
            boolean low = height < 76;
            boolean compact = compactByUser || tiny;
            boolean detailed = detailedByUser && !tiny && width >= 250;

            float clockUser = clamp(WeatherRepository.getWidgetClockScale(context) / 100f, 0.70f, 1.90f);
            float weatherUser = clamp(WeatherRepository.getWidgetWeatherScale(context) / 100f, 0.80f, 2.20f);
            float safe = tiny ? 0.82f : low ? 0.92f : 1.00f;
            float widthBoost = clamp(width / 320f, 0.98f, 1.16f);

            int baseTime = compact ? 42 : detailed ? 64 : 58;
            int timeSize = clamp(Math.round(baseTime * clockUser * safe * widthBoost), tiny ? 24 : 38, 110);

            float weatherBase = clamp(width / 320f, 0.98f, 1.16f) * safe;
            boolean largeWeather = weatherUser >= 1.45f;
            boolean hugeWeather = weatherUser >= 1.75f;
            boolean showCity = height >= 54 && width >= 132;
            boolean showDayNight = height >= 58 && width >= 150;
            boolean showCondition = height >= 104 && width >= 190 && !hugeWeather;
            if (height <= 116 && largeWeather) showCondition = false;

            int citySize = clamp(Math.round(11 * weatherBase), 8, 14);
            int conditionSize = clamp(Math.round(11 * weatherBase), 8, 13);
            int dayNightSize = clamp(Math.round(10 * weatherBase), 8, 12);
            int tempSize = clamp(Math.round((compact ? 34 : detailed ? 46 : 44) * weatherBase * weatherUser), tiny ? 22 : 34, low ? 68 : 86);
            int iconSize = clamp(Math.round((compact ? 40 : detailed ? 56 : 50) * weatherBase * weatherUser), tiny ? 34 : 50, low ? 72 : 96);

            views.setTextViewTextSize(R.id.widgetClockTime, TypedValue.COMPLEX_UNIT_SP, timeSize);
            views.setTextViewTextSize(R.id.widgetClockDate, TypedValue.COMPLEX_UNIT_SP, clamp(Math.round(11 * safe), 8, 13));
            views.setTextViewTextSize(R.id.widgetClockHint, TypedValue.COMPLEX_UNIT_SP, clamp(Math.round(9 * safe), 8, 11));
            views.setTextViewTextSize(R.id.widgetCity, TypedValue.COMPLEX_UNIT_SP, citySize);
            views.setTextViewTextSize(R.id.widgetTemp, TypedValue.COMPLEX_UNIT_SP, tempSize);
            views.setTextViewTextSize(R.id.widgetEffect, TypedValue.COMPLEX_UNIT_SP, clamp(conditionSize - 1, 8, 12));
            views.setTextViewTextSize(R.id.widgetCondition, TypedValue.COMPLEX_UNIT_SP, conditionSize);
            views.setTextViewTextSize(R.id.widgetUpdated, TypedValue.COMPLEX_UNIT_SP, dayNightSize);
            applyImageViewSizeDp(views, R.id.widgetWeatherImage, iconSize);

            int padH = clamp(Math.round(width / 46f), 4, 11);
            int padV = clamp(Math.round(height / 38f), 2, 6);
            views.setViewPadding(R.id.widgetRootClock, padH, padV, padH, padV);

            views.setViewVisibility(R.id.widgetClockDate, height < 50 ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.widgetClockHint, (tiny || height < 82 || clockUser > 1.50f) ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.widgetCity, showCity ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widgetWeatherImage, (height < 48 || width < 132) ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.widgetEffect, (detailed && height >= 128 && width >= 240 && !largeWeather) ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widgetCondition, showCondition ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widgetUpdated, showDayNight ? View.VISIBLE : View.GONE);
        }
    }

    private static void applyHourlySizing(RemoteViews views, Bundle options) {
        int width = optionDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220);
        int height = optionDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150);
        float factor = Math.min(clamp(width / 220f, 0.62f, 1.08f), clamp(height / 170f, 0.58f, 1.08f));
        views.setTextViewTextSize(R.id.hourlyWidgetTitle, TypedValue.COMPLEX_UNIT_SP, clamp(Math.round(13 * factor), 9, 14));
        int visibleLines = height < 88 ? 2 : height < 116 ? 3 : height < 150 ? 4 : height < 205 ? 6 : 8;
        int[] lines = {R.id.hourlyLine1, R.id.hourlyLine2, R.id.hourlyLine3, R.id.hourlyLine4, R.id.hourlyLine5, R.id.hourlyLine6, R.id.hourlyLine7, R.id.hourlyLine8};
        for (int i = 0; i < lines.length; i++) {
            views.setViewVisibility(lines[i], i < visibleLines ? View.VISIBLE : View.GONE);
            views.setTextViewTextSize(lines[i], TypedValue.COMPLEX_UNIT_SP, clamp(Math.round(12 * factor), 8, 13));
        }
        int padH = clamp(Math.round(width / 28f), 5, 10);
        int padV = clamp(Math.round(height / 26f), 4, 10);
        views.setViewPadding(R.id.hourlyRoot, padH, padV, padH, padV);
    }

    private static void applyForecastSizing(RemoteViews views, Bundle options) {
        int width = optionDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 170);
        int height = optionDp(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 88);
        float factor = Math.min(clamp(width / 180f, 0.58f, 1.08f), clamp(height / 110f, 0.58f, 1.08f));
        views.setTextViewTextSize(R.id.forecastWidgetTitle, TypedValue.COMPLEX_UNIT_SP, clamp(Math.round(13 * factor), 9, 14));
        views.setTextViewTextSize(R.id.forecastWidgetCity, TypedValue.COMPLEX_UNIT_SP, clamp(Math.round(10 * factor), 8, 11));
        int daySize = clamp(Math.round(12 * factor), 8, 13);
        views.setTextViewTextSize(R.id.forecastDay1, TypedValue.COMPLEX_UNIT_SP, daySize);
        views.setTextViewTextSize(R.id.forecastDay2, TypedValue.COMPLEX_UNIT_SP, daySize);
        views.setTextViewTextSize(R.id.forecastDay3, TypedValue.COMPLEX_UNIT_SP, daySize);
        views.setViewVisibility(R.id.forecastWidgetCity, height < 72 ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.forecastDay2, height < 82 ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.forecastDay3, height < 100 ? View.GONE : View.VISIBLE);
        int padH = clamp(Math.round(width / 28f), 5, 10);
        int padV = clamp(Math.round(height / 22f), 4, 10);
        views.setViewPadding(R.id.forecastRoot, padH, padV, padH, padV);
    }

    private static PendingIntent openAppIntent(Context context, String section, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setData(Uri.parse("ltmweather://open/" + requestCode + "/" + (section == null ? "top" : section)));
        if (section != null && !section.isEmpty()) intent.putExtra(MainActivity.EXTRA_SECTION, section);
        return PendingIntent.getActivity(context, requestCode, intent, flags());
    }

    private static PendingIntent openClockIntent(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = bestClockIntent(pm);
        if (intent == null || intent.resolveActivity(pm) == null) {
            intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        }
        boolean fallbackToApp = false;
        if (intent == null || intent.resolveActivity(pm) == null) {
            intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            fallbackToApp = true;
        }
        if (!fallbackToApp) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 5001, intent, flags());
    }

    private static Intent bestClockIntent(PackageManager pm) {
        Intent implicitAlarms = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        if (implicitAlarms.resolveActivity(pm) != null) return implicitAlarms;

        String[] clockPackages = {
                "com.google.android.deskclock",
                "com.android.deskclock",
                "com.sec.android.app.clockpackage",
                "com.miui.clock",
                "com.huawei.deskclock",
                "com.hihonor.deskclock",
                "com.honor.deskclock",
                "com.coloros.alarmclock",
                "com.oplus.alarmclock",
                "com.vivo.alarmclock",
                "com.oneplus.deskclock",
                "com.asus.deskclock",
                "com.motorola.blur.alarmclock",
                "com.tct.clock",
                "com.tcl.clock",
                "com.mediatek.deskclock",
                "com.transsion.deskclock"
        };

        for (String packageName : clockPackages) {
            Intent showAlarms = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            showAlarms.setPackage(packageName);
            if (showAlarms.resolveActivity(pm) != null) return showAlarms;
        }

        Intent implicitSetAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
        implicitSetAlarm.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (implicitSetAlarm.resolveActivity(pm) != null) return implicitSetAlarm;

        for (String packageName : clockPackages) {
            Intent setAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
            setAlarm.setPackage(packageName);
            setAlarm.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
            if (setAlarm.resolveActivity(pm) != null) return setAlarm;
        }

        for (String packageName : clockPackages) {
            Intent launch = pm.getLaunchIntentForPackage(packageName);
            if (launch != null) return launch;
        }
        return null;
    }

    private static PendingIntent openWidgetConfigIntent(Context context, int appWidgetId, int requestCode) {
        Intent intent = new Intent(context, WidgetConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra(WidgetConfigureActivity.EXTRA_FROM_WIDGET_SETTINGS, true);
        intent.setAction("widget_config_" + appWidgetId + "_" + requestCode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode + appWidgetId, intent, flags());
    }

    private static boolean hasClockWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            return manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidgetProvider.class)).length > 0
                    || manager.getAppWidgetIds(new ComponentName(context, CenterClockWeatherWidgetProvider.class)).length > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasAnyWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            return manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidgetProvider.class)).length > 0
                    || manager.getAppWidgetIds(new ComponentName(context, CenterClockWeatherWidgetProvider.class)).length > 0
                    || manager.getAppWidgetIds(new ComponentName(context, HourlyWidgetProvider.class)).length > 0
                    || manager.getAppWidgetIds(new ComponentName(context, ForecastWidgetProvider.class)).length > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void applyImageViewSizeDp(RemoteViews views, int viewId, int sizeDp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutWidth(viewId, sizeDp, TypedValue.COMPLEX_UNIT_DIP);
            views.setViewLayoutHeight(viewId, sizeDp, TypedValue.COMPLEX_UNIT_DIP);
        }
    }

    private static int optionLargestDp(Bundle options, String minKey, String maxKey, int fallback) {
        if (options == null) return fallback;
        int min = Math.max(1, options.getInt(minKey, fallback));
        int max = Math.max(1, options.getInt(maxKey, min));
        return Math.max(min, max);
    }

    private static int optionDp(Bundle options, String key, int fallback) {
        if (options == null) return fallback;
        return Math.max(1, options.getInt(key, fallback));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static int flags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }
}
