package tr.aprs.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
final class RadioIdClient {

    interface Callback {
        void onError(String str);

        void onSuccess(Identity identity);
    }

    RadioIdClient() {
    }

    static final class Identity {
        final String callsign;
        final String city;
        final String country;
        final String dmrId;
        final String name;

        Identity(String callsign, String dmrId, String name, String city, String country) {
            this.callsign = callsign;
            this.dmrId = dmrId;
            this.name = name;
            this.city = city;
            this.country = country;
        }
    }

    void lookup(final String query, final Callback callback) {
        new Thread(new Runnable() { // from class: tr.aprs.app.RadioIdClient$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RadioIdClient.lambda$lookup$0(query, callback);
            }
        }, "radioid-lookup").start();
    }

    static /* synthetic */ void lambda$lookup$0(String query, Callback callback) {
        HttpURLConnection connection = null;
        try {
            try {
                boolean numeric = query.matches("\\d{5,9}");
                String parameter = numeric ? "id=" : "callsign=";
                String selector = numeric ? "&id_sel=%3D" : "&callsign_sel=%3D";
                URL url = new URL("https://database.radioid.net/api/users?" + parameter + URLEncoder.encode(query, "UTF-8") + selector + "&page=1&per_page=10");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestProperty("User-Agent", "TR-APRS/0.3.0 (contact: local-user)");
                int code = connection.getResponseCode();
                InputStream stream = (code < 200 || code >= 300) ? connection.getErrorStream() : connection.getInputStream();
                JSONObject root = new JSONObject(readAll(stream));
                JSONArray results = root.optJSONArray("results");
                if (results == null || results.length() == 0) {
                    throw new IllegalStateException("RadioID kaydı bulunamadı");
                }
                JSONObject item = results.getJSONObject(0);
                Identity identity = new Identity(item.optString("callsign", query).toUpperCase(Locale.ROOT), String.valueOf(item.optLong("id", 0L)), item.optString("name", ""), item.optString("city", ""), item.optString("country", ""));
                callback.onSuccess(identity);
                if (connection == null) {
                    return;
                }
                connection.disconnect();
            } catch (Exception error) {
                callback.onError(error.getMessage() == null ? "RadioID sorgusu başarısız" : error.getMessage());
                if (0 == 0) {
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                connection.disconnect();
            }
            throw th;
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "{}";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return result.toString();
            }
            result.append(line);
        }
    }
}
