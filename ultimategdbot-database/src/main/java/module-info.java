module ultimategdbot.database {
	requires transitive org.hibernate.orm.core;
	requires java.persistence;
	requires java.naming;
	requires java.sql;
	
	exports com.github.alex1304.ultimategdbot.database;
}