package tr.aprs.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes3.dex */
final class AprsIsClient {
    private static final long[] RETRY_SECONDS = {0, 15, 30, 60, 120, 240};
    private String loginCall;
    private Socket socket;
    private volatile boolean verified;
    private BufferedWriter writer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger messageSequence = new AtomicInteger();
    private final Map<String, PendingMessage> pending = new ConcurrentHashMap();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    interface DeliveryCallback {
        void onStatus(String str, String str2, int r3);
    }

    interface Listener {
        void onError(String str);

        void onMessage(AprsMessage aprsMessage);

        void onPacket(AprsPacketParser.Packet packet);

        void onState(String str);
    }

    AprsIsClient() {
    }

    static final class AprsMessage {
        final boolean acknowledgement;
        final String destination;

        /* JADX INFO: renamed from: id */
        final String f13id;
        final boolean rejection;
        final String source;
        final String text;

        AprsMessage(String source, String destination, String text, String id, boolean acknowledgement, boolean rejection) {
            this.source = source;
            this.destination = destination;
            this.text = text;
            this.f13id = id;
            this.acknowledgement = acknowledgement;
            this.rejection = rejection;
        }
    }

    void connect(final String callsign, final String passcode, final double latitude, final double longitude, final Listener listener) {
        disconnect();
        this.running.set(true);
        new Thread(new Runnable() { // from class: tr.aprs.app.AprsIsClient$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AprsIsClient.this.lambda$connect$0(callsign, passcode, latitude, longitude, listener);
            }
        }, "aprs-is-rx").start();
    }

    void disconnect() {
        this.running.set(false);
        this.verified = false;
        for (PendingMessage message : this.pending.values()) {
            message.callback.onStatus(message.f14id, "BAĞLANTI KESİLDİ", message.attempt);
        }
        this.pending.clear();
        this.writer = null;
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (Exception e) {
            }
        }
    }

    boolean isVerified() {
        return this.verified;
    }

    synchronized boolean sendBeacon(double latitude, double longitude, double altitude, double speedKmh, double course, char symbol, String comment) {
        String payload;
        String ns = latitude >= 0.0d ? "N" : "S";
        String ew = longitude >= 0.0d ? "E" : "W";
        double alat = Math.abs(latitude);
        double alon = Math.abs(longitude);
        int latDeg = (int) alat;
        int lonDeg = (int) alon;
        double latMin = (alat - ((double) latDeg)) * 60.0d;
        double lonMin = (alon - ((double) lonDeg)) * 60.0d;
        int knots = (int) Math.max(0.0d, speedKmh / 1.852d);
        payload = String.format(Locale.US, "!%02d%05.2f%s/%03d%05.2f%s%c%03d/%03d/A=%06d %s", Integer.valueOf(latDeg), Double.valueOf(latMin), ns, Integer.valueOf(lonDeg), Double.valueOf(lonMin), ew, Character.valueOf(symbol), Integer.valueOf((int) course), Integer.valueOf(knots), Integer.valueOf((int) Math.max(0.0d, altitude * 3.28084d)), safe(comment, 36));
        return sendLine(this.loginCall + ">APTR04,TCPIP*:" + payload);
    }

    synchronized boolean sendMessage(String destination, String text, String id) {
        String payload;
        String call = destination.toUpperCase(Locale.ROOT);
        if (call.length() > 9) {
            call = call.substring(0, 9);
        }
        String dest = String.format(Locale.US, "%-9s", call);
        payload = ":" + dest + ":" + safe(text, 67) + ((id == null || id.isEmpty()) ? "" : "{" + safe(id, 5));
        return sendLine(this.loginCall + ">APTR04,TCPIP*:" + payload);
    }

    String sendReliableMessage(String destination, String text, DeliveryCallback callback) {
        if (!this.verified || destination == null || destination.trim().isEmpty() || text == null || text.trim().isEmpty()) {
            return null;
        }
        String id = String.format(Locale.US, "%03d", Integer.valueOf(Math.floorMod(this.messageSequence.incrementAndGet(), 1000)));
        PendingMessage message = new PendingMessage(destination.trim().toUpperCase(Locale.ROOT), safe(text.trim(), 67), id, callback == null ? new DeliveryCallback() { // from class: tr.aprs.app.AprsIsClient$$ExternalSyntheticLambda2
            @Override // tr.aprs.app.AprsIsClient.DeliveryCallback
            public final void onStatus(String str, String str2, int r3) {
                AprsIsClient.lambda$sendReliableMessage$1(str, str2, r3);
            }
        } : callback);
        this.pending.put(id, message);
        scheduleAttempt(message, 0);
        return id;
    }

    static /* synthetic */ void lambda$sendReliableMessage$1(String a, String b, int c) {
    }

    private void scheduleAttempt(final PendingMessage message, final int index) {
        if (index >= RETRY_SECONDS.length) {
            this.scheduler.schedule(new Runnable() { // from class: tr.aprs.app.AprsIsClient$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AprsIsClient.this.lambda$scheduleAttempt$2(message);
                }
            }, RETRY_SECONDS[RETRY_SECONDS.length - 1], TimeUnit.SECONDS);
        } else {
            this.scheduler.schedule(new Runnable() { // from class: tr.aprs.app.AprsIsClient$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AprsIsClient.this.lambda$scheduleAttempt$3(message, index);
                }
            }, RETRY_SECONDS[index], TimeUnit.SECONDS);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleAttempt$2(PendingMessage message) {
        if (this.pending.remove(message.f14id) != null) {
            message.callback.onStatus(message.f14id, "TESLİM EDİLEMEDİ", message.attempt);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleAttempt$3(PendingMessage message, int index) {
        if (this.pending.containsKey(message.f14id)) {
            message.attempt = index + 1;
            boolean sent = sendMessage(message.destination, message.text, message.f14id);
            message.callback.onStatus(message.f14id, sent ? "GÖNDERİLDİ • ACK BEKLENİYOR" : "GÖNDERİLEMEDİ", message.attempt);
            scheduleAttempt(message, index + 1);
        }
    }

    private boolean sendLine(String line) {
        if (!this.verified || this.writer == null) {
            return false;
        }
        try {
            this.writer.write(line + "\r\n");
            this.writer.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String safe(String value, int max) {
        String clean = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: run, reason: merged with bridge method [inline-methods] */
    public void lambda$connect$0(String callsign, String passcode, double latitude, double longitude, Listener listener) {
        String line;
        try {
                    this.loginCall = callsign.toUpperCase(Locale.ROOT);
                    listener.onState("BAĞLANIYOR");
                    this.socket = new Socket();
                    this.socket.setTcpNoDelay(true);
                    this.socket.connect(new InetSocketAddress("rotate.aprs2.net", 14580), 8000);
                    this.socket.setSoTimeout(45000);
                    this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.US_ASCII));
                    String filter = String.format(Locale.US, "filter r/%.4f/%.4f/100", Double.valueOf(latitude), Double.valueOf(longitude));
                    String pass = (passcode == null || passcode.trim().isEmpty()) ? "-1" : passcode.trim();
                    this.writer.write("user " + this.loginCall + " pass " + pass + " vers TR-APRS 0.15.2 " + filter + "\r\n");
                    this.writer.flush();
                    listener.onState("CANLI RX");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.US_ASCII));
                    AprsPacketParser parser = new AprsPacketParser();
                    while (this.running.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith("#")) {
                            String lower = line.toLowerCase(Locale.ROOT);
                            if (lower.contains("logresp")) {
                                this.verified = lower.contains(" verified");
                                listener.onState(this.verified ? "DOĞRULANDI TX/RX" : "SADECE RX");
                            }
                        } else {
                            AprsMessage message = parseMessage(line);
                            if (message != null) {
                                handleMessage(message, listener);
                                listener.onMessage(message);
                            }
                            AprsPacketParser.Packet packet = parser.parse(line);
                            if (packet != null) {
                                listener.onPacket(packet);
                            }
                        }
                    }
        } catch (Exception error) {
            if (this.running.get()) {
                listener.onError(error.getMessage() == null ? "APRS-IS bağlantısı kesildi" : error.getMessage());
            }
        } finally {
            this.running.set(false);
            this.verified = false;
            this.writer = null;
            listener.onState("BAĞLI DEĞİL");
        }
    }

    private void handleMessage(AprsMessage message, Listener listener) {
        if (sameStation(message.destination, this.loginCall)) {
            if (message.acknowledgement || message.rejection) {
                PendingMessage sent = this.pending.remove(message.f13id);
                if (sent != null) {
                    sent.callback.onStatus(sent.f14id, message.rejection ? "REDDEDİLDİ (REJ)" : "TESLİM EDİLDİ (ACK)", sent.attempt);
                    return;
                }
                return;
            }
            if (!message.f13id.isEmpty()) {
                sendMessage(message.source, "ack" + message.f13id, null);
            }
        }
    }

    private static AprsMessage parseMessage(String line) {
        String text;
        int idAt;
        int sourceEnd = line.indexOf(62);
        int payloadAt = line.indexOf(58);
        if (sourceEnd >= 1 && payloadAt >= sourceEnd && payloadAt + 11 < line.length()) {
            String body = line.substring(payloadAt + 1);
            if (body.length() < 11 || body.charAt(0) != ':' || body.charAt(10) != ':') {
                return null;
            }
            String source = line.substring(0, sourceEnd).trim().toUpperCase(Locale.ROOT);
            String destination = body.substring(1, 10).trim().toUpperCase(Locale.ROOT);
            String content = body.substring(11);
            boolean ack = content.startsWith("ack") && content.length() > 3;
            boolean rej = content.startsWith("rej") && content.length() > 3;
            String id = (ack || rej) ? content.substring(3).trim() : "";
            if (!ack && !rej && (idAt = content.lastIndexOf(123)) >= 0 && idAt + 1 < content.length()) {
                id = content.substring(idAt + 1).trim();
                String text2 = content.substring(0, idAt);
                text = text2;
            } else {
                text = content;
            }
            if (id.length() > 5) {
                id = id.substring(0, 5);
            }
            return new AprsMessage(source, destination, text, id, ack, rej);
        }
        return null;
    }

    private static boolean sameStation(String a, String b) {
        return (a == null || b == null || !a.equalsIgnoreCase(b)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class PendingMessage {
        volatile int attempt;
        final DeliveryCallback callback;
        final String destination;

        /* JADX INFO: renamed from: id */
        final String f14id;
        final String text;

        PendingMessage(String destination, String text, String id, DeliveryCallback callback) {
            this.destination = destination;
            this.text = text;
            this.f14id = id;
            this.callback = callback;
        }
    }
}
