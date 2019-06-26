package com.github.alex1304.ultimategdbot.api.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.alex1304.ultimategdbot.api.command.argument.IntParser;
import com.github.alex1304.ultimategdbot.api.command.argument.StringParser;

import reactor.core.publisher.Mono;

class CommandProviderTest {
	
	private CommandProvider provider;
	private TestCommand testCmd;
	private TestCommandOneParam testCmd1;
	private TestCommandTwoParams testCmd2;
	private TestCommandOneOrTwoParams testCmd1Or2;
	private TestCommandPrimitiveParam testCmdPrim;
	private TestCommandNoSpec testCmdNoSpec;
	private TestCommandNoAlias testCmdNoAlias;
	private TestCommandNoAction testCmdNoAction;
	private TestCommandArgCountMismatch testCmdArgCountMismatch;
	private TestCommandArgCountMismatch2 testCmdArgCountMismatch2;
	private TestCommandIncompatibleTypes testCmdIncompTypes;
	private TestCommandDuplicateActions testCmdDupActions;
	private TestCommandConflictingActions testCmdConflActions;
	private TestCommandInvalidReturnType testCmdInvRetType;
	
	@BeforeEach
	void setUp() throws Exception {
		provider = new CommandProvider();
		testCmd = new TestCommand();
		testCmd1 = new TestCommandOneParam();
		testCmd2 = new TestCommandTwoParams();
		testCmd1Or2 = new TestCommandOneOrTwoParams();
		testCmdPrim = new TestCommandPrimitiveParam();
		testCmdNoSpec = new TestCommandNoSpec();
		testCmdNoAlias = new TestCommandNoAlias();
		testCmdNoAction = new TestCommandNoAction();
		testCmdArgCountMismatch = new TestCommandArgCountMismatch();
		testCmdArgCountMismatch2 = new TestCommandArgCountMismatch2();
		testCmdIncompTypes = new TestCommandIncompatibleTypes();
		testCmdDupActions = new TestCommandDuplicateActions();
		testCmdConflActions = new TestCommandConflictingActions();
		testCmdInvRetType = new TestCommandInvalidReturnType();
	}

	@Test
	void testAddAnnotated() {
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd), "Add valid annotated command, without params");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd1), "Add valid annotated command, with 1 param");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd2), "Add valid annotated command, with 2 params");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd1Or2), "Add valid annotated command, with 1 or 2 params");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmdPrim), "Add valid annotated command, with primitive param");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdNoSpec),
				"Add invalid annotated command, missing spec");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdNoAlias),
				"Add invalid annotated command, missing alias");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdNoAction),
				"Add invalid annotated command, missing action");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdArgCountMismatch),
				"Add invalid annotated command, too few arg parsers");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdArgCountMismatch2),
				"Add invalid annotated command, too many arg parsers");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdIncompTypes),
				"Add invalid annotated command, incompatible types");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdDupActions),
				"Add invalid annotated command, duplicate actions");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdConflActions),
				"Add invalid annotated command, conflicting actions");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdInvRetType),
				"Add invalid annotated command, invalid return type");
	}

}

@CommandSpec(aliases="test")
class TestCommand {
	
	@CommandAction
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandOneParam {
	
	@CommandAction(StringParser.class)
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandTwoParams {
	
	@CommandAction({ StringParser.class, IntParser.class })
	public Mono<Void> test(Context ctx, String param1, Integer param2) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandOneOrTwoParams {
	
	@CommandAction(StringParser.class)
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
	
	@CommandAction({ StringParser.class, IntParser.class })
	public Mono<Void> test(Context ctx, String param1, Integer param2) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandPrimitiveParam {
	
	@CommandAction(IntParser.class)
	public Mono<Void> test(Context ctx, int param) {
		return Mono.empty();
	}
}

class TestCommandNoSpec {
	
	@CommandAction
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandSpec(aliases = {})
class TestCommandNoAlias {
	
	@CommandAction
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandNoAction {
	
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandArgCountMismatch {
	
	@CommandAction
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandArgCountMismatch2 {
	
	@CommandAction({ StringParser.class, IntParser.class })
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandIncompatibleTypes {
	
	@CommandAction(StringParser.class)
	public Mono<Void> test(Context ctx, Integer param) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandDuplicateActions {
	
	@CommandAction(StringParser.class)
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
	
	@CommandAction(StringParser.class)
	public Mono<Void> test2(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandSpec(aliases="test")
class TestCommandConflictingActions {
	
	@CommandAction(IntParser.class)
	public Mono<Void> test(Context ctx, Integer param) {
		return Mono.empty();
	}
	
	@CommandAction(IntParser.class)
	public Mono<Void> test(Context ctx, int param) {
		return Mono.empty();
	}
}


@CommandSpec(aliases="test")
class TestCommandInvalidReturnType {
	
	@CommandAction(IntParser.class)
	public int test(Context ctx, Integer param) {
		return 0;
	}
}