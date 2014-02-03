/*******************************************************************************
 * Copyright (c) 2002, 2012 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.util;

import java.lang.ref.SoftReference;

/**
 * Factory maintains a free list and, with FactoryObject,
 * serves as a basis for factories of all types.
 * Factory should only be subclassed in singleton classes;
 * for static factories, it may be instantiated as
 * a static object.
 * @author Bob Foster
 */
public class Factory {
	
	/**
	 * Return the first object on the free list
	 * or null if none.
	 */
	public FactoryObject getFree() {
		Head head = getHead();
		FactoryObject obj = head.next;
		if (obj != null) {
			head.next = obj.next();
			obj.next(null);
		}
		return obj;
	}
	
	/**
	 * Add an object to the free list.
	 */
	public void setFree(FactoryObject obj) {
		Head head = getHead();
		obj.next(head.next);
		head.next = obj;
	}
	
	private Head getHead() {
		Head head = (Head) free.get();
		if (head == null) {
			// head is needed because you can't change
			// the referent of a SoftReference.
			// Without head, we would need to create
			// a new SoftReference each time we remove
			// a map from the list. With head, getting
			// a free object only causes memory allocation
			// when the list has been previously collected.
			head = new Head();
			free = new SoftReference(head);
		}
		return head;
	}

	private static class Head {
		public FactoryObject next;
	}
	private SoftReference free = new SoftReference(new Head());
}
