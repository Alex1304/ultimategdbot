package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RateLimitException;
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
	private ObservableList<IMessage> results;
	private Consumer<ObservableList<? extends IMessage>> onDone;
	private boolean isDone;
	
	public MessageBroadcaster(List<IChannel> channels, Function<IChannel, BroadcastableMessage> messageForChannel) {
		this.channels = channels;
		this.messageForChannel = messageForChannel;
		this.broadcastMap = new ConcurrentHashMap<>();
		this.results = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
		this.onDone = results -> {};
		this.isDone = false;
	}
	
	public void broadcast() {
		channels.forEach(channel -> {
			broadcastMap.put(channel, messageForChannel.apply(channel));
		});
		
		results.addListener((ListChangeListener.Change<? extends IMessage> event) -> {
			while(event.next()) {
				event.getAddedSubList().forEach(m -> {
					StringBuilder sb = new StringBuilder("[MessageBroadcaster] ");
					if (m == null)
						sb.append("Failed to send a message");
					else
						sb.append("Successfully sent a message to guild " + m.getGuild().getStringID());
					
					System.out.println(sb.toString());
				});
				
				if (!isDone && event.getList().size() >= broadcastMap.entrySet().size()) {
					onDone.accept(event.getList());
					this.isDone = true;
				}
			}
		});
		
		broadcastMap.entrySet().parallelStream().forEach(entry -> {
			IChannel channel = entry.getKey();
			BroadcastableMessage bm = entry.getValue();
			
			RequestBuffer.request(() -> {
				try {
					results.add(channel.sendMessage(bm.buildContent(), bm.buildEmbed()));
				} catch (Exception e) {
					if (e instanceof RateLimitException)
						throw e;
					System.err.println("[MessageBroadcaster] " + e.getClass().getName() + ": " + e.getMessage());
					results.add(null);
				} 
			});
		});
		
		/*
		List<Thread> tlist = new ArrayList<>();
		
		for (Entry<IChannel, BroadcastableMessage> entry : broadcastMap.entrySet()) {
			IChannel channel = entry.getKey();
			BroadcastableMessage bm = entry.getValue();
			
			Thread t = new Thread(() -> {
				results.add(RequestBuffer.request(() -> {
					try {
						return channel.sendMessage(bm.buildContent(), bm.buildEmbed());
					} catch (MissingPermissionsException | DiscordException e) {
						System.err.println("[MessageBroadcaster] " + e.getClass().getName() + ": " + e.getMessage());
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
		*/
	}
	
	public void setOnDone(Consumer<ObservableList<? extends IMessage>> onDone) {
		this.onDone = onDone;
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
