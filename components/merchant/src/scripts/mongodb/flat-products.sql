// load inventory

//db.inventory.drop();

db.inventory.insertMany( [
    {
        "productId": "F001",
        "name": "Rod's Fishing Rod",
        "type": "Fishing Rod",
        "imageUrl": "https://tmblobstorage1.blob.core.windows.net/$web/Fishing-Rod.png?sp=r&st=2022-03-06T16:27:50Z&se=2024-03-31T23:27:50Z&spr=https&sv=2020-08-04&sr=b&sig=W%2Bz2VLjYrcWVuWxqnqKvD%2F21Hc4jPumYV0QvG2AZKRY%3D",
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
        "productId": "F002",
        "name": "Larry's Fishing Lure",
        "type": "Fishing Lure",
        "imageUrl": "https://tmblobstorage1.blob.core.windows.net/$web/Fishing-Lure.png?sp=r&st=2022-03-06T16:24:28Z&se=2024-03-31T23:24:28Z&spr=https&sv=2020-08-04&sr=b&sig=IBnCoL9aPVlFhenbKaAwc8g58HjVRhIh9B8hodndUHc%3D",
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
        "productId": "F003",
        "name": "Frank's Fishing Boat",
        "type": "Row Boat",
        "imageUrl": "https://tmblobstorage1.blob.core.windows.net/$web/Fishing-Row-Boat.png?sp=r&st=2022-03-06T16:28:52Z&se=2024-03-31T23:28:52Z&spr=https&sv=2020-08-04&sr=b&sig=V0tAVdO42l9uyPUH4qQsID7ALMDt8RTZbTP%2BLBuXqPA%3D",
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
        "productId": "F004",
        "name": "Ray's Fishing Tool Repair",
        "type": "Fishing Tool Repair",
        "imageUrl": "https://tmblobstorage1.blob.core.windows.net/$web/Fishing-Repair-Tools.png?sp=r&st=2022-03-06T16:26:36Z&se=2024-03-31T23:26:36Z&spr=https&sv=2020-08-04&sr=b&sig=Pfn0knmOaEYnr2rXMlc8UdklMOEepMnNYumjNw0O0Gk%3D",
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


