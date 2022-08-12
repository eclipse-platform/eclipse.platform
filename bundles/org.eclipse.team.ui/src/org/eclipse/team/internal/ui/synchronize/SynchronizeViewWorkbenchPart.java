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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Fake part to use as keys in page book for synchronize participants
 */
public class SynchronizeViewWorkbenchPart implements IWorkbenchPart {

	private ISynchronizeParticipant participant;
	private IWorkbenchPartSite site;
	private ISynchronizePageConfiguration configuration;

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SynchronizeViewWorkbenchPart) &&
			participant.equals(((SynchronizeViewWorkbenchPart)obj).getParticipant());
	}

	@Override
	public int hashCode() {
		return participant.hashCode();
	}

	/**
	 * Constructs a part for the given participant that binds to the given
	 * site
	 */
	public SynchronizeViewWorkbenchPart(ISynchronizeParticipant participant, IWorkbenchPartSite site) {
		this.participant = participant;
		this.site = site;
	}

	@Override
	public void addPropertyListener(IPropertyListener listener) {
	}

	@Override
	public void createPartControl(Composite parent) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public IWorkbenchPartSite getSite() {
		return site;
	}

	@Override
	public String getTitle() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public Image getTitleImage() {
		return null;
	}

	@Override
	public String getTitleToolTip() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public void removePropertyListener(IPropertyListener listener) {
	}

	@Override
	public void setFocus() {
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	/**
	 * Returns the participant associated with this part.
	 *
	 * @return participant associated with this part
	 */
	public ISynchronizeParticipant getParticipant() {
		return participant;
	}

	public void setConfiguration(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
	}

	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}
}
