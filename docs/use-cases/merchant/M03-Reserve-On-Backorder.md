# M03 - Reserve on Back Order

| Name | Description|
| -----| -----------|
|ID | M03|
|Name| Reserve on Back Order|
|Actors| Merchant|
|Preconditions| An order has been placed and there is not enough inventory to support the order.|
|Triggers| There is not enough inventory to support order via ([M02 - Reserve Items](M02-Reserve-Items.md)).|
|Main Flow| 1. The merchant reserves as much of the desired quantity from inventory as possible for a product.<br/>2. The merchant creates a backorder entry for later shipment.|
|Alternate Flows| |
|Exceptions| |
|Postconditions| The merchant has reserved the item on backorder.|
