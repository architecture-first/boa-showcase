# C06 - View Suggested Products

| Name | Description|
| -----| -----------|
|ID | C06|
|Name| View Suggested Products|
|Actors| Customer, Merchant|
|Preconditions| The customer is registered|
|Triggers| The customer is viewing a product via ([C11 - View Product](C11-View-Product.md)).|
|Main Flow| 1. The merchant suggests additional products via ([M06 - Suggest Products](../merchant/M06-Suggest-Products.md)).<br/>2. The customer views the suggested products.|
|Alternate Flows| At step 1, there are no related products to show.<br/>1.1 The Use Case ends.|
|Exceptions| |
|Postconditions| The customer has seen the suggested products.|
