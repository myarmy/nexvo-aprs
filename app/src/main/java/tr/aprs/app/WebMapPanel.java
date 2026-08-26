package tr.aprs.app;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.view.View;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes3.dex */
final class WebMapPanel extends WebView {
    private boolean loaded;
    private int reloadAttempts;
    private SelectionListener selectionListener;
    private final LinkedHashMap<String, PendingStation> pendingStations = new LinkedHashMap<>();
    private boolean stationFlushScheduled;

    interface SelectionListener {
        void onSelected(String str);
    }

    interface ManualMapListener { void onManual(); }
    private ManualMapListener manualMapListener;

    WebMapPanel(Context context) {
        super(context);
        setBackgroundColor(-15394791);
        // Map tiles and markers are continuously updated. Hardware composition
        // prevents the software WebView layer from dropping its tile bitmap
        // after a DMR station recenters the map.
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setOffscreenPreRaster(true);
        addJavascriptInterface(new Bridge(), "Android");
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient() { // from class: tr.aprs.app.WebMapPanel.1
            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView view, String url) {
                WebMapPanel.this.loaded = true;
                WebMapPanel.this.reloadAttempts = 0;
                WebMapPanel.this.refreshViewport();
                WebMapPanel.this.scheduleStationFlush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame() && WebMapPanel.this.reloadAttempts < 3) {
                    WebMapPanel.this.loaded = false;
                    WebMapPanel.this.reloadAttempts++;
                    WebMapPanel.this.postDelayed(() -> WebMapPanel.this.reload(), 1500L * WebMapPanel.this.reloadAttempts);
                }
            }
        });
        loadUrl("file:///android_asset/map.html");
    }

    void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        // The dashboard is rebuilt after a TG change. Leaflet may finish loading
        // while this WebView still has its previous/zero size, leaving a black
        // map until another control causes a layout pass.
        refreshViewport();
    }

    private void refreshViewport() {
        postDelayed(() -> evaluateJavascript(
                "if(typeof map!=='undefined'){map.invalidateSize({animate:false,pan:false});}",
                null), 350L);
    }
    void setManualMapListener(ManualMapListener listener) { this.manualMapListener = listener; }

    void updateStation(String name, double lat, double lng, boolean self) {
        updateStationDetails(name, lat, lng, self, name, "APRS", "AUTO");
    }

    void updateStation(String name, double lat, double lng, boolean self, boolean moving) {
        updateStationDetails(name, lat, lng, self, name, "APRS", moving ? "CAR" : "HOUSE");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX INFO: renamed from: updateStationDetails, reason: merged with bridge method [inline-methods] */
    public void lambda$updateStationDetails$0(final String name, final double lat, final double lng, final boolean self, final String detail) { updateStationDetails(name, lat, lng, self, detail, "APRS", "AUTO"); }
    void updateStationDetails(final String name, final double lat, final double lng, final boolean self, final String detail, final boolean moving) { updateStationDetails(name, lat, lng, self, detail, "APRS", moving ? "CAR" : "HOUSE"); }
    void updateDmrStation(final String name, final double lat, final double lng, final String detail) { updateStationDetails(name, lat, lng, false, detail, "DMR", "REPEATER"); }
    private void updateStationDetails(final String name, final double lat, final double lng, final boolean self, final String detail, final String type, final String kind) {
        String safe = name.replace("\\", "").replace("'", "");
        String safeDetail = detail.replace("\\", "").replace("'", "").replace("\r", "").replace("\n", "<br>");
        // APRS-IS can deliver hundreds of packets per second. Keep only the
        // newest position for each station and send one compact JS batch. This
        // prevents the WebView main thread from being flooded and freezing.
        this.pendingStations.put(safe, new PendingStation(safe, lat, lng, self, safeDetail, type, kind));
        scheduleStationFlush();
    }

    private void scheduleStationFlush() {
        if (!this.loaded || this.stationFlushScheduled || this.pendingStations.isEmpty()) return;
        this.stationFlushScheduled = true;
        postDelayed(new Runnable() {
            @Override public void run() { flushStations(); }
        }, 200L);
    }

    private void flushStations() {
        this.stationFlushScheduled = false;
        if (!this.loaded || this.pendingStations.isEmpty()) return;
        StringBuilder js = new StringBuilder("(function(){");
        int count = 0;
        java.util.Iterator<Map.Entry<String, PendingStation>> iterator = this.pendingStations.entrySet().iterator();
        while (iterator.hasNext() && count < 50) {
            PendingStation s = iterator.next().getValue();
            iterator.remove();
            js.append("updateStation('").append(s.name).append("',").append(s.lat).append(',').append(s.lng)
                    .append(',').append(s.self).append(",'" ).append(s.detail).append("','")
                    .append(s.type).append("','").append(s.kind).append("');");
            count++;
        }
        js.append("})()");
        evaluateJavascript(js.toString(), null);
        if (!this.pendingStations.isEmpty()) scheduleStationFlush();
    }

    private static final class PendingStation {
        final String name, detail, type, kind;
        final double lat, lng;
        final boolean self;
        PendingStation(String name, double lat, double lng, boolean self, String detail, String type, String kind) {
            this.name = name; this.lat = lat; this.lng = lng; this.self = self;
            this.detail = detail; this.type = type; this.kind = kind;
        }
    }

    void center(double lat, double lng) {
        evaluateJavascript("centerMap(" + lat + "," + lng + ",12)", null);
    }
    void setFollowMode(final String mode) { if (!loaded) { postDelayed(() -> setFollowMode(mode), 500L); return; } evaluateJavascript("setFollowMode('" + mode + "')", null); }
    void setLayers(final boolean aprs, final boolean dmr) { if (!loaded) { postDelayed(() -> setLayers(aprs, dmr), 500L); return; } evaluateJavascript("setLayers(" + aprs + "," + dmr + ")", null); }
    void setFilters(final int distanceKm, final int ageMinutes) { if (!loaded) { postDelayed(() -> setFilters(distanceKm, ageMinutes), 500L); return; } evaluateJavascript("setFilters(" + distanceKm + "," + ageMinutes + ")", null); }
    void pinStation(final String name, final boolean center) {
        if (!loaded) { postDelayed(() -> pinStation(name, center), 500L); return; }
        String safe = name.replace("\\", "").replace("'", "");
        evaluateJavascript("pinStation('" + safe + "'," + center + ")", null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class Bridge {
        private Bridge() {
        }

        @JavascriptInterface
        public void onStationSelected(final String callsign) {
            WebMapPanel.this.post(new Runnable() { // from class: tr.aprs.app.WebMapPanel$Bridge$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Bridge.this.lambda$onStationSelected$0(callsign);
                }
            });
        }

        @JavascriptInterface public void onManualMap() { WebMapPanel.this.post(() -> { if (manualMapListener != null) manualMapListener.onManual(); }); }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStationSelected$0(String callsign) {
            if (WebMapPanel.this.selectionListener != null) {
                WebMapPanel.this.selectionListener.onSelected(callsign);
            }
        }
    }
}
