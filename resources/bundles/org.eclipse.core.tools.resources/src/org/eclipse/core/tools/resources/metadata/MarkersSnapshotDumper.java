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
import org.eclipse.core.tools.metadata.*;

/**
 * A dumper for .markers.snap files.
 *
 * @see org.eclipse.core.tools.metadata.AbstractDumper
 * @see org.eclipse.core.tools.resources.metadata.MarkersSnapshotDumpingStrategy_1
 * @see org.eclipse.core.tools.resources.metadata.MarkersSnapshotDumpingStrategy_2
 */
public class MarkersSnapshotDumper extends MultiStrategyDumper {

	// type constants
	static final byte INDEX = 1;
	static final byte QNAME = 2;

	// marker attribute types
	static final byte ATTRIBUTE_NULL = 0;
	static final byte ATTRIBUTE_BOOLEAN = 1;
	static final byte ATTRIBUTE_INTEGER = 2;
	static final byte ATTRIBUTE_STRING = 3;

	/**
	 * @see org.eclipse.core.tools.metadata.MultiStrategyDumper#getStringDumpingStrategy(java.io.DataInputStream)
	 */
	@Override
	protected IStringDumpingStrategy getStringDumpingStrategy(DataInputStream dataInput) throws Exception {
		int versionId;
		IStringDumpingStrategy strategy;
		versionId = dataInput.readInt();
		switch (versionId) {
			case 1 :
				strategy = new MarkersSnapshotDumpingStrategy_1();
				break;
			case 2 :
				strategy = new MarkersSnapshotDumpingStrategy_2();
				break;
			default :
				throw new DumpException("Unknown markers snapshot file version: " + versionId); //$NON-NLS-1$
		}
		return strategy;
	}

	/**
	 * @see org.eclipse.core.tools.metadata.AbstractDumper#openInputStream(java.io.File)
	 */
	@Override
	protected InputStream openInputStream(File file) throws IOException {
		return new SafeChunkyInputStream(file);
	}

}
