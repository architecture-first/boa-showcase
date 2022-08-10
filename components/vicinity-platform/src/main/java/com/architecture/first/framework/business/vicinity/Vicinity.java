package com.architecture.first.framework.business.vicinity;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.messages.VicinityMessage;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Vicinity {
    void onApplicationEvent(ArchitectureFirstEvent event);
    VicinityMessage generateMessage(ArchitectureFirstEvent event, String to);
    void publishMessage(String to, String contents);
    void subscribe(Actor owner, String target, BiFunction<Actor, ArchitectureFirstEvent, Void> fnCallback);
    void unsubscribe(String target);
    boolean areConnectionsOk(String target, int numberOfConnections);
    String findActor(String type, String project);
    String findActor(String type);
    boolean actorIsAvailable(String name);

}
