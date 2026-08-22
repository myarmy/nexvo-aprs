package tr.aprs.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class NexvoApiClient {
    static final String BASE_URL = "https://api.selabiz.com/nexvo";

    static final class Message {
        final long id;
        final String sender;
        final String recipient;
        final String body;
        final String status;
        final String createdAt;

        Message(long id, String sender, String recipient, String body, String status, String createdAt) {
            this.id = id;
            this.sender = sender;
            this.recipient = recipient;
            this.body = body;
            this.status = status;
            this.createdAt = createdAt;
        }
    }

    interface Callback<T> {
        void onSuccess(T value);
        void onError(String error);
    }

    void health(Callback<Boolean> callback) {
        request("GET", "/health", "", null, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) { callback.onSuccess(value.optBoolean("ok") && value.optBoolean("database")); }
            @Override public void onError(String error) { callback.onError(error); }
        });
    }

    void send(String token, String from, String to, String body, Callback<Long> callback) {
        try {
            JSONObject payload = new JSONObject().put("from", from).put("to", to).put("body", body);
            request("POST", "/v1/messages", token, payload, new Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject value) { callback.onSuccess(value.optLong("id")); }
                @Override public void onError(String error) { callback.onError(error); }
            });
        } catch (Exception e) { callback.onError("Mesaj hazırlanamadı."); }
    }

    void messages(String token, String callsign, Callback<List<Message>> callback) {
        request("GET", "/v1/messages?callsign=" + callsign, token, null, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) {
                ArrayList<Message> result = new ArrayList<>();
                JSONArray items = value.optJSONArray("messages");
                if (items != null) for (int i = 0; i < items.length(); i++) {
                    JSONObject m = items.optJSONObject(i);
                    if (m != null) result.add(new Message(m.optLong("id"), m.optString("sender"), m.optString("recipient"), m.optString("body"), m.optString("status"), m.optString("created_at")));
                }
                callback.onSuccess(result);
            }
            @Override public void onError(String error) { callback.onError(error); }
        });
    }

    private void request(String method, String path, String token, JSONObject payload, Callback<JSONObject> callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");
                if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
                if (payload != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream out = connection.getOutputStream()) { out.write(payload.toString().getBytes(StandardCharsets.UTF_8)); }
                }
                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String body = read(stream);
                JSONObject json = body.isEmpty() ? new JSONObject() : new JSONObject(body);
                if (status >= 200 && status < 300) callback.onSuccess(json);
                else if (status == 401) callback.onError("Nexvo sunucu anahtarı geçersiz.");
                else callback.onError("Nexvo sunucu hatası: " + status);
            } catch (Exception e) {
                callback.onError("Nexvo sunucusuna ulaşılamadı.");
            } finally { if (connection != null) connection.disconnect(); }
        }, "nexvo-api").start();
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }
}
