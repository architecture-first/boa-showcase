package com.architecture.first.framework.business.vicinity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Vicinity.class)
public class VicinityTests {
    @Autowired
    Vicinity vicinity;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Test
    public void testFindActor() {
        String actorName = vicinity.findActor("Merchant");
        Assert.assertNotNull(actorName);
    }
}
