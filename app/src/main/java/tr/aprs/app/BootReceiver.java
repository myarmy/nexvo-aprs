package tr.aprs.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getBoolean("background_aprs", false)) return;
        Intent service = new Intent(context, AprsBackgroundService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service); else context.startService(service);
    }
}
