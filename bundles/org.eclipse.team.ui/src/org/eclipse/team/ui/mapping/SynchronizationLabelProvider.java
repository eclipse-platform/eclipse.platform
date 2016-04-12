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
package org.eclipse.team.ui.mapping;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.*;

/**
 * A label provider wrapper that adds synchronization image and/or text decorations
 * to the image and label obtained from the delegate provider.
 *
 * @since 3.2
 */
public abstract class SynchronizationLabelProvider extends AbstractSynchronizeLabelProvider implements ICommonLabelProvider, IFontProvider {

	private ISynchronizationScope scope;
	private ISynchronizationContext context;
	private ITreeContentProvider contentProvider;
	private ICommonContentExtensionSite site;

	private void init(ISynchronizationScope input, ISynchronizationContext context) {
		this.scope = input;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.ICommonLabelProvider#init(org.eclipse.ui.navigator.ICommonContentExtensionSite)
	 */
	@Override
	public void init(ICommonContentExtensionSite site) {
		this.site = site;
		contentProvider = site.getExtension().getContentProvider();
		init((ISynchronizationScope)site.getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE),
				(ISynchronizationContext)site.getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT));
	}

	/**
	 * Return the synchronization context associated with the view to which
	 * this label provider applies. A <code>null</code> is returned if
	 * no context is available.
	 * @return the synchronization context or <code>null</code>
	 */
	public ISynchronizationContext getContext() {
		return context;
	}

	/**
	 * Return the resource mapping scope associated with the view to which
	 * this label provider applies. A <code>null</code> is returned if
	 * no scope is available.
	 * @return the resource mapping scope or <code>null</code>
	 */
	public ISynchronizationScope getScope() {
		return scope;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void restoreState(IMemento aMemento) {
		ILabelProvider provider = getDelegateLabelProvider();
		if (provider instanceof ICommonLabelProvider) {
			((ICommonLabelProvider) provider).restoreState(aMemento);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento aMemento) {
		ILabelProvider provider = getDelegateLabelProvider();
		if (provider instanceof ICommonLabelProvider) {
			((ICommonLabelProvider) provider).saveState(aMemento);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IDescriptionProvider#getDescription(java.lang.Object)
	 */
	@Override
	public String getDescription(Object anElement) {
		ILabelProvider provider = getDelegateLabelProvider();
		if (provider instanceof IDescriptionProvider) {
			return ((IDescriptionProvider) provider).getDescription(internalGetElement(anElement));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.mapping.SynchronizationStateLabelProvider#isDecorationEnabled()
	 */
	@Override
	protected boolean isDecorationEnabled() {
		return getContext() != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.mapping.SynchronizationStateLabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		Image image = super.getImage(element);
		if (image == null && internalGetElement(element) instanceof ModelProvider) {
			image = super.getImage(getModelRoot());
		}
		return image;
	}

	/**
	 * Return the root object for the model. By default, it is the
	 * workspace root. Subclasses may override. This object is used to
	 * obtain an image for the model provider.
	 * @return the root object for the model
	 */
	protected Object getModelRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		String text = super.getText(element);
		if (contentProvider instanceof SynchronizationContentProvider) {
			SynchronizationContentProvider scp = (SynchronizationContentProvider) contentProvider;
			ISynchronizationContext context = getContext();
			if (context != null && !scp.isInitialized(context)) {
				return NLS.bind(TeamUIMessages.SynchronizationLabelProvider_0, text);
			}
		}
		return text;
	}

	/**
	 * Return the Common Navigator extension site for this
	 * label provider.
	 * @return the Common Navigator extension site for this
	 * label provider
	 */
	public ICommonContentExtensionSite getExtensionSite() {
		return site;
	}

	private Object internalGetElement(Object element) {
		if (element instanceof TreePath) {
			TreePath tp = (TreePath) element;
			element = tp.getLastSegment();
		}
		return element;
	}
}
