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
import java.io.IOException;
import org.eclipse.core.tools.metadata.IStringDumpingStrategy;

/**
 * A strategy for reading .location files.
 */
@SuppressWarnings("restriction")
class LocationStrategy implements IStringDumpingStrategy {

	/**
	 * @see org.eclipse.core.tools.metadata.IStringDumpingStrategy#dumpStringContents(DataInputStream)
	 */
	@Override
	public String dumpStringContents(DataInputStream dataInput) throws IOException {
		StringBuilder contents = new StringBuilder(100);
		String location = dataInput.readUTF();
		contents.append("Location: '"); //$NON-NLS-1$
		contents.append(location);
		contents.append('\'');
		//try to read the dynamic references
		int numRefs = dataInput.readInt();
		if (numRefs < 0) {
			return contents.toString();
		}
		contents.append('\n');
		contents.append("Dynamic references ("); //$NON-NLS-1$
		contents.append(numRefs);
		contents.append("): "); //$NON-NLS-1$
		for (int i = 0; i < numRefs; i++) {
			String projectName = dataInput.readUTF();
			contents.append("\n\t"); //$NON-NLS-1$
			contents.append(projectName);
		}
		return contents.toString();
	}

	/**
	 * @see org.eclipse.core.tools.metadata.IStringDumpingStrategy#getFormatDescription()
	 */
	@Override
	public String getFormatDescription() {
		return "Project location file"; //$NON-NLS-1$
	}
}
