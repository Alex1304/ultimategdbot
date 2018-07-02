package com.github.alex1304.ultimategdbot.modules.reply;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;

/**
 * Allows users to reply to a message sent by the bot. They are specific to a channel and is designed to close
 * after a certain time of inactivity
 *
 * @author Alex1304
 */
public class Reply {

	public static final long DEFAULT_TIMEOUT_MILLIS = 60000;
	
	private IMessage initialMessage;
	private IUser user;
	private Predicate<IMessage> replyHandler;
	private long timeout;
	private Procedure onSuccess, onFailure, onCancel;
	private Timer timer;
	
	/**
	 * @param initialMessage
	 *            - The message initially sent by the bot to ask the user to
	 *            reply
	 * @param user
	 *            - the user who is supposed to reply
	 * @param replyHandler
	 *            - Executes what should happen when the user replies. The
	 *            predicate should return false if the bot received an
	 *            unexpected reply, true otherwise
	 * @param timeout
	 *            - delay given to the user to reply timeout
	 */
	public Reply(IMessage initialMessage, IUser user, Predicate<IMessage> replyHandler, long timeout) {
		this.initialMessage = initialMessage;
		this.user = user;
		this.replyHandler = replyHandler;
		this.timeout = timeout;
		this.onSuccess = () -> {};
		this.onFailure = () -> {};
		this.onCancel = () -> {};
		this.timer = null;
	}
	
	/**
	 * @param initialMessage
	 *            - The message initially sent by the bot to ask the user to
	 *            reply
	 * @param user
	 *            - the user who is supposed to reply
	 * @param replyHandler
	 *            - Executes what should happen when the user replies. The
	 *            predicate should return false if the bot received an
	 *            unexpected reply, true otherwise
	 */
	public Reply(IMessage initialMessage, IUser user, Predicate<IMessage> replyHandler) {
		this(initialMessage, user, replyHandler, DEFAULT_TIMEOUT_MILLIS);
	}
	
	/**
	 * Starts the timeout, in other words it schedules the deletion of the
	 * intial message. The timer is cancelled when either {@link Reply#cancel()}
	 * or {@link Reply#handle(IMessage)} is called
	 */
	public synchronized void startTimeout() {
		if (timer != null)
			return;
		
		this.timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				Reply.this.cancel();
			}
			
		}, timeout);
	}
	
	/**
	 * Cancels the reply. Equivalent to {@code cancel(true)}
	 */
	public synchronized void cancel() {
		this.cancel(true);
	}
	
	/**
	 * Cancels the reply.
	 * 
	 * @param runOnCancel - Whether to run the onCancel procedure
	 */
	public synchronized void cancel(boolean runOnCancel) {
		if (timer == null)
			return;
		
		timer.cancel();
		this.timer = null;
		
		if (runOnCancel)
			onCancel.run();
	}
	
	/**
	 * Handles the reply given by the user. Executes onSuccess and onFailure accordingly.
	 * The reply is no longer opened after the reply has been handled.
	 * 
	 * @param message
	 */
	public synchronized void handle(IMessage message) {
		this.cancel(false);
		
		if (replyHandler.test(message))
			onSuccess.run();
		else
			onFailure.run();
	}
	
	/**
	 * Deletes the initial message. Doesn't do anything if message is already deleted
	 * or if deleteOnCancel is set to false
	 */
	public void deleteInitialMessage() {
		try {
			if (!initialMessage.isDeleted())
				initialMessage.delete();
		} catch (DiscordException e) {
			return;
		}
	}
	
	private Procedure emptyProcedureIfNull(Procedure p) {
		return p == null ? () -> {} : p;
	}

	/**
	 * Gets the initialMessage
	 *
	 * @return IMessage
	 */
	public IMessage getInitialMessage() {
		return initialMessage;
	}

	/**
	 * Sets the initialMessage
	 *
	 * @param initialMessage - IMessage
	 */
	public void setInitialMessage(IMessage initialMessage) {
		this.initialMessage = initialMessage;
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
	 * Sets the user
	 *
	 * @param user - IUser
	 */
	public void setUser(IUser user) {
		this.user = user;
	}

	/**
	 * Gets the replyHandler
	 *
	 * @return Predicate<IMessage>
	 */
	public Predicate<IMessage> getReplyHandler() {
		return replyHandler;
	}

	/**
	 * Sets the replyHandler
	 *
	 * @param replyHandler - Predicate<IMessage>
	 */
	public void setReplyHandler(Predicate<IMessage> replyHandler) {
		this.replyHandler = replyHandler;
	}

	/**
	 * Gets the timeout
	 *
	 * @return long
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timeout
	 *
	 * @param timeout - long
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Gets the onSuccess
	 *
	 * @return Procedure
	 */
	public Procedure getOnSuccess() {
		return onSuccess;
	}

	/**
	 * Sets the onSuccess. Won't do anything if the timeout is running
	 *
	 * @param onSuccess - Procedure
	 */
	public void setOnSuccess(Procedure onSuccess) {
		if (timer == null)
			this.onSuccess = emptyProcedureIfNull(onSuccess);
	}

	/**
	 * Gets the onFailure
	 *
	 * @return Procedure
	 */
	public Procedure getOnFailure() {
		return onFailure;
	}

	/**
	 * Sets the onFailure. Won't do anything if the timeout is running
	 *
	 * @param onFailure - Procedure
	 */
	public void setOnFailure(Procedure onFailure) {
		if (timer == null)
			this.onFailure = emptyProcedureIfNull(onFailure);
	}

	/**
	 * Gets the onCancel
	 *
	 * @return Procedure
	 */
	public Procedure getOnCancel() {
		return onCancel;
	}

	/**
	 * Sets the onCancel. Won't do anything if the timeout is running
	 *
	 * @param onCancel - Procedure
	 */
	public void setOnCancel(Procedure onCancel) {
		if (timer == null)
			this.onCancel = emptyProcedureIfNull(onCancel);
	}
	
}
