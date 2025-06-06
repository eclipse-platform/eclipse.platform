/*******************************************************************************
 * Copyright (c) 2002, 2013 Object Factory Inc.
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.dtd.ParseError;
import org.eclipse.ant.internal.ui.dtd.util.SortedMap;
import org.eclipse.ant.internal.ui.dtd.util.SortedMapFactory;

/**
 * NfmParser parses an NFA and returns an equivalent DFA if it can do so in linear time and space (in terms of the original NFA).
 * <p>
 *
 * As used here, NfmParser is called lazily when someone actually asks for a Dfm. This is for performance reasons. Why go to the work of calculating
 * all those Dfms if nobody ever uses them?
 *
 * Well-formed errors in content models have already been detected. The only error that can arise in NfmParser is an ambiguity error.
 *
 * Bruggemann-Klein showed that if an NFA is 1-unambiguous, an epsilon-free NFA constructed from it in linear time is actually a DFA. This is obvious
 * in NfmParser. The algorithm works by removing all ambiguous transitions as the graph is constructed, then proving that the reduced graph is
 * equivalent to the original in time linear in the number of ambiguous transitions.
 *
 * An effort is made to keep the DFA small, but there is no optimization step, as DFAs are small anyway, with some linear inflation around *. In a
 * pathological case, like the classical (a*,a*,a*,..., a*) the number of transitions in the DFA can be quadratic but this algorithm will not blow up
 * exponentially.
 *
 * @author Bob Foster
 */
public class NfmParser {

	public Dfm parse(Nfm nfm) throws ParseError {

		// Parse nfm into dfm

		Dfm dfm = parseStart(nfm.getStart(), nfm.getStop());

		// Make list of dfms in graph

		ArrayList<Dfm> dfms = new ArrayList<>();
		collect(dfm, dfms);

		// Detect accept conflicts

		HashMap<Dfm, Dfm> duplicates = new HashMap<>();
		detect(dfms, duplicates);

		// Replace duplicate dfms in graph

		replace(dfms, duplicates);

		// Allow nfm memory to be re-used

		Nfm.free(nfm);
		NfmNode.freeAll();

		return dfm;
	}

	private void reportError(String name) throws ParseError {
		throw new ParseError(MessageFormat.format(AntDTDSchemaMessages.NfmParser_Ambiguous, name));
	}

	public static void collect(Dfm dfm, List<Dfm> dfms) {
		dfms.add(dfm);
		collect1(dfm, dfms);
	}

	private static void collect1(Dfm dfm, List<Dfm> dfms) {
		Object[] follows = dfm.getValues();
		if (follows != null) {
			for (Object follow2 : follows) {
				Dfm follow = (Dfm) follow2;
				if (!dfms.contains(follow)) {
					dfms.add(follow);
					collect1(follow, dfms);
				}
			}
		}
	}

	/**
	 * Replace duplicate dfms found during conflict resolution.
	 */
	private void replace(ArrayList<Dfm> dfms, HashMap<Dfm, Dfm> removed) {
		for (Dfm dfm : dfms) {
			Object[] follows = dfm.getValues();
			if (follows != null) {
				for (int j = 0; j < follows.length; j++) {
					Dfm replacement, follow = (Dfm) follows[j];
					while ((replacement = removed.get(follow)) != null) {
						follow = replacement;
					}
					follows[j] = follow;
				}
			}
		}
		// release dfms so can be re-used
		for (Dfm dfm : removed.keySet()) {
			Dfm.free(dfm);
		}
	}

	/**
	 * Detect conflicts in each state. Two transitions are a potential conflict if they accept the same string value. They are an actual conflict if
	 * their follow dfms are not identical and they are an actual ambiguity if the transition atoms of the follow dfms are not pairwise identical.
	 * This is derived from the rule of Bruggemann-Klein, which determines that (a|a)b is not ambiguous, but both (a,b)|(a,c) and (a,b)|(a,b) are. The
	 * latter might be surprising, but that's committee work for you. If two transitions are not ambiguous, one can be removed without affecting the
	 * language accepted, and thus we have converted our epsilon-free NFA into a DFA. If any two transitions are ambiguous, we report an error and our
	 * responsibility ends. Note that no transitions can be removed until all have been checked; this might disguise the ambiguity in, e.g.,
	 * ((a|a),b,(a|a))*.
	 */
	private void detect(ArrayList<Dfm> dfms, HashMap<Dfm, Dfm> duplicates) throws ParseError {
		for (Dfm dfm : dfms) {
			Object[] accepts = dfm.getKeys();
			Object[] follows = dfm.getValues();
			if (accepts != null) {
				String last = null;
				for (int i = 0, lasti = -1; i < accepts.length; i++) {
					String accept = accepts[i].toString();
					// accepts strings are interned allowing identity comparison

					if (last != null && last == accept) {
						if (follows[i] != follows[lasti]) {
							checkConflict(new Conflict(accept, (Dfm) follows[lasti], (Dfm) follows[i]));
						}
					} else {
						last = accept;
						lasti = i;
					}
				}
			}
		}

		// once more for removal

		for (Dfm dfm : dfms) {
			// record conflicts
			Object[] accepts = dfm.getKeys();
			Object[] follows = dfm.getValues();
			boolean remove = false;
			if (accepts != null) {
				boolean[] removes = new boolean[accepts.length];
				String last = null;
				for (int i = 0, lasti = -1; i < accepts.length; i++) {
					String accept = accepts[i].toString();

					if (last != null && last == accept) {
						remove = true;
						removes[i] = true;
						if (follows[i] != follows[lasti]) {
							Dfm dfmhi = (Dfm) follows[i];
							Dfm dfmlo = (Dfm) follows[lasti];
							if (dfmhi.id < dfmlo.id) {
								Dfm tmp = dfmhi;
								dfmhi = dfmlo;
								dfmlo = tmp;
							}
							Dfm dup = duplicates.get(dfmhi);
							if (dup == null || dfmlo.id < dup.id) {
								duplicates.put(dfmhi, dfmlo);
							} else {
								duplicates.put(dfmlo, dup);
							}
						}
					} else {
						last = accept;
						lasti = i;
					}
				}

				if (remove) {
					SortedMap map = dfm.getMap();
					int i = 0;
					for (Iterator<?> iterator = map.keyIterator(); iterator.hasNext(); i++) {
						iterator.next();
						if (removes[i]) {
							iterator.remove();
						}
					}
					SortedMapFactory.freeMap(map);
				}
			}
		}
	}

	/**
	 * Check conflict and report ambiguity.
	 *
	 * @param conflict
	 *            Potential ambiguity
	 */
	private void checkConflict(Conflict conflict) throws ParseError {
		if (conflict.dfm1.accepting != conflict.dfm2.accepting) {
			reportError(conflict.name);
		}
		Object[] accept1 = conflict.dfm1.getKeys();
		Object[] accept2 = conflict.dfm2.getKeys();
		if ((accept1 == null) != (accept2 == null)) {
			reportError(conflict.name);
		}
		if (accept1 != null) {
			if (accept1.length != accept2.length) {
				reportError(conflict.name);
			}
			for (int j = 0; j < accept2.length; j++) {
				if (accept1[j] != accept2[j]) {
					reportError(conflict.name);
				}
			}
		}
	}

	/**
	 * Recursive parse that visits every node reachable from the start symbol.
	 */
	private Dfm parseStart(NfmNode start, NfmNode accept) {
		// mark the start node
		Dfm result = Dfm.dfm(false);
		start.dfm = result;

		// we can minimize alias dfms by marking all starting transfer links
		while (start.next1 != null && start.next2 == null && start.symbol == null) {
			start = start.next1;
			start.dfm = result;
		}

		Dfm parsed = parse(1, start, accept);
		result.merge(parsed);

		Dfm.free(parsed);

		return result;
	}

	private void parseNext(int mark, Dfm result, NfmNode start, NfmNode accept) {
		Dfm parsed = parse(mark + 1, start, accept);
		result.merge(parsed);

		Dfm.free(parsed);
	}

	/**
	 * Recursive parse that visits every node reachable from the start symbol.
	 */
	private Dfm parse(int mark, NfmNode start, NfmNode accept) {

		// eliminate useless recursion (note that accept node has no branches)
		while (start.next1 != null && start.next2 == null && start.symbol == null) {
			start = start.next1;
		}

		// if we reached the accept node, return an empty dfm that accepts
		if (start == accept) {
			return Dfm.dfm(true);
		}

		// for a symbol, construct a dfm that accepts the symbol
		if (start.symbol != null) {
			Dfm nextdfm = null;
			NfmNode next = start.next1, snext = next;
			while (snext.dfm == null && snext.next1 != null && snext.next2 == null && snext.symbol == null) {
				snext = snext.next1;
			}
			if (snext.dfm != null) {
				for (NfmNode n = next; n != snext; n = n.next1) {
					n.dfm = snext.dfm;
				}
				nextdfm = snext.dfm;
			} else {
				nextdfm = Dfm.dfm(false);
				snext.dfm = nextdfm;
				for (NfmNode n = next; n != snext; n = n.next1) {
					n.dfm = nextdfm;
				}
				parseNext(mark, nextdfm, snext, accept);
			}
			Dfm dfm = Dfm.dfm(start.symbol, nextdfm);
			return dfm;
		}

		// otherwise, follow both branches and return the combined result
		Dfm dfm1 = null, dfm2 = null;
		int saveMark;
		if (start.next1 != null && start.next1.mark != mark) {
			saveMark = start.next1.mark;
			start.next1.mark = mark;
			dfm1 = parse(mark, start.next1, accept);
			start.next1.mark = saveMark;
		}
		if (start.next2 != null && start.next2.mark != mark) {
			saveMark = start.next2.mark;
			start.next2.mark = mark;
			dfm2 = parse(mark, start.next2, accept);
			start.next2.mark = saveMark;
		}

		if (dfm2 != null) {
			if (dfm1 != null) {
				dfm1.merge(dfm2);
			} else {
				dfm1 = dfm2;
			}
		}
		return dfm1;
	}

	private static class Conflict {
		public String name;
		public Dfm dfm1, dfm2;

		public Conflict(String name, Dfm dfm1, Dfm dfm2) {
			this.name = name;
			this.dfm1 = dfm1;
			this.dfm2 = dfm2;
		}

		@Override
		public int hashCode() {
			return dfm1.hashCode() + dfm2.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Conflict other)) {
				return false;
			}
			return (dfm1 == other.dfm1 && dfm2 == other.dfm2) || (dfm1 == other.dfm2 && dfm2 == other.dfm1);
		}
	}

}
