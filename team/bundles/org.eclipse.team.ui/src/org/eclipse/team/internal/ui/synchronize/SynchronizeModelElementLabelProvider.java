/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
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
	private Map<ImageDescriptor, Image> fgImageCache;

	// Contains direction images
	CompareConfiguration compareConfig = new CompareConfiguration();

	// Used as the base label provider for retreiving image and text from
	// the workbench adapter.
	private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

	// Font used to display busy elements
	private Font busyFont;

	public SynchronizeModelElementLabelProvider() {
	}

	@Override
	public Color getForeground(Object element) {
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}

	@Override
	public Font getFont(Object element) {
		if (element instanceof ISynchronizeModelElement) {
			ISynchronizeModelElement node = (ISynchronizeModelElement)element;
			if(node.getProperty(ISynchronizeModelElement.BUSY_PROPERTY)) {
				if (busyFont == null) {
					Font defaultFont = JFaceResources.getDefaultFont();
					FontData[] data = defaultFont.getFontData();
					for (FontData d : data) {
						d.setStyle(SWT.ITALIC);
					}
					busyFont = new Font(TeamUIPlugin.getStandardDisplay(), data);
				}
				return busyFont;
			}
		}
		return null;
	}

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
				fgImageCache = new HashMap<>(10);
			}
			Image conflictDecoratedImage = fgImageCache.get(overlay);
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
