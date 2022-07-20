// load inventory

//db.inventory.drop();

db.inventory.insertMany( [
    {
        "productId": 1001,
        "name": "Rod's Fishing Rod",
        "type": "Fishing Rod",
        "imageUrl": "images/Fishing-Rod.png",
        "attributes": ["Color: Red", "Color: White", "Freshwater"],
        "price": [
            {"value": NumberDecimal(45.99), "type": "one-time", "discounts": [
                    {"name": "dollar-off", "partOf": "Dollar Days Discount", "value": NumberDecimal(1.00), "effectiveDate": ISODate("2022-05-10"), "expirationDate": ISODate("2022-05-31") }
                ]
            }
        ],
        "unitsAvailable": 105,
        "isActive": true,
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "productId": 1002,
        "name": "Larry's Fishing Lure",
        "type": "Fishing Lure",
        "imageUrl": "images/Fishing-Lure.png",
        "attributes": ["Color: Grey", "Freshwater", "Bass"],
        "price": [
            {"value": NumberDecimal(9.99), "type": "one-time", "discounts": []
            }
        ],
        "unitsAvailable": 400,
        "isActive": true,
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "productId": 1003,
        "name": "Frank's Fishing Boat",
        "type": "Row Boat",
        "imageUrl": "images/Fishing-Row-Boat.png",
        "attributes": ["Color: Blue", "Freshwater", "Oars"],
        "price": [
            {"value": NumberDecimal(300.99), "type": "one-time", "discounts": [
                    {"name": "dollar-off", "partOf": "Dollar Days Discount", "value": NumberDecimal(5.00), "effectiveDate": ISODate("2022-05-10"), "expirationDate": ISODate("2022-05-31") }
                ]
            }
        ],
        "unitsAvailable": 40,
        "isActive": true,
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "productId": 1004,
        "name": "Ray's Fishing Tool Repair",
        "type": "Fishing Tool Repair",
        "imageUrl": "images/Fishing-Repair-Tools.png",
        "attributes": ["Color: Grey", "Screwdriver", "Wrench"],
        "price": [
            {"value": NumberDecimal(25.99), "type": "one-time", "discounts": []
            }
        ],
        "unitsAvailable": 144,
        "isActive": true,
        "updateDate": new Date(),
        "updatedBy": "system"
    }
]);

// Find Active Products with Inventory
db.inventory.find(
    {"isActive": true, "unitsAvailable": {$gt: 0}}
    );

// Find Referenced Product (alone)
db.inventory.find(
    {"isActive": true, "productId": "F002"},
    {_id: 0, "productId": 1, "name": 1, "type": 1, "imageUrl": 1,
        "attributes": 1, "price": 1 }
    );

// Find Inventory Available
db.inventory.find(
    {"isActive": true, "productId": "F002"},
    {_id: 0, "productId": 1, "unitsAvailable": 1 }
    );

// Create Index
db.inventory.createIndex({
    "attributes": "text", "name": "text", "products.name": "text", "type": "text", "attributes": "text",
});

// Search products
db.inventory.find({$text: {$search: "wrenches"}});
db.inventory.find({$text: {$search: "Red"}});




// Misc


