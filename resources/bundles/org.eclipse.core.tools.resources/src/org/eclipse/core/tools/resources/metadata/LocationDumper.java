/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources.metadata;

import java.io.*;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.tools.metadata.IStringDumpingStrategy;
import org.eclipse.core.tools.metadata.MultiStrategyDumper;

/**
 * A dumper for .location files.
 *
 * @see org.eclipse.core.tools.metadata.AbstractDumper
 * @see org.eclipse.core.tools.resources.metadata.LocationStrategy
 */
public class LocationDumper extends MultiStrategyDumper {

	/**
	 * @see org.eclipse.core.tools.metadata.MultiStrategyDumper#getStringDumpingStrategy(java.io.DataInputStream)
	 */
	@Override
	protected IStringDumpingStrategy getStringDumpingStrategy(DataInputStream input) throws Exception {
		return new LocationStrategy();
	}

	/**
	 * @see org.eclipse.core.tools.metadata.AbstractDumper#openInputStream(java.io.File)
	 */
	@Override
	protected InputStream openInputStream(File file) throws IOException {
		return new SafeChunkyInputStream(file);
	}

}
