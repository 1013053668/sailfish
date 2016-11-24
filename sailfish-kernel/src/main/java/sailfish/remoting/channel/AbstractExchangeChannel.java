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
package sailfish.remoting.channel;

import java.net.SocketAddress;
import java.util.UUID;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import sailfish.remoting.RequestControl;
import sailfish.remoting.ResponseCallback;
import sailfish.remoting.Tracer;
import sailfish.remoting.constants.RemotingConstants;
import sailfish.remoting.exceptions.ExceptionCode;
import sailfish.remoting.exceptions.SailfishException;
import sailfish.remoting.future.BytesResponseFuture;
import sailfish.remoting.future.ResponseFuture;
import sailfish.remoting.protocol.RequestProtocol;
import sailfish.remoting.protocol.ResponseProtocol;
import sailfish.remoting.utils.StrUtils;

/**
 * @author spccold
 * @version $Id: AbstractExchangeChannel.java, v 0.1 2016年11月21日 下午10:49:12 spccold Exp $
 */
public abstract class AbstractExchangeChannel implements ExchangeChannel {
	private final UUID id;
	/** underlying channel */
	protected volatile Channel channel;
	protected volatile boolean closed = false;
	
	private final ExchangeChannelGroup parent;
	protected final SocketAddress remoteAddress;

	protected AbstractExchangeChannel(ExchangeChannelGroup parent, SocketAddress remoteAddress, UUID id) {
		this.id = id;
		this.parent = parent;
		this.remoteAddress = remoteAddress;
	}

	@Override
	public ExchangeChannelGroup parent() {
		return parent;
	}

	@Override
	public ExchangeChannel next() {
		return this;
	}

	@Override
	public UUID id() {
		return id;
	}

	@Override
	public SocketAddress localAddress() {
		if(null == channel){
			return null;
		}
		return channel.localAddress();
	}

	@Override
	public SocketAddress remoteAdress() {
		if(null == channel){
			return remoteAddress;
		}
		return channel.remoteAddress();
	}
	
	@Override
	public boolean isAvailable() {
		return null != channel && channel.isOpen() && channel.isActive();
	}

	@Override
	public void close() {
		close(0);
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public void oneway(byte[] data, RequestControl requestControl) throws SailfishException {
		RequestProtocol protocol = RequestProtocol.newRequest(requestControl);
		protocol.oneway(true);
		protocol.body(data);
		try {
			if (requestControl.sent()) {
				// TODO write or writeAndFlush?
				ChannelFuture future = channel.writeAndFlush(protocol);
				boolean ret = future.await(requestControl.timeout());
				if (!ret) {
					future.cancel(true);
					throw new SailfishException(ExceptionCode.TIMEOUT, "oneway request timeout");
				}
				return;
			}
			// reduce memory consumption
			channel.writeAndFlush(protocol, channel.voidPromise());
		} catch (InterruptedException cause) {
			throw new SailfishException(ExceptionCode.INTERRUPTED, "interrupted exceptions");
		}
	}

	@Override
	public ResponseFuture<byte[]> request(byte[] data, RequestControl requestControl) throws SailfishException {
		return requestWithFuture(data, null, requestControl);
	}

	@Override
	public void request(byte[] data, ResponseCallback<byte[]> callback, RequestControl requestControl)
			throws SailfishException {
		requestWithFuture(data, callback, requestControl);
	}

	private ResponseFuture<byte[]> requestWithFuture(byte[] data, ResponseCallback<byte[]> callback,
			RequestControl requestControl) throws SailfishException {
		final RequestProtocol protocol = RequestProtocol.newRequest(requestControl);
		protocol.oneway(false);
		protocol.body(data);

		ResponseFuture<byte[]> respFuture = new BytesResponseFuture(protocol.packetId());
		respFuture.setCallback(callback, requestControl.timeout());
		// trace before write
		Tracer.trace(this, protocol.packetId(), respFuture);
		try {
			if (requestControl.sent()) {
				ChannelFuture future = channel.writeAndFlush(protocol)
						.addListener(new SimpleChannelFutureListener(protocol.packetId()));
				boolean ret = future.await(requestControl.timeout());
				if (!ret) {
					future.cancel(true);
					throw new SailfishException(ExceptionCode.TIMEOUT, "oneway request timeout");
				}
				return respFuture;
			}
			channel.writeAndFlush(protocol, channel.voidPromise());
		} catch (InterruptedException cause) {
			throw new SailfishException(ExceptionCode.INTERRUPTED, "interrupted exceptions");
		}
		return respFuture;
	}

	// reduce class create
	static class SimpleChannelFutureListener implements ChannelFutureListener {
		private int packetId;

		public SimpleChannelFutureListener(int packetId) {
			this.packetId = packetId;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (!future.isSuccess()) {
				String errorMsg = "write fail!";
				if (null != future.cause()) {
					errorMsg = StrUtils.exception2String(future.cause());
				}
				// FIXME maybe need more concrete error, like
				// WriteOverFlowException or some other special exceptions
				Tracer.erase(ResponseProtocol.newErrorResponse(packetId, errorMsg, RemotingConstants.RESULT_FAIL));
			}
		}
	}
}
