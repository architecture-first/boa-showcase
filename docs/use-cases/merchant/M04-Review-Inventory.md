# M04 - Review Inventory

| Name | Description|
| -----| -----------|
|ID | M04|
|Name| Review Inventory|
|Actors| Merchant|
|Preconditions| |
|Triggers| The scheduled time has occurred to review inventory.|
|Main Flow| 1. For each category, the merchant records the number of items in inventory minus the reserved items.<br/>2. The merchant analyzes and determines the rate of sales per items<br/>3. The merchant combines the results and number of items on backorder.<br/>4. The merchant orders supplies via ([M05 - Order Supplies](M05-Order-Supplies.md)). |
|Alternate Flows| At step 3, the merchant determines that there is no need to order supplies.<br/>1.1 The use case ends.|
|Exceptions| |
|Postconditions| The merchant has reviewed the inventory and has ordered supplies.|
