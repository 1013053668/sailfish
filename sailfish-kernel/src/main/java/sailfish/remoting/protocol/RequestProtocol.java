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
package sailfish.remoting.protocol;

import io.netty.buffer.ByteBuf;
import sailfish.remoting.RemotingConstants;
import sailfish.remoting.exceptions.SailfishException;

/**
 * sailfish binary request protocol
 * <pre>
 * 1-- magic(2 bytes)
 * 2-- total length(header length + body length, 4 bytes)
 * 3-- header (8 bytes)
 *    3.1-- direction + oneway or not + heartbeat request or normal request + serializeType (1 byte)
 *          --request(1)(eighth high-order bit)
 *          --oneway(1)/twoway(0)(seventh high-order bit)
 *          --heartbeat request(1)/normal request(0)(sixth high-order bit)
 *          --serializeType([0~31])(five low-order bits)
 *    3.2-- packetId (4 bytes)
 *    3.3-- opcode   (2 bytes)
 *    3.4-- compressType + langType (1 byte)
 *          --compressType(four high-order bits)
 *          --langType(four low-order bits)
 * 4-- body ((total length - header length) bytes)
 * </pre>
 * 
 * @author spccold
 * @version $Id: RequestProtocol.java, v 0.1 2016年10月11日 下午8:44:48 jileng Exp $
 */
public class RequestProtocol implements Protocol {
    private static final int HEADER_LENGTH = 8;
    public static final int REQUEST_FLAG = 0x80;
    private static final int ONEWAY_FLAG = 0x40;
    private static final int HEARTBEAT_FLAG = 0x20;
    
    
    //request direction
    private boolean          heartbeat;
    private boolean          oneway;
    private byte             serializeType;

    private int              packetId;
    private short            opcode;

    private byte             compressType;
    private byte             langType = RemotingConstants.JAVA;

    private byte[]           body;
    
    @Override
    public void serialize(ByteBuf output) throws SailfishException {
        try {
            //write magic first
            output.writeShort(RemotingConstants.SAILFISH_MAGIC);
            //write package length(not contain current length field(4 bytes))
            output.writeInt(HEADER_LENGTH + bodyLength());

            byte compactByte = (byte)REQUEST_FLAG; 
            if(this.oneway){
                compactByte = (byte)(compactByte | ONEWAY_FLAG);
            }
            if(this.heartbeat){
                compactByte = (byte)(compactByte | HEARTBEAT_FLAG);
            }
            
            output.writeByte(compactByte | serializeType);
            
            output.writeInt(packetId);
            output.writeShort(opcode);
            
            output.writeByte(compressType << 4 | langType);

            if (bodyLength() != 0) {
                output.writeBytes(this.body);
            }
        } catch (Throwable cause) {
            throw new SailfishException(cause);
        }
    }

    @Override
    public void deserialize(ByteBuf input, int totalLength) throws SailfishException {
        try {
            byte compactByte = input.readByte();
            this.oneway = ((compactByte & ONEWAY_FLAG) != 0);
            this.heartbeat =  ((compactByte & HEARTBEAT_FLAG) != 0);
            this.serializeType = (byte)(compactByte & 0x1F);
            
            this.packetId = input.readInt();
            this.opcode = input.readShort();
            
            byte tmp = input.readByte();
            this.compressType = (byte)(tmp >> 4 & 0xF);
            this.langType = (byte)(tmp >> 0 & 0xF);
            
            //read body
            int bodyLength = totalLength - HEADER_LENGTH;
            if(bodyLength > 0){
                this.body = new byte[bodyLength];
                input.readBytes(this.body);
            }
        } catch (Throwable cause) {
            throw new SailfishException(cause);
        }
    }
    
    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public boolean isOneway() {
        return oneway;
    }

    public void setOneway(boolean oneway) {
        this.oneway = oneway;
    }

    public byte getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = ProtocolParameterChecker.checkSerializeType(serializeType);
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public short getOpcode() {
        return opcode;
    }

    public void setOpcode(short opcode) {
        this.opcode = opcode;
    }

    public byte getCompressType() {
        return compressType;
    }

    public void setCompressType(byte compressType) {
        this.compressType = ProtocolParameterChecker.checkCompressType(compressType);
    }

    public byte getLangType() {
        return langType;
    }

    public void setLangType(byte langType) {
        this.langType = ProtocolParameterChecker.checkLangType(langType);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    private int bodyLength() {
        if (null == body) {
            return 0;
        }
        return body.length;
    }

    @Override
    public boolean request() {
        return true;
    }
}
