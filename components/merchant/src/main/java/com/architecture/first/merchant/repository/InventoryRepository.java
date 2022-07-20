package com.architecture.first.merchant.repository;

import com.architecture.first.framework.business.retail.model.criteria.ShowProductsCriteria;
import com.architecture.first.framework.business.retail.model.results.InventorySuggestedProductsResult;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.results.BonusPointAnalysisResult;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.results.InventoryBackorderAnalysisResult;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.results.InventoryReorderAnalysisResult;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.*;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class InventoryRepository {
    private MongoClient mongoClient;
    private final MongoDatabase database;

    @Value("${data.database.connectionString}")
    private String dbConnectionString;

    @Value("${data.database.name}")
    private String databaseName;

    @Autowired
    public InventoryRepository(MongoDatabase mongoDatabase) {
        database = mongoDatabase;
    }

    // M01-Show-Products
    public List<Product> getProducts(ShowProductsCriteria criteria) {
        MongoCollection<Product> collection = database.getCollection("inventory", Product.class);
        String filter  = """
            {"isActive": true :replacementSnippet }
            """;
//     for example: {"isActive": true, "unitsAvailable": {$gt: 0}}
        filter = filter.replace(":replacementSnippet",  (!criteria.isEmpty()) ? ", " + criteria.getJsonCriteria(): "");

        BasicDBObject query = BasicDBObject.parse(filter);

        return collection.find(query).into(new ArrayList<Product>());
    }

    // M08-Show-Product
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


    public List<InventoryBackorderAnalysisResult> findItemsThatAreOnBackorder() {
        MongoCollection<InventoryBackorderAnalysisResult> collection = database.getCollection("orders", InventoryBackorderAnalysisResult.class);

        String query = """
                    [
                      {$match: {"status": "processed"}}
                      ,{$unwind: {path: "$items"}}
                      ,{
                          $lookup: {
                              from: "inventory",
                              localField: "items.productId",
                              foreignField: "productId",
                              pipeline: [
                                  {$unwind: {path: "$price"}}
                              ],
                              as: "inventoryItems"
                          }
                      }
                      ,{$unwind: {path: "$inventoryItems"}}
                      ,{$project: {_id: 0, "orderNumber": 1, "productId": "inventoryItems.productId", "inventoryQuantity": "$inventoryItems.unitsAvailable",
                              "orderQuantity": "$items.quantity",
                              "availableQuantity": {$subtract: ["$inventoryItems.unitsAvailable", "$items.quantity"]}
                      }}
                      ,{$project: {"orderNumber": 1, "productId": 1, "inventoryQuantity": 1,"orderQuantity": 1,
                              "coveredQuantity": {$subtract: ["$inventoryQuantity", "$availableQuantity"]}
                          }}
                      ,{$project: {"orderNumber": 1, "productId": 1, "inventoryQuantity": 1,"orderQuantity": 1, "coveredQuantity": 1,
                              "backorderedQuantity": {$subtract: ["$orderQuantity", "$coveredQuantity"]}
                          }}
                      ,{$match: {"backorderedQuantity": {gt: 0}}}
                  ]                                
                """;

        Gson gson = new Gson();
        var pipeline = convertString(query);

        return collection.aggregate(pipeline).into(new ArrayList<InventoryBackorderAnalysisResult>());
    }

    public List<InventoryReorderAnalysisResult> findItemsToPotentiallyReorder(Integer minimumAvailableQuantity) {
        MongoCollection<InventoryReorderAnalysisResult> collection = database.getCollection("orders", InventoryReorderAnalysisResult.class);

        List<BasicDBObject> pipeline = new ArrayList<>(
                List.of(
                        BasicDBObject.parse("""
                                {$match: {"status": "complete"}}
                                """)
                        ,BasicDBObject.parse("""
                                {$unwind: {path: "$items"}}
                                """)
                        ,BasicDBObject.parse("""
                                {$group: {_id: "$items.productId", quantity: {$sum: "$items.quantity"}, count: {$sum: 1}} }
                                """)
                        ,BasicDBObject.parse("""
                                {
                                        $lookup: {
                                            from: "inventory",
                                            localField: "_id",
                                            foreignField: "productId",
                                            pipeline: [
                                                {$unwind: {path: "$price"}}
                                            ],
                                            as: "inventoryItems"
                                        }
                                    }
                                """
                        )
                        ,BasicDBObject.parse("""
                                {$unwind: {path: "$inventoryItems"}}
                                """)
                        ,BasicDBObject.parse("""
                                {$project: {_id: 0, "productId": "$_id", "inventoryQuantity": "$inventoryItems.unitsAvailable",
                                            "orderQuantity": "$quantity", "availableQuantity": {$subtract: ["$inventoryItems.unitsAvailable", "$quantity"]}}}
                                """)
                        ,BasicDBObject.parse("{$match: {\"availableQuantity\": {$lt: " + minimumAvailableQuantity + "}}}")
                )
        );

        return collection.aggregate(pipeline).into(new ArrayList<InventoryReorderAnalysisResult>());
    }

    // M03 - Reserve on Back Order
    public long reserveOnBackOrder(Long productId, Integer unitsBackordered) {
        MongoCollection<Document> collection = database.getCollection("inventory");

        String filter = """
                {"productId": :productId},
                """;
        filter = filter.replace(":productId", productId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {$inc: {unitsBackordered: :unitsBackordered}}
                """;
        sPipeline = sPipeline.replace(":unitsOnOrder", unitsBackordered.toString());
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    // M05 - Order Supplies / Record Units on Order
    public long recordUnitsOnOrder(Long productId, Integer unitsOnOrder) {
        MongoCollection<Document> collection = database.getCollection("inventory");

        String filter = """
                {"productId": :productId},
                """;
        filter = filter.replace(":productId", productId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {$inc: {unitsBackordered: :unitsOnOrder}}
                """;
        sPipeline = sPipeline.replace(":unitsOnOrder", unitsOnOrder.toString());
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    // M05 - Order Supplies / Record Deliveries
    public long recordSupplyOrderHistory(Long productId, Integer supplyOrderQuantity) {
        MongoCollection<Document> collection = database.getCollection("inventory");

        String filter = """
                {"productId": :productId},
                """;
        filter = filter.replace(":productId", productId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {
                    $inc: {unitsAvailable: :supplyOrderQuantity},
                    $set: {unitsOnOrder: 0, updatedBy: "system", updateDate: ISODate()},
                    $push: {
                        supplyOrderHistory: {
                            quantity: :supplyOrderQuantity,
                            deliveryDate: ISODate()
                        }
                    }
                }
                """;
        sPipeline = sPipeline.replaceAll(":supplyOrderQuantity", supplyOrderQuantity.toString());
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    // M06 - Suggest Products
    // Find the customer's orders
    public List<InventorySuggestedProductsResult> getSuggestedProducts(Long customerId, Long currentProductId,
                                                                       Integer numberOfOrdersToAnalyze, Integer numberOfProductsToSuggest) {
        MongoCollection<InventorySuggestedProductsResult> collection = database.getCollection("orders", InventorySuggestedProductsResult.class);

        String query = """
                [
                 {$match: {"userId": :userId}}
                 ,{$unwind: {path: "$items"}}
                 ,{$sort: {datePurchased: -1}}
                 ,{$limit: 1}
                ,{
                   $lookup: {
                       from: "orders",
                       let: {purchasedProductId: "$productId"},
                       pipeline: [
                           {$limit: :numberOfOrdersToAnalyze}
                           // -- insert analytical logic
                       ],
                       as: "suggestedItems"
                   }
                 }
                 ,{$unwind: {path: "$suggestedItems"}}
                 ,{$unwind: {path: "$suggestedItems.items"}}
                 ,{$project: {_id: 0, "suggestedItem": "$suggestedItems.items"}}
                 ,{$match: {"suggestedItem.productId": {$ne: ":currentProductId" }}}
                 ,{$project: {_id: 0, "suggestedItem.quantity": 0}}
                 ,{$project: {"productId": "$suggestedItem.productId", "attributes": "$suggestedItem.attributes",
                                "productName": "$suggestedItem.productName", "imageUrl": "$suggestedItem.imageUrl",
                              "originalPrice": "$suggestedItem.originalPrice", "calculatedPrice": "$suggestedItem.calculatedPrice"}}
                 ,{$limit: :numberOfProductsToSuggest}
                ]                                
                 """;

        query = query.replace(":userId", customerId.toString())
                        .replace(":currentProductId", currentProductId.toString())
                .replace(":numberOfOrdersToAnalyze", numberOfOrdersToAnalyze.toString())
                .replace(":numberOfProductsToSuggest", numberOfProductsToSuggest.toString());

        Gson gson = new Gson();
        var pipeline = convertString(query);

        return collection.aggregate(pipeline).into(new ArrayList<InventorySuggestedProductsResult>());
    }

    // M07 - Remove Reservation
    public long removeReservation(Long productId, Integer unitsBackordered) {
        MongoCollection<Document> collection = database.getCollection("inventory");

        String filter = """
                {"productId": :productId},
                """;
        filter = filter.replace(":productId", productId.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {
                    $inc: {unitsBackordered: -:unitsBackordered},
                    $set: {unitsOnOrder: 0, updatedBy: "system", updateDate: ISODate()}
                }
                """;
        sPipeline = sPipeline.replaceAll(":unitsBackordered", unitsBackordered.toString());
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
    }

    // M09 - Give Bonus Points / Analysis
    public List<BonusPointAnalysisResult> determineBonusPoints(Long customerId, BigDecimal totalPriceThreshold, BigDecimal totalPriceFactor) {
        MongoCollection<BonusPointAnalysisResult> collection = database.getCollection("orders", BonusPointAnalysisResult.class);

        String query = """
                    [
                    {$match: {"userId": :userId, "status": "processed", totalPrice: {$gte: :totalPriceThreshold}}}
                    ,{$project: {_id: 0, "userId": 1, "orderNumber": 1, totalPrice: {$round: ["$totalPrice", 2]}, bonusPoints: {$round: [{$multiply: ["$totalPrice", :totalPriceFactor]}, 2]}}}
                  ]                                
                """;
        query = query.replace(":userId", customerId.toString())
                .replace(":totalPriceThreshold", totalPriceThreshold.toString())
                .replace(":totalPriceFactor", totalPriceFactor.toString());

        Gson son = new Gson();
        var pipeline = convertString(query);

        return collection.aggregate(pipeline).into(new ArrayList<BonusPointAnalysisResult>());
    }

    public long doTransaction(String productId, Integer unitsBackordered) {
        ClientSession session = mongoClient.startSession();

        try {
            session.startTransaction(TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build());
            // add logic or call here
        } catch (MongoCommandException e) {
            session.abortTransaction();
            log.info("####### ROLLBACK TRANSACTION #######");
            throw new RuntimeException(e);
        } finally {
            session.close();
            log.info("####################################\n");
        }

        return -1;
    }

    // M09 - Give Bonus Points
    public long addBonusPointsToOrder(Long orderNumber, BigDecimal bonusPointsEarned) {
        MongoCollection<Document> collection = database.getCollection("orders");

        String filter = """
                {"orderNumber": :orderId, "status": "processed"},
                """;
        filter = filter.replace(":orderId", orderNumber.toString());
        BasicDBObject query = BasicDBObject.parse(filter);

        String sPipeline = """
                {
                    $set: {"bonusPointsEarned": :bonusPointsEarned, updatedBy: "system", updateDate: ISODate()}
                }
                """;
        sPipeline = sPipeline.replace(":bonusPointsEarned", bonusPointsEarned.toString());
        BasicDBObject pipeline = BasicDBObject.parse(sPipeline);

        UpdateResult result = collection.updateOne(query, pipeline);

        return result.getModifiedCount();
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
