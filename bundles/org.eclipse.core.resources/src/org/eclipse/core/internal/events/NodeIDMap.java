/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.events;

import org.eclipse.core.runtime.IPath;

/**
 * A specialized map that maps Node IDs to their old and new paths.
 * Used for calculating moves during resource change notification.
 */
public class NodeIDMap {
	//using prime table sizes improves our hash function
	private static final int[] SIZES = new int[] {13, 29, 71, 173, 349, 733, 1511, 3079, 6133, 16381, 32653, 65543, 131111, 262139, 524287, 1051601};
	private static final double LOAD_FACTOR = 0.75;
	//2^32 * golden ratio
	private static final long LARGE_NUMBER = 2654435761L;

	int sizeOffset = 0;
	protected int elementCount = 0;
	protected long[] ids;
	protected IPath[] oldPaths;
	protected IPath[] newPaths;

	/**
	 * Creates a new node ID map of default capacity.
	 */
	public NodeIDMap() {
		this.sizeOffset = 0;
		this.ids = new long[SIZES[sizeOffset]];
		this.oldPaths = new IPath[SIZES[sizeOffset]];
		this.newPaths = new IPath[SIZES[sizeOffset]];
	}

	/**
	 * The array isn't large enough so double its size and rehash
	 * all its current values.
	 */
	protected void expand() {
		int newLength;
		try {
			newLength = SIZES[++sizeOffset];
		} catch (ArrayIndexOutOfBoundsException e) {
			//will only occur if there are > 1 million elements in delta
			newLength = ids.length * 2;
		}
		long[] grownIds = new long[newLength];
		IPath[] grownOldPaths = new IPath[newLength];
		IPath[] grownNewPaths = new IPath[newLength];
		int maxArrayIndex = newLength - 1;
		for (int i = 0; i < ids.length; i++) {
			long id = ids[i];
			if (id != 0) {
				int hash = hashFor(id, newLength);
				while (grownIds[hash] != 0) {
					hash++;
					if (hash > maxArrayIndex)
						hash = 0;
				}
				grownIds[hash] = id;
				grownOldPaths[hash] = oldPaths[i];
				grownNewPaths[hash] = newPaths[i];
			}
		}
		ids = grownIds;
		oldPaths = grownOldPaths;
		newPaths = grownNewPaths;
	}

	/**
	 * Returns the index of the given element in the map.  If not
	 * found, returns -1.
	 */
	private int getIndex(long searchID) {
		final int len = ids.length;
		int hash = hashFor(searchID, len);

		// search the last half of the array
		for (int i = hash; i < len; i++) {
			if (ids[i] == searchID)
				return i;
			// marker info not found so return -1
			if (ids[i] == 0)
				return -1;
		}

		// search the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			if (ids[i] == searchID)
				return i;
			// marker info not found so return -1
			if (ids[i] == 0)
				return -1;
		}
		// marker info not found so return -1
		return -1;
	}

	/**
	 * Returns the new path location for the given ID, or null
	 * if no new path is available.
	 */
	public IPath getNewPath(long nodeID) {
		int index = getIndex(nodeID);
		if (index == -1)
			return null;
		return newPaths[index];
	}

	/**
	 * Returns the old path location for the given ID, or null
	 * if no old path is available.
	 */
	public IPath getOldPath(long nodeID) {
		int index = getIndex(nodeID);
		if (index == -1)
			return null;
		return oldPaths[index];
	}

	private int hashFor(long id, int size) {
		//Knuth's hash function from Art of Computer Programming section 6.4
		return (int) Math.abs((id * LARGE_NUMBER) % size);
	}

	/**
	 * Returns true if there are no elements in the map, and
	 * false otherwise.
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}

	/**
	 * Adds the given path mappings to the map.  If either oldPath
	 * or newPath is null, they are ignored (old map values are not overwritten).
	 */
	private void put(long id, IPath oldPath, IPath newPath) {
		if (oldPath == null && newPath == null)
			return;
		int hash = hashFor(id, ids.length);

		// search for an empty slot at the end of the array
		for (int i = hash; i < ids.length; i++) {
			if (ids[i] == id) {
				//replace value for existing entry
				if (oldPath != null)
					oldPaths[i] = oldPath;
				if (newPath != null)
					newPaths[i] = newPath;
				return;
			}
			if (ids[i] == 0) {
				//add a new entry to the map
				ids[i] = id;
				if (oldPath != null)
					oldPaths[i] = oldPath;
				if (newPath != null)
					newPaths[i] = newPath;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return;
			}
		}

		// search for an empty slot at the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			if (ids[i] == id) {
				//replace value for existing entry
				if (oldPath != null)
					oldPaths[i] = oldPath;
				if (newPath != null)
					newPaths[i] = newPath;
				return;
			}
			if (ids[i] == 0) {
				//add a new entry to the map
				ids[i] = id;
				if (oldPath != null)
					oldPaths[i] = oldPath;
				if (newPath != null)
					newPaths[i] = newPath;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return;
			}
		}
		// if we didn't find a free slot, then try again with the expanded set
		expand();
		put(id, oldPath, newPath);
	}

	/**
	 * Adds an entry for a node's old path
	 */
	public void putOldPath(long id, IPath path) {
		put(id, path, null);
	}

	/**
	 * Adds an entry for a node's old path
	 */
	public void putNewPath(long id, IPath path) {
		put(id, null, path);
	}

	private boolean shouldGrow() {
		return elementCount > ids.length * LOAD_FACTOR;
	}
}
