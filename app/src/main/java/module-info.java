import botrino.api.annotation.BotModule;
import botrino.api.extension.BotrinoExtension;
import ultimategdbot.framework.UltimateGDBotExtension;

@BotModule
open module ultimategdbot {

    provides BotrinoExtension with UltimateGDBotExtension;

    requires botrino.api;
    requires botrino.command;
    requires java.sql;
    requires jdash.events;
    requires jdash.graphics;
    requires jdk.management;
    requires org.mongodb.driver.reactivestreams;
    requires org.immutables.criteria.common;
    requires org.immutables.criteria.mongo;
    requires org.immutables.criteria.reactor;
    requires org.mongodb.bson;
    requires static org.immutables.value;
    requires java.desktop;
}