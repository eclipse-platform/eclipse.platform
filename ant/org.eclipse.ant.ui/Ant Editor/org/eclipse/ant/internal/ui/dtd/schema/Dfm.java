/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 * Deterministic finite state machine. Once constructed DFM is immutable and can be used by multiple threads. A Dfm node is essentially an accepting
 * flag and a hashtable mapping atoms to Dfm nodes. (Almost of org.eclipse.ant.internal.ui.dtd.util is aimed at reducing the storage overhead of
 * hundreds of little hashtables.)
 *
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
		if (dfm == null) {
			dfm = new Dfm();
		}
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

	@Override
	public boolean isAccepting() {
		return accepting;
	}

	@Override
	public IDfm advance(String name) {
		if (any) {
			return this;
		}
		if (empty) {
			return null;
		}
		if (keys == null) {
			return null;
		}
		SortedMap map = getIndirectStringMap(this);
		Dfm dfm = (Dfm) map.get(name);
		freeMap(map);
		return dfm;
	}

	@Override
	public String[] getAccepts() {
		if (keys == null) {
			return new String[0];
		}
		String[] s = new String[keys.length];
		for (int i = 0; i < s.length; i++) {
			s[i] = keys[i].toString();
		}
		return s;
	}

	public Dfm[] getFollows() {
		if (values == null) {
			return new Dfm[0];
		}
		Dfm[] s = new Dfm[values.length];
		System.arraycopy(values, 0, s, 0, values.length);
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

	@Override
	public FactoryObject next() {
		return fNext;
	}

	@Override
	public void next(FactoryObject obj) {
		fNext = (Dfm) obj;
	}

	@Override
	public boolean isAny() {
		return any;
	}

	@Override
	public boolean isEmpty() {
		return empty;
	}

	@Override
	public IAtom getAtom(String name) {
		Object[] allKeys = getKeys();
		if (empty || allKeys == null) {
			return null;
		}
		SortedMap map = getIndirectStringMap(this);
		int index = map.keyIndex(name);
		if (index < 0) {
			return null;
		}
		return (IAtom) allKeys[index];
	}

	@Override
	public IDfm advance(String namespace, String localname) {
		// no namespace support here
		return advance(localname);
	}
}