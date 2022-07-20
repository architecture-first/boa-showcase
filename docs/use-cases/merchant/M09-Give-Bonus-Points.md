# M09 - Give Bonus Points

| Name | Description|
| -----| -----------|
|ID | M09|
|Name| Give Bonus Points|
|Actors| Merchant, Customer|
|Preconditions| The customer is registered|
|Triggers| The customer has checked out via ([C07 - Checkout as Registered Customer](../customer/C07-Checkout-As-Registered-Customer.md)).|
|Main Flow| 1. The merchant calculates the bonus points based on the order size.<br/> 2. The merchant notifies the customer.|
|Alternate Flows| At step 1, the customer has purchased items below the threshold to receive bonus points.<br/>2.1 The use case ends.|
|Exceptions| |
|Postconditions| The merchant has given the customer bonus points.|
