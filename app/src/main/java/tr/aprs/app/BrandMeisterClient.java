package tr.aprs.app;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import java.net.URISyntaxException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
final class BrandMeisterClient {
    private Socket socket;
    private volatile long watchedTalkGroup;

    interface Listener {
        void onState(String str);

        void onTalker(Talker talker);
    }

    BrandMeisterClient() {
    }

    static final class Talker {
        final String alias;
        final double ber;
        final String callsign;
        final long dmrId;
        final String event;
        final String linkCall;
        final long linkId;
        final double linkLatitude;
        final double linkLongitude;
        final String name;
        final double rssi;
        final int slot;
        final long talkGroup;

        Talker(String callsign, String name, String alias, String linkCall, String event, long dmrId, long talkGroup, long linkId, int slot, double rssi, double ber, double linkLatitude, double linkLongitude) {
            this.callsign = callsign;
            this.name = name;
            this.alias = alias;
            this.linkCall = linkCall;
            this.event = event;
            this.dmrId = dmrId;
            this.talkGroup = talkGroup;
            this.linkId = linkId;
            this.slot = slot;
            this.rssi = rssi;
            this.ber = ber;
            this.linkLatitude = linkLatitude;
            this.linkLongitude = linkLongitude;
        }
    }

    void connect(String talkGroup, final Listener listener) {
        disconnect();
        try {
            this.watchedTalkGroup = Long.parseLong(talkGroup);
            try {
                IO.Options options = new IO.Options();
                options.path = "/lh/socket.io";
                options.reconnection = true;
                options.forceNew = true;
                options.transports = new String[]{WebSocket.NAME};
                this.socket = IO.socket("https://api.brandmeister.network", options);
                this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() { // from class: tr.aprs.app.BrandMeisterClient$$ExternalSyntheticLambda0
                    @Override // io.socket.emitter.Emitter.Listener
                    public final void call(Object[] objArr) {
                        BrandMeisterClient.this.lambda$connect$0(listener, objArr);
                    }
                });
                this.socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() { // from class: tr.aprs.app.BrandMeisterClient$$ExternalSyntheticLambda1
                    @Override // io.socket.emitter.Emitter.Listener
                    public final void call(Object[] objArr) {
                        listener.onState("BrandMeister koptu");
                    }
                });
                this.socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() { // from class: tr.aprs.app.BrandMeisterClient$$ExternalSyntheticLambda2
                    @Override // io.socket.emitter.Emitter.Listener
                    public final void call(Object[] objArr) {
                        listener.onState("BrandMeister bağlantı yok");
                    }
                });
                this.socket.on("mqtt", new Emitter.Listener() { // from class: tr.aprs.app.BrandMeisterClient$$ExternalSyntheticLambda3
                    @Override // io.socket.emitter.Emitter.Listener
                    public final void call(Object[] objArr) {
                        BrandMeisterClient.this.lambda$connect$3(listener, objArr);
                    }
                });
                this.socket.connect();
            } catch (URISyntaxException e) {
                listener.onState("BrandMeister adres hatası");
            }
        } catch (Exception e2) {
            listener.onState("TG geçersiz");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$connect$0(Listener listener, Object[] args) {
        listener.onState("BrandMeister canlı");
        this.socket.emit("join", "everything");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$connect$3(Listener listener, Object[] args) {
        try {
            JSONObject envelope = args[0] instanceof JSONObject ? (JSONObject) args[0] : new JSONObject(String.valueOf(args[0]));
            Object raw = envelope.opt("payload");
            JSONObject data = raw instanceof JSONObject ? (JSONObject) raw : new JSONObject(String.valueOf(raw));
            long tg = number(data, "DestinationID");
            if (tg != this.watchedTalkGroup) {
                return;
            }
            Talker t = new Talker(data.optString("SourceCall", ""), data.optString("SourceName", ""), data.optString("TalkerAlias", ""), data.optString("LinkCall", data.optString("LinkName", "")), data.optString("Event", ""), number(data, "SourceID"), tg, firstNumber(data, "LinkID", "ContextID", "RepeaterID"), (int) number(data, "Slot"), data.optDouble("RSSI", 0.0d), data.optDouble("BER", 0.0d), firstDouble(data, "LinkLatitude", "Latitude", "Lat"), firstDouble(data, "LinkLongitude", "Longitude", "Lng", "Lon"));
            try {
                listener.onTalker(t);
            } catch (Exception e) {
            }
        } catch (Exception e2) {
        }
    }

    void disconnect() {
        if (this.socket != null) {
            this.socket.off();
            this.socket.disconnect();
            this.socket.close();
            this.socket = null;
        }
    }

    private static long number(JSONObject data, String key) {
        Object v = data.opt(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long firstNumber(JSONObject data, String... keys) {
        for (String key : keys) {
            long value = number(data, key);
            if (value != 0) {
                return value;
            }
        }
        return 0L;
    }

    private static double firstDouble(JSONObject data, String... keys) {
        double parsed;
        for (String key : keys) {
            Object value = data.opt(key);
            if (value != null) {
                try {
                    if (value instanceof Number) {
                        parsed = ((Number) value).doubleValue();
                    } else {
                        parsed = Double.parseDouble(String.valueOf(value));
                    }
                    if (parsed != 0.0d) {
                        return parsed;
                    }
                } catch (Exception e) {
                }
            }
        }
        return 0.0d;
    }
}
