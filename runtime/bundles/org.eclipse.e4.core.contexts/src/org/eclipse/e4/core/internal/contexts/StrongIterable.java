/*******************************************************************************
 * Copyright (c) 2021 Joerg Kubitz.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz              - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.contexts;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An Iterable that automatically removes cleared References while iterating.
 * The consumer then iterates over strong referents only.
 *
 * @see Iterable
 * @see Reference
 * @see WeakReference
 */
public final class StrongIterable<T> implements Iterable<T> {
	private final Iterable<? extends Reference<T>> weakIterable;

	/**
	 * @param weakIterable must support {@link Iterator#remove()} and must not throw
	 *                     {@link ConcurrentModificationException} while iterating
	 * @see ConcurrentLinkedDeque
	 * @see ConcurrentLinkedQueue
	 */
	public StrongIterable(Iterable<? extends Reference<T>> weakIterable) {
		this.weakIterable = weakIterable;
	}

	@Override
	public Iterator<T> iterator() {
		Iterator<? extends Reference<T>> i = weakIterable.iterator();
		return new Iterator<>() {
			private T next;

			@Override
			public boolean hasNext() {
				if (next != null)
					return true;
				while (i.hasNext()) {
					next = i.next().get();
					if (next == null) {
						i.remove(); // remove cleared Reference
					} else {
						break;
					}
				}
				return next != null;
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				T result = next;
				next = null;
				return result;
			}

			@Override
			public void remove() {
				i.remove();
			}
		};
	}
}