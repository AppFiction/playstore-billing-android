package demo.appfiction.playbilling;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

/**
 * A class to implement shared preference
 */
public class Cache {

    private final String KEY_REMOVE_ADS_DATA = "key_ads_purchase_data";
    private final String KEY_SUBS_DATA = "key_subscription_purchase_data";
    private static final String APP_CACHE_FILE = "cache_file_1";//file name of app preferences
    private SharedPreferences prefs;
    private Gson gson;


    public Cache(Context context) {
        this.prefs = context.getSharedPreferences(APP_CACHE_FILE, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(APP_CACHE_FILE, Context.MODE_PRIVATE);
    }

    /**
     * System data for user's remove_ads purchase.
     * @param nonConsumableData
     */
    public void setNonConsumableData(NonConsumableData nonConsumableData) {
        prefs.edit().putString(KEY_REMOVE_ADS_DATA, gson.toJson(nonConsumableData)).apply();
    }

    /**
     * System data for user's subscription.
     * @return
     */
    public NonConsumableData getNonConsumableData() {
        String json = prefs.getString(KEY_REMOVE_ADS_DATA, null);
        return gson.fromJson(json, NonConsumableData.class);
    }

    public void setConsumableData(ConsumableData consumableData) {
        prefs.edit().putString(KEY_SUBS_DATA, gson.toJson(consumableData)).apply();
    }

    public ConsumableData getSubscriptionData() {
        String json = prefs.getString(KEY_SUBS_DATA, null);
        return gson.fromJson(json, ConsumableData.class);
    }

}
