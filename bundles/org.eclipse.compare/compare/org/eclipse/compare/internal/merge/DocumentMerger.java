/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.merge;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.CompareContentViewerSwitchingPane;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.core.LCS;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.compare.structuremergeviewer.Differencer;

/**
 * A document merger manages the differences between two documents
 * for either a 2-way or 3-way comparison. 
 * <p>
 * This class should not have any UI dependencies.
 */
public class DocumentMerger {

	private static final String DIFF_RANGE_CATEGORY = CompareUIPlugin.PLUGIN_ID + ".DIFF_RANGE_CATEGORY"; //$NON-NLS-1$
	
	/** Selects between smartTokenDiff and mergingTokenDiff */
	private static final boolean USE_MERGING_TOKEN_DIFF= false;
	
	/** if true copying conflicts from one side to other concatenates both sides */
	private static final boolean APPEND_CONFLICT= true;
	
	/** All diffs for calculating scrolling position (includes line ranges without changes) */
	private ArrayList fAllDiffs;
	/** Subset of above: just real differences. */
	private ArrayList fChangeDiffs;
	
	private final boolean fLeftIsLocal;

	private IDocumentMergerInput fInput;
	
	/**
	 * Interface that defines that input to the document merge process
	 */
	public interface IDocumentMergerInput {

		IDocument getDocument(char contributor);

		Position getRegion(char contributor);

		boolean isIgnoreAncestor();

		boolean isThreeWay();

		CompareConfiguration getCompareConfiguration();

		ITokenComparator createTokenComparator(String s);
		
		boolean isHunkOnLeft();

		int getHunkStart();

		boolean isPatchHunk();

		boolean isShowPseudoConflicts();

		boolean isPatchHunkOk();
	}
	
	public class Diff {
		/** character range in ancestor document */
		Position fAncestorPos;
		/** character range in left document */
		Position fLeftPos;
		/** character range in right document */
		Position fRightPos;
		/** if this is a TokenDiff fParent points to the enclosing LineDiff */
		Diff fParent;	
		/** if Diff has been resolved */
		boolean fResolved;
		int fDirection;
		boolean fIsToken= false;
		/** child token diffs */
		ArrayList fDiffs;
		boolean fIsWhitespace= false;

		/*
		 * Create Diff from two ranges and an optional parent diff.
		 */
		Diff(Diff parent, int dir, IDocument ancestorDoc, Position aRange, int ancestorStart, int ancestorEnd,
							 IDocument leftDoc, Position lRange, int leftStart, int leftEnd,
							 IDocument rightDoc, Position rRange, int rightStart, int rightEnd) {
			fParent= parent != null ? parent : this;
			fDirection= dir;
			
			fLeftPos= createPosition(leftDoc, lRange, leftStart, leftEnd);
			fRightPos= createPosition(rightDoc, rRange, rightStart, rightEnd);
			if (ancestorDoc != null)
				fAncestorPos= createPosition(ancestorDoc, aRange, ancestorStart, ancestorEnd);
		}
		
		public Position getPosition(char type) {
			switch (type) {
			case MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR:
				return fAncestorPos;
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				return fLeftPos;
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				return fRightPos;
			}
			return null;
		}
		
		boolean isInRange(char type, int pos) {
			Position p= getPosition(type);
			return (pos >= p.offset) && (pos < (p.offset+p.length));
		}
		
		public String changeType() {
			boolean leftEmpty= fLeftPos.length == 0;
			boolean rightEmpty= fRightPos.length == 0;
			
			if (fDirection == RangeDifference.LEFT) {
				if (!leftEmpty && rightEmpty)
					return CompareMessages.TextMergeViewer_changeType_addition; 
				if (leftEmpty && !rightEmpty)
					return CompareMessages.TextMergeViewer_changeType_deletion; 
			} else {
				if (leftEmpty && !rightEmpty)
					return CompareMessages.TextMergeViewer_changeType_addition; 
				if (!leftEmpty && rightEmpty)
					return CompareMessages.TextMergeViewer_changeType_deletion; 
			}
			return CompareMessages.TextMergeViewer_changeType_change; 
		}
		
		public Image getImage() {
			int code= Differencer.CHANGE;
			switch (fDirection) {
			case RangeDifference.RIGHT:
				code+= Differencer.LEFT;
				break;
			case RangeDifference.LEFT:
				code+= Differencer.RIGHT;
				break;
			case RangeDifference.ANCESTOR:
			case RangeDifference.CONFLICT:
				code+= Differencer.CONFLICTING;
				break;
			}
			if (code != 0)
				return getCompareConfiguration().getImage(code);
			return null;
		}
		
		Position createPosition(IDocument doc, Position range, int start, int end) {
			try {
				int l= end-start;
				if (range != null) {
					int dl= range.length;
					if (l > dl)
						l= dl;					
				} else {
					int dl= doc.getLength();
					if (start+l > dl)
						l= dl-start;
				}
					
				Position p= null;
				try {
					p= new Position(start, l);
				} catch (RuntimeException ex) {
					p= new Position(0, 0);
				}
				
				try {
					doc.addPosition(DIFF_RANGE_CATEGORY, p);
				} catch (BadPositionCategoryException ex) {
					// silently ignored
				}
				return p;
			} catch (BadLocationException ee) {
				// silently ignored
			}
			return null;
		}

		void add(Diff d) {
			if (fDiffs == null)
				fDiffs= new ArrayList();
			fDiffs.add(d);
		}
		
		public boolean isDeleted() {
			if (fAncestorPos != null && fAncestorPos.isDeleted())
				return true;
			return fLeftPos.isDeleted() || fRightPos.isDeleted();
		}
		
		void setResolved(boolean r) {
			fResolved= r;
			if (r)
				fDiffs= null;
		}

		public boolean isResolved() {
			if (!fResolved && fDiffs != null) {
				Iterator e= fDiffs.iterator();
				while (e.hasNext()) {
					Diff d= (Diff) e.next();
					if (!d.isResolved())
						return false;
				}
				return true;
			}
			return fResolved;
		}
		
//		private boolean isIncoming() {
//			switch (fDirection) {
//			case RangeDifference.RIGHT:
//				if (fLeftIsLocal)
//					return true;
//				break;
//			case RangeDifference.LEFT:
//				if (!fLeftIsLocal)
//					return true;
//				break;
//			}
//			return false;
//		}
		
		public boolean isIncomingOrConflicting() {
			switch (fDirection) {
			case RangeDifference.RIGHT:
				if (fLeftIsLocal)
					return true;
				break;
			case RangeDifference.LEFT:
				if (!fLeftIsLocal)
					return true;
				break;
			case RangeDifference.CONFLICT:
				return true;
			}
			return false;
		}
		
//		private boolean isUnresolvedIncoming() {
//			if (fResolved)
//				return false;
//			return isIncoming();
//		}
		
		public boolean isUnresolvedIncomingOrConflicting() {
			if (fResolved)
				return false;
			return isIncomingOrConflicting();
		}
				
		Position getPosition(int contributor) {
			if (contributor == MergeViewerContentProvider.LEFT_CONTRIBUTOR)
				return fLeftPos;
			if (contributor == MergeViewerContentProvider.RIGHT_CONTRIBUTOR)
				return fRightPos;
			if (contributor == MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR)
				return fAncestorPos;
			return null;
		}
		
		/*
		 * Returns true if given character range overlaps with this Diff.
		 */
		public boolean overlaps(int contributor, int start, int end, int docLength) {
			Position h= getPosition(contributor);
			if (h != null) {
				int ds= h.getOffset();
				int de= ds + h.getLength();
				if ((start < de) && (end >= ds))
					return true;
				if ((start == docLength) && (start <= de) && (end >= ds))
					return true;
			}
			return false;
		}
				
		public int getMaxDiffHeight() {
			Point region= new Point(0, 0);
			int h= getLineRange(getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR), fLeftPos, region).y;
			if (isThreeWay())
				h= Math.max(h, getLineRange(getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR), fAncestorPos, region).y);
			return Math.max(h, getLineRange(getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR), fRightPos, region).y);
		}
		
		public int getAncestorHeight() {
			Point region= new Point(0, 0);			
			return getLineRange(getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR), fAncestorPos, region).y;
		}

		public int getLeftHeight() {
			Point region= new Point(0, 0);			
			return getLineRange(getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR), fLeftPos, region).y;
		}

		public int getRightHeight() {
			Point region= new Point(0, 0);			
			return getLineRange(getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR), fRightPos, region).y;
		}

		public Diff[] getChangeDiffs(int contributor, IRegion region) {
			if (fDiffs != null && intersectsRegion(contributor, region)) {
				List result = new ArrayList();
				for (Iterator iterator = fDiffs.iterator(); iterator.hasNext();) {
					Diff diff = (Diff) iterator.next();
					if (diff.intersectsRegion(contributor, region)) {
						result.add(diff);
					}
				}
				return (Diff[]) result.toArray(new Diff[result.size()]);
			}
			return new Diff[0];
		}

		private boolean intersectsRegion(int contributor, IRegion region) {
			Position p = getPosition(contributor);
			if (p != null)
				return p.overlapsWith(region.getOffset(), region.getLength());
			return false;
		}

		public boolean hasChildren() {
			return fDiffs != null && !fDiffs.isEmpty();
		}

		public int getKind() {
			return fDirection;
		}

		public boolean isToken() {
			return fIsToken;
		}

		public Diff getParent() {
			return fParent;
		}

		public Iterator childIterator() {
			if (fDiffs == null)
				return new ArrayList().iterator();
			return fDiffs.iterator();
		}
	}
	
	public DocumentMerger(IDocumentMergerInput input) {
		this.fInput = input;
		fLeftIsLocal= Utilities.getBoolean(getCompareConfiguration(), "LEFT_IS_LOCAL", false); //$NON-NLS-1$
	}
	
	/**
	 * Perform a two level 2- or 3-way diff.
	 * The first level is based on line comparison, the second level on token comparison.
	 * @throws CoreException 
	 */
	public void doDiff() throws CoreException {
						
		fChangeDiffs= new ArrayList();
		IDocument lDoc = getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		IDocument rDoc = getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		
		if (lDoc == null || rDoc == null)
			return;
			
		Position lRegion= getRegion(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		Position rRegion= getRegion(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		
		IDocument aDoc = null;
		Position aRegion= null;
		if (isThreeWay() && !isIgnoreAncestor()) {
			aDoc= getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
			aRegion= getRegion(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
		}
		
		resetPositions(lDoc);
		resetPositions(rDoc);
		resetPositions(aDoc);
		
		boolean ignoreWhiteSpace= isIgnoreWhitespace();		
		ICompareFilter[] compareFilters = getCompareFilters();

		DocLineComparator sright = new DocLineComparator(rDoc,
				toRegion(rRegion), ignoreWhiteSpace, compareFilters,
				MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		DocLineComparator sleft = new DocLineComparator(lDoc,
				toRegion(lRegion), ignoreWhiteSpace, compareFilters,
				MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		DocLineComparator sancestor = null;
		if (aDoc != null) {
			sancestor = new DocLineComparator(aDoc, toRegion(aRegion),
					ignoreWhiteSpace, compareFilters,
					MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
			/*if (isPatchHunk()) {
				if (isHunkOnLeft()) {
					sright= new DocLineComparator(aDoc, toRegion(aRegion), ignoreWhiteSpace);
				} else {
					sleft= new DocLineComparator(aDoc, toRegion(aRegion), ignoreWhiteSpace);
				}
			}*/
		}

		final Object[] result= new Object[1];
		final DocLineComparator sa= sancestor, sl= sleft, sr= sright;
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				monitor.beginTask(CompareMessages.DocumentMerger_0, maxWork(sa, sl, sr));
				try {
					result[0]= RangeDifferencer.findRanges(monitor, sa, sl, sr);
				} catch (OutOfMemoryError ex) {
					System.gc();
					throw new InvocationTargetException(ex);
				}
				if (monitor.isCanceled())	{ // canceled
					throw new InterruptedException();
				}
				monitor.done();
			}
		};
		
		RangeDifference[] e= null;
		try {
			getCompareConfiguration().getContainer().run(true, true, runnable);
			e= (RangeDifference[]) result[0];
		} catch (InvocationTargetException ex) {
			// we create a NOCHANGE range for the whole document
			Diff diff= new Diff(null, RangeDifference.NOCHANGE,
				aDoc, aRegion, 0, aDoc != null ? aDoc.getLength() : 0,
				lDoc, lRegion, 0, lDoc.getLength(),
				rDoc, rRegion, 0, rDoc.getLength());	
				
			fAllDiffs = new ArrayList();
			fAllDiffs.add(diff);
			throw new CoreException(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, CompareMessages.DocumentMerger_1, ex.getTargetException()));
		} catch (InterruptedException ex) {
			// we create a NOCHANGE range for the whole document
			Diff diff= new Diff(null, RangeDifference.NOCHANGE,
				aDoc, aRegion, 0, aDoc != null ? aDoc.getLength() : 0,
				lDoc, lRegion, 0, lDoc.getLength(),
				rDoc, rRegion, 0, rDoc.getLength());	
				
			fAllDiffs = new ArrayList();
			fAllDiffs.add(diff);
			return;
		}

		if (isCapped(sa, sl, sr))
			fInput.getCompareConfiguration().setProperty(
					CompareContentViewerSwitchingPane.OPTIMIZED_ALGORITHM_USED,
					new Boolean(true));
		else
			fInput.getCompareConfiguration().setProperty(
					CompareContentViewerSwitchingPane.OPTIMIZED_ALGORITHM_USED,
					new Boolean(false));

		ArrayList newAllDiffs = new ArrayList();
		for (int i= 0; i < e.length; i++) {
			RangeDifference es= e[i];
			
			int ancestorStart= 0;
			int ancestorEnd= 0;
			if (sancestor != null) {
				ancestorStart= sancestor.getTokenStart(es.ancestorStart());
				ancestorEnd= getTokenEnd2(sancestor, es.ancestorStart(), es.ancestorLength());
			}
			
			int leftStart= sleft.getTokenStart(es.leftStart());
			int leftEnd= getTokenEnd2(sleft, es.leftStart(), es.leftLength());
			
			int rightStart= sright.getTokenStart(es.rightStart());
			int rightEnd= getTokenEnd2(sright, es.rightStart(), es.rightLength());

			/*if (isPatchHunk()) {
				if (isHunkOnLeft()) {
					rightStart = rightEnd = getHunkStart();
				} else {
					leftStart = leftEnd = getHunkStart();
				}
			}*/

			Diff diff= new Diff(null, es.kind(),
				aDoc, aRegion, ancestorStart, ancestorEnd,
				lDoc, lRegion, leftStart, leftEnd,
				rDoc, rRegion, rightStart, rightEnd);	
			
			newAllDiffs.add(diff);	// remember all range diffs for scrolling
	
			if (isPatchHunk()) {
				if (useChange(diff)) {
					recordChangeDiff(diff);
				}
			} else {
				if (ignoreWhiteSpace || useChange(es.kind())) {
					
					// Extract the string for each contributor.
					String a= null;
					if (sancestor != null)
						a= extract2(aDoc, sancestor, es.ancestorStart(), es.ancestorLength());
					String s= extract2(lDoc, sleft, es.leftStart(), es.leftLength());
					String d= extract2(rDoc, sright, es.rightStart(), es.rightLength());
				
					// Indicate whether all contributors are whitespace
					if (ignoreWhiteSpace 
							&& (a == null || a.trim().length() == 0) 
							&& s.trim().length() == 0 
							&& d.trim().length() == 0) {
						diff.fIsWhitespace= true;
					}
					
					// If the diff is of interest, record it and generate the token diffs
					if (useChange(diff)) {
						recordChangeDiff(diff);
						if (s.length() > 0 && d.length() > 0) {
							if (a == null && sancestor != null)
								a= extract2(aDoc, sancestor, es.ancestorStart(), es.ancestorLength());
							if (USE_MERGING_TOKEN_DIFF)
								mergingTokenDiff(diff, aDoc, a, rDoc, d, lDoc, s);
							else
								simpleTokenDiff(diff, aDoc, a, rDoc, d, lDoc, s);
						}
					}
				}
			}
		}
		fAllDiffs = newAllDiffs;
	}

	private boolean isCapped(DocLineComparator ancestor,
			DocLineComparator left, DocLineComparator right) {
		if (isCappingDisabled())
			return false;
		int aLength = ancestor == null? 0 : ancestor.getRangeCount();
		int lLength = left.getRangeCount();
		int rLength = right.getRangeCount();
		if ((double) aLength * (double) lLength > LCS.TOO_LONG
				|| (double) aLength * (double) rLength > LCS.TOO_LONG
				|| (double) lLength * (double) rLength > LCS.TOO_LONG)
			return true;
		return false;
	}

	public Diff findDiff(char type, int pos) throws CoreException {
		
		IDocument aDoc= null;
		IDocument lDoc= getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		IDocument rDoc= getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		if (lDoc == null || rDoc == null)
			return null;
			
		Position aRegion= null;
		Position lRegion= null;
		Position rRegion= null;
				
		boolean threeWay= isThreeWay();
		
		if (threeWay && !isIgnoreAncestor())
			aDoc= getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);

		boolean ignoreWhiteSpace= isIgnoreWhitespace();
		ICompareFilter[] compareFilters = getCompareFilters();

		DocLineComparator sright= new DocLineComparator(rDoc, toRegion(rRegion), ignoreWhiteSpace, compareFilters, MergeViewerContentProvider.RIGHT_CONTRIBUTOR);		
		DocLineComparator sleft= new DocLineComparator(lDoc, toRegion(lRegion), ignoreWhiteSpace, compareFilters, MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		DocLineComparator sancestor= null;
		if (aDoc != null)
			sancestor= new DocLineComparator(aDoc, toRegion(aRegion), ignoreWhiteSpace, compareFilters, MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
			
		final Object[] result= new Object[1];
		final DocLineComparator sa= sancestor, sl= sleft, sr= sright;
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				monitor.beginTask(CompareMessages.DocumentMerger_2, maxWork(sa, sl, sr));
				try {
					result[0]= RangeDifferencer.findRanges(monitor, sa, sl, sr);
				} catch (OutOfMemoryError ex) {
					System.gc();
					throw new InvocationTargetException(ex);
				}
				if (monitor.isCanceled())	{ // canceled
					throw new InterruptedException();
				}
				monitor.done();
			}
		};
		IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
		
		RangeDifference[] e= null;
		try {
			progressService.run(true, true, runnable);
			e= (RangeDifference[]) result[0];
		} catch (InvocationTargetException ex) {
			throw new CoreException(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, CompareMessages.DocumentMerger_3, ex.getTargetException()));
		} catch (InterruptedException ex) {
			// 
		}
					
		if (e != null) {
			for (int i= 0; i < e.length; i++) {
				RangeDifference es= e[i];
				
				int kind= es.kind();
				
				int ancestorStart= 0;
				int ancestorEnd= 0;
				if (sancestor != null) {
					ancestorStart= sancestor.getTokenStart(es.ancestorStart());
					ancestorEnd= getTokenEnd2(sancestor, es.ancestorStart(), es.ancestorLength());
				}
				
				int leftStart= sleft.getTokenStart(es.leftStart());
				int leftEnd= getTokenEnd2(sleft, es.leftStart(), es.leftLength());
				
				int rightStart= sright.getTokenStart(es.rightStart());
				int rightEnd= getTokenEnd2(sright, es.rightStart(), es.rightLength());
				
				Diff diff= new Diff(null, kind,
					aDoc, aRegion, ancestorStart, ancestorEnd,
					lDoc, lRegion, leftStart, leftEnd,
					rDoc, rRegion, rightStart, rightEnd);	

				if (diff.isInRange(type, pos))
					return diff;
			}
		}
		
		return null;
	}
	
	private void recordChangeDiff(Diff diff) {
		fChangeDiffs.add(diff);	// here we remember only the real diffs
	}
	
	/*private boolean isHunkOnLeft() {
		return fInput.isHunkOnLeft();
	}

	private int getHunkStart() {
		return fInput.getHunkStart();
	}*/

	private boolean isPatchHunk() {
		return fInput.isPatchHunk();
	}

	private boolean isIgnoreWhitespace() {
		return Utilities.getBoolean(getCompareConfiguration(), CompareConfiguration.IGNORE_WHITESPACE, false);
	}

	private ICompareFilter[] getCompareFilters() {
		return Utilities.getCompareFilters(getCompareConfiguration());
	}

	private boolean isCappingDisabled() {
		return CompareUIPlugin.getDefault().getPreferenceStore().getBoolean(ComparePreferencePage.CAPPING_DISABLED);
	}

	private IDocument getDocument(char contributor) {
		return fInput.getDocument(contributor);
	}

	private Position getRegion(char contributor) {
		return fInput.getRegion(contributor);
	}

	public boolean isIgnoreAncestor() {
		return fInput.isIgnoreAncestor();
	}

	public boolean isThreeWay() {
		return fInput.isThreeWay();
	}

	/**
	 * Return the compare configuration associated with this merger.
	 * @return the compare configuration associated with this merger
	 */
	public CompareConfiguration getCompareConfiguration() {
		return fInput.getCompareConfiguration();
	}
	
	/*
	 * Returns true if kind of change should be shown.
	 */
	public boolean useChange(Diff diff) {
		if (diff.fIsWhitespace)
			return false;
		int kind = diff.getKind();
		return useChange(kind);
	}

	private boolean useChange(int kind) {
		if (kind == RangeDifference.NOCHANGE)
			return false;
		if (fInput.getCompareConfiguration().isChangeIgnored(kind))
			return false;
		if (kind == RangeDifference.ANCESTOR)
			return fInput.isShowPseudoConflicts();
		return true;
	}
	
	private int getTokenEnd(ITokenComparator tc, int start, int count) {
		if (count <= 0)
			return tc.getTokenStart(start);
		int index= start + count - 1;
		return tc.getTokenStart(index) + tc.getTokenLength(index);
	}
	
	private static int getTokenEnd2(ITokenComparator tc, int start, int length) {
		return tc.getTokenStart(start + length);
	}
	
	/**
	 * Returns the content of lines in the specified range as a String.
	 * This includes the line separators.
	 *
	 * @param doc the document from which to extract the characters
	 * @param start index of first line
	 * @param length number of lines
	 * @return the contents of the specified line range as a String
	 */
	private String extract2(IDocument doc, ITokenComparator tc, int start, int length) {
		int count= tc.getRangeCount();
		if (length > 0 && count > 0) {
			
//			
//			int startPos= tc.getTokenStart(start);
//			int endPos= startPos;
//			
//			if (length > 1)
//				endPos= tc.getTokenStart(start + (length-1));
//			endPos+= tc.getTokenLength(start + (length-1));
//				

			int startPos= tc.getTokenStart(start);
			int endPos;
			
			if (length == 1) {
				endPos= startPos + tc.getTokenLength(start);
			} else {
				endPos= tc.getTokenStart(start + length);
			}

			try {
				return doc.get(startPos, endPos - startPos);
			} catch (BadLocationException e) {
				// silently ignored
			}

		}
		return ""; //$NON-NLS-1$
	}
	
	private static IRegion toRegion(Position position) {
		if (position != null)
			return new Region(position.getOffset(), position.getLength());
		return null;
	}
	
	/*
	 * Performs a "smart" token based 3-way diff on the character range specified by the given baseDiff.
	 * It is "smart" because it tries to minimize the number of token diffs by merging them.
	 */
	private void mergingTokenDiff(Diff baseDiff, 
				IDocument ancestorDoc, String a,
				IDocument rightDoc, String d,
				IDocument leftDoc, String s) {
		ITokenComparator sa= null;
		int ancestorStart= 0;
		if (ancestorDoc != null) {
			sa= createTokenComparator(a);
			ancestorStart= baseDiff.fAncestorPos.getOffset();
		}
		
		int rightStart= baseDiff.fRightPos.getOffset();	
		ITokenComparator sm= createTokenComparator(d);
		
		int leftStart= baseDiff.fLeftPos.getOffset();
		ITokenComparator sy= createTokenComparator(s);
		
		RangeDifference[] r= RangeDifferencer.findRanges(sa, sy, sm);
		for (int i= 0; i < r.length; i++) {
			RangeDifference  es= r[i];
			// determine range of diffs in one line
			int start= i;
			int leftLine= -1;
			int rightLine= -1;
			try {
				leftLine= leftDoc.getLineOfOffset(leftStart+sy.getTokenStart(es.leftStart()));
				rightLine= rightDoc.getLineOfOffset(rightStart+sm.getTokenStart(es.rightStart()));
			} catch (BadLocationException e) {
				// silently ignored
			}
			i++;
			for (; i < r.length; i++) {
				es= r[i];
				try {
					if (leftLine != leftDoc.getLineOfOffset(leftStart+sy.getTokenStart(es.leftStart())))
						break;
					if (rightLine != rightDoc.getLineOfOffset(rightStart+sm.getTokenStart(es.rightStart())))
						break;
				} catch (BadLocationException e) {
					// silently ignored
				}
			}
			int end= i;
			
			// find first diff from left
			RangeDifference first= null;
			for (int ii= start; ii < end; ii++) {
				es= r[ii];
				if (useChange(es.kind())) {
					first= es;
					break;
				}
			}
			
			// find first diff from mine
			RangeDifference last= null;
			for (int ii= end-1; ii >= start; ii--) {
				es= r[ii];
				if (useChange(es.kind())) {
					last= es;
					break;
				}
			}
			
			if (first != null && last != null) {
				
				int ancestorStart2= 0;
				int ancestorEnd2= 0;
				if (ancestorDoc != null) {
					ancestorStart2= ancestorStart+sa.getTokenStart(first.ancestorStart());
					ancestorEnd2= ancestorStart+getTokenEnd(sa, last.ancestorStart(), last.ancestorLength());
				}
				
				int leftStart2= leftStart+sy.getTokenStart(first.leftStart());
				int leftEnd2= leftStart+getTokenEnd(sy, last.leftStart(), last.leftLength());
				
				int rightStart2= rightStart+sm.getTokenStart(first.rightStart());
				int rightEnd2= rightStart+getTokenEnd(sm, last.rightStart(), last.rightLength());
				Diff diff= new Diff(baseDiff, first.kind(),
							ancestorDoc, null, ancestorStart2, ancestorEnd2,
							leftDoc, null, leftStart2, leftEnd2,
							rightDoc, null, rightStart2, rightEnd2);	
				diff.fIsToken= true;
				baseDiff.add(diff);
			}
		}
	}
	
	/*
	 * Performs a token based 3-way diff on the character range specified by the given baseDiff.
	 */
	private void simpleTokenDiff(final Diff baseDiff,
				IDocument ancestorDoc, String a,
				IDocument rightDoc, String d,
				IDocument leftDoc, String s) {

		int ancestorStart= 0;
		ITokenComparator sa= null;
		if (ancestorDoc != null) {
			ancestorStart= baseDiff.fAncestorPos.getOffset();
			sa= createTokenComparator(a);
		}
		
		int rightStart= baseDiff.fRightPos.getOffset();	
		ITokenComparator sm= createTokenComparator(d);
		
		int leftStart= baseDiff.fLeftPos.getOffset();
		ITokenComparator sy= createTokenComparator(s);
		
		RangeDifference[] e= RangeDifferencer.findRanges(sa, sy, sm);
		for (int i= 0; i < e.length; i++) {
			RangeDifference es= e[i];
			int kind= es.kind();
			if (kind != RangeDifference.NOCHANGE) {
				
				int ancestorStart2= ancestorStart;
				int ancestorEnd2= ancestorStart;
				if (ancestorDoc != null) {
					ancestorStart2 += sa.getTokenStart(es.ancestorStart());
					ancestorEnd2 += getTokenEnd(sa, es.ancestorStart(), es.ancestorLength());
				}
				
				int leftStart2= leftStart + sy.getTokenStart(es.leftStart());
				int leftEnd2= leftStart + getTokenEnd(sy, es.leftStart(), es.leftLength());
				
				int rightStart2= rightStart + sm.getTokenStart(es.rightStart());
				int rightEnd2= rightStart + getTokenEnd(sm, es.rightStart(), es.rightLength());
				
				Diff diff= new Diff(baseDiff, kind,
						ancestorDoc, null, ancestorStart2, ancestorEnd2,
						leftDoc, null, leftStart2, leftEnd2,
						rightDoc, null, rightStart2, rightEnd2);
				
				// ensure that token diff is smaller than basediff
				int leftS= baseDiff.fLeftPos.offset;
				int leftE= baseDiff.fLeftPos.offset+baseDiff.fLeftPos.length;
				int rightS= baseDiff.fRightPos.offset;
				int rightE= baseDiff.fRightPos.offset+baseDiff.fRightPos.length;
				if (leftS != leftStart2 || leftE != leftEnd2 ||
							rightS != rightStart2 || rightE != rightEnd2) {
					diff.fIsToken= true;
					// add to base Diff
					baseDiff.add(diff);
				}
			}
		}
	}
	
	private ITokenComparator createTokenComparator(String s) {
		return fInput.createTokenComparator(s);
	}
	
	private static int maxWork(IRangeComparator a, IRangeComparator l, IRangeComparator r) {
		int ln= l.getRangeCount();
		int rn= r.getRangeCount();
		if (a != null) {
			int an= a.getRangeCount();
			return (2 * Math.max(an, ln)) + (2 * Math.max(an, rn));
		}
		return 2 * Math.max(ln, rn);
	}
	
	private void resetPositions(IDocument doc) {
		if (doc == null)
			return;
		try {
			doc.removePositionCategory(DIFF_RANGE_CATEGORY);
		} catch (BadPositionCategoryException e) {
			// Ignore
		}
		doc.addPositionCategory(DIFF_RANGE_CATEGORY);
	}

	/*
	 * Returns the start line and the number of lines which correspond to the given position.
	 * Starting line number is 0 based.
	 */
	protected Point getLineRange(IDocument doc, Position p, Point region) {
		
		if (p == null || doc == null) {
			region.x= 0;
			region.y= 0;
			return region;
		}
		
		int start= p.getOffset();
		int length= p.getLength();
		
		int startLine= 0;
		try {
			startLine= doc.getLineOfOffset(start);
		} catch (BadLocationException e) {
			// silently ignored
		}
		
		int lineCount= 0;
		
		if (length == 0) {
//			// if range length is 0 and if range starts a new line
//			try {
//				if (start == doc.getLineStartOffset(startLine)) {
//					lines--;
//				}
//			} catch (BadLocationException e) {
//				lines--;
//			}
			
		} else {
			int endLine= 0;
			try {
				endLine= doc.getLineOfOffset(start + length - 1);	// why -1?
			} catch (BadLocationException e) {
				// silently ignored
			}
			lineCount= endLine-startLine+1;
		}
				
		region.x= startLine;
		region.y= lineCount;
		return region;
	}
	
    public Diff findDiff(Position p, boolean left) {
		for (Iterator iterator = fAllDiffs.iterator(); iterator.hasNext();) {
			Diff diff = (Diff) iterator.next();
			Position diffPos;
			if (left) {
				diffPos = diff.fLeftPos;
			} else {
				diffPos = diff.fRightPos;
			}
			// If the element falls within a diff, highlight that diff
			if (diffPos.offset + diffPos.length >= p.offset && diff.fDirection != RangeDifference.NOCHANGE)
				return diff;
			// Otherwise, highlight the first diff after the elements position
			if (diffPos.offset >= p.offset)
				return diff;
		}
		return null;
	}

	public void reset() {
		fChangeDiffs= null;
		fAllDiffs= null;
	}
	
	/**
	 * Returns the virtual position for the given view position.
	 * @param contributor
	 * @param vpos
	 * @return the virtual position for the given view position
	 */
	public int realToVirtualPosition(char contributor, int vpos) {

		if (fAllDiffs == null)
			return vpos;
				
		int viewPos= 0;		// real view position
		int virtualPos= 0;	// virtual position
		Point region= new Point(0, 0);
		
		Iterator e= fAllDiffs.iterator();
		while (e.hasNext()) {
			Diff diff= (Diff) e.next();
			Position pos= diff.getPosition(contributor);
			getLineRange(getDocument(contributor),pos, region);
			int realHeight= region.y;
			int virtualHeight= diff.getMaxDiffHeight();
			if (vpos <= viewPos + realHeight) {	// OK, found!
				vpos-= viewPos;	// make relative to this slot
				// now scale position within this slot to virtual slot
				if (realHeight <= 0)
					vpos= 0;
				else
					vpos= (vpos*virtualHeight)/realHeight;
				return virtualPos+vpos;
			}
			viewPos+= realHeight;
			virtualPos+= virtualHeight;
		}
		return virtualPos;
	}
	
	/**
	 * maps given virtual position into a real view position of this view. 
	 * @param contributor
	 * @param v
	 * @return the real view position
	 */
	public int virtualToRealPosition(char contributor, int v) {
			
		if (fAllDiffs == null)
			return v;
					
		int virtualPos= 0;
		int viewPos= 0;
		Point region= new Point(0, 0);
		
		Iterator e= fAllDiffs.iterator();
		while (e.hasNext()) {
			Diff diff= (Diff) e.next();
			Position pos= diff.getPosition(contributor);
			int viewHeight= getLineRange(getDocument(contributor), pos, region).y;
			int virtualHeight= diff.getMaxDiffHeight();
			if (v < (virtualPos + virtualHeight)) {
				v-= virtualPos;		// make relative to this slot
				if (viewHeight <= 0) {
					v= 0;
				} else {
					v= (int) (v * ((double)viewHeight/virtualHeight));
				}
				return viewPos+v;
			}
			virtualPos+= virtualHeight;
			viewPos+= viewHeight;
		}
		return viewPos;
	}
	
	/*
	 * Calculates virtual height (in lines) of views by adding the maximum of corresponding diffs.
	 */
	public int getVirtualHeight() {
		int h= 1;
		if (fAllDiffs != null) {
			Iterator e= fAllDiffs.iterator();
			while (e.hasNext()) {
				Diff diff= (Diff) e.next();
				h+= diff.getMaxDiffHeight();
			}
		}
		return h;
	}
	
	/*
	 * Calculates height (in lines) of right view by adding the height of the right diffs.
	 */
	public int getRightHeight() {
		int h= 1;
		if (fAllDiffs != null) {
			Iterator e= fAllDiffs.iterator();
			while (e.hasNext()) {
				Diff diff= (Diff) e.next();
				h+= diff.getRightHeight();
			}
		}
		return h;
	}
	
	public int findInsertionPoint(Diff diff, char type) {
		if (diff != null) {
			switch (type) {
			case MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR:
				if (diff.fAncestorPos != null)
					return diff.fAncestorPos.offset;
				break;
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				if (diff.fLeftPos != null)
					return diff.fLeftPos.offset;
				break;
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				if (diff.fRightPos != null)
					return diff.fRightPos.offset;
				break;
			}
		}
		return 0;
	}
	
	public Diff[] getChangeDiffs(char contributor, IRegion region) {
		if (fChangeDiffs == null)
			return new Diff[0];
		List intersectingDiffs = new ArrayList();
		for (Iterator iterator = fChangeDiffs.iterator(); iterator.hasNext();) {
			Diff diff = (Diff) iterator.next();
			Diff[] changeDiffs = diff.getChangeDiffs(contributor, region);
			for (int i = 0; i < changeDiffs.length; i++) {
				Diff changeDiff = changeDiffs[i];
				intersectingDiffs.add(changeDiff);
			}
		}
		return (Diff[]) intersectingDiffs.toArray(new Diff[intersectingDiffs.size()]);
	}
	
	public Diff findDiff(int viewportHeight, boolean synchronizedScrolling, Point size, int my) {
		int virtualHeight= synchronizedScrolling ? getVirtualHeight() : getRightHeight();		
		if (virtualHeight < viewportHeight)
			return null;
		
		int yy, hh;
		int y= 0;
		if (fAllDiffs != null) {
			Iterator e= fAllDiffs.iterator();
			while (e.hasNext()) {
				Diff diff= (Diff) e.next();
				int h= synchronizedScrolling ? diff.getMaxDiffHeight()
											  : diff.getRightHeight();
				if (useChange(diff.getKind()) && !diff.fIsWhitespace) {
									
					yy= (y*size.y)/virtualHeight;
					hh= (h*size.y)/virtualHeight;
					if (hh < 3)
						hh= 3;
						
					if (my >= yy && my < yy+hh)
						return diff;
				}
				y+= h;
			}
		}
		return null;
	}

	public boolean hasChanges() {
		return fChangeDiffs != null && !fChangeDiffs.isEmpty();
	}
	
	public Iterator changesIterator() {
		if (fChangeDiffs == null)
			return new ArrayList().iterator();
		return fChangeDiffs.iterator();
	}
	
	public Iterator rangesIterator() {
		if (fAllDiffs == null)
			return new ArrayList().iterator();
		return fAllDiffs.iterator();
	}
	
	public boolean isFirstChildDiff(char contributor, int childStart, Diff diff) {
		if (!diff.hasChildren())
			return false;
		Diff d = (Diff)diff.fDiffs.get(0);
		Position p= d.getPosition(contributor);
		return (p.getOffset() >= childStart);
	}
	
	public Diff getWrappedDiff(Diff diff, boolean down) {
		if (fChangeDiffs != null && fChangeDiffs.size() > 0) {
			if (down)
				return (Diff) fChangeDiffs.get(0);
			return (Diff) fChangeDiffs.get(fChangeDiffs.size()-1);
		}
		return null;
	}
	
	/*
	 * Copy the contents of the given diff from one side to the other but
	 * doesn't reveal anything.
	 * Returns true if copy was successful.
	 */
	public boolean copy(Diff diff, boolean leftToRight) {
		
		if (diff != null && !diff.isResolved()) {
			Position fromPos= null;
			Position toPos= null;
			IDocument fromDoc= null;
			IDocument toDoc= null;

			if (leftToRight) {
				fromPos= diff.getPosition(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
				toPos= diff.getPosition(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
				fromDoc= getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
				toDoc= getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
			} else {
				fromPos= diff.getPosition(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
				toPos= diff.getPosition(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
				fromDoc= getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
				toDoc= getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
			}
			
			if (fromDoc != null) {
				
				int fromStart= fromPos.getOffset();
				int fromLen= fromPos.getLength();
				
				int toStart= toPos.getOffset();
				int toLen= toPos.getLength();

				try {
					String s= null;
											
					switch (diff.getKind()) {
					case RangeDifference.RIGHT:
					case RangeDifference.LEFT:
						s= fromDoc.get(fromStart, fromLen);
						break;
					case RangeDifference.ANCESTOR:
						break;
					case RangeDifference.CONFLICT:
						if (APPEND_CONFLICT) {
							s= toDoc.get(toStart, toLen);
							String ls = TextUtilities.getDefaultLineDelimiter(toDoc);
							if (!s.endsWith(ls))
								s += ls;
							s+= fromDoc.get(fromStart, fromLen);
						} else
							s= fromDoc.get(fromStart, fromLen);
						break;
					}
					if (s != null) {
						toDoc.replace(toStart, toLen, s);						
						toPos.setOffset(toStart);
						toPos.setLength(s.length());
					}	
				
				} catch (BadLocationException e) {
					// silently ignored
				}
			}
		
			diff.setResolved(true);
			return true;
		}
		return false;
	}

	public int changesCount() {
		if (fChangeDiffs == null)
			return 0;
		return fChangeDiffs.size();
	}
	
	public Diff findDiff(char contributor, int rangeStart, int rangeEnd) {
		if (hasChanges()) {
			for (Iterator iterator = changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted() || diff.getKind() == RangeDifference.NOCHANGE)
				    continue;
				if (diff.overlaps(contributor, rangeStart, rangeEnd, getDocument(contributor).getLength()))
					return diff;
			}
		}
		return null;
	}
	
	public Diff findDiff(char contributor, Position range) {
		int start= range.getOffset();
		int end= start + range.getLength();
		return findDiff(contributor, start, end);
	}
	
	public Diff findNext(char contributor, int start, int end, boolean deep) {
		return findNext(contributor, fChangeDiffs, start, end, deep);
	}
	
	private Diff findNext(char contributor, List v, int start, int end, boolean deep) {
		if (v == null)
			return null;
		for (int i= 0; i < v.size(); i++) {
			Diff diff= (Diff) v.get(i);
			Position p= diff.getPosition(contributor);
			if (p != null) {
				int startOffset= p.getOffset();
				if (end < startOffset)  // <=
					return diff;
				if (deep && diff.hasChildren()) {
					Diff d= null;
					int endOffset= startOffset + p.getLength();
					if (start == startOffset && (end == endOffset || end == endOffset-1)) {
						d= findNext(contributor, diff.fDiffs, start-1, start-1, deep);
					} else if (end < endOffset) {
						d= findNext(contributor, diff.fDiffs, start, end, deep);
					}
					if (d != null)
						return d;
				}
			}
		}
		return null;
	}
	
	public Diff findPrev(char contributor, int start, int end, boolean deep) {
		return findPrev(contributor, fChangeDiffs, start, end, deep);
	}
	
	private Diff findPrev(char contributor, List v, int start, int end, boolean deep) {
		if (v == null)
			return null;
		for (int i= v.size()-1; i >= 0; i--) {
			Diff diff= (Diff) v.get(i);
			Position p= diff.getPosition(contributor);
			if (p != null) {
				int startOffset= p.getOffset();
				int endOffset= startOffset + p.getLength();
				if (start > endOffset) {
					if (deep && diff.hasChildren()) {
						// If we are going deep, find the last change in the diff
						return findPrev(contributor, diff.fDiffs, end, end, deep);
					}
					return diff;
				}
				if (deep && diff.hasChildren()) {
					Diff d= null;
					if (start == startOffset && end == endOffset) {
						// A whole diff is selected so we'll fall through
						// and go the the last change in the previous diff
					} else if (start >= startOffset) {
						// If we are at or before the first diff, select the 
						// entire diff so next and previous are symmetrical
						if (isFirstChildDiff(contributor, start, diff)) {
							return diff;
						}
						d= findPrev(contributor, diff.fDiffs, start, end, deep);
					}
					if (d != null)
						return d;
				}
			}
		}
		return null;
	}
	
}
