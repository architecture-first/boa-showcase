# M06 - Suggest Products

| Name | Description|
| -----| -----------|
|ID | M06|
|Name| Suggest Products|
|Actors| Merchant|
|Preconditions| The customer is registered.|
|Triggers| A registered customer is looking at a product via ([C11 - View Product](../customer/C11-View-Product.md)).|
|Main Flow| 1. The merchant analyzes customers' order history patterns.<br/>2. The merchant shows recommended products based on the analysis. |
|Alternate Flows| At step 1, the merchant realizes that the customer is already ordering the suggested product.<br/>1.1 The use case ends.|
|Exceptions| |
|Postconditions| The merchant has suggested products.|
