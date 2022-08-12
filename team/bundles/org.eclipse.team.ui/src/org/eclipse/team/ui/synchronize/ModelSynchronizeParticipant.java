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
package org.eclipse.team.ui.synchronize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ModelEnablementPreferencePage;
import org.eclipse.team.internal.ui.mapping.ModelSynchronizePage;
import org.eclipse.team.internal.ui.preferences.SyncViewerPreferencePage;
import org.eclipse.team.internal.ui.synchronize.IRefreshSubscriberListener;
import org.eclipse.team.internal.ui.synchronize.IRefreshable;
import org.eclipse.team.internal.ui.synchronize.RefreshModelParticipantJob;
import org.eclipse.team.internal.ui.synchronize.RefreshParticipantJob;
import org.eclipse.team.internal.ui.synchronize.RefreshUserNotificationPolicy;
import org.eclipse.team.internal.ui.synchronize.StartupPreferencePage;
import org.eclipse.team.internal.ui.synchronize.SubscriberRefreshSchedule;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.progress.IProgressConstants2;

/**
 * Synchronize participant that obtains it's synchronization state from
 * a {@link ISynchronizationContext}.
 * <p>
 * This class may be subclassed by clients.
 *
 * @since 3.2
 */
public class ModelSynchronizeParticipant extends
		AbstractSynchronizeParticipant {

	/**
	 * Property constant used to store and retrieve the id of the active
	 * {@link ModelProvider} from an {@link ISynchronizePageConfiguration}. The
	 * active model provider will be the only one visible in the page. If
	 * <code>null</code> or <code>ALL_MODEL_PROVIDERS_ACTIVE</code> is
	 * returned, all model providers are considered active and are visible.
	 */
	public static final String P_VISIBLE_MODEL_PROVIDER = TeamUIPlugin.ID + ".activeModelProvider"; //$NON-NLS-1$

	/**
	 * Constant used with the <code>P_ACTIVE_MODEL_PROVIDER</code> property to indicate
	 * that all enabled model providers are active.
	 */
	public static final String ALL_MODEL_PROVIDERS_VISIBLE = TeamUIPlugin.ID + ".activeModelProvider"; //$NON-NLS-1$

	/**
	 * Property constant used during property change notification to indicate
	 * that the enabled model providers for this participant have changed.
	 */
	public static final String PROP_ENABLED_MODEL_PROVIDERS = TeamUIPlugin.ID + ".ENABLED_MODEL_PROVIDERS"; //$NON-NLS-1$

	/**
	 * Property constant used during property change notification to indicate
	 * that the active model of this participant has changed.
	 */
	public static final String PROP_ACTIVE_SAVEABLE = TeamUIPlugin.ID + ".ACTIVE_SAVEABLE"; //$NON-NLS-1$

	/**
	 * Property constant used during property change notification to indicate
	 * that the dirty state for the active saveable model of this participant has changed.
	 */
	public static final String PROP_DIRTY = TeamUIPlugin.ID + ".DIRTY"; //$NON-NLS-1$

	/*
	 * Key for settings in memento
	 */
	private static final String CTX_PARTICIPANT_SETTINGS = TeamUIPlugin.ID + ".MODEL_PARTICIPANT_SETTINGS"; //$NON-NLS-1$

	/*
	 * Key for schedule in memento
	 */
	private static final String CTX_REFRESH_SCHEDULE_SETTINGS = TeamUIPlugin.ID + ".MODEL_PARTICIPANT_REFRESH_SCHEDULE"; //$NON-NLS-1$

	/*
	 * Key for description in memento
	 */
	private static final String CTX_DESCRIPTION = TeamUIPlugin.ID + ".MODEL_PARTICIPANT_DESCRIPTION"; //$NON-NLS-1$

	/*
	 * Constants used to save and restore this scope
	 */
	private static final String CTX_PARTICIPANT_MAPPINGS = TeamUIPlugin.ID + ".MODEL_PARTICIPANT_MAPPINGS"; //$NON-NLS-1$
	private static final String CTX_MODEL_PROVIDER_ID = "modelProviderId"; //$NON-NLS-1$
	private static final String CTX_MODEL_PROVIDER_MAPPINGS = "mappings"; //$NON-NLS-1$
	private static final String CTX_STARTUP_ACTION = "startupAction"; //$NON-NLS-1$

	private SynchronizationContext context;
	private boolean mergingEnabled = true;
	private SubscriberRefreshSchedule refreshSchedule;
	private String description;
	private SaveableComparison activeSaveable;
	private PreferenceStore preferences = new PreferenceStore() {
		@Override
		public void save() throws IOException {
			// Nothing to do. Preference will be saved with participant.
		}
	};

	private IPropertyListener dirtyListener = (source, propId) -> {
		if (source instanceof SaveableComparison && propId == SaveableComparison.PROP_DIRTY) {
			SaveableComparison scm = (SaveableComparison) source;
			boolean isDirty = scm.isDirty();
			firePropertyChange(ModelSynchronizeParticipant.this, PROP_DIRTY, Boolean.valueOf(!isDirty), Boolean.valueOf(isDirty));
		}
	};

	/**
	 * Create a participant for the given context and name
	 * @param context the synchronization context
	 * @param name the name of the participant
	 * @return a participant for the given context
	 */
	public static ModelSynchronizeParticipant createParticipant(SynchronizationContext context, String name) {
		return new ModelSynchronizeParticipant(context, name);
	}

	/*
	 * Create a participant for the given context
	 * @param context the synchronization context
	 * @param name the name of the participant
	 */
	private ModelSynchronizeParticipant(SynchronizationContext context, String name) {
		initializeContext(context);
		try {
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor("org.eclipse.team.ui.synchronization_context_synchronize_participant")); //$NON-NLS-1$
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		setSecondaryId(Long.toString(System.currentTimeMillis()));
		setName(name);
		refreshSchedule = new SubscriberRefreshSchedule(createRefreshable());
	}

	/**
	 * Create a participant for the given context
	 * @param context the synchronization context
	 */
	public ModelSynchronizeParticipant(SynchronizationContext context) {
		initializeContext(context);
		refreshSchedule = new SubscriberRefreshSchedule(createRefreshable());
	}

	/**
	 * Create a participant in order to restore it from saved state.
	 */
	public ModelSynchronizeParticipant() {
	}

	@Override
	public String getName() {
		String name = super.getName();
		if (description == null)
			description = Utils.getScopeDescription(getContext().getScope());
		return NLS.bind(TeamUIMessages.SubscriberParticipant_namePattern, new String[] { name, description });
	}

	/**
	 * Return the name of the participant as specified in the plugin manifest file.
	 * This method is provided to give access to this name since it is masked by
	 * the <code>getName()</code> method defined in this class.
	 * @return the name of the participant as specified in the plugin manifest file
	 * @since 3.1
	 */
	protected final String getShortName() {
		return super.getName();
	}

	@Override
	protected void initializeConfiguration(
			ISynchronizePageConfiguration configuration) {
		if (isMergingEnabled()) {
			// The context menu groups are defined by the org.eclipse.ui.navigator.viewer extension
			configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, ModelSynchronizeParticipantActionGroup.MERGE_ACTION_GROUP);
			configuration.addActionContribution(createMergeActionGroup());
		}
		configuration.setSupportedModes(ISynchronizePageConfiguration.ALL_MODES);
		configuration.setMode(ISynchronizePageConfiguration.BOTH_MODE);
		configuration.setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT, getContext());
		configuration.setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE, getContext().getScope());
		if (getHandler() != null)
			configuration.setProperty(StartupPreferencePage.STARTUP_PREFERENCES, preferences);
	}

	/**
	 * Create the merge action group for this participant.
	 * Subclasses can override in order to provide a
	 * merge action group that configures certain aspects
	 * of the merge actions.
	 * @return the merge action group for this participant
	 */
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new ModelSynchronizeParticipantActionGroup();
	}

	@Override
	public final IPageBookViewPage createPage(
			ISynchronizePageConfiguration configuration) {
		return new ModelSynchronizePage(configuration);
	}

	@Override
	public void run(IWorkbenchPart part) {
		refresh(part != null ? part.getSite() : null, context.getScope().getMappings());
	}

	/**
	 * Refresh a participant in the background the result of the refresh are shown in the progress view. Refreshing
	 * can also be considered synchronizing, or refreshing the synchronization state. Basically this is a long
	 * running operation that will update the participant's context with new changes detected on the
	 * server.
	 *
	 * @param site the workbench site the synchronize is running from. This can be used to notify the site
	 * that a job is running.
	 * @param mappings the resource mappings to be refreshed
	 */
	public final void refresh(IWorkbenchSite site, ResourceMapping[] mappings) {
		IRefreshSubscriberListener listener = new RefreshUserNotificationPolicy(this);
		internalRefresh(mappings, null, null, site, listener);
	}

	@Override
	public void dispose() {
		context.dispose();
		Job.getJobManager().cancel(this);
		refreshSchedule.dispose();
	}

	/**
	 * Set the context of this participant. This method must be invoked
	 * before a page is obtained from the participant.
	 * @param context the context for this participant
	 */
	protected void initializeContext(SynchronizationContext context) {
		this.context = context;
		mergingEnabled = context instanceof IMergeContext;
		SubscriberDiffTreeEventHandler handler = getHandler();
		if (handler != null) {
			preferences.setDefault(StartupPreferencePage.PROP_STARTUP_ACTION, StartupPreferencePage.STARTUP_ACTION_NONE);
			if (isSynchronizeOnStartup()) {
				run(null); // TODO: Would like to get the Sync view part if possible
			} else if (isPopulateOnStartup()) {
				handler.initializeIfNeeded();
			}
		}
	}

	private boolean isPopulateOnStartup() {
		String pref = preferences.getString(StartupPreferencePage.PROP_STARTUP_ACTION);
		return pref != null && pref.equals(StartupPreferencePage.STARTUP_ACTION_POPULATE);
	}

	private boolean isSynchronizeOnStartup() {
		String pref = preferences.getString(StartupPreferencePage.PROP_STARTUP_ACTION);
		return pref != null && pref.equals(StartupPreferencePage.STARTUP_ACTION_SYNCHRONIZE);
	}

	/**
	 * Return the synchronization context for this participant.
	 * @return the synchronization context for this participant
	 */
	public ISynchronizationContext getContext() {
		return context;
	}

	/**
	 * Return a compare input for the given model object or <code>null</code>
	 * if the object is not eligible for comparison.
	 * @param object the model object
	 * @return a compare input for the model object or <code>null</code>
	 */
	public ICompareInput asCompareInput(Object object) {
		if (object instanceof ICompareInput) {
			return (ICompareInput) object;
		}
		// Get a compare input from the model provider's compare adapter
		ISynchronizationCompareAdapter adapter = Utils.getCompareAdapter(object);
		if (adapter != null)
			return adapter.asCompareInput(getContext(), object);
		return null;
	}

	/**
	 * Return whether their is a compare input associated with the given object.
	 * In other words, return <code>true</code> if {@link #asCompareInput(Object) }
	 * would return a value and <code>false</code> if it would return <code>null</code>.
	 * @param object the object.
	 * @return whether their is a compare input associated with the given object
	 */
	public boolean hasCompareInputFor(Object object) {
		// Get a content viewer from the model provider's compare adapter
		ISynchronizationCompareAdapter adapter = Utils.getCompareAdapter(object);
		if (adapter != null)
			return adapter.hasCompareInput(getContext(), object);
		return false;
	}

	/**
	 * Return whether merge capabilities are enabled for this participant.
	 * If merging is enabled, merge actions can be shown. If merging is disabled, no
	 * merge actions should be surfaced.
	 * @return whether merge capabilities should be enabled for this participant
	 */
	public boolean isMergingEnabled() {
		return mergingEnabled;
	}

	/**
	 * Set whether merge capabilities should be enabled for this participant.
	 * @param mergingEnabled whether merge capabilities should be enabled for this participant
	 */
	public void setMergingEnabled(boolean mergingEnabled) {
		this.mergingEnabled = mergingEnabled;
	}

	private void internalRefresh(ResourceMapping[] mappings, String jobName, String taskName, IWorkbenchSite site, IRefreshSubscriberListener listener) {
		if (jobName == null)
			jobName = getShortTaskName();
		if (taskName == null)
			taskName = getLongTaskName(mappings);
		Job.getJobManager().cancel(this);
		RefreshParticipantJob job = new RefreshModelParticipantJob(this, jobName, taskName, mappings, listener);
		job.setUser(true);
		job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
		Utils.schedule(job, site);

		// Remember the last participant synchronized
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT, getId());
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID, getSecondaryId());
	}

	/**
	 * Returns the short task name (e.g. no more than 25 characters) to describe
	 * the behavior of the refresh operation to the user. This is typically
	 * shown in the status line when this participant is refreshed in the
	 * background. When refreshed in the foreground, only the long task name is
	 * shown.
	 *
	 * @return the short task name to show in the status line.
	 */
	protected String getShortTaskName() {
		return NLS.bind(TeamUIMessages.Participant_synchronizingDetails, getShortName());
	}

	/**
	 * Returns the long task name to describe the behavior of the refresh
	 * operation to the user. This is typically shown in the status line when
	 * this subscriber is refreshed in the background.
	 *
	 * @param mappings the mappings being refreshed
	 * @return the long task name
	 * @since 3.1
	 */
	protected String getLongTaskName(ResourceMapping[] mappings) {
		if (mappings == null) {
			// If the mappings are null, assume we are refreshing everything
			mappings = getContext().getScope().getMappings();
		}
		int mappingCount = mappings.length;
		if (mappingCount == getContext().getScope().getMappings().length) {
			// Assume we are refreshing everything and only use the input mapping count
			mappings = getContext().getScope().getInputMappings();
			mappingCount = mappings.length;
		}
		if (mappingCount == 1) {
			return NLS.bind(TeamUIMessages.Participant_synchronizingMoreDetails, new String[] { getShortName(), Utils.getLabel(mappings[0]) });
		}
		return NLS.bind(TeamUIMessages.Participant_synchronizingResources, new String[] { getShortName(), Integer.toString(mappingCount) });
	}

	private IRefreshable createRefreshable() {
		return new IRefreshable() {

			@Override
			public RefreshParticipantJob createJob(String interval) {
				String jobName = NLS.bind(TeamUIMessages.RefreshSchedule_15, new String[] { ModelSynchronizeParticipant.this.getName(), interval });
				return new RefreshModelParticipantJob(ModelSynchronizeParticipant.this,
						jobName,
						jobName,
						context.getScope().getMappings(),
						new RefreshUserNotificationPolicy(ModelSynchronizeParticipant.this));
			}
			@Override
			public ISynchronizeParticipant getParticipant() {
				return ModelSynchronizeParticipant.this;
			}
			@Override
			public void setRefreshSchedule(SubscriberRefreshSchedule schedule) {
				ModelSynchronizeParticipant.this.setRefreshSchedule(schedule);
			}
			@Override
			public SubscriberRefreshSchedule getRefreshSchedule() {
				return refreshSchedule;
			}

		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IRefreshable.class && refreshSchedule != null) {
			return (T) refreshSchedule.getRefreshable();

		}
		if (adapter == SubscriberRefreshSchedule.class) {
			return (T) refreshSchedule;
		}
		return super.getAdapter(adapter);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IMemento settings = memento.createChild(CTX_PARTICIPANT_SETTINGS);
		if (description != null)
			settings.putString(CTX_DESCRIPTION, description);
		refreshSchedule.saveState(settings.createChild(CTX_REFRESH_SCHEDULE_SETTINGS));
		saveMappings(settings);
		settings.putString(CTX_STARTUP_ACTION, preferences.getString(StartupPreferencePage.PROP_STARTUP_ACTION));
	}

	private void saveMappings(IMemento settings) {
		ISynchronizationScope inputScope = getContext().getScope().asInputScope();
		ModelProvider[] providers = inputScope.getModelProviders();
		for (ModelProvider provider : providers) {
			ISynchronizationCompareAdapter adapter = Utils.getCompareAdapter(provider);
			if (adapter != null) {
				IMemento child = settings.createChild(CTX_PARTICIPANT_MAPPINGS);
				String id = provider.getDescriptor().getId();
				child.putString(CTX_MODEL_PROVIDER_ID, id);
				adapter.save(inputScope.getMappings(id), child.createChild(CTX_MODEL_PROVIDER_MAPPINGS));
			}
		}
	}

	@Override
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		super.init(secondaryId, memento);
		if(memento != null) {
			IMemento settings = memento.getChild(CTX_PARTICIPANT_SETTINGS);
			String startupAction = settings.getString(StartupPreferencePage.PROP_STARTUP_ACTION);
			if (startupAction != null)
				preferences.putValue(StartupPreferencePage.PROP_STARTUP_ACTION, startupAction);
			ResourceMapping[] mappings = loadMappings(settings);
			if (mappings.length == 0)
				throw new PartInitException(NLS.bind(TeamUIMessages.ModelSynchronizeParticipant_0, getId()));
			initializeContext(mappings);
			if(settings != null) {
				SubscriberRefreshSchedule schedule = SubscriberRefreshSchedule.init(settings.getChild(CTX_REFRESH_SCHEDULE_SETTINGS), createRefreshable());
				description = settings.getString(CTX_DESCRIPTION);
				setRefreshSchedule(schedule);
				if(schedule.isEnabled()) {
					schedule.startJob();
				}
			}
		}
	}

	private ResourceMapping[] loadMappings(IMemento settings) throws PartInitException {
		List<ResourceMapping> result = new ArrayList<>();
		IMemento[] children = settings.getChildren(CTX_PARTICIPANT_MAPPINGS);
		for (IMemento memento : children) {
			String id = memento.getString(CTX_MODEL_PROVIDER_ID);
			if (id != null) {
				IModelProviderDescriptor desc = ModelProvider.getModelProviderDescriptor(id);
				try {
					ModelProvider provider = desc.getModelProvider();
					ISynchronizationCompareAdapter adapter = Utils.getCompareAdapter(provider);
					if (adapter != null) {
						ResourceMapping[] mappings = adapter.restore(memento.getChild(CTX_MODEL_PROVIDER_MAPPINGS));
						Collections.addAll(result, mappings);
					}
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	private void initializeContext(ResourceMapping[] mappings) throws PartInitException {
		try {
			ISynchronizationScopeManager manager = createScopeManager(mappings);
			MergeContext context = restoreContext(manager);
			initializeContext(context);
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
			throw new PartInitException(e.getStatus());
		}
	}

	/**
	 * Recreate the context for this participant. This method is invoked when
	 * the participant is restored after a restart. Although it is provided
	 * with a progress monitor, long running operations should be avoided.
	 * @param manager the restored scope
	 * @return the context for this participant
	 * @throws CoreException if restoring context failed
	 */
	protected MergeContext restoreContext(ISynchronizationScopeManager manager) throws CoreException {
		throw new PartInitException(NLS.bind(TeamUIMessages.ModelSynchronizeParticipant_1, getId()));
	}

	/**
	 * Create and return a scope manager that can be used to build the scope of this
	 * participant when it is restored after a restart. By default, this method
	 * returns a scope manager that uses the local content.
	 * This method can be overridden by subclasses.
	 *
	 * @param mappings the restored mappings
	 * @return a scope manager that can be used to build the scope of this
	 * participant when it is restored after a restart
	 */
	protected ISynchronizationScopeManager createScopeManager(ResourceMapping[] mappings) {
		return new SynchronizationScopeManager(super.getName(), mappings, ResourceMappingContext.LOCAL_CONTEXT, true);
	}

	/* private */ void setRefreshSchedule(SubscriberRefreshSchedule schedule) {
		if (refreshSchedule != schedule) {
			if (refreshSchedule != null) {
				refreshSchedule.dispose();
			}
			this.refreshSchedule = schedule;
		}
		// Always fir the event since the schedule may have been changed
		firePropertyChange(this, AbstractSynchronizeParticipant.P_SCHEDULED, schedule, schedule);
	}

	/**
	 * Return the active saveable for this participant.
	 * There is at most one saveable active at any
	 * time.
	 * @return the active saveable for this participant
	 * or <code>null</code>
	 */
	public SaveableComparison getActiveSaveable() {
		return activeSaveable;
	}

	/**
	 * Set the active saveable of this participant.
	 * @param activeSaveable the active saveable (may be <code>null</code>)
	 */
	public void setActiveSaveable(SaveableComparison activeSaveable) {
		boolean wasDirty = false;
		SaveableComparison oldModel = this.activeSaveable;
		if (oldModel != null) {
			oldModel.removePropertyListener(dirtyListener);
			wasDirty = oldModel.isDirty();
		}
		this.activeSaveable = activeSaveable;
		firePropertyChange(this, PROP_ACTIVE_SAVEABLE, oldModel, activeSaveable);
		boolean isDirty = false;
		if (activeSaveable != null) {
			activeSaveable.addPropertyListener(dirtyListener);
			isDirty = activeSaveable.isDirty();
		}
		if (isDirty != wasDirty)
			firePropertyChange(this, PROP_DIRTY, Boolean.valueOf(wasDirty), Boolean.valueOf(isDirty));
	}

	/**
	 * Convenience method for switching the active saveable of this participant
	 * to the saveable of the given input.
	 * @param shell a shell
	 * @param input the compare input about to be displayed
	 * @param cancelAllowed whether the display of the compare input can be canceled
	 * @param monitor a progress monitor or <code>null</code> if progress reporting is not required
	 * @return whether the user choose to continue with the display of the given compare input
	 * @throws CoreException if an error occurs
	 */
	public boolean checkForBufferChange(Shell shell, ISynchronizationCompareInput input, boolean cancelAllowed, IProgressMonitor monitor) throws CoreException {
		SaveableComparison currentBuffer = getActiveSaveable();
		SaveableComparison targetBuffer = input.getSaveable();
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			ModelParticipantAction.handleTargetSaveableChange(shell, targetBuffer, currentBuffer, cancelAllowed, Policy.subMonitorFor(monitor, 10));
		} catch (InterruptedException e) {
			return false;
		}
		setActiveSaveable(targetBuffer);
		return true;
	}

	/**
	 * Return the list of model providers that are enabled for the participant.
	 * By default, the list is those model providers that contain mappings
	 * in the scope. Subclasses may override to add additional model providers.
	 * @return the list of model providers that are active for the participant
	 */
	public ModelProvider[] getEnabledModelProviders() {
		return getContext().getScope().getModelProviders();
	}

	@Override
	public PreferencePage[] getPreferencePages() {
		List<PreferencePage> pages = new ArrayList<>();
		SyncViewerPreferencePage syncViewerPreferencePage = new SyncViewerPreferencePage();
		syncViewerPreferencePage.setIncludeDefaultLayout(false);
		pages.add(syncViewerPreferencePage);
		pages.add(new ModelEnablementPreferencePage());
		ITeamContentProviderDescriptor[] descriptors = TeamUI.getTeamContentProviderManager().getDescriptors();
		for (ITeamContentProviderDescriptor descriptor : descriptors) {
			if (isIncluded(descriptor)) {
				try {
					PreferencePage page = (PreferencePage)descriptor.createPreferencePage();
					if (page != null) {
						pages.add(page);
					}
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
			}
		}
		if (getHandler() != null) {
			pages.add(new StartupPreferencePage(preferences));
		}
		return pages.toArray(new PreferencePage[pages.size()]);
	}

	private boolean isIncluded(ITeamContentProviderDescriptor descriptor) {
		ModelProvider[] providers = getEnabledModelProviders();
		for (ModelProvider provider : providers) {
			if (provider.getId().equals(descriptor.getModelProviderId())) {
				return true;
			}
		}
		return false;
	}

	private SubscriberDiffTreeEventHandler getHandler() {
		return Adapters.adapt(context, SubscriberDiffTreeEventHandler.class);
	}

}
