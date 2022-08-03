# S02 - Process Payment

| Name | Description|
| -----| -----------|
|ID | S02|
|Name| Process Payment|
|Actors| Cashier, Customer|
|Preconditions| The customer has chosen a payment method.|
|Triggers| The customer has attempted to checkout via ([C03 - Checkout](../customer/C03-Checkout.md)).|
|Main Flow| 1. The cashier processes the payment against to the desired banking provider.<br/>|
|Alternate Flows| At step 1, the banking provider has declined the purchase.<br/>1.1 The cashier notifies the customer.<br/>1.2 The merchant removes the reserve of the items via ([M07 - Remove Reservation](../merchant/M07-Remove-Reservation.md)).<br/>1.3 The Use Case ends.|
|Exceptions| |
|Postconditions| The cashier has processed the payment.|
