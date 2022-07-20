# C10 - Checkout as Guest

| Name | Description|
| -----| -----------|
|ID | C10|
|Name| Checkout as Guest|
|Notes| extends C03 - Checkout at step 8|
|Actors| Customer, Merchant, Cashier|
|Preconditions| The customer is not registered. |
|Triggers| The customer has navigated to the checkout area.|
|Main Flow| 8. The cashier uses the temporary identifying information and presents the order confirmation via ([S03 - Present Order Confirmation](../cashier/S03-Present-Order-Confirmation.md)).<br/>9. The customer views the order confirmation via ([C05 - View Order Confirmation](C05-View-Order-Confirmation.md)). |
|Alternate Flows| |
|Exceptions| |
|Postconditions| The customer has purchased the items.|
