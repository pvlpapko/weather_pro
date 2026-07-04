package com.ltm.weather;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RainAlertReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "ltm_weather_rain";
    private static final int REQUEST_CODE = 7100;
    private static final int NOTIFICATION_ID = 7101;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();
        if (!WeatherRepository.rainAlertsEnabled(app)) return;
        EXECUTOR.execute(() -> checkAndNotify(app));
    }

    public static void schedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent pending = pendingIntent(app);
        long first = System.currentTimeMillis() + 15 * 60 * 1000L;
        long interval = 3 * 60 * 60 * 1000L;
        manager.cancel(pending);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, interval, pending);
    }

    public static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pendingIntent(context.getApplicationContext()));
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, RainAlertReceiver.class);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, WeatherWidgetUpdater.flags());
    }

    private static void checkAndNotify(Context context) {
        try {
            WeatherRepository.SavedLocation loc = WeatherRepository.getSavedLocation(context);
            WeatherRepository.WeatherSnapshot snapshot = WeatherRepository.fetchWeather(context, loc.latitude, loc.longitude, WeatherRepository.displayLocationName(loc.name));
            WeatherRepository.saveSnapshot(context, snapshot);
            int threshold = WeatherRepository.getRainThreshold(context);
            WeatherRepository.HourlyItem best = null;
            for (int i = 0; i < Math.min(8, snapshot.hourly.size()); i++) {
                WeatherRepository.HourlyItem h = snapshot.hourly.get(i);
                if (h.precipitationProbability >= threshold && (best == null || h.precipitationProbability > best.precipitationProbability)) {
                    best = h;
                }
            }
            WeatherWidgetUpdater.updateAllWidgets(context);
            LockScreenWeatherNotifier.update(context);
            if (best != null) notifyRain(context, WeatherRepository.displayLocationName(loc.name), best);
        } catch (Exception ignored) {}
    }

    private static void notifyRain(Context context, String city, WeatherRepository.HourlyItem hour) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, L10n.t(context, "rain_alert_channel"), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(L10n.t(context, "rain_alert_channel_desc"));
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra(MainActivity.EXTRA_SECTION, MainActivity.SECTION_HOURLY);
        PendingIntent openIntent = PendingIntent.getActivity(context, 7200, open, WeatherWidgetUpdater.flags());
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_weather)
                .setContentTitle(L10n.t(context, "rain_possible") + city)
                .setContentText(hour.time + " · " + L10n.t(context, "probability") + " " + hour.precipitationProbability + "% · " + WeatherRepository.weatherLabel(context, hour.weatherCode))
                .setContentIntent(openIntent)
                .setAutoCancel(true);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
