# C09 - Receive Bonus Points

| Name | Description|
| -----| -----------|
|ID | C09|
|Name| Receive Bonus Points|
|Actors| Customer, Merchant|
|Preconditions| The customer is registered|
|Triggers| The customer has purchased items via ([C07 - Checkout as Registered Customer](C07-Checkout-As-Registered-Customer.md)).|
|Main Flow| 1. The merchant analyzes the purchase and applies bonus points based on criteria.<br/>|
|Alternate Flows| At step 1, the merchant determines that no bonus points apply.<br/>1.1 The Use Case ends.|
|Exceptions| |
|Postconditions| The customer has earned bonus points.|
