package com.github.alex1304.ultimategdbot.modules.reply;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.modules.Module;

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
	
	public void open(Reply reply) {
		this.openedReplies.put(toReplyID(reply), reply);
	}
	
	private static String toReplyID(Reply reply) {
		return reply.getChannel().getStringID() + reply.getUser().getStringID();
	}
	
	private static String toReplyID(IChannel channel, IUser user) {
		return channel.getStringID() + user.getStringID();
	}
	
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!isEnabled)
			return;
		
		String id = toReplyID(event.getChannel(), event.getAuthor());
		Reply openedReply = openedReplies.remove(id);
		
		if (openedReply == null || openedReply.isTimedOut())
			return;
		
		openedReply.handle(event.getMessage().getContent());
	}

}
