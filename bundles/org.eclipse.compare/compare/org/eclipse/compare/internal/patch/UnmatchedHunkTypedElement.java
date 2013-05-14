/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ContentChangeNotifier;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class UnmatchedHunkTypedElement extends HunkTypedElement implements IContentChangeNotifier, IEditableContent {

	private ContentChangeNotifier changeNotifier;
	
	public UnmatchedHunkTypedElement(HunkResult result) {
		// An unmatched hunk element is always used for the before state and is full context
		super(result, false, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeNotifier#addContentChangeListener(org.eclipse.compare.IContentChangeListener)
	 */
	public synchronized void addContentChangeListener(IContentChangeListener listener) {
		if (changeNotifier == null)
			changeNotifier = new ContentChangeNotifier(this);
		changeNotifier.addContentChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeNotifier#removeContentChangeListener(org.eclipse.compare.IContentChangeListener)
	 */
	public synchronized void removeContentChangeListener(IContentChangeListener listener) {
		if (changeNotifier != null)
			changeNotifier.removeContentChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContent#isEditable()
	 */
	public boolean isEditable() {
		IFile file = ((WorkspaceFileDiffResult)getHunkResult().getDiffResult()).getTargetFile();
		return file != null && file.isAccessible();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContent#replace(org.eclipse.compare.ITypedElement, org.eclipse.compare.ITypedElement)
	 */
	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		// Not supported
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContent#setContent(byte[])
	 */
	public void setContent(byte[] newContent) {
		getPatcher().setManuallyMerged(getHunkResult().getHunk(), true);
		getPatcher().cacheContents(getDiff(), newContent);
		if (changeNotifier != null)
			changeNotifier.fireContentChanged();
	}

	private FilePatch2 getDiff() {
		return getHunkResult().getDiffResult().getDiff();
	}

	private Patcher getPatcher() {
		return Patcher.getPatcher(getConfiguration());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.patch.HunkTypedElement#getContents()
	 */
	public InputStream getContents() throws CoreException {
		// If there are cached contents, use them
		if (getPatcher().hasCachedContents(getDiff()))
			return new ByteArrayInputStream(getPatcher().getCachedContents(getDiff()));
		// Otherwise return the after state of the diff result
		List lines = getHunkResult().getDiffResult().getAfterLines();
		String content = LineReader.createString(getHunkResult().getDiffResult().isPreserveLineDelimeters(), lines);
		byte[] bytes = null;
		if (getCharset() != null)
			try {
				bytes = content.getBytes(getCharset());
			} catch (UnsupportedEncodingException e) {
				CompareUIPlugin.log(e);
			}
		if (bytes == null)
			bytes = content.getBytes();
		return new ByteArrayInputStream(bytes);
	}

	private PatchConfiguration getConfiguration() {
		return getHunkResult().getDiffResult().getConfiguration();
	}
}
