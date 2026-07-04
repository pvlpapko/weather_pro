package com.ltm.weather;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class ForecastWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WeatherWidgetUpdater.refreshAndUpdate(context);
        WeatherWidgetUpdater.scheduleClockTick(context);
        WeatherWidgetUpdater.scheduleWeatherRefresh(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        WeatherWidgetUpdater.updateAllWidgets(context);
        WeatherWidgetUpdater.scheduleClockTick(context);
        WeatherWidgetUpdater.scheduleWeatherRefresh(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        WeatherWidgetUpdater.refreshAndUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        WeatherWidgetUpdater.cancelWeatherRefreshIfNoWidgets(context);
        super.onDisabled(context);
    }
}
