package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

/**
 * Broadcasts a message to a list of channels
 *
 * @author Alex1304
 */
public class MessageBroadcaster {

	private Map<IChannel, BroadcastableMessage> broadcastMap;
	private List<IChannel> channels;
	private Function<IChannel, BroadcastableMessage> messageForChannel;
	private List<IMessage> results;
	
	public MessageBroadcaster(List<IChannel> channels, Function<IChannel, BroadcastableMessage> messageForChannel) {
		this.channels = channels;
		this.messageForChannel = messageForChannel;
		this.broadcastMap = new ConcurrentHashMap<>();
		this.results = Collections.synchronizedList(new ArrayList<>());
	}
	
	public void broadcast() {
		channels.parallelStream().forEach(channel -> {
			broadcastMap.put(channel, messageForChannel.apply(channel));
		});
		
		List<Thread> tlist = new ArrayList<>();
		
		for (Entry<IChannel, BroadcastableMessage> entry : broadcastMap.entrySet()) {
			IChannel channel = entry.getKey();
			BroadcastableMessage bm = entry.getValue();
			
			Thread t = new Thread(() -> {
				results.add(RequestBuffer.request(() -> {
					try {
						return channel.sendMessage(bm.buildContent(), bm.buildEmbed());
					} catch (MissingPermissionsException | DiscordException e) {
						UltimateGDBot.logException(e);
						return null;
					}
				}).get());
			});
			
			tlist.add(t);
			t.start();
		}
		
		for (Thread t : tlist) {
			try {
				t.join();
			} catch (InterruptedException e) {
				UltimateGDBot.logException(e);
			}
		}
	}

	/**
	 * Gets the results
	 *
	 * @return List&lt;IMessage&gt;
	 */
	public List<IMessage> getResults() {
		return results;
	}
}
