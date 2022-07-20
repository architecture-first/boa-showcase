package com.architecture.first.framework.business.retail.model.cashier.repository;

import com.architecture.first.framework.business.retail.model.cashier.model.ItemsToProcess;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.business.retail.model.cashier.model.PaymentInfo;
import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.OrderConfirmation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class OrderRepository {
    private MongoClient mongoClient;
    private final MongoDatabase database;

    @Value("${data.database.connectionString}")
    private String dbConnectionString;

    @Value("${data.database.name}")
    private String databaseName;

    @Autowired
    public OrderRepository(MongoDatabase mongoDatabase) {
        database = mongoDatabase;
    }

    // S01 - Request Payment
    // Find the customer's active order
    public Order getOrder(Long userId, Long orderNumber, String status) {
        var collection = database.getCollection("orders", Order.class);

        String query = """
              [
               {$match: {"userId": :userId, "orderNumber": :orderNumber, "status": ":status"}}
              ,{$unwind: {path: "$items"}}
              ,{$project: {_id: 0, "userId": 1, "orderNumber": 1, "items": 1}}
              ,{$project: {"userId": 1, "orderNumber": 1, "productId": "$items.productId", "totalUnitPrice": {$multiply:["$items.calculatedPrice", "$items.quantity"]}}}
              ,{$project: {"userId": 1, "orderNumber": 1, "totalPrice": {$sum: "$totalUnitPrice"}}}
              ,{$project: {"userId": 1, "orderNumber": 1, "totalPrice": 1, "shippingCost": {$multiply: ["$totalPrice", 0.1]}}}
              ,{$project: {"userId": 1, "orderNumber": 1, "totalPrice": {$round: ["$totalPrice", 2]}, "shippingCost": {$round: ["$shippingCost", 2]}}}
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
                          ,{$project: {"productId": 1, "name": "$itemDetails.name", "type": "$itemDetails.type",
                                  "imageUrl": "$itemDetails.imageUrl", "attributes": "$itemDetails.attributes",
                                  "shippingCost": 1, "quantity": 1, "totalUnitPrice": 1}}
                      ],
                      as: "purchaseItems"
                  }
              }
              ,{$limit: 1}
          ]                                
                 """;

        query = query.replace(":userId", userId.toString())
                .replace(":status", status)
                .replace(":orderNumber", orderNumber.toString());

        Gson gson = new Gson();
        var pipeline = convertString(query);

        var orders = collection.aggregate(pipeline).into(new ArrayList<Order>());
        return orders.get(0);
    }


    // S02 - Process Payment (start)
    // Get Payment info from customer information
    public List<PaymentInfo> getPaymentInformation(Long userId) {
        var collection = database.getCollection("customers", PaymentInfo.class);

        String query = """
                [
                  {$match: {"userId": :userId, "isRegistered": true}}
                  ,{$unwind: {path: "$payment"}}
                  ,{$project: {_id: 0, "payment": 1}}
                  ,{$match: {"payment.isDefault": true}}
                ]                                
                   """;

        query = query.replace(":userId", userId.toString());

        Gson gson = new Gson();
        var pipeline = convertString(query);

        return collection.aggregate(pipeline).into(new ArrayList<PaymentInfo>());
    }

    public List<ItemsToProcess> getItemsToProcess() {
        var collection = database.getCollection("inventory", ItemsToProcess.class);

        String query = """
                [
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
                ]                                
                   """;

        Gson gson = new Gson();
        var pipeline = convertString(query);

        return collection.aggregate(pipeline).into(new ArrayList<ItemsToProcess>());
    }

    // cart (shopping cart) -> processed (paid) -> complete (history)
    public long updateOrderStatus(Long userId, Long orderNumber, String currentStatus, String newStatus) {
        var collection = database.getCollection("orders");

        String filter = """
                {"userId": :userId, "orderNumber": :orderNumber, "status": ":currentStatus"},
                """;
        filter = filter.replace(":userId", userId.toString())
                .replace(":currentStatus", currentStatus)
                .replace(":orderNumber", orderNumber.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {
                    $set: {"status": ":newStatus", updatedBy: "system", updateDate: ISODate()}
                }
                """;
        sPipeline = sPipeline.replaceAll(":newStatus", newStatus);
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    public long updateOrderTotals(Long userId, Long orderNumber, BigDecimal totalPrice, BigDecimal shippingCost) {
        var collection = database.getCollection("orders");

        String filter = """
                {"userId": :userId, "orderNumber": :orderNumber},
                """;
        filter = filter.replace(":userId", userId.toString())
                .replace(":orderNumber", orderNumber.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {
                    $set: {"totalPrice": NumberDecimal(":totalPrice"), "shippingCost": NumberDecimal(":shippingCost"),  updatedBy: "system", updateDate: ISODate()}
                }
                """;
        sPipeline = sPipeline.replace(":totalPrice", totalPrice.toString())
                .replace(":shippingCost", shippingCost.toString());
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    // S03 - Present Order Confirmation
    public OrderConfirmation getOrderConfirmation(Long userId, Long orderNumber) {
        var collection = database.getCollection("orders", OrderConfirmation.class);

        var filter = """
                {"userId": :userId, "orderNumber": :orderNumber, "status": "processed"},
                """;
        filter = filter.replace(":userId", userId.toString())
                .replace(":orderNumber", orderNumber.toString());
        BasicDBObject bfilter = BasicDBObject.parse(filter);

        String projection = """
                    {_id: 0, isActive: 0, status: 0, updateDate: 0, updatedBy: 0 }
                """;
        BasicDBObject bProjection = BasicDBObject.parse(projection);

        return collection.find(bfilter).projection(bProjection).first();
    }


    private List<BasicDBObject> convertString(String query) {
        Gson gson = new Gson();
        var listType = new TypeToken<ArrayList<Object>>(){}.getType();
        ArrayList<Object> b = gson.fromJson(query, listType );
        var a = gson.fromJson(query, Object[].class);

        List<BasicDBObject> pipeline = new ArrayList<>();

        b.stream().forEach(o -> {
            pipeline.add(BasicDBObject.parse(gson.toJson(o)));
        });

        return pipeline;
    }

    public Product getProductById(Long id) {
        MongoCollection<Product> collection = database.getCollection("inventory", Product.class);

        String filter = """
                {"isActive": true, "productId": :productId},
                """;
        filter = filter.replace(":productId", id.toString());
        BasicDBObject bfilter = BasicDBObject.parse(filter);

        String projection = """
                    {_id: 0, "productId": 1, "name": 1, "type": 1, "imageUrl": 1,
                        "attributes": 1, "price": 1 }
                """;
        BasicDBObject bProjection = BasicDBObject.parse(projection);

        return collection.find(bfilter).projection(bProjection).first();
    }
}
