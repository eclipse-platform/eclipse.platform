/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IFontDecorator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * Label decorator that decorates the default active change set.
 */
public class ChangeSetLabelDecorator extends LabelProvider implements ILabelDecorator, IFontDecorator{

	private Font boldFont;
	private ActiveChangeSetManager collector;

	public ChangeSetLabelDecorator(ISynchronizePageConfiguration configuration) {
		ISynchronizeParticipant participant = configuration.getParticipant();
		if (participant instanceof IChangeSetProvider) {
			this.collector = ((IChangeSetProvider)participant).getChangeSetCapability().getActiveChangeSetManager();
		}
	}

	@Override
	public String decorateText(String input, Object element) {
		String text = input;
		if (element instanceof ChangeSetDiffNode) {
			ChangeSet set = ((ChangeSetDiffNode)element).getSet();
			if (set instanceof ActiveChangeSet && isDefaultActiveSet((ActiveChangeSet)set)) {
				text = NLS.bind(TeamUIMessages.CommitSetDiffNode_0, new String[] { text });
			}
		}
		return text;
	}

	@Override
	public void dispose() {
		if(boldFont != null) {
			boldFont.dispose();
		}
	}

	@Override
	public Font decorateFont(Object element) {
		if (element instanceof ChangeSetDiffNode) {
			ChangeSet set = ((ChangeSetDiffNode)element).getSet();
			if (set instanceof ActiveChangeSet && isDefaultActiveSet((ActiveChangeSet)set)) {
				if (boldFont == null) {
					Font defaultFont = JFaceResources.getDefaultFont();
					FontData[] data = defaultFont.getFontData();
					for (FontData d : data) {
						d.setStyle(SWT.BOLD);
					}
					boldFont = new Font(TeamUIPlugin.getStandardDisplay(), data);
				}
				return boldFont;
			}
		}
		return null;
	}

	private boolean isDefaultActiveSet(ActiveChangeSet set) {
		return collector.isDefault(set);
	}

	@Override
	public Image decorateImage(Image image, Object element) {
		return image;
	}

}
