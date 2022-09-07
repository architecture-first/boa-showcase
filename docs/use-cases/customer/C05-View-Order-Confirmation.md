# C05 - View Order Confirmation

| Name | Description|
| -----| -----------|
|ID | C05|
|Name| View Order Confirmation|
|Actors| Customer, Cashier, Merchant|
|Preconditions| The order has been processed.|
|Triggers| The cashier has presented the order confirmation via ([S03 - Present Order Confirmation]()).|
|Main Flow| 1. The customer views the order confirmation.<br/>|
|Alternate Flows| At step 1, the customer declines to view the order confirmation.<br/>1.1 The Use Case ends.|
|Exceptions| At step 1, the customer notices an error in the order details.<br/>1.1 The customer registers a complaint with the merchant.<br/>1.2 The user case ends.|
|Postconditions| The customer has seen the order confirmation.|
