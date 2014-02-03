/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Object Factory Inc. - Initial implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.dtd;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.ant.internal.ui.dtd.IDfm;
import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.IModel;
import org.eclipse.ant.internal.ui.dtd.ISchema;
import org.eclipse.ant.internal.ui.dtd.ParseError;
import org.eclipse.ant.internal.ui.dtd.Parser;

/**
 * This class is intended to be used from the command line (hence the
 * uncapitalized class name).
 * @author Bob Foster
 */
public class dumper {

	public static int main(String[] args) {

		if (args.length <= 0) {
			System.out.println("Usage: java DTDMerger URL");
			return 0;
		}
		String document = args[0];

		Parser parser = new Parser();
		
		ISchema schema;
		try {
			schema = parser.parse(document);
		} catch (ParseError e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return 1;
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		System.out.println("DTD successfully parsed");
		dumpSchema(schema);
		return 0;
	}
	
	/**
	 * Write schema contents to standard output.
	 */
	private static void dumpSchema(ISchema schema) {
		IElement[] elements = schema.getElements();
		System.out.println(""+elements.length+" elements defined");
		for (int i = 0; i < elements.length; i++) {
			IElement element = elements[i];
			IModel model = element.getContentModel();
			System.out.println("ELEMENT "+element.getName()
				+'"'+model.stringRep()+'"');
			dumpDfm(element.getDfm());
		}
	}

	/**
	 * Dump dfm as a series of states.
	 * <pre>
	 * S0  a=>S1 b=>S2 
	 * S1  c=>S2
	 * S2* d=>S2
	 * </pre>
	 * Where * indicates accepting state.
	 * @param dfm to dump
	 */
	private static void dumpDfm(IDfm dfm) {
		HashMap map = new HashMap();
		dumpDfm(dfm, map, 0);
		LinkedList list = new LinkedList();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			list.add(new State((IDfm)entry.getKey(), (Integer)entry.getValue()));
		}
		State[] states = (State[]) list.toArray(new State[list.size()]);
		Arrays.sort(states);
		for (int i = 0; i < states.length; i++) {
			print(states[i], map);
		}
	}

	private static void print(State state, HashMap map) {
		System.out.print("  S"+state.n.intValue()
			+(state.dfm.isAccepting() ? "*  " : "  "));
		String[] accepts = state.dfm.getAccepts();
		for (int i = 0; i < accepts.length; i++) {
			String accept = accepts[i];
			IDfm next = state.dfm.advance(accept);
			int n = ((Integer)map.get(next)).intValue();
			System.out.print(" "+accept+"=>S"+n);
		}
		System.out.println();
	}
	
	private static int dumpDfm(IDfm dfm, HashMap map, int num) {
		if (!map.containsKey(dfm)) {
			map.put(dfm, new Integer(num++));
			String[] accepts = dfm.getAccepts();
			for (int i = 0; i < accepts.length; i++) {
				IDfm next = dfm.advance(accepts[i]);
				num = dumpDfm(next, map, num);
			}
		}
		return num;
	}
	
	private static class State implements Comparable {
		public IDfm dfm;
		public Integer n;
		public State(IDfm dfm, Integer n) {
			this.dfm = dfm;
			this.n = n;
		}
		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			State other = (State) o;
			return n.intValue() < other.n.intValue()
				? -1 
				: (n.intValue() == other.n.intValue() 
					? 0 
					: 1);
		}

	}
}
