//db.customers.drop();

db.customers.insertMany([
    {
        "userId": 1001,
        "firstName": "Bob",
        "middleInitial": "N",
        "lastName": "Weave",
        "billingAddress": {
            "street": "123 Boxing Lane",
            "city": "Ring",
            "state": "NJ",
            "zip": "01111"
        },
        "emailAddress": "bob-n-weave@nowhere.com",
        "username": "bob-n-weave@nowhere.com",
        "isRegistered": true,
        "isActive": true,
        "payment": [
            {
                "type": "MasterCard",
                "name": "Bob N Weave",
                "number": "****4999",
                "expirationDate": ISODate("2029-03-31"),
                "isDefault": true
            }
        ],
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "userId": 1002,
        "firstName": "Erin",
        "lastName": "Waters",
        "billingAddress": {
            "street": "456 Sea Drive",
            "city": "Lakeshore",
            "state": "NJ",
            "zip": "01112"
        },
        "emailAddress": "erin-waters@nowhere.com",
        "username": "erin-waters@nowhere.com",
        "isRegistered": true,
        "isActive": true,
        "payment": [
            {
                "type": "Visa",
                "name": "Erin Waters",
                "number": "****3838",
                "expirationDate": ISODate("2028-05-31"),
                "isDefault": true
            }
        ],
        "updateDate": new Date(),
        "updatedBy": "system"
    },
    {
        "userId": 101,
        "emailAddress": "guest-who@nowhere.com",
        "isRegistered": false,
        "isActive": true,
        "updateDate": new Date(),
        "updatedBy": "system"
    }
]);

// Create Security Customer (start)
db.customers.insertOne(
    {
        "userId": 201,
        "firstName": "Bill",
        "middleInitial": "D",
        "lastName": "Wahl",
        "billingAddress": {
            "street": "99 Security Street",
            "city": "Fort Lee",
            "state": "NJ",
            "zip": "01010"
        },
        "emailAddress": "bill.d.wahl@matrix.com",
        "username": "bill.d.wahl@matrix.com",
        "isRegistered": true,
        "isActive": true,
        "payment": [
            {
                "type": "MasterCard",
                "name": "Bill D Wahl",
                "number": "****4888",
                "expirationDate": ISODate("2029-05-31"),
                "isDefault": true
            }
        ],
        "updateDate": new Date(),
        "updatedBy": "system"
    }
    );
// Create Security Customer (end)

// Find Customer Information
db.customers.find({"userId": 1001});

// Add Product To Cart (start)
db.inventory.find({"productId": 1004},
    {
        "productId": 1,
        "attributes": 1,
        "price": 1
    }
    );

db.customers.updateOne(
    {"userId": 1001},
    {
        $push: {
            "shoppingCart":
                {
                    "productId": 1004,
                    "attributes": ["Color: Grey"],
                    "quantity": 1,
                    "originalPrice": 25.99,
                    "calculatedPrice": 25.99,
                    "discounts": []
                }
        }
    });
// Add Product To Cart (end)