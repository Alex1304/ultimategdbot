package com.github.alex1304.ultimategdbot.modules.reply;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.api.events.EventSubscriber;
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
	private boolean isEnabled;
	
	public ReplyModule() {
		this.openedReplies = new ConcurrentHashMap<>();
	}

	@Override
	public void start() {
		isEnabled = true;
	}

	@Override
	public void stop() {
		isEnabled = false;
	}
	
	public void open(Reply reply, boolean retryOnFailure, boolean deleteInitialMessageAfterReply) {
		if (!isEnabled)
			return;
		
		String id = toReplyID(reply);
				
		Procedure closeReply = () -> openedReplies.remove(id);
		
		if (deleteInitialMessageAfterReply)
			closeReply = closeReply.andThen(() -> reply.deleteInitialMessage());
		
		Reply existingReply = openedReplies.get(id);
		if (existingReply != null)
			existingReply.cancel();
		
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
	
	private static String toReplyID(Reply reply) {
		return reply.getInitialMessage().getChannel().getStringID() + reply.getUser().getStringID();
	}
	
	private static String toReplyID(IChannel channel, IUser user) {
		return channel.getStringID() + user.getStringID();
	}
	
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!isEnabled)
			return;
		
		String id = toReplyID(event.getChannel(), event.getAuthor());
		Reply openedReply = openedReplies.get(id);
		
		if (openedReply == null)
			return;
		
		if (event.getMessage().getContent().equalsIgnoreCase("cancel"))
			openedReply.cancel();
		
		openedReply.handle(event.getMessage());
	}

}
