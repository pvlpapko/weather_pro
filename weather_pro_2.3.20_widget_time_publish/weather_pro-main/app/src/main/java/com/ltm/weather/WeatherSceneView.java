package com.ltm.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class WeatherSceneView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int weatherCode = 0;
    private boolean day = true;
    private double temperature = Double.NaN;

    public WeatherSceneView(Context context) { super(context); init(); }
    public WeatherSceneView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setWeather(int code, boolean isDay, double temp) {
        weatherCode = code;
        day = isDay;
        temperature = temp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float time = (SystemClock.uptimeMillis() % 60000L) / 1000f;
        drawSky(canvas, w, h);
        if (!day) drawStars(canvas, w, h, time);
        drawLightSource(canvas, w, h, time);
        drawCloudLayer(canvas, w, h, time);
        if (isRain()) drawRain(canvas, w, h, time);
        if (isSnow()) drawSnow(canvas, w, h, time);
        if (isStorm()) drawLightning(canvas, w, h, time);
        if (isFog()) drawFog(canvas, w, h, time);
        drawGroundGlow(canvas, w, h);
        postInvalidateOnAnimation();
    }

    private void drawSky(Canvas canvas, int w, int h) {
        int top;
        int bottom;
        if (!day) {
            top = Color.rgb(3, 7, 18);
            bottom = Color.rgb(20, 31, 60);
        } else if (isStorm() || isRain()) {
            top = Color.rgb(42, 55, 79);
            bottom = Color.rgb(99, 116, 139);
        } else if (isSnow()) {
            top = Color.rgb(147, 197, 253);
            bottom = Color.rgb(241, 245, 249);
        } else if (isFog()) {
            top = Color.rgb(148, 163, 184);
            bottom = Color.rgb(226, 232, 240);
        } else {
            top = Color.rgb(59, 130, 246);
            bottom = Color.rgb(186, 230, 253);
        }
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0, 0, w, h), dp(22), dp(22), paint);
        paint.setShader(null);
    }

    private void drawLightSource(Canvas canvas, int w, int h, float time) {
        if (day) drawRealSun(canvas, w, h, time); else drawRealMoon(canvas, w, h, time);
    }

    private void drawRealSun(Canvas canvas, int w, int h, float time) {
        float cx = w * 0.22f + wave(time, 7);
        float cy = h * 0.28f + wave(time * 0.7f, 4);
        float r = Math.min(w, h) * 0.14f;
        paint.setShader(new RadialGradient(cx, cy, r * 2.6f,
                new int[]{Color.argb(150, 254, 240, 138), Color.argb(75, 251, 191, 36), Color.TRANSPARENT},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 2.6f, paint);
        paint.setShader(null);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.argb(160, 254, 240, 138));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30 + time * 2);
            canvas.drawLine(cx + (float)Math.cos(a) * r * 1.25f, cy + (float)Math.sin(a) * r * 1.25f,
                    cx + (float)Math.cos(a) * r * 1.78f, cy + (float)Math.sin(a) * r * 1.78f, paint);
        }
        paint.setShader(new RadialGradient(cx - r * 0.28f, cy - r * 0.28f, r * 1.25f,
                Color.rgb(254, 249, 195), Color.rgb(245, 158, 11), Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);
    }

    private void drawRealMoon(Canvas canvas, int w, int h, float time) {
        float cx = w * 0.22f + wave(time, 5);
        float cy = h * 0.28f;
        float r = Math.min(w, h) * 0.14f;
        paint.setShader(new RadialGradient(cx, cy, r * 2.4f,
                Color.argb(95, 226, 232, 240), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 2.4f, paint);
        paint.setShader(null);
        paint.setColor(Color.rgb(226, 232, 240));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Color.rgb(15, 23, 42));
        canvas.drawCircle(cx + r * 0.42f, cy - r * 0.12f, r * 0.92f, paint);
        paint.setColor(Color.argb(70, 148, 163, 184));
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.18f, r * 0.10f, paint);
        canvas.drawCircle(cx - r * 0.12f, cy + r * 0.28f, r * 0.07f, paint);
    }

    private void drawStars(Canvas canvas, int w, int h, float time) {
        for (int i = 0; i < 28; i++) {
            float x = (i * 83 % 100) / 100f * w;
            float y = (i * 47 % 68) / 100f * h;
            float pulse = 0.55f + 0.45f * (float)Math.sin(time * 1.6f + i);
            paint.setColor(Color.argb((int)(95 + pulse * 120), 255, 255, 255));
            canvas.drawCircle(x, y, dp(1) + (i % 3) * 0.8f, paint);
        }
    }

    private void drawCloudLayer(Canvas canvas, int w, int h, float time) {
        boolean heavy = weatherCode == 3 || isRain() || isStorm() || isSnow() || isFog();
        drawCloud(canvas, w * 0.50f + wave(time * 0.55f, 18), h * 0.42f, w * (heavy ? 0.38f : 0.30f), 235);
        drawCloud(canvas, w * 0.74f - wave(time * 0.46f, 14), h * 0.61f, w * (heavy ? 0.30f : 0.23f), heavy ? 205 : 150);
        if (heavy) drawCloud(canvas, w * 0.25f + wave(time * 0.35f, 10), h * 0.58f, w * 0.26f, 150);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float size, int alpha) {
        paint.setColor(Color.argb(Math.max(50, alpha - 70), 15, 23, 42));
        canvas.drawOval(new RectF(cx - size * 0.55f, cy + size * 0.09f, cx + size * 0.65f, cy + size * 0.49f), paint);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        canvas.drawOval(new RectF(cx - size * 0.55f, cy, cx + size * 0.65f, cy + size * 0.42f), paint);
        canvas.drawCircle(cx - size * 0.28f, cy, size * 0.24f, paint);
        canvas.drawCircle(cx + size * 0.02f, cy - size * 0.10f, size * 0.31f, paint);
        canvas.drawCircle(cx + size * 0.35f, cy + size * 0.02f, size * 0.22f, paint);
    }

    private void drawRain(Canvas canvas, int w, int h, float time) {
        paint.setColor(Color.argb(205, 186, 230, 253));
        paint.setStrokeWidth(dp(2));
        for (int i = 0; i < 48; i++) {
            float x = ((i * 37) % 100) / 100f * w + wave(time + i, 10);
            float y = (((time * 260) + i * 43) % (h + 120)) - 60;
            canvas.drawLine(x, y, x - dp(10), y + dp(28), paint);
        }
    }

    private void drawSnow(Canvas canvas, int w, int h, float time) {
        paint.setColor(Color.argb(230, 255, 255, 255));
        paint.setStrokeWidth(dp(1));
        for (int i = 0; i < 45; i++) {
            float x = ((i * 47) % 100) / 100f * w + wave(time * 0.7f + i, 16);
            float y = (((time * 42) + i * 31) % (h + 50)) - 20;
            float r = dp(2 + (i % 3));
            canvas.drawCircle(x, y, r, paint);
        }
    }

    private void drawFog(Canvas canvas, int w, int h, float time) {
        paint.setColor(Color.argb(150, 255, 255, 255));
        paint.setStrokeWidth(dp(8));
        for (int i = 0; i < 6; i++) {
            float y = h * (0.38f + i * 0.105f);
            float offset = (time * 18 + i * 62) % (w * 1.4f);
            canvas.drawLine(-w + offset, y, offset, y, paint);
            canvas.drawLine(offset + dp(35), y + dp(5), w + offset, y + dp(5), paint);
        }
    }

    private void drawLightning(Canvas canvas, int w, int h, float time) {
        if (((int)(time * 4)) % 11 > 1) return;
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(4));
        paint.setColor(Color.rgb(254, 240, 138));
        Path path = new Path();
        path.moveTo(w * 0.62f, h * 0.25f);
        path.lineTo(w * 0.50f, h * 0.50f);
        path.lineTo(w * 0.60f, h * 0.48f);
        path.lineTo(w * 0.47f, h * 0.78f);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawGroundGlow(Canvas canvas, int w, int h) {
        paint.setColor(day ? Color.argb(55, 255, 255, 255) : Color.argb(34, 148, 163, 184));
        canvas.drawOval(new RectF(w * 0.03f, h * 0.78f, w * 0.97f, h * 1.18f), paint);
    }

    private boolean isRain() { return (weatherCode >= 51 && weatherCode <= 67) || (weatherCode >= 80 && weatherCode <= 82); }
    private boolean isSnow() { return (weatherCode >= 71 && weatherCode <= 77) || weatherCode == 85 || weatherCode == 86; }
    private boolean isStorm() { return weatherCode >= 95; }
    private boolean isFog() { return weatherCode == 45 || weatherCode == 48; }
    private float wave(float time, float amount) { return (float)Math.sin(time * 1.4f) * amount; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
