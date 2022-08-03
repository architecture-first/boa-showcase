# C03 - Checkout

| Name | Description|
| -----| -----------|
|ID | C03|
|Name| Checkout|
|Actors| Customer, Merchant, Cashier|
|Preconditions| There are items in the cart |
|Triggers| The customer has navigated to the checkout area.|
|Main Flow| 1. The customer presents the cart to the cashier.<br/> 2. The merchant reserves the products via ([M02 - Reserve Items](../merchant/M02-Reserve-Items.md)).<br/>3. The cashier requests payment via ([S01 - Request Payment](../cashier/S01-Request-Payment.md)).<br/>4. The customer provides payment via ([C04 - Provide Payment](C04-Provide-Payment.md)).<br/>5. The cashier processes the payment via ([S02 - Process Payment](../cashier/S02-Process-Payment.md)).<br/>6. The cashier presents the order confirmation via ([S03 - Present Order Confirmation](../cashier/S03-Present-Order-Confirmation.md)).<br/>7. The customer views the order confirmation via ([C05 - View Order Confirmation](C05-View-Order-Confirmation.md)). |
|Alternate Flows| At step 2, the merchant is unable to reserve a product due to lack of inventory.<br/>2.1 The merchant notifies the customer and reserves the item on backorder via ([M03 - Reserve on Back Order](../merchant/M03-Reserve-On-Backorder.md)).<br/>2.2 The Use Case continues.|
|Exceptions| At step 4, the cashier is unable to process payment due to a customer issue.<br/>2.1 The cashier notifies the customer.<br/>2.2 The user case ends.|
|Postconditions| The customer has purchased the items.|
