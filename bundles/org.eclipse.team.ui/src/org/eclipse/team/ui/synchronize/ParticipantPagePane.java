/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.ui.synchronize;

import org.eclipse.compare.CompareViewerPane;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Stand alone presentation of a participant page within a view pane. This
 * allows showing a participant page with it's toolbar in dialogs and embedded
 * in views and editors.
 *
 * @since 3.1
 */
public final class ParticipantPagePane {

	private ISynchronizeParticipant participant;
	private ISynchronizePageConfiguration pageConfiguration;
	private Image titleImage;
	private Shell shell;
	private boolean isModal;

	// SWT controls
	private CompareViewerPane fEditionPane;
	private IPageBookViewPage fPage;
	private DialogSynchronizePageSite site;

	/**
	 * Creates a part for the provided participant. The page configuration is used when creating the participant page and the resulting
	 * compare/merge panes will be configured with the provided compare configuration.
	 * <p>
	 * For example, clients can decide if the user can edit the compare panes by calling {@link org.eclipse.compare.CompareConfiguration#setLeftEditable(boolean)}
	 * or {@link org.eclipse.compare.CompareConfiguration#setRightEditable(boolean)}.
	 * </p>
	 * @param shell the parent shell for this part
	 * @param isModal to set the pane as modal or not
	 * @param pageConfiguration the configuration that will be provided to the participant prior to creating the page
	 * @param participant the participant whose page will be displayed in this part
	 */
	public ParticipantPagePane(Shell shell, boolean isModal, ISynchronizePageConfiguration pageConfiguration, ISynchronizeParticipant participant) {
		this.isModal = isModal;
		this.shell = shell;
		this.participant = participant;
		this.pageConfiguration = pageConfiguration;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.SaveablePartAdapter#dispose()
	 */
	public void dispose() {
		if(titleImage != null) {
			titleImage.dispose();
		}
		if (fPage != null) {
			fPage.dispose();
		}
		if (site != null)
			site.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleImage()
	 */
	public Image getTitleImage() {
		if(titleImage == null) {
			titleImage = participant.getImageDescriptor().createImage();
		}
		return titleImage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitle()
	 */
	public String getTitle() {
		return Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, participant.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		top.setLayout(layout);

		shell = parent.getShell();

		fEditionPane = new CompareViewerPane(top, SWT.BORDER | SWT.FLAT);
		fEditionPane.setText(TeamUIMessages.ParticipantPageSaveablePart_0);

		fEditionPane.setLayoutData(SWTUtils.createHVFillGridData());

		fPage = participant.createPage(pageConfiguration);
		site = new DialogSynchronizePageSite(shell, isModal);
		((SynchronizePageConfiguration)pageConfiguration).setSite(site);
		ToolBarManager tbm = CompareViewerPane.getToolBarManager(fEditionPane);
		site.createActionBars(tbm);
		try {
			((ISynchronizePage)fPage).init(pageConfiguration.getSite());
		} catch (PartInitException e1) {
		   TeamUIPlugin.log(IStatus.ERROR, TeamUIMessages.ParticipantPagePane_0, e1);
		}

		fPage.createControl(fEditionPane);
		fPage.setActionBars(site.getActionBars());
		fEditionPane.setContent(fPage.getControl());
		tbm.update(true);

		return top;
	}

	/**
	 * Return the synchronize page configuration for this part
	 *
	 * @return Returns the pageConfiguration.
	 */
	public ISynchronizePageConfiguration getPageConfiguration() {
		return pageConfiguration;
	}

	/**
	 * Return the Synchronize participant for this part
	 *
	 * @return Returns the participant.
	 */
	public ISynchronizeParticipant getParticipant() {
		return participant;
	}
}
