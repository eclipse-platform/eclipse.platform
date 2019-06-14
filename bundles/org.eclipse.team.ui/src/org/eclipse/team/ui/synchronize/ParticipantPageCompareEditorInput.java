/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.DialogSynchronizePageSite;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.ui.PageCompareEditorInput;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Displays a synchronize participant page combined with the compare/merge infrastructure. This only works if the
 * synchronize page viewer provides selections that are of the following types: ITypedElement and ICompareInput
 * or if the participant is a {@link ModelSynchronizeParticipant}.
 *
 * @since 3.3
 */
public class ParticipantPageCompareEditorInput extends PageCompareEditorInput {

	private ISynchronizeParticipant participant;
	private ISynchronizePageConfiguration pageConfiguration;

	private Image titleImage;
	private IPageBookViewPage page;
	private DialogSynchronizePageSite site;
	private IPropertyChangeListener listener;
	private Button rememberParticipantButton;

	/**
	 * Creates a part for the provided participant. The page configuration is used when creating the participant page and the resulting
	 * compare/merge panes will be configured with the provided compare configuration.
	 * <p>
	 * For example, clients can decide if the user can edit the compare panes by calling {@link CompareConfiguration#setLeftEditable(boolean)}
	 * or {@link CompareConfiguration#setRightEditable(boolean)}.
	 * </p>
	 * @param configuration the compare configuration that will be used to create the compare panes
	 * @param pageConfiguration the configuration that will be provided to the participant prior to creating the page
	 * @param participant the participant whose page will be displayed in this part
	 */
	public ParticipantPageCompareEditorInput(
			CompareConfiguration configuration,
			ISynchronizePageConfiguration pageConfiguration,
			ISynchronizeParticipant participant) {
		super(configuration);
		this.pageConfiguration = pageConfiguration;
		this.participant = participant;
		pageConfiguration.setRunnableContext(this);
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		setTitle(Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, participant.getName()));
		return participant;
	}

	@Override
	public Image getTitleImage() {
		if(titleImage == null) {
			titleImage = participant.getImageDescriptor().createImage();
		}
		return titleImage;
	}

	@Override
	protected void handleDispose() {
		if(titleImage != null) {
			titleImage.dispose();
		}
		if (page != null)
			page.dispose();
		if (site != null)
			site.dispose();
		pageConfiguration.removePropertyChangeListener(listener);
		super.handleDispose();
	}

	@Override
	protected IPage createPage(CompareViewerPane parent,
			IToolBarManager toolBarManager) {
		listener = event -> {
			if (event.getProperty().equals(ISynchronizePageConfiguration.P_PAGE_DESCRIPTION)) {
				updateDescription();
			}
		};
		pageConfiguration.addPropertyChangeListener(listener);
		updateDescription();

		page = participant.createPage(pageConfiguration);
		site = new DialogSynchronizePageSite(parent.getShell(), true);
		((SynchronizePageConfiguration)pageConfiguration).setSite(site);
		site.createActionBars(toolBarManager);
		try {
			((ISynchronizePage)page).init(pageConfiguration.getSite());
		} catch (PartInitException e1) {
		}

		page.createControl(parent);

		page.setActionBars(site.getActionBars());
		toolBarManager.update(true);
		return page;
	}

	/* private */ void updateDescription() {
		String description = (String)pageConfiguration.getProperty(ISynchronizePageConfiguration.P_PAGE_DESCRIPTION);
		if (description != null) {
			setPageDescription(description);
		}
	}

	@Override
	protected ISelectionProvider getSelectionProvider() {
		return ((ISynchronizePage)page).getViewer();
	}

	@Override
	protected ICompareInput asCompareInput(ISelection selection) {
		ICompareInput compareInput = super.asCompareInput(selection);
		if (compareInput != null)
			return compareInput;

		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o = ss.getFirstElement();
				if (participant instanceof ModelSynchronizeParticipant) {
					ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
					return msp.asCompareInput(o);
				}
			}
		}
		return null;
	}

	@Override
	protected void prepareInput(ICompareInput input,
			CompareConfiguration configuration, IProgressMonitor monitor)
			throws InvocationTargetException {
		monitor.beginTask(TeamUIMessages.SyncInfoCompareInput_3, 100);
		monitor.setTaskName(TeamUIMessages.SyncInfoCompareInput_3);
		try {
			// First, see if the active buffer is changing
			checkForBufferChange(pageConfiguration.getSite().getShell(), input, false /* cancel not allowed */, monitor);
			if (input instanceof SyncInfoModelElement) {
				final SyncInfoModelElement node = (SyncInfoModelElement) input;
				IResource resource = node.getResource();
				if (resource != null && resource.getType() == IResource.FILE) {
					participant.prepareCompareInput(node, configuration, monitor);
				}
			} else {
				ISynchronizationCompareInput adapter = asModelCompareInput(input);
				if (adapter != null) {
					adapter.prepareInput(configuration, Policy.subMonitorFor(monitor, 90));
				}
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	private void checkForBufferChange(Shell shell, final ICompareInput input, boolean cancelAllowed, IProgressMonitor monitor) throws CoreException {
		ISynchronizeParticipant participant = pageConfiguration.getParticipant();
		if (participant instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			if (input instanceof ISynchronizationCompareInput) {
				ISynchronizationCompareInput mci = (ISynchronizationCompareInput) input;
				msp.checkForBufferChange(shell, mci, cancelAllowed, monitor);
			}
		}
	}

	private ISynchronizationCompareInput asModelCompareInput(ICompareInput input) {
		return Adapters.adapt(input, ISynchronizationCompareInput.class);
	}

	@Override
	public boolean isSaveNeeded() {
		if (participant instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			SaveableComparison currentBuffer = msp.getActiveSaveable();
			if (currentBuffer != null) {
				return currentBuffer.isDirty();
			}
		}
		return super.isSaveNeeded();
	}

	@Override
	public void saveChanges(IProgressMonitor monitor) throws CoreException {
		super.saveChanges(monitor);
		Object input = ((ISynchronizePage)page).getViewer().getInput();
		if (input instanceof ISynchronizeModelElement) {
			ISynchronizeModelElement root = (ISynchronizeModelElement)input;
			if (root != null && root instanceof DiffNode) {
				try {
					commit(monitor, (DiffNode)root);
				} catch (CoreException e) {
					Utils.handle(e);
				} finally {
					setDirty(false);
				}
			}
		}
	}

	private static void commit(IProgressMonitor pm, DiffNode node) throws CoreException {
		ITypedElement left = node.getLeft();
		if (left instanceof LocalResourceTypedElement)
			((LocalResourceTypedElement) left).commit(pm);

		ITypedElement right = node.getRight();
		if (right instanceof LocalResourceTypedElement)
			((LocalResourceTypedElement) right).commit(pm);

		IDiffElement[] children = node.getChildren();
		for (IDiffElement c : children) {
			commit(pm, (DiffNode) c);
		}
	}

	@Override
	public void contentChanged(IContentChangeNotifier source) {
		super.contentChanged(source);
		try {
			if (source instanceof DiffNode) {
				commit(new NullProgressMonitor(), (DiffNode) source);
			} else if (source instanceof LocalResourceTypedElement) {
				((LocalResourceTypedElement) source).commit(new NullProgressMonitor());
			}
		} catch (CoreException e) {
			Utils.handle(e);
		}
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

	@Override
	public Control createContents(Composite parent) {
		if (shouldCreateRememberButton()) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			Control control = super.createContents(composite);
			control.setLayoutData(new GridData(GridData.FILL_BOTH));
			rememberParticipantButton = new Button(composite, SWT.CHECK);
			rememberParticipantButton.setText(TeamUIMessages.ParticipantCompareDialog_1);
			rememberParticipantButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			return composite;
		}
		return super.createContents(parent);
	}

	/**
	 * Return whether the ability to remember the participant in the synchronize
	 * view should be presented to the user. By default, <code>true</code> is
	 * returned. Subclasses may override.
	 * @return whether the ability to remember the participant in the synchronize
	 * view should be presented to the user
	 */
	protected boolean isOfferToRememberParticipant() {
		return true;
	}

	private boolean shouldCreateRememberButton() {
		return isOfferToRememberParticipant() && participant != null && ! particantRegisteredWithSynchronizeManager(participant);
	}

	private boolean isRememberParticipant() {
		return getParticipant() != null && rememberParticipantButton != null && rememberParticipantButton.getSelection();
	}

	private boolean particantRegisteredWithSynchronizeManager(ISynchronizeParticipant participant) {
		return TeamUI.getSynchronizeManager().get(participant.getId(), participant.getSecondaryId()) != null;
	}

	private void rememberParticipant() {
		if(getParticipant() != null) {
			ISynchronizeManager mgr = TeamUI.getSynchronizeManager();
			ISynchronizeView view = mgr.showSynchronizeViewInActivePage();
			mgr.addSynchronizeParticipants(new ISynchronizeParticipant[] {getParticipant()});
			view.display(participant);
		}
	}

	@Override
	public boolean okPressed() {
		if (isEditable()) {
			// If the CompareConfiguration is editable, then OK is Save and we want to leave the dialog open
			super.okPressed();
			return false;
		}
		// If the CompareConfiguration is not editable, then the OK button is the done button
		if (isRememberParticipant())
			rememberParticipant();
		return super.okPressed();
	}

	@Override
	public void cancelPressed() {
		// If the CompareConfiguration is editable, then the CANCEL button is the done button
		if (isEditable() && isRememberParticipant())
			rememberParticipant();
		super.cancelPressed();
	}

	@Override
	public String getOKButtonLabel() {
		if (isEditable())
			return TeamUIMessages.ParticipantPageCompareEditorInput_0;
		return TeamUIMessages.ResourceMappingMergeOperation_2;
	}

	private boolean isEditable() {
		return getCompareConfiguration().isLeftEditable()
			|| getCompareConfiguration().isRightEditable();
	}

	@Override
	public String getCancelButtonLabel() {
		return TeamUIMessages.ResourceMappingMergeOperation_2;
	}

}
