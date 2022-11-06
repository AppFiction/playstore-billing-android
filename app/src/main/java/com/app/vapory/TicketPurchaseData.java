package com.app.vapory;

/**
 * A class than encapsulated data about ticket that was purchaed concerning and event/ raffle
 */
public class TicketPurchaseData {

    private String userID;
    private String eventID;
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

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }
}
