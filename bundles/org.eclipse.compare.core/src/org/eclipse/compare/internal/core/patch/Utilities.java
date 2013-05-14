/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core.patch;

import java.io.InputStreamReader;

public class Utilities {

	public static String getCharset(Object resource) {
		if (resource instanceof InputStreamReader) {
			return ((InputStreamReader) resource).getEncoding();
		}
		return null;
	}
}
