# M01 - Show Products

| Name | Description|
| -----| -----------|
|ID | M01|
|Name| Show Products|
|Actors| Merchant, Customer|
|Preconditions| |
|Triggers| The customer has attempted to view products via ([C01 - View-Products](../customer/C01-View-Products.md)).|
|Main Flow| 1. The merchant finds products from inventory based on criteria.<br/> 2. The merchant displays the corresponding products.|
|Alternate Flows| At step 1, the merchant cannot find available products.<br/>2.1 The merchant notifies the customer.<br/>2.2 The use case ends.|
|Exceptions| At step 2, due to technical difficulties, the merchant cannot display the products.<br/>2.1 The merchant notifies the customer.<br/>2.2 The user case ends.|
|Postconditions| The merchant has displayed the desired products.|
