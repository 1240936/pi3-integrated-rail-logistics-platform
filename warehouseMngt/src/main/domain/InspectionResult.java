package main.domain;

/**

Represents the result of inspecting a returned item.
Contains information about what action was taken and quantities involved.**/
public class InspectionResult {
    private final String returnId;
    private final String sku;
    private final InspectionAction action;
    private final int totalQuantity;
    private final int quantityRestocked;
    private final int quantityDiscarded;

    /**

    Creates an inspection result.
    @param returnId the return ID that was inspected
    @param sku the SKU that was inspected
    @param action the action taken (Restocked or Discarded)
    @param totalQuantity total quantity in the return
    @param quantityRestocked quantity that was restocked (0 if discarded)
    @param quantityDiscarded quantity that was discarded (0 if fully restocked)**/
    public InspectionResult(String returnId, String sku, InspectionAction action,
                            int totalQuantity, int quantityRestocked, int quantityDiscarded) {
        this.returnId = returnId;
        this.sku = sku;
        this.action = action;
        this.totalQuantity = totalQuantity;
        this.quantityRestocked = quantityRestocked;
        this.quantityDiscarded = quantityDiscarded;}

    public String getReturnId() {
        return returnId;
    }

    public String getSku() {
        return sku;
    }

    public InspectionAction getAction() {
        return action;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getQuantityRestocked() {
        return quantityRestocked;
    }

    public int getQuantityDiscarded() {
        return quantityDiscarded;
    }

    /**

     Returns true if this was a partial restock (some items restocked, some discarded).*/
    public boolean isPartialRestock() {
        return quantityRestocked > 0 && quantityDiscarded > 0;}
}