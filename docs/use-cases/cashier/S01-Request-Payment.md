# S01 - Request Payment

| Name | Description|
| -----| -----------|
|ID | S01|
|Name| Request Payment|
|Actors| Cashier, Customer|
|Preconditions| There are items in the cart for purchasing.|
|Triggers| The customer has attempted to checkout via ([C03 - Checkout](../customer/C03-Checkout.md)).|
|Main Flow| 1. The cashier asks the customer for payment.<br/>|
|Alternate Flows| At step 1, a registered customer has pre-existing credit that is applied to the purchase.<br/>1.1 The use case ends.|
|Exceptions| |
|Postconditions| The cashier has requested payment.|
