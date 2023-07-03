/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.views;

import org.eclipse.help.internal.base.MissingContentManager;
import org.eclipse.help.internal.base.remote.RemoteStatusData;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

public class MissingContentPart extends AbstractFormPart implements IHelpPart  {

	private Composite container;
	private String id;
	private ReusableHelpPart helpPart;
	private ImageHyperlink statusLink;
	private boolean wasRemoteHelpUnavailable = false;
	private boolean wasUnresolvedPlaceholders = false;

	public MissingContentPart(Composite parent, FormToolkit toolkit) {
		container = toolkit.createComposite(parent, SWT.NULL);
		container.setBackgroundMode(SWT.INHERIT_DEFAULT);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		layout.marginTop = 2;
		container.setLayout(layout);
		updateStatus();
	}

	public void updateStatus() {
		// Only update the controls if the status has changed
		boolean isRemoteHelpUnavailable = RemoteStatusData.isAnyRemoteHelpUnavailable();
		if ( isRemoteHelpUnavailable && wasRemoteHelpUnavailable) {
			return; // Nothing to do, remote help unavailable message already showing
		}
		boolean isUnresolvedPlaceholders = MissingContentManager.getInstance().isUnresolvedPlaceholders();
		if ( isRemoteHelpUnavailable == wasRemoteHelpUnavailable && isUnresolvedPlaceholders == wasUnresolvedPlaceholders ) {
			return;
		}
		disposeLink();
		wasRemoteHelpUnavailable = isRemoteHelpUnavailable;
		wasUnresolvedPlaceholders = isUnresolvedPlaceholders;
		FormToolkit toolkit = new FormToolkit(container.getDisplay());
		if ( isRemoteHelpUnavailable ) {
			createHelpMissingLink(container, toolkit, Dialog.DLG_IMG_MESSAGE_WARNING, Messages.remoteHelpUnavailable,
			        MissingContentManager.getInstance().getRemoteHelpUnavailablePage(true), true);
		} else if ( isUnresolvedPlaceholders) {
			createHelpMissingLink(container, toolkit, Dialog.DLG_IMG_MESSAGE_INFO, Messages.ReusableHelpPart_missingContent,
					MissingContentManager.getInstance().getHelpMissingPage(true), false);
		}
		toolkit.dispose();
	}

	private void createHelpMissingLink(Composite container, FormToolkit toolkit, String imageKey, String linkText, String linkTarget, boolean isRemoteUnavailableLink) {
		final String target = linkTarget;
		final boolean isRemote = isRemoteUnavailableLink;
		Composite padding = new Composite(container, SWT.NULL);
		GridData paddingData = new GridData();
		paddingData.heightHint = 2;
		padding.setLayoutData(paddingData);
		toolkit.adapt(padding);
		Image warningImage = JFaceResources.getImage(imageKey);
		statusLink = toolkit.createImageHyperlink(container, SWT.NULL);
		statusLink.setText(linkText);
		statusLink.setImage(warningImage);
		statusLink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				helpPart.showURL(target);
				if ( isRemote ) {
					helpPart.checkRemoteStatus();
				} else {
					helpPart.checkPlaceholderStatus();
				}
			}
		});
		GridData statusData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		statusLink.setLayoutData(statusData);
	}

	private void disposeLink() {
		if (statusLink != null) {
			statusLink.dispose();
		}
		statusLink = null;
	}

	public void setSubsequentPage(String subsequentPage) {

	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.id = id;
		this.helpPart = parent;
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		if (container != null) {
			container.setVisible(visible);
		}
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return false;
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return false;
	}

	@Override
	public IAction getGlobalAction(String id) {
		return null;
	}

	@Override
	public void stop() {

	}

	@Override
	public void toggleRoleFilter() {

	}

	@Override
	public void refilter() {

	}

	@Override
	public void dispose() {
		disposeLink();
	}

}
