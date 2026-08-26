package tr.aprs.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/* JADX INFO: loaded from: classes3.dex */
final class SecureStore {
    private static final String ALIAS = "tr_aprs_api_key";

    /* JADX INFO: renamed from: IV */
    private static final String f16IV = "api_key_iv";
    private static final String PREF = "secure_settings";
    private static final String VALUE = "api_key_value";
    private final SharedPreferences preferences;
    private final Context context;

    SecureStore(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF, 0);
    }

    void saveApiKey(String apiKey) throws Exception {
        saveSecret(VALUE, f16IV, apiKey);
    }

    void saveAprsPasscode(String passcode) throws Exception {
        saveSecret("aprs_pass_value", "aprs_pass_iv", passcode);
    }

    String readAprsPasscode() {
        return readSecret("aprs_pass_value", "aprs_pass_iv");
    }

    void saveBrandMeisterPassword(String password) throws Exception {
        saveSecret("bm_pass_value", "bm_pass_iv", password);
    }

    String readBrandMeisterPassword() {
        return readSecret("bm_pass_value", "bm_pass_iv");
    }

    void saveMumblePassword(String password) throws Exception {
        saveSecret("mumble_pass_value", "mumble_pass_iv", password);
    }

    String readMumblePassword() {
        return readSecret("mumble_pass_value", "mumble_pass_iv");
    }

    void saveDvSwitchPassword(String password) throws Exception {
        saveSecret("dvswitch_pass_value", "dvswitch_pass_iv", password);
    }

    String readDvSwitchPassword() {
        return readSecret("dvswitch_pass_value", "dvswitch_pass_iv");
    }

    private void saveSecret(String valueKey, String ivKey, String secret) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
        this.preferences.edit().putString(valueKey, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(ivKey, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .remove("fallback_" + valueKey).remove("fallback_" + ivKey).apply();
    }

    String readApiKey() {
        return readSecret(VALUE, f16IV);
    }

    void saveNexvoToken(String token) throws Exception {
        saveSecret("nexvo_token_value", "nexvo_token_iv", token);
    }

    String readNexvoToken() {
        return readSecret("nexvo_token_value", "nexvo_token_iv");
    }

    void saveRepeaterBookToken(String token) throws Exception {
        saveSecret("repeaterbook_token_value", "repeaterbook_token_iv", token);
    }

    String readRepeaterBookToken() {
        return readSecret("repeaterbook_token_value", "repeaterbook_token_iv");
    }

    private String readSecret(String valueKey, String ivKey) {
        try {
            String value = this.preferences.getString(valueKey, "");
            String iv = this.preferences.getString(ivKey, "");
            if (!value.isEmpty() && !iv.isEmpty()) {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(2, getOrCreateKey(), new GCMParameterSpec(128, Base64.decode(iv, 2)));
                return new String(cipher.doFinal(Base64.decode(value, 2)), StandardCharsets.UTF_8);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }


    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, 3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            return generator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(ALIAS, null)).getSecretKey();
    }
}
