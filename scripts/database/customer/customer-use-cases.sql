// Customer Use Cases

// Customer
// C01 - View Products (start)
// call merchant.showProducts()
// C01 - View Products (end)

// C02 - Add Product to Cart (start)
// 1. find if current cart
db.orders.find({"userId": 1001,"status": "cart"});

// 2. Get customer information
db.customers.aggregate([
    {$match: {"userId": 1001, "isActive": true}}
    ,{$project: {_id: 0, isActive: 0, isRegistered: 0, payment: 0, updateDate: 0, updatedBy: 0, userId: 0 }}
]);

// 3a. Create an order / cart if doesn't exist
db.orders.insertOne(
    {
        "orderNumber": 10000 + Math.floor(Math.random() * 1000),
        "billingAddress": {
            "street": "123 Boxing Lane",
            "city": "Ring",
            "state": "NJ",
            "zip": "01111"
        },
        "userId": 1001,
        "emailAddress": "bob-n-weave@nowhere.com",
        "firstName": "Bob",
        "lastName": "Weave",
        "middleInitial": "N",
        "isActive": true,
        "items": [
            {
                "productId": 1002,
                "attributes": ["Color: Grey"],
                "quantity": 1,
                "originalPrice": NumberDecimal(9.99),
                "calculatedPrice": NumberDecimal(9.99),
                "discounts": []
            }
        ],
        "status": "cart",
        "updateDate": ISODate(),
        "updatedBy": "system"
    }
    );

// 3b. update existing cart
db.orders.updateOne(
    {"userId": 1001, "status": "cart"},
    {
        $push: {
            "items":
                {
                    "productId": 1004,
                    "attributes": ["Color: Grey"],
                    "quantity": 1,
                    "originalPrice": 25.99,
                    "calculatedPrice": 25.99,
                    "discounts": []
                }
        }
    }
    );


// misc.
db.orders.find();
db.customers.find();
db.customers.find({"userId": 1001,"isRegistered": true}, {_id: 0, isRegistered: 1});

db.orders.find({"userId": 1001, "status": "cart"});
db.orders.find({"userId": 1001, "status": "processed"});
db.orders.updateOne({"userId": 1001}, {$set: {"status": "cart"}});

db.orders.deleteOne({_id: ObjectId("623bb15647ba91da0c554495") });
db.orders.updateOne(
    {"userId": 1001, "status": "cart"},
    {$unset: {"shoppingCart": ""}}
    );
// C02 - Add Product to Cart (end)

// C03 - Checkout (start)
// call merchant.checkout()
// C03 - Checkout (end)

// C04 - Provide Payment (start)
// send the following to merchant.acceptPayment()
// {
//     "type": "MasterCard",
//     "name": "Bob N Weave",
//     "number": "****4999",
//     "expirationDate": ISODate("2029-03-31"),
//     "isDefault": true
// }
// C04 - Provide Payment (end)

// C05 - View Order Confirmation (start)
// call merchant.showOrderConfirmation()
// C05 - View Order Confirmation (end)

// C11 - View Product (start)
// call merchant.showProduct()
// C11 - View Product (end)

// Registered Customer
// C06 - View Suggested Products (start)
// call merchant.showSuggestedProducts()
// C06 - View Suggested Products (end)

// C07 - Checkout as Registered Customer (start)
// call cashier.checkout()
// C07 - Checkout as Registered Customer (end)

// C08 - View Order History (start)
// call cashier.showOrderHistory
// C08 - View Order History (end)

// C09 - Receive Bonus Points (start)
// call merchant.giveBonusPoints
// C09 - Receive Bonus Points (end)

// Guest
// C10 - Checkout as Guest (start)
// call cashier.checkout()
// C10 - Checkout as Guest (start)

// C12 - Sign Up (start)
db.customers.insertOne(
    {
        "userId": 1005,
        "firstName": "Rob",
        "middleInitial": "",
        "lastName": "Bot001",
        "billingAddress": {
            "street": "1010 Ascii Drive",
            "city": "K8s",
            "state": "NJ",
            "zip": "01010"
        },
        "emailAddress": "rob.bot001@matrix.com",
        "userId": "rob.bot001@matrix.com",
        "isRegistered": true,
        "isActive": true,
        "payment": [
            {
                "type": "MasterCard",
                "name": "Rob Bot",
                "number": "****4888",
                "expirationDate": ISODate("2029-05-31"),
                "isDefault": true
            }
        ],
        "updateDate": new Date(),
        "updatedBy": "system"
    }
    );
// C12 - Sign Up (end)

db.customers.find();

//db.customers.drop();

// load customers (start)
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
        "username": "guest-who@nowhere.com",
        "emailAddress": "guest-who@nowhere.com",
        "isRegistered": false,
        "isActive": true,
        "updateDate": new Date(),
        "updatedBy": "system"
    }
]);
// load customers (end)

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


// Create Security Customer (start)
db.customers.insertOne(
    {
        "userId": 99,
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