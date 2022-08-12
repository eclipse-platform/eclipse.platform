/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.properties;

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.localstore.Bucket;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class PropertyBucket extends Bucket {
	public static class PropertyEntry extends Entry {

		private final static Comparator<String[]> COMPARATOR = (o1, o2) -> {
			int qualifierComparison = o1[0].compareTo(o2[0]);
			return qualifierComparison != 0 ? qualifierComparison : o1[1].compareTo(o2[1]);
		};
		private static final String[][] EMPTY_DATA = new String[0][];
		/**
		 * value is an array of qualified-key value pairs String[][] of
		 * {{propertyKey.qualifier, propertyKey.localName, propertyValue}}
		 */
		private String[][] value;

		/**
		 * Deletes the property with the given name, and returns the result array. Returns the original
		 * array if the property to be deleted could not be found. Returns <code>null</code> if the property was found
		 * and the original array had size 1 (instead of a zero-length array).
		 */
		static String[][] delete(String[][] existing, QualifiedName propertyName) {
			// a size-1 array is a special case
			if (existing.length == 1)
				return (existing[0][0].equals(propertyName.getQualifier()) && existing[0][1].equals(propertyName.getLocalName())) ? null : existing;
			// find the guy to delete
			int deletePosition = search(existing, propertyName);
			if (deletePosition < 0)
				// not found, nothing to delete
				return existing;
			String[][] newValue = new String[existing.length - 1][];
			if (deletePosition > 0)
				// copy elements preceding the one to be removed
				System.arraycopy(existing, 0, newValue, 0, deletePosition);
			if (deletePosition < existing.length - 1)
				// copy elements succeeding the one to be removed
				System.arraycopy(existing, deletePosition + 1, newValue, deletePosition, newValue.length - deletePosition);
			return newValue;
		}

		static String[][] insert(String[][] existing, QualifiedName propertyName, String propertyValue) {
			// look for the right spot where to insert the new guy
			int index = search(existing, propertyName);
			if (index >= 0) {
				// found existing occurrence - just replace the value
				existing[index][2] = propertyValue;
				return existing;
			}
			// not found - insert
			int insertPosition = -index - 1;
			String[][] newValue = new String[existing.length + 1][];
			if (insertPosition > 0)
				System.arraycopy(existing, 0, newValue, 0, insertPosition);
			newValue[insertPosition] = new String[] {propertyName.getQualifier(), propertyName.getLocalName(), propertyValue};
			if (insertPosition < existing.length)
				System.arraycopy(existing, insertPosition, newValue, insertPosition + 1, existing.length - insertPosition);
			return newValue;
		}

		/**
		 * Merges two entries (are always sorted). Duplicated additions replace existing ones.
		 */
		static Object merge(String[][] base, String[][] additions) {
			int additionPointer = 0;
			int basePointer = 0;
			int added = 0;
			String[][] result = new String[base.length + additions.length][];
			while (basePointer < base.length && additionPointer < additions.length) {
				int comparison = COMPARATOR.compare(base[basePointer], additions[additionPointer]);
				if (comparison == 0) {
					result[added++] = additions[additionPointer++];
					// duplicate, override
					basePointer++;
				} else if (comparison < 0)
					result[added++] = base[basePointer++];
				else
					result[added++] = additions[additionPointer++];
			}
			// copy the remaining states from either additions or base arrays
			String[][] remaining = basePointer == base.length ? additions : base;
			int remainingPointer = basePointer == base.length ? additionPointer : basePointer;
			int remainingCount = remaining.length - remainingPointer;
			System.arraycopy(remaining, remainingPointer, result, added, remainingCount);
			added += remainingCount;
			if (added == base.length + additions.length)
				// no collisions
				return result;
			// there were collisions, need to compact
			String[][] finalResult = new String[added][];
			System.arraycopy(result, 0, finalResult, 0, finalResult.length);
			return finalResult;
		}

		private static int search(String[][] existing, QualifiedName propertyName) {
			return Arrays.binarySearch(existing, new String[] {propertyName.getQualifier(), propertyName.getLocalName(), null}, COMPARATOR);
		}

		public PropertyEntry(IPath path, PropertyEntry base) {
			super(path);
			//copy 2-dimensional array [x][y]
			int xLen = base.value.length;
			this.value = new String[xLen][];
			for (int i = 0; i < xLen; i++) {
				int yLen = base.value[i].length;
				this.value[i] = new String[yLen];
				System.arraycopy(base.value[i], 0, value[i], 0, yLen);
			}
		}

		/**
		 * @param path  a path to identify this property entry
		 * @param value an array of qualified-key value pairs
		 *              (<code>String[][] {{propertyKey.qualifier, propertyKey.localName, propertyValue}}</code>)
		 */
		protected PropertyEntry(IPath path, String[][] value) {
			super(path);
			this.value = value;
		}

		/**
		 * Compacts the data array removing any null slots. If non-null slots
		 * are found, the entry is marked for removal.
		 */
		private void compact() {
			if (!isDirty())
				return;
			int occurrences = 0;
			for (String[] s : value) {
				if (s != null) {
					value[occurrences++] = s;
				}
			}
			if (occurrences == value.length)
				// no states deleted
				return;
			if (occurrences == 0) {
				// no states remaining
				value = EMPTY_DATA;
				delete();
				return;
			}
			String[][] result = new String[occurrences][];
			System.arraycopy(value, 0, result, 0, occurrences);
			value = result;
		}

		@Override
		public int getOccurrences() {
			return value == null ? 0 : value.length;
		}

		public String getProperty(QualifiedName name) {
			int index = search(value, name);
			return index < 0 ? null : value[index][2];
		}

		public QualifiedName getPropertyName(int i) {
			return new QualifiedName(this.value[i][0], this.value[i][1]);
		}

		public String getPropertyValue(int i) {
			return this.value[i][2];
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public void visited() {
			compact();
		}
	}

	public static final byte INDEX = 1;

	public static final byte QNAME = 2;

	/** Version number for the current implementation file's format.
	 * <p>
	 * Version 1:
	 * </p>
	 * <pre> {@code
	 * FILE ::= VERSION_ID ENTRY+
	 * ENTRY ::= PATH PROPERTY_COUNT PROPERTY+
	 * PATH ::= string (does not contain project name)
	 * PROPERTY_COUNT ::= int
	 * PROPERTY ::= QUALIFIER LOCAL_NAME VALUE
	 * QUALIFIER ::= INDEX | QNAME
	 * INDEX -> byte int
	 * QNAME -> byte string
	 * UUID ::= byte[16]
	 * LAST_MODIFIED ::= byte[8]
	 * }</pre>
	 */
	private static final byte VERSION = 1;

	private final List<String> qualifierIndex = new ArrayList<>();

	public PropertyBucket() {
		super(!Boolean.getBoolean("org.eclipse.core.PropertyCache.disable")); //$NON-NLS-1$
	}

	@Override
	protected Entry createEntry(IPath path, Object value) {
		return new PropertyEntry(path, (String[][]) value);
	}

	private PropertyEntry getEntry(IPath path) {
		String pathAsString = path.toString();
		String[][] existing = (String[][]) getEntryValue(pathAsString);
		if (existing == null)
			return null;
		return new PropertyEntry(path, existing);
	}

	@Override
	protected String getIndexFileName() {
		return "properties.index"; //$NON-NLS-1$
	}

	public String getProperty(IPath path, QualifiedName name) {
		PropertyEntry entry = getEntry(path);
		if (entry == null)
			return null;
		return entry.getProperty(name);
	}

	@Override
	protected byte getVersion() {
		return VERSION;
	}

	@Override
	protected String getVersionFileName() {
		return "properties.version"; //$NON-NLS-1$
	}

	@Override
	public void load(String newProjectName, File baseLocation, boolean force) throws CoreException {
		qualifierIndex.clear();
		super.load(newProjectName, baseLocation, force);
	}

	@Override
	protected Object readEntryValue(DataInputStream source) throws IOException, CoreException {
		int length = source.readUnsignedShort();
		String[][] properties = new String[length][3];
		for (String[] propertie : properties) {
			// qualifier
			byte constant = source.readByte();
			switch (constant) {
				case QNAME:
					propertie[0] = source.readUTF();
					qualifierIndex.add(propertie[0]);
					break;
				case INDEX:
					propertie[0] = qualifierIndex.get(source.readInt());
					break;
				default :
					//if we get here the properties file is corrupt
					IPath resourcePath = projectName == null ? Path.ROOT : Path.ROOT.append(projectName);
					String msg = NLS.bind(Messages.properties_readProperties, resourcePath.toString());
					throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, null, msg, null);
			}
			// localName
			propertie[1] = source.readUTF();
			// propertyValue
			propertie[2] = source.readUTF();
		}
		return properties;
	}

	@Override
	public void save() throws CoreException {
		qualifierIndex.clear();
		super.save();
	}

	public void setProperties(PropertyEntry entry) {
		IPath path = entry.getPath();
		String[][] additions = (String[][]) entry.getValue();
		String pathAsString = path.toString();
		String[][] existing = (String[][]) getEntryValue(pathAsString);
		if (existing == null) {
			setEntryValue(pathAsString, additions);
			return;
		}
		setEntryValue(pathAsString, PropertyEntry.merge(existing, additions));
	}

	public void setProperty(IPath path, QualifiedName name, String value) {
		String pathAsString = path.toString();
		String[][] existing = (String[][]) getEntryValue(pathAsString);
		if (existing == null) {
			if (value != null)
				setEntryValue(pathAsString, new String[][] {{name.getQualifier(), name.getLocalName(), value}});
			return;
		}
		String[][] newValue;
		if (value != null)
			newValue = PropertyEntry.insert(existing, name, value);
		else
			newValue = PropertyEntry.delete(existing, name);
		// even if newValue == existing we should mark as dirty (insert may just change the existing array)
		setEntryValue(pathAsString, newValue);
	}

	@Override
	protected void writeEntryValue(DataOutputStream destination, Object entryValue) throws IOException {
		String[][] properties = (String[][]) entryValue;
		destination.writeShort(properties.length);
		for (String[] propertie : properties) {
			// writes the property key qualifier
			int index = qualifierIndex.indexOf(propertie[0]);
			if (index == -1) {
				destination.writeByte(QNAME);
				destination.writeUTF(propertie[0]);
				qualifierIndex.add(propertie[0]);
			} else {
				destination.writeByte(INDEX);
				destination.writeInt(index);
			}
			// then the local name
			destination.writeUTF(propertie[1]);
			// then the property value
			destination.writeUTF(propertie[2]);
		}
	}
}
