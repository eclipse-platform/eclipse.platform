/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.nls;

public class IntegerMap {

	private static final int DEFAULT = 0;
	private static final int GROW_SIZE = 10;

	private int[] keys;
	private int[] values;

	public IntegerMap(int size) {
		super();
		keys = new int[size];
		values = new int[size];
	}

	public int get(int key) {
		for (int i = 0; i < keys.length; i++) {
			int current = keys[i];
			if (current != 0 && current == key)
				return values[i];
		}
		return DEFAULT;
	}

	public void put(int key, int value) {

		// replace if exists
		int emptySlot = -1;
		for (int i = 0; i < keys.length; i++) {
			int current = keys[i];
			if (current == 0) {
				emptySlot = i;
				continue;
			} else if (current == key) {
				values[i] = value;
				return;
			}
		}

		// grow if needed, then fill the empty slot
		if (emptySlot == -1)
			emptySlot = grow();
		keys[emptySlot] = key;
		values[emptySlot] = value;
	}

	private int grow() {
		int size = keys.length;
		int[] tempKeys = new int[size + GROW_SIZE];
		System.arraycopy(keys, 0, tempKeys, 0, size);
		keys = tempKeys;
		int[] tempValues = new int[size + GROW_SIZE];
		System.arraycopy(values, 0, tempValues, 0, size);
		values = tempValues;
		return size;
	}

}
