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
package org.eclipse.ant.internal.ui.dtd.schema;

import org.eclipse.ant.internal.ui.dtd.IAtom;
import org.eclipse.ant.internal.ui.dtd.IDfm;
import org.eclipse.ant.internal.ui.dtd.util.Factory;
import org.eclipse.ant.internal.ui.dtd.util.FactoryObject;
import org.eclipse.ant.internal.ui.dtd.util.MapHolder;
import org.eclipse.ant.internal.ui.dtd.util.SortedMap;


/**
 * Deterministic finite state machine.
 * Once constructed DFM is immutable and can be used by multiple threads.
 * A Dfm node is essentially an accepting flag and a hashtable mapping atoms to
 * Dfm nodes. (Almost of org.eclipse.ant.internal.ui.dtd.util is aimed at reducing the storage
 * overhead of hundreds of little hashtables.)
 * @author Bob Foster
 */
public class Dfm extends MapHolder implements IDfm, FactoryObject {

	public boolean accepting;
	public boolean empty, any;
	public int id;
	private static int unique = 0;
	private static Factory factory = new Factory();
	private Dfm fNext;
	
	public static Dfm dfm(boolean accepting) {
		Dfm dfm = free();
		dfm.accepting = accepting;
		return dfm;
	}

	protected Dfm() {
	}
	
	private static Dfm free() {
		Dfm dfm = (Dfm) factory.getFree();
		if (dfm == null)
			dfm = new Dfm();
		dfm.accepting = dfm.empty = dfm.any = false;
		dfm.id = unique++;
		return dfm;
	}

	public static Dfm dfm(IAtom accept, Dfm follow) {
		Dfm dfm = free();
		dfm.keys = new Object[1];
		dfm.keys[0] = accept;
		dfm.values = new Object[1];
		dfm.values[0] = follow;
		return dfm;
	}
	
	public static void free(Dfm dfm) {
		dfm.setKeys(null);
		dfm.setValues(null);
		factory.setFree(dfm);
	}
	
	public boolean isAccepting() {
		return accepting;
	}
	
	public IDfm advance(String name) {
		if (any)
			return this;
		if (empty)
			return null;
		if (keys == null)
			return null;
		SortedMap map = getIndirectStringMap(this);
		Dfm dfm = (Dfm) map.get(name);
		freeMap(map);
		return dfm;
	}
	
	public String[] getAccepts() {
		if (keys == null)
			return new String[0];
		String[] s = new String[keys.length];
		for (int i = 0; i < s.length; i++) {
			s[i] = keys[i].toString();
		}
		return s;
	}

	public Dfm[] getFollows() {
		if (values == null)
			return new Dfm[0];
		Dfm[] s = new Dfm[values.length];
		System.arraycopy(values,0,s,0,values.length);
		return s;
	}

	public void merge(Dfm other) {
		accepting |= other.accepting;
		SortedMap map = getIndirectStringMap(this);
		SortedMap othermap = getIndirectStringMap(other);
		map.merge(othermap);
		freeMap(map);
		freeMap(othermap);
	}
	
	public SortedMap getMap() {
		return getIndirectStringMap(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.util.FactoryObject#next()
	 */
	public FactoryObject next() {
		return fNext;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.util.FactoryObject#next(org.eclipse.ant.internal.ui.dtd.util.FactoryObject)
	 */
	public void next(FactoryObject obj) {
		fNext = (Dfm) obj;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.IDfm#isAny()
	 */
	public boolean isAny() {
		return any;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.IDfm#isEmpty()
	 */
	public boolean isEmpty() {
		return empty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.IDfm#getAtom(java.lang.String)
	 */
	public IAtom getAtom(String name) {
		Object[] allKeys = getKeys();
		if (empty || allKeys == null){
			return null;
		}
		SortedMap map = getIndirectStringMap(this);
		int index = map.keyIndex(name);
		if (index < 0) {
			return null;
		}
		return (IAtom) allKeys[index];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.IDfm#advance(java.lang.String, java.lang.String)
	 */
	public IDfm advance(String namespace, String localname) {
		// no namespace support here
		return advance(localname);
	}
}