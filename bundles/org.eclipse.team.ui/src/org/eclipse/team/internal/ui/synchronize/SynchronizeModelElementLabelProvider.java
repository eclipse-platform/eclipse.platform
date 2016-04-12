/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.*;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A label provider that decorates viewers showing 
 * {@link ISynchronizeModelElement}.
 * 
 * @since 3.0
 */
public class SynchronizeModelElementLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {

	// Cache for folder images that have been overlayed with conflict icon
	private Map fgImageCache;
	
	// Contains direction images
	CompareConfiguration compareConfig = new CompareConfiguration();
	
	// Used as the base label provider for retreiving image and text from
	// the workbench adapter.
	private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
	
	// Font used to display busy elements
	private Font busyFont;

	public SynchronizeModelElementLabelProvider() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	@Override
	public Color getForeground(Object element) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	@Override
	public Font getFont(Object element) {
		if (element instanceof ISynchronizeModelElement) {
			ISynchronizeModelElement node = (ISynchronizeModelElement)element;
			if(node.getProperty(ISynchronizeModelElement.BUSY_PROPERTY)) {
				if (busyFont == null) {
					Font defaultFont = JFaceResources.getDefaultFont();
					FontData[] data = defaultFont.getFontData();
					for (int i = 0; i < data.length; i++) {
						data[i].setStyle(SWT.ITALIC);
					}				
					busyFont = new Font(TeamUIPlugin.getStandardDisplay(), data);
				}
				return busyFont;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		Image base = workbenchLabelProvider.getImage(element);
		if (base != null) {
			if (element instanceof ISynchronizeModelElement) {
				ISynchronizeModelElement syncNode = (ISynchronizeModelElement) element;
				int kind = syncNode.getKind();
				Image decoratedImage;
				decoratedImage = getCompareImage(base, kind);				
				// The reason we still overlay the compare image is to
				// ensure that the image width for all images shown in the viewer
				// are consistent.
				return propagateConflicts(decoratedImage, syncNode);				
			}
		}
		return base;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		String base = workbenchLabelProvider.getText(element);
		if (element instanceof DiffNode) {
			if (TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL)) {
				// if the folder is already conflicting then don't bother
				// propagating the conflict
				int kind = ((DiffNode) element).getKind();
				if (kind != SyncInfo.IN_SYNC) {
					String syncKindString = SyncInfo.kindToString(kind);
					return NLS.bind(TeamUIMessages.TeamSubscriberSyncPage_labelWithSyncKind, new String[] { base, syncKindString }); // 
				}
			}
		}
		return base;
	}

	protected Image getCompareImage(Image base, int kind) {
		switch (kind & SyncInfo.DIRECTION_MASK) {
			case SyncInfo.OUTGOING :
				kind = (kind & ~SyncInfo.OUTGOING) | SyncInfo.INCOMING;
				break;
			case SyncInfo.INCOMING :
				kind = (kind & ~SyncInfo.INCOMING) | SyncInfo.OUTGOING;
				break;
		}
		return compareConfig.getImage(base, kind);
	}

	private Image propagateConflicts(Image base, ISynchronizeModelElement element) {

		ImageDescriptor[] overlayImages = new ImageDescriptor[4];
		boolean hasOverlay = false;
		
		// Decorate with the busy indicator
		if (element.getProperty(ISynchronizeModelElement.BUSY_PROPERTY)) {
			overlayImages[IDecoration.TOP_LEFT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_HOURGLASS_OVR);
			hasOverlay = true;
		}
		// Decorate with propagated conflicts and problem markers
		int kind = element.getKind();
		if ((kind & SyncInfo.DIRECTION_MASK) != SyncInfo.CONFLICTING) {
			// if the folder is already conflicting then don't bother propagating
			// the conflict
			if (hasDecendantConflicts(element)) {
				overlayImages[IDecoration.BOTTOM_RIGHT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CONFLICT_OVR);
				hasOverlay = true;
			}
		}
		if (hasErrorMarker(element)) {
			overlayImages[IDecoration.BOTTOM_LEFT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_ERROR_OVR);
			hasOverlay = true;
		} else if (hasWarningMarker(element)) {
			overlayImages[IDecoration.BOTTOM_LEFT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WARNING_OVR);
			hasOverlay = true;
		}
		if (hasOverlay) {
			ImageDescriptor overlay = new DecorationOverlayIcon(base, overlayImages, new Point(base.getBounds().width, base.getBounds().height));
			if (fgImageCache == null) {
				fgImageCache = new HashMap(10);
			}
			Image conflictDecoratedImage = (Image) fgImageCache.get(overlay);
			if (conflictDecoratedImage == null) {
				conflictDecoratedImage = overlay.createImage();
				fgImageCache.put(overlay, conflictDecoratedImage);
			}
			return conflictDecoratedImage;
		}
		return base;
	}
	
	/**
	 * Return whether this diff node has descendant conflicts in the view in
	 * which it appears.
	 * @return whether the node has descendant conflicts
	 */
	private boolean hasDecendantConflicts(ISynchronizeModelElement node) {
		return node.getProperty(ISynchronizeModelElement.PROPAGATED_CONFLICT_PROPERTY);
	}
	
	/**
	 * Return whether this diff node has descendant conflicts in the view in which it appears.
	 * @return whether the node has descendant conflicts
	 */
	private boolean hasErrorMarker(ISynchronizeModelElement node) {
		return node.getProperty(ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY);
	}
	
	/**
	 * Return whether this diff node has descendant conflicts in the view in which it appears.
	 * @return whether the node has descendant conflicts
	 */
	private boolean hasWarningMarker(ISynchronizeModelElement node) {
		return node.getProperty(ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
        workbenchLabelProvider.dispose();
		if(busyFont != null) {
			busyFont.dispose();
		}
		compareConfig.dispose();
		if (fgImageCache != null) {
			Iterator it = fgImageCache.values().iterator();
			while (it.hasNext()) {
				Image element = (Image) it.next();
				element.dispose();
			}
		}
	}
}
