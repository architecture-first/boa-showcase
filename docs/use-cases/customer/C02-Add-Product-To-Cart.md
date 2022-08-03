# C02 - Add Product to Cart

| Name | Description|
| -----| -----------|
|ID | C02|
|Name| Add Product to Cart|
|Actors| Customer, Merchant|
|Preconditions| |
|Triggers| The customer has navigated to a specific product.|
|Main Flow| 1. The customer chooses the product with the desired attributes and quantity.<br/> 2. The customer adds the product(s) to the cart.|
|Alternate Flows| At step 2, the customer cannot find a product with the desired attributes.<br/>2.1 The customer stops viewing the product and exits.<br/>2.2 The Use Case ends.|
|Exceptions| At step 2, the customer is notified that the desired product is out of stock.<br/>2.1 The merchant notifies the customer.<br/>2.2 The user case ends.|
|Postconditions| The product(s) has been added to the cart or the customer has exited.|
