/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.core.internal.jobs;

import java.util.*;

/**
 * A Queue of objects.
 */
public class Queue {
	protected Object[] elements;
	protected int head;
	protected boolean reuse;
	protected int tail;

	public Queue() {
		this(20, false);
	}

	/**
	 * The parameter reuse indicates what do you want to happen with
	 * the object reference when you remove it from the queue. If
	 * reuse is false the queue no longer holds a reference to the
	 * object when it is removed. If reuse is true you can use the
	 * method getNextAvailableObject to get an used object, set its
	 * new values and add it again to the queue.
	 */
	public Queue(int size, boolean reuse) {
		elements = new Object[size];
		head = tail = 0;
		this.reuse = reuse;
	}

	/**
	 * Adds an object to the tail of the queue.
	 */
	public void enqueue(Object element) {
		int newTail = increment(tail);
		if (newTail == head) {
			grow();
			newTail = tail + 1;
		}
		elements[tail] = element;
		tail = newTail;
	}

	/**
	 * This method does not affect the queue itself. It is only a
	 * helper to decrement an index in the queue.
	 */
	public int decrement(int index) {
		return (index == 0) ? (elements.length - 1) : index - 1;
	}

	public Iterator elements() {
		/**/
		if (isEmpty())
			return new ArrayList(0).iterator();

		/* if head < tail we can use the same array */
		if (head <= tail)
			return Arrays.asList(elements).iterator();

		/* otherwise we need to create a new array */
		Object[] newElements = new Object[size()];
		int end = (elements.length - head);
		System.arraycopy(elements, head, newElements, 0, end);
		System.arraycopy(elements, 0, newElements, end, tail);
		return Arrays.asList(newElements).iterator();
	}

	public Object get(Object o) {
		int index = head;
		while (index != tail) {
			if (elements[index].equals(o))
				return elements[index];
			index = increment(index);
		}
		return null;
	}

	/**
	 * Removes the given object from the queue. Shifts the underlying array.
	 */
	public boolean remove(Object o) {
		int index = head;
		//find the object to remove
		while (index != tail) {
			if (elements[index].equals(o))
				break;
			index = increment(index);
		}
		//if element wasn't found, return
		if (index == tail)
			return false;
		//store a reference to it (needed for reuse of objects)
		Object toRemove = elements[index];
		int nextIndex = -1;
		while (index != tail) {
			nextIndex = increment(index);
			if (nextIndex != tail)
				elements[index] = elements[nextIndex];

			index = nextIndex;
		}
		//decrement tail
		tail = decrement(tail);

		//if objects are reused, transfer the reference that is removed to the end of the queue
		//otherwise set the element after the last one to null (to avoid duplicate references)
		elements[tail] = reuse ? toRemove : null;
		return true;
	}

	protected void grow() {
		int newSize = (int) (elements.length * 1.5);
		Object[] newElements = new Object[newSize];
		if (tail >= head)
			System.arraycopy(elements, head, newElements, head, size());
		else {
			int newHead = newSize - (elements.length - head);
			System.arraycopy(elements, 0, newElements, 0, tail + 1);
			System.arraycopy(elements, head, newElements, newHead, (newSize - newHead));
			head = newHead;
		}
		elements = newElements;
	}

	/**
	 * This method does not affect the queue itself. It is only a
	 * helper to increment an index in the queue.
	 */
	public int increment(int index) {
		return (index == (elements.length - 1)) ? 0 : index + 1;
	}

	public boolean isEmpty() {
		return tail == head;
	}

	public Object peek() {
		return elements[head];
	}

	/**
	 * Removes an returns the item at the head of the queue.
	 */
	public Object dequeue() {
		if (isEmpty())
			return null;
		Object result = peek();
		if (!reuse)
			elements[head] = null;
		head = increment(head);
		return result;
	}

	public int size() {
		return tail > head ? (tail - head) : ((elements.length - head) + tail);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("["); //$NON-NLS-1$
		if (!isEmpty()) {
			Iterator it = elements();
			while (true) {
				sb.append(it.next());
				if (it.hasNext())
					sb.append(", "); //$NON-NLS-1$
				else
					break;
			}
		}
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}
}
