package co.il.codeline.trip2go;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

/**
 * A class to implement shared preference
 *
 * Created by emmanuel.tagoe on 18/06/2016. Updated 2021
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
     * @param removeAdsData
     */
    public void setRemoveAdsData(RemoveAdsData removeAdsData) {
        prefs.edit().putString(KEY_REMOVE_ADS_DATA, gson.toJson(removeAdsData)).apply();
    }

    /**
     * System data for user's subscription.
     * @return
     */
    public RemoveAdsData getRemoveAdsData() {
        String json = prefs.getString(KEY_REMOVE_ADS_DATA, null);
        return gson.fromJson(json, RemoveAdsData.class);
    }

    public void setSubscriptionData(SubscriptionData subscriptionData) {
        prefs.edit().putString(KEY_SUBS_DATA, gson.toJson(subscriptionData)).apply();
    }

    public SubscriptionData getSubscriptionData() {
        String json = prefs.getString(KEY_SUBS_DATA, null);
        return gson.fromJson(json, SubscriptionData.class);
    }

}
