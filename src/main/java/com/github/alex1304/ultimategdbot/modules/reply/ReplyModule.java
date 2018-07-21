package com.github.alex1304.ultimategdbot.modules.reply;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

/**
 * A reply is an action executed by the bot whenever a user sands a message in a specific channel.
 * Unlike commands, they don't require a prefix and they can be used once for a limited time.
 *
 * @author Alex1304
 */
public class ReplyModule implements Module {
	
	private Map<String, Reply> openedReplies;
	
	@Override
	public void start() {
		this.openedReplies = new ConcurrentHashMap<>();
	}

	@Override
	public void stop() {
		for (Reply r : openedReplies.values())
			r.cancel();
		
		openedReplies.clear();
		this.openedReplies = null;
	}
	
	public void open(Reply reply, boolean retryOnFailure, boolean deleteInitialMessageAfterReply) {
		String id = toReplyID(reply);
		
		if (id == null)
			return;
				
		Procedure closeReply = () -> openedReplies.remove(id);
		
		if (deleteInitialMessageAfterReply)
			closeReply = closeReply.andThen(() -> reply.deleteInitialMessage());
		
		openedReplies.put(id, reply);
		
		reply.setOnSuccess(reply.getOnSuccess().compose(closeReply));
		reply.setOnCancel(reply.getOnCancel().compose(closeReply));
		if (!retryOnFailure)
			reply.setOnFailure(reply.getOnFailure().compose(closeReply));
		else
			reply.setOnFailure(reply.getOnFailure().compose(() -> {
				reply.cancel(false);
				reply.startTimeout();
			}));
		
		reply.startTimeout();
	}
	
	/**
	 * Gets the opened reply for this channel and user. Returns null if no reply is open
	 * 
	 * @param channel
	 * @param user
	 * @return Reply
	 */
	public Reply getReply(IChannel channel, IUser user) {
		return openedReplies.get(toReplyID(channel, user));
	}
	
	private static String toReplyID(Reply reply) {
		if (reply.getInitialMessage() == null)
			return null;
		return reply.getInitialMessage().getChannel().getStringID() + reply.getUser().getStringID();
	}
	
	private static String toReplyID(IChannel channel, IUser user) {
		return channel.getStringID() + user.getStringID();
	}
	
	public void onMessageReceived(MessageReceivedEvent event) {
		if (openedReplies == null)
			return;
		
		String id = toReplyID(event.getChannel(), event.getAuthor());
		
		if (id == null)
			return;
		
		Reply openedReply = openedReplies.get(id);
		
		if (openedReply == null)
			return;
		
		if (event.getMessage().getContent().equalsIgnoreCase("close"))
			openedReply.cancel();
		
		openedReply.handle(event.getMessage());
	}

}
