package tr.aprs.app;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/* JADX INFO: loaded from: classes3.dex */
final class WebMapPanel extends WebView {
    private boolean loaded;
    private SelectionListener selectionListener;

    interface SelectionListener {
        void onSelected(String str);
    }

    interface ManualMapListener { void onManual(); }
    private ManualMapListener manualMapListener;

    WebMapPanel(Context context) {
        super(context);
        setBackgroundColor(-15394791);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAllowFileAccess(true);
        addJavascriptInterface(new Bridge(), "Android");
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient() { // from class: tr.aprs.app.WebMapPanel.1
            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView view, String url) {
                WebMapPanel.this.loaded = true;
            }
        });
        loadUrl("file:///android_asset/map.html");
    }

    void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }
    void setManualMapListener(ManualMapListener listener) { this.manualMapListener = listener; }

    void updateStation(String name, double lat, double lng, boolean self) {
        updateStationDetails(name, lat, lng, self, name, "APRS");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX INFO: renamed from: updateStationDetails, reason: merged with bridge method [inline-methods] */
    public void lambda$updateStationDetails$0(final String name, final double lat, final double lng, final boolean self, final String detail) { updateStationDetails(name, lat, lng, self, detail, "APRS"); }
    void updateDmrStation(final String name, final double lat, final double lng, final String detail) { updateStationDetails(name, lat, lng, false, detail, "DMR"); }
    private void updateStationDetails(final String name, final double lat, final double lng, final boolean self, final String detail, final String type) {
        if (!this.loaded) {
            postDelayed(new Runnable() { // from class: tr.aprs.app.WebMapPanel$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WebMapPanel.this.updateStationDetails(name, lat, lng, self, detail, type);
                }
            }, 500L);
            return;
        }
        String safe = name.replace("\\", "").replace("'", "");
        String safeDetail = detail.replace("\\", "").replace("'", "").replace("\r", "").replace("\n", "<br>");
        evaluateJavascript("updateStation('" + safe + "'," + lat + "," + lng + "," + self + ",'" + safeDetail + "','" + type + "')", null);
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
