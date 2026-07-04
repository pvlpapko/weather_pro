package com.ltm.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class WeatherRepository {
    public static final String PREFS = "ltm_weather_prefs";

    public static final String KEY_CITY = "city";
    public static final String KEY_LAT = "latitude";
    public static final String KEY_LON = "longitude";
    public static final String KEY_SAVED_CITIES = "saved_cities";
    public static final String KEY_DARK_THEME = "dark_theme";
    public static final String KEY_APP_THEME_MODE = "app_theme_mode";
    public static final String KEY_TEMP_UNIT = "temp_unit";
    public static final String KEY_WIND_UNIT = "wind_unit";
    public static final String KEY_PRESSURE_UNIT = "pressure_unit";
    public static final String KEY_WIDGET_TRANSPARENCY = "widget_transparency";
    public static final String KEY_WIDGET_MODE = "widget_mode";
    public static final String KEY_WIDGET_DARK = "widget_dark";
    public static final String KEY_WIDGET_THEME_MODE = "widget_theme_mode";
    public static final String KEY_WIDGET_CLOCK_SCALE = "widget_clock_scale";
    public static final String KEY_WIDGET_WEATHER_SCALE = "widget_weather_scale";
    public static final String KEY_WEATHER_REFRESH_INTERVAL = "weather_refresh_interval";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_FORECAST_DAYS = "forecast_days";
    public static final String KEY_RAIN_ALERTS = "rain_alerts";
    public static final String KEY_RAIN_THRESHOLD = "rain_threshold";
    public static final String KEY_LOCK_SCREEN_WEATHER = "lock_screen_weather";

    public static final String DEFAULT_CITY = "Минск";
    public static final double DEFAULT_LAT = 53.9023;
    public static final double DEFAULT_LON = 27.5619;

    public static final String TEMP_C = "C";
    public static final String TEMP_F = "F";
    public static final String WIND_KMH = "kmh";
    public static final String WIND_MS = "ms";
    public static final String PRESSURE_HPA = "hpa";
    public static final String PRESSURE_MMHG = "mmhg";

    public static final String WIDGET_MODE_COMPACT = "compact";
    public static final String WIDGET_MODE_NORMAL = "normal";
    public static final String WIDGET_MODE_DETAILED = "detailed";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private WeatherRepository() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isDarkTheme(Context context) {
        String mode = getAppThemeMode(context);
        if (THEME_DARK.equals(mode)) return true;
        if (THEME_LIGHT.equals(mode)) return false;
        return isSystemDark(context);
    }

    public static String getAppThemeMode(Context context) {
        String mode = prefs(context).getString(KEY_APP_THEME_MODE, THEME_SYSTEM);
        if (THEME_LIGHT.equals(mode) || THEME_DARK.equals(mode) || THEME_SYSTEM.equals(mode)) return mode;
        return prefs(context).getBoolean(KEY_DARK_THEME, false) ? THEME_DARK : THEME_SYSTEM;
    }

    public static void setAppThemeMode(Context context, String mode) {
        String value = (THEME_LIGHT.equals(mode) || THEME_DARK.equals(mode) || THEME_SYSTEM.equals(mode)) ? mode : THEME_SYSTEM;
        prefs(context).edit().putString(KEY_APP_THEME_MODE, value).putBoolean(KEY_DARK_THEME, THEME_DARK.equals(value)).apply();
    }

    public static void setDarkTheme(Context context, boolean enabled) {
        setAppThemeMode(context, enabled ? THEME_DARK : THEME_LIGHT);
    }

    public static boolean isWidgetDark(Context context) {
        String mode = getWidgetThemeMode(context);
        if (THEME_DARK.equals(mode)) return true;
        if (THEME_LIGHT.equals(mode)) return false;
        return isSystemDark(context);
    }

    public static String getWidgetThemeMode(Context context) {
        String mode = prefs(context).getString(KEY_WIDGET_THEME_MODE, THEME_SYSTEM);
        if (THEME_LIGHT.equals(mode) || THEME_DARK.equals(mode) || THEME_SYSTEM.equals(mode)) return mode;
        return prefs(context).getBoolean(KEY_WIDGET_DARK, true) ? THEME_DARK : THEME_SYSTEM;
    }

    public static void setWidgetThemeMode(Context context, String mode) {
        String value = (THEME_LIGHT.equals(mode) || THEME_DARK.equals(mode) || THEME_SYSTEM.equals(mode)) ? mode : THEME_SYSTEM;
        prefs(context).edit().putString(KEY_WIDGET_THEME_MODE, value).putBoolean(KEY_WIDGET_DARK, THEME_DARK.equals(value)).apply();
    }

    public static void setWidgetDark(Context context, boolean enabled) {
        setWidgetThemeMode(context, enabled ? THEME_DARK : THEME_LIGHT);
    }

    public static boolean isSystemDark(Context context) {
        int night = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    public static String getTempUnit(Context context) {
        return prefs(context).getString(KEY_TEMP_UNIT, TEMP_C);
    }

    public static void setTempUnit(Context context, String unit) {
        prefs(context).edit().putString(KEY_TEMP_UNIT, unit).apply();
        rebuildCacheUsingLastRaw(context);
    }

    public static String getWindUnit(Context context) {
        return prefs(context).getString(KEY_WIND_UNIT, WIND_KMH);
    }

    public static void setWindUnit(Context context, String unit) {
        prefs(context).edit().putString(KEY_WIND_UNIT, unit).apply();
        rebuildCacheUsingLastRaw(context);
    }

    public static String getPressureUnit(Context context) {
        return prefs(context).getString(KEY_PRESSURE_UNIT, PRESSURE_HPA);
    }

    public static void setPressureUnit(Context context, String unit) {
        prefs(context).edit().putString(KEY_PRESSURE_UNIT, unit).apply();
        rebuildCacheUsingLastRaw(context);
    }

    public static int getWidgetTransparency(Context context) {
        return prefs(context).getInt(KEY_WIDGET_TRANSPARENCY, 55);
    }

    public static void setWidgetTransparency(Context context, int transparency) {
        int value = Math.max(0, Math.min(100, transparency));
        prefs(context).edit().putInt(KEY_WIDGET_TRANSPARENCY, value).apply();
    }

    public static String getWidgetMode(Context context) {
        return prefs(context).getString(KEY_WIDGET_MODE, WIDGET_MODE_NORMAL);
    }

    public static void setWidgetMode(Context context, String mode) {
        prefs(context).edit().putString(KEY_WIDGET_MODE, mode).apply();
    }

    public static int getWidgetClockScale(Context context) {
        return clamp(prefs(context).getInt(KEY_WIDGET_CLOCK_SCALE, 100), 70, 170);
    }

    public static void setWidgetClockScale(Context context, int scale) {
        prefs(context).edit().putInt(KEY_WIDGET_CLOCK_SCALE, clamp(scale, 70, 170)).apply();
    }

    public static int getWidgetWeatherScale(Context context) {
        return clamp(prefs(context).getInt(KEY_WIDGET_WEATHER_SCALE, 145), 80, 220);
    }

    public static void setWidgetWeatherScale(Context context, int scale) {
        prefs(context).edit().putInt(KEY_WIDGET_WEATHER_SCALE, clamp(scale, 80, 220)).apply();
    }

    public static int getWeatherRefreshInterval(Context context) {
        return clamp(prefs(context).getInt(KEY_WEATHER_REFRESH_INTERVAL, 30), 15, 720);
    }

    public static void setWeatherRefreshInterval(Context context, int minutes) {
        prefs(context).edit().putInt(KEY_WEATHER_REFRESH_INTERVAL, clamp(minutes, 15, 720)).apply();
    }

    public static String getLanguage(Context context) {
        return L10n.normalizeLanguage(prefs(context).getString(KEY_LANGUAGE, L10n.LANG_RU));
    }

    public static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANGUAGE, L10n.normalizeLanguage(language)).apply();
    }

    public static int getForecastDays(Context context) {
        int days = prefs(context).getInt(KEY_FORECAST_DAYS, 7);
        if (days == 3 || days == 5 || days == 7 || days == 10) return days;
        return 7;
    }

    public static void setForecastDays(Context context, int days) {
        int value = (days == 3 || days == 5 || days == 7 || days == 10) ? days : 7;
        prefs(context).edit().putInt(KEY_FORECAST_DAYS, value).apply();
    }

    public static boolean lockScreenWeatherEnabled(Context context) {
        return prefs(context).getBoolean(KEY_LOCK_SCREEN_WEATHER, false);
    }

    public static void setLockScreenWeatherEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_LOCK_SCREEN_WEATHER, enabled).apply();
    }

    public static boolean rainAlertsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_RAIN_ALERTS, false);
    }

    public static void setRainAlertsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_RAIN_ALERTS, enabled).apply();
    }

    public static int getRainThreshold(Context context) {
        return prefs(context).getInt(KEY_RAIN_THRESHOLD, 55);
    }

    public static void setRainThreshold(Context context, int value) {
        prefs(context).edit().putInt(KEY_RAIN_THRESHOLD, Math.max(10, Math.min(95, value))).apply();
    }

    public static SavedLocation getSavedLocation(Context context) {
        SharedPreferences p = prefs(context);
        return new SavedLocation(
                p.getString(KEY_CITY, DEFAULT_CITY),
                Double.longBitsToDouble(p.getLong(KEY_LAT, Double.doubleToRawLongBits(DEFAULT_LAT))),
                Double.longBitsToDouble(p.getLong(KEY_LON, Double.doubleToRawLongBits(DEFAULT_LON)))
        );
    }

    public static void saveLocation(Context context, SavedLocation location) {
        prefs(context).edit()
                .putString(KEY_CITY, displayLocationName(location.name))
                .putLong(KEY_LAT, Double.doubleToRawLongBits(location.latitude))
                .putLong(KEY_LON, Double.doubleToRawLongBits(location.longitude))
                .apply();
        addSavedCity(context, location);
    }

    public static List<SavedLocation> getSavedCities(Context context) {
        ArrayList<SavedLocation> list = new ArrayList<>();
        String json = prefs(context).getString(KEY_SAVED_CITIES, "");
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    list.add(new SavedLocation(displayLocationName(o.optString("name", DEFAULT_CITY)), o.optDouble("lat", DEFAULT_LAT), o.optDouble("lon", DEFAULT_LON)));
                }
            } catch (Exception ignored) {}
        }
        if (list.isEmpty()) {
            list.add(new SavedLocation(DEFAULT_CITY, DEFAULT_LAT, DEFAULT_LON));
            list.add(new SavedLocation("Berlin", 52.5200, 13.4050));
            list.add(new SavedLocation("Москва", 55.7558, 37.6173));
            saveCities(context, list);
        }
        return list;
    }

    public static void addSavedCity(Context context, SavedLocation location) {
        List<SavedLocation> cities = getSavedCities(context);
        ArrayList<SavedLocation> result = new ArrayList<>();
        result.add(location);
        for (SavedLocation city : cities) {
            if (!sameCity(city, location) && result.size() < 8) {
                result.add(city);
            }
        }
        saveCities(context, result);
    }

    public static void clearSavedCities(Context context) {
        saveCities(context, new ArrayList<>());
    }

    private static boolean sameCity(SavedLocation a, SavedLocation b) {
        return a.name.equalsIgnoreCase(b.name) || (Math.abs(a.latitude - b.latitude) < 0.01 && Math.abs(a.longitude - b.longitude) < 0.01);
    }

    private static void saveCities(Context context, List<SavedLocation> cities) {
        JSONArray array = new JSONArray();
        try {
            for (SavedLocation city : cities) {
                JSONObject o = new JSONObject();
                o.put("name", displayLocationName(city.name));
                o.put("lat", city.latitude);
                o.put("lon", city.longitude);
                array.put(o);
            }
        } catch (Exception ignored) {}
        prefs(context).edit().putString(KEY_SAVED_CITIES, array.toString()).apply();
    }


    public static String displayLocationName(String raw) {
        if (raw == null) return DEFAULT_CITY;
        String clean = raw.replace("Моё место ·", "").replace("Моё место", "").trim();
        if (clean.isEmpty()) return DEFAULT_CITY;
        String[] segments = clean.split("\\s*·\\s*");
        StringBuilder out = new StringBuilder();
        for (String segment : segments) {
            String part = segment == null ? "" : segment.trim();
            if (part.isEmpty() || isCoordinateLike(part) || isAccuracyLike(part)) continue;
            if (out.length() > 0) out.append(" · ");
            out.append(part);
        }
        clean = out.length() == 0 ? clean : out.toString();
        clean = clean.replaceAll("\\s+", " ").trim();
        return clean.isEmpty() ? DEFAULT_CITY : clean;
    }

    private static boolean isCoordinateLike(String value) {
        if (value == null) return false;
        String v = value.trim();
        return v.matches(".*[-+]?\\d{1,2}[\\.,]\\d{3,}.*[-+]?\\d{1,3}[\\.,]\\d{3,}.*");
    }

    private static boolean isAccuracyLike(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase(Locale.getDefault());
        return v.startsWith("±") || v.matches(".*±\\d+\\s*м.*");
    }

    public static CityResult findCity(String query) throws Exception {
        String encoded = URLEncoder.encode(query.trim(), "UTF-8");
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=ru&format=json";
        JSONObject root = new JSONObject(httpGet(url));
        JSONArray results = root.optJSONArray("results");
        if (results == null || results.length() == 0) {
            throw new IllegalStateException("Город не найден");
        }
        JSONObject first = results.getJSONObject(0);
        String name = first.optString("name", query.trim());
        String country = first.optString("country", "");
        String admin = first.optString("admin1", "");
        StringBuilder display = new StringBuilder(name);
        if (!admin.isEmpty() && !admin.equalsIgnoreCase(name)) display.append(", ").append(admin);
        if (!country.isEmpty()) display.append(", ").append(country);
        return new CityResult(display.toString(), first.getDouble("latitude"), first.getDouble("longitude"));
    }


    public static String reverseGeocode(double latitude, double longitude) throws Exception {
        String url = String.format(Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f&zoom=18&addressdetails=1&accept-language=ru",
                latitude, longitude);
        JSONObject root = new JSONObject(httpGet(url));
        JSONObject address = root.optJSONObject("address");
        if (address == null) {
            String display = root.optString("display_name", "");
            return shortenDisplayName(display);
        }
        String road = firstNonEmpty(address.optString("road", ""), address.optString("pedestrian", ""), address.optString("footway", ""), address.optString("path", ""));
        String house = address.optString("house_number", "");
        String street = road.isEmpty() ? "" : house.isEmpty() ? road : road + " " + house;
        String district = firstNonEmpty(address.optString("suburb", ""), address.optString("city_district", ""), address.optString("neighbourhood", ""), address.optString("quarter", ""));
        String city = firstNonEmpty(address.optString("city", ""), address.optString("town", ""), address.optString("village", ""), address.optString("hamlet", ""), address.optString("locality", ""), address.optString("municipality", ""));
        StringBuilder out = new StringBuilder();
        appendDistinct(out, city);
        appendDistinct(out, district);
        appendDistinct(out, street);
        if (out.length() > 0) return out.toString();
        return shortenDisplayName(root.optString("display_name", ""));
    }

    private static String shortenDisplayName(String value) {
        if (value == null) return "";
        String[] parts = value.split(",");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(4, parts.length); i++) appendDistinct(out, parts[i]);
        return out.toString();
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private static void appendDistinct(StringBuilder out, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.isEmpty()) return;
        String existing = out.toString().toLowerCase(Locale.getDefault());
        if (existing.contains(clean.toLowerCase(Locale.getDefault()))) return;
        if (out.length() > 0) out.append(", ");
        out.append(clean);
    }

    public static WeatherSnapshot fetchWeather(Context context, double latitude, double longitude, String locationName) throws Exception {
        return fetchWeather(latitude, longitude, locationName, getLanguage(context));
    }

    public static WeatherSnapshot fetchWeather(double latitude, double longitude, String locationName) throws Exception {
        return fetchWeather(latitude, longitude, locationName, L10n.LANG_RU);
    }

    private static WeatherSnapshot fetchWeather(double latitude, double longitude, String locationName, String language) throws Exception {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover,is_day" +
                        "&hourly=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,precipitation_probability,weather_code,wind_speed_10m,wind_direction_10m,cloud_cover,surface_pressure" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset,uv_index_max,wind_speed_10m_max,wind_gusts_10m_max,daylight_duration" +
                        "&timezone=auto&forecast_days=10",
                latitude, longitude);

        JSONObject root = new JSONObject(httpGet(url));
        JSONObject current = root.getJSONObject("current");
        WeatherSnapshot snapshot = new WeatherSnapshot();
        snapshot.locationName = locationName;
        snapshot.latitude = latitude;
        snapshot.longitude = longitude;
        snapshot.time = current.optString("time", "");
        snapshot.temperature = current.optDouble("temperature_2m", Double.NaN);
        snapshot.apparentTemperature = current.optDouble("apparent_temperature", Double.NaN);
        snapshot.humidity = current.optInt("relative_humidity_2m", -1);
        snapshot.windSpeed = current.optDouble("wind_speed_10m", Double.NaN);
        snapshot.windDirection = current.optInt("wind_direction_10m", -1);
        snapshot.pressure = current.optDouble("surface_pressure", Double.NaN);
        snapshot.cloudCover = current.optInt("cloud_cover", -1);
        snapshot.isDay = current.optInt("is_day", -1);
        snapshot.precipitation = current.optDouble("precipitation", Double.NaN);
        snapshot.weatherCode = current.optInt("weather_code", -1);
        snapshot.updatedReadable = nowReadable();
        parseHourly(root.getJSONObject("hourly"), snapshot);
        parseDaily(root.getJSONObject("daily"), snapshot, language);
        return snapshot;
    }

    private static void parseHourly(JSONObject hourly, WeatherSnapshot snapshot) throws Exception {
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temps = hourly.getJSONArray("temperature_2m");
        JSONArray apparent = hourly.optJSONArray("apparent_temperature");
        JSONArray humidity = hourly.optJSONArray("relative_humidity_2m");
        JSONArray precip = hourly.optJSONArray("precipitation");
        JSONArray prob = hourly.optJSONArray("precipitation_probability");
        JSONArray codes = hourly.getJSONArray("weather_code");
        JSONArray wind = hourly.optJSONArray("wind_speed_10m");
        JSONArray windDir = hourly.optJSONArray("wind_direction_10m");
        JSONArray cloud = hourly.optJSONArray("cloud_cover");
        JSONArray pressure = hourly.optJSONArray("surface_pressure");
        int start = 0;
        if (snapshot.time != null && !snapshot.time.isEmpty()) {
            String currentHour = hourKey(snapshot.time);
            boolean found = false;
            for (int i = 0; i < times.length(); i++) {
                String value = times.optString(i, "");
                if (!currentHour.isEmpty() && value.startsWith(currentHour)) {
                    start = i;
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (int i = 0; i < times.length(); i++) {
                    String value = times.optString(i, "");
                    if (!value.isEmpty() && value.compareTo(snapshot.time) >= 0) {
                        start = Math.max(0, i - 1);
                        break;
                    }
                }
            }
        }
        int limit = Math.min(times.length(), start + 36);
        for (int i = start; i < limit; i++) {
            HourlyItem item = new HourlyItem();
            item.time = shortHour(times.optString(i, ""));
            item.rawTime = times.optString(i, "");
            item.temperature = temps.optDouble(i, Double.NaN);
            item.apparentTemperature = apparent == null ? Double.NaN : apparent.optDouble(i, Double.NaN);
            item.humidity = humidity == null ? -1 : humidity.optInt(i, -1);
            item.precipitation = precip == null ? Double.NaN : precip.optDouble(i, Double.NaN);
            item.precipitationProbability = prob == null ? -1 : prob.optInt(i, -1);
            item.weatherCode = codes.optInt(i, -1);
            item.windSpeed = wind == null ? Double.NaN : wind.optDouble(i, Double.NaN);
            item.windDirection = windDir == null ? -1 : windDir.optInt(i, -1);
            item.cloudCover = cloud == null ? -1 : cloud.optInt(i, -1);
            item.pressure = pressure == null ? Double.NaN : pressure.optDouble(i, Double.NaN);
            snapshot.hourly.add(item);
        }
    }

    private static String hourKey(String iso) {
        if (iso == null) return "";
        String value = iso.trim();
        return value.length() >= 13 ? value.substring(0, 13) : value;
    }

    private static void parseDaily(JSONObject daily, WeatherSnapshot snapshot, String language) throws Exception {
        JSONArray times = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray max = daily.getJSONArray("temperature_2m_max");
        JSONArray min = daily.getJSONArray("temperature_2m_min");
        JSONArray rain = daily.optJSONArray("precipitation_sum");
        JSONArray prob = daily.optJSONArray("precipitation_probability_max");
        JSONArray sunrise = daily.optJSONArray("sunrise");
        JSONArray sunset = daily.optJSONArray("sunset");
        JSONArray uv = daily.optJSONArray("uv_index_max");
        JSONArray wind = daily.optJSONArray("wind_speed_10m_max");
        JSONArray gust = daily.optJSONArray("wind_gusts_10m_max");
        JSONArray daylight = daily.optJSONArray("daylight_duration");
        int limit = Math.min(times.length(), 10);
        for (int i = 0; i < limit; i++) {
            DailyItem item = new DailyItem();
            item.date = L10n.dailyLabel(language, i, times.getString(i));
            item.rawDate = times.getString(i);
            item.weatherCode = codes.optInt(i, -1);
            item.temperatureMax = max.optDouble(i, Double.NaN);
            item.temperatureMin = min.optDouble(i, Double.NaN);
            item.precipitationSum = rain == null ? Double.NaN : rain.optDouble(i, Double.NaN);
            item.precipitationProbabilityMax = prob == null ? -1 : prob.optInt(i, -1);
            item.sunrise = sunrise == null ? "--" : shortHour(sunrise.optString(i, ""));
            item.sunset = sunset == null ? "--" : shortHour(sunset.optString(i, ""));
            item.uvIndexMax = uv == null ? Double.NaN : uv.optDouble(i, Double.NaN);
            item.windSpeedMax = wind == null ? Double.NaN : wind.optDouble(i, Double.NaN);
            item.windGustsMax = gust == null ? Double.NaN : gust.optDouble(i, Double.NaN);
            item.daylightDurationSeconds = daylight == null ? Double.NaN : daylight.optDouble(i, Double.NaN);
            snapshot.daily.add(item);
        }
    }

    public static void saveSnapshot(Context context, WeatherSnapshot snapshot) {
        SharedPreferences.Editor e = prefs(context).edit();
        e.putString("cache_city", displayLocationName(snapshot.locationName));
        e.putString("cache_updated", snapshot.updatedReadable);
        e.putString("cache_temp", formatTemp(context, snapshot.temperature));
        String lang = getLanguage(context);
        e.putString("cache_condition", weatherEmoji(snapshot.weatherCode) + " " + weatherLabel(lang, snapshot.weatherCode));
        e.putInt("cache_weather_code", snapshot.weatherCode);
        for (int i = 0; i < 8; i++) {
            String line = i < snapshot.hourly.size() ? snapshot.hourly.get(i).widgetLine(context) : "--";
            e.putString("cache_hourly_" + i, line);
        }
        for (int i = 0; i < 3; i++) {
            String line = i < snapshot.daily.size() ? snapshot.daily.get(i).compactLine(context) : "--";
            e.putString("cache_day_" + i, line);
        }
        e.putString("cache_today_day_night", snapshot.daily.isEmpty() ? "" : formatDayNightCompact(context, snapshot.daily.get(0)));
        e.putInt("cache_is_day", snapshot.isDay);
        e.putString("cache_moon_icon", moonPhaseIcon(snapshot.time));
        e.putString("last_raw_city", displayLocationName(snapshot.locationName));
        e.putLong("last_raw_lat", Double.doubleToRawLongBits(snapshot.latitude));
        e.putLong("last_raw_lon", Double.doubleToRawLongBits(snapshot.longitude));
        e.putString("last_raw_time", snapshot.time);
        e.putFloat("last_raw_temp", (float) snapshot.temperature);
        e.putInt("last_raw_code", snapshot.weatherCode);
        e.apply();
    }

    private static void rebuildCacheUsingLastRaw(Context context) {
        // Widgets are rebuilt on next weather refresh; this keeps the method lightweight and safe.
    }

    public static CachedSnapshot readCachedSnapshot(Context context) {
        SharedPreferences p = prefs(context);
        CachedSnapshot c = new CachedSnapshot();
        c.city = displayLocationName(p.getString("cache_city", p.getString(KEY_CITY, DEFAULT_CITY)));
        c.temp = p.getString("cache_temp", "--°");
        c.condition = p.getString("cache_condition", L10n.t(context, "open_app_to_update"));
        c.weatherCode = p.getInt("cache_weather_code", -1);
        c.updated = p.getString("cache_updated", "");
        c.todayDayNight = p.getString("cache_today_day_night", "");
        c.isDay = p.getInt("cache_is_day", 1);
        c.moonIcon = p.getString("cache_moon_icon", moonPhaseIcon(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())));
        for (int i = 0; i < c.hourly.length; i++) c.hourly[i] = p.getString("cache_hourly_" + i, "--");
        for (int i = 0; i < c.daily.length; i++) c.daily[i] = p.getString("cache_day_" + i, "--");
        return c;
    }

    private static String httpGet(String urlString) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "WeatherPro-Android/2.3");
            int status = connection.getResponseCode();
            InputStream in = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line);
            if (status < 200 || status >= 300) throw new IllegalStateException("HTTP " + status + ": " + out);
            return out.toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static String formatTemp(Context context, double celsius) {
        if (Double.isNaN(celsius)) return "--°";
        if (TEMP_F.equals(getTempUnit(context))) {
            return Math.round(celsius * 9 / 5 + 32) + "°F";
        }
        return Math.round(celsius) + "°";
    }

    public static String formatTempRange(Context context, double min, double max) {
        return formatTemp(context, min) + " / " + formatTemp(context, max);
    }

    public static String formatDayNight(Context context, DailyItem item) {
        if (item == null) return "--";
        return L10n.t(context, "day_temp") + " " + formatTemp(context, item.temperatureMax) + " · " + L10n.t(context, "night_temp") + " " + formatTemp(context, item.temperatureMin);
    }

    public static String formatDayNightCompact(Context context, DailyItem item) {
        if (item == null) return "--";
        return L10n.t(context, "day_short") + " " + formatTemp(context, item.temperatureMax) + " · " + L10n.t(context, "night_short") + " " + formatTemp(context, item.temperatureMin);
    }

    public static String dailyLabelWithDate(DailyItem item) {
        if (item == null) return "";
        String day = item.date == null ? "" : item.date.trim();
        String raw = item.rawDate == null ? "" : item.rawDate.trim();
        if (raw.length() >= 10) {
            String value = raw.substring(8, 10) + "." + raw.substring(5, 7);
            return day.isEmpty() ? value : day + " " + value;
        }
        return day;
    }

    public static String settlementName(String raw) {
        String clean = displayLocationName(raw);
        if (clean == null || clean.trim().isEmpty()) return DEFAULT_CITY;
        clean = clean.replace(" · ", ",");
        String[] parts = clean.split(",");
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (value.isEmpty() || isCoordinateLike(value) || isAccuracyLike(value) || isStreetLike(value)) continue;
            return value;
        }
        return parts.length == 0 ? DEFAULT_CITY : parts[0].trim();
    }

    private static boolean isStreetLike(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase(Locale.getDefault());
        return v.startsWith("улица ") || v.startsWith("ул. ") || v.startsWith("проспект ") || v.startsWith("пр-т ") || v.startsWith("переулок ") || v.startsWith("шоссе ") || v.startsWith("площадь ") || v.contains(" улица") || v.contains(" проспект") || v.contains(" street") || v.contains(" road");
    }

    public static String formatWind(Context context, double kmh) {
        if (Double.isNaN(kmh)) return "--";
        boolean cyrillic = L10n.LANG_RU.equals(getLanguage(context)) || L10n.LANG_BE.equals(getLanguage(context));
        if (WIND_MS.equals(getWindUnit(context))) return String.format(L10n.locale(context), "%.1f %s", kmh / 3.6, cyrillic ? "м/с" : "m/s");
        return Math.round(kmh) + (cyrillic ? " км/ч" : " km/h");
    }

    public static String formatPressure(Context context, double hpa) {
        if (Double.isNaN(hpa)) return "--";
        boolean cyrillic = L10n.LANG_RU.equals(getLanguage(context)) || L10n.LANG_BE.equals(getLanguage(context));
        if (PRESSURE_MMHG.equals(getPressureUnit(context))) return Math.round(hpa * 0.750062) + (cyrillic ? " мм рт. ст." : " mmHg");
        return Math.round(hpa) + (cyrillic ? " гПа" : " hPa");
    }

    public static String formatOneDecimal(double value, String suffix) {
        if (Double.isNaN(value)) return "--";
        return String.format(Locale.getDefault(), "%.1f%s", value, suffix);
    }

    public static String daylightLabel(Context context, double seconds) {
        if (Double.isNaN(seconds)) return "--";
        int totalMinutes = (int) Math.round(seconds / 60.0);
        return (totalMinutes / 60) + " " + L10n.t(context, "hour") + " " + (totalMinutes % 60) + " " + L10n.t(context, "min");
    }

    public static String daylightLabel(double seconds) {
        if (Double.isNaN(seconds)) return "--";
        int totalMinutes = (int) Math.round(seconds / 60.0);
        return (totalMinutes / 60) + " ч " + (totalMinutes % 60) + " мин";
    }

    public static String windDirectionLabel(int degrees) {
        if (degrees < 0) return "--";
        String[] labels = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
        return labels[(int) Math.round(((degrees % 360) / 45.0)) % 8];
    }

    public static String weatherEmoji(int code) {
        if (code == 0) return "☀️";
        if (code == 1 || code == 2) return "🌤";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "🌧";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 95) return "⛈";
        return "🌡";
    }

    public static String weatherEmojiFromCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) return "🌡";
        String clean = condition.trim();
        int cp = clean.codePointAt(0);
        if (!Character.isLetterOrDigit(cp)) return new String(Character.toChars(cp));
        return "🌡";
    }

    public static String weatherLabel(Context context, int code) {
        return weatherLabel(getLanguage(context), code);
    }

    public static String weatherLabel(String language, int code) {
        return L10n.weatherLabel(language, code);
    }

    public static String weatherLabel(int code) {
        return weatherLabel(L10n.LANG_RU, code);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /*
     * Kept for compatibility with old callers that expect the original Russian
     * labels. New UI should call weatherLabel(Context, int).
     */
    static String russianWeatherLabel(int code) {
        switch (code) {
            case 0: return "ясно";
            case 1: return "почти ясно";
            case 2: return "переменная облачность";
            case 3: return "пасмурно";
            case 45: case 48: return "туман";
            case 51: case 53: case 55: return "морось";
            case 56: case 57: return "ледяная морось";
            case 61: return "слабый дождь";
            case 63: return "дождь";
            case 65: return "сильный дождь";
            case 66: case 67: return "ледяной дождь";
            case 71: return "слабый снег";
            case 73: return "снег";
            case 75: return "сильный снег";
            case 77: return "снежная крупа";
            case 80: case 81: case 82: return "ливни";
            case 85: case 86: return "снегопад";
            case 95: return "гроза";
            case 96: case 99: return "гроза с градом";
            default: return "погода";
        }
    }

    public static String moonPhaseIcon(String isoDateTime) {
        int phase = moonPhaseIndex(isoDateTime);
        String[] icons = {"🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"};
        return icons[Math.max(0, Math.min(icons.length - 1, phase))];
    }

    public static int moonPhaseIndex(String isoDateTime) {
        try {
            String date = isoDateTime == null ? "" : isoDateTime.trim();
            if (date.length() >= 10) date = date.substring(0, 10);
            if (date.length() < 10) date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8, 10));
            if (month < 3) {
                year--;
                month += 12;
            }
            month++;
            double c = 365.25 * year;
            double e = 30.6 * month;
            double jd = c + e + day - 694039.09;
            jd /= 29.5305882;
            double fraction = jd - Math.floor(jd);
            int index = (int) Math.floor(fraction * 8.0 + 0.5) & 7;
            return index;
        } catch (Exception ignored) {
            return 1;
        }
    }

    public static String nowReadable() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    public static String shortHour(String iso) {
        if (iso == null || iso.length() < 16) return "--";
        return iso.substring(11, 16);
    }

    public static class SavedLocation {
        public final String name;
        public final double latitude;
        public final double longitude;
        public SavedLocation(String name, double latitude, double longitude) {
            this.name = name; this.latitude = latitude; this.longitude = longitude;
        }
    }

    public static final class CityResult extends SavedLocation {
        public CityResult(String name, double latitude, double longitude) { super(name, latitude, longitude); }
    }

    public static final class WeatherSnapshot {
        public String locationName;
        public double latitude;
        public double longitude;
        public String time;
        public double temperature;
        public double apparentTemperature;
        public int humidity;
        public double windSpeed;
        public int windDirection;
        public double pressure;
        public int cloudCover;
        public int isDay;
        public double precipitation;
        public int weatherCode;
        public String updatedReadable;
        public final List<HourlyItem> hourly = new ArrayList<>();
        public final List<DailyItem> daily = new ArrayList<>();
    }

    public static final class HourlyItem {
        public String time;
        public String rawTime;
        public double temperature;
        public double apparentTemperature;
        public int humidity;
        public double precipitation;
        public int precipitationProbability;
        public int weatherCode;
        public double windSpeed;
        public int windDirection;
        public int cloudCover;
        public double pressure;

        public String widgetLine(Context context) {
            String rain = precipitationProbability >= 0 ? precipitationProbability + "%" : "--";
            return time + "  " + weatherEmoji(weatherCode) + "  " + formatTemp(context, temperature) + "  💧" + rain + "  💨" + formatWind(context, windSpeed);
        }
    }

    public static final class DailyItem {
        public String date;
        public String rawDate;
        public int weatherCode;
        public double temperatureMax;
        public double temperatureMin;
        public double precipitationSum;
        public int precipitationProbabilityMax;
        public String sunrise;
        public String sunset;
        public double uvIndexMax;
        public double windSpeedMax;
        public double windGustsMax;
        public double daylightDurationSeconds;

        public String compactLine(Context context) {
            return dailyLabelWithDate(this) + ": " + weatherEmoji(weatherCode) + " " + formatDayNightCompact(context, this) + " · " + L10n.t(context, "rain") + " " + (precipitationProbabilityMax >= 0 ? precipitationProbabilityMax + "%" : "--");
        }
    }

    public static final class CachedSnapshot {
        public String city;
        public String temp;
        public String condition;
        public int weatherCode = -1;
        public String updated;
        public String todayDayNight;
        public int isDay = 1;
        public String moonIcon = "🌙";
        public final String[] hourly = new String[8];
        public final String[] daily = new String[3];
    }
}
