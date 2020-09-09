package com.github.alex1304.ultimategdbot.api.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.InvalidAnnotatedObjectException;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public final class CommandProviderTest {
	
	private CommandProvider provider;
	private TestCommand testCmd;
	private TestCommandOneParam testCmd1;
	private TestCommandTwoParams testCmd2;
	private TestCommandWithSubcommand testCmdWSubcmd;
	private TestCommandOneOrTwoParams testCmd1Or2;
	private TestCommandPrimitiveParam testCmdPrim;
	private TestCommandNoSpec testCmdNoSpec;
	private TestCommandNoAlias testCmdNoAlias;
	private TestCommandNoAction testCmdNoAction;
	private TestCommandDuplicateActions testCmdDupActions;
	private TestCommandDuplicateSubcommands testCmdDupSubcmd;
	private TestCommandMissingContext testCmdMissingCtx;
	private TestCommandInvalidReturnType testCmdInvRetType;
	
	@BeforeEach
	public void setUp() throws Exception {
		provider = new CommandProvider("test", new PermissionChecker());
		testCmd = new TestCommand();
		testCmd1 = new TestCommandOneParam();
		testCmd2 = new TestCommandTwoParams();
		testCmdWSubcmd = new TestCommandWithSubcommand();
		testCmd1Or2 = new TestCommandOneOrTwoParams();
		testCmdPrim = new TestCommandPrimitiveParam();
		testCmdNoSpec = new TestCommandNoSpec();
		testCmdNoAlias = new TestCommandNoAlias();
		testCmdNoAction = new TestCommandNoAction();
		testCmdDupActions = new TestCommandDuplicateActions();
		testCmdDupSubcmd = new TestCommandDuplicateSubcommands();
		testCmdMissingCtx = new TestCommandMissingContext();
		testCmdInvRetType = new TestCommandInvalidReturnType();
	}

	@Test
	public void testAddAnnotated() {
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd), "Add valid annotated command, without params");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd1), "Add valid annotated command, with 1 param");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd2), "Add valid annotated command, with 2 params");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmdWSubcmd), "Add valid annotated command, with subcommand");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmd1Or2), "Add valid annotated command, with 1 or 2 params");
		assertDoesNotThrow(() -> provider.addAnnotated(testCmdPrim), "Add valid annotated command, with primitive param");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdNoSpec),
				"Add invalid annotated command, missing spec");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdNoAlias),
				"Add invalid annotated command, missing alias");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdNoAction),
				"Add invalid annotated command, missing action");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdDupActions),
				"Add invalid annotated command, duplicate actions");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdDupSubcmd),
				"Add invalid annotated command, duplicate subcommands");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdMissingCtx),
				"Add invalid annotated command, missing context");
		assertThrows(InvalidAnnotatedObjectException.class, () -> provider.addAnnotated(testCmdInvRetType),
				"Add invalid annotated command, invalid return type");
	}

}

@CommandDescriptor(aliases="test")
class TestCommand {
	
	@CommandAction
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandOneParam {
	
	@CommandAction
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandTwoParams {
	
	@CommandAction
	public Mono<Void> test(Context ctx, String param1, Integer param2) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandWithSubcommand {
	
	@CommandAction
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
	
	@CommandAction("a")
	public Mono<Void> test2(Context ctx) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandOneOrTwoParams {
	
	@CommandAction
	public Mono<Void> test(Context ctx, String param, @Nullable Integer param2) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandPrimitiveParam {
	
	@CommandAction
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

@CommandDescriptor(aliases = {})
class TestCommandNoAlias {
	
	@CommandAction
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandNoAction {
	
	public Mono<Void> test(Context ctx) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandDuplicateActions {
	
	@CommandAction
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
	
	@CommandAction
	public Mono<Void> test2(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandDuplicateSubcommands {
	
	@CommandAction
	public Mono<Void> test(Context ctx, String param) {
		return Mono.empty();
	}
	
	@CommandAction("a")
	public Mono<Void> test2(Context ctx, String param) {
		return Mono.empty();
	}
	
	@CommandAction("a")
	public Mono<Void> test3(Context ctx, String param) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandMissingContext {
	
	@CommandAction
	public Mono<Void> test(Integer param) {
		return Mono.empty();
	}
}

@CommandDescriptor(aliases="test")
class TestCommandInvalidReturnType {
	
	@CommandAction
	public int test(Context ctx, Integer param) {
		return 0;
	}
}