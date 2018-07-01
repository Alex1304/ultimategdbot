package com.github.alex1304.ultimategdbot.modules.reply;

import java.util.function.Predicate;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

/**
 * Represents a dialog with another user. They are specific to a channel and is designed to close
 * after a certain time of inactivity
 *
 * @author Alex1304
 */
public class Reply {

	public static final int REPLY_TIMEOUT_MILLIS = 60000;
	
	private IUser user;
	private IChannel channel;
	private long beginTimestamp;
	private Predicate<String> replyHandler;
	private Runnable onCancel;
	private boolean isCancelled;
	
	public Reply(IUser user, IChannel channel, Predicate<String> replyHandler, Runnable onCancel) {
		this.user = user;
		this.channel = channel;
		this.beginTimestamp = System.currentTimeMillis();
		this.replyHandler = replyHandler;
		this.onCancel = onCancel;
		this.isCancelled = false;
	}
	
	public Reply(IUser user, IChannel channel, Predicate<String> replyHandler) {
		this.user = user;
		this.channel = channel;
		this.beginTimestamp = System.currentTimeMillis();
		this.replyHandler = replyHandler;
		this.onCancel = () -> {};
		this.isCancelled = false;
	}
	
	/**
	 * Handles the message in order to reply
	 * 
	 * @param message - The message to reply to
	 */
	public boolean handle(String message) {
		return replyHandler.test(message);
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
	
	/**
	 * Resets the timestamp to the current time and reverts the cancelled state.
	 */
	public void resetTimeout() {
		this.beginTimestamp = System.currentTimeMillis();
		this.isCancelled = false;
	}
	
	/**
	 * Cancels the reply. Executes the onCancel action if provided
	 */
	public void cancel() {
		this.isCancelled = true;
		onCancel.run();
	}

	/**
	 * Whether the reply is cancelled
	 *
	 * @return boolean
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

}
