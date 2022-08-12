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
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.INavigatable;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ICacheListener;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.Saveable;

/**
 * A saveable based compare editor input for compare inputs from a {@link ModelSynchronizeParticipant}.
 */
public class ModelCompareEditorInput extends SaveableCompareEditorInput implements IPropertyChangeListener {

	private static final String IGNORE_WHITSPACE_PAGE_PROPERTY = "org.eclipse.compare." + CompareConfiguration.IGNORE_WHITESPACE; //$NON-NLS-1$

	private final ModelSynchronizeParticipant participant;
	private final ICompareInput input;
	private final ICacheListener contextListener;
	private final ISynchronizePageConfiguration synchronizeConfiguration;

	public ModelCompareEditorInput(ModelSynchronizeParticipant participant, ICompareInput input, IWorkbenchPage page, ISynchronizePageConfiguration synchronizeConfiguration) {
		super(createCompareConfiguration(synchronizeConfiguration), page);
		this.synchronizeConfiguration = synchronizeConfiguration;
		Assert.isNotNull(participant);
		Assert.isNotNull(input);
		this.participant = participant;
		this.input = input;
		contextListener = cache -> closeEditor(true);
		getCompareConfiguration().addPropertyChangeListener(this);
		setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title, new String[] { input.getName() }));
	}

	private static CompareConfiguration createCompareConfiguration(ISynchronizePageConfiguration pageConfiguration) {
		CompareConfiguration compareConfiguration = new CompareConfiguration();
		Object o = pageConfiguration.getProperty(IGNORE_WHITSPACE_PAGE_PROPERTY);
		if (o != null && (o.equals(Boolean.TRUE) || o.equals(Boolean.FALSE))) {
			compareConfiguration.setProperty(CompareConfiguration.IGNORE_WHITESPACE, o);
		}
		return compareConfiguration;
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		participant.getContext().getCache().addCacheListener(contextListener);
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		participant.getContext().getCache().removeCacheListener(contextListener);
		getCompareConfiguration().removePropertyChangeListener(this);
		ICompareNavigator navigator = (ICompareNavigator)synchronizeConfiguration.getProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR);
		if (navigator != null && navigator == super.getNavigator()) {
			synchronizeConfiguration.setProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR, new CompareNavigator() {
				@Override
				protected INavigatable[] getNavigatables() {
					return new INavigatable[0];
				}
			});
		}
	}

	@Override
	protected Saveable createSaveable() {
		if (input instanceof ISynchronizationCompareInput) {
			ISynchronizationCompareInput mci = (ISynchronizationCompareInput) input;
			SaveableComparison compareModel = mci.getSaveable();
			if (compareModel != null)
				return compareModel;
		}
		return super.createSaveable();
	}

	@Override
	protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		monitor.beginTask(TeamUIMessages.SyncInfoCompareInput_3, 100);
		monitor.setTaskName(TeamUIMessages.SyncInfoCompareInput_3);
		getCompareConfiguration().setLeftEditable(isLeftEditable(input));
		getCompareConfiguration().setRightEditable(false);
		try {
			ISynchronizationCompareInput adapter = asModelCompareInput(input);
			if (adapter != null) {
				adapter.prepareInput(getCompareConfiguration(), Policy.subMonitorFor(monitor, 100));
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
		return input;
	}

	private ISynchronizationCompareInput asModelCompareInput(ICompareInput input) {
		return Adapters.adapt(input, ISynchronizationCompareInput.class);
	}

	/**
	 * Return whether the compare input of this editor input matches the
	 * given object.
	 * @param object the object
	 * @param participant the participant associated with the given object
	 * @return whether the compare input of this editor input matches the
	 * given object
	 */
	public boolean matches(Object object, ISynchronizeParticipant participant) {
		if (participant == this.participant && input instanceof ISynchronizationCompareInput) {
			ISynchronizationCompareInput mci = (ISynchronizationCompareInput) input;
			return mci.isCompareInputFor(object);
		}
		return false;
	}

	@Override
	public String getToolTipText() {
		String fullPath;
		ISynchronizationCompareInput adapter = asModelCompareInput(input);
		if (adapter != null) {
			fullPath = adapter.getFullPath();
		} else {
			fullPath = getName();
		}
		return NLS.bind(TeamUIMessages.SyncInfoCompareInput_tooltip, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, participant.getName()), fullPath });
	}

	@Override
	protected void fireInputChange() {
		if (input instanceof ResourceDiffCompareInput) {
			ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) input;
			rdci.fireChange();
		}
	}

	@Override
	public void registerContextMenu(MenuManager menu, ISelectionProvider provider) {
		super.registerContextMenu(menu, provider);
		Saveable saveable = getSaveable();
		if (saveable instanceof LocalResourceSaveableComparison) {
			menu.addMenuListener(this::handleMenuAboutToShow);
		}
	}

	protected void handleMenuAboutToShow(IMenuManager manager) {
		StructuredSelection selection = new StructuredSelection(((IResourceProvider)getCompareInput()).getResource());
		final ResourceMarkAsMergedHandler markAsMergedHandler = new ResourceMarkAsMergedHandler(getSynchronizeConfiguration());
		markAsMergedHandler.updateEnablement(selection);
		Action markAsMergedAction = new Action(TeamUIMessages.ModelCompareEditorInput_0) {
			@Override
			public void run() {
				try {
					markAsMergedHandler.execute(new ExecutionEvent());
				} catch (ExecutionException e) {
					TeamUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}

		};
		Utils.initAction(markAsMergedAction, "action.markAsMerged."); //$NON-NLS-1$
		markAsMergedAction.setEnabled(markAsMergedAction.isEnabled());

		final ResourceMergeHandler mergeHandler = new ResourceMergeHandler(getSynchronizeConfiguration(), false);
		mergeHandler.updateEnablement(selection);
		Action mergeAction = new Action(TeamUIMessages.ModelCompareEditorInput_1) {
			@Override
			public void run() {
				try {
					mergeHandler.execute(new ExecutionEvent());
				} catch (ExecutionException e) {
					TeamUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}

		};
		Utils.initAction(mergeAction, "action.merge."); //$NON-NLS-1$
		mergeAction.setEnabled(mergeAction.isEnabled());

		final ResourceMergeHandler overwriteHandler = new ResourceMergeHandler(getSynchronizeConfiguration(), true);
		overwriteHandler.updateEnablement(selection);
		Action overwriteAction = new Action(TeamUIMessages.ModelCompareEditorInput_2) {
			@Override
			public void run() {
				try {
					overwriteHandler.execute(new ExecutionEvent());
				} catch (ExecutionException e) {
					TeamUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}

		};
		Utils.initAction(overwriteAction, "action.overwrite."); //$NON-NLS-1$
		overwriteAction.setEnabled(overwriteAction.isEnabled());

		manager.insertAfter(IWorkbenchActionConstants.MB_ADDITIONS, new Separator("merge")); //$NON-NLS-1$
		manager.insertAfter("merge", new Separator("overwrite")); //$NON-NLS-1$ //$NON-NLS-2$
		manager.insertAfter("merge", markAsMergedAction); //$NON-NLS-1$
		manager.insertAfter("merge", mergeAction); //$NON-NLS-1$
		manager.insertAfter("overwrite", overwriteAction); //$NON-NLS-1$
	}

	protected ISynchronizePageConfiguration getSynchronizeConfiguration() {
		return synchronizeConfiguration;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(CompareConfiguration.IGNORE_WHITESPACE)) {
			synchronizeConfiguration.setProperty(IGNORE_WHITSPACE_PAGE_PROPERTY, event.getNewValue());
		}
	}

	@Override
	public boolean belongsTo(Object family) {
		return super.belongsTo(family) || family == participant;
	}

	@Override
	public synchronized ICompareNavigator getNavigator() {
		if (isSelectedInSynchronizeView()) {
			ICompareNavigator nav = (ICompareNavigator)synchronizeConfiguration.getProperty(SynchronizePageConfiguration.P_NAVIGATOR);
			// Set the input navigator property so that the advisor can get at it if needed.
			synchronizeConfiguration.setProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR, super.getNavigator());
			return nav;
		}
		return super.getNavigator();
	}

	private boolean isSelectedInSynchronizeView() {
		ISelection s = synchronizeConfiguration.getSite().getSelectionProvider().getSelection();
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) s;
			Object element = ss.getFirstElement();
			return matches(element, participant);
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof ModelCompareEditorInput) {
			ModelCompareEditorInput other = (ModelCompareEditorInput) obj;
			return other.input.equals(input) && other.participant.equals(participant);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return input.hashCode();
	}

	private boolean isLeftEditable(ICompareInput input) {
		Object left = input.getLeft();
		if (left instanceof IEditableContent) {
			return ((IEditableContent) left).isEditable();
		}
		return false;
	}

}
