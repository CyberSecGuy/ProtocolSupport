package protocolsupport.protocol.packet.handler;

import java.text.MessageFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFutureListener;
import protocolsupport.ProtocolSupport;
import protocolsupport.api.events.ServerPingResponseEvent;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.utils.pingresponse.PingResponseHandlerProvider;
import protocolsupport.utils.JavaSystemProperty;
import protocolsupport.zplatform.ServerPlatform;
import protocolsupport.zplatform.network.NetworkManagerWrapper;
import protocolsupportbuildprocessor.Preload;

@Preload
public abstract class AbstractStatusListener {

	private static final int statusThreads = JavaSystemProperty.getValue("statusthreads", 2, Integer::parseInt);
	private static final int statusThreadKeepAlive = JavaSystemProperty.getValue("statusthreadskeepalive", 60, Integer::parseInt);

	static {
		ProtocolSupport.logInfo(MessageFormat.format("Status threads max count: {0}, keep alive time: {1}", statusThreads, statusThreadKeepAlive));
	}

	protected static final Executor statusprocessor = new ThreadPoolExecutor(
		1, statusThreads,
		statusThreadKeepAlive, TimeUnit.SECONDS,
		new LinkedBlockingQueue<Runnable>(),
		r -> new Thread(r, "StatusProcessingThread")
	);

	protected final NetworkManagerWrapper networkManager;
	public AbstractStatusListener(NetworkManagerWrapper networkmanager) {
		this.networkManager = networkmanager;
	}

	private boolean sentInfo = false;

	public void handleStatusRequest() {
		if (sentInfo) {
			networkManager.close("Status request has already been handled.");
		}
		sentInfo = true;

		statusprocessor.execute(() -> {
			ServerPingResponseEvent revent = PingResponseHandlerProvider.get().createResponse(ConnectionImpl.getFromChannel(networkManager.getChannel()));
			networkManager.sendPacket(ServerPlatform.get().getPacketFactory().createStausServerInfoPacket(
				revent.getPlayers(), revent.getProtocolInfo(),
				revent.getIcon(), revent.getMotd(),
				revent.getOnlinePlayers(), revent.getMaxPlayers()
			));
		});
	}

	public void handlePing(long pingId) {
		networkManager.sendPacket(ServerPlatform.get().getPacketFactory().createStatusPongPacket(pingId), ChannelFutureListener.CLOSE);
	}

}
