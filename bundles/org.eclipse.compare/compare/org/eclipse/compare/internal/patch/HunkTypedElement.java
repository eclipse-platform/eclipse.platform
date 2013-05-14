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

import java.io.InputStream;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.DiffImageDescriptor;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Image;

public class HunkTypedElement implements ITypedElement, IEncodedStreamContentAccessor, IAdaptable {

	private final HunkResult fHunkResult;
	private final boolean fIsAfterState;
	private final boolean fFullContext;

	public HunkTypedElement(HunkResult result, boolean isAfterState, boolean fullContext) {
		this.fHunkResult = result;
		this.fIsAfterState = isAfterState;
		this.fFullContext = fullContext;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getImage()
	 */
	public Image getImage() {
		LocalResourceManager imageCache = PatchCompareEditorInput.getImageCache(fHunkResult.getDiffResult().getConfiguration());
		ImageDescriptor imageDesc = CompareUIPlugin.getImageDescriptor(ICompareUIConstants.HUNK_OBJ);
		Image image = imageCache.createImage(imageDesc);
		if (!fHunkResult.isOK()) {
			return getHunkErrorImage(image, imageCache, true);
		}  else if (fHunkResult.getFuzz() > 0) {
			return getHunkOverlayImage(image, imageCache, ICompareUIConstants.WARNING_OVERLAY, true);
		}
		return image;
	}

	public static Image getHunkErrorImage(Image baseImage, LocalResourceManager imageCache, boolean onLeft) {
		return getHunkOverlayImage(baseImage, imageCache, ICompareUIConstants.ERROR_OVERLAY, onLeft);
	}
	
	private static Image getHunkOverlayImage(Image baseImage, LocalResourceManager imageCache, String path, boolean onLeft) {
		ImageDescriptor desc = new DiffImageDescriptor(baseImage, CompareUIPlugin.getImageDescriptor(path), ICompareUIConstants.COMPARE_IMAGE_WIDTH, onLeft);
		Image image = imageCache.createImage(desc);
		return image;
	}

	public boolean isManuallyMerged() {
		return getPatcher().isManuallyMerged(getHunkResult().getHunk());
	}

	private Patcher getPatcher() {
		return Patcher.getPatcher(fHunkResult.getDiffResult().getConfiguration());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	public String getName() {
		return fHunkResult.getHunk().getLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		return fHunkResult.getDiffResult().getDiff().getTargetPath(fHunkResult.getDiffResult().getConfiguration()).getFileExtension();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IStreamContentAccessor#getContents()
	 */
	public InputStream getContents() throws CoreException {
		String contents = fHunkResult.getContents(fIsAfterState, fFullContext);
		return FileDiffResult.asInputStream(contents, fHunkResult.getCharset());
	}

	public String getCharset() throws CoreException {
		return fHunkResult.getCharset();
	}

	public HunkResult getHunkResult() {
		return fHunkResult;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IHunk.class)
			return fHunkResult;
		if (adapter == HunkResult.class)
			return fHunkResult;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

}
