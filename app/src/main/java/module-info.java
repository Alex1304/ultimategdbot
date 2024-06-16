import botrino.api.annotation.BotModule;
import botrino.api.extension.BotrinoExtension;
import ultimategdbot.framework.UltimateGDBotExtension;

@BotModule
open module ultimategdbot {

    provides BotrinoExtension with UltimateGDBotExtension;

    requires botrino.api;
    requires botrino.interaction;
    requires java.desktop;
    requires java.sql;
    requires jdash.events;
    requires jdash.graphics;
    requires jdk.management;
    requires org.mongodb.driver.reactivestreams;
    requires org.immutables.criteria.common;
    requires org.immutables.criteria.mongo;
    requires org.immutables.criteria.reactor;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires org.apache.commons.lang3;

    requires static org.immutables.value;
}