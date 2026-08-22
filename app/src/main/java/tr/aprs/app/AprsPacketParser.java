package tr.aprs.app;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes3.dex */
final class AprsPacketParser {
    private static final Pattern POSITION = Pattern.compile("([0-8][0-9])([0-5][0-9]\\.[0-9]{2})([NS]).([0-1][0-9]{2})([0-5][0-9]\\.[0-9]{2})([EW])");

    AprsPacketParser() {
    }

    static final class Packet {
        final double latitude;
        final double longitude;
        final String raw;
        final String source;

        Packet(String source, double latitude, double longitude, String raw) {
            this.source = source;
            this.latitude = latitude;
            this.longitude = longitude;
            this.raw = raw;
        }
    }

    Packet parse(String line) {
        int separator = line.indexOf(62);
        int payload = line.indexOf(58);
        if (separator >= 1 && payload >= separator) {
            String source = line.substring(0, separator).trim().toUpperCase(Locale.ROOT);
            String body = line.substring(payload + 1);
            Matcher matcher = POSITION.matcher(body);
            if (!matcher.find()) {
                return null;
            }
            double lat = ((double) Integer.parseInt(matcher.group(1))) + (Double.parseDouble(matcher.group(2)) / 60.0d);
            double lng = ((double) Integer.parseInt(matcher.group(4))) + (Double.parseDouble(matcher.group(5)) / 60.0d);
            if ("S".equals(matcher.group(3))) {
                lat = -lat;
            }
            double lat2 = lat;
            if ("W".equals(matcher.group(6))) {
                lng = -lng;
            }
            return new Packet(source, lat2, lng, line);
        }
        return null;
    }
}
