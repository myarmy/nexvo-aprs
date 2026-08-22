package tr.aprs.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/* JADX INFO: loaded from: classes3.dex */
public final class HoselineActivity extends Activity {
    private WebView webView;

    @Override // android.app.Activity
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String talkGroup = getIntent().getStringExtra("talkgroup");
        if (talkGroup == null || !talkGroup.matches("\\d+")) {
            talkGroup = "28642";
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(Color.rgb(21, 24, 25));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(16);
        header.setPadding(10, 8, 12, 8);
        Button back = new Button(this);
        back.setText("‹  GERİ");
        back.setTextColor(Color.rgb(57, 217, 138));
        back.setTextSize(14.0f);
        back.setAllCaps(false);
        back.setBackgroundColor(Color.rgb(48, 54, 58));
        back.setOnClickListener(new View.OnClickListener() { // from class: tr.aprs.app.HoselineActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HoselineActivity.this.lambda$onCreate$0(view);
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(130, 52));
        TextView note = new TextView(this);
        note.setText("TG " + talkGroup + " • Resmî BrandMeister Hoseline\n▶ Oynat düğmesine dokunun. Bu ekran yalnızca dinleme (RX) yapar.");
        note.setTextColor(Color.rgb(57, 217, 138));
        note.setTextSize(14.0f);
        note.setPadding(16, 6, 12, 6);
        header.addView(note, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(header);
        this.webView = new WebView(this);
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(1);
        this.webView.setWebViewClient(new WebViewClient());
        this.webView.setWebChromeClient(new WebChromeClient());
        this.webView.setBackgroundColor(Color.rgb(21, 24, 25));
        linearLayout.addView(this.webView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        setContentView(linearLayout);
        this.webView.loadUrl("https://hose.brandmeister.network/#/?subscribe=" + talkGroup);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(View v) {
        finish();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        finish();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.webView != null) {
            this.webView.stopLoading();
            this.webView.destroy();
        }
        super.onDestroy();
    }
}
