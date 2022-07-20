# C07 - Checkout as Registered Customer

| Name | Description|
| -----| -----------|
|ID | C07|
|Name| Checkout as Registered Customer|
|Notes| extends C03 - Checkout at step 3|
|Actors| Customer,Merchant,Cashier|
|Preconditions| The customer is registered. |
|Triggers| The customer has navigated to the checkout area.|
|Main Flow| 3. The cashier uses the customer's stored payment information.<br/>4. The customer provides payment via ([C04 - Provide Payment](C04-Provide-Payment.md)).<br/>5. The cashier processes the payment via ([S02 - Process Payment](../cashier/S02-Process-Payment.md)).<br/>6. The cashier stores the order history via ([S04 - Store Order History](../cashier/S04-Store-Order-History.md))<br/>7. The merchant determines and applies bonus points via ([C09 - Receive Bonus Points](C09-Receive-Bonus-Points.md))<br/>8. The cashier presents the order confirmation via ([S03 - Present Order Confirmation](../cashier/S03-Present-Order-Confirmation.md)).<br/>9. The customer views the order confirmation via ([C05 - View Order Confirmation](C05-View-Order-Confirmation.md)). |
|Alternate Flows| At step 3, the customer desires to use different payment information.<br/>3.1 The customer provides the payment information. 3.2 The use case continues.|
|Exceptions| At step 3, the cashier informs the customer that the default payment information is no longer viable.<br/>3.1 The user case ends.|
|Postconditions| The customer has purchased the items.|
