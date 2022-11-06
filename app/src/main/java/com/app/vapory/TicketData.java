package com.app.vapory;

public class TicketData {

    private String userID;
    private String purchaseToken;
    private String sku;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
