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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.runtime.IPath;

public class HistoryBucket extends Bucket {

	/**
	 * A entry in the bucket index. Each entry has one path and a collection
	 * of states, which by their turn contain a (UUID, timestamp) pair.
	 * <p>
	 * This class is intended as a lightweight way of hiding the internal data structure.
	 * Objects of this class are supposed to be short-lived. No instances
	 * of this class are kept stored anywhere. The real stuff (the internal data structure)
	 * is.
	 * </p>
	 */
	public static final class HistoryEntry extends Bucket.Entry {

		final static Comparator<byte[]> COMPARATOR = HistoryEntry::compareStates;

		// the length of each component of the data array
		private final static byte[][] EMPTY_DATA = new byte[0][];
		// the length of a long in bytes
		private final static int LONG_LENGTH = 8;
		// the length of a UUID in bytes
		private final static int UUID_LENGTH = UniversalUniqueIdentifier.BYTES_SIZE;
		public final static int DATA_LENGTH = UUID_LENGTH + LONG_LENGTH;

		/**
		 * The history states. The first array dimension is the number of states. The
		 * second dimension is an encoding of the {UUID,timestamp} pair for that entry.
		 */
		private byte[][] data;

		/**
		 * Comparison logic for states in byte[] form.
		 *
		 * @see Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		static int compareStates(byte[] state1, byte[] state2) {
			long timestamp1 = getTimestamp(state1);
			long timestamp2 = getTimestamp(state2);
			if (timestamp1 == timestamp2)
				return -UniversalUniqueIdentifier.compareTime(state1, state2);
			return timestamp1 < timestamp2 ? +1 : -1;
		}

		/**
		 * Returns the byte array representation of a (UUID, timestamp) pair.
		 */
		static byte[] getState(UniversalUniqueIdentifier uuid, long timestamp) {
			byte[] uuidBytes = uuid.toBytes();
			byte[] state = new byte[DATA_LENGTH];
			System.arraycopy(uuidBytes, 0, state, 0, uuidBytes.length);
			for (int j = 0; j < LONG_LENGTH; j++) {
				state[UUID_LENGTH + j] = (byte) (0xFF & timestamp);
				timestamp >>>= 8;
			}
			return state;
		}

		private static long getTimestamp(byte[] state) {
			long timestamp = 0;
			for (int j = 0; j < LONG_LENGTH; j++)
				timestamp += (state[UUID_LENGTH + j] & 0xFFL) << j * 8;
			return timestamp;
		}

		/**
		 * Inserts the given item into the given array at the right position.
		 * Returns the resulting array. Returns null if the item already exists.
		 */
		static byte[][] insert(byte[][] existing, byte[] toAdd) {
			// look for the right spot where to insert the new guy
			int index = search(existing, toAdd);
			if (index >= 0)
				// already there - nothing else to be done
				return null;
			// not found - insert
			int insertPosition = -index - 1;
			byte[][] newValue = new byte[existing.length + 1][];
			if (insertPosition > 0)
				System.arraycopy(existing, 0, newValue, 0, insertPosition);
			newValue[insertPosition] = toAdd;
			if (insertPosition < existing.length)
				System.arraycopy(existing, insertPosition, newValue, insertPosition + 1, existing.length - insertPosition);
			return newValue;
		}

		/**
		 * Merges two entries (are always sorted). Duplicates are discarded.
		 */
		static byte[][] merge(byte[][] base, byte[][] additions) {
			int additionPointer = 0;
			int basePointer = 0;
			int added = 0;
			byte[][] result = new byte[base.length + additions.length][];
			while (basePointer < base.length && additionPointer < additions.length) {
				int comparison = compareStates(base[basePointer], additions[additionPointer]);
				if (comparison == 0) {
					result[added++] = base[basePointer++];
					// duplicate, ignore
					additionPointer++;
				} else if (comparison < 0)
					result[added++] = base[basePointer++];
				else
					result[added++] = additions[additionPointer++];
			}
			// copy the remaining states from either additions or base arrays
			byte[][] remaining = basePointer == base.length ? additions : base;
			int remainingPointer = basePointer == base.length ? additionPointer : basePointer;
			int remainingCount = remaining.length - remainingPointer;
			System.arraycopy(remaining, remainingPointer, result, added, remainingCount);
			added += remainingCount;
			if (added == base.length + additions.length)
				// no collisions
				return result;
			// there were collisions, need to compact
			byte[][] finalResult = new byte[added][];
			System.arraycopy(result, 0, finalResult, 0, finalResult.length);
			return finalResult;
		}

		private static int search(byte[][] existing, byte[] element) {
			return Arrays.binarySearch(existing, element, COMPARATOR);
		}

		public HistoryEntry(IPath path, byte[][] data) {
			super(path);
			this.data = data;
		}

		public HistoryEntry(IPath path, HistoryEntry base) {
			super(path);
			this.data = new byte[base.data.length][];
			System.arraycopy(base.data, 0, this.data, 0, this.data.length);
		}

		/**
		 * Compacts the data array removing any null slots. If non-null slots
		 * are found, the entry is marked for removal.
		 */
		private void compact() {
			if (!isDirty())
				return;
			int occurrences = 0;
			for (byte[] d : data) {
				if (d != null) {
					data[occurrences++] = d;
				}
			}
			if (occurrences == data.length)
				// no states deleted
				return;
			if (occurrences == 0) {
				// no states remaining
				data = EMPTY_DATA;
				delete();
				return;
			}
			byte[][] result = new byte[occurrences][];
			System.arraycopy(data, 0, result, 0, occurrences);
			data = result;
		}

		public void deleteOccurrence(int i) {
			markDirty();
			data[i] = null;
		}

		byte[][] getData() {
			return data;
		}

		@Override
		public int getOccurrences() {
			return data.length;
		}

		public long getTimestamp(int i) {
			return getTimestamp(data[i]);
		}

		public UniversalUniqueIdentifier getUUID(int i) {
			return new UniversalUniqueIdentifier(data[i]);
		}

		@Override
		public Object getValue() {
			return data;
		}

		@Override
		public boolean isEmpty() {
			return data.length == 0;
		}

		@Override
		public void visited() {
			compact();
		}

	}

	/**
	 * Version number for the current implementation file's format.
	 * <p>
	 * Version 2 (3.1 M5):
	 * </p>
	 * <pre>
	 * FILE ::= VERSION_ID ENTRY+
	 * ENTRY ::= PATH STATE_COUNT STATE+
	 * PATH ::= string (does not include project name)
	 * STATE_COUNT ::= int
	 * STATE ::= UUID LAST_MODIFIED
	 * UUID	 ::= byte[16]
	 * LAST_MODIFIED ::= byte[8]
	 * </pre>
	 * <p>
	 * Version 1 (3.1 M4):
	 * </p>
	 * <pre>
	 * FILE ::= VERSION_ID ENTRY+
	 * ENTRY ::= PATH STATE_COUNT STATE+
	 * PATH ::= string
	 * STATE_COUNT ::= int
	 * STATE ::= UUID LAST_MODIFIED
	 * UUID	 ::= byte[16]
	 * LAST_MODIFIED ::= byte[8]
	 * </pre>
	 */
	public final static byte VERSION = 2;

	public HistoryBucket() {
		super(false); // history is fire and forget
	}

	public void addBlob(IPath path, UniversalUniqueIdentifier uuid, long lastModified) {
		byte[] state = HistoryEntry.getState(uuid, lastModified);
		String pathAsString = path.toString();
		byte[][] existing = (byte[][]) getEntryValue(pathAsString);
		if (existing == null) {
			setEntryValue(pathAsString, new byte[][] {state});
			return;
		}
		byte[][] newValue = HistoryEntry.insert(existing, state);
		if (newValue == null)
			return;
		setEntryValue(pathAsString, newValue);
	}

	public void addBlobs(HistoryEntry fileEntry) {
		IPath path = fileEntry.getPath();
		byte[][] additions = fileEntry.getData();
		String pathAsString = path.toString();
		byte[][] existing = (byte[][]) getEntryValue(pathAsString);
		if (existing == null) {
			setEntryValue(pathAsString, additions);
			return;
		}
		setEntryValue(pathAsString, HistoryEntry.merge(existing, additions));
	}

	@Override
	protected Bucket.Entry createEntry(IPath path, Object value) {
		return new HistoryEntry(path, (byte[][]) value);
	}

	public HistoryEntry getEntry(IPath path) {
		String pathAsString = path.toString();
		byte[][] existing = (byte[][]) getEntryValue(pathAsString);
		if (existing == null)
			return null;
		return new HistoryEntry(path, existing);
	}

	@Override
	protected String getIndexFileName() {
		return "history.index"; //$NON-NLS-1$
	}

	@Override
	protected byte getVersion() {
		return VERSION;
	}

	@Override
	protected String getVersionFileName() {
		return "history.version"; //$NON-NLS-1$
	}

	@Override
	protected Object readEntryValue(DataInputStream source) throws IOException {
		int length = source.readUnsignedShort();
		byte[][] uuids = new byte[length][HistoryEntry.DATA_LENGTH];
		for (byte[] uuid : uuids)
			source.read(uuid);
		return uuids;
	}

	@Override
	protected void writeEntryValue(DataOutputStream destination, Object entryValue) throws IOException {
		byte[][] uuids = (byte[][]) entryValue;
		destination.writeShort(uuids.length);
		for (byte[] uuid : uuids)
			destination.write(uuid);
	}
}
