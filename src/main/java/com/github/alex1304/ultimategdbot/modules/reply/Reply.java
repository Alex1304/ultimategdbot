package com.github.alex1304.ultimategdbot.modules.reply;

import java.util.function.Consumer;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

/**
 * Represents a dialog with another user. They are specific to a channel and is designed to close
 * after a certain time of inactivity
 *
 * @author Alex1304
 */
public class Reply {

	private static final int REPLY_TIMEOUT_MILLIS = 30000;
	
	private IUser user;
	private IChannel channel;
	private long beginTimestamp;
	private Consumer<String> replyHandler;
	
	/**
	 * @param user
	 * @param channel
	 * @param replyHandler
	 */
	public Reply(IUser user, IChannel channel, Consumer<String> replyHandler) {
		this.user = user;
		this.channel = channel;
		this.beginTimestamp = System.currentTimeMillis();
		this.replyHandler = replyHandler;
	}
	
	/**
	 * Handles the message in order to reply
	 * 
	 * @param message - The message to reply to
	 */
	public void handle(String message) {
		this.replyHandler.accept(message);
	}
	
	/**
	 * Gets the user
	 *
	 * @return IUser
	 */
	public IUser getUser() {
		return user;
	}
	/**
	 * Gets the channel
	 *
	 * @return IChannel
	 */
	public IChannel getChannel() {
		return channel;
	}
	
	/**
	 * Whether the dialog has timed out
	 * 
	 * @return boolean
	 */
	public boolean isTimedOut() {
		return System.currentTimeMillis() - beginTimestamp > REPLY_TIMEOUT_MILLIS;
	}

}
