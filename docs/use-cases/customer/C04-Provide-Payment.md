# C04 - Provide Payment

| Name | Description|
| -----| -----------|
|ID | C04|
|Name| Provide Payment|
|Actors| Customer, Cashier|
|Preconditions| |
|Triggers| The cashier requests payment via ([S01 - Request Payment]()).|
|Main Flow| 1. The customer views the payment request.<br/> 2. The customer provides payment.|
|Alternate Flows| At step 2, the customer chooses an alternative form of payment.<br/>2.2 The Use Case ends.|
|Exceptions| At step 2, the customer provides payment that is not accepted.<br/>2.1 The cashier notifies the customer.<br/>2.2 The user case ends.|
|Postconditions| The customer has provided payment to the cashier.|
