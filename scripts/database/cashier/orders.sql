//db.orders.drop();

db.orders.insertMany([
    {
        "orderNumber": 100001,
        "userId": 1001,
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
                "productId": 1001,
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
        "orderNumber": 100002,
        "userId": 1002,
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
                "productId": 1001,
                "attributes": ["Color: White"],
                "quantity": 1,
                "originalPrice": NumberDecimal("45.99"),
                "calculatedPrice": NumberDecimal("44.99"),
                "discounts": [NumberDecimal("1.00")]
            },
            {
                "productId": 1002,
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
        "orderNumber": 100003,
        "userId": 101,
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
                "productId": 1003,
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

// Find order
db.orders.find({"orderNumber": 100001});

// Find pending orders
db.orders.find({"status": "pending"});

// Change order status
db.orders.updateOne(
    {"orderNumber": 100001 },
    {
        $set: {status: "pending", updatedBy: "system" },
        $currentDate: {updatedDate: true}
    }
    );