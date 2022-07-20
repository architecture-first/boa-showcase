package com.architecture.first.merchant;

import com.architecture.first.framework.business.retail.model.criteria.ShowProductsCriteria;
import com.architecture.first.merchant.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@SpringBootTest
class ProductDaoTests {

    @Autowired
    InventoryRepository inventoryRepository;

    @Test
    void testShowProducts() {
        var products = inventoryRepository.getProducts(new ShowProductsCriteria("\"unitsAvailable\": {$gt: 0}"));
        Assert.notEmpty(products, "Products are empty");
    }

    @Test
    void testShowProduct() {
        var product = inventoryRepository.getProductById(1002l);
        Assert.notNull(product, "Product is empty");
    }

    @Test
    void testFindfItemsOnBackorder() {
        var data = inventoryRepository.findItemsThatAreOnBackorder();
        Assert.notNull(data, "data is null");
    }

    @Test
    void testReserveOnBackorder() {
        var results = inventoryRepository.reserveOnBackOrder(1001l, 2);
        Assert.isTrue(results > 0, "Unable to update item");
    }

    @Test
    void testFindItemsToPotentiallyReorder() {
        var results = inventoryRepository.findItemsToPotentiallyReorder(100);
        Assert.notEmpty(results, "Results are empty");
    }

    @Test
    void testRecordUnitsOnOrder() {
        var results = inventoryRepository.recordUnitsOnOrder(1001l, 2);
        Assert.isTrue(results > 0, "Unable to update item");
    }

    @Test
    void testDeliveries() {
        var results = inventoryRepository.recordSupplyOrderHistory(1001l, 2);
        Assert.isTrue(results > 0, "Unable to update item");
    }

    @Test
    void testGetSuggestedProducts() {
        var results = inventoryRepository.getSuggestedProducts(1001l, 1001l, 10, 2);
        Assert.notEmpty(results, "Results are empty");
    }

    @Test
    void testRemoveReservation() {
        var results = inventoryRepository.removeReservation(1001l, 2);
        Assert.isTrue(results > 0, "Unable to update item");
    }

    @Test
    void testDetermineBonusPoints() {
        var results = inventoryRepository.determineBonusPoints(1001l, new BigDecimal(40), new BigDecimal(0.1));
        Assert.notEmpty(results, "Results are empty");
    }

    @Test
    void testAddBonusPointsToOrder() {
        var results = inventoryRepository.addBonusPointsToOrder(100001l, new BigDecimal(5.00));
        Assert.isTrue(results > 0, "Unable to update item");
    }

}
