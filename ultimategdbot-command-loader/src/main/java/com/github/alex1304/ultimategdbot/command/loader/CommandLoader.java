package com.github.alex1304.ultimategdbot.command.loader;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * This loader is an agent that loads jar files dynamically during runtime.
 * This allows to add and remove implementations of Discord commands from the bot
 * without having to restart it. 
 *
 * @author Alex1304
 *
 */
public class CommandLoader {
	
	public static void agentmain(String args, Instrumentation instrumentation) throws IOException {
        instrumentation.appendToSystemClassLoaderSearch(new JarFile(args));
    }
}
