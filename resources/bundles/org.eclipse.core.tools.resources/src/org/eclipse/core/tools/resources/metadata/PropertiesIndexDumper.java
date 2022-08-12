/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.tools.resources.metadata;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.internal.properties.PropertyBucket;
import org.eclipse.core.tools.metadata.AbstractDumper;
import org.eclipse.core.tools.metadata.DumpException;

public class PropertiesIndexDumper extends AbstractDumper {

	@Override
	protected void dumpContents(PushbackInputStream input, StringBuilder contents) throws IOException, Exception, DumpException {
		DataInputStream source = new DataInputStream(input);
		int version = source.readByte();
		contents.append("version: ");
		contents.append(version);
		contents.append('\n');
		int entryCount = source.readInt();
		contents.append("entries: ");
		contents.append(entryCount);
		contents.append('\n');
		List<String> qualifierIndex = new ArrayList<>();
		for (int i = 0; i < entryCount; i++) {
			contents.append("Key: ");
			contents.append(source.readUTF());
			int length = source.readUnsignedShort();
			contents.append('\n');
			contents.append("Value: (");
			contents.append(length);
			contents.append(")\n");
			for (int j = 0; j < length; j++) {
				// qualifier
				byte constant = source.readByte();
				String qualifier;
				switch (constant) {
					case PropertyBucket.QNAME :
						qualifier = source.readUTF();
						qualifierIndex.add(qualifier);
						break;
					case PropertyBucket.INDEX :
						int index = source.readInt();
						if (index < 0 || index >= qualifierIndex.size())
							throw new DumpException("Invalid qualifier index: " + index + ". Table size is " + qualifierIndex.size());
						qualifier = qualifierIndex.get(index);
						break;
					default : {
						contents.append("Invalid qualifier tag: ");
						contents.append(constant);
						return;
					}
				}
				String localName = source.readUTF();
				String propertyValue = source.readUTF();
				contents.append('\t');
				contents.append(qualifier);
				contents.append(':');
				contents.append(localName);
				contents.append('=');
				contents.append(propertyValue);
				contents.append('\n');
				if (qualifier == null)
					throw new DumpException("Missing qualifier");
				if (localName == null)
					throw new DumpException("Missing local name");
				if (propertyValue == null)
					throw new DumpException("Missing property value");
			}
		}

	}

}
