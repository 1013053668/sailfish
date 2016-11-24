/**
 *
 *	Copyright 2016-2016 spccold
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *   	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 *
 */
package sailfish.remoting.handler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import sailfish.remoting.channel.ChannelConfig;
import sailfish.remoting.channel.ChannelType;
import sailfish.remoting.constants.ChannelAttrKeys;
import sailfish.remoting.constants.Opcode;
import sailfish.remoting.constants.RemotingConstants;
import sailfish.remoting.protocol.Protocol;
import sailfish.remoting.protocol.RequestProtocol;
import sailfish.remoting.protocol.ResponseProtocol;
import sailfish.remoting.utils.ChannelUtil;

/**
 * negotiate idleTimeout, maxIdleTimeout and settings about {@link ChannelConfig} with
 * remote peer
 * 
 * @author spccold
 * @version $Id: NegotiateChannelHandler.java, v 0.1 2016年11月23日 下午10:11:26 spccold Exp $
 */
@ChannelHandler.Sharable
public class NegotiateChannelHandler extends SimpleChannelInboundHandler<Protocol> {

	private static final Logger logger = LoggerFactory.getLogger(NegotiateChannelHandler.class);
	public static final NegotiateChannelHandler INSTANCE = new NegotiateChannelHandler();
	// This way we can reduce the memory usage compared to use Attributes.
	private final ConcurrentMap<ChannelHandlerContext, Boolean> negotiateMap = PlatformDependent.newConcurrentHashMap();

	public static final ConcurrentMap<String, ChannelHandlerContexts> readWriteContexts = new ConcurrentHashMap<>();
	public static final ConcurrentMap<ChannelHandlerContext, String> context2Uuid = new ConcurrentHashMap<>();

	private NegotiateChannelHandler() { };

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (ChannelUtil.clientSide(ctx)) {
			negotiate(ctx);
		}
		ctx.fireChannelActive();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Protocol msg) throws Exception {
		if (!ChannelUtil.clientSide(ctx) && msg.request() && msg.heartbeat()) {
			dealNegotiate(ctx, msg);
			return;
		}
		//no sense to Protocol in fact
		ReferenceCountUtil.retain(msg);
		ctx.fireChannelRead(msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("Failed to negotiate. Closing: " + ctx.channel(), cause);
		ctx.close();
	}
	
	private boolean negotiate(ChannelHandlerContext ctx) throws Exception {
		if (negotiateMap.putIfAbsent(ctx, Boolean.TRUE) == null) { // Guard against re-entrance.
			try {
				int idleTimeout = ctx.channel().attr(ChannelAttrKeys.idleTimeout).get();
				int maxIdleTimeout = ctx.channel().attr(ChannelAttrKeys.maxIdleTimeout).get();
				Attribute<UUID> uuidAttr = ctx.channel().attr(ChannelAttrKeys.uuid);
				// only one byte
				byte channelType = ctx.channel().attr(ChannelAttrKeys.channelType).get();
				short connections = ctx.channel().attr(ChannelAttrKeys.connections).get();
				short channelIndex = ctx.channel().attr(ChannelAttrKeys.channelIndex).get();
				// negotiate idle timeout and read write splitting settings with remote peer
				ctx.writeAndFlush(RequestProtocol.newNegotiateHeartbeat((byte) idleTimeout, (byte) maxIdleTimeout,
						uuidAttr.get(), channelType, connections, channelIndex));
			} catch (Throwable cause) {
				exceptionCaught(ctx, cause);
			} finally {
				remove(ctx);
			}
			return true;
		}
		return false;
	}

	private void dealNegotiate(ChannelHandlerContext ctx, Protocol msg) throws Exception {
		try{
			RequestProtocol requestProtocol = (RequestProtocol) msg;
			if (requestProtocol.opcode() == Opcode.HEARTBEAT_WITH_NEGOTIATE) {// negotiate idle timeout
				byte[] body = requestProtocol.body();
				DataInputStream dis = new DataInputStream(new ByteArrayInputStream(body));
				byte idleTimeout = dis.readByte();
				byte idleMaxTimeout = dis.readByte();
				UUID uuid = new UUID(dis.readLong(), dis.readLong());
				byte channelType = dis.readByte();
				short connections = dis.readShort();
				short channelIndex = dis.readShort();
				// no sense to dis(ByteArrayInputStream) in fact
				dis.close();
				
				ChannelHandlerContext idleHandlerContext = ctx.pipeline().context(IdleStateHandler.class);
				if (null != idleHandlerContext) {
					ctx.pipeline().replace(IdleStateHandler.class, idleHandlerContext.name(),
							new IdleStateHandler(idleTimeout, 0, 0));
					ctx.channel().attr(ChannelAttrKeys.maxIdleTimeout).set(idleMaxTimeout);
				}

				if (channelType != ChannelType.readwrite.code()) {// negotiate read write splitting settings
					String uuidStr = uuid.toString();
					ChannelHandlerContexts contexts = readWriteContexts.get(uuidStr);
					if (null == contexts) {
						ChannelHandlerContexts existed = readWriteContexts.putIfAbsent(uuidStr,
								contexts = new ChannelHandlerContexts());
						if (null != existed) {
							contexts = existed;
						}
					}
					// contrary to remote peer, read to write, write to read
					if (channelType == ChannelType.write.code()) {
						contexts.addReadChannelHandlerContext(ctx, channelIndex);
					} else if(channelType == ChannelType.read.code()){
						contexts.addWriteChannelHandlerContext(ctx, channelIndex);
					}
					context2Uuid.put(ctx, uuidStr);
				}
			}
			// normal heart beat request
			ctx.writeAndFlush(ResponseProtocol.newHeartbeat());
		}catch(Exception cause){
			exceptionCaught(ctx, cause);
		}finally {
			remove(ctx);
		}
	}

	private void remove(ChannelHandlerContext ctx) {
		try {
			ChannelPipeline pipeline = ctx.pipeline();
			if (pipeline.context(this) != null) {
				pipeline.remove(this);
			}
		} finally {
			negotiateMap.remove(ctx);
		}
	}
}
