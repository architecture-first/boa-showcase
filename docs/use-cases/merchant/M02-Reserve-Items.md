# M02 - Reserve Items

| Name | Description|
| -----| -----------|
|ID | M02|
|Name| Reserve Items|
|Actors| Merchant|
|Preconditions| An order has been placed.|
|Triggers| The customer has place an order via ([C01 - Checkout](../customer/C03-Checkout.md)).|
|Main Flow| 1. The merchant reserves the desired quantity from inventory for each product in the order.<br/>|
|Alternate Flows| At step 1, the merchant is unable to reserve one or more products.<br/>1.1 The merchant notifies the customer.<br/>1.2 The merchant reserves the items via ([M03 - Reserve on Back Order](M03-Reserve-On-Backorder.md))<br/>1.3 The Use Case ends.|
|Exceptions| |
|Postconditions| The merchant has reserved the items and has reflected it in the inventory.|
