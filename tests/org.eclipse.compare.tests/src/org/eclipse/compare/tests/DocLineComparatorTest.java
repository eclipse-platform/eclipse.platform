/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class DocLineComparatorTest extends TestCase {
	
	public DocLineComparatorTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		// empty
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRangesEqual() {
		IDocument doc1= new Document();
		doc1.set("if (s.strip))"); //$NON-NLS-1$
		
		IDocument doc2= new Document();
		doc2.set("if (s.strip)"); //$NON-NLS-1$
		
		IRangeComparator comp1= new DocLineComparator(doc1, null, true);
		IRangeComparator comp2= new DocLineComparator(doc2, null, true);

		Assert.assertFalse(comp1.rangesEqual(0, comp2, 0));
	}

	public void testWhitespaceAtEnd() {
		IDocument doc1= new Document();
		doc1.set("if (s.strip))"); //$NON-NLS-1$
		
		IDocument doc2= new Document();
		doc2.set("if (s.strip))   "); //$NON-NLS-1$
		
		IRangeComparator comp1= new DocLineComparator(doc1, null, true);
		IRangeComparator comp2= new DocLineComparator(doc2, null, true);

		Assert.assertTrue(comp1.rangesEqual(0, comp2, 0));
	}

	public void testOneCompareFilter() {
		IDocument doc1 = new Document();
		doc1.set("if (s.strip))"); //$NON-NLS-1$

		IDocument doc2 = new Document();
		doc2.set("IF (S.stRIp))"); //$NON-NLS-1$

		IDocument doc3 = new Document();
		doc3.set("IF (S.stRIp))   "); //$NON-NLS-1$

		ICompareFilter filter = new ICompareFilter() {

			public void setInput(Object input, Object ancestor, Object left,
					Object right) {
				// EMPTY
			}

			public IRegion[] getFilteredRegions(HashMap lineComparison) {
				return new IRegion[] { new Region(0, 2), new Region(4, 1),
						new Region(8, 2) };
			}

			public boolean isEnabledInitially() {
				return false;
			}

			public boolean canCacheFilteredRegions() {
				return false;
			}
		};

		IRangeComparator comp1 = new DocLineComparator(doc1, null, false,
				new ICompareFilter[] { filter }, 'L');
		IRangeComparator comp2 = new DocLineComparator(doc2, null, false,
				new ICompareFilter[] { filter }, 'R');
		Assert.assertTrue(comp1.rangesEqual(0, comp2, 0));

		IRangeComparator comp3 = new DocLineComparator(doc1, null, true,
				new ICompareFilter[] { filter }, 'L');
		IRangeComparator comp4 = new DocLineComparator(doc3, null, true,
				new ICompareFilter[] { filter }, 'R');
		Assert.assertTrue(comp3.rangesEqual(0, comp4, 0));

		IRangeComparator comp5 = new DocLineComparator(doc1, null, false,
				new ICompareFilter[] { filter }, 'L');
		IRangeComparator comp6 = new DocLineComparator(doc3, null, false,
				new ICompareFilter[] { filter }, 'R');
		Assert.assertFalse(comp5.rangesEqual(0, comp6, 0));
	}

	public void testMultipleCompareFilters() {
		IDocument doc1 = new Document();
		doc1.set("if (s.strip))"); //$NON-NLS-1$

		IDocument doc2 = new Document();
		doc2.set("IF (S.stRIp))"); //$NON-NLS-1$

		ICompareFilter filter1 = new ICompareFilter() {

			public void setInput(Object input, Object ancestor, Object left,
					Object right) {
				// EMPTY
			}

			public IRegion[] getFilteredRegions(HashMap lineComparison) {
				return new IRegion[] { new Region(0, 2) };
			}

			public boolean isEnabledInitially() {
				return false;
			}

			public boolean canCacheFilteredRegions() {
				return false;
			}
		};

		ICompareFilter filter2 = new ICompareFilter() {

			public void setInput(Object input, Object ancestor, Object left,
					Object right) {
				// EMPTY
			}

			public IRegion[] getFilteredRegions(HashMap lineComparison) {
				return new IRegion[] { new Region(4, 1) };
			}

			public boolean isEnabledInitially() {
				return false;
			}

			public boolean canCacheFilteredRegions() {
				return false;
			}
		};

		ICompareFilter filter3 = new ICompareFilter() {

			public void setInput(Object input, Object ancestor, Object left,
					Object right) {
				// EMPTY
			}

			public IRegion[] getFilteredRegions(HashMap lineComparison) {
				return new IRegion[] { new Region(8, 2) };
			}

			public boolean isEnabledInitially() {
				return false;
			}

			public boolean canCacheFilteredRegions() {
				return false;
			}
		};

		IRangeComparator comp1 = new DocLineComparator(doc1, null, false,
				new ICompareFilter[] { filter1, filter2, filter3 }, 'L');
		IRangeComparator comp2 = new DocLineComparator(doc2, null, false,
				new ICompareFilter[] { filter1, filter2, filter3 }, 'R');
		Assert.assertTrue(comp1.rangesEqual(0, comp2, 0));

		IRangeComparator comp3 = new DocLineComparator(doc1, null, false,
				new ICompareFilter[] { filter2, filter3 }, 'L');
		IRangeComparator comp4 = new DocLineComparator(doc2, null, false,
				new ICompareFilter[] { filter2, filter3 }, 'R');
		Assert.assertFalse(comp3.rangesEqual(0, comp4, 0));
	}
	
	public void testWhitespace() {
		IDocument[] docs = new IDocument[6];
		docs[0] = new Document();
		docs[1] = new Document();
		docs[2] = new Document();
		docs[3] = new Document();
		docs[4] = new Document();
		docs[5] = new Document();
		
		docs[0].set("if (s.strip))\r\n");//$NON-NLS-1$
		docs[1].set("if (s.strip))\n");  //$NON-NLS-1$
		docs[2].set("if (s .strip))\n"); //$NON-NLS-1$
		docs[3].set("if (s.str ip))\r"); //$NON-NLS-1$
		docs[4].set("if (s.strip))");    //$NON-NLS-1$
		docs[5].set("if (s.stri p))");   //$NON-NLS-1$
		
		ICompareFilter[][] filters = new ICompareFilter[3][];
		filters[0] = null;
		filters[1] = new ICompareFilter[]{
				new ICompareFilter() {

					public void setInput(Object input, Object ancestor, Object left,
							Object right) {
						// EMPTY
					}
		
					public IRegion[] getFilteredRegions(HashMap lineComparison) {
						return new IRegion[] { new Region(0, 2) };
					}
		
					public boolean isEnabledInitially() {
						return false;
					}
		
					public boolean canCacheFilteredRegions() {
						return true; // cache-able
					}
				}
		};
		
		filters[2] = new ICompareFilter[]{
				new ICompareFilter() {

					public void setInput(Object input, Object ancestor, Object left,
							Object right) {
						// EMPTY
					}
		
					public IRegion[] getFilteredRegions(HashMap lineComparison) {
						return new IRegion[] { new Region(0, 2) };
					}
		
					public boolean isEnabledInitially() {
						return false;
					}
		
					public boolean canCacheFilteredRegions() {
						return false; // not cache-able
					}
				}
		};

		IRangeComparator l, r;
		for (int i=0;i<docs.length;i++)
			for (int j=i+1;j<docs.length;j++)
				for (int k=0;k<filters.length;k++) {
					l = new DocLineComparator(docs[i], null, false, filters[k], 'L');
					r = new DocLineComparator(docs[j], null, false, filters[k], 'R');
					Assert.assertFalse(l.rangesEqual(0, r, 0));
					
					l = new DocLineComparator(docs[i], null, true, filters[k], 'L');
					r = new DocLineComparator(docs[j], null, true, filters[k], 'R');
					Assert.assertTrue(l.rangesEqual(0, r, 0));
			}
	}

	public void testEmpty() {
		IDocument doc1= new Document();
		doc1.set(""); //$NON-NLS-1$
		
		IDocument doc2= new Document();
		doc2.set("    "); //$NON-NLS-1$
		
		IRangeComparator comp1= new DocLineComparator(doc1, null, true);
		IRangeComparator comp2= new DocLineComparator(doc2, null, true);

		Assert.assertTrue(comp1.rangesEqual(0, comp2, 0));
	}

	public void testNoContent() {
		IDocument doc= new Document();
		
		IRangeComparator comp1= new DocLineComparator(doc, null, true);
		IRangeComparator comp2= new DocLineComparator(doc, new Region(0, doc.getLength()), true);

		Assert.assertTrue(comp1.rangesEqual(0, comp2, 0));
		Assert.assertEquals(comp1.getRangeCount(), comp2.getRangeCount());
		Assert.assertEquals(1, comp2.getRangeCount());
	}
	
	public void testOneLine() {
		IDocument doc = new Document();
		doc.set("line1"); //$NON-NLS-1$
		
		IRangeComparator comp1= new DocLineComparator(doc, null, true);
		IRangeComparator comp2= new DocLineComparator(doc, new Region(0, doc.getLength()), true);

		Assert.assertEquals(comp1.getRangeCount(), comp2.getRangeCount());
		Assert.assertEquals(1, comp2.getRangeCount());
	}
	
	public void testTwoLines() {
		IDocument doc = new Document();
		doc.set("line1\nline2"); //$NON-NLS-1$
		
		IRangeComparator comp1= new DocLineComparator(doc, null, true);
		IRangeComparator comp2= new DocLineComparator(doc, new Region(0, doc.getLength()), true);

		Assert.assertEquals(comp1.getRangeCount(), comp2.getRangeCount());
		Assert.assertEquals(2, comp2.getRangeCount());
		
		IRangeComparator comp3= new DocLineComparator(doc, new Region(0, "line1".length()), true);
		Assert.assertEquals(1, comp3.getRangeCount());
		
		comp3= new DocLineComparator(doc, new Region(0, "line1".length()+1), true);
		Assert.assertEquals(2, comp3.getRangeCount()); // two lines
	}
	
	public void testBug259422() {
		IDocument doc = new Document();
		doc.set(""); //$NON-NLS-1$
		
		IRangeComparator comp1= new DocLineComparator(doc, null, true);
		IRangeComparator comp2= new DocLineComparator(doc, new Region(0, doc.getLength()), true);

		Assert.assertEquals(comp1.getRangeCount(), comp2.getRangeCount());
	}
	
}
