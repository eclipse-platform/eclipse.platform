/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Pawel Piech - Bug 154598: DebugModelContextBindingManager does not use IAdaptable.getAdapter() to retrieve IDebugModelProvider adapter
 *     Pawel Piech - Bug 298648:  [View Management] Race conditions and other issues make view management unreliable.
 *******************************************************************************/
package org.eclipse.debug.internal.ui.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugModelProvider;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ActivityManagerEvent;
import org.eclipse.ui.activities.IActivity;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IActivityManagerListener;
import org.eclipse.ui.activities.IActivityPatternBinding;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.progress.UIJob;

/**
 * Manages <code>debugModelContextBindings</code> extensions.
 * <p>
 * As debug contexts are activated, associated <code>org.eclipse.ui.contexts</code>
 * are activated. When a debug session (launch) terminates, the associated contexts
 * are disabled. Debug model activation also triggers assocaited activities.
 * </p>
 * @since 3.2
 */
public class DebugModelContextBindingManager implements IDebugContextListener, ILaunchesListener2, IActivityManagerListener {

	/**
	 * Map of debug model identifier to associated contexts as defined
	 * by <code>debugModelContextBindings</code> extensions.
	 */
	private Map<String, List<String>> fModelToContextIds = new HashMap<>();

	/**
	 * Map of launch objects to enabled model ids
	 */
	private Map<ILaunch, Set<String>> fLaunchToModelIds = new HashMap<>();

	/**
	 * Map of launch objects to context activations
	 */
	private Map<ILaunch, List<IContextActivation>> fLanuchToContextActivations = new HashMap<>();

	/**
	 * A list of activity pattern bindings for debug models.
	 */
	private List<IActivityPatternBinding> fModelPatternBindings = new ArrayList<>();

	/**
	 * Map of debug model ids to associated activity ids.
	 */
	private Map<String, Set<String>> fModelToActivities = new HashMap<>();

	/**
	 * A set of debug model ids for which activities have been enabled.
	 * Cleared when enabled activities change.
	 */
	private Set<String> fModelsEnabledForActivities = new HashSet<>();

	// extension point
	public static final String ID_DEBUG_MODEL_CONTEXT_BINDINGS= "debugModelContextBindings"; //$NON-NLS-1$

	// extension point attributes
	public static final String ATTR_CONTEXT_ID= "contextId"; //$NON-NLS-1$
	public static final String ATTR_DEBUG_MODEL_ID= "debugModelId"; //$NON-NLS-1$

	// base debug context
	public static final String DEBUG_CONTEXT= "org.eclipse.debug.ui.debugging"; //$NON-NLS-1$

	// suffix for debug activities triggered by debug model context binding activation
	private static final String DEBUG_MODEL_ACTIVITY_SUFFIX = "/debugModel"; //$NON-NLS-1$

	// singleton manager
	private static DebugModelContextBindingManager fgManager;

	private static IContextService fgContextService = PlatformUI.getWorkbench().getAdapter(IContextService.class);

	public static DebugModelContextBindingManager getDefault() {
		if (fgManager == null) {
			fgManager = new DebugModelContextBindingManager();
		}
		return fgManager;
	}

	private DebugModelContextBindingManager() {
		loadDebugModelContextBindings();
		loadDebugModelActivityExtensions();
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
		DebugUITools.getDebugContextManager().addDebugContextListener(this);
		IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
		activitySupport.getActivityManager().addActivityManagerListener(this);
	}

	/**
	 * Loads the extensions which map debug model identifiers
	 * to context ids.
	 */
	private void loadDebugModelContextBindings() {
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), ID_DEBUG_MODEL_CONTEXT_BINDINGS);
		for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
			String modelIdentifier = element.getAttribute(ATTR_DEBUG_MODEL_ID);
			String contextId = element.getAttribute(ATTR_CONTEXT_ID);
			synchronized (this) {
				if (modelIdentifier != null && contextId != null) {
					List<String> contextIds = fModelToContextIds.get(modelIdentifier);
					if (contextIds == null) {
						contextIds = new ArrayList<>();
						fModelToContextIds.put(modelIdentifier, contextIds);
					}
					contextIds.add(contextId);
				}
			}
		}
	}

	/**
	 * Loads the extensions which map debug model patterns
	 * to activity ids. This information is used to activate the
	 * appropriate activities when a debug element is selected.
	 */
	private void loadDebugModelActivityExtensions() {
		IActivityManager activityManager = PlatformUI.getWorkbench().getActivitySupport().getActivityManager();
		Set<String> activityIds = activityManager.getDefinedActivityIds();
		for (String activityId : activityIds) {
			IActivity activity = activityManager.getActivity(activityId);
			if (activity != null) {
				Set<IActivityPatternBinding> patternBindings = activity.getActivityPatternBindings();
				for (IActivityPatternBinding patternBinding : patternBindings) {
					String pattern = patternBinding.getPattern().pattern();
					if (pattern.endsWith(DEBUG_MODEL_ACTIVITY_SUFFIX)) {
						fModelPatternBindings.add(patternBinding);
					}
				}
			}
		}
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		if ((event.getFlags() & DebugContextEvent.ACTIVATED) > 0) {
			ISelection selection = event.getContext();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				Iterator<?> iterator = ss.iterator();
				while (iterator.hasNext()) {
					activated(iterator.next());
				}
			}
		}
	}

	/**
	 * The specified object has been activated. Activate contexts and activities as
	 * required for the object.
	 *
	 * @param object object that has been activated
	 */
	private void activated(Object object) {
		String[] modelIds = getDebugModelIds(object);
		if (modelIds == null) {
			return;
		}
		ILaunch launch = getLaunch(object);
		if (launch == null || launch.isTerminated()) {
			return;
		}
		List<String> toEnable = new ArrayList<>(modelIds.length);
		synchronized (this) {
			Set<String> alreadyEnabled = fLaunchToModelIds.get(launch);
			if (alreadyEnabled == null) {
				alreadyEnabled = new HashSet<>();
				fLaunchToModelIds.put(launch, alreadyEnabled);
			}
			for (String id : modelIds) {
				if (!alreadyEnabled.contains(id)) {
					alreadyEnabled.add(id);
					toEnable.add(id);
				}
			}
		}
		for (String element : toEnable) {
			activateModel(element, launch);
		}

		enableActivitiesFor(modelIds);
	}

	/**
	 * Activates the given model identifier for the specified launch. This activates
	 * associated contexts and all parent contexts for the model.
	 *
	 * @param modelId model to be enabled
	 * @param launch the launch the model is being enabled for
	 */
	private void activateModel(String modelId, ILaunch launch) {
		List<String> contextIds = null;
		synchronized (this) {
			contextIds = fModelToContextIds.get(modelId);
			if (contextIds == null) {
				// if there are no contexts for a model, the base debug context should
				// be activated (i.e. a debug model with no org.eclipse.ui.contexts and
				// associated org.eclipse.debug.ui.modelContextBindings)
				contextIds = new ArrayList<>();
				contextIds.add(DEBUG_CONTEXT);
				fModelToContextIds.put(modelId, contextIds);
			}
		}
		for (String id : contextIds) {
			activateContext(id, launch);
		}
	}

	/**
	 * Activates the given context and all its parent contexts.
	 *
	 * @param contextId
	 * @param launch
	 */
	private void activateContext(String contextId, ILaunch launch) {
		while (contextId != null) {
			Context context = fgContextService.getContext(contextId);
			IContextActivation activation = fgContextService.activateContext(contextId);
			addActivation(launch, activation);
			try {
				if (contextId.equals(DEBUG_CONTEXT)) {
					// don't enable windows contexts and higher
					break;
				}
				contextId = context.getParentId();
			} catch (NotDefinedException e) {
				contextId = null;
				DebugUIPlugin.log(e);
			}
		}
	}

	/**
	 * Notes the activation for a context and launch so we can de-activate later.
	 *
	 * @param launch
	 * @param activation
	 */
	private synchronized void addActivation(ILaunch launch, IContextActivation activation) {
		List<IContextActivation> activations = fLanuchToContextActivations.get(launch);
		if (activations == null) {
			activations = new ArrayList<>();
			fLanuchToContextActivations.put(launch, activations);
		}
		activations.add(activation);
	}

	/**
	 * Returns the debug model identifiers associated with the given object or <code>null</code>
	 * if none.
	 *
	 * @param object
	 * @return debug model identifiers associated with the given object or <code>null</code>
	 */
	private String[] getDebugModelIds(Object object) {
		if (object instanceof IAdaptable) {
			IDebugModelProvider modelProvider= ((IAdaptable)object).getAdapter(IDebugModelProvider.class);
			if (modelProvider != null) {
				String[] modelIds= modelProvider.getModelIdentifiers();
				if (modelIds != null) {
					return modelIds;
				}
			}
		}
		if (object instanceof IStackFrame) {
			return new String[] { ((IStackFrame) object).getModelIdentifier() };
		}
		return null;
	}

	/**
	 * Returns the ILaunch associated with the given object or
	 * <code>null</code> if none.
	 *
	 * @param object object for which launch is required
	 * @return the ILaunch associated with the given object or <code>null</code>
	 */
	public static ILaunch getLaunch(Object object) {
		ILaunch launch = null;
		if (object instanceof IAdaptable) {
			launch = ((IAdaptable)object).getAdapter(ILaunch.class);
		}
		if (launch == null && object instanceof IDebugElement) {
			launch = ((IDebugElement) object).getLaunch();
		}
		return launch;
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		// disable activated contexts
		for (ILaunch launch : launches) {
			List<IContextActivation> activations;
			synchronized(this) {
				activations = fLanuchToContextActivations.remove(launch);
				fLaunchToModelIds.remove(launch);
			}
			if (activations != null) {
				final List<IContextActivation> _activations = activations;
				UIJob job = new UIJob("Deactivate debug contexts") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						for (IContextActivation activation : _activations) {
							activation.getContextService().deactivateContext(activation);
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}

		}
		// TODO: Terminated notification
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {}

	@Override
	public void launchesAdded(ILaunch[] launches) {
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
	}

	/**
	 * Returns the workbench contexts associated with a debug context
	 *
	 * @param target debug context
	 * @return associated workbench contexts
	 */
	public List<String> getWorkbenchContextsForDebugContext(Object target) {
		List<String> workbenchContexts = new ArrayList<>();
		String[] modelIds = getDebugModelIds(target);
		if (modelIds != null) {
			for (String modelId : modelIds) {
				synchronized (this) {
					List<String> contextIds = fModelToContextIds.get(modelId);
					if (contextIds != null) {
						for (String contextId : contextIds) {
							if (!workbenchContexts.contains(contextId)) {
								workbenchContexts.add(contextId);
							}
						}
					}
				}
			}
		}
		return workbenchContexts;
	}

	/**
	 * Enables activities in the workbench associated with the given debug
	 * model ids that have been activated.
	 *
	 * @param debug model ids for which to enable activities
	 */
	private void enableActivitiesFor(String[] modelIds) {
		Set<String> activities = null;
		for (String id : modelIds) {
			if (!fModelsEnabledForActivities.contains(id)) {
				Set<String> ids = fModelToActivities.get(id);
				if (ids == null) {
					// first time the model has been seen, perform pattern matching
					ids = new HashSet<>();
					fModelToActivities.put(id, ids);
					for (IActivityPatternBinding binding : fModelPatternBindings) {
						String regex = binding.getPattern().pattern();
						regex = regex.substring(0, regex.length() - DEBUG_MODEL_ACTIVITY_SUFFIX.length());
						if (Pattern.matches(regex, id)) {
							ids.add(binding.getActivityId());
						}
					}
				}
				if (!ids.isEmpty()) {
					if (activities == null) {
						activities = new HashSet<>();
					}
					activities.addAll(ids);
				}
				fModelsEnabledForActivities.add(id);
			}
		}
		if (activities != null) {
			IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
			Set<String> enabledActivityIds = activitySupport.getActivityManager().getEnabledActivityIds();
			if (!enabledActivityIds.containsAll(activities)) {
				enabledActivityIds = new HashSet<>(enabledActivityIds);
				enabledActivityIds.addAll(activities);
				activitySupport.setEnabledActivityIds(activities);
			}
		}
	}

	@Override
	public void activityManagerChanged(ActivityManagerEvent activityManagerEvent) {
		if (activityManagerEvent.haveEnabledActivityIdsChanged()) {
			fModelsEnabledForActivities.clear();
		}
	}

}
