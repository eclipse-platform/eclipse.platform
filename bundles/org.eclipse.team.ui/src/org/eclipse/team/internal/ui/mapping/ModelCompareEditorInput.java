/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ICache;
import org.eclipse.team.core.ICacheListener;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

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
		contextListener = new ICacheListener() {
			@Override
			public void cacheDisposed(ICache cache) {
				closeEditor(true);
			}
		};
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

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#contentsCreated()
	 */
	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		participant.getContext().getCache().addCacheListener(contextListener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#handleDispose()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.LocalResourceCompareEditorInput#createSaveable()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#prepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
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
		return (ISynchronizationCompareInput)Utils.getAdapter(input, ISynchronizationCompareInput.class);
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.LocalResourceCompareEditorInput#fireInputChange()
	 */
	@Override
	protected void fireInputChange() {
		if (input instanceof ResourceDiffCompareInput) {
			ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) input;
			rdci.fireChange();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#registerContextMenu(org.eclipse.jface.action.MenuManager)
	 */
	@Override
	public void registerContextMenu(MenuManager menu, ISelectionProvider provider) {
		super.registerContextMenu(menu, provider);
		Saveable saveable = getSaveable();
		if (saveable instanceof LocalResourceSaveableComparison) {
			menu.addMenuListener(new IMenuListener() {
				@Override
				public void menuAboutToShow(IMenuManager manager) {
					handleMenuAboutToShow(manager);
				}
			});
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

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#belongsTo(java.lang.Object)
	 */
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

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
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
