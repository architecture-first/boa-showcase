# I01 - Authenticate Customer

| Name | Description|
| -----| -----------|
|ID | I01|
|Name| Authenticate Customer|
|Actors| Customer, Identity Provider|
|Preconditions| The customer is not authenticated|
|Triggers| The customer has navigated to a secured area.|
|Main Flow| 1. The customer attempts to enter a secured area.<br/>2. The identity provider request credentials from the customer.<br/>3. The customer provides the credentials.<br/>4. The identify provider validates the credentials and gives the customer a token for access.|
|Alternate Flows| At step 3, the customer declines to provide credentials.<br/>3.1 The identity provider escorts the customer to the non-secured area.<br/>1.2 The use case ends.|
|Exceptions| At step 4, the identity provider is unable to validate the user credentials.<br/>4.1 The identity provider notifies the customer and escorts the customer to the non-secured area.<br/>4.2 The use case ends.|
|Postconditions| The customer has been authenticated and has received an access token.|