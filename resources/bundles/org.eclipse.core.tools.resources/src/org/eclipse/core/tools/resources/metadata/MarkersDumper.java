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

import java.io.DataInputStream;
import org.eclipse.core.tools.metadata.*;

/**
 * A dumper for .markers files.
 *
 * @see org.eclipse.core.tools.metadata.AbstractDumper
 * @see org.eclipse.core.tools.resources.metadata.MarkersDumpingStrategy_1
 * @see org.eclipse.core.tools.resources.metadata.MarkersDumpingStrategy_2
 * @see org.eclipse.core.tools.resources.metadata.MarkersDumpingStrategy_3
 */
@SuppressWarnings("restriction")
public class MarkersDumper extends MultiStrategyDumper {

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
		int versionId = dataInput.readInt();
		IStringDumpingStrategy strategy;
		switch (versionId) {
			case 1 :
				strategy = new MarkersDumpingStrategy_1();
				break;
			case 2 :
				strategy = new MarkersDumpingStrategy_2();
				break;
			case 3 :
				strategy = new MarkersDumpingStrategy_3();
				break;
			default :
				throw new DumpException("Unknown markers file version: " + versionId); //$NON-NLS-1$
		}
		return strategy;
	}
}
