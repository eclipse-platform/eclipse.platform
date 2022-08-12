/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceDiffCompareInput;
import org.eclipse.team.internal.ui.synchronize.ImageManager;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;

/**
 * A label provider wrapper that adds synchronization image and/or text decorations
 * to the image and label obtained from the delegate provider.
 *
 * @since 3.2
 */
public abstract class AbstractSynchronizeLabelProvider implements ILabelProvider {

	private ImageManager localImageManager;

	@Override
	public Image getImage(Object element) {
		Image base = getDelegateImage(element);
		if (isDecorationEnabled() && base != null) {
			Image decorateImage = decorateImage(base, element);
			base = decorateImage;
		}
		if (isIncludeOverlays() && base != null) {
			base = addOverlays(base, element);
		}
		return base;
	}

	/**
	 * Decorate the image with the appropriate diff decorations.
	 * By default, this method uses the diff associated with
	 * the given element to determine how to decorate the image.
	 * It then uses the {@link CompareConfiguration#getImage(Image, int)}
	 * method to apply the decoration to the base image.
	 * @param base the base image to be decorated.
	 * @param element the element
	 * @return the image decorated appropriately using the diff associated with
	 * the element
	 * @see #getDiff(Object)
	 * @see CompareConfiguration#getImage(Image, int)
	 */
	protected Image decorateImage(Image base, Object element) {
		Image decoratedImage;
		if (element instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput) element;
			decoratedImage = getCompareImage(base, ci.getKind());
		} else {
			IDiff node = getDiff(element);
			decoratedImage = getCompareImage(base, node);
		}
		// The reason we still overlay the compare image is to
		// ensure that the image width for all images shown in the viewer
		// are consistent.
		return decoratedImage;
	}

	/**
	 * Return the image for the item from the delegate label provider.
	 * @param element the element
	 * @return the image for the item from the delegate label provider
	 */
	protected Image getDelegateImage(Object element) {
		ILabelProvider modelLabelProvider = getDelegateLabelProvider();
		Image base = modelLabelProvider.getImage(internalGetElement(element));
		if (base == null && element instanceof ModelProvider) {
			ModelProvider mp = (ModelProvider) element;
			base = getImageManager().getImage(getImageDescriptor(mp));
		}
		return base;
	}

	private ImageDescriptor getImageDescriptor(ModelProvider provider) {
		ITeamContentProviderManager manager = TeamUI.getTeamContentProviderManager();
		ITeamContentProviderDescriptor desc = manager.getDescriptor(provider.getId());
		return desc.getImageDescriptor();
	}

	private Object internalGetElement(Object element) {
		if (element instanceof TreePath) {
			TreePath tp = (TreePath) element;
			element = tp.getLastSegment();
		}
		return element;
	}

	private Image getCompareImage(Image base, IDiff node) {
		int compareKind = getCompareKind(node);
		return getCompareImage(base, compareKind);
	}

	/**
	 * Returns an image showing the specified change kind applied to a given base image.
	 *
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected Image getCompareImage(Image base, int compareKind) {
		return getImageManager().getImage(base, compareKind);
	}

	private int getCompareKind(IDiff node) {
		return ResourceDiffCompareInput.getCompareKind(node);
	}

	@Override
	public String getText(Object element) {
		String base = getDelegateText(element);
		if (isSyncInfoInTextEnabled()) {
			return decorateText(base, element);
		}
		return base;
	}

	/**
	 * Obtain the text for the object from the delegate label provider.
	 * @param element the element
	 * @return the text label for the element
	 */
	protected String getDelegateText(Object element) {
		ILabelProvider modelLabelProvider = getDelegateLabelProvider();
		element = internalGetElement(element);
		String base = modelLabelProvider.getText(element);
		if (base == null || base.length() == 0) {
			if (element instanceof ModelProvider) {
				ModelProvider provider = (ModelProvider) element;
				base = Utils.getLabel(provider);
			}
		}
		return base;
	}

	/**
	 * Decorate the text with the appropriate diff decorations.
	 * By default, this method uses the diff associated with
	 * the given element to determine how to decorate the text.
	 * @param base the base text to be decorated.
	 * @param element the element
	 * @return the text decorated appropriately using the diff associated with
	 * the element
	 * @see #getDiff(Object)
	 */
	protected String decorateText(String base, Object element) {
		IDiff node = getDiff(element);
		if (node != null && node.getKind() != IDiff.NO_CHANGE) {
			String syncKindString = node.toDiffString();
			return NLS.bind(TeamUIMessages.AbstractSynchronizationLabelProvider_0, new String[] { base, syncKindString });
		}
		return base;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		getDelegateLabelProvider().addListener(listener);
	}

	@Override
	public void dispose() {
		if (localImageManager != null)
			localImageManager.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return getDelegateLabelProvider().isLabelProperty(internalGetElement(element), property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		getDelegateLabelProvider().removeListener(listener);
	}

	/**
	 * Returns whether the synchronization state should be included in the
	 * text of the label. By default, the Team preference is used to determine
	 * what to return. Subclasses may override.
	 * @return whether the synchronization state should be included in the
	 * text of the label
	 */
	protected boolean isSyncInfoInTextEnabled() {
		return isDecorationEnabled() && TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL);
	}

	/**
	 * Return the label provider that will return the text and image
	 * appropriate for the given model element. Subclasses are responsible for
	 * disposing of the label provider.
	 * @return the label provider that will return the text and image
	 * appropriate for the given model element
	 */
	protected abstract ILabelProvider getDelegateLabelProvider();

	/**
	 * Return whether the label provider should decorate with the synchronization state.
	 * @return whether the label provider should decorate with the synchronization state
	 */
	protected abstract boolean isDecorationEnabled();

	/**
	 * Return the sync kind of the given element. This is used
	 * to determine how to decorate the image and label of the
	 * element. The sync kind is described in the {@link SyncInfo}
	 * class. A <code>null</code> is returned by default.
	 * @param element the element being tested
	 * @return the sync kind of the given element
	 */
	protected IDiff getDiff(Object element) {
		return null;
	}

	private Image addOverlays(Image base, Object element) {
		if (!isIncludeOverlays())
			return base;

		ImageDescriptor[] overlayImages = new ImageDescriptor[4];
		boolean hasOverlay = false;

		// Decorate with the busy indicator
		if (isBusy(element)) {
			overlayImages[IDecoration.TOP_LEFT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_HOURGLASS_OVR);
			hasOverlay = true;
		}
		// Decorate with propagated conflicts and problem markers
		if (!isConflicting(element)) {
			// if the folder is already conflicting then don't bother propagating
			if (hasDecendantConflicts(element)) {
				overlayImages[IDecoration.BOTTOM_RIGHT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CONFLICT_OVR);
				hasOverlay = true;
			}
		}
		int severity = getMarkerSeverity(element);
		if (severity == IMarker.SEVERITY_ERROR) {
			overlayImages[IDecoration.BOTTOM_LEFT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_ERROR_OVR);
			hasOverlay = true;
		} else if (severity == IMarker.SEVERITY_WARNING) {
			overlayImages[IDecoration.BOTTOM_LEFT] = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WARNING_OVR);
			hasOverlay = true;
		}
		if (hasOverlay) {
			ImageDescriptor overlay = new DecorationOverlayIcon(base, overlayImages, new Point(base.getBounds().width, base.getBounds().height));
			return getImageManager().getImage(overlay);
		}
		return base;
	}

	/**
	 * Indicate whether the overlays provided by this class should be applied.
	 * By default, <code>false</code> is returned. Subclasses may override
	 * and control individual overlays by overriding the appropriate
	 * query methods. Overlays provided by this class include problem marker
	 * severity ({@link #getMarkerSeverity(Object)}), propagated conflicts
	 * ({@link #hasDecendantConflicts(Object)} and busy state ({@link #isBusy(Object)}).
	 * @return whether the overlays provided by this class should be applied
	 */
	protected boolean isIncludeOverlays() {
		return false;
	}

	/**
	 * Return the marker severity (one of IMarker.SEVERITY_ERROR or
	 * IMarker.SEVERITY_WARNING) to be overlayed on the given element or -1 if
	 * there are no markers. By Default, the element is adapted to resource
	 * mapping in order to look for markers.
	 * <p>
	 * Although this class handles providing the overlays, it does not react
	 * to marker changes. Subclasses must issue label updates when the markers on
	 * a logical model element change.
	 *
	 * @param element
	 *            the element
	 * @return the marker severity
	 */
	protected int getMarkerSeverity(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(internalGetElement(element));
		int result = -1;
		if (mapping != null) {
			try {
				IMarker[] markers = mapping.findMarkers(IMarker.PROBLEM, true, null);
				for (IMarker marker : markers) {
					Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
					if (severity != null) {
						if (severity.intValue() == IMarker.SEVERITY_ERROR) {
							return IMarker.SEVERITY_ERROR;
						} else if (severity.intValue() == IMarker.SEVERITY_WARNING) {
							result = IMarker.SEVERITY_WARNING;
						}
					}
				}
			} catch (CoreException e) {
				// Ignore
			}
		}
		return result;
	}

	/**
	 * Return whether the given element has descendant conflicts.
	 * By default, <code>false</code> is returned. Subclasses
	 * may override.
	 * @param element the element
	 * @return whether the given element has descendant conflicts
	 */
	protected boolean hasDecendantConflicts(Object element) {
		return false;
	}

	private boolean isConflicting(Object element) {
		IDiff node = getDiff(element);
		if (node != null) {
			if (node instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) node;
				return twd.getDirection() == IThreeWayDiff.CONFLICTING;
			}
		}
		return false;
	}

	/**
	 * Return whether the given element is busy (i.e. is involved
	 * in an operation. By default, <code>false</code> is returned.
	 * Subclasses may override.
	 * @param element the element
	 * @return whether the given element is busy
	 */
	protected boolean isBusy(Object element) {
		return false;
	}

	/**
	 * Method that provides a custom font for elements that are
	 * busy. Although this label provider does not implement
	 * {@link IFontProvider}, subclasses that wish to get
	 * busy indication using a font can do so.
	 * @param element the element
	 * @return the font to indicate that the element is busy
	 */
	public Font getFont(Object element) {
		if(isBusy(internalGetElement(element))) {
			return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
		}
		return null;
	}

	private ImageManager getImageManager() {
		ISynchronizationContext context = getContext();
		if (context != null) {
			return ImageManager.getImageManager(context, getConfiguration());
		}
		if (localImageManager == null) {
			localImageManager = new ImageManager();
		}
		return localImageManager;
	}

	private ISynchronizePageConfiguration getConfiguration() {
		if (this instanceof SynchronizationLabelProvider) {
			SynchronizationLabelProvider slp = (SynchronizationLabelProvider) this;
			return (ISynchronizePageConfiguration)slp.getExtensionSite().getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
		}
		return null;
	}

	private ISynchronizationContext getContext() {
		if (this instanceof SynchronizationLabelProvider) {
			SynchronizationLabelProvider slp = (SynchronizationLabelProvider) this;
			return slp.getContext();
		}
		return null;
	}

}
