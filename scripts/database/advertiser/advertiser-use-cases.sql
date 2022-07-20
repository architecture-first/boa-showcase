// Advertiser Use Cases

// A01 - Review Customer Analytics (start)
// Find top ordered products
db.orders.aggregate([
    {$match: {status: "complete"}}
    ,{$unwind: {path: "$items"}}
    ,{$group: {_id: {productId: "$items.productId"}, totalQuantity: {$sum: "$items.quantity"}}}
    ,{$sort: {totalQuantity: -1}}
    ,{$limit: 1}
    ,{
        $lookup: {
            from: "inventory",
            localField: "_id.productId",
            foreignField: "productId",
            pipeline: [
                {$match: {"isActive": true}}
            ],
            as: "products"
        }
    }
    ,{$unwind: {path: "$products"}}
    ,{$unwind: {path: "$products.price"}}
    ,{$project: {
        _id: 0, productId: "$products.productId", name: "$products.name", type: "$products.type",
        imageUrl: "$products.imageUrl", attributes: "$products.attributes", price: {$round: ["$products.price.value", 2]},
            discounts: "$products.price.discounts"}
    }
   ] );
// A01 - Review Customer Analytics (end)

// A02 - Present Advertisement (start)
// Return product from Review Customer Analytics use case
// A02 - Present Advertisement (end)