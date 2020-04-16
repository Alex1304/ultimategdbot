package com.github.alex1304.ultimategdbot.core.database;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface BotAdminDao {
	
	@SqlQuery("SELECT * FROM bot_admin")
	List<Long> getAll();
	
	@SqlQuery("SELECT * FROM bot_admin WHERE user_id = ?")
	Optional<Long> get(long id);

	@SqlUpdate("INSERT INTO bot_admin VALUES (?)")
	void insert(long id);
	
	@SqlUpdate("DELETE FROM bot_admin WHERE user_id = ?")
	boolean delete(long id);
	
	@Transaction(TransactionIsolationLevel.SERIALIZABLE)
	default boolean insertIfNotExists(long id) {
		return get(id).map(__ -> false).orElseGet(() -> {
			insert(id);
			return true;
		});
	}
}
