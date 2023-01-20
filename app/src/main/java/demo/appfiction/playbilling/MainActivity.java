package demo.appfiction.playbilling;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

import demo.appfiction.playbilling.databinding.ActivityMainBinding;


/**
 * Demonstrate one-time purchase/non-consumable and consumable types of purchases
 * Docs: https://developer.android.com/google/play/billing/integrate
 */

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private ActivityMainBinding binding;
    private BillingClient billingClient;


    /**
     * ID of user who is logged into the app/. For demo purpose we make user constant. Change to your login.
     */
    private String USER_ID = "id_of_the_user_123";

//    IDs of the two products we use in this demo. You can get this from play console
    private final String PRODUCT_1 = "prod1";
    private final String PRODUCT_2 = "prod2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //Connect to Google Play billing at start of activity
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        //Connect to google play
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryPurchases();
                    Toast.makeText(MainActivity.this, "Billing successfully connected", Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Toast.makeText(MainActivity.this, "Billing disconnected", Toast.LENGTH_LONG).show();

            }
        });

        //Button Clicks Listeners
        binding.nonConsumable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buyProduct(PRODUCT_1);
            }
        });

        binding.consumable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buyProduct(PRODUCT_2);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Call this here to update app when billing status changes
        queryPurchases();
    }

    @Override
    protected void onDestroy() {
        billingClient.endConnection();
        super.onDestroy();
    }

    /**
     * Query in-app details from playstore using product_id. You will then use the received details about the product
     * to launch billing flow to buy product
     *
     * @param productID id of product
     */
    private void buyProduct(String productID) {
        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(productID)
                                                .setProductType(BillingClient.ProductType.INAPP)
                                                .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                (billingResult, productDetailsList) -> {
                    if (productDetailsList == null) {
                        Toast.makeText(MainActivity.this, "Account unable to access products. Check if your gmail account is a license tester on Google Play Console", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Process the result.
                    launchBillingFlow(productDetailsList.get(0));
                }
        );
    }


    /**
     * Save ProductIDs and purchase tokens to your system data.In this case SharedPreference
     */
    private void recordPurchase(String userID, Purchase purchase) {
        //Your code here

    }


    /**
     * Check existing purchases that user has previously made.
     */
    private void queryPurchases() {
        if (billingClient.isReady()) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    (billingResult, purchases) -> {
                        // check billingResult
                        // process returned purchase list, e.g. display the plans user owns
                        for (Purchase p : purchases) {
                            handlePurchase(p);
                        }
                    }
            );
        }
    }

    /**
     * Handle purchase to make consumable or non-consumable
     *
     * @param purchase
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            //Save transaction on system
            recordPurchase(USER_ID, purchase);
            if (!purchase.isAcknowledged()) {
                //Acknowledge purchase to prevent refund to buyer using acknowledge params or consume params
                if (purchase.getProducts().get(0).equals(PRODUCT_1)) {
//                    Non-consumable
                    //Run acknowledge api so that user will not need to buy again

                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);


                } else if (purchase.getProducts().get(0).equals(PRODUCT_2)) {
//                    Consumable
                    //Run consume api so that user can buy again
                    ConsumeParams consumeParams =
                            ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            // Handle the success of the consume operation.
                        }
                    };

                    billingClient.consumeAsync(consumeParams, listener);

                }
            }
        }
    }

    /**
     * Callback to deliver result of the purchase operation
     *
     * @param billingResult
     * @param purchases
     */
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    @Override
    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

    }

    /**
     * Launch playstore payment screen to buy using product details you got previously
     *
     * @param productDetails
     */
    private void launchBillingFlow(ProductDetails productDetails) {
        ImmutableList productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                .setProductDetails(productDetails)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        // Launch the billing flow
        BillingResult billingResult = billingClient.launchBillingFlow(this, billingFlowParams);
    }
}
