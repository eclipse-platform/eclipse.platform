/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.StructureCreator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class StructureCreatorTest extends TestCase {

	public StructureCreatorTest() {
		super();
	}

	public StructureCreatorTest(String name) {
		super(name);
	}

	public void testIgnoreWhitespace() {
		IDocument[] docs = new IDocument[15];
		docs[0] = new Document();
		docs[1] = new Document();
		docs[2] = new Document();
		docs[3] = new Document();
		docs[4] = new Document();
		docs[5] = new Document();
		docs[6] = new Document();
		docs[7] = new Document();
		docs[8] = new Document();
		docs[9] = new Document();
		docs[10] = new Document();
		docs[11] = new Document();
		docs[12] = new Document();
		docs[13] = new Document();
		docs[14] = new Document();

		docs[0].set("ABCDEF"); //$NON-NLS-1$
		docs[1].set("ABC DEF"); //$NON-NLS-1$
		docs[2].set("ABC\r\nDEF"); //$NON-NLS-1$
		docs[3].set("ABC\r\nDEF  "); //$NON-NLS-1$
		docs[4].set("\r\nABC\r\nDEF"); //$NON-NLS-1$
		docs[5].set("   \r\nABC\r\nDEF"); //$NON-NLS-1$
		docs[6].set("\r\nABC\r\nDEF\r\n"); //$NON-NLS-1$
		docs[7].set("\r\nA BC\r\nDE F\r\n"); //$NON-NLS-1$
		docs[8].set("ABC\r\n\r\nDEF"); //$NON-NLS-1$
		docs[9].set("ABC\nDEF"); //$NON-NLS-1$
		docs[10].set("ABC\nDEF   "); //$NON-NLS-1$
		docs[11].set("ABC\nDEF\n"); //$NON-NLS-1$
		docs[12].set("\nABCDEF"); //$NON-NLS-1$
		docs[13].set(" \nAB CD EF"); //$NON-NLS-1$
		docs[14].set("\nABC\nDEF\n"); //$NON-NLS-1$

		ICompareFilter[][] filters = new ICompareFilter[3][];
		filters[0] = null;
		filters[1] = new ICompareFilter[] { new ICompareFilter() {

			public void setInput(Object input, Object ancestor, Object left,
					Object right) {
				// EMPTY
			}

			public IRegion[] getFilteredRegions(HashMap lineComparison) {
				String line = lineComparison.get(ICompareFilter.THIS_LINE)
						.toString();
				int index = line.indexOf("A");
				if (index > -1)
					return new IRegion[] { new Region(index, 1) };
				return null;
			}

			public boolean isEnabledInitially() {
				return false;
			}

			public boolean canCacheFilteredRegions() {
				return true; // cache-able
			}
		} };

		filters[2] = new ICompareFilter[] { new ICompareFilter() {

			public void setInput(Object input, Object ancestor, Object left,
					Object right) {
				// EMPTY
			}

			public IRegion[] getFilteredRegions(HashMap lineComparison) {
				String line = lineComparison.get(ICompareFilter.THIS_LINE)
						.toString();
				int index = line.indexOf("E");
				if (index > -1)
					return new IRegion[] { new Region(index, 1) };
				return null;
			}

			public boolean isEnabledInitially() {
				return false;
			}

			public boolean canCacheFilteredRegions() {
				return false; // not cache-able
			}
		} };

		StructureCreator creator = new StructureCreator() {
			private Pattern whitespace = Pattern.compile("\\s+");
			private Matcher matcher = null;

			public String getName() {
				return "NAME";
			}

			public String getContents(Object node, boolean ignoreWhitespace) {
				DocumentRangeNode drn = (DocumentRangeNode) node;
				String retval = null;
				try {
					retval = drn.getDocument().get(drn.getRange().getOffset(),
							drn.getRange().getLength());
					if (ignoreWhitespace) {
						if (matcher == null)
							matcher = whitespace.matcher(retval);
						else
							matcher.reset(retval);
						retval = matcher.replaceAll("");
					}
				} catch (BadLocationException ble) {
					assertNull(ble);
				}
				return retval;
			}

			protected IStructureComparator createStructureComparator(
					Object element, IDocument document,
					ISharedDocumentAdapter sharedDocumentAdapter,
					IProgressMonitor monitor) throws CoreException {
				return new DocumentRangeNode(1, "ID", document, 0,
						document.getLength());
			}

		};
		DocumentRangeNode l, r;
		for (int i = 0; i < docs.length; i++)
			for (int j = i + 1; j < docs.length; j++)
				for (int k = 0; k < filters.length; k++) {

					l = new DocumentRangeNode(1, "ID", docs[i], 0,
							docs[i].getLength());
					r = new DocumentRangeNode(1, "ID", docs[j], 0,
							docs[j].getLength());
					creator.contentsEquals(l, 'L', r, 'R', true, filters[k]);
					Assert.assertFalse(creator.contentsEquals(l, 'L', r, 'R',
							false, filters[k]));
					Assert.assertTrue(creator.contentsEquals(l, 'L', r, 'R',
							true, filters[k]));
				}
	}
}