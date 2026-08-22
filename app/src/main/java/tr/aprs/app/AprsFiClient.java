package tr.aprs.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
final class AprsFiClient {

    interface Callback {
        void onError(String str);

        void onSuccess(Station station);
    }

    interface MessageCallback {
        void onError(String str);

        void onSuccess(List<Message> list);
    }

    AprsFiClient() {
    }

    static final class Message {
        final String destination;

        /* JADX INFO: renamed from: id */
        final String f12id;
        final String source;
        final String text;
        final long time;

        Message(String source, String destination, String text, long time, String id) {
            this.source = source;
            this.destination = destination;
            this.text = text;
            this.time = time;
            this.f12id = id;
        }
    }

    static final class Station {
        final double altitude;
        final String comment;
        final double course;
        final long lastTime;
        final double latitude;
        final double longitude;
        final String name;
        final double speed;

        Station(String name, double latitude, double longitude, double speed, double altitude, double course, long lastTime, String comment) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.speed = speed;
            this.altitude = altitude;
            this.course = course;
            this.lastTime = lastTime;
            this.comment = comment;
        }
    }

    void getLocation(final String callsign, final String apiKey, final Callback callback) {
        new Thread(new Runnable() { // from class: tr.aprs.app.AprsFiClient$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AprsFiClient.lambda$getLocation$0(callsign, apiKey, callback);
            }
        }).start();
    }

    /* JADX WARN: Code duplicated, block: B:41:0x0128  */
    static /* synthetic */ void lambda$getLocation$0(String callsign, String apiKey, Callback callback) {
        HttpURLConnection connection = null;
        try {
            String query = "name=" + URLEncoder.encode(callsign, "UTF-8") + "&what=loc&apikey=" + URLEncoder.encode(apiKey, "UTF-8") + "&format=json";
            connection = (HttpURLConnection) new URL("https://api.aprs.fi/api/get?" + query).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("User-Agent", "TR-APRS/0.10.1");
            int code = connection.getResponseCode();
            InputStream stream = (code < 200 || code >= 300) ? connection.getErrorStream() : connection.getInputStream();
            JSONObject root = new JSONObject(readAll(stream));
            if (!"ok".equals(root.optString("result"))) {
                throw new IllegalStateException(root.optString("description", "aprs.fi sorgusu başarısız"));
            }
            JSONArray entries = root.optJSONArray("entries");
            if (entries == null || entries.length() == 0) {
                throw new IllegalStateException("İstasyon bulunamadı");
            }
            JSONObject e = entries.getJSONObject(0);
            callback.onSuccess(new Station(e.optString("name", callsign), number(e, "lat"), number(e, "lng"), number(e, "speed"), number(e, "altitude"), number(e, "course"), e.optLong("lasttime", 0L), e.optString("comment", "")));
        } catch (Exception error) {
            callback.onError(error.getMessage() == null ? "Bağlantı hatası" : error.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    void getMessages(final String callsign, final String apiKey, final MessageCallback callback) {
        new Thread(new Runnable() { // from class: tr.aprs.app.AprsFiClient$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AprsFiClient.lambda$getMessages$1(callsign, apiKey, callback);
            }
        }, "aprsfi-messages").start();
    }

    static /* synthetic */ void lambda$getMessages$1(String callsign, String apiKey, MessageCallback callback) {
        String message = "Mesaj sorgusu başarısız";
        HttpURLConnection connection = null;
        try {
                String query = "what=msg&dst=" + URLEncoder.encode(callsign, "UTF-8") + "&apikey=" + URLEncoder.encode(apiKey, "UTF-8") + "&format=json";
                URL url = new URL("https://api.aprs.fi/api/get?" + query);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestProperty("User-Agent", "TR-APRS/0.3.0 (+https://example.invalid/tr-aprs)");
                int code = connection.getResponseCode();
                InputStream stream = (code < 200 || code >= 300) ? connection.getErrorStream() : connection.getInputStream();
                JSONObject root = new JSONObject(readAll(stream));
                if (!"ok".equals(root.optString("result"))) {
                    throw new IllegalStateException(root.optString("description", "Mesaj sorgusu başarısız"));
                }
                ArrayList<Message> messages = new ArrayList<>();
                JSONArray entries = root.optJSONArray("entries");
                if (entries != null) {
                    int i = 0;
                    while (i < entries.length()) {
                        JSONObject e = entries.getJSONObject(i);
                        messages.add(new Message(e.optString("srccall", ""), e.optString("dst", callsign), e.optString("message", ""), e.optLong("time", 0L), e.optString("messageid", "")));
                        i++;
                        query = query;
                        url = url;
                    }
                }
                callback.onSuccess(messages);
        } catch (Exception error) {
            if (error.getMessage() != null) {
                message = error.getMessage();
            }
            callback.onError(message);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static double number(JSONObject object, String key) {
        try {
            return Double.parseDouble(object.optString(key, "0"));
        } catch (NumberFormatException e) {
            return 0.0d;
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
