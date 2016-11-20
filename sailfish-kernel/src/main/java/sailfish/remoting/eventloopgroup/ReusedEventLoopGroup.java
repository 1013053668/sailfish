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
package sailfish.remoting.eventloopgroup;

import io.netty.channel.EventLoopGroup;

/**
 * @author spccold
 * @version $Id: ReusedEventLoopGroup.java, v 0.1 2016年11月20日 下午7:26:51 spccold Exp $
 */
public interface ReusedEventLoopGroup {
	EventLoopGroup get();
	void destory();
}
