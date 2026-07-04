package com.ltm.weather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)) {
            WeatherWidgetUpdater.updateAllWidgets(context);
            WeatherWidgetUpdater.scheduleClockTick(context);
            WeatherWidgetUpdater.scheduleWeatherRefresh(context);
            WeatherWidgetUpdater.refreshAndUpdate(context);
            LockScreenWeatherNotifier.update(context);
            if (WeatherRepository.rainAlertsEnabled(context)) {
                RainAlertReceiver.schedule(context);
            }
        }
    }
}
