package com.architecture.first.framework.business.retail.model.dynamic.actors;

import com.architecture.first.framework.business.BusinessActor;
import com.architecture.first.framework.business.actors.external.behavior.script.model.PipelineEntry;
import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.retail.model.dynamic.DynamicApplication;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DynamicActor extends BusinessActor {

    @Value("${actor.dynamic.group}")
    private String groupName;

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void doMyWork() {
        super.doMyWork();
    }

    @Override
    public String group() {return groupName;}

    @Override
    protected void onTerminate(String reason) {
        super.onTerminate(reason);
        log.info("Terminating Cashier: " + name());

        DynamicApplication.stop();
    }
}
