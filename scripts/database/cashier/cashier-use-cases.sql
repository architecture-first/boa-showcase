// Cashier Use Cases

// S01 - Request Payment (start)
// ask customer the request
// total bill
db.orders.aggregate([
    {$match: {"userId": "C001","status": "cart"}}
    ,{$unwind: {path: "$items"}}
    ,{$project: {_id: 0, "userId": 1, "items": 1}}
    ,{$project: {"userId": 1,"productId": "$items.productId", "totalUnitPrice": {$multiply:["$items.calculatedPrice", "$items.quantity"]}}}
    ,{$project: {"userId": 1,"totalPrice": {$sum: "$totalUnitPrice"}}}
    ,{$project: {"userId": 1,"totalPrice": 1, "shippingCost": {$multiply: ["$totalPrice", 0.1]}}}
    ,{$project: {"userId": 1,"totalPrice": {$round: ["$totalPrice", 2]}, "shippingCost": {$round: ["$shippingCost", 2]}}}
    ,{
        $lookup: {
            from: "orders",
            localField: "userId",
            foreignField: "userId",
            pipeline: [
                {$match: {"status": "cart"}}
                ,{$unwind: {path: "$items"}}
                ,{$project: {_id: 0, "items": 1}}
                ,{$project: {"productId": "$items.productId", "quantity": "$items.quantity",
                        "totalUnitPrice": {$multiply:["$items.calculatedPrice", "$items.quantity"]}}}
                ,{$project: {"productId": 1, "quantity": 1, "totalUnitPrice": {$round:["$totalUnitPrice", 2]}}}
                ,{
                    $lookup: {
                        from: "inventory",
                        localField: "productId",
                        foreignField: "productId",
                        as: "itemDetails"
                    }
                }
                ,{$unwind: {path: "$itemDetails"}}
                ,{$project: {"name": "$itemDetails.name", "type": "$itemDetails.type",
                        "imageUrl": "$itemDetails.imageUrl", "attributes": "$itemDetails.attributes",
                        "shippingCost": 1, "quantity": 1, "totalUnitPrice": 1}}
            ],
            as: "purchaseItems"
        }
    }
]);

db.orders.find();
//db.orders.drop();

db.orders.aggregate([
    {$match: {"userId": "C001","status": "cart"}}
    ,{$project: {_id: 0, "userId": 1, "items": 1}}
    // ,{$project: {"userId": 1,"productId": "$items.productId", "totalUnitPrice": {$multiply:["$items.calculatedPrice", "$items.quantity"]}}}
    // ,{$project: {"userId": 1,"totalPrice": {$sum: "$totalUnitPrice"}}}
    // ,{$project: {"userId": 1,"totalPrice": 1, "shippingCost": {$multiply: ["$totalPrice", 0.1]}}}
    // ,{$project: {"userId": 1,"totalPrice": {$round: ["$totalPrice", 2]}, "shippingCost": {$round: ["$shippingCost", 2]}}}

]);
// S01 - Request Payment (end)

// S02 - Process Payment (start)
// Get Payment info from customer collection
db.customers.aggregate([
    {$match: {"userId": "C001", "isRegistered": true}}
    ,{$unwind: {path: "$payment"}}
    ,{$project: {_id: 0, "payment": 1}}
    ,{$match: {"payment.isDefault": true}}
    ]);

// Find items to update
db.inventory.aggregate([
    {
        $lookup: {
            from: "orders",
            localField: "productId",
            foreignField: "items.productId",
            pipeline: [
                {$match: {"status": "processing"}}
             ],
            as: "purchasedItems"
        }
    }
    ,{ "$match": {"purchasedItems.0": { "$exists": true } } }
    ,{$unwind: {path: "$purchasedItems"}}
    ,{$unwind: {path: "$purchasedItems.items"}}
    ,{$project: {_id: 0, "productId": 1, "orderNumber": 1, "userId": 1,
            "purchasedQuantity": "$purchasedItems.items.quantity"}}
]);

// Update based on results of previous query for each product (handled by the Merchant)
db.inventory.updateOne(
    {"productId": "F001"},
    {$inc: {unitsAvailable: -1}},
    {$set: {unitsOnOrder: 0, updateBy: "system", updateDate: ISODate()}}
);

// cart (shopping cart) -> processing (payment) -> processed (paid) -> complete (history)
db.orders.updateOne({"userId": "C001", "status": "processing"}, {$set: {"status": "processed", updateBy: "system", updateDate: ISODate()}});

var session = db.getMongo().startSession();
session1.startTransaction({readConcern: {level: 'snapshot'}, writeConcern: {w: 'majority'}});
// update order
// db.orders.find({"userId": "C001"});
// db.orders.updateOne({"userId": "C001"}, {$set: {"status": "cart"}});

db.orders.updateOne({"userId": "C001", "status": "processed"}, {$set: {"status": "complete", updatedBy: "system", updateDate: ISODate()}});

// Reduce inventory
db.orders.updateOne({"userId": "C001"}, {$set: {"status": "processing"}});

db.orders.find({"userId": "C001","status": "processing"}).forEach((o) => {print(o.userId);});

session1.commitTransaction();
session1.endSession();
// S02 - Process Payment (end)

// update backordered items
db.inventory.updateOne(
    {"productId": "F001"},
    {$inc: {unitsBackordered: -1}},
    {$set: {unitsOnOrder: 0, updatedBy: "system", updateDate: ISODate()}}
    );

// S03 - Present Order Confirmation (start)
db.orders.find(
    {"userId": "C001", "status": "processed"},
    {_id: 0, isActive: 0, status: 0, updateDate: 0, updatedBy: 0 }
    );
// S03 - Present Order Confirmation (end)

// S04 - Store Order History (start)
db.orders.updateOne({"userId": "C001", "status": "processed"}, {$set: {"status": "complete", updatedBy: "system", updateDate: ISODate()}});
// S04 - Store Order History (end)

db.orders.find({"userId": "C001","status": "cart"});
db.inventory.find({"productId": "F001"});

db.orders.updateOne(
    {"orderNumber": "100003"},
    {$rename: {shoppingCart: "items"}}
    );


// add orders (start)
db.orders.insertMany([
    {
        "orderNumber": "100001",
        "userId": "C001",
        "billingAddress": {
            "street": "123 Boxing Lane",
            "city": "Ring",
            "state": "NJ",
            "zip": "01111"
        },
        "shippingAddress": {
            "street": "123 Boxing Lane",
            "city": "Ring",
            "state": "NJ",
            "zip": "01111"
        },
        "emailAddress": "bob-n-weave@nowhere.com",
        "isActive": true,
        "items": [
            {
                "productId": "F001",
                "attributes": ["Color: Red"],
                "quantity": 1,
                "originalPrice": NumberDecimal("45.99"),
                "calculatedPrice": NumberDecimal("44.99"),
                "discounts": [NumberDecimal("1.00")]
            }
        ],
        "shippingCost": NumberDecimal("5.00"),
        "totalPrice": NumberDecimal("49.99"),
        "datePurchased": ISODate("2022-02-21"),
        "status": "cart",
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "orderNumber": "100002",
        "userId": "C002",
        "shippingAddress": {
            "street": "456 Sea Drive",
            "city": "Lakeshore",
            "state": "NJ",
            "zip": "01112"
        },
        "billingAddress": {
            "street": "456 Sea Drive",
            "city": "Lakeshore",
            "state": "NJ",
            "zip": "01112"
        },
        "emailAddress": "erin-waters@nowhere.com",
        "isActive": true,
        "items": [
            {
                "productId": "F001",
                "attributes": ["Color: White"],
                "quantity": 1,
                "originalPrice": NumberDecimal("45.99"),
                "calculatedPrice": NumberDecimal("44.99"),
                "discounts": [NumberDecimal("1.00")]
            },
            {
                "productId": "F002",
                "attributes": ["Color: Grey"],
                "quantity": 2,
                "originalPrice": NumberDecimal("9.99"),
                "calculatedPrice": NumberDecimal("9.99"),
                "discounts": []
            }
        ],
        "shippingCost": NumberDecimal("5.00"),
        "totalPrice": NumberDecimal("74.97"),
        "datePurchased": ISODate("2022-02-24"),
        "status": "complete",
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "orderNumber": "100003",
        "userId": "guest-who@nowhere.com",
        "shippingAddress": {
            "street": "789 Guest Ave",
            "city": "Mountainville",
            "state": "NJ",
            "zip": "01113"
        },
        "billingAddress": {
            "street": "789 Guest Ave",
            "city": "Mountainville",
            "state": "NJ",
            "zip": "01113"
        },
        "emailAddress": "guest-who@nowhere.com",
        "isRegistered": false,
        "isActive": true,
        "items": [
            {
                "productId": "F003",
                "attributes": ["Color: Blue"],
                "quantity": 1,
                "originalPrice": NumberDecimal("300.99"),
                "calculatedPrice": NumberDecimal("295.99"),
                "discounts": [NumberDecimal("5.00")]
            }
        ],
        "shippingCost": NumberDecimal("0"),
        "totalPrice": NumberDecimal("295.99"),
        "datePurchased": ISODate("2022-02-28"),
        "status": "complete",
        "updateDate": new Date(),
        "updatedBy": "system"
    }
]);


// add orders (end)