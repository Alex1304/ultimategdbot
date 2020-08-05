package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

public final class GuildConfiguratorTest {
	
	private TestBean bean1, bean2;
	private GuildConfigurator<TestBean> configurator1, configurator2;
	
	@BeforeEach
	public void setUp() throws Exception {
		bean1 = new TestBean(Snowflake.of(123), 40L, "toto");
		bean2 = new TestBean(Snowflake.of(1), null, null);
		configurator1 = createConfigurator(bean1);
		configurator2 = createConfigurator(bean2);
	}
	
	private static GuildConfigurator<TestBean> createConfigurator(TestBean b) {
		return GuildConfigurator.builder("Test", b, TestDao.class)
				.addEntry(LongConfigEntry.<TestBean>builder("mylong")
						.setValueGetter(bean -> Mono.justOrEmpty(bean.getMyLong()))
						.setValueSetter(TestBean::setMyLong)
						.setValidator(Validator.allowingAll()))
				.addEntry(StringConfigEntry.<TestBean>builder("mystring")
						.setValueGetter(bean -> Mono.justOrEmpty(bean.getMyString()))
						.setValueSetter(TestBean::setMyString)
						.setValidator(Validator.allowingIf(
								str -> str.startsWith("a"),
								"value must start with 'a'")))
				.addEntry(LongConfigEntry.builder("readonly"))
				.build();
	}

	@Test
	public void testGetEntries() {
		var entries = configurator1.getConfigEntries();
		var keys = entries.stream().map(ConfigEntry::getKey).collect(Collectors.toSet());
		assertEquals(Set.of("mylong", "mystring", "readonly"), keys);
		
		assertDoesNotThrow(() -> configurator1.getConfigEntry("mylong"));
		assertDoesNotThrow(() -> configurator1.getConfigEntry("mystring"));
		assertThrows(IllegalArgumentException.class, () -> configurator1.getConfigEntry("myglasses"));
	}
	
	@Test
	public void testManipulateValue() {
		// Testing getters in normal conditions
		
		var entry1 = configurator1.getConfigEntry("mylong");
		var entry2 = configurator2.getConfigEntry("mystring");
		var entry3 = configurator1.getConfigEntry("readonly");
		
		var value1 = entry1.getValue().block();
		var value2 = entry2.getValue().block();
		
		assertEquals(40L, value1);
		assertNull(value2);
		
		// Testing setters in normal conditions
		
		assertDoesNotThrow(() -> {
			entry1.accept(new TestVisitor<Void>() {
				@Override
				public Mono<Void> visit(LongConfigEntry entry) {
					return entry.setValue(100L);
				}
			}).block();
		});
		
		// Testing read-only state
		
		assertFalse(entry1.isReadOnly());
		assertFalse(entry2.isReadOnly());
		assertTrue(entry3.isReadOnly());
		
		assertThrows(ReadOnlyConfigEntryException.class, () -> {
			entry3.accept(new TestVisitor<Void>() {
				@Override
				public Mono<Void> visit(LongConfigEntry entry) {
					return entry.setValue(100L);
				}
			}).block();
		});
		
		// Testing validator
		
		assertDoesNotThrow(() -> {
			entry2.accept(new TestVisitor<Void>() {
				@Override
				public Mono<Void> visit(StringConfigEntry entry) {
					return entry.setValue("alice");
				}
			}).block();
		});
		
		assertThrows(ValidationException.class, () -> {
			entry2.accept(new TestVisitor<Void>() {
				@Override
				public Mono<Void> visit(StringConfigEntry entry) {
					return entry.setValue("bob");
				}
			}).block();
		});
		
		// Testing values have been set successfully
		
		var newBean = configurator1.getData();
		var newBean2 = configurator2.getData();
		assertEquals(100L, newBean.getMyLong());
		assertEquals("alice", newBean2.getMyString());
	}
	
	interface TestDao extends GuildConfigDao<TestBean> {
		// Unused 
	}

	public static class TestBean implements GuildConfigData<TestBean> {
		
		private Snowflake guildId;
		private Long myLong;
		private String myString;
		
		public TestBean() {
		}

		public TestBean(Snowflake guildId, Long myLong, String myString) {
			this.guildId = guildId;
			this.myLong = myLong;
			this.myString = myString;
		}

		@Override
		public Snowflake guildId() {
			return guildId;
		}
		
		public void setGuildId(Snowflake guildId) {
			this.guildId = guildId;
		}
		
		public Long getMyLong() {
			return myLong;
		}
		
		public TestBean setMyLong(Long myLong) {
			this.myLong = myLong;
			return this;
		}
		
		public String getMyString() {
			return myString;
		}
		
		public TestBean setMyString(String myString) {
			this.myString = myString;
			return this;
		}
	}
}

interface TestVisitor<R> extends ConfigEntryVisitor<R> {
	@Override
	default Mono<R> visit(IntegerConfigEntry entry) {
		return fail();
	}

	@Override
	default Mono<R> visit(LongConfigEntry entry) {
		return fail();
	}

	@Override
	default Mono<R> visit(BooleanConfigEntry entry) {
		return fail();
	}

	@Override
	default Mono<R> visit(StringConfigEntry entry) {
		return fail();
	}

	@Override
	default Mono<R> visit(GuildChannelConfigEntry entry) {
		return fail();
	}

	@Override
	default Mono<R> visit(GuildRoleConfigEntry entry) {
		return fail();
	}

	@Override
	default Mono<R> visit(GuildMemberConfigEntry entry) {
		return fail();
	}
}
