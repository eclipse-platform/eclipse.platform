/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
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
import org.eclipse.ant.internal.ui.dtd.util.Factory;
import org.eclipse.ant.internal.ui.dtd.util.FactoryObject;


/**
 * Non-deterministic finite state machine.
 * @author Bob Foster
 */
public class Nfm implements FactoryObject {
	private NfmNode start;
	private NfmNode stop;
	
	public NfmNode getStart() {
		return start;
	}
	
	public NfmNode getStop() {
		return stop;
	}
	
	/**
	 * Construct an nfm such that:
	 * <pre>
	 * start  stop
	 *   |      |
	 * +---+  +---+
	 * | s |->|   |
	 * +---+  +---+
	 * </pre>
	 * In all pictures, boxes are NfmNodes.
	 */
	private static Nfm nfm(IAtom s) {
		Nfm nfm = free();
		nfm.stop = NfmNode.nfmNode();
		nfm.start = NfmNode.nfmNode(s, nfm.stop);
		return nfm;
	}
	
	/**
	 * Construct an nfm that "wraps" an existing nfm x such that:
	 * <pre>
	 * start                            stop
	 *   |                                |
	 * +---+  +---------+  +---------+  +---+
	 * |   |->| x start |  | x stop  |->|   |
	 * +---+  +---------+  +---------+  +---+
	 * </pre>
	 */
	private static Nfm nfm(Nfm x) {
		Nfm nfm = free();
		nfm.start = NfmNode.nfmNode(x.start);
		nfm.stop = NfmNode.nfmNode();
		x.stop.next1 = nfm.stop;
		return nfm;
	}
	
	private static Nfm nfm() {
		Nfm nfm = free();
		nfm.start = NfmNode.nfmNode();
		nfm.stop = NfmNode.nfmNode();
		return nfm;
	}

	public static Nfm getNfm(IAtom s) {
		return nfm(s);
	}
	
	/**
	 * "Star" an existing nfm x.
	 * <pre>
	 * start                            stop
	 *   |                                |
	 * +---+  +---------+  +---------+  +---+
	 * |   |->| x start |  | x stop  |->|   |
	 * +---+  +---------+  +---------+  +---+
	 *   |         |            |         |
	 *   |         +-----<------+         |
	 *   +------>-------------------------+
	 * </pre>
	 * Frees x.
	 */
	public static Nfm getStar(Nfm x) {
		// Make the back link
		x.stop.next2 = x.start;
		Nfm tmp = nfm(x);
		// Make the forward link
		tmp.start.next2 = tmp.stop;
		free(x);
		return tmp;
	}
	
	/**
	 * "Question" an existing nfm x: x => x?
	 * <pre>
	 * start                            stop
	 *   |                                |
	 * +---+  +---------+  +---------+  +---+
	 * |   |->| x start |  | x stop  |->|   |
	 * +---+  +---------+  +---------+  +---+
	 *   |                                |
	 *   +---------------->---------------+
	 * </pre>
	 * Frees x.
	 */
	public static Nfm getQuestion(Nfm x) {
		Nfm tmp = nfm(x);
		// Make the forward link
		tmp.start.next2 = tmp.stop;
		free(x);
		return tmp;
	}

	/**
	 * "Plus" an existing nfm x -> x+
	 * <pre>
	 * start                            stop
	 *   |                                |
	 * +---+  +---------+  +---------+  +---+
	 * |   |->| x start |  | x stop  |->|   |
	 * +---+  +---------+  +---------+  +---+
	 *             |            |
	 *             +-----<------+
	 * </pre>
	 * Frees x.
	 */
	public static Nfm getPlus(Nfm x) {
		// Make the back link
		x.stop.next2 = x.start;
		Nfm tmp = nfm(x);
		free(x);
		return tmp;
	}
	
	/**
	 * "Or" two existing nfms x,y -> x|y
	 * <pre>
	 * start                            stop
	 *   |                                |
	 * +---+  +---------+  +---------+  +---+
	 * |   |->| x start |  | x stop  |->|   |
	 * +---+  +---------+  +---------+  +---+
	 *   |                                |
	 *   |    +---------+  +---------+    |
	 *   +--->| y start |  | y stop  |-->-+
	 *        +---------+  +---------+
	 * </pre>
	 * Frees x and y.
	 */
	public static Nfm getOr(Nfm x, Nfm y) {
		Nfm tmp = nfm();
		tmp.start.next1 = x.start;
		tmp.start.next2 = y.start;
		x.stop.next1 = tmp.stop;
		y.stop.next1 = tmp.stop;
		free(x);
		free(y);
		return tmp;
	}
	
	/**
	 * "Comma" two existing nfms x,y -> x,y
	 * 
	 * <p>Re-uses x so that x.stop is first
	 * transformed to y.start and then
	 * x.stop is reset to y.stop.
	 * This is as efficient as possible.
	 * <pre>
	 * x start      former x stop   x stop
	 *     |               |           |
	 * +---------+  +----------+  +--------+
	 * | x start |  | y start  |->| y stop |
	 * +---------+  +----------+  +--------+
	 * </pre>
	 * Frees y, returns x modified.
	 */
	public static Nfm getComma(Nfm x, Nfm y) {
		x.stop.next1 = y.start.next1;
		x.stop.next2 = y.start.next2;
		x.stop.symbol = y.start.symbol;
		x.stop = y.stop;
		free(y);
		return x;
	}
	
	/**
	 * "{min,*}" an existing nfm x -> x[0],x[1],...,x[min-1],x[min]*
	 * Frees x.
	 */
	public static Nfm getUnbounded(Nfm x, int min) {
		if (min == 0)
			return getStar(x);
		if (min == 1)
			return getPlus(x);
		Nfm last1 = nfm(x), last2 = nfm(x);
		for (int i = 2; i < min; i++) {
			last1 = getComma(last1, last2);
			free(last2);
			last2 = nfm(x);
		}
		free(x);
		return getComma(last1, getStar(last2));
	}
	
	/**
	 * "{min,max}" an existing nfm x -> x[0],x[1],...,x[min-1],x[min]?,...,x[max-1]?
	 * Frees or returns x.
	 */
	public static Nfm getMinMax(Nfm x, int min, int max) {
		if (max == Integer.MAX_VALUE)
			return getUnbounded(x, min);
		if (max == 0) {
			free(x);
			return nfm((IAtom)null);
		}
		if (max == 1) {
			if (min == 0)
				return getQuestion(x);
			return x;
		}
		Nfm last = null;
		int i = 0;
		for (; i < min; i++) {
			if (last == null)
				last = nfm(x);
			else {
				Nfm tmp = nfm(x);
				last = getComma(last, tmp);
				free(tmp);
			}
		}
		for (; i < max; i++) {
			if (last == null)
				last = getQuestion(x);
			else {
				Nfm tmp = getQuestion(x);
				last = getComma(last, tmp);
				free(tmp);
				//??? this is inefficient since the first failure
				//    in a sequence of x?,x?,...,x? can skip to
				//    the end rather than keep trying to match x
			}
		}
		free(x);
		return last;
	}
	
	// Below here is factory stuff
	
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
		fNext = (Nfm) obj;
	}
	private Nfm fNext;
	private static Factory fFactory = new Factory();
	private static Nfm free() {
		Nfm nfm = (Nfm) fFactory.getFree();
		if (nfm == null)
			return new Nfm();
		return nfm;
	}
	public static void free(Nfm nfm) {
		nfm.start = nfm.stop = null;
		fFactory.setFree(nfm);
	}
	private Nfm() {
	}
}
