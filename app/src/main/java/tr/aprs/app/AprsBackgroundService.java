package tr.aprs.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AprsBackgroundService extends Service implements LocationListener, AprsIsClient.Listener {
    private static final String CHANNEL_STATUS = "aprs_status";
    private static final String CHANNEL_MESSAGES = "aprs_messages";
    private static final int STATUS_ID = 4101;
    private final AprsIsClient client = new AprsIsClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private LocationManager locationManager;
    private FeatureStore store;
    private PowerManager.WakeLock wakeLock;
    private Location lastLocation;
    private long lastBeaconAt;
    private float lastBeaconBearing;
    private volatile boolean reconnectPending;
    private String callsign = "";
    private String passcode = "";
    private String talkGroup = "28642";
    private char symbol = '>';
    private String beaconProfile = "ARAC";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        startForeground(STATUS_ID, statusNotification("Başlatılıyor"));
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        store = new FeatureStore(this);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tr.aprs.app:background");
        wakeLock.acquire(10 * 60_000L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        loadSettings();
        if (callsign.isEmpty()) {
            updateStatus("Çağrı işareti eksik");
            stopSelf();
            return START_NOT_STICKY;
        }
        requestLocationUpdates();
        connect();
        return START_STICKY;
    }

    private void loadSettings() {
        SharedPreferences preferences = getSharedPreferences("MainActivity", MODE_PRIVATE);
        callsign = preferences.getString("callsign", "").trim().toUpperCase();
        talkGroup = preferences.getString("talkgroup", "28642");
        String configuredSymbol = preferences.getString("aprs_symbol", ">");
        symbol = configuredSymbol.isEmpty() ? '>' : configuredSymbol.charAt(0);
        passcode = new SecureStore(this).readAprsPasscode();
        beaconProfile = preferences.getString("beacon_profile", "ARAC");
    }

    private void connect() {
        double latitude = lastLocation == null ? 41.0082 : lastLocation.getLatitude();
        double longitude = lastLocation == null ? 28.9784 : lastLocation.getLongitude();
        client.connect(callsign, passcode, latitude, longitude, this);
        store.log("INFO", "APRS_CONNECT", callsign);
    }

    private void requestLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            updateStatus("Konum izni gerekli");
            return;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15_000L, 10f, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60_000L, 50f, this);
        } catch (RuntimeException ignored) {
            updateStatus("Konum servisi kullanılamıyor");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!isLocationReliable(location)) return;
        lastLocation = location;
        store.track(location.getLatitude(), location.getLongitude(), location.hasSpeed() ? location.getSpeed() * 3.6 : 0, location.hasBearing() ? location.getBearing() : 0);
        if (!client.isVerified() || !shouldBeacon(location)) return;
        double speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
        double altitude = location.hasAltitude() ? location.getAltitude() : 0;
        double course = location.hasBearing() ? location.getBearing() : 0;
        if (client.sendBeacon(location.getLatitude(), location.getLongitude(), altitude, speedKmh, course,
                symbol, "TR APRS • TG " + talkGroup)) {
            lastBeaconAt = System.currentTimeMillis();
            lastBeaconBearing = location.hasBearing() ? location.getBearing() : lastBeaconBearing;
            updateStatus("Beacon gönderildi • " + Math.round(speedKmh) + " km/sa");
            store.log("INFO", "BEACON", location.getLatitude() + "," + location.getLongitude());
            getSharedPreferences("runtime_status", MODE_PRIVATE).edit().putLong("last_beacon", lastBeaconAt).putString("last_error", "").apply();
        }
    }

    private boolean isLocationReliable(Location location) {
        if (location == null) return false;
        if (location.hasAccuracy() && location.getAccuracy() > 100f) {
            store.log("WARN", "GPS_REJECT", "Düşük doğruluk: " + Math.round(location.getAccuracy()) + " m");
            return false;
        }
        if (lastLocation != null) {
            long dtMs = location.getTime() - lastLocation.getTime();
            if (dtMs > 0 && dtMs < 120_000L) {
                double calculatedKmh = lastLocation.distanceTo(location) / (dtMs / 1000.0) * 3.6;
                double measuredKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
                if (calculatedKmh > 250 && measuredKmh < 200) {
                    store.log("WARN", "GPS_REJECT", "Ani sıçrama: " + Math.round(calculatedKmh) + " km/sa");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean shouldBeacon(Location location) {
        long elapsed = System.currentTimeMillis() - lastBeaconAt;
        double speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
        long interval;
        if ("YAYA".equals(beaconProfile)) interval = speedKmh < 1 ? 10 * 60_000L : 5 * 60_000L;
        else if ("SABIT".equals(beaconProfile)) interval = 30 * 60_000L;
        else if ("BISIKLET".equals(beaconProfile)) interval = speedKmh < 2 ? 12 * 60_000L : 4 * 60_000L;
        else interval = speedKmh < 2 ? 15 * 60_000L : speedKmh < 25 ? 5 * 60_000L : 2 * 60_000L;
        BatteryManager battery = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int percent = battery == null ? 100 : battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (percent > 0 && percent <= 20) interval *= 2;
        float turn = location.hasBearing() ? Math.abs(location.getBearing() - lastBeaconBearing) : 0;
        turn = Math.min(turn, 360 - turn);
        return lastBeaconAt == 0 || elapsed >= interval || (speedKmh >= 10 && turn >= 30 && elapsed >= 30_000L);
    }

    @Override public void onState(String state) { updateStatus(state); store.log("INFO", "STATE", state); getSharedPreferences("runtime_status", MODE_PRIVATE).edit().putString("state", state).putLong("state_time", System.currentTimeMillis()).apply(); }
    @Override public void onPacket(AprsPacketParser.Packet packet) {
        store.log("RX", "PACKET", packet.source + " " + packet.latitude + "," + packet.longitude);
        if (store.isFavorite(packet.source)) store.log("INFO", "FAVORITE_SEEN", packet.source);
    }

    @Override
    public void onMessage(AprsIsClient.AprsMessage message) {
        if (!message.acknowledgement && !message.rejection && message.destination.equalsIgnoreCase(callsign)) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify((int) (System.currentTimeMillis() & 0x7fffffff),
                    messageNotification(message.source, message.text));
            store.message(message.source, "GELEN", message.text, "ALINDI");
        }
    }

    @Override
    public void onError(String error) {
        updateStatus("Bağlantı kesildi • yeniden denenecek");
        store.log("ERROR", "CONNECTION", error == null ? "bilinmeyen" : error);
        getSharedPreferences("runtime_status", MODE_PRIVATE).edit().putString("last_error", error == null ? "bilinmeyen" : error).putLong("error_time", System.currentTimeMillis()).apply();
        if (reconnectPending) return;
        reconnectPending = true;
        scheduler.schedule(() -> {
            reconnectPending = false;
            if (!scheduler.isShutdown()) connect();
        }, 30, TimeUnit.SECONDS);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel status = new NotificationChannel(CHANNEL_STATUS, "APRS arka plan bağlantısı", NotificationManager.IMPORTANCE_LOW);
        NotificationChannel messages = new NotificationChannel(CHANNEL_MESSAGES, "APRS mesajları", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(status);
        manager.createNotificationChannel(messages);
    }

    private PendingIntent openAppIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification statusNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_STATUS) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_launcher).setContentTitle("TR APRS arka planda aktif")
                .setContentText(text).setContentIntent(openAppIntent()).setOngoing(true).build();
    }

    private Notification messageNotification(String source, String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_MESSAGES) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_launcher).setContentTitle("APRS mesajı • " + source)
                .setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(openAppIntent()).setAutoCancel(true).build();
    }

    private void updateStatus(String text) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(STATUS_ID, statusNotification(text));
    }

    @Override
    public void onDestroy() {
        client.disconnect();
        if (locationManager != null) locationManager.removeUpdates(this);
        scheduler.shutdownNow();
        if (store != null) store.close();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { updateStatus("GPS kapalı"); }
}
