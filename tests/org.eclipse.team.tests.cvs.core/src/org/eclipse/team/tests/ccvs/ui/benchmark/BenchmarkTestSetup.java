/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui.benchmark;


import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.Test;

import org.eclipse.core.runtime.*;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.osgi.framework.Bundle;

public class BenchmarkTestSetup extends CVSTestSetup {
	public static final File BIG_ZIP_FILE;
	public static final File SMALL_ZIP_FILE;
	public static final File TINY_ZIP_FILE;
	public static int LOOP_COUNT;

	// Static initializer for constants
	static {
		try {
			LOOP_COUNT = Integer.valueOf(System.getProperty("eclipse.cvs.loopCount", "6")).intValue();
		} catch (NumberFormatException e1) {
			LOOP_COUNT = 1;
		}
		try {
			BIG_ZIP_FILE = getTestFile("benchmarkBig.zip");
			SMALL_ZIP_FILE = getTestFile("benchmarkSmall.zip");
			TINY_ZIP_FILE = getTestFile("benchmarkTiny.zip");
		} catch (IOException e) {
			throw new Error(e.getMessage());
		}
	}
	
	public static File getTestFile(String name) throws IOException {
		Bundle b = Platform.getBundle("org.eclipse.team.tests.cvs.core");
		URL url = b.getEntry("resources/BenchmarkTest/" + name);
		url = Platform.resolve(url);
		if (url.getProtocol().equals("file")) {
			return new File(url.getFile()).getAbsoluteFile();
		}
		throw new IOException("Cannot find test file: " + name);
	}

	public BenchmarkTestSetup(Test test) {
		super(test);
	}
}
