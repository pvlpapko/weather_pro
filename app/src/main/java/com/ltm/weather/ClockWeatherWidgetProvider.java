package com.ltm.weather;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class ClockWeatherWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WeatherWidgetUpdater.refreshAndUpdate(context);
        WeatherWidgetUpdater.scheduleClockTick(context);
        WeatherWidgetUpdater.scheduleWeatherRefresh(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, android.os.Bundle newOptions) {
        WeatherWidgetUpdater.updateAllWidgets(context);
        WeatherWidgetUpdater.scheduleClockTick(context);
        WeatherWidgetUpdater.scheduleWeatherRefresh(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        WeatherWidgetUpdater.refreshAndUpdate(context);
        WeatherWidgetUpdater.scheduleClockTick(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && WeatherWidgetUpdater.ACTION_CLOCK_TICK.equals(intent.getAction())) {
            WeatherWidgetUpdater.updateAllWidgets(context);
            WeatherWidgetUpdater.scheduleClockTick(context);
            return;
        }
        if (intent != null && WeatherWidgetUpdater.ACTION_WEATHER_REFRESH.equals(intent.getAction())) {
            WeatherWidgetUpdater.refreshAndUpdate(context);
            WeatherWidgetUpdater.scheduleWeatherRefresh(context);
            return;
        }
        super.onReceive(context, intent);
    }


    @Override
    public void onDisabled(Context context) {
        WeatherWidgetUpdater.cancelClockTick(context);
        WeatherWidgetUpdater.cancelWeatherRefreshIfNoWidgets(context);
        super.onDisabled(context);
    }
}
