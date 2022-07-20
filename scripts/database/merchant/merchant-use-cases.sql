// Merchant Use Cases

// M01-Show-Products (start)
// Find Active Products with Inventory
db.inventory.find(
    {"isActive": true, "unitsAvailable": {$gt: 0}}
    );
// M01-Show-Products (end)

// M08-Show-Product (start)
// Find Referenced Product (alone)
db.inventory.find(
    {"isActive": true, "productId": 1002},
    {_id: 0, "productId": 1, "name": 1, "type": 1, "imageUrl": 1,
        "attributes": 1, "price": 1 }
    );
// M08-Show-Product (end)

// M04-Review Inventory (start)
// Find items that must be reordered due to lower than threshold
db.orders.aggregate([
    {$match: {"status": "complete"}}
    ,{$unwind: {path: "$items"}}
    ,{$group: {_id: "$items.productId", quantity: {$sum: "$items.quantity"}, count: {$sum: 1}} }
    ,{
        $lookup: {
            from: "inventory",
            localField: "_id",
            foreignField: "productId",
            pipeline: [
                {$unwind: {path: "$price"}}
            ],
            as: "inventoryItems"
        }
    }
    ,{$unwind: {path: "$inventoryItems"}}
    ,{$project: {_id: 0, "productId": "$_id", "inventoryQuantity": "$inventoryItems.unitsAvailable",
            "orderQuantity": "$quantity", "availableQuantity": {$subtract: ["$inventoryItems.unitsAvailable", "$quantity"]}}}
    ,{$match: {"availableQuantity": {$lt: 200}}}
]);

// Find items on backorder
db.orders.aggregate([
    {$match: {"status": "processed"}}
    ,{$unwind: {path: "$items"}}
    ,{
        $lookup: {
            from: "inventory",
            localField: "items.productId",
            foreignField: "productId",
            pipeline: [
                {$unwind: {path: "$price"}}
            ],
            as: "inventoryItems"
        }
    }
    ,{$unwind: {path: "$inventoryItems"}}
    ,{$project: {_id: 0, "orderNumber": 1, "productId": "inventoryItems.productId", "inventoryQuantity": "$inventoryItems.unitsAvailable",
            "orderQuantity": "$items.quantity",
            "availableQuantity": {$subtract: ["$inventoryItems.unitsAvailable", "$items.quantity"]},
    }}
    ,{$project: {"orderNumber": 1, "productId": 1, "inventoryQuantity": 1,"orderQuantity": 1,
            "coveredQuantity": {$subtract: ["$inventoryQuantity", "$availableQuantity"]}
        }}
    ,{$project: {"orderNumber": 1, "productId": 1, "inventoryQuantity": 1,"orderQuantity": 1, "coveredQuantity": 1,
            "backorderedQuantity": {$subtract: ["$orderQuantity", "$coveredQuantity"]}
        }}
    ,{$match: {"backorderedQuantity": {gt: 0}}}
]);
// M04-Review Inventory (end)

// M02 - Reserve Items (start)
// this is part of the normal create order
// M02 - Reserve Items (end)

// M03 - Reserve on Back Order (start)
db.inventory.updateOne(
    {"productId": 1001},
    {$inc: {unitsBackordered: 2}}
    );

// db.inventory.updateOne(
//     {"productId": "F001"},
//     {$rename: {unitsBackorderd: "unitsBackordered"}}
//     );

//db.inventory.find({"productId": "F003"});
// M03 - Reserve on Back Order (end)

// M05 - Order Supplies (start)
// before order
db.inventory.updateOne(
    {"productId": 1001},
    {$inc: {unitsOnOrder: 25}}
    );

// after delivery
db.inventory.updateOne(
    {"productId": 1001},
    {
        $inc: {unitsAvailable: 25},
        $set: {unitsOnOrder: 0, updatedBy: "system", updateDate: ISODate()},
        $push: {
            supplyOrderHistory: {
                quantity: 25,
                deliveryDate: ISODate()
            }
        }
    }
    );

db.inventory.find();
db.inventory.find({"productId": 1004});
// M05 - Order Supplies (end)

// M06 - Suggest Products (start)
// Find the customer's orders
db.orders.aggregate([
    {$match: {"userId": 1001}}
    ,{$unwind: {path: "$items"}}
    ,{$sort: {datePurchased: -1}}
    ,{$limit: 1}
       ,{
          $lookup: {
              from: "orders",
              let: {purchasedProductId: "$productId"},
              pipeline: [
                  {$limit: 10},
                  // -- insert analytical logic
              ],
              as: "suggestedItems"
          }
      }
    ,{$unwind: {path: "$suggestedItems"}}
    ,{$unwind: {path: "$suggestedItems.items"}}
    ,{$project: {_id: 0, "suggestedItem": "$suggestedItems.items"}}
    ,{$match: {"suggestedItem.productId": {$ne: "F001" }}}
    ,{$project: {_id: 0, "suggestedItem.quantity": 0}}
    ,{$project: {"productId": "$suggestedItem.productId", "attributes": "$suggestedItem.attributes",
            "originalPrice": "$suggestedItem.originalPrice", "calculatedPrice": "$suggestedItem.calculatedPrice",
            "discounts": "$suggestedItem.discounts"}}
    ,{$limit: 2}
]);

db.orders.find( {"orderNumber": 100001});
// M06 - Suggest Products (end)

// M07 - Remove Reservation (start)
db.inventory.updateOne(
    {"productId": 1001},
    {
        $inc: {unitsBackordered: -8},
        $set: {unitsOnOrder: 0, updatedBy: "system", updateDate: ISODate()}
    }
    );
// M07 - Remove Reservation (end)

// M09 - Give Bonus Points (start)
db.orders.aggregate([
    {$match: {"userId": 1001, "status": "processed", totalPrice: {$gte: 40}}}
    ,{$project: {_id: 0, "userId": 1, "orderNumber": 1, totalPrice: {$round: ["$totalPrice", 2]}, bonusPoints: {$round: [{$multiply: ["$totalPrice", 0.1]}, 2]}}}
]);

db.orders.updateOne(
    {"orderNumber": 100001},
    {
        $set: {"bonusPointsEarned": new NumberDecimal(5.00), updatedBy: "system", updateDate: ISODate()}
    }
    );

// NOTE: The bonus points should be dynamic based on total for customer based on order date.
// The customer should just have a bonusPointsUsed property, which alleviates the need of a transaction
db.customers.updateOne(
    {"userId": "C001"},
    {
        $inc: {bonusPoints: NumberDecimal(5.00)},
        $set: {updateBy: "system", updateDate: ISODate()}
    });



// M09 - Give Bonus Points (end)

db.customers.find({"userId": 1001});



// Find top order number
db.orders.aggregate([
    {$match: {"isActive": true}}
    ,{$project: {_id: 0, orderNumber: 1}}
    ,{$sort: {orderNumber: -1}}
    ,{$limit: 1}
]);

// Find order numbers
db.orders.aggregate([
    {$match: {"isActive": true}}
    ,{$project: {_id: 0, orderNumber: 1, orderNum: 1}}
    ,{$addFields: {orderNumType: {$type: "orderNumber"}}}
    //,{$addFields: {orderNum: {$toLong: "orderNumber"}}}
/*    ,{$addFields: {orderNum: {convert: {
        input: "orderNumber",
                    to: "string",
                    onError: {
                        $concat:
                            [
                                "Could not convert ",
                                { $toString:"orderNumber" },
                                " to type integer."
                            ]
                    }
                }}}}*/
    //,{$sort: {orderNumber: -1}}
]);

db.orders.find( );