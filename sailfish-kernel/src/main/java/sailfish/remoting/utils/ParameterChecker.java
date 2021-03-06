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
package sailfish.remoting.utils;

/**
 * 
 * @author spccold
 * @version $Id: ParameterChecker.java, v 0.1 2016年10月26日 下午11:15:06 jileng Exp $
 */
public class ParameterChecker {
    
    public static <T> T checkNotNull(T reference, String hint) {
        if (reference == null) {
            throw new NullPointerException(hint);
        }
        return reference;
    }
    
    public static String checkNotBlank(String content, String hint){
        if(StrUtils.isBlank(content)){
            throw new IllegalArgumentException(hint);
        }
        return content;
    }
    
    public static int checkNotNegative(int number, String hint){
        if(number < 0){
            throw new IllegalArgumentException(hint + ": " + number + " (expected: >= 0)");
        }
        return number;
    }
    
    public static int checkPositive(int number, String hint){
        if(number <= 0){
            throw new IllegalArgumentException(hint + ": " + number + " (expected: > 0)");
        }
        return number;
    }

    public static short checkPositive(short number, String hint){
    	if(number <= 0){
    		throw new IllegalArgumentException(hint + ": " + number + " (expected: > 0)");
    	}
    	return number;
    }

    public static long checkPositive(long number, String hint){
        if(number <= 0){
            throw new IllegalArgumentException(hint + ": " + number + " (expected: > 0)");
        }
        return number;
    }
    
    public static int checkBytePositive(int number){
        if(number <= 0 || number > 0x7F){
            throw new IllegalArgumentException("number: " + number + " (expected: 0 < number <= 0x7F)");
        }
        return number;
    }
}
