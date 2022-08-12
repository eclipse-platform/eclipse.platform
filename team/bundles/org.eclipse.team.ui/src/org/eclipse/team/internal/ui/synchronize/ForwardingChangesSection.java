/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * A changes section that points the user to a mode that has changes when the current mode
 * is empty.
 */
public abstract class ForwardingChangesSection extends ChangesSection {

	/**
	 * Shows message to user is no changes are to be shown in the diff
	 * tree viewer.
	 */
	private Composite messageArea;

	public ForwardingChangesSection(Composite parent, AbstractSynchronizePage page, ISynchronizePageConfiguration configuration) {
		super(parent, page, configuration);
	}

	@Override
	protected void initializeChangesViewer() {
		calculateDescription();
	}

	protected void calculateDescription() {
		if (getContainer().isDisposed())
			return;
		if(getVisibleChangesCount() == 0) {
			TeamUIPlugin.getStandardDisplay().asyncExec(() -> {
				if (!getContainer().isDisposed())
					updatePage(getEmptyChangesComposite(getContainer()));
			});
		} else {
			TeamUIPlugin.getStandardDisplay().asyncExec(() -> updatePage(null));
		}
	}

	protected void updatePage(Composite message) {
		if (getContainer().isDisposed()) return;
		if(messageArea != null) {
			messageArea.dispose();
			messageArea = null;
		}
		messageArea = message;
		if (message == null) {
			Control control = getChangesViewer().getControl();
			if (!getContainer().isDisposed() && !control.isDisposed()) {
				getContainer().showPage(control);
			}
		} else {
			getContainer().showPage(messageArea);
		}
	}

	protected Composite getEmptyChangesComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getListBackgroundColor());
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);

		if(! isThreeWay()) {
			createDescriptionLabel(composite,NLS.bind(TeamUIMessages.ChangesSection_noChanges, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, getConfiguration().getParticipant().getName()) }));
			return composite;
		}

		int allChanges = getChangesCount();
		long visibleChanges = getVisibleChangesCount();

		if(visibleChanges == 0 && allChanges != 0) {
			final int candidateMode = getCandidateMode();
			int currentMode = getConfiguration().getMode();
			if (candidateMode != currentMode) {
				long numChanges = getChangesInMode(candidateMode);
				if (numChanges > 0) {
					String message;
					if(numChanges > 1) {
						message = NLS.bind(TeamUIMessages.ChangesSection_filterHidesPlural, new String[] { Long.toString(numChanges), Utils.modeToString(candidateMode) });
					} else {
						message = NLS.bind(TeamUIMessages.ChangesSection_filterHidesSingular, new String[] { Long.toString(numChanges), Utils.modeToString(candidateMode) });
					}
					message = NLS.bind(TeamUIMessages.ChangesSection_filterHides, new String[] { Utils.modeToString(getConfiguration().getMode()), message });

					Label warning = new Label(composite, SWT.NONE);
					warning.setImage(TeamUIPlugin.getPlugin().getImage(ISharedImages.IMG_WARNING_OVR));

					Hyperlink link = getForms().createHyperlink(composite, NLS.bind(TeamUIMessages.ChangesSection_filterChange, new String[] { Utils.modeToString(candidateMode) }), SWT.WRAP);
					link.addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							getConfiguration().setMode(candidateMode);
						}
					});
					getForms().getHyperlinkGroup().add(link);
					createDescriptionLabel(composite, message);
					return composite;
				}
			}
		}
		// There is no other mode that can be shown so just indicate that there are no changes
		createDescriptionLabel(composite,NLS.bind(TeamUIMessages.ChangesSection_noChanges, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, getConfiguration().getParticipant().getName()) }));	 //
		return composite;
	}

	protected Label createDescriptionLabel(Composite parent, String text) {
		Label description = new Label(parent, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.widthHint = 100;
		description.setLayoutData(data);
		description.setText(text);
		description.setBackground(getListBackgroundColor());
		return description;
	}

	protected abstract int getChangesCount();

	protected abstract long getChangesInMode(int candidateMode);

	protected abstract long getVisibleChangesCount();

	protected abstract int getCandidateMode();
}
