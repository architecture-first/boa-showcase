# M05 - Order Supplies

| Name | Description|
| -----| -----------|
|ID | M05|
|Name| Order Supplies|
|Actors| Merchant|
|Preconditions| There is a need to order supplies.|
|Triggers| The merchant has reviewed inventory via ([M04 - Review Inventory](M04-Review-Inventory.md)) and determined a need to order supplies.|
|Main Flow| 1. The merchant uses the results of analysis and orders more supplies. |
|Alternate Flows| At step 1, a desired product is not available to ship in the desired time.<br/>1.1. The product is marked as out of stock with a reason.<br/>1.2 The use case continues.|
|Exceptions| At step 1, a desired product is permanently unavailable.<br/>1.1 The item is deducted from the order<br/>1.2 The customer is reimbursed<br/>1.3 The item is marked out of stock with a no reorder note.<br/>1.4 The use case continues.|
|Postconditions| The merchant has ordered the desired supplies.|
