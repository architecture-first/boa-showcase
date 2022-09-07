# C01 - View Products

| Name | Description|
| -----| -----------|
|ID | C01|
|Name| View Products|
|Actors| Customer, Merchant|
|Preconditions| |
|Triggers| The customer has navigated to the product area.|
|Main Flow| 1. The customer requests to see the products based on the desired criteria.<br/> 2. The merchant displays the corresponding products via ([M01 - Show Products](../merchant/M01-Show-Products.md))|
|Alternate Flows| At step 2, the merchant cannot find available products.<br/>2.1 The merchant notifies the customer.<br/>2.2 The Use Case ends.|
|Exceptions| At step 2, due to technical difficulties, the merchant cannot display the products.<br/>2.1 The merchant notifies the customer.<br/>2.2 The user case ends.|
|Postconditions| The customer has seen the desired products.|
