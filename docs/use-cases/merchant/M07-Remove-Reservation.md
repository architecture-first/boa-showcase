# M07 - Remove Reservation

| Name | Description|
| -----| -----------|
|ID | M07|
|Name| Remove Reservation|
|Actors| Merchant|
|Preconditions| An order has been placed.|
|Triggers| The customer has unsuccessfully checked out via ([C01 - Checkout](../customer/C03-Checkout.md)).|
|Main Flow| 1. The merchant removes the reserves against inventory for each product in the order.<br/>|
|Alternate Flows| |
|Exceptions| |
|Postconditions| The merchant has removed the reservation.|
