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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.registry;

/**
 *	The SortOperation takes a collection of objects and returns
 *	a sorted collection of these objects.  Concrete instances of this
 *	class provide the criteria for the sorting of the objects based on
 *	the type of the objects.
 */
public abstract class Sorter {
	/**
	 *	Returns true if elementTwo is 'greater than' elementOne
	 *	This is the 'ordering' method of the sort operation.
	 *	Each subclass overides this method with the particular
	 *	implementation of the 'greater than' concept for the
	 *	objects being sorted.
	 */
	/*package*/ abstract boolean compare(Object elementOne, Object elementTwo);

	/**
	 *	Sort the objects in sorted collection and return that collection.
	 */
	private Object[] quickSort(Object[] sortedCollection, int left, int right) {
		int originalLeft = left;
		int originalRight = right;
		Object mid = sortedCollection[(left + right) / 2];

		do {
			while (compare(sortedCollection[left], mid))
				left++;
			while (compare(mid, sortedCollection[right]))
				right--;
			if (left <= right) {
				Object tmp = sortedCollection[left];
				sortedCollection[left] = sortedCollection[right];
				sortedCollection[right] = tmp;
				left++;
				right--;
			}
		}
		while (left <= right);

		if (originalLeft < right)
			sortedCollection = quickSort(sortedCollection, originalLeft, right);
		if (left < originalRight)
			sortedCollection = quickSort(sortedCollection, left, originalRight);

		return sortedCollection;
	}

	/**
	 *	Return a new sorted collection from this unsorted collection.
	 *	Sort using quick sort.
	 */
	/*package*/ Object[] sort(Object[] unSortedCollection) {
		int size = unSortedCollection.length;
		Object[] sortedCollection = new Object[size];

		//copy the array so can return a new sorted collection
		System.arraycopy(unSortedCollection, 0, sortedCollection, 0, size);
		if (size > 1)
			quickSort(sortedCollection, 0, size - 1);

		return sortedCollection;
	}
}
