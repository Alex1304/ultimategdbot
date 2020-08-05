package com.github.alex1304.ultimategdbot.api.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import discord4j.common.util.Snowflake;

public final class DatabaseTest {
	
	private DatabaseService db;
	
	@BeforeEach
	public void setUp() throws Exception {
		db = DatabaseService.create(Jdbi.create("jdbc:h2:mem:test"));
		db.configureJdbi(jdbi -> {
			jdbi.registerRowMapper(BeanMapper.factory(TestBean.class));
		});
	}

	@Test
	public void testCreate() {
		db.useExtension(TestDao.class, dao -> {
			dao.createTable();
			dao.create(Snowflake.of(123));
		}).block();
	}

	@Test
	public void testGetOrCreate() {
		var bean = db.withExtension(TestDao.class, dao -> {
			dao.createTable();
			return dao.getOrCreate(Snowflake.of(123));
		}).block();
		
		assertEquals(Snowflake.of(123), bean.getGuildId());
	}

	@Test
	public void testUpdate() {
		var beanOpt = db.withExtension(TestDao.class, dao -> {
			dao.createTable();
			var id = Snowflake.of(123);
			var bean = dao.getOrCreate(id);
			bean.setMyLong(40L);
			bean.setMyString("toto");
			dao.update(bean);
			return dao.get(id);
		}).block();
		
		assertTrue(beanOpt.isPresent());
		var bean = beanOpt.orElseThrow();
		assertEquals(Snowflake.of(123), bean.getGuildId());
		assertEquals(40L, bean.getMyLong());
		assertEquals("toto", bean.getMyString());
	}

	public static class TestBean {
		
		private Snowflake guildId;
		private Long myLong;
		private String myString;
		
		public Snowflake getGuildId() {
			return guildId;
			
		}
		public void setGuildId(Snowflake guildId) {
			this.guildId = guildId;
		}
		
		public Long getMyLong() {
			return myLong;
		}
		
		public void setMyLong(Long myLong) {
			this.myLong = myLong;
		}
		
		public String getMyString() {
			return myString;
		}
		
		public void setMyString(String myString) {
			this.myString = myString;
		}
	}

	public static interface TestDao {
		
		@SqlUpdate("CREATE TABLE test ("
						+ "guildId BIGINT PRIMARY KEY,"
						+ "myLong BIGINT,"
						+ "myString VARCHAR(64)"
					+ ")")
		void createTable();
		
		@SqlUpdate("INSERT INTO test VALUES (?, NULL, NULL)")
		void create(Snowflake guildId);
		
		@SqlUpdate("UPDATE test SET myLong = :myLong, myString = :myString WHERE guildId = :guildId")
		void update(@BindBean TestBean settings);

		@SqlQuery("SELECT * FROM test WHERE guildId = ?")
		Optional<TestBean> get(Snowflake guildId);
		
		@Transaction(TransactionIsolationLevel.SERIALIZABLE)
		default TestBean getOrCreate(Snowflake guildId) {
			return get(guildId).orElseGet(() -> {
				create(guildId);
				return get(guildId).orElseThrow();
			});
		}
	}
}








