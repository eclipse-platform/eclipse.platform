/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.jarprocessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamProcessor extends Thread {
	public static final String STDERR = "STDERR"; //$NON-NLS-1$
	public static final String STDOUT = "STDOUT"; //$NON-NLS-1$

	private InputStream inputStream;
	private String name;
	private boolean verbose;

	public StreamProcessor(InputStream is, String name, boolean verbose) {
		this.inputStream = is;
		this.name = name;
		this.verbose = verbose;
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(inputStream);
			BufferedReader br = new BufferedReader(isr);
			while (true) {
				String s = br.readLine();
				if (s == null) {
					break;
				}
				if (verbose) {
					if (STDERR.equals(name))
						System.err.println(name + ": " + s); //$NON-NLS-1$
					else
						System.out.println(name + ": " + s); //$NON-NLS-1$
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
