package tr.aprs.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public final class MainActivity extends Activity {
    private TextView activeTalker;
    private AprsIsClient aprs;
    private BrandMeisterClient brandMeister;
    private String callsign;
    private boolean compact;
    private String dmrId;
    private boolean home;
    private boolean mainMenuVisible;
    private TextView ledCall;
    private TextView ledRange;
    private WebMapPanel map;
    private TextView metricAltitude;
    private TextView metricLast;
    private TextView metricSpeed;
    private TextView rxState;
    private TextView signalPanel;
    private SecureStore secureStore;
    private NexvoApiClient nexvo;
    private FeatureStore featureStore;
    private TextView selectedCard;
    private int stationCount;
    private boolean reconnectPending;
    private boolean destroyed;
    private boolean tablet;
    private boolean denseUi;
    private String talkGroup;
    private String selectedStationName = "";
    private double selectedStationLat = Double.NaN;
    private double selectedStationLng = Double.NaN;
    private final ArrayList<TalkerEntry> talkerHistory = new ArrayList<>();
    private WebView tgAudioWebView;
    private Button tgListenButton;
    private boolean tgListening;
    private boolean tgKeepAliveScheduled;
    private final HashMap<String, AprsPacketParser.Packet> pendingAprsPackets = new HashMap<>();
    private boolean aprsUiFlushScheduled;

    /* JADX INFO: renamed from: BG */
    private static final int f15BG = Color.rgb(21, 24, 25);
    private static final int PANEL = Color.rgb(36, 40, 43);
    private static final int PANEL_2 = Color.rgb(48, 54, 58);
    private static final int GREEN = Color.rgb(57, 217, 138);
    private static final int TEXT = Color.rgb(238, 242, 239);
    private static final int MUTED = Color.rgb(159, 170, 166);
    private static final int LINE = Color.rgb(68, 77, 80);
    private static final int WARN = Color.rgb(255, 193, 7);

    @Override // android.app.Activity
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        this.secureStore = new SecureStore(this);
        this.nexvo = new NexvoApiClient();
        this.featureStore = new FeatureStore(this);
        this.aprs = new AprsIsClient();
        this.brandMeister = new BrandMeisterClient();
        this.callsign = getPreferences(0).getString("callsign", "");
        this.dmrId = getPreferences(0).getString("dmr_id", "");
        this.talkGroup = getPreferences(0).getString("talkgroup", "28642");
        if (getPreferences(0).getBoolean("app_lock", false)) requestDeviceUnlock();
        int w = getResources().getConfiguration().screenWidthDp;
        int h = getResources().getConfiguration().screenHeightDp;
        this.compact = Math.min(w, h) <= 360 || h <= 320;
        this.tablet = w >= 600;
        this.denseUi = this.compact || h <= 600;
        if (this.callsign.isEmpty()) {
            showSetup();
        } else {
            showDashboard();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSetup() {
        this.home = false;
        LinearLayout body = column();
        body.setPadding(m11dp(20), m11dp(18), m11dp(20), m11dp(24));
        body.addView(title("Nexvo APRS", 30));
        if (!this.callsign.isEmpty()) {
            Button back = action("‹ GERİ • ANA MENÜ", PANEL_2);
            back.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    MainActivity.this.showMainMenu();
                }
            });
            body.addView(back);
        }
        body.addView(info("İLK KURULUM • ANDROID 7+", GREEN));
        final EditText call = field("Çağrı işareti (TA1ABC-9)", false);
        final EditText dmr = field("DMR ID (7 haneli)", false);
        final EditText tg = field("Sabit TG (ör. 28642)", false);
        final EditText key = field("aprs.fi API anahtarı", true);
        final EditText pass = field("APRS-IS passcode (gönderim için)", true);
        final EditText nexvoToken = field("Nexvo sunucu anahtarı", true);
        call.setText(this.callsign);
        dmr.setText(this.dmrId);
        tg.setText(this.talkGroup);
        dmr.setInputType(2);
        tg.setInputType(2);
        body.addView(label("ÇAĞRI İŞARETİ"));
        body.addView(call);
        body.addView(label("DMR NUMARASI"));
        body.addView(dmr);
        body.addView(label("SABİT TALK GROUP"));
        body.addView(tg);
        body.addView(label("APRS.FI API ANAHTARI"));
        body.addView(key);
        body.addView(label("APRS-IS PASSCODE"));
        body.addView(pass);
        body.addView(label("NEXVO ÇEVRİMDIŞI MESAJ ANAHTARI"));
        body.addView(nexvoToken);
        body.addView(info("Anahtar cihazın güvenli anahtar deposunda saklanır.", MUTED));
        final TextView setupStatus = info("Çağrı işareti zorunludur. Örnek: TA1ABC-9", WARN);
        body.addView(setupStatus);
        Button permission = action("KONUM İZNİ VER", PANEL_2);
        permission.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda28
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showSetup$0(view);
            }
        });
        body.addView(permission);
        Button save = action("KAYDET VE HARİTAYI AÇ", GREEN);
        save.setTextColor(f15BG);
        save.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda29
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.saveSetup(call, key, pass, nexvoToken, dmr, tg, setupStatus);
            }
        });
        body.addView(save);
        body.addView(info("APRS konum ve mesajları kamusal ağda görülebilir.", WARN));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(f15BG);
        scroll.addView(body);
        setContentView(scroll);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSetup$0(View v) {
        requestLocationPermission();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSetup$1(EditText call, EditText key, EditText pass, EditText dmr, EditText tg, TextView setupStatus, View v) {
        saveSetup(call, key, pass, null, dmr, tg, setupStatus);
    }

    private void saveSetup(EditText call, EditText key, EditText pass, EditText nexvoToken, EditText dmr, EditText tg, TextView setupStatus) {
        String c = call.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!c.matches("[A-Z0-9]{3,6}(-[A-Z0-9]{1,2})?")) {
            setupStatus.setText("HATA: Geçerli çağrı işareti girin. Örnek: TA1ABC-9");
            setupStatus.setTextColor(WARN);
            call.requestFocus();
            return;
        }
        try {
            String k = key.getText().toString().trim();
            if (!k.isEmpty()) {
                this.secureStore.saveApiKey(k);
            }
            String pc = pass.getText().toString().trim();
            if (!pc.isEmpty()) {
                this.secureStore.saveAprsPasscode(pc);
            }
            if (nexvoToken != null) {
                String nt = nexvoToken.getText().toString().trim();
                if (!nt.isEmpty()) this.secureStore.saveNexvoToken(nt);
            }
            this.callsign = c;
            this.dmrId = dmr.getText().toString().trim();
            this.talkGroup = tg.getText().toString().trim();
            if (this.talkGroup.isEmpty()) {
                this.talkGroup = "28642";
            }
            getPreferences(0).edit().putString("callsign", this.callsign).putString("dmr_id", this.dmrId).putString("talkgroup", this.talkGroup).apply();
            setupStatus.setText("Ayarlar kaydedildi. Harita açılıyor…");
            setupStatus.setTextColor(GREEN);
            toast("Profil ve APRS anahtarları kaydedildi.");
            showDashboard();
            if (!this.dmrId.isEmpty() && !this.secureStore.readNexvoToken().isEmpty()
                    && !this.secureStore.readAprsPasscode().isEmpty()) {
                this.nexvo.registerProfile(this.secureStore.readNexvoToken(), this.callsign,
                        this.secureStore.readAprsPasscode(), this.dmrId, this.callsign,
                        this.talkGroup, true, new NexvoApiClient.Callback<JSONObject>() {
                    public void onSuccess(JSONObject ignored) { }
                    public void onError(String ignored) { }
                });
            }
        } catch (Exception e) {
            setupStatus.setText("KAYIT HATASI: " + (e.getMessage() == null ? "Cihaz anahtar deposu kullanılamıyor." : e.getMessage()));
            setupStatus.setTextColor(WARN);
            toast("Ayarlar kaydedilemedi.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDashboard() {
        this.home = true;
        this.mainMenuVisible = false;
        LinearLayout root = column();
        root.addView(radioHeader());
        if (this.compact) {
            root.addView(compactBody(), verticalWeight(1));
        } else if (this.tablet) {
            LinearLayout split = row();
            split.addView(createMap(), new LinearLayout.LayoutParams(0, -1, 2.0f));
            split.addView(stationPanel(), new LinearLayout.LayoutParams(0, -1, 1.0f));
            root.addView(split, verticalWeight(1));
        } else {
            LinearLayout center = column();
            center.addView(createMap(), verticalWeight(1));
            this.selectedCard = stationCard();
            center.addView(this.selectedCard);
            addTgListenControl(center);
            root.addView(center, verticalWeight(1));
        }
        root.addView(bottomBar());
        setContentView(root);
        startReception();
    }

    private View radioHeader() {
        LinearLayout wrap = column();
        wrap.setBackgroundColor(f15BG);
        LinearLayout status = row();
        status.setGravity(16);
        status.setPadding(m11dp(8), m11dp(this.denseUi ? 2 : 4), m11dp(6), m11dp(2));
        status.addView(text("SİNYAL  ▂▄▆█", this.compact ? 10 : 12, GREEN, true), weight(1));
        this.rxState = text("GPS ●  APRS ●", this.compact ? 9 : 11, GREEN, true);
        status.addView(this.rxState);
        status.addView(text("  3.92V  " + new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date()), this.compact ? 9 : 11, TEXT, false));
        Button menu = smallButton("MENÜ");
        menu.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda31
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$radioHeader$2(view);
            }
        });
        status.addView(menu);
        wrap.addView(status);
        Location currentLocation = getBestLastLocation();
        String gpsQuality = currentLocation == null ? "GPS YOK" : "GPS ±" + Math.round(currentLocation.hasAccuracy() ? currentLocation.getAccuracy() : 0) + "m";
        this.signalPanel = text(gpsQuality + "   NET ◌   RX 0   TX —", this.compact ? 9 : 11, TEXT, true);
        this.signalPanel.setPadding(m11dp(8), m11dp(2), m11dp(8), m11dp(this.denseUi ? 2 : 4));
        this.signalPanel.setBackgroundColor(PANEL);
        wrap.addView(this.signalPanel);
        LinearLayout callRow = row();
        callRow.setGravity(16);
        callRow.setPadding(m11dp(8), m11dp(1), m11dp(6), m11dp(1));
        LinearLayout leds = column();
        this.ledCall = led(this.callsign.isEmpty() ? "N0CALL-9" : this.callsign, this.compact ? 22 : (this.denseUi ? 26 : 30));
        this.ledRange = led("--.- KM   ---°", this.compact ? 16 : (this.denseUi ? 18 : 22));
        leds.addView(this.ledCall);
        leds.addView(this.ledRange);
        callRow.addView(leds, weight(1));
        Button tg = smallButton("TG " + this.talkGroup);
        tg.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda32
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$radioHeader$3(view);
            }
        });
        tg.setOnLongClickListener(new View.OnLongClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda34
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view) {
                return MainActivity.this.lambda$radioHeader$4(view);
            }
        });
        callRow.addView(tg);
        wrap.addView(callRow);
        LinearLayout metrics = row();
        this.metricAltitude = metric("İRTİFA", "--- m");
        this.metricSpeed = metric("HIZ", "--- km/sa");
        this.metricLast = metric("SON", "--");
        metrics.addView(this.metricAltitude, weight(1));
        metrics.addView(this.metricSpeed, weight(1));
        metrics.addView(this.metricLast, weight(1));
        wrap.addView(metrics);
        this.activeTalker = text("TG " + this.talkGroup + " • AKTİF KONUŞMACI BEKLENİYOR", this.compact ? 9 : 11, MUTED, true);
        this.activeTalker.setGravity(17);
        this.activeTalker.setPadding(m11dp(5), m11dp(this.denseUi ? 2 : 4), m11dp(5), m11dp(this.denseUi ? 2 : 4));
        this.activeTalker.setMaxLines(this.denseUi ? 3 : 4);
        this.activeTalker.setBackgroundColor(PANEL_2);
        this.activeTalker.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda35
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$radioHeader$5(view);
            }
        });
        String savedTalkerTg = getPreferences(0).getString("last_talker_tg", "");
        String savedTalkerText = getPreferences(0).getString("last_talker_text", "");
        if (this.talkGroup.equals(savedTalkerTg) && !savedTalkerText.isEmpty()) {
            this.activeTalker.setText(savedTalkerText);
            this.activeTalker.setTextColor(GREEN);
        }
        wrap.addView(this.activeTalker);
        return wrap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$radioHeader$2(View v) {
        showMainMenu();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$radioHeader$3(View v) {
        showTalkGroupDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$radioHeader$4(View v) {
        openHoseline();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$radioHeader$5(View v) {
        showTalkerHistory();
    }

    private View createMap() {
        this.map = new WebMapPanel(this);
        this.map.setSelectionListener(new WebMapPanel.SelectionListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda26
            @Override // tr.aprs.app.WebMapPanel.SelectionListener
            public final void onSelected(String str) {
                MainActivity.this.selectStation(str);
            }
        });
        this.map.setManualMapListener(new WebMapPanel.ManualMapListener() {
            @Override public void onManual() {
                getPreferences(0).edit().putString("map_follow", "MANUAL").apply();
                toast("Harita elle gezinti moduna geçti.");
            }
        });
        applyMapSettings();
        return this.map;
    }

    private void applyMapSettings() {
        if (this.map == null) return;
        SharedPreferences p = getPreferences(0);
        this.map.setFollowMode(p.getString("map_follow", "MANUAL"));
        this.map.setLayers(p.getBoolean("map_aprs", true), p.getBoolean("map_dmr", true));
        this.map.setFilters(p.getInt("map_distance", 250), p.getInt("map_age", 60));
    }

    private View compactBody() {
        LinearLayout body = column();
        body.setPadding(m11dp(6), m11dp(4), m11dp(6), m11dp(4));
        body.addView(createMap(), verticalWeight(1));
        this.selectedCard = text("İSTASYON SEÇİLMEDİ\n\nHarita veya arama menüsünden istasyon seçin.", 13, TEXT, true);
        this.selectedCard.setGravity(17);
        this.selectedCard.setBackground(round(PANEL, GREEN, 1, 8));
        this.selectedCard.setPadding(m11dp(8), m11dp(7), m11dp(8), m11dp(7));
        body.addView(this.selectedCard);
        addTgListenControl(body);
        return body;
    }

    private LinearLayout stationPanel() {
        LinearLayout p = column();
        p.setPadding(m11dp(16), m11dp(15), m11dp(16), m11dp(12));
        p.setBackgroundColor(PANEL);
        p.addView(info("SEÇİLİ İSTASYON", GREEN));
        this.selectedCard = stationCard();
        p.addView(this.selectedCard);
        Button focus = action("HARİTADA ODAKLA", PANEL_2);
        focus.setOnClickListener(v -> focusSelectedStation());
        p.addView(focus);
        Button msg = action("APRS MESAJLARI", PANEL_2);
        msg.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$stationPanel$6(view);
            }
        });
        p.addView(msg);
        Button search = action("ÇAĞRI / DMR ARA", GREEN);
        search.setTextColor(f15BG);
        search.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$stationPanel$7(view);
            }
        });
        p.addView(search);
        addTgListenControl(p);
        return p;
    }

    private void addTgListenControl(LinearLayout host) {
        this.tgListenButton = action("▶ TG " + this.talkGroup + " DİNLE", PANEL_2);
        if (this.tgListening) {
            this.tgListenButton.setText("■ TG " + this.talkGroup + " DİNLEMEYİ DURDUR");
            this.tgListenButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause, 0, 0, 0);
        } else {
            this.tgListenButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
        }
        this.tgListenButton.setCompoundDrawablePadding(m11dp(5));
        this.tgListenButton.setOnClickListener(v -> toggleTgListening(host));
        host.addView(this.tgListenButton);
        if (this.tgListening && this.tgAudioWebView != null) {
            if (this.tgAudioWebView.getParent() instanceof ViewGroup) {
                ((ViewGroup) this.tgAudioWebView.getParent()).removeView(this.tgAudioWebView);
            }
            host.addView(this.tgAudioWebView, new LinearLayout.LayoutParams(2, 2));
            this.tgAudioWebView.onResume();
            this.tgAudioWebView.resumeTimers();
        }
    }

    private void toggleTgListening(LinearLayout host) {
        if (this.tgListening) {
            stopTgListening();
            toast("TG " + this.talkGroup + " dinleme durduruldu.");
            return;
        }
        this.tgListening = true;
        this.tgKeepAliveScheduled = false;
        this.tgListenButton.setText("■ TG " + this.talkGroup + " DİNLEMEYİ DURDUR");
        this.tgListenButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause, 0, 0, 0);
        WebView audio = new WebView(this);
        this.tgAudioWebView = audio;
        WebSettings settings = audio.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        audio.setWebChromeClient(new WebChromeClient());
        audio.setBackgroundColor(Color.TRANSPARENT);
        audio.setFocusable(false);
        audio.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (url == null || !url.startsWith("https://hose.brandmeister.network")) return;
                String script = "(function(){" +
                        "window.__nexvoAutoPlay=true;" +
                        "var buttons=[].slice.call(document.querySelectorAll('button'));" +
                        "var player=buttons.find(function(b){return (b.getAttribute('aria-label')||b.innerText||'').trim()==='Player';});" +
                        "if(player&&!player.getAttribute('data-nexvo-opened')){player.setAttribute('data-nexvo-opened','1');player.click();}" +
                        "setTimeout(function(){var all=[].slice.call(document.querySelectorAll('button'));var start=all.find(function(b){return (b.getAttribute('aria-label')||'').indexOf('Start player')>=0;});if(start&&!start.disabled)start.click();},900);" +
                        "return 'ready';})()";
                view.evaluateJavascript(script, null);
                // Hoseline is client-rendered. Keep exactly one retry loop alive;
                // page reloads must not create overlapping player loops.
                if (!MainActivity.this.tgKeepAliveScheduled) {
                    MainActivity.this.tgKeepAliveScheduled = true;
                    view.postDelayed(() -> keepHoselineAlive(view), 1500L);
                }
            }
        });
        audio.setAlpha(0.01f);
        host.addView(audio, new LinearLayout.LayoutParams(2, 2));
        audio.onResume();
        audio.resumeTimers();
        audio.loadUrl("https://hose.brandmeister.network/?subscribe=" + this.talkGroup);
        toast("TG " + this.talkGroup + " ana ekranda dinleniyor. Trafik varsa ses otomatik gelir.");
    }

    private void startEmbeddedHoselineAudio(WebView view) {
        if (!this.tgListening || view != this.tgAudioWebView) return;
        String tg = this.talkGroup.replace("'", "");
        String js = "(function(){" +
                "var input=document.querySelector('input[aria-label=Talkgroup]');" +
                "if(!input){var bs=document.querySelectorAll('button');for(var i=0;i<bs.length;i++){if((bs[i].innerText||'').trim().toUpperCase()==='PLAYER'){bs[i].click();break;}}return;}" +
                "var selected=false,buttons=document.querySelectorAll('button');for(var b=0;b<buttons.length;b++){if((buttons[b].innerText||'').trim()==='" + tg + "'){selected=true;break;}}" +
                "if(!selected&&input.value!=='" + tg + "'){var set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;set.call(input,'" + tg + "');input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));}" +
                "var opts=document.querySelectorAll('[role=option]');for(var o=0;o<opts.length;o++){var tx=(opts[o].innerText||'').trim();if(tx==='" + tg + "'||tx.endsWith('(" + tg + ")')){opts[o].click();break;}}" +
                "var start=document.querySelector('button[aria-label=\"Start player\"]');if(start&&!start.disabled)start.click();" +
                "var a=document.querySelectorAll('audio,video');for(var j=0;j<a.length;j++){a[j].muted=false;a[j].volume=1;try{a[j].play();}catch(e){}}})()";
        view.evaluateJavascript(js, null);
    }

    private void keepHoselineAlive(WebView view) {
        if (!this.tgListening || view != this.tgAudioWebView) return;
        startEmbeddedHoselineAudio(view);
        view.postDelayed(() -> keepHoselineAlive(view), 5000L);
    }

    private void stopTgListening() {
        this.tgListening = false;
        this.tgKeepAliveScheduled = false;
        if (this.tgAudioWebView != null) {
            this.tgAudioWebView.evaluateJavascript("(function(){window.__nexvoAutoPlay=false;document.querySelectorAll('audio,video').forEach(function(a){try{a.pause();a.removeAttribute('src');a.load();}catch(e){}});var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return (x.getAttribute('aria-label')||'').indexOf('Stop player')>=0;});if(b)b.click();})()", null);
            this.tgAudioWebView.stopLoading();
            if (this.tgAudioWebView.getParent() instanceof ViewGroup) {
                ((ViewGroup) this.tgAudioWebView.getParent()).removeView(this.tgAudioWebView);
            }
            this.tgAudioWebView.onPause();
            this.tgAudioWebView.pauseTimers();
            this.tgAudioWebView.removeAllViews();
            this.tgAudioWebView.destroy();
            this.tgAudioWebView = null;
        }
        if (this.tgListenButton != null) {
            this.tgListenButton.setText("▶ TG " + this.talkGroup + " DİNLE");
            this.tgListenButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$stationPanel$6(View v) {
        showMessages(this.callsign);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$stationPanel$7(View v) {
        showSearch();
    }

    private TextView stationCard() {
        TextView v = text("●  İSTASYON SEÇİN\n    Çağrı işareti • Mesafe • Son görülme", this.compact ? 13 : 15, TEXT, true);
        v.setPadding(m11dp(14), m11dp(12), m11dp(14), m11dp(12));
        v.setBackground(round(PANEL, GREEN, 1, 7));
        v.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda42
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$stationCard$8(view);
            }
        });
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$stationCard$8(View x) {
        if (this.selectedStationName.isEmpty()) showSearch(); else focusSelectedStation();
    }

    private void focusSelectedStation() {
        if (this.map == null || this.selectedStationName.isEmpty()
                || Double.isNaN(this.selectedStationLat) || Double.isNaN(this.selectedStationLng)) {
            toast("Önce bir çağrı işareti seçin veya arayın.");
            return;
        }
        this.map.pinStation(this.selectedStationName, true);
        toast(this.selectedStationName + " haritada odaklandı.");
    }

    private View bottomBar() {
        LinearLayout bar = row();
        bar.setPadding(m11dp(3), m11dp(2), m11dp(3), m11dp(6));
        bar.setBackgroundColor(PANEL);
        String[] items = this.compact ? new String[]{"MENÜ", "ARA", "MESAJ", "BEACON"} : new String[]{"HARİTA", "ARA", "MESAJ", "BAĞLANTI", "BEACON"};
        int[] icons = this.compact
                ? new int[]{android.R.drawable.ic_menu_more, android.R.drawable.ic_menu_search, android.R.drawable.ic_dialog_email, android.R.drawable.presence_online}
                : new int[]{android.R.drawable.ic_menu_mapmode, android.R.drawable.ic_menu_search, android.R.drawable.ic_dialog_email, android.R.drawable.ic_menu_manage, android.R.drawable.presence_online};
        int iconIndex = 0;
        for (String item : items) {
            Button b = action(item, PANEL);
            b.setTextSize(this.compact ? 8.0f : 9.0f);
            android.graphics.drawable.Drawable icon = getResources().getDrawable(icons[iconIndex++]);
            icon.setBounds(0, 0, m11dp(this.compact ? 16 : 18), m11dp(this.compact ? 16 : 18));
            b.setCompoundDrawables(null, icon, null, null);
            b.setCompoundDrawablePadding(0);
            b.setPadding(0, 0, 0, 0);
            b.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda36
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.lambda$bottomBar$9(view);
                }
            });
            bar.addView(b, new LinearLayout.LayoutParams(0, m11dp(38), 1.0f));
        }
        return bar;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$bottomBar$9(View v) {
        String x = ((Button) v).getText().toString();
        if (!"MENÜ".equals(x)) {
            if (!"ARA".equals(x)) {
                if (!"MESAJ".equals(x)) {
                    if (!"BAĞLANTI".equals(x)) {
                        if (!"BEACON".equals(x)) {
                            showDashboard();
                            return;
                        } else {
                            showBeacon();
                            return;
                        }
                    }
                    showConnections();
                    return;
                }
                showMessages(this.callsign);
                return;
            }
            showSearch();
            return;
        }
        showMainMenu();
    }

    private void showMainMenu() {
        this.home = false;
        this.mainMenuVisible = true;
        LinearLayout screen = screen();
        screen.addView(radioHeader());
        screen.addView(pageTitle("‹", "ANA MENÜ"));
        LinearLayout list = column();
        addMenu(list, "♙", "Profilim", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showSetup();
            }
        });
        addMenu(list, "☆", "Sembolüm", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showSymbolSettings();
            }
        });
        addMenu(list, "⌕", "Çağrı İşareti / DMR Ara", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showSearch();
            }
        });
        addMenu(list, "◴", "Son Görüşmeler", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$showMainMenu$10();
            }
        });
        addMenu(list, "▱", "Harita Görünümü", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showDashboard();
            }
        });
        addMenu(list, "◎", "Harita Takip ve Filtreler", new Runnable() {
            @Override public void run() { MainActivity.this.showMapSettings(); }
        });
        addMenu(list, "♧", "TG ve DMR", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showTalkGroupDialog();
            }
        });
        addMenu(list, "↗", "Bağlantılar", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showConnections();
            }
        });
        addMenu(list, "◇", "Birimler", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showUnitSettings();
            }
        });
        addMenu(list, "?", "Yardım", new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.showHelp();
            }
        });
        addMenu(list, "♥", "Destekçilerimiz", new Runnable() {
            @Override public void run() { MainActivity.this.showSponsors(); }
        });
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        screen.addView(scroll, verticalWeight(1));
        setContentView(screen);
    }

    private void showSponsors() {
        this.home = false;
        LinearLayout root = screen();
        root.addView(radioHeader());
        root.addView(pageTitle("‹", "DESTEKÇİLERİMİZ"));
        LinearLayout body = column();
        body.addView(info("Nexvo APRS sunucu altyapı sponsoru", GREEN));
        ImageView logo = new ImageView(this);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 16;
        logo.setImageBitmap(BitmapFactory.decodeResource(getResources(), tr.aprs.app.R.drawable.sponsor_vds_hosting, options));
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setPadding(24, 28, 24, 28);
        body.addView(logo, new LinearLayout.LayoutParams(-1, 360));
        body.addView(info("VDS HOSTING BİLİŞİM TEKNOLOJİLERİ\nSunucu desteği: 1 vCPU • 2 GB RAM • 20 GB NVMe • Sabit IPv4\nTB4VAV • 73", TEXT));
        Button website = action("VDShosting.com SİTESİNİ AÇ", GREEN);
        website.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.vdshosting.com/"))));
        body.addView(website);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        root.addView(scroll, verticalWeight(1));
        setContentView(root);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMainMenu$10() {
        showMessages(this.callsign);
    }

    private void showFavorites() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "FAVORİ İSTASYONLAR"));
        LinearLayout body = column(); final EditText call = field("Çağrı işareti", false); body.addView(call);
        Button toggle = action("FAVORİYE EKLE / ÇIKAR", GREEN);
        toggle.setOnClickListener(v -> { if (!call.getText().toString().trim().isEmpty()) { featureStore.toggleFavorite(call.getText().toString()); showFavorites(); } });
        body.addView(toggle);
        List<String> items = featureStore.favorites();
        if (items.isEmpty()) body.addView(info("Henüz favori yok. Favoriler arka planda görüldüğünde kayda alınır.", MUTED));
        for (String item : items) body.addView(info("★ " + item, GREEN));
        ScrollView scroll = new ScrollView(this); scroll.addView(body); root.addView(scroll, verticalWeight(1)); setContentView(root);
    }

    private void showDiagnostics() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "KAYITLAR VE TEŞHİS"));
        LinearLayout body = column(); body.addView(info("Son APRS olayları, bağlantı hataları ve beacon kayıtları", TEXT));
        List<String> logs = featureStore.recentLogs(100); for (String item : logs) body.addView(info(item, MUTED));
        Button share = action("GÜNLÜĞÜ DIŞA AKTAR", GREEN); share.setOnClickListener(v -> shareText("Nexvo APRS teşhis günlüğü", joinLines(featureStore.recentLogs(500)))); body.addView(share);
        ScrollView scroll = new ScrollView(this); scroll.addView(body); root.addView(scroll, verticalWeight(1)); setContentView(root);
    }

    private void showProfilesAndBackup() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "PROFİLLER VE YEDEK"));
        LinearLayout body = column(); final EditText name = field("Profil adı", false); body.addView(name);
        Button save = action("MEVCUT AYARLARI PROFİLE KAYDET", GREEN);
        save.setOnClickListener(v -> { String n = name.getText().toString().trim(); if (n.isEmpty()) { toast("Profil adı girin."); return; } getSharedPreferences("profiles", 0).edit().putString(n + "_call", callsign).putString(n + "_dmr", dmrId).putString(n + "_tg", talkGroup).apply(); toast("Profil kaydedildi: " + n); }); body.addView(save);
        Button load = action("PROFİLİ YÜKLE", PANEL_2);
        load.setOnClickListener(v -> { String n = name.getText().toString().trim(); SharedPreferences p = getSharedPreferences("profiles", 0); String c = p.getString(n + "_call", ""); if (c.isEmpty()) { toast("Profil bulunamadı."); return; } callsign = c; dmrId = p.getString(n + "_dmr", ""); talkGroup = p.getString(n + "_tg", "28642"); getPreferences(0).edit().putString("callsign", callsign).putString("dmr_id", dmrId).putString("talkgroup", talkGroup).apply(); toast("Profil yüklendi."); }); body.addView(load);
        Button backup = action("AYAR YEDEĞİNİ PAYLAŞ", GREEN); backup.setOnClickListener(v -> shareText("Nexvo APRS ayar yedeği", "callsign=" + callsign + "\ndmr_id=" + dmrId + "\ntalkgroup=" + talkGroup + "\nsymbol=" + getPreferences(0).getString("aprs_symbol", ">"))); body.addView(backup);
        root.addView(body, verticalWeight(1)); setContentView(root);
    }

    private void showSecurity() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "GÜVENLİK")); LinearLayout body = column();
        boolean locked = getPreferences(0).getBoolean("app_lock", false);
        body.addView(info("API anahtarı ve passcode Android Keystore ile şifrelenir. Kilit cihaz PIN'i, parmak izi veya yüz kilidini kullanır.", TEXT));
        Button toggle = action(locked ? "UYGULAMA KİLİDİNİ KAPAT" : "UYGULAMA KİLİDİNİ AÇ", locked ? WARN : GREEN);
        toggle.setOnClickListener(v -> { getPreferences(0).edit().putBoolean("app_lock", !locked).apply(); toast(!locked ? "Uygulama kilidi açıldı." : "Uygulama kilidi kapatıldı."); showSecurity(); }); body.addView(toggle);
        root.addView(body, verticalWeight(1)); setContentView(root);
    }

    private void requestDeviceUnlock() {
        KeyguardManager manager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (manager != null && manager.isDeviceSecure()) { Intent intent = manager.createConfirmDeviceCredentialIntent("Nexvo APRS kilidi", "Devam etmek için kimliğinizi doğrulayın"); if (intent != null) startActivityForResult(intent, 712); }
    }
    private void shareText(String subject, String text) { Intent send = new Intent(Intent.ACTION_SEND); send.setType("text/plain"); send.putExtra(Intent.EXTRA_SUBJECT, subject); send.putExtra(Intent.EXTRA_TEXT, text); startActivity(Intent.createChooser(send, "Dışa aktar")); }
    private static String joinLines(List<String> lines) { StringBuilder out = new StringBuilder(); for (String line : lines) out.append(line).append('\n'); return out.toString(); }

    private void showFieldTestCenter() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "SAHA TEST MERKEZİ"));
        LinearLayout body = column(); SecureStore secure = new SecureStore(this);
        boolean callOk = !callsign.isEmpty(); boolean apiOk = !secure.readApiKey().isEmpty(); boolean passOk = !secure.readAprsPasscode().isEmpty();
        boolean locationOk = checkSelfPermissionCompat("android.permission.ACCESS_FINE_LOCATION") == 0;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE); NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo(); boolean networkOk = ni != null && ni.isConnected();
        SharedPreferences runtime = getSharedPreferences("runtime_status", 0);
        String state = runtime.getString("state", "Henüz durum yok"); String lastError = runtime.getString("last_error", ""); long lastBeacon = runtime.getLong("last_beacon", 0);
        body.addView(testRow("Çağrı işareti", callOk, callsign));
        body.addView(testRow("APRS.fi API anahtarı", apiOk, apiOk ? "Kayıtlı" : "Eksik"));
        body.addView(testRow("APRS-IS passcode", passOk, passOk ? "Kayıtlı" : "Eksik"));
        body.addView(testRow("Konum izni", locationOk, locationOk ? "Verildi" : "Eksik"));
        body.addView(testRow("İnternet", networkOk, networkOk ? "Bağlı" : "Bağlantı yok"));
        body.addView(testRow("Arka plan APRS", getPreferences(0).getBoolean("background_aprs", false), state));
        body.addView(info("Son beacon: " + (lastBeacon == 0 ? "Yok" : new SimpleDateFormat("dd.MM HH:mm:ss", Locale.ROOT).format(new Date(lastBeacon))), MUTED));
        if (!lastError.isEmpty()) body.addView(info("Son hata: " + lastError, WARN));
        final TextView apiResult = info("APRS.fi canlı testi henüz çalıştırılmadı.", MUTED); body.addView(apiResult);
        Button apiTest = action("APRS.FI CANLI TEST", GREEN); apiTest.setOnClickListener(v -> runAprsFiSelfTest(apiResult)); body.addView(apiTest);
        Button report = action("TEST RAPORUNU PAYLAŞ", PANEL_2); report.setOnClickListener(v -> shareText("Nexvo APRS 0.17.0 saha testi", "Çağrı=" + callOk + "\nAPI=" + apiOk + "\nPasscode=" + passOk + "\nKonum=" + locationOk + "\nİnternet=" + networkOk + "\nDurum=" + state + "\nSon hata=" + lastError)); body.addView(report);
        ScrollView scroll = new ScrollView(this); scroll.addView(body); root.addView(scroll, verticalWeight(1)); setContentView(root);
    }

    private View testRow(String name, boolean ok, String detail) { return info((ok ? "✓ " : "✗ ") + name + " • " + detail, ok ? GREEN : WARN); }

    private void runAprsFiSelfTest(final TextView result) {
        String key = secureStore.readApiKey();
        if (callsign.isEmpty() || key.isEmpty()) { result.setText("TEST BAŞARISIZ: Çağrı işareti veya API anahtarı eksik."); result.setTextColor(WARN); return; }
        result.setText("APRS.fi bağlantısı deneniyor…");
        new AprsFiClient().getLocation(callsign, key, new AprsFiClient.Callback() {
            @Override public void onSuccess(AprsFiClient.Station station) { runOnUiThread(() -> { result.setText("TEST BAŞARILI: " + station.name + " • " + station.latitude + ", " + station.longitude); result.setTextColor(GREEN); }); }
            @Override public void onError(String error) { runOnUiThread(() -> { result.setText("TEST BAŞARISIZ: " + error); result.setTextColor(WARN); }); }
        });
    }

    private void showBeaconProfiles() {
        this.home = false; LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "SMARTBEACON PROFİLİ")); LinearLayout body = column();
        String current = getPreferences(0).getString("beacon_profile", "ARAC"); body.addView(info("Seçili profil: " + current + "\nPil %20 altına düştüğünde aralıklar otomatik iki katına çıkar.", GREEN));
        String[] profiles = {"ARAC", "YAYA", "BISIKLET", "SABIT"};
        for (String profile : profiles) { Button b = action(profile, profile.equals(current) ? GREEN : PANEL_2); b.setOnClickListener(v -> { getPreferences(0).edit().putString("beacon_profile", profile).apply(); toast("SmartBeacon profili: " + profile); showBeaconProfiles(); }); body.addView(b); }
        root.addView(body, verticalWeight(1)); setContentView(root);
    }

    private void showMapSettings() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "HARİTA TAKİP VE FİLTRELER"));
        LinearLayout body = column(); SharedPreferences p = getPreferences(0);
        body.addView(info("Takip modu: " + p.getString("map_follow", "MANUAL") + "\nAPRS yeşil, DMR çıkışları turuncu gösterilir. Haritayı elle sürüklemek takibi durdurur.", GREEN));
        String[][] modes = {{"MANUAL", "SERBEST / HARİTA ZIPLAMASIN"}, {"SELF", "BENİ TAKİP ET"}, {"DMR", "TG KONUŞMACI ÇIKIŞINI TAKİP ET"}, {"SELECTED", "SEÇTİĞİM İSTASYONU TAKİP ET"}};
        for (String[] mode : modes) { Button b = action(mode[1], mode[0].equals(p.getString("map_follow", "MANUAL")) ? GREEN : PANEL_2); b.setOnClickListener(v -> { p.edit().putString("map_follow", mode[0]).apply(); applyMapSettings(); showMapSettings(); }); body.addView(b); }
        Button aprsLayer = action("APRS KATMANI: " + (p.getBoolean("map_aprs", true) ? "AÇIK" : "KAPALI"), PANEL_2); aprsLayer.setOnClickListener(v -> { p.edit().putBoolean("map_aprs", !p.getBoolean("map_aprs", true)).apply(); applyMapSettings(); showMapSettings(); }); body.addView(aprsLayer);
        Button dmrLayer = action("DMR / TG KATMANI: " + (p.getBoolean("map_dmr", true) ? "AÇIK" : "KAPALI"), PANEL_2); dmrLayer.setOnClickListener(v -> { p.edit().putBoolean("map_dmr", !p.getBoolean("map_dmr", true)).apply(); applyMapSettings(); showMapSettings(); }); body.addView(dmrLayer);
        body.addView(info("MESAFE FİLTRESİ", MUTED));
        for (int km : new int[]{25, 50, 100, 250, 1000}) { Button b = action(km + " KM", p.getInt("map_distance", 250) == km ? GREEN : PANEL_2); b.setOnClickListener(v -> { p.edit().putInt("map_distance", km).apply(); applyMapSettings(); showMapSettings(); }); body.addView(b); }
        body.addView(info("İSTASYON YAŞI FİLTRESİ", MUTED));
        for (int min : new int[]{15, 30, 60, 180}) { Button b = action(min + " DAKİKA", p.getInt("map_age", 60) == min ? GREEN : PANEL_2); b.setOnClickListener(v -> { p.edit().putInt("map_age", min).apply(); applyMapSettings(); showMapSettings(); }); body.addView(b); }
        ScrollView scroll = new ScrollView(this); scroll.addView(body); root.addView(scroll, verticalWeight(1)); setContentView(root);
    }

    private void showKeyLearner() {
        this.home = false; getPreferences(0).edit().putBoolean("key_learning", true).apply();
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "HYTERA TUŞ ÖĞRENME")); LinearLayout body = column();
        int last = getPreferences(0).getInt("last_key_code", -1); body.addView(info("Öğrenmek istediğiniz fiziksel tuşa basın. Kod kaydedilir.\nSon kod: " + (last < 0 ? "Henüz yok" : last), GREEN));
        body.addView(info("Varsayılan: F1/Kamera = Beacon, F2/Kulaklık = Mesaj, F3 = Menü", MUTED));
        if (last >= 0) {
            Button beacon = action("SON TUŞU BEACON'A ATA", GREEN); beacon.setOnClickListener(v -> { getPreferences(0).edit().putInt("key_beacon", last).apply(); toast("Beacon tuşu atandı: " + last); }); body.addView(beacon);
            Button message = action("SON TUŞU MESAJLARA ATA", PANEL_2); message.setOnClickListener(v -> { getPreferences(0).edit().putInt("key_message", last).apply(); toast("Mesaj tuşu atandı: " + last); }); body.addView(message);
            Button menu = action("SON TUŞU MENÜYE ATA", PANEL_2); menu.setOnClickListener(v -> { getPreferences(0).edit().putInt("key_menu", last).apply(); toast("Menü tuşu atandı: " + last); }); body.addView(menu);
        }
        Button done = action("ÖĞRENMEYİ BİTİR", PANEL_2); done.setOnClickListener(v -> { getPreferences(0).edit().putBoolean("key_learning", false).apply(); showMainMenu(); }); body.addView(done);
        root.addView(body, verticalWeight(1)); setContentView(root);
    }

    private void addMenu(LinearLayout list, String icon, String name, final Runnable run) {
        LinearLayout row = row();
        row.setGravity(16);
        row.setPadding(m11dp(18), m11dp(4), m11dp(12), m11dp(4));
        row.setBackgroundColor(PANEL);
        row.addView(text(icon, 28, GREEN, false), new LinearLayout.LayoutParams(m11dp(48), m11dp(48)));
        row.addView(text(name, this.compact ? 15 : 18, TEXT, false), weight(1));
        row.addView(text("›", 28, MUTED, false));
        row.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                run.run();
            }
        });
        list.addView(row, new LinearLayout.LayoutParams(-1, m11dp(this.compact ? 52 : 58)));
        View line = new View(this);
        line.setBackgroundColor(LINE);
        list.addView(line, new LinearLayout.LayoutParams(-1, m11dp(1)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSymbolSettings() {
        Dialog dialog = new Dialog(this);
        LinearLayout box = column();
        box.setPadding(m11dp(18), m11dp(14), m11dp(18), m11dp(18));
        box.addView(title("APRS SEMBOLÜM", 21));
        box.addView(dialogBack(dialog));
        final TextView selected = info("SEÇİLİ: " + getPreferences(0).getString("aprs_symbol_name", "ARAÇ"), GREEN);
        box.addView(selected);
        String[][] symbols = {new String[]{">", "ARAÇ"}, new String[]{"-", "EV / SABİT"}, new String[]{"[", "YÜRÜYÜŞ"}, new String[]{"k", "KAMYON"}, new String[]{"R", "RV / KARAVAN"}, new String[]{"Y", "TEKNE"}, new String[]{"b", "BİSİKLET"}, new String[]{"O", "BALON"}};
        for (final String[] item : symbols) {
            Button button = action(item[1] + "   [" + item[0] + "]", PANEL_2);
            button.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda21
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.lambda$showSymbolSettings$12(item, selected, view);
                }
            });
            box.addView(button);
        }
        box.addView(info("Seçilen sembol bir sonraki APRS beacon paketinde yayınlanır.", MUTED));
        dialog.setContentView(box);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(round(f15BG, GREEN, 1, 10));
        }
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSymbolSettings$12(String[] item, TextView selected, View v) {
        getPreferences(0).edit().putString("aprs_symbol", item[0]).putString("aprs_symbol_name", item[1]).apply();
        selected.setText("SEÇİLİ: " + item[1]);
        toast("APRS sembolü kaydedildi.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showUnitSettings() {
        final Dialog dialog = new Dialog(this);
        LinearLayout box = column();
        box.setPadding(m11dp(18), m11dp(14), m11dp(18), m11dp(18));
        box.addView(title("BİRİMLER", 21));
        box.addView(dialogBack(dialog));
        TextView selected = info("SEÇİLİ: " + (isImperial() ? "MİL / FEET" : "KM / METRE"), GREEN);
        box.addView(selected);
        Button metric = action("METRİK • KM / KM-SA / METRE", PANEL_2);
        metric.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showUnitSettings$13(dialog, view);
            }
        });
        box.addView(metric);
        Button imperial = action("İNGİLİZ • MİL / MPH / FEET", PANEL_2);
        imperial.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showUnitSettings$14(dialog, view);
            }
        });
        box.addView(imperial);
        dialog.setContentView(box);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(round(f15BG, GREEN, 1, 10));
        }
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUnitSettings$13(Dialog dialog, View v) {
        getPreferences(0).edit().putBoolean("imperial_units", false).apply();
        dialog.dismiss();
        toast("Metrik birimler etkin.");
        showDashboard();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUnitSettings$14(Dialog dialog, View v) {
        getPreferences(0).edit().putBoolean("imperial_units", true).apply();
        dialog.dismiss();
        toast("İngiliz birimleri etkin.");
        showDashboard();
    }

    private boolean isImperial() {
        return getPreferences(0).getBoolean("imperial_units", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showHelp() {
        this.home = false;
        LinearLayout screen = screen();
        screen.addView(pageTitle("‹", "YARDIM"));
        LinearLayout body = column();
        body.setPadding(m11dp(16), m11dp(10), m11dp(16), m11dp(22));
        body.addView(title("Nexvo APRS • HIZLI KILAVUZ", 21));
        body.addView(info("HARİTA\nTG’de mandala basan istasyon BrandMeister’dan alınır. Çağrı işaretinin APRS konumu varsa kişi; yoksa BrandMeister koordinat sağlıyorsa kullanılan çıkış haritada gösterilir.", TEXT));
        body.addView(info("APRS MESAJ\nGönderici ve alıcı çağrı işaretini girin. Gönderim için geçerli APRS-IS passcode gerekir; mesaj ACK gelene kadar yeniden denenir.", TEXT));
        body.addView(info("BEACON\nTelefon konumu, hız, irtifa, yön, seçilen APRS sembolü ve TG bilgisi APRS-IS ağına gönderilir.", TEXT));
        body.addView(info("DMR / TG\nKonuşmacı adı, çağrı işareti, DMR ID, slot ve çıkış bilgileri BrandMeister canlı akışından alınır. Hoseline yalnız dinleme içindir.", TEXT));
        body.addView(info("GİZLİLİK\nAPRS konumu ve mesajları kamusal ağda görülebilir. API anahtarı ve parolalar Android Keystore ile şifreli saklanır.", WARN));
        body.addView(info("SÜRÜM 0.10.0 • Android 7 ve üzeri • Telefon / tablet", MUTED));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        screen.addView(scroll, verticalWeight(1));
        setContentView(screen);
    }

    private View pageTitle(String back, String title) {
        this.mainMenuVisible = "ANA MENÜ".equals(title);
        LinearLayout row = row();
        row.setGravity(16);
        row.setPadding(m11dp(10), m11dp(7), m11dp(12), m11dp(7));
        Button b = smallButton(back + " GERİ");
        b.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda30
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$pageTitle$15(view);
            }
        });
        row.addView(b);
        row.addView(text(title, 17, TEXT, true), weight(1));
        return row;
    }

    private Button dialogBack(final Dialog dialog) {
        Button back = action("‹ GERİ", PANEL_2);
        back.setOnClickListener(v -> dialog.dismiss());
        return back;
    }

    private Button dialogBack(final Dialog dialog, final Runnable afterDismiss) {
        Button back = action("‹ GERİ", PANEL_2);
        back.setOnClickListener(v -> {
            dialog.dismiss();
            if (afterDismiss != null) afterDismiss.run();
        });
        return back;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$pageTitle$15(View v) {
        if (this.mainMenuVisible) {
            showDashboard();
        } else {
            showMainMenu();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSearch() {
        final Dialog d = new Dialog(this);
        LinearLayout box = column();
        box.setPadding(m11dp(18), m11dp(16), m11dp(18), m11dp(18));
        box.addView(title("ÇAĞRI / DMR ARA", 21));
        box.addView(dialogBack(d));
        final EditText q = field("TA1ABC-9 veya DMR ID", false);
        box.addView(q);
        Button search = action("SORGULA", GREEN);
        search.setTextColor(f15BG);
        search.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showSearch$16(q, d, view);
            }
        });
        box.addView(search);
        d.setContentView(box);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(round(f15BG, GREEN, 1, 10));
        }
        d.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSearch$16(EditText q, Dialog d, View v) {
        String query = q.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (query.isEmpty()) {
            return;
        }
        d.dismiss();
        new RadioIdClient().lookup(query, new C01131(query));
    }

    /* JADX INFO: renamed from: tr.aprs.app.MainActivity$1 */
    class C01131 implements RadioIdClient.Callback {
        final /* synthetic */ String val$query;

        C01131(String str) {
            this.val$query = str;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onSuccess$0(RadioIdClient.Identity i) {
            MainActivity.this.queryAprs(i.callsign, i.dmrId);
        }

        @Override // tr.aprs.app.RadioIdClient.Callback
        public void onSuccess(final RadioIdClient.Identity i) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    C01131.this.lambda$onSuccess$0(i);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$1(String query) {
            MainActivity.this.queryAprs(query, "");
        }

        @Override // tr.aprs.app.RadioIdClient.Callback
        public void onError(String e) {
            MainActivity mainActivity = MainActivity.this;
            final String str = this.val$query;
            mainActivity.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    C01131.this.lambda$onError$1(str);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryAprs(String call, String dmr) {
        String key = this.secureStore.readApiKey();
        if (key.isEmpty()) {
            toast("APRS.fi API anahtarını Profilim ekranına girin.");
        } else {
            toast("İstasyon sorgulanıyor…");
            new AprsFiClient().getLocation(call, key, new C01142(dmr));
        }
    }

    /* JADX INFO: renamed from: tr.aprs.app.MainActivity$2 */
    class C01142 implements AprsFiClient.Callback {
        final /* synthetic */ String val$dmr;

        C01142(String str) {
            this.val$dmr = str;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onSuccess$0(AprsFiClient.Station s, String dmr) {
            MainActivity.this.updateStation(s, dmr);
            if (MainActivity.this.map != null) MainActivity.this.map.pinStation(s.name, true);
        }

        @Override // tr.aprs.app.AprsFiClient.Callback
        public void onSuccess(final AprsFiClient.Station s) {
            MainActivity mainActivity = MainActivity.this;
            final String str = this.val$dmr;
            mainActivity.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    C01142.this.lambda$onSuccess$0(s, str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$1(String e) {
            MainActivity.this.toast(e);
        }

        @Override // tr.aprs.app.AprsFiClient.Callback
        public void onError(final String e) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    C01142.this.lambda$onError$1(e);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code duplicated, block: B:14:0x0045  */
    /* JADX WARN: Code duplicated, block: B:21:0x0067  */
    /* JADX WARN: Code duplicated, block: B:29:0x0079  */
    /* JADX WARN: Code duplicated, block: B:30:0x008a  */
    /* JADX WARN: Code duplicated, block: B:33:0x0094  */
    /* JADX WARN: Code duplicated, block: B:34:0x0097  */
    /* JADX WARN: Code duplicated, block: B:37:0x00a6  */
    /* JADX WARN: Code duplicated, block: B:40:0x00af  */
    /* JADX WARN: Code duplicated, block: B:46:0x00d6  */
    /* JADX WARN: Code duplicated, block: B:49:0x00e0  */
    /* JADX WARN: Code duplicated, block: B:50:0x00e3  */
    /* JADX WARN: Code duplicated, block: B:53:0x00eb  */
    /* JADX WARN: Code duplicated, block: B:54:0x00ee  */
    /* JADX WARN: Code duplicated, block: B:57:0x0178  */
    /* JADX WARN: Code duplicated, block: B:58:0x0190  */
    /* JADX WARN: Code duplicated, block: B:61:0x01d1  */
    /* JADX WARN: Code duplicated, block: B:62:0x01d3  */
    /* JADX WARN: Code duplicated, block: B:65:0x024a  */
    /* JADX WARN: Code duplicated, block: B:68:0x025c  */
    /* JADX WARN: Code duplicated, block: B:70:0x02e9  */
    public void updateStation(AprsFiClient.Station s, String dmr) {
        this.selectedStationName = s.name;
        this.selectedStationLat = s.latitude;
        this.selectedStationLng = s.longitude;
        String distance;
        String movement;
        boolean zIsImperial;
        double shownSpeed;
        boolean zIsImperial2;
        double shownAltitude;
        String distance2;
        String speedUnit;
        String altitudeUnit;
        String distance3;
        String str;
        String str2;
        String movement2;
        long age = s.lastTime > 0 ? Math.max(0L, (System.currentTimeMillis() / 1000) - s.lastTime) : 0L;
        float distanceKm = -1.0f;
        if (checkSelfPermissionCompat("android.permission.ACCESS_FINE_LOCATION") == 0) {
            try {
                LocationManager manager = (LocationManager) getSystemService("location");
                Location own = manager.getLastKnownLocation("gps");
                if (own == null) {
                    try {
                        own = manager.getLastKnownLocation("network");
                        if (own != null) {
                            float[] result = new float[1];
                            try {
                                Location.distanceBetween(own.getLatitude(), own.getLongitude(), s.latitude, s.longitude, result);
                                distanceKm = result[0] / 1000.0f;
                            } catch (SecurityException e) {
                                distanceKm = -1.0f;
                            }
                        }
                    } catch (SecurityException e2) {
                    }
                } else if (own != null) {
                    float[] result2 = new float[1];
                    Location.distanceBetween(own.getLatitude(), own.getLongitude(), s.latitude, s.longitude, result2);
                    distanceKm = result2[0] / 1000.0f;
                }
            } catch (SecurityException e3) {
            }
            if (distanceKm >= 0.0f) {
                distance = String.format(Locale.ROOT, "%.1f KM", Float.valueOf(distanceKm));
            } else {
                distance = "MESAFE YOK";
            }
            if (s.speed >= 1.0d) {
                movement = "HAREKETLİ";
            } else {
                movement = "SABİT";
            }
            zIsImperial = isImperial();
            shownSpeed = s.speed;
            if (zIsImperial) {
                shownSpeed *= 0.621371d;
            }
            zIsImperial2 = isImperial();
            shownAltitude = s.altitude;
            if (zIsImperial2) {
                shownAltitude *= 3.28084d;
            }
            if (distanceKm < 0.0f && isImperial()) {
                distance2 = String.format(Locale.ROOT, "%.1f MİL", Double.valueOf(((double) distanceKm) * 0.621371d));
            } else {
                distance2 = distance;
            }
            if (isImperial()) {
                speedUnit = "mph";
            } else {
                speedUnit = "km/sa";
            }
            if (isImperial()) {
                altitudeUnit = "ft";
            } else {
                altitudeUnit = "m";
            }
            this.ledCall.setText(s.name);
            distance3 = distance2;
            this.ledRange.setText(distance2 + "   " + ((int) s.course) + "°");
            this.metricAltitude.setText("İRTİFA\n" + ((int) shownAltitude) + " " + altitudeUnit);
            this.metricSpeed.setText("HIZ\n" + ((int) shownSpeed) + " " + speedUnit);
            TextView textView = this.metricLast;
            StringBuilder sbAppend = new StringBuilder().append("SON\n");
            if (age < 120) {
                str = age + " sn";
            } else {
                str = (age / 60) + " dk";
            }
            textView.setText(sbAppend.append(str).toString());
            TextView textView2 = this.selectedCard;
            StringBuilder sbAppend2 = new StringBuilder().append("●  ").append(s.name);
            if (dmr.isEmpty()) {
                str2 = "";
            } else {
                str2 = "  •  DMR " + dmr;
            }
            movement2 = movement;
            textView2.setText(sbAppend2.append(str2).append("\n    Mesafe ").append(distance3).append("  •  İrtifa ").append((int) shownAltitude).append(" ").append(altitudeUnit).append("\n    ").append(movement2).append("  •  Hız ").append((int) shownSpeed).append(" ").append(speedUnit).append("  •  Yön ").append((int) s.course).append("°").toString());
            if (this.map != null) {
                String mapDetail = s.name + (dmr.isEmpty() ? "" : " • DMR " + dmr) + "\nMesafe: " + distance3 + "\nİrtifa: " + ((int) shownAltitude) + " " + altitudeUnit + "\n" + movement2 + " • " + ((int) shownSpeed) + " " + speedUnit + "\nYön: " + ((int) s.course) + "°";
                this.map.updateStationDetails(s.name, s.latitude, s.longitude, false, mapDetail, s.speed >= 3.0d);
            }
        }
        distanceKm = -1.0f;
        if (distanceKm >= 0.0f) {
            distance = String.format(Locale.ROOT, "%.1f KM", Float.valueOf(distanceKm));
        } else {
            distance = "MESAFE YOK";
        }
        if (s.speed >= 1.0d) {
            movement = "HAREKETLİ";
        } else {
            movement = "SABİT";
        }
        zIsImperial = isImperial();
        shownSpeed = s.speed;
        if (zIsImperial) {
            shownSpeed *= 0.621371d;
        }
        zIsImperial2 = isImperial();
        shownAltitude = s.altitude;
        if (zIsImperial2) {
            shownAltitude *= 3.28084d;
        }
        if (distanceKm < 0.0f) {
            distance2 = distance;
        } else {
            distance2 = distance;
        }
        if (isImperial()) {
            speedUnit = "mph";
        } else {
            speedUnit = "km/sa";
        }
        if (isImperial()) {
            altitudeUnit = "ft";
        } else {
            altitudeUnit = "m";
        }
        this.ledCall.setText(s.name);
        distance3 = distance2;
        this.ledRange.setText(distance2 + "   " + ((int) s.course) + "°");
        this.metricAltitude.setText("İRTİFA\n" + ((int) shownAltitude) + " " + altitudeUnit);
        this.metricSpeed.setText("HIZ\n" + ((int) shownSpeed) + " " + speedUnit);
        TextView textView3 = this.metricLast;
        StringBuilder sbAppend3 = new StringBuilder().append("SON\n");
        if (age < 120) {
            str = age + " sn";
        } else {
            str = (age / 60) + " dk";
        }
        textView3.setText(sbAppend3.append(str).toString());
        TextView textView4 = this.selectedCard;
        StringBuilder sbAppend4 = new StringBuilder().append("●  ").append(s.name);
        if (dmr.isEmpty()) {
            str2 = "";
        } else {
            str2 = "  •  DMR " + dmr;
        }
        movement2 = movement;
        textView4.setText(sbAppend4.append(str2).append("\n    Mesafe ").append(distance3).append("  •  İrtifa ").append((int) shownAltitude).append(" ").append(altitudeUnit).append("\n    ").append(movement2).append("  •  Hız ").append((int) shownSpeed).append(" ").append(speedUnit).append("  •  Yön ").append((int) s.course).append("°").toString());
        if (this.map != null) {
            String mapDetail2 = s.name + (dmr.isEmpty() ? "" : " • DMR " + dmr) + "\nMesafe: " + distance3 + "\nİrtifa: " + ((int) shownAltitude) + " " + altitudeUnit + "\n" + movement2 + " • " + ((int) shownSpeed) + " " + speedUnit + "\nYön: " + ((int) s.course) + "°";
            this.map.updateStationDetails(s.name, s.latitude, s.longitude,
                    sameStationIdentity(s.name, this.callsign), mapDetail2, s.speed >= 3.0d);
        }
    }

    private void showMessages(String target) {
        this.home = false;
        LinearLayout linearLayoutScreen = screen();
        linearLayoutScreen.addView(pageTitle("‹", "APRS MESAJLAŞMA"));
        final ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout linearLayoutColumn = column();
        LinearLayout addressing = column();
        addressing.setPadding(m11dp(12), m11dp(8), m11dp(12), m11dp(4));
        addressing.addView(label("GÖNDERİCİ ÇAĞRI İŞARETİ"));
        final EditText sender = field("Gönderici", false);
        sender.setText(this.callsign);
        sender.setAllCaps(true);
        addressing.addView(sender);
        addressing.addView(label("ALICI ÇAĞRI İŞARETİ / SSID VEYA DMR ID"));
        final EditText recipient = field("Ör. TA1ABC-9 veya 2861234", false);
        if (target != null && !target.equalsIgnoreCase(this.callsign)) {
            recipient.setText(target);
        }
        addressing.addView(recipient);
        Button loadThread = action("ALICI MESAJLARINI YÜKLE", PANEL_2);
        addressing.addView(loadThread);
        linearLayoutColumn.addView(addressing);
        final LinearLayout messages = column();
        messages.setPadding(m11dp(12), m11dp(12), m11dp(12), m11dp(12));
        final TextView loading = info("Alıcı çağrı işareti/SSID veya 7 haneli DMR ID girin.", MUTED);
        messages.addView(loading);
        linearLayoutColumn.addView(messages);
        scrollView.addView(linearLayoutColumn);
        linearLayoutScreen.addView(scrollView, verticalWeight(1));
        LinearLayout compose = row();
        compose.setPadding(m11dp(8), m11dp(7), m11dp(8), m11dp(8));
        final EditText input = field("APRS mesajı yaz…", false);
        LinearLayout templates = row();
        String[] quickMessages = {"Konumum iyi", "Müsait misin?", "73"};
        for (String quick : quickMessages) { Button q = action(quick, PANEL_2); q.setOnClickListener(v -> input.setText(quick)); templates.addView(q, weight(1)); }
        linearLayoutScreen.addView(templates);
        compose.addView(input, weight(1));
        Button send = action("GÖNDER", GREEN);
        send.setTextColor(f15BG);
        send.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showMessages$19(input, sender, recipient, messages, view);
            }
        });
        compose.addView(send, new LinearLayout.LayoutParams(m11dp(92), m11dp(52)));
        linearLayoutScreen.addView(compose);
        setContentView(linearLayoutScreen);
        recipient.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                MainActivity.lambda$showMessages$21(scrollView, recipient, view, z);
            }
        });
        loadThread.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showMessages$22(recipient, loading, messages, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMessages$19(EditText input, EditText sender, EditText recipient, LinearLayout messages, View v) {
        final String body = input.getText().toString().trim();
        final String source = sender.getText().toString().trim().toUpperCase(Locale.ROOT);
        String destination = recipient.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!source.matches("[A-Z0-9]{3,6}(-[A-Z0-9]{1,2})?")) {
            toast("Gönderici çağrı işareti/SSID geçersiz.");
            return;
        }
        if (destination.isEmpty()) {
            toast("Alıcı çağrı işareti/SSID veya DMR ID girin.");
            return;
        }
        if (!destination.matches("[A-Z0-9]{3,6}(-[0-9]{1,2})?") && !destination.matches("[0-9]{7}")) {
            toast("Çağrı işareti/SSID veya 7 haneli DMR ID geçersiz.");
            return;
        }
        if (body.isEmpty()) {
            return;
        }
        if (body.length() > 67) {
            toast("APRS mesajı en fazla 67 karakter olabilir.");
            return;
        }
        if (!source.equalsIgnoreCase(this.callsign)) {
            this.callsign = source;
            getPreferences(0).edit().putString("callsign", this.callsign).apply();
            startReception();
            toast("Gönderici çağrı işareti düzeltildi. APRS bağlantısı yenileniyor; bağlanınca tekrar Gönder'e basın.");
            return;
        }
        final TextView outgoing = (TextView) messageBubble(source + " • HAZIRLANIYOR", body);
        messages.addView(outgoing);
        String nexvoToken = this.secureStore.readNexvoToken();
        if (!nexvoToken.isEmpty()) {
            String aprsPasscode = this.secureStore.readAprsPasscode();
            if (aprsPasscode.isEmpty()) {
                outgoing.setText(source + " • APRS-IS PASSCODE EKSİK\n" + body);
                toast("Çok kullanıcılı sunucu gönderimi için Profilim ekranında APRS-IS passcode girin.");
                return;
            }
            this.nexvo.send(nexvoToken, source, aprsPasscode, destination, body, new NexvoApiClient.Callback<NexvoApiClient.SendResult>() {
                @Override public void onSuccess(NexvoApiClient.SendResult result) {
                    runOnUiThread(() -> {
                        String resolved = result.dmrId.isEmpty() ? result.recipient : result.dmrId + " → " + result.recipient;
                        outgoing.setText(source + " → " + resolved + " • SUNUCU KUYRUĞUNDA • #" + result.id + "\n" + body);
                        input.setText("");
                        featureStore.message(result.recipient, "GİDEN", body, "SUNUCU KUYRUĞUNDA");
                        toast(result.dmrId.isEmpty() ? "Mesaj Nexvo sunucusuna ulaştı; APRS ACK beklenecek." : "DMR ID " + result.recipient + " çağrı işaretine çözüldü; APRS ACK beklenecek.");
                    });
                }
                @Override public void onError(String error) {
                    runOnUiThread(() -> {
                        if (!destination.matches("[0-9]{7}") && (error.contains("503") || error.contains("mesaj servisi şu anda çevrimdışı"))) {
                            String fallbackId = aprs.sendReliableMessage(destination, body, new AprsIsClient.DeliveryCallback() {
                                @Override public void onStatus(String messageId, String status, int attempt) {
                                    lambda$showMessages$18(outgoing, source, body, messageId, status, attempt);
                                }
                            });
                            if (fallbackId != null) {
                                input.setText("");
                                featureStore.message(destination, "GİDEN", body, "DOĞRUDAN APRS • ACK BEKLENİYOR");
                                outgoing.setText(source + " • VDS ÇEVRİMDIŞI • DOĞRUDAN APRS GÖNDERİLDİ\n" + body);
                                toast("VDS mesaj servisi çevrimdışı; doğrudan APRS-IS kullanıldı.");
                                return;
                            }
                        }
                        outgoing.setText(source + " • SUNUCUYA ULAŞMADI\n" + body);
                        toast(error + " APRS-IS TX bağlantısını kontrol edin.");
                    });
                }
            });
            return;
        }
        String id = this.aprs.sendReliableMessage(destination, body, new AprsIsClient.DeliveryCallback() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda23
            @Override // tr.aprs.app.AprsIsClient.DeliveryCallback
            public final void onStatus(String str, String str2, int r10) {
                MainActivity.this.lambda$showMessages$18(outgoing, source, body, str, str2, r10);
            }
        });
        if (id != null) {
            featureStore.message(destination, "GİDEN", body, "ACK BEKLENİYOR");
            input.setText("");
            toast("APRS mesajı doğrudan gönderildi; ACK beklenecek.");
        } else {
            outgoing.setText(source + " • GÖNDERİLEMEDİ • APRS-IS TX DOĞRULANMADI\n" + body);
            toast("APRS-IS doğrulanmadı ve Nexvo anahtarı yok. Profil ayarlarını kontrol edin.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMessages$18(final TextView outgoing, final String source, final String body, String messageId, final String status, final int attempt) {
        runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda27
            @Override // java.lang.Runnable
            public final void run() {
                outgoing.setText(source + " • " + status + " • DENEME " + attempt + "\n" + body);
            }
        });
    }

    static /* synthetic */ void lambda$showMessages$21(final ScrollView scroll, final EditText recipient, View v, boolean focused) {
        if (focused) {
            scroll.post(new Runnable() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda41
                @Override // java.lang.Runnable
                public final void run() {
                    scroll.smoothScrollTo(0, recipient.getBottom());
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMessages$22(EditText recipient, TextView loading, LinearLayout messages, View v) {
        String destination = recipient.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (destination.isEmpty()) {
            toast("Önce alıcı çağrı işaretini girin.");
            return;
        }
        String nexvoToken = this.secureStore.readNexvoToken();
        if (!nexvoToken.isEmpty()) {
            loading.setText("Nexvo çevrimdışı mesajları yükleniyor…");
            String aprsPasscode = this.secureStore.readAprsPasscode();
            if (aprsPasscode.isEmpty()) {
                loading.setText("Mesaj geçmişi için APRS-IS passcode gerekli.");
                return;
            }
            this.nexvo.messages(nexvoToken, destination, aprsPasscode, new NexvoApiClient.Callback<List<NexvoApiClient.Message>>() {
                @Override public void onSuccess(List<NexvoApiClient.Message> items) {
                    runOnUiThread(() -> {
                        messages.removeAllViews();
                        if (items.isEmpty()) messages.addView(info("Nexvo sunucusunda mesaj bulunamadı.", MUTED));
                        for (NexvoApiClient.Message m : items) messages.addView(messageBubble(m.sender + " → " + m.recipient + " • " + messageStatus(m.status), m.body));
                    });
                }
                @Override public void onError(String error) { runOnUiThread(() -> loading.setText(error)); }
            });
            return;
        }
        String key = this.secureStore.readApiKey();
        if (key.isEmpty()) {
            loading.setText("Nexvo veya APRS.fi anahtarı girilmedi.");
        } else {
            loading.setText("Mesajlar yükleniyor…");
            new AprsFiClient().getMessages(destination, key, new C01153(messages, loading));
        }
    }

    private static String messageStatus(String status) {
        if (status == null) return "BİLİNMİYOR";
        switch (status.toLowerCase(Locale.ROOT)) {
            case "queued": return "KUYRUKTA";
            case "sent": return "GÖNDERİLDİ • ACK BEKLENİYOR";
            case "retrying": return "TEKRAR DENENİYOR";
            case "acked": return "TESLİM EDİLDİ (ACK)";
            case "received": return "ALINDI";
            case "rejected": return "REDDEDİLDİ (REJ)";
            case "failed": return "TESLİM EDİLEMEDİ";
            default: return status.toUpperCase(Locale.ROOT);
        }
    }

    /* JADX INFO: renamed from: tr.aprs.app.MainActivity$3 */
    class C01153 implements AprsFiClient.MessageCallback {
        final /* synthetic */ TextView val$loading;
        final /* synthetic */ LinearLayout val$messages;

        C01153(LinearLayout linearLayout, TextView textView) {
            this.val$messages = linearLayout;
            this.val$loading = textView;
        }

        @Override // tr.aprs.app.AprsFiClient.MessageCallback
        public void onSuccess(final List<AprsFiClient.Message> items) {
            MainActivity mainActivity = MainActivity.this;
            final LinearLayout linearLayout = this.val$messages;
            mainActivity.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    C01153.this.lambda$onSuccess$0(linearLayout, items);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onSuccess$0(LinearLayout messages, List items) {
            messages.removeAllViews();
            if (items.isEmpty()) {
                messages.addView(MainActivity.this.info("Mesaj bulunamadı.", MainActivity.MUTED));
            }
            Iterator it = items.iterator();
            while (it.hasNext()) {
                AprsFiClient.Message m = (AprsFiClient.Message) it.next();
                messages.addView(MainActivity.this.messageBubble(m.source + " → " + m.destination, m.text));
            }
        }

        @Override // tr.aprs.app.AprsFiClient.MessageCallback
        public void onError(final String e) {
            MainActivity mainActivity = MainActivity.this;
            final TextView textView = this.val$loading;
            mainActivity.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$3$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    textView.setText(e);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public View messageBubble(String source, String body) {
        TextView bubble = text(source + "\n" + body, 15, TEXT, false);
        bubble.setPadding(m11dp(12), m11dp(9), m11dp(12), m11dp(9));
        bubble.setBackground(round(PANEL_2, GREEN, 1, 12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(m11dp(28), m11dp(5), m11dp(4), m11dp(5));
        bubble.setLayoutParams(p);
        return bubble;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showTalkGroupDialog() {
        final Dialog d = new Dialog(this);
        LinearLayout box = column();
        box.setPadding(m11dp(18), m11dp(15), m11dp(18), m11dp(18));
        box.addView(title("TG ve DMR", 22));
        box.addView(dialogBack(d));
        final EditText tg = field("Talk Group", false);
        tg.setInputType(2);
        tg.setText(this.talkGroup);
        box.addView(tg);
        String[] strArr = {"28642", "28600", "28634"};
        for (int r3 = 0; r3 < 3; r3++) {
            final String quick = strArr[r3];
            Button b = action("TG " + quick, PANEL_2);
            b.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda33
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    tg.setText(quick);
                }
            });
            box.addView(b);
        }
        Button listen = action("▶ TG SESİNİ DİNLE (HOSELINE)", PANEL_2);
        listen.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda37
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showTalkGroupDialog$24(tg, d, view);
            }
        });
        box.addView(listen);
        Button history = action("TG KONUŞMACI GEÇMİŞİ", PANEL_2);
        history.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda38
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showTalkGroupDialog$25(d, view);
            }
        });
        box.addView(history);
        Button pttSetup = action("TELEFON / TABLET DMR BAS-KONUŞ", PANEL_2);
        pttSetup.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda39
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showTalkGroupDialog$26(d, view);
            }
        });
        box.addView(pttSetup);
        Button save = action("KAYDET", GREEN);
        save.setTextColor(f15BG);
        save.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda40
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showTalkGroupDialog$27(tg, d, view);
            }
        });
        box.addView(save);
        Button center = action("DMR–APRS MERKEZİ", GREEN);
        center.setTextColor(f15BG);
        center.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                d.dismiss();
                MainActivity.this.showDmrAprsCenter();
            }
        });
        box.addView(center);
        box.addView(info("Konuşmacı bilgisi Last Heard, ses ise resmî BrandMeister Hoseline üzerinden alınır. Yalnızca RX/dinleme yapılır.", MUTED));
        d.setContentView(box);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(round(f15BG, GREEN, 1, 10));
        }
        d.show();
    }

    private void showDmrAprsCenter() {
        final Dialog d = new Dialog(this);
        LinearLayout box = column();
        box.setPadding(m11dp(16), m11dp(12), m11dp(16), m11dp(18));
        box.addView(title("DMR–APRS MERKEZİ", 21));
        box.addView(dialogBack(d, this::showTalkGroupDialog));
        TextView status = info("TG " + talkGroup + " • Profil, TG mesajı, yoklama ve sunucu geçmişi", MUTED);
        box.addView(status);
        Button register = action("PROFİLİ KAYDET • TG MESAJLARINI AÇ", GREEN);
        register.setTextColor(f15BG);
        register.setOnClickListener(v -> registerServerProfile(status));
        box.addView(register);
        final EditText groupMessage = field("TG üyelerine APRS mesajı (en çok 67 karakter)", false);
        box.addView(groupMessage);
        Button sendGroup = action("TG " + talkGroup + " • AKTİF ÜYELERE GÖNDER", PANEL_2);
        sendGroup.setOnClickListener(v -> {
            String body = groupMessage.getText().toString().trim();
            if (body.isEmpty()) { toast("Mesaj yazın."); return; }
            status.setText("TG mesajı sıraya alınıyor…");
            nexvo.sendTalkgroup(secureStore.readNexvoToken(), callsign, secureStore.readAprsPasscode(), talkGroup, body, true, uiJson(status, "TG mesajı sıraya alındı"));
        });
        box.addView(sendGroup);
        final EditText netName = field("Çevrim / yoklama adı (ör. PAZAR ÇEVRİMİ)", false);
        box.addView(netName);
        Button checkin = action("YOKLAMAYA KATIL", PANEL_2);
        checkin.setOnClickListener(v -> nexvo.checkIn(secureStore.readNexvoToken(), callsign, secureStore.readAprsPasscode(), talkGroup, netName.getText().toString().trim(), uiJson(status, "Yoklamaya kaydedildiniz")));
        box.addView(checkin);
        Button checkinList = action("YOKLAMA LİSTESİNİ GETİR", PANEL_2);
        checkinList.setOnClickListener(v -> nexvo.checkInList(secureStore.readNexvoToken(), callsign, secureStore.readAprsPasscode(), talkGroup, netName.getText().toString().trim(), new NexvoApiClient.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { runOnUiThread(() -> status.setText(formatList(value.optJSONArray("checkins"), "callsign", "dmr_id", "Henüz yoklama kaydı yok."))); }
            public void onError(String error) { runOnUiThread(() -> status.setText(error)); }
        }));
        box.addView(checkinList);
        Button serverHistory = action("SUNUCU TG KONUŞMACI GEÇMİŞİ", PANEL_2);
        serverHistory.setOnClickListener(v -> nexvo.talkgroupHistory(secureStore.readNexvoToken(), callsign, secureStore.readAprsPasscode(), talkGroup, new NexvoApiClient.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { runOnUiThread(() -> status.setText(formatList(value.optJSONArray("talkers"), "callsign", "dmr_id", "Sunucuda konuşmacı kaydı yok."))); }
            public void onError(String error) { runOnUiThread(() -> status.setText(error)); }
        }));
        box.addView(serverHistory);
        Button serverStatus = action("SUNUCU SAĞLIK VE KUYRUK DURUMU", PANEL_2);
        serverStatus.setOnClickListener(v -> nexvo.systemStatus(secureStore.readNexvoToken(), callsign, secureStore.readAprsPasscode(), new NexvoApiClient.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { runOnUiThread(() -> status.setText("Sunucu: " + value.optJSONObject("worker") + "\nMesajlar: " + value.optJSONObject("message_counts") + "\nProfiller: " + value.optInt("profiles") + " • TG üyelikleri: " + value.optInt("tg_memberships"))); }
            public void onError(String error) { runOnUiThread(() -> status.setText(error)); }
        }));
        box.addView(serverStatus);
        box.addView(dialogBack(d, this::showTalkGroupDialog));
        ScrollView scroll = new ScrollView(this); scroll.addView(box);
        d.setContentView(scroll);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(round(f15BG, GREEN, 1, 10));
        d.show();
        if (d.getWindow() != null) d.getWindow().setLayout(-1, -1);
    }

    private NexvoApiClient.Callback<JSONObject> uiJson(final TextView status, final String success) {
        return new NexvoApiClient.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { runOnUiThread(() -> status.setText(success + (value.has("target_count") ? " • " + value.optInt("target_count") + " alıcı" : ""))); }
            public void onError(String error) { runOnUiThread(() -> status.setText(error)); }
        };
    }

    private void registerServerProfile(final TextView status) {
        status.setText("Profil doğrulanıyor…");
        nexvo.registerProfile(secureStore.readNexvoToken(), callsign, secureStore.readAprsPasscode(), dmrId, callsign, talkGroup, true, new NexvoApiClient.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) {
                String fcm = getSharedPreferences("nexvo", MODE_PRIVATE).getString("fcm_token", "");
                if (!fcm.isEmpty()) nexvo.registerDevice(secureStore.readNexvoToken(), callsign, fcm, uiJson(status, "Profil ve bildirimler hazır"));
                else runOnUiThread(() -> status.setText("Profil hazır • " + callsign + " • TG " + talkGroup + " mesajları açık"));
            }
            public void onError(String error) { runOnUiThread(() -> status.setText(error)); }
        });
    }

    private String formatList(JSONArray items, String first, String second, String empty) {
        if (items == null || items.length() == 0) return empty;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < items.length() && i < 30; i++) {
            JSONObject item = items.optJSONObject(i); if (item == null) continue;
            if (out.length() > 0) out.append('\n');
            out.append(item.optString(first, "BİLİNMİYOR"));
            String extra = item.optString(second); if (!extra.isEmpty()) out.append(" • DMR ").append(extra);
        }
        return out.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showTalkGroupDialog$24(EditText tg, Dialog d, View v) {
        String value = tg.getText().toString().trim();
        if (value.isEmpty() || !value.matches("\\d+")) {
            toast("Geçerli bir TG numarası yazın.");
            return;
        }
        if (!value.equals(this.talkGroup)) {
            stopTgListening();
            this.brandMeister.disconnect();
            getPreferences(0).edit().remove("last_talker_tg").remove("last_talker_text").apply();
        }
        this.talkGroup = value;
        getPreferences(0).edit().putString("talkgroup", value).apply();
        d.dismiss();
        showDashboard();
        getWindow().getDecorView().postDelayed(() -> {
            if (this.tgListenButton != null && !this.tgListening) this.tgListenButton.performClick();
        }, 350L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showTalkGroupDialog$25(Dialog d, View v) {
        d.dismiss();
        showTalkerHistory();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showTalkGroupDialog$26(Dialog d, View v) {
        d.dismiss();
        showDmrPttSetup();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showTalkGroupDialog$27(EditText tg, Dialog d, View v) {
        String value = tg.getText().toString().trim();
        if (!value.isEmpty()) {
            if (!value.equals(this.talkGroup)) {
                stopTgListening();
                this.brandMeister.disconnect();
                getPreferences(0).edit().remove("last_talker_tg").remove("last_talker_text").apply();
            }
            this.talkGroup = value;
            getPreferences(0).edit().putString("talkgroup", value).apply();
            d.dismiss();
            showDashboard();
        }
    }

    private void showDmrPttSetup() {
        this.home = false;
        LinearLayout linearLayoutScreen = screen();
        linearLayoutScreen.addView(pageTitle("‹", "TELEFON / TABLET DMR BAS-KONUŞ"));
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayoutColumn = column();
        linearLayoutColumn.setPadding(m11dp(16), m11dp(8), m11dp(16), m11dp(22));
        final SharedPreferences prefs = getPreferences(0);
        final TextView selectedPtt = info("SEÇİLİ PTT ALTYAPISI: " + prefs.getString("ptt_backend", "AYARLANMADI"), GREEN);
        linearLayoutColumn.addView(selectedPtt);
        LinearLayout backendRow = row();
        String[] strArr = {"MUMBLE", "DVSWITCH"};
        for (int r2 = 0; r2 < 2; r2++) {
            final String backend = strArr[r2];
            Button choice = action(backend, PANEL_2);
            choice.setTextSize(11.0f);
            choice.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda16
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.lambda$showDmrPttSetup$28(prefs, backend, selectedPtt, view);
                }
            });
            backendRow.addView(choice, new LinearLayout.LayoutParams(0, m11dp(48), 1.0f));
        }
        linearLayoutColumn.addView(backendRow);
        linearLayoutColumn.addView(title("BRANDMEISTER AMATÖR DMR", 19));
        linearLayoutColumn.addView(info("İnternet üzerinden gerçek DMR PTT için geçerli amatör telsiz çağrı işareti, DMR ID, Hotspot Security parolası ve lisanslı AMBE+2 vocoder gerekir.", WARN));
        final EditText bmId = field("7 haneli DMR ID", false);
        bmId.setInputType(2);
        bmId.setText(this.dmrId);
        final EditText essid = field("ESSID (01–99)", false);
        essid.setInputType(2);
        essid.setText(prefs.getString("bm_essid", "01"));
        final EditText master = field("Master (ör. 2861.master.brandmeister.network)", false);
        master.setText(prefs.getString("bm_master", "2861.master.brandmeister.network"));
        final EditText masterPort = field("Master portu", false);
        masterPort.setInputType(2);
        masterPort.setText(prefs.getString("bm_port", "62031"));
        final EditText bmPassword = field("BrandMeister Hotspot Security parolası", true);
        linearLayoutColumn.addView(label("DMR ID"));
        linearLayoutColumn.addView(bmId);
        linearLayoutColumn.addView(label("ESSID"));
        linearLayoutColumn.addView(essid);
        linearLayoutColumn.addView(label("BRANDMEISTER MASTER"));
        linearLayoutColumn.addView(master);
        linearLayoutColumn.addView(label("MASTER PORTU"));
        linearLayoutColumn.addView(masterPort);
        linearLayoutColumn.addView(label("HOTSPOT SECURITY"));
        linearLayoutColumn.addView(bmPassword);
        linearLayoutColumn.addView(title("ALTERNATİF 1 • BRANDMEISTER MUMBLE", 19));
        linearLayoutColumn.addView(info("Telefon ve tablette AMBE codec kurmadan kullanılabilen en uygun alternatif. Yalnızca Mumble köprüsü açılmış TG ve bölgesel BrandMeister sunucularında çalışır. VOX kapalı, PTT açık olmalıdır.", GREEN));
        final EditText mumbleServer = field("Mumble sunucusu", false);
        mumbleServer.setText(prefs.getString("mumble_server", ""));
        final EditText mumblePort = field("Mumble portu", false);
        mumblePort.setInputType(2);
        mumblePort.setText(prefs.getString("mumble_port", "64738"));
        final EditText mumbleUser = field("KULLANICI: ÇAĞRI-DMRID", false);
        mumbleUser.setText(prefs.getString("mumble_user", this.callsign + (this.dmrId.isEmpty() ? "" : "-" + this.dmrId)));
        final EditText mumblePassword = field("BrandMeister Hotspot Security parolası", true);
        linearLayoutColumn.addView(label("MUMBLE SUNUCUSU"));
        linearLayoutColumn.addView(mumbleServer);
        linearLayoutColumn.addView(label("PORT"));
        linearLayoutColumn.addView(mumblePort);
        linearLayoutColumn.addView(label("KULLANICI"));
        linearLayoutColumn.addView(mumbleUser);
        linearLayoutColumn.addView(label("PAROLA"));
        linearLayoutColumn.addView(mumblePassword);
        Button mumbleGuide = action("BRANDMEISTER MUMBLE KURULUMUNU AÇ", PANEL_2);
        mumbleGuide.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showDmrPttSetup$29(view);
            }
        });
        linearLayoutColumn.addView(mumbleGuide);
        linearLayoutColumn.addView(title("ALTERNATİF 2 • DVSWITCH SUNUCUSU", 19));
        linearLayoutColumn.addView(info("Android telefon/tablet yalnızca düşük gecikmeli PCM/USRP sesi taşır. DMR/AMBE dönüşümü size ait Raspberry Pi veya sunucudaki Analog_Bridge + MMDVM_Bridge tarafından yapılır.", GREEN));
        final EditText dvsHost = field("DVSwitch / Analog_Bridge sunucusu", false);
        dvsHost.setText(prefs.getString("dvs_host", ""));
        final EditText dvsPort = field("USRP portu (50000–59999)", false);
        dvsPort.setInputType(2);
        dvsPort.setText(prefs.getString("dvs_port", "50111"));
        final EditText dvsUser = field("DVSwitch hesap adı", false);
        dvsUser.setText(prefs.getString("dvs_user", this.callsign));
        final EditText dvsPassword = field("DVSwitch hesap parolası", true);
        linearLayoutColumn.addView(label("SUNUCU"));
        linearLayoutColumn.addView(dvsHost);
        linearLayoutColumn.addView(label("USRP PORTU"));
        linearLayoutColumn.addView(dvsPort);
        linearLayoutColumn.addView(label("HESAP"));
        linearLayoutColumn.addView(dvsUser);
        linearLayoutColumn.addView(label("PAROLA"));
        linearLayoutColumn.addView(dvsPassword);
        Button dvsGuide = action("DVSWITCH ALTYAPI BELGESİNİ AÇ", PANEL_2);
        dvsGuide.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showDmrPttSetup$30(view);
            }
        });
        linearLayoutColumn.addView(dvsGuide);
        Button save = action("BAĞLANTI PARAMETRELERİNİ GÜVENLİ KAYDET", GREEN);
        save.setTextColor(f15BG);
        save.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showDmrPttSetup$31(bmId, essid, bmPassword, mumblePassword, dvsPassword, prefs, master, masterPort, mumbleServer, mumblePort, mumbleUser, dvsHost, dvsPort, dvsUser, view);
            }
        });
        linearLayoutColumn.addView(save);
        Button ptt = action("BASILI TUT • PTT", PANEL_2);
        ptt.setEnabled(false);
        linearLayoutColumn.addView(ptt);
        linearLayoutColumn.addView(info("PTT şu anda bilinçli olarak kapalıdır: Mumble/DVSwitch bağlantısı veya BrandMeister uyumlu lisanslı AMBE vocoder sağlanmadan ses gönderilemez. Bu düğme ağda sahte yayın yapmaz.", MUTED));
        scrollView.addView(linearLayoutColumn);
        linearLayoutScreen.addView(scrollView, verticalWeight(1));
        setContentView(linearLayoutScreen);
    }

    static /* synthetic */ void lambda$showDmrPttSetup$28(SharedPreferences prefs, String backend, TextView selectedPtt, View v) {
        prefs.edit().putString("ptt_backend", backend).apply();
        selectedPtt.setText("SEÇİLİ PTT ALTYAPISI: " + backend);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDmrPttSetup$29(View v) {
        openWeb("https://help.brandmeister.us/mumble/mumble-mobile-app-setup/android");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDmrPttSetup$30(View v) {
        openWeb("https://dvswitch.org/");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDmrPttSetup$31(EditText bmId, EditText essid, EditText bmPassword, EditText mumblePassword, EditText dvsPassword, SharedPreferences prefs, EditText master, EditText masterPort, EditText mumbleServer, EditText mumblePort, EditText mumbleUser, EditText dvsHost, EditText dvsPort, EditText dvsUser, View v) {
        try {
            String id = bmId.getText().toString().trim();
            String e = essid.getText().toString().trim();
            if (!id.isEmpty() && !id.matches("\\d{7}")) {
                toast("DMR ID 7 haneli olmalıdır.");
                return;
            }
            if (!e.matches("\\d{2}")) {
                toast("ESSID 01–99 olmalıdır.");
                return;
            }
            this.dmrId = id;
            String bp = bmPassword.getText().toString();
            if (!bp.isEmpty()) {
                this.secureStore.saveBrandMeisterPassword(bp);
            }
            String mp = mumblePassword.getText().toString();
            if (!mp.isEmpty()) {
                this.secureStore.saveMumblePassword(mp);
            }
            String dp = dvsPassword.getText().toString();
            if (!dp.isEmpty()) {
                this.secureStore.saveDvSwitchPassword(dp);
            }
            prefs.edit().putString("dmr_id", this.dmrId).putString("bm_essid", e).putString("bm_master", master.getText().toString().trim()).putString("bm_port", masterPort.getText().toString().trim()).putString("mumble_server", mumbleServer.getText().toString().trim()).putString("mumble_port", mumblePort.getText().toString().trim()).putString("mumble_user", mumbleUser.getText().toString().trim()).putString("dvs_host", dvsHost.getText().toString().trim()).putString("dvs_port", dvsPort.getText().toString().trim()).putString("dvs_user", dvsUser.getText().toString().trim()).apply();
            toast("Telefon/tablet DMR bağlantı parametreleri kaydedildi.");
        } catch (Exception e2) {
            toast("Parolalar güvenli kaydedilemedi.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showConnections() {
        this.home = false;
        LinearLayout s = screen();
        s.addView(radioHeader());
        s.addView(pageTitle("‹", "BAĞLANTILAR"));
        LinearLayout list = column();
        list.addView(connection("APRS-IS", "rotate.aprs2.net:14580", true));
        list.addView(connection("aprs.fi API", this.secureStore.readApiKey().isEmpty() ? "Anahtar girilmedi" : "Hazır", !this.secureStore.readApiKey().isEmpty()));
        list.addView(connection("DMR / TG", "BrandMeister Last Heard + Hoseline ses • TG " + this.talkGroup, true));
        boolean backgroundEnabled = getPreferences(0).getBoolean("background_aprs", false);
        list.addView(connection("Arka plan APRS", backgroundEnabled ? "AKTİF • SmartBeaconing açık" : "Kapalı", backgroundEnabled));
        Button backgroundButton = action(backgroundEnabled ? "ARKA PLAN APRS'Yİ DURDUR" : "ARKA PLAN APRS'Yİ BAŞLAT", backgroundEnabled ? WARN : GREEN);
        backgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { toggleBackgroundAprs(); }
        });
        list.addView(backgroundButton);
        addMenu(list, "⌁", "APRS Ayarları", new Runnable() {
            @Override public void run() { MainActivity.this.showSetup(); }
        });
        addMenu(list, "▣", "Harita Ayarları", new Runnable() {
            @Override public void run() { MainActivity.this.showMapSettings(); }
        });
        addMenu(list, "▥", "APRS Sunucuları", new Runnable() {
            @Override public void run() { MainActivity.this.showAprsServers(); }
        });
        addMenu(list, "◉", "APRS Canlı Monitor", new Runnable() {
            @Override public void run() { MainActivity.this.showDiagnostics(); }
        });
        addMenu(list, "♬", "Havadaki DMR Kullanıcıları", new Runnable() {
            @Override public void run() { MainActivity.this.showTalkerHistory(); }
        });
        addMenu(list, "☏", "Son QSO / Mesajlar", new Runnable() {
            @Override public void run() { MainActivity.this.showMessages(MainActivity.this.callsign); }
        });
        addMenu(list, "★", "Favori İstasyonlar", new Runnable() {
            @Override public void run() { MainActivity.this.showFavorites(); }
        });
        addMenu(list, "▤", "Kayıtlar ve Teşhis", new Runnable() {
            @Override public void run() { MainActivity.this.showDiagnostics(); }
        });
        addMenu(list, "♙", "Profiller ve Yedek", new Runnable() {
            @Override public void run() { MainActivity.this.showProfilesAndBackup(); }
        });
        addMenu(list, "⌑", "Güvenlik", new Runnable() {
            @Override public void run() { MainActivity.this.showSecurity(); }
        });
        addMenu(list, "✓", "Saha Test Merkezi", new Runnable() {
            @Override public void run() { MainActivity.this.showFieldTestCenter(); }
        });
        addMenu(list, "⌁", "SmartBeacon Profili", new Runnable() {
            @Override public void run() { MainActivity.this.showBeaconProfiles(); }
        });
        addMenu(list, "⌨", "Hytera Tuş Öğrenme", new Runnable() {
            @Override public void run() { MainActivity.this.showKeyLearner(); }
        });
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(list);
        s.addView(scroll, verticalWeight(1));
        setContentView(s);
    }

    private void showAprsServers() {
        this.home = false;
        LinearLayout root = screen(); root.addView(radioHeader()); root.addView(pageTitle("‹", "APRS SUNUCULARI"));
        LinearLayout body = column();
        body.addView(connection("Otomatik Avrupa Havuzu", "rotate.aprs2.net:14580 • 8 sn bağlantı zaman aşımı", true));
        body.addView(info("Alım filtresi tabletin son geçerli GPS konumuna göre 100 km yarıçapla açılır. Bağlantı kesilirse 5 saniye sonra otomatik yeniden denenir.", MUTED));
        body.addView(connection("APRS.fi", secureStore.readApiKey().isEmpty() ? "API anahtarı eksik" : "API anahtarı güvenli depoda hazır", !secureStore.readApiKey().isEmpty()));
        Button test = action("BAĞLANTI TEST MERKEZİNİ AÇ", GREEN); test.setOnClickListener(v -> showFieldTestCenter()); body.addView(test);
        ScrollView scroll = new ScrollView(this); scroll.addView(body); root.addView(scroll, verticalWeight(1)); setContentView(root);
    }

    private void toggleBackgroundAprs() {
        boolean enabled = getPreferences(0).getBoolean("background_aprs", false);
        Intent service = new Intent(this, AprsBackgroundService.class);
        if (enabled) {
            stopService(service);
            getPreferences(0).edit().putBoolean("background_aprs", false).apply();
            toast("Arka plan APRS durduruldu.");
        } else {
            if (this.callsign.isEmpty()) {
                toast("Önce Profilim ekranından çağrı işaretini girin.");
                return;
            }
            if (checkSelfPermissionCompat("android.permission.ACCESS_FINE_LOCATION") != 0) {
                requestLocationPermission();
                toast("Konum izninden sonra arka plan APRS'yi tekrar başlatın.");
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermissionCompat("android.permission.POST_NOTIFICATIONS") != 0) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 43);
                toast("Bildirim izninden sonra arka plan APRS'yi tekrar başlatın.");
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            getPreferences(0).edit().putBoolean("background_aprs", true).apply();
            toast("Arka plan APRS ve SmartBeaconing başlatıldı.");
        }
        showConnections();
    }

    private View connection(String name, String detail, boolean ok) {
        TextView v = text((ok ? "● " : "○ ") + name + "\n    " + detail, 16, ok ? GREEN : TEXT, true);
        v.setPadding(m11dp(18), m11dp(15), m11dp(18), m11dp(15));
        v.setBackgroundColor(PANEL);
        return v;
    }

    private void showBeacon() {
        if (!this.aprs.isVerified()) {
            toast("APRS-IS doğrulanmadı. Profilim ekranında passcode girin.");
            return;
        }
        if (checkSelfPermissionCompat("android.permission.ACCESS_FINE_LOCATION") == 0) {
            LocationManager lm = (LocationManager) getSystemService("location");
            Location location = null;
            try {
                location = lm.getLastKnownLocation("gps");
                if (location == null) {
                    location = lm.getLastKnownLocation("network");
                }
            } catch (SecurityException e) {
            }
            if (location == null) {
                toast("Geçerli GPS konumu bulunamadı.");
                return;
            }
            String symbolSetting = getPreferences(0).getString("aprs_symbol", ">");
            char beaconSymbol = symbolSetting.isEmpty() ? '>' : symbolSetting.charAt(0);
            boolean sent = this.aprs.sendBeacon(location.getLatitude(), location.getLongitude(), location.hasAltitude() ? location.getAltitude() : 0.0d, location.hasSpeed() ? ((double) location.getSpeed()) * 3.6d : 0.0d, location.hasBearing() ? location.getBearing() : 0.0d, beaconSymbol, "Nexvo APRS • TG " + this.talkGroup);
            if (this.signalPanel != null) this.signalPanel.setText("GPS ●   NET ●   RX " + this.stationCount + "   TX " + (sent ? "●" : "✕"));
            toast(sent ? "Beacon APRS-IS ağına gönderildi." : "Beacon gönderilemedi.");
            return;
        }
        requestLocationPermission();
    }

    private void startReception() {
        this.stationCount = 0;
        this.reconnectPending = false;
        Location location = getBestLastLocation();
        double latitude = location == null ? 41.0082d : location.getLatitude();
        double longitude = location == null ? 28.9784d : location.getLongitude();
        if (this.map != null && location != null) {
            this.map.updateStation(this.callsign, latitude, longitude, true, location.hasSpeed() && location.getSpeed() * 3.6f >= 3.0f);
        }
        this.aprs.connect(this.callsign, this.secureStore.readAprsPasscode(), latitude, longitude, new C01164());
        this.brandMeister.connect(this.talkGroup, new C01175());
    }

    private Location getBestLastLocation() {
        if (checkSelfPermissionCompat("android.permission.ACCESS_FINE_LOCATION") != 0
                && checkSelfPermissionCompat("android.permission.ACCESS_COARSE_LOCATION") != 0) return null;
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) return null;
        try {
            Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gps == null) return network;
            if (network == null) return gps;
            return gps.getTime() >= network.getTime() ? gps : network;
        } catch (SecurityException ignored) { return null; }
    }

    private void enqueueAprsPacket(final AprsPacketParser.Packet packet) {
        boolean schedule;
        synchronized (this.pendingAprsPackets) {
            this.pendingAprsPackets.put(packet.source, packet);
            schedule = !this.aprsUiFlushScheduled;
            if (schedule) this.aprsUiFlushScheduled = true;
        }
        if (!schedule) return;
        runOnUiThread(new Runnable() {
            @Override public void run() {
                getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override public void run() { flushAprsPackets(); }
                }, 300L);
            }
        });
    }

    private void flushAprsPackets() {
        ArrayList<AprsPacketParser.Packet> packets;
        synchronized (this.pendingAprsPackets) {
            packets = new ArrayList<>(this.pendingAprsPackets.values());
            this.pendingAprsPackets.clear();
            this.aprsUiFlushScheduled = false;
        }
        if (this.destroyed) return;
        this.stationCount += packets.size();
        WebMapPanel currentMap = this.map;
        for (AprsPacketParser.Packet packet : packets) {
            if (currentMap != null) {
                currentMap.updateStation(packet.source, packet.latitude, packet.longitude,
                        sameStationIdentity(packet.source, this.callsign));
            }
        }
        if (!packets.isEmpty()) {
            AprsPacketParser.Packet last = packets.get(packets.size() - 1);
            this.featureStore.log("RX", "PACKET", last.source + " " + last.latitude + "," + last.longitude);
        }
        if (this.rxState != null) this.rxState.setText("GPS ●  APRS ●  RX " + this.stationCount);
        if (this.signalPanel != null) this.signalPanel.setText("GPS ●   NET ●   RX " + this.stationCount + "   TX —");
        synchronized (this.pendingAprsPackets) {
            if (!this.pendingAprsPackets.isEmpty() && !this.aprsUiFlushScheduled) {
                this.aprsUiFlushScheduled = true;
                getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override public void run() { flushAprsPackets(); }
                }, 300L);
            }
        }
    }

    private static boolean sameStationIdentity(String first, String second) {
        if (first == null || second == null) return false;
        String a = first.trim().toUpperCase(Locale.ROOT);
        String b = second.trim().toUpperCase(Locale.ROOT);
        if (a.equals(b)) return true;
        int dashA = a.indexOf('-');
        int dashB = b.indexOf('-');
        if (dashA > 0) a = a.substring(0, dashA);
        if (dashB > 0) b = b.substring(0, dashB);
        return !a.isEmpty() && a.equals(b);
    }

    /* JADX INFO: renamed from: tr.aprs.app.MainActivity$4 */
    class C01164 implements AprsIsClient.Listener {
        C01164() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onState$0(String state) {
            if (MainActivity.this.rxState != null) {
                MainActivity.this.rxState.setText("GPS ●  APRS ●  " + state);
            }
            if (MainActivity.this.signalPanel != null) {
                Location location = MainActivity.this.getBestLastLocation();
                String gps = location == null ? "GPS YOK" : "GPS ±" + Math.round(location.hasAccuracy() ? location.getAccuracy() : 0) + "m";
                MainActivity.this.signalPanel.setText(gps + "   NET " + (state.contains("BAĞLI DEĞİL") ? "○" : "●") + "   RX " + MainActivity.this.stationCount + "   TX —");
            }
        }

        @Override // tr.aprs.app.AprsIsClient.Listener
        public void onState(final String state) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$4$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    C01164.this.lambda$onState$0(state);
                }
            });
        }

        @Override // tr.aprs.app.AprsIsClient.Listener
        public void onPacket(final AprsPacketParser.Packet p) {
            MainActivity.this.enqueueAprsPacket(p);
        }

        @Override // tr.aprs.app.AprsIsClient.Listener
        public void onMessage(final AprsIsClient.AprsMessage message) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$4$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    C01164.this.lambda$onMessage$2(message);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onMessage$2(AprsIsClient.AprsMessage message) {
            if (!message.acknowledgement && !message.rejection && message.destination.equalsIgnoreCase(MainActivity.this.callsign)) {
                MainActivity.this.featureStore.message(message.source, "GELEN", message.text, "ALINDI");
                MainActivity.this.toast("Yeni APRS mesajı: " + message.source + " • " + message.text);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$3() {
            if (MainActivity.this.rxState != null) {
                MainActivity.this.rxState.setText("APRS ○  BAĞLANTI YOK");
            }
        }

        @Override // tr.aprs.app.AprsIsClient.Listener
        public void onError(String e) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$4$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    C01164.this.lambda$onError$3();
                }
            });
            if (!MainActivity.this.destroyed && !MainActivity.this.reconnectPending) {
                MainActivity.this.reconnectPending = true;
                MainActivity.this.runOnUiThread(() -> MainActivity.this.getWindow().getDecorView().postDelayed(() -> {
                    if (!MainActivity.this.destroyed) MainActivity.this.startReception();
                }, 5000L));
            }
        }
    }

    /* JADX INFO: renamed from: tr.aprs.app.MainActivity$5 */
    class C01175 implements BrandMeisterClient.Listener {
        C01175() {
        }

        @Override // tr.aprs.app.BrandMeisterClient.Listener
        public void onState(final String state) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$5$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    C01175.this.lambda$onState$0(state);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onState$0(String state) {
            boolean z = false;
            if (MainActivity.this.talkGroup.equals(MainActivity.this.getPreferences(0).getString("last_talker_tg", "")) && !MainActivity.this.getPreferences(0).getString("last_talker_text", "").isEmpty()) {
                z = true;
            }
            boolean hasSavedTalker = z;
            if (MainActivity.this.activeTalker != null && !hasSavedTalker && MainActivity.this.activeTalker.getText().toString().contains("BEKLENİYOR")) {
                MainActivity.this.activeTalker.setText("TG " + MainActivity.this.talkGroup + " • " + state.toUpperCase(Locale.ROOT));
            }
        }

        @Override // tr.aprs.app.BrandMeisterClient.Listener
        public void onTalker(final BrandMeisterClient.Talker t) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$5$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    C01175.this.lambda$onTalker$1(t);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onTalker$1(BrandMeisterClient.Talker t) {
            String outputName;
            String phase = "Session-Stop".equalsIgnoreCase(t.event) ? "SON KONUŞAN" : "KONUŞUYOR";
            MainActivity.this.recordTalker(t, phase);
            if (!MainActivity.this.secureStore.readNexvoToken().isEmpty()
                    && !MainActivity.this.secureStore.readAprsPasscode().isEmpty()) {
                MainActivity.this.nexvo.publishTalker(
                        MainActivity.this.secureStore.readNexvoToken(), MainActivity.this.callsign,
                        MainActivity.this.secureStore.readAprsPasscode(), String.valueOf(t.talkGroup),
                        t.dmrId, t.callsign, t.name, t.event, t.slot, t.linkCall,
                        new NexvoApiClient.Callback<JSONObject>() {
                            public void onSuccess(JSONObject ignored) { }
                            public void onError(String ignored) { }
                        });
            }
            String resolvedCallsign = t.callsign;
            if (resolvedCallsign.isEmpty() && !MainActivity.this.dmrId.isEmpty()
                    && MainActivity.this.dmrId.equals(String.valueOf(t.dmrId))) {
                resolvedCallsign = MainActivity.this.callsign;
            }
            String identity = resolvedCallsign.isEmpty() ? "DMR " + t.dmrId : resolvedCallsign;
            String talkerText = "TG " + t.talkGroup + " • " + phase + "\nİSİM: " + (t.name.isEmpty() ? "BİLİNMİYOR" : t.name) + "\nÇAĞRI: " + identity + " • DMR " + t.dmrId + " • SLOT " + t.slot + (t.alias.isEmpty() ? "" : "\nALIAS: " + t.alias) + (t.linkCall.isEmpty() ? "" : "\nBAĞLANTI: " + t.linkCall);
            if (MainActivity.this.activeTalker != null) {
                MainActivity.this.activeTalker.setText(talkerText);
                MainActivity.this.activeTalker.setTextColor(MainActivity.GREEN);
            }
            MainActivity.this.getPreferences(0).edit().putString("last_talker_tg", String.valueOf(t.talkGroup)).putString("last_talker_text", talkerText).apply();
            if (MainActivity.this.map != null && Math.abs(t.linkLatitude) <= 90.0d && Math.abs(t.linkLongitude) <= 180.0d && t.linkLatitude != 0.0d && t.linkLongitude != 0.0d) {
                if (t.linkCall.isEmpty()) {
                    outputName = t.linkId > 0 ? "DMR " + t.linkId : "DMR ÇIKIŞI";
                } else {
                    outputName = t.linkCall;
                }
                String outputDetail = "KONUŞMACI: " + identity + "\nİSİM: " + (t.name.isEmpty() ? "BİLİNMİYOR" : t.name) + "\nÇIKIŞ: " + outputName + (t.linkId > 0 ? "\nÇIKIŞ ID: " + t.linkId : "") + "\nTG " + t.talkGroup + " • SLOT " + t.slot;
                MainActivity.this.map.updateDmrStation("ÇIKIŞ " + outputName, t.linkLatitude, t.linkLongitude, outputDetail);
                MainActivity.this.selectedCard.setText(outputDetail.replace("\n", " • "));
            }
            if (!resolvedCallsign.isEmpty()) {
                MainActivity.this.queryAprsSilent(resolvedCallsign, String.valueOf(t.dmrId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recordTalker(BrandMeisterClient.Talker talker, String phase) {
        long now = System.currentTimeMillis();
        if (!this.talkerHistory.isEmpty()) {
            TalkerEntry latest = this.talkerHistory.get(0);
            if (latest.dmrId == talker.dmrId && latest.talkGroup == talker.talkGroup) {
                if (latest.phase.equals(phase) && now - latest.time < 3000) {
                    return;
                }
            }
        }
        this.talkerHistory.add(0, new TalkerEntry(talker.callsign, talker.dmrId, talker.talkGroup, talker.slot, phase, now));
        while (this.talkerHistory.size() > 100) {
            this.talkerHistory.remove(this.talkerHistory.size() - 1);
        }
    }

    private void showTalkerHistory() {
        final Dialog dialog = new Dialog(this);
        LinearLayout linearLayoutColumn = column();
        linearLayoutColumn.setPadding(m11dp(16), m11dp(12), m11dp(16), m11dp(16));
        linearLayoutColumn.addView(title("TG " + this.talkGroup + " • KONUŞMACILAR", 21));
        linearLayoutColumn.addView(dialogBack(dialog, this::showTalkGroupDialog));
        linearLayoutColumn.addView(info("BrandMeister canlı akışından bu cihaz açıkken alınan son 100 görüşme olayı.", MUTED));
        LinearLayout rows = column();
        if (this.talkerHistory.isEmpty()) {
            rows.addView(info("Henüz bu TG için konuşmacı olayı alınmadı.", MUTED));
        } else {
            SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
            for (TalkerEntry entry : this.talkerHistory) {
                if (String.valueOf(entry.talkGroup).equals(this.talkGroup)) {
                    TextView row = text(clock.format(new Date(entry.time)) + "  •  " + entry.phase + "\n" + entry.callsign + "  •  DMR " + entry.dmrId + "  •  SLOT " + entry.slot, 14, "KONUŞUYOR".equals(entry.phase) ? GREEN : TEXT, true);
                    row.setPadding(m11dp(12), m11dp(10), m11dp(12), m11dp(10));
                    row.setBackground(round(PANEL_2, LINE, 1, 6));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                    params.setMargins(0, m11dp(3), 0, m11dp(3));
                    rows.addView(row, params);
                }
            }
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(rows);
        linearLayoutColumn.addView(scroll, new LinearLayout.LayoutParams(-1, m11dp(this.compact ? 310 : 480)));
        Button clear = action("GEÇMİŞİ TEMİZLE", PANEL_2);
        clear.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.MainActivity$$ExternalSyntheticLambda22
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$showTalkerHistory$33(dialog, view);
            }
        });
        linearLayoutColumn.addView(clear);
        dialog.setContentView(linearLayoutColumn);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(round(f15BG, GREEN, 1, 10));
        }
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showTalkerHistory$33(Dialog dialog, View v) {
        this.talkerHistory.clear();
        dialog.dismiss();
        toast("TG konuşmacı geçmişi temizlendi.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryAprsSilent(String call, String dmr) {
        String key = this.secureStore.readApiKey();
        if (key.isEmpty()) {
            return;
        }
        new AprsFiClient().getLocation(call, key, new C01186(dmr));
    }

    /* JADX INFO: renamed from: tr.aprs.app.MainActivity$6 */
    class C01186 implements AprsFiClient.Callback {
        final /* synthetic */ String val$dmr;

        C01186(String str) {
            this.val$dmr = str;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onSuccess$0(AprsFiClient.Station s, String dmr) {
            MainActivity.this.updateStation(s, dmr);
            if (MainActivity.this.map != null) MainActivity.this.map.pinStation(s.name, true);
        }

        @Override // tr.aprs.app.AprsFiClient.Callback
        public void onSuccess(final AprsFiClient.Station s) {
            MainActivity mainActivity = MainActivity.this;
            final String str = this.val$dmr;
            mainActivity.runOnUiThread(new Runnable() { // from class: tr.aprs.app.MainActivity$6$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    C01186.this.lambda$onSuccess$0(s, str);
                }
            });
        }

        @Override // tr.aprs.app.AprsFiClient.Callback
        public void onError(String e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void selectStation(String call) {
        queryAprs(call, "");
    }

    private void openHoseline() {
        Intent intent = new Intent(this, (Class<?>) HoselineActivity.class);
        intent.putExtra("talkgroup", this.talkGroup);
        startActivity(intent);
    }

    private void openWeb(String url) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
    }

    private static final class TalkerEntry {
        final String callsign;
        final long dmrId;
        final String phase;
        final int slot;
        final long talkGroup;
        final long time;

        TalkerEntry(String callsign, long dmrId, long talkGroup, int slot, String phase, long time) {
            this.callsign = (callsign == null || callsign.isEmpty()) ? "BİLİNMİYOR" : callsign;
            this.dmrId = dmrId;
            this.talkGroup = talkGroup;
            this.slot = slot;
            this.phase = phase;
            this.time = time;
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.home) {
            super.onBackPressed();
        } else if (this.mainMenuVisible) {
            showDashboard();
        } else {
            showMainMenu();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.destroyed = true;
        stopTgListening();
        this.aprs.disconnect();
        this.brandMeister.disconnect();
        if (this.featureStore != null) this.featureStore.close();
        super.onDestroy();
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int code, KeyEvent event) {
        if (getPreferences(0).getBoolean("key_learning", false) && code != KeyEvent.KEYCODE_BACK) { getPreferences(0).edit().putInt("last_key_code", code).apply(); showKeyLearner(); return true; }
        if (code == getPreferences(0).getInt("key_beacon", -999)) { showBeacon(); return true; }
        if (code == getPreferences(0).getInt("key_message", -999)) { showMessages(this.callsign); return true; }
        if (code == getPreferences(0).getInt("key_menu", -999)) { showMainMenu(); return true; }
        if (code == KeyEvent.KEYCODE_F1 || code == KeyEvent.KEYCODE_CAMERA) { showBeacon(); return true; }
        if (code == KeyEvent.KEYCODE_F2 || code == KeyEvent.KEYCODE_HEADSETHOOK) { showMessages(this.callsign); return true; }
        if (code == KeyEvent.KEYCODE_F3) { showMainMenu(); return true; }
        if (code != 4 || this.home) {
            return super.onKeyDown(code, event);
        }
        if (this.mainMenuVisible) {
            showDashboard();
        } else {
            showMainMenu();
        }
        return true;
    }

    private LinearLayout screen() {
        LinearLayout v = column();
        v.setBackgroundColor(f15BG);
        return v;
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(1);
        v.setBackgroundColor(f15BG);
        return v;
    }

    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(0);
        return v;
    }

    private LinearLayout.LayoutParams weight(int value) {
        return new LinearLayout.LayoutParams(0, -1, value);
    }

    private LinearLayout.LayoutParams verticalWeight(int value) {
        return new LinearLayout.LayoutParams(-1, 0, value);
    }

    private TextView title(String value, int size) {
        TextView v = text(value, size, TEXT, true);
        v.setPadding(0, m11dp(8), 0, m11dp(8));
        return v;
    }

    private TextView label(String value) {
        TextView v = text(value, 12, GREEN, true);
        v.setPadding(0, m11dp(12), 0, m11dp(5));
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView info(String value, int color) {
        TextView v = text(value, 12, color, false);
        v.setPadding(m11dp(2), m11dp(8), m11dp(2), m11dp(8));
        return v;
    }

    private TextView led(String value, int size) {
        TextView v = text(value, size, GREEN, true);
        v.setTypeface(Typeface.MONOSPACE, 1);
        v.setGravity(17);
        return v;
    }

    private TextView metric(String name, String value) {
        TextView v = text(name + "\n" + value, this.compact ? 9 : (this.denseUi ? 10 : 11), TEXT, false);
        v.setGravity(17);
        v.setPadding(m11dp(3), m11dp(this.denseUi ? 2 : 4), m11dp(3), m11dp(this.denseUi ? 2 : 4));
        v.setBackgroundColor(PANEL);
        return v;
    }

    private EditText field(String hint, boolean secret) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setSingleLine(true);
        e.setTextSize(16.0f);
        e.setPadding(m11dp(12), 0, m11dp(12), 0);
        e.setBackground(round(PANEL_2, LINE, 1, 6));
        e.setInputType(secret ? 129 : 1);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, m11dp(52));
        p.setMargins(0, m11dp(3), 0, m11dp(7));
        e.setLayoutParams(p);
        return e;
    }

    private Button action(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(TEXT);
        b.setTextSize(this.denseUi ? 11.0f : 13.0f);
        b.setTypeface(Typeface.DEFAULT, 1);
        b.setAllCaps(false);
        b.setBackground(round(color, color, 1, 6));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, m11dp(this.denseUi ? 44 : 50));
        p.setMargins(m11dp(3), m11dp(this.denseUi ? 2 : 3), m11dp(3), m11dp(this.denseUi ? 2 : 3));
        b.setLayoutParams(p);
        return b;
    }

    private Button smallButton(String value) {
        Button b = action(value, f15BG);
        b.setTextColor(GREEN);
        b.setBackground(round(f15BG, GREEN, 1, 5));
        b.setMinWidth(0);
        b.setPadding(m11dp(10), 0, m11dp(10), 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(-2, m11dp(38)));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) {
            v.setTypeface(Typeface.DEFAULT, 1);
        }
        return v;
    }

    private GradientDrawable round(int fill, int stroke, int width, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(m11dp(radius));
        g.setStroke(m11dp(width), stroke);
        return g;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 42);
    }

    private int checkSelfPermissionCompat(String permission) {
        return checkSelfPermission(permission);
    }

    /* JADX INFO: renamed from: dp */
    private int m11dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(String s) {
        Toast.makeText(this, s, 1).show();
    }
}
