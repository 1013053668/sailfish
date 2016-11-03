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
package sailfish.remoting;

/**
 * 
 * @author spccold
 * @version $Id: RequestControl.java, v 0.1 2016年11月1日 下午2:24:47 jileng Exp $
 */
public class RequestControl {
    //request timeout in milliseconds, for callback invoke
    private int timeout;
    private short opcode;
    private byte serializeType;
    private byte compressType;
    
    public int timeout() {
        return timeout;
    }
    public void timeout(int timeout) {
        this.timeout = timeout;
    }
    
    public short opcode() {
        return opcode;
    }
    public void opcode(short opcode) {
        this.opcode = opcode;
    }
    public byte serializeType() {
        return serializeType;
    }
    public void serializeType(byte serializeType) {
        this.serializeType = serializeType;
    }
    public byte compressType() {
        return compressType;
    }
    public void compressType(byte compressType) {
        this.compressType = compressType;
    }
}