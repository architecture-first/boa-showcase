//db.customers.drop();

db.customers.insertMany([
    {
        "userId": "C001",
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
        "userId": "bob-n-weave@nowhere.com",
        "isRegistered": true,
        "isActive": true,
        "shoppingCart": [
            {
                "productId": "F001",
                "attributes": ["Color: Red"],
                "quantity": 1,
                "originalPrice": NumberDecimal(45.99),
                "calculatedPrice": NumberDecimal(44.99),
                "discounts": [NumberDecimal(1.00)]
            }
        ],
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
        "userId": "C002",
        "firstName": "Erin",
        "lastName": "Waters",
        "billingAddress": {
            "street": "456 Sea Drive",
            "city": "Lakeshore",
            "state": "NJ",
            "zip": "01112"
        },
        "emailAddress": "erin-waters@nowhere.com",
        "userId": "erin-waters@nowhere.com",
        "isRegistered": true,
        "isActive": true,
        "shoppingCart": [
        ],
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
        "customerId": "guest-who@nowhere.com",
        "emailAddress": "guest-who@nowhere.com",
        "isRegistered": false,
        "isActive": true,
        "shoppingCart": [
        ],
        "updateDate": new Date(),
        "updatedBy": "system"
    }
]);

// Find Customer Information
db.customers.find({"userId": "C001"});

// Add Product To Cart (start)
db.inventory.find({"productId": "F004"},
    {
        "productId": 1,
        "attributes": 1,
        "price": 1
    }
    );

db.customers.updateOne(
    {"userId": "C001"},
    {
        $push: {
            "shoppingCart":
                {
                    "productId": "F004",
                    "attributes": ["Color: Grey"],
                    "quantity": 1,
                    "originalPrice": 25.99,
                    "calculatedPrice": 25.99,
                    "discounts": []
                }
        }
    });
// Add Product To Cart (end)