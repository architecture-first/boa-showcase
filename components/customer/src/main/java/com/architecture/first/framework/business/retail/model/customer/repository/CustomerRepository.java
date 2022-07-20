package com.architecture.first.framework.business.retail.model.customer.repository;

import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.business.retail.model.customer.CustomerRegistration;
import com.architecture.first.framework.business.retail.model.customer.CustomerSignUp;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import com.architecture.first.framework.business.retail.model.customer.cart.ShoppingCart;
import com.architecture.first.framework.business.vicinity.tickets.TicketNumber;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class CustomerRepository {
    private MongoClient mongoClient;
    private final MongoDatabase database;
    private final TicketNumber ticketNumber;

    @Value("${data.database.connectionString}")
    private String dbConnectionString;

    @Value("${data.database.name}")
    private String databaseName;

    @Autowired
    public CustomerRepository(MongoDatabase mongoDatabase, TicketNumber ticketNumber) {
        database = mongoDatabase;
        this.ticketNumber = ticketNumber;
    }

    // C02 - Add Product to Cart (start)
    // 1. find if current cart
    public Optional<ShoppingCart> getShoppingCart(Long userId) {
        var collection = database.getCollection("orders", ShoppingCart.class);
        String filter  = """
            {"userId": :userId,"status": "cart"}
            """;

        filter = filter.replace(":userId", userId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        var carts = collection.find(query).into(new ArrayList<ShoppingCart>());
        return carts.size() > 0 ? Optional.of(carts.get(0)) : Optional.empty();
    }


    public Order getOrderPreview(Long userId) {
        var collection = database.getCollection("orders", Order.class);

        String query = """
              [
               {$match: {"userId": :userId,"status": "cart"}}
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
                                  foreignField: productId,
                                  as: "itemDetails"
                              }
                          }
                          ,{$unwind: {path: "$itemDetails"}}
                          ,{$project: {"userId": 1, "productId": 1, "name": "$itemDetails.name", "type": "$itemDetails.type",
                                  "imageUrl": "$itemDetails.imageUrl", "attributes": "$itemDetails.attributes",
                                  "shippingCost": 1, "quantity": 1, "totalUnitPrice": 1}}
                      ],
                      as: "purchaseItems"
                  }
              }
          ]                                
                 """;

        query = query.replace(":userId", userId.toString());

        Gson gson = new Gson();
        var pipeline = convertString(query);

        var orders = collection.aggregate(pipeline).into(new ArrayList<Order>());
        return (orders.size() > 0) ? orders.get(0) : null;
    }

    // C02 - Add Product to Cart (continued)
    // 2. Get customer information
    public Optional<CustomerRegistration> getCustomerInfo(Long userId) {
        var collection = database.getCollection("customers", CustomerRegistration.class);

        String query = """
                    [
                    {$match: {"userId": :userId, "isActive": true}}
                    ,{$project: {_id: 0, isActive: 0, isRegistered: 0, payment: 0, updateDate: 0, updatedBy: 0}}
                  ]                                
                """;
        query = query.replace(":userId", userId.toString());

        Gson son = new Gson();
        var pipeline = convertString(query);

        var customerRegs =  collection.aggregate(pipeline).into(new ArrayList<CustomerRegistration>());

        return customerRegs.size() > 0 ? Optional.of(customerRegs.get(0)) : Optional.empty();
    }

    // C02 - Add Product to Cart (continued)
    // 3a. Create an order / cart if doesn't exist
    public long createOrder(ShoppingCart shoppingCart) {
        var collection = database.getCollection("orders", ShoppingCart.class);

        long orderNumber = ticketNumber.next("orderNumber", n -> {
            return getTopOrderNumber();
        });

        shoppingCart.setOrderNumber(orderNumber);
        shoppingCart.setIsActive(true);
        shoppingCart.setUpdateDate(new Date());
        shoppingCart.setUpdatedBy(shoppingCart.getEmailAddress());

        collection.insertOne(shoppingCart);

        return 1;
    }

    // C02 - Add Product to Cart (continued)
    // 3b. update existing cart
    public long addItemToExistingOrder(Long userId, CartItem item) {
        var collection = database.getCollection("orders");

        String filter = """
                {"userId": :userId, "status": "cart"},
                """;
        filter = filter.replace(":userId", userId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {
                    $push: {
                         "items":
                             {
                                 "productId": :productId,
                                 "attributes": [],
                                 "quantity": :quantity,
                                 "originalPrice": NumberDecimal(":originalPrice"),
                                 "calculatedPrice": NumberDecimal(":calculatedPrice"),
                                 "discounts": [],
                                 updatedBy: "system", updateDate: ISODate()
                             }
                    }
                }
                """;
        sPipeline = sPipeline
                .replace(":productId", item.getProductId().toString())
                .replace(":quantity", item.getQuantity().toString())
                .replace(":originalPrice", item.getOriginalPrice().toString())
                .replace(":calculatedPrice", item.getCalculatedPrice().toString())
        ;

        // TODO - support attributes and discounts

        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    // C12 - Sign Up
    public long registerCustomer(CustomerSignUp customerSignUp) {
        var collection = database.getCollection("customers", CustomerSignUp.class);

        if (userIdExists(customerSignUp.getEmailAddress())) {
            return -1;
        }

        long customerNumber = ticketNumber.next("customerId", n -> {
            return getTopCustomerId();
        });

        customerSignUp.setUserId(customerNumber);
        customerSignUp.setIsRegistered(true);
        customerSignUp.setIsActive(true);
        customerSignUp.setUpdateDate(new Date());
        customerSignUp.setUpdatedBy(customerSignUp.getEmailAddress());

        collection.insertOne(customerSignUp);

        return customerNumber;
    }

    public boolean isCustomerRegistered(Long userId) {
        var collection = database.getCollection("customers");
        String filter  = """
            {"userId": :userId,"isRegistered": true}
            """;

        filter = filter.replace(":userId", userId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        var customers = collection.find(query).into(new ArrayList<Document>());
        return customers.size() > 0;
    }

    public boolean userIdExists(String emailAddress) {
        var collection = database.getCollection("customers");
        String filter  = """
            {"emailAddress": ":emailAddress"}
            """;

        filter = filter.replace(":userId", emailAddress);
        BasicDBObject query = BasicDBObject.parse(filter);

        var customers = collection.find(query).into(new ArrayList<Document>());
        return customers.size() > 0;
    }

    public Long getTopOrderNumber() {
        var collection = database.getCollection("orders");

        String query = """
                [
                   {$match: {"isActive": true}}
                  ,{$project: {_id: 0, orderNumber: 1}}
                  ,{$sort: {orderNumber: -1}}
                  ,{$limit: 1}
                ]                                
                   """;

        Gson gson = new Gson();
        var pipeline = convertString(query);

        var orderNumbers = collection.aggregate(pipeline).into(new ArrayList<Document>());

        return Double.valueOf(orderNumbers.get(0).get("orderNumber").toString()).longValue();
    }

    public Long getTopCustomerId() {
        var collection = database.getCollection("customers");

        String query = """
                [
                   {$match: {"isActive": true}}
                  ,{$project: {_id: 0, userId: 1}}
                  ,{$sort: {userId: -1}}
                  ,{$limit: 1}
                ]                                
                   """;

        Gson gson = new Gson();
        var pipeline = convertString(query);

        var userIds = collection.aggregate(pipeline).into(new ArrayList<Document>());

        return Double.valueOf(userIds.get(0).get("userId").toString()).longValue();
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

}
