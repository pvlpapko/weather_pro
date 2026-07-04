package com.ltm.weather;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Locale;

public class MapActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (WeatherRepository.isDarkTheme(this)) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);

        double lat = getIntent().getDoubleExtra("lat", WeatherRepository.DEFAULT_LAT);
        double lon = getIntent().getDoubleExtra("lon", WeatherRepository.DEFAULT_LON);
        String city = getIntent().getStringExtra("city");
        if (city == null || city.isEmpty()) city = WeatherRepository.DEFAULT_CITY;
        city = WeatherRepository.displayLocationName(city);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(240, 246, 255));
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setPadding(0, 0, 0, 0);
        webView.setClipToPadding(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request != null && request.isForMainFrame()) {
                    view.loadDataWithBaseURL("https://weather-pro.local/", fallbackHtml(), "text/html", "UTF-8", null);
                }
            }
        });

        setContentView(webView);
        webView.loadDataWithBaseURL("https://tile.openstreetmap.org/", html(lat, lon, city), "text/html", "UTF-8", null);
        Toast.makeText(this, L10n.t(this, "rain_map_toast"), Toast.LENGTH_SHORT).show();
    }

    private int statusBarHeight() {
        int res = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return res > 0 ? getResources().getDimensionPixelSize(res) : 0;
    }

    private int navigationBarHeight() {
        int res = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return res > 0 ? getResources().getDimensionPixelSize(res) : 0;
    }

    private String html(double lat, double lon, String city) {
        String ready = jsString(L10n.t(this, "radar_ready"));
        String noFrames = jsString(L10n.t(this, "radar_no_frames"));
        String error = jsString(L10n.t(this, "radar_error"));
        String loadingJs = jsString(L10n.t(this, "radar_loading"));
        String cityText = htmlSafe(WeatherWidgetUpdater.formatWidgetLocation(city));
        String latText = String.format(Locale.US, "%.6f", lat);
        String lonText = String.format(Locale.US, "%.6f", lon);
        String safeTop = String.valueOf(Math.max(dp(4), statusBarHeight() + dp(4)));
        String safeBottom = String.valueOf(Math.max(dp(10), navigationBarHeight() + dp(10)));

        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
                    <style>
                        html, body {
                            height: 100%;
                            margin: 0;
                            background: #f0f6ff;
                            color: #0f172a;
                            font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;
                            overflow: hidden;
                            overscroll-behavior: none;
                            touch-action: none;
                            -webkit-user-select: none;
                            user-select: none;
                        }
                        #map { position: absolute; inset: 0; background: #eef6ff; overflow: hidden; touch-action: none; }
                        #base, #rain { position: absolute; inset: 0; overflow: hidden; touch-action: none; }
                        .tile { position: absolute; object-fit: cover; background: #eef6ff; will-change: transform; pointer-events: none; }
                        .rain { opacity: .82; pointer-events: none; }
                        #marker {
                            position: absolute; z-index: 8; width: 20px; height: 20px;
                            margin: -26px 0 0 -10px; background: #ef4444; border: 3px solid #fff;
                            border-radius: 50%; box-shadow: 0 3px 18px rgba(15,23,42,.50); pointer-events: none;
                        }
                        #marker:after {
                            content: ""; position: absolute; left: 6px; top: 15px;
                            border-left: 4px solid transparent; border-right: 4px solid transparent; border-top: 11px solid #ef4444;
                        }
                        .zoomControls {
                            position: absolute; z-index: 20; left: 10px; top: __SAFE_TOP__px;
                            display: flex; flex-direction: column; gap: 7px;
                        }
                        .mapBtn, .locBtn {
                            width: 40px; height: 40px; border: 0; border-radius: 13px;
                            background: rgba(255,255,255,.94); color: #0f172a;
                            font-size: 22px; font-weight: 900; box-shadow: 0 8px 20px rgba(15,23,42,.22);
                            backdrop-filter: blur(10px);
                        }
                        .mapBtn:active, .locBtn:active, .timeChip:active { transform: scale(.96); }
                        .locBtn { position: absolute; z-index: 20; right: 10px; top: __SAFE_TOP__px; font-size: 20px; color: #2563eb; }
                        .status {
                            position: absolute; z-index: 19; left: 60px; right: 60px; top: __SAFE_TOP__px;
                            min-height: 22px; padding: 7px 10px; border-radius: 14px;
                            background: rgba(255,255,255,.88); color: #1e3a8a; font-size: 12px; line-height: 1.2;
                            text-align: center; box-shadow: 0 8px 20px rgba(15,23,42,.12); backdrop-filter: blur(10px);
                            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
                        }
                        .timeline {
                            position: absolute; z-index: 20; left: 10px; right: 10px; bottom: __SAFE_BOTTOM__px;
                            padding: 8px 7px; border-radius: 18px; background: rgba(255,255,255,.92);
                            box-shadow: 0 8px 26px rgba(15,23,42,.20); backdrop-filter: blur(10px);
                            overflow-x: auto; overflow-y: hidden; white-space: nowrap; scrollbar-width: none;
                            -webkit-overflow-scrolling: touch; touch-action: pan-x;
                        }
                        .timeline::-webkit-scrollbar { display: none; }
                        .timeChip {
                            display: inline-flex; align-items: center; justify-content: center;
                            min-width: 68px; height: 34px; margin: 0 4px; padding: 0 9px;
                            border: 0; border-radius: 14px; background: #e2e8f0; color: #334155;
                            font-size: 12px; font-weight: 750;
                        }
                        .timeChip.active { background: #2563eb; color: #fff; box-shadow: 0 5px 14px rgba(37,99,235,.28); }
                        .timeChip.future { background: #dbeafe; color: #1d4ed8; }
                        .timeChip.future.active { background: #2563eb; color: #fff; }
                        @media (max-width: 360px) {
                            .mapBtn, .locBtn { width: 38px; height: 38px; border-radius: 12px; }
                            .status { left: 56px; right: 56px; font-size: 11px; }
                            .timeChip { min-width: 60px; height: 32px; font-size: 11px; }
                        }
                    </style>
                </head>
                <body>
                    <div id="map"><div id="base"></div><div id="rain"></div><div id="marker"></div></div>
                    <div class="zoomControls"><button class="mapBtn" onclick="zoomBy(1)">+</button><button class="mapBtn" onclick="zoomBy(-1)">−</button></div>
                    <button class="locBtn" onclick="goHome(true)">⌖</button>
                    <div id="status" class="status">__CITY__ · радар</div>
                    <div id="timeline" class="timeline"><div id="timeItems"></div></div>

                    <script>
                        const TILE = 256, MAX_BASE_ZOOM = 19, MAX_RAIN_ZOOM = 10;
                        const TXT_LOADING = '__LOADING_JS__', TXT_READY = '__READY_JS__', TXT_NO_FRAMES = '__NO_FRAMES_JS__', TXT_ERROR = '__ERROR_JS__';
                        const CITY = '__CITY_JS__', LAT0 = __LAT__, LON0 = __LON__;
                        const baseHosts = ['https://a.tile.openstreetmap.org','https://b.tile.openstreetmap.org','https://c.tile.openstreetmap.org'];
                        const map = document.getElementById('map'), base = document.getElementById('base'), rain = document.getElementById('rain'), marker = document.getElementById('marker');
                        const status = document.getElementById('status'), timeItems = document.getElementById('timeItems');
                        let z = 12, cx = lon2x(LON0, z), cy = lat2y(LAT0, z), host = '', frames = [], frameIndex = 0, raf = 0, animTimer = null;

                        function pow2(zoom){return Math.pow(2, zoom);} 
                        function lon2x(lon, zoom){return (lon + 180) / 360 * pow2(zoom);} 
                        function lat2y(lat, zoom){const r = lat * Math.PI / 180; return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2 * pow2(zoom);} 
                        function x2lon(x, zoom){return x / pow2(zoom) * 360 - 180;} 
                        function y2lat(y, zoom){const n = Math.PI - 2 * Math.PI * y / pow2(zoom); return 180 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));}
                        function centerLon(){return x2lon(cx, z);} function centerLat(){return y2lat(cy, z);} 
                        function clamp(v,min,max){return Math.max(min, Math.min(max, v));}
                        function wrapX(){const n = pow2(z); cx = ((cx % n) + n) % n; cy = clamp(cy, 0, n - .001);} 
                        function drawLayer(target, tpl, cls, layerZoom){
                            target.innerHTML = '';
                            const vw = Math.max(1, innerWidth), vh = Math.max(1, innerHeight);
                            const cLon = centerLon(), cLat = centerLat(), lcx = lon2x(cLon, layerZoom), lcy = lat2y(cLat, layerZoom);
                            const scale = Math.pow(2, z - layerZoom), tileSize = TILE * scale, n = pow2(layerZoom);
                            const minX = Math.floor(lcx - vw / 2 / tileSize) - 1, maxX = Math.ceil(lcx + vw / 2 / tileSize) + 1;
                            const minY = Math.floor(lcy - vh / 2 / tileSize) - 1, maxY = Math.ceil(lcy + vh / 2 / tileSize) + 1;
                            for (let y = minY; y <= maxY; y++) {
                                if (y < 0 || y >= n) continue;
                                for (let x = minX; x <= maxX; x++) {
                                    const xx = ((x % n) + n) % n;
                                    const img = new Image();
                                    img.className = 'tile ' + (cls || ''); img.decoding = 'async'; img.loading = 'eager'; img.referrerPolicy = 'no-referrer';
                                    img.onerror = function(){ this.style.display = 'none'; };
                                    img.src = tpl(xx, y, layerZoom);
                                    img.style.width = Math.ceil(tileSize) + 'px'; img.style.height = Math.ceil(tileSize) + 'px';
                                    img.style.left = Math.round((x - lcx) * tileSize + vw / 2) + 'px';
                                    img.style.top = Math.round((y - lcy) * tileSize + vh / 2) + 'px';
                                    target.appendChild(img);
                                }
                            }
                        }
                        function drawBase(){
                            drawLayer(base, function(x,y,zz){ return baseHosts[Math.abs(x+y)%baseHosts.length] + '/' + zz + '/' + x + '/' + y + '.png'; }, '', Math.min(z, MAX_BASE_ZOOM));
                        }
                        function drawRain(){
                            if (!host || !frames.length || !frames[frameIndex]) { rain.innerHTML = ''; return; }
                            const rz = Math.min(z, MAX_RAIN_ZOOM);
                            drawLayer(rain, function(x,y,zz){ return host + frames[frameIndex].path + '/256/' + zz + '/' + x + '/' + y + '/2/1_1.png'; }, 'rain', rz);
                        }
                        function updateMarker(){
                            const vw = Math.max(1, innerWidth), vh = Math.max(1, innerHeight);
                            const mx = (lon2x(LON0, z) - cx) * TILE + vw / 2, my = (lat2y(LAT0, z) - cy) * TILE + vh / 2;
                            marker.style.left = Math.round(mx) + 'px'; marker.style.top = Math.round(my) + 'px';
                            marker.style.display = (mx > -80 && mx < vw + 80 && my > -80 && my < vh + 80) ? 'block' : 'none';
                        }
                        function requestDraw(){ if (raf) return; raf = requestAnimationFrame(function(){ raf = 0; wrapX(); drawBase(); drawRain(); updateMarker(); }); }
                        function zoomBy(delta, anchorX, anchorY){
                            const oldZ = z, newZ = clamp(z + delta, 4, MAX_BASE_ZOOM); if (newZ === oldZ) return;
                            const vw = Math.max(1, innerWidth), vh = Math.max(1, innerHeight);
                            const ax = typeof anchorX === 'number' ? anchorX : vw/2, ay = typeof anchorY === 'number' ? anchorY : vh/2;
                            const worldX = cx + (ax - vw/2) / TILE, worldY = cy + (ay - vh/2) / TILE;
                            const anchorLon = x2lon(worldX, oldZ), anchorLat = y2lat(worldY, oldZ);
                            z = newZ; cx = lon2x(anchorLon, z) - (ax - vw/2) / TILE; cy = lat2y(anchorLat, z) - (ay - vh/2) / TILE; requestDraw();
                        }
                        function goHome(zoomClose){ if (zoomClose) z = 18; cx = lon2x(LON0, z); cy = lat2y(LAT0, z); requestDraw(); }
                        function frameLabel(frame){
                            if (!frame || !frame.time) return '--:--';
                            return new Date(frame.time * 1000).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
                        }
                        function setFrame(index, scrollIntoView){
                            if (!frames.length) return; frameIndex = clamp(index, 0, frames.length - 1); drawRain(); renderTimeline(scrollIntoView); status.textContent = CITY + ' · ' + TXT_READY + ' · ' + frameLabel(frames[frameIndex]);
                        }
                        function renderTimeline(scrollIntoView){
                            timeItems.innerHTML = '';
                            frames.forEach(function(frame, index){
                                const b = document.createElement('button');
                                b.className = 'timeChip' + (index === frameIndex ? ' active' : '') + ((frame.time * 1000) > Date.now() + 5*60000 ? ' future' : '');
                                b.textContent = frameLabel(frame); b.onclick = function(){ stopAnimation(); setFrame(index, true); };
                                timeItems.appendChild(b);
                                if (scrollIntoView && index === frameIndex) setTimeout(function(){ b.scrollIntoView({inline:'center', block:'nearest', behavior:'smooth'}); }, 30);
                            });
                        }
                        function startAnimation(){
                            stopAnimation();
                            animTimer = setInterval(function(){ if (!frames.length) return; setFrame(frameIndex >= frames.length - 1 ? 0 : frameIndex + 1, true); }, 850);
                        }
                        function stopAnimation(){ if (animTimer) { clearInterval(animTimer); animTimer = null; } }
                        function loadRain(){
                            status.textContent = CITY + ' · ' + TXT_LOADING;
                            fetch('https://api.rainviewer.com/public/weather-maps.json?ts=' + Date.now(), {cache:'no-store'})
                                .then(function(r){ return r.json(); })
                                .then(function(d){
                                    const past = d && d.radar && d.radar.past ? d.radar.past : [];
                                    const nowcast = d && d.radar && d.radar.nowcast ? d.radar.nowcast : [];
                                    frames = past.concat(nowcast);
                                    if (!frames.length) { status.textContent = CITY + ' · ' + TXT_NO_FRAMES; rain.innerHTML = ''; timeItems.innerHTML = ''; return; }
                                    host = d.host || 'https://tilecache.rainviewer.com'; frameIndex = past.length > 0 ? past.length - 1 : frames.length - 1;
                                    renderTimeline(true); drawRain(); status.textContent = CITY + ' · ' + TXT_READY + ' · ' + frameLabel(frames[frameIndex]); startAnimation();
                                })
                                .catch(function(){ status.textContent = CITY + ' · ' + TXT_ERROR; });
                        }
                        let drag = null;
                        function touchList(e){ const out=[]; for(let i=0;i<e.touches.length;i++) out.push({x:e.touches[i].clientX,y:e.touches[i].clientY}); return out; }
                        function distance(a,b){ const dx=a.x-b.x, dy=a.y-b.y; return Math.sqrt(dx*dx+dy*dy); }
                        function midpoint(a,b){ return {x:(a.x+b.x)/2,y:(a.y+b.y)/2}; }
                        map.addEventListener('touchstart', function(e){
                            if(!e.touches.length) return; e.preventDefault(); const t=touchList(e);
                            if(t.length===1) drag={mode:'drag',x:t[0].x,y:t[0].y,cx:cx,cy:cy}; else { const mid=midpoint(t[0],t[1]); drag={mode:'pinch',cx:cx,cy:cy,mid:mid,dist:Math.max(20,distance(t[0],t[1]))}; }
                        }, {passive:false});
                        map.addEventListener('touchmove', function(e){
                            if(!drag) return; e.preventDefault(); const t=touchList(e);
                            if(drag.mode==='drag' && t.length===1){ cx=drag.cx-(t[0].x-drag.x)/TILE; cy=drag.cy-(t[0].y-drag.y)/TILE; requestDraw(); }
                            else if(t.length>=2){ const mid=midpoint(t[0],t[1]), dist=Math.max(20,distance(t[0],t[1])); cx=drag.cx-(mid.x-drag.mid.x)/TILE; cy=drag.cy-(mid.y-drag.mid.y)/TILE; const scale=dist/drag.dist; if(scale>1.16){ zoomBy(1,mid.x,mid.y); drag={mode:'pinch',cx:cx,cy:cy,mid:mid,dist:dist}; } else if(scale<.86){ zoomBy(-1,mid.x,mid.y); drag={mode:'pinch',cx:cx,cy:cy,mid:mid,dist:dist}; } else requestDraw(); }
                        }, {passive:false});
                        map.addEventListener('touchend', function(e){ if(e.touches.length===0) drag=null; }, {passive:false});
                        map.addEventListener('wheel', function(e){ e.preventDefault(); zoomBy(e.deltaY < 0 ? 1 : -1, e.clientX, e.clientY); }, {passive:false});
                        let mouse=null;
                        map.addEventListener('mousedown', function(e){ mouse={x:e.clientX,y:e.clientY,cx:cx,cy:cy}; });
                        window.addEventListener('mousemove', function(e){ if(!mouse) return; cx=mouse.cx-(e.clientX-mouse.x)/TILE; cy=mouse.cy-(e.clientY-mouse.y)/TILE; requestDraw(); });
                        window.addEventListener('mouseup', function(){ mouse=null; });
                        window.addEventListener('resize', requestDraw);
                        requestDraw(); setTimeout(loadRain, 150);
                    </script>
                </body>
                </html>
                """
                .replace("__SAFE_TOP__", safeTop)
                .replace("__SAFE_BOTTOM__", safeBottom)
                .replace("__CITY__", cityText)
                .replace("__CITY_JS__", jsString(WeatherWidgetUpdater.formatWidgetLocation(city)))
                .replace("__LOADING_JS__", loadingJs)
                .replace("__READY_JS__", ready)
                .replace("__NO_FRAMES_JS__", noFrames)
                .replace("__ERROR_JS__", error)
                .replace("__LAT__", latText)
                .replace("__LON__", lonText);
    }

    private String fallbackHtml() {
        String title = htmlSafe(L10n.t(this, "radar_title"));
        String error = htmlSafe(L10n.t(this, "radar_error"));
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>html,body{height:100%;margin:0;background:#f0f6ff;color:#0f172a;font-family:system-ui,Arial;display:flex;align-items:center;justify-content:center}.box{margin:18px;padding:18px;border-radius:18px;background:#fff;border:1px solid #bfdbfe;box-shadow:0 12px 34px rgba(15,23,42,.16)}.title{font-size:18px;font-weight:800}.sub{margin-top:8px;color:#1d4ed8}</style></head><body><div class='box'><div class='title'>" + title + "</div><div class='sub'>" + error + "</div></div></body></html>";
    }

    private String htmlSafe(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", " ");
    }

    private String jsString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("</", "<\\/");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
