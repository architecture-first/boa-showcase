# M08 - Show Product

| Name | Description|
| -----| -----------|
|ID | M08|
|Name| Show Product|
|Actors| Merchant, Customer|
|Preconditions| |
|Triggers| The customer has attempted to view a product via ([C11 - View-Product](../customer/C11-View-Product.md)).|
|Main Flow| 1. The merchant finds the product from inventory based on criteria.<br/> 2. The merchant displays the corresponding product.|
|Alternate Flows| At step 1, the merchant cannot find the desired product.<br/>2.1 The merchant notifies the customer.<br/>2.2 The Use Case ends.|
|Exceptions| |
|Postconditions| The merchant has displayed the desired product.|
