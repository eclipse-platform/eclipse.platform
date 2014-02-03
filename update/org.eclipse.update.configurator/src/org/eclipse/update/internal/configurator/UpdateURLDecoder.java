/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UpdateURLDecoder {
	static boolean init=false;
	static boolean useEnc=true;
	
	public static String decode(String s, String enc) throws UnsupportedEncodingException {
		if (!init) {
			init = true;
			try {
				return URLDecoder.decode(s, enc);
			} catch (NoSuchMethodError e) {
				useEnc=false;				
			}
		}
		
		if (useEnc) {
			return URLDecoder.decode(s, enc);
		} 			
		return URLDecoder.decode(s);
	}

}
