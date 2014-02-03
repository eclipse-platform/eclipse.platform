/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.core.ant;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.input.DefaultInputHandler;

public class FailInputHandler extends DefaultInputHandler {
	
	protected InputStream getInputStream() {
		//ensure any attempts to read input fail
		return new InputStream(){
			public int read() throws IOException {
				throw new IOException();
			}
		};
	}
}
