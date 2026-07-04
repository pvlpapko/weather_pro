package com.ltm.weather;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class LockScreenWeatherNotifier {
    private static final String CHANNEL_ID = "weather_lock_screen";
    private static final int NOTIFICATION_ID = 9301;
    private static final int REQUEST_CODE = 9302;

    private LockScreenWeatherNotifier() {}

    public static void update(Context context) {
        Context app = context.getApplicationContext();
        if (!WeatherRepository.lockScreenWeatherEnabled(app)) {
            cancel(app);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        ensureChannel(manager);

        WeatherRepository.CachedSnapshot cache = WeatherRepository.readCachedSnapshot(app);
        String city = WeatherWidgetUpdater.formatWidgetLocation(cache.city);
        String condition = cache.condition == null ? "" : cache.condition.trim();
        String temp = cache.temp == null || cache.temp.trim().isEmpty() ? "--°" : cache.temp.trim();
        String dayNight = cache.todayDayNight == null ? "" : cache.todayDayNight.trim();
        String icon = cache.isDay == 0 ? (cache.moonIcon == null || cache.moonIcon.trim().isEmpty() ? "🌙" : cache.moonIcon) : WeatherRepository.weatherEmojiFromCondition(condition);
        String title = temp + " · " + city;
        String text = (icon + " " + stripEmojiPrefix(condition) + (dayNight.isEmpty() ? "" : " · " + dayNight)).trim();

        Intent open = new Intent(app, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(app, REQUEST_CODE, open, WeatherWidgetUpdater.flags());

        Notification.Builder builder = new Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setDefaults(0)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancel(Context context) {
        NotificationManager manager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
    }

    private static void ensureChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
        if (channel != null) return;
        channel = new NotificationChannel(CHANNEL_ID, "Погода на экране блокировки", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Постоянное тихое уведомление с погодой для экрана блокировки и AOD");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    private static String stripEmojiPrefix(String value) {
        if (value == null) return "";
        String clean = value.trim();
        if (clean.length() > 2 && !Character.isLetterOrDigit(clean.charAt(0))) {
            clean = clean.substring(clean.offsetByCodePoints(0, 1)).trim();
        }
        return clean;
    }
}
