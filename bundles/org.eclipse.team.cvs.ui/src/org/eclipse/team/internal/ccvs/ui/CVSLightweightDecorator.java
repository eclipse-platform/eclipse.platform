/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;


import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.core.util.ResourceStateChangeListeners;
import org.eclipse.team.internal.core.ExceptionCollector;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.SynchronizationStateTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.osgi.framework.Bundle;

public class CVSLightweightDecorator extends LabelProvider implements ILightweightLabelDecorator, IResourceStateChangeListener, IPropertyChangeListener {

	// Decorator id as defined in the decorator extension point
	public final static String ID = "org.eclipse.team.cvs.ui.decorator"; //$NON-NLS-1$

	private static ExceptionCollector exceptions = new ExceptionCollector(CVSUIMessages.CVSDecorator_exceptionMessage, CVSUIPlugin.ID, IStatus.ERROR, CVSUIPlugin.getPlugin().getLog()); //;
	
	private static String DECORATOR_FORMAT = "yyyy/MM/dd HH:mm:ss"; //$NON-NLS-1$
	private static SimpleDateFormat decorateFormatter = new SimpleDateFormat(DECORATOR_FORMAT, Locale.getDefault());
	
	private static String[] fonts = new String[]  {
			CVSDecoratorConfiguration.IGNORED_FONT,
			CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT};
	
	private static String[] colors = new String[] {
			 CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR,
			 CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR,
			 CVSDecoratorConfiguration.IGNORED_BACKGROUND_COLOR,
			 CVSDecoratorConfiguration.IGNORED_FOREGROUND_COLOR};
	
	private static final SynchronizationStateTester DEFAULT_TESTER = new SynchronizationStateTester();

	public CVSLightweightDecorator() {
		ResourceStateChangeListeners.getListener().addResourceStateChangeListener(this);
		TeamUI.addPropertyChangeListener(this);
		CVSUIPlugin.addPropertyChangeListener(this);
		
		// This is an optimization to ensure that while decorating our fonts and colors are
		// pre-created and decoration can occur without having to syncExec.
		ensureFontAndColorsCreated(fonts, colors);
		
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().addPropertyChangeListener(this);
		CVSProviderPlugin.broadcastDecoratorEnablementChanged(true /* enabled */);
	}
	
	/**
	 * This method will ensure that the fonts and colors used by the decorator
	 * are cached in the registries. This avoids having to syncExec when
	 * decorating since we ensure that the fonts and colors are pre-created.
	 * 
	 * @param fonts fonts ids to cache
	 * @param colors color ids to cache
	 */
	private void ensureFontAndColorsCreated(final String[] fonts, final String[] colors) {
		CVSUIPlugin.getStandardDisplay().syncExec(() -> {
			ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			for (String color : colors) {
				theme.getColorRegistry().get(color);
			}
			for (String font : fonts) {
				theme.getFontRegistry().get(font);
			}
		});
	}

	public static boolean isDirty(final ICVSResource resource) throws CVSException {
		return getSubscriber().isDirty(resource, null);
	}

	public static boolean isDirty(IResource resource) {
		try {
			return getSubscriber().isDirty(resource, null);
		} catch (CVSException e) {
			handleException(resource, e);
			return true;
		}
	}
	
	/*
	 * Answers null if a provider does not exist or the provider is not a CVS provider. These resources
	 * will be ignored by the decorator.
	 */
	private static CVSTeamProvider getCVSProviderFor(IResource resource) {
		if (resource == null) return null;
		RepositoryProvider p =
			RepositoryProvider.getProvider(
				resource.getProject(),
				CVSProviderPlugin.getTypeId());
		if (p == null) {
			return null;
		}
		return (CVSTeamProvider) p;
	}
	
	/**
	 * This method should only be called by the decorator thread.
	 * 
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
	public void decorate(Object element, IDecoration decoration) {
		
		// Don't decorate the workspace root
		IResource resource = getResource(element);
		if (resource != null && resource.getType() == IResource.ROOT)
			return;
		
		// Get the mapping for the object and ensure it overlaps with CVS projects
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping == null)
			return;
		if (!isMappedToCVS(mapping))
			return;	
		
		// Get the sync state tester from the context
		IDecorationContext context = decoration.getDecorationContext();
		SynchronizationStateTester tester = DEFAULT_TESTER;
		Object property = context.getProperty(SynchronizationStateTester.PROP_TESTER);
		if (property instanceof SynchronizationStateTester) {
			tester = (SynchronizationStateTester) property;
		}
		
		// Calculate and apply the decoration
		try {
			if (tester.isDecorationEnabled(element)) {
				CVSDecoration cvsDecoration = decorate(element, tester);
				cvsDecoration.apply(decoration);
			}
		} catch(CoreException e) {
			handleException(element, e);
		} catch (IllegalStateException e) {
			// This is thrown by Core if the workspace is in an illegal state
			// If we are not active, ignore it. Otherwise, propagate it.
			// (see bug 78303)
			if (Platform.getBundle(CVSUIPlugin.ID).getState() == Bundle.ACTIVE) {
				throw e;
			}
		}
	}

	private static IResource getResource(Object element) {
		if (element instanceof ResourceMapping) {
			element = ((ResourceMapping) element).getModelObject();
		}
		return Utils.getResource(element);
	}

	/*
	 * Return whether any of the projects of the mapping are mapped to CVS
	 */
	private boolean isMappedToCVS(ResourceMapping mapping) {
		IProject[] projects = mapping.getProjects();
		boolean foundOne = false;
		for (IProject project : projects) {
			if (project != null) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project);
				if (provider instanceof CVSTeamProvider) {
					foundOne = true;
				} else if (provider != null) {
					return false;
				}
			}
		}
		return foundOne;
	}
	
	public static CVSDecoration decorate(Object element, SynchronizationStateTester tester) throws CoreException {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		CVSDecoration result = new CVSDecoration();
		
		// First, decorate the synchronization state
		int state = IDiff.NO_CHANGE;
		if (isSupervised(element)) {
			// TODO: Not quite right
			result.setHasRemote(true);
			state = tester.getState(element, 
					store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY) 
						? IDiff.ADD | IDiff.REMOVE | IDiff.CHANGE | IThreeWayDiff.OUTGOING 
						: 0, 
					new NullProgressMonitor());
			result.setStateFlags(state);
		} else {
			result.setIgnored(true);
		}
		// Tag
		if (!result.isIgnored()) {
			CVSTag tag = getTagToShow(element);
			if (tag != null) {
				String name = tag.getName();
				if (tag.getType() == CVSTag.DATE) {
					Date date = tag.asDate();
					if (date != null) {
						name = decorateFormatter.format(date);
					}
				}
				result.setTag(name);
			}
		}
		
		// If the element adapts to a single resource, add additional decorations
		IResource resource = getResource(element);
		if (resource == null) {
			result.setResourceType(CVSDecoration.MODEL);
		} else {
			decorate(resource, result);
		}
		tester.elementDecorated(element, result.asTeamStateDescription(null));
		return result;
	}
	
	private static boolean isSupervised(Object element) throws CoreException {
		IResource[] resources = getTraversalRoots(element);
		for (IResource resource : resources) {
			if (getSubscriber().isSupervised(resource))
				return true;
		}
		return false;
	}
	
	private static IResource[] getTraversalRoots(Object element) throws CoreException {
		Set result = new HashSet();
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			ResourceTraversal[] traversals = mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
			for (ResourceTraversal traversal : traversals) {
				IResource[] resources = traversal.getResources();
				Collections.addAll(result, resources);
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	private static void decorate(IResource resource, CVSDecoration cvsDecoration) throws CVSException {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		cvsDecoration.setResourceType(resource.getType());
		
		cvsDecoration.setHasRemote(hasRemote(cvsResource));
		if (cvsResource.isIgnored()) {
			cvsDecoration.setIgnored(true);
		}
		if (!cvsDecoration.isIgnored()) {
			// Dirty: Only decorate dirty state if we're not set to decorate models
			boolean decorateModel = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);
			if (!decorateModel) {
				// Dirty
				try {
					IDiff node = getSubscriber().getDiff(resource);
					if (node != null) {
						if (node instanceof IThreeWayDiff) {
							IThreeWayDiff twd = (IThreeWayDiff) node;
							cvsDecoration.setDirty(twd.getDirection() == IThreeWayDiff.OUTGOING 
								|| twd.getDirection() == IThreeWayDiff.CONFLICTING);
						}
					}
				} catch (CoreException e) {
					handleException(resource, e);
				}
				// Has a remote
				//cvsDecoration.setHasRemote(CVSWorkspaceRoot.hasRemote(resource));
			}
			// Is a new resource
			if (store.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION)) {
				if (cvsResource.exists()) {
					if (cvsResource.isFolder()) {
						if (!((ICVSFolder) cvsResource).isCVSFolder()) {
							cvsDecoration.setNewResource(true);
						}
					} else if (!cvsResource.isManaged()) {
						cvsDecoration.setNewResource(true);
					}
				}
			}
			// Extract type specific properties
			if (resource.getType() == IResource.FILE) {
				extractFileProperties((IFile) resource, cvsDecoration);
			} else {
				extractContainerProperties((IContainer) resource, cvsDecoration);
			}
		}
	}

	private static boolean hasRemote(ICVSResource cvsResource) {
		try {
			return (cvsResource.isManaged() || cvsResource.isFolder() && ((ICVSFolder)cvsResource).isCVSFolder());
		} catch (CVSException e) {
			return false;
		}
	}

	public static CVSDecoration decorate(IResource resource, boolean includeDirtyCheck) throws CVSException {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		CVSDecoration cvsDecoration = new CVSDecoration();
		cvsDecoration.setResourceType(resource.getType());
		
		if (cvsResource.isIgnored()) {
			cvsDecoration.setIgnored(true);
		}
		if (!cvsDecoration.isIgnored()) {
			// Dirty
			if (includeDirtyCheck) {
				boolean computeDeepDirtyCheck = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);
				int type = resource.getType();
				if (type == IResource.FILE || computeDeepDirtyCheck) {
					cvsDecoration.setDirty(CVSLightweightDecorator.isDirty(resource));
				}
			}
		}
		decorate(resource, cvsDecoration);
		return cvsDecoration;
	}

	private static void extractContainerProperties(IContainer resource, CVSDecoration cvsDecoration) throws CVSException {
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(resource);
		FolderSyncInfo folderInfo = folder.getFolderSyncInfo();
		if (folderInfo != null) {
			cvsDecoration.setLocation(KnownRepositories.getInstance().getRepository(folderInfo.getRoot()));
			
			// Ignore the relative 'repository' path if it is the same locally 
			if (!resource.getFullPath().toString().substring(1).equals(folderInfo.getRepository()))
				cvsDecoration.setRepository(folderInfo.getRepository());
			cvsDecoration.setVirtualFolder(folderInfo.isVirtualDirectory());
		}
	}

	private static void extractFileProperties(IFile resource, CVSDecoration cvsDecoration) throws CVSException {
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(resource);
		ResourceSyncInfo fileInfo = file.getSyncInfo();
		KSubstOption option = KSubstOption.fromFile(resource);
		if (fileInfo != null) {
			cvsDecoration.setAdded(fileInfo.isAdded());
			cvsDecoration.setRevision(fileInfo.getRevision());
			cvsDecoration.setReadOnly(file.isReadOnly());
			cvsDecoration.setNeedsMerge(fileInfo.isNeedsMerge(file.getTimeStamp()));
			option = fileInfo.getKeywordMode();
		}
		cvsDecoration.setKeywordSubstitution(option.getShortDisplayText());
		CVSTeamProvider provider = getCVSProviderFor(resource);
		if (provider != null)
			cvsDecoration.setWatchEditEnabled(provider.isWatchEditEnabled());	
	}

	protected static CVSTag getTagToShow(Object element) throws CoreException {
		IResource r = getResource(element);
		if (r != null)
			return getTagToShow(r);
		IResource[] resources = getTraversalRoots(element);
		boolean first = true;
		CVSTag tag = null;
		for (IResource resource : resources) {
			if (getSubscriber().isSupervised(resource)) {
				CVSTag nextTag = getTagToShow(resource);
				if (first) {
					tag = nextTag;
					first = false;
				} else if (!equals(tag, nextTag)) {
					return null;
				}
				
			}
		}
		return tag;
	}
	
	private static boolean equals(CVSTag tag, CVSTag nextTag) {
		if (tag == nextTag)
			return true;
		if (tag == null || nextTag == null)
			return false;
		return tag.getName().equals(nextTag.getName());
	}

	/**
	 * Only show the tag if the resources tag is different than the parents. Or else, tag
	 * names will clutter the text decorations.
	 */
	protected static CVSTag getTagToShow(IResource resource) throws CVSException {
		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		CVSTag tag = null;

		// for unmanaged resources don't show a tag since they will be added in
		// the context of their parents tag. For managed resources only show tags
		// if different than parent.
		boolean managed = false;

		if(cvsResource.isFolder()) {
			FolderSyncInfo folderInfo = ((ICVSFolder)cvsResource).getFolderSyncInfo();
			if(folderInfo != null) {
				tag = folderInfo.getTag();
				managed = true;
			}
		} else {
			ResourceSyncInfo info = ((ICVSFile)cvsResource).getSyncInfo();
			if(info != null) {
				tag = info.getTag();
				managed = true;
			}
		}

		ICVSFolder parent = cvsResource.getParent();
		if(parent != null && managed) {
			FolderSyncInfo parentInfo = parent.getFolderSyncInfo();
			if(parentInfo != null) {
				CVSTag parentTag = parentInfo.getTag();
				parentTag = (parentTag == null ? CVSTag.DEFAULT : parentTag);
				tag = (tag == null ? CVSTag.DEFAULT : tag);
				// must compare tags by name because CVS doesn't do a good job of
				// using  T and N prefixes for folders and files.
				if( parentTag.getName().equals(tag.getName())) {
					tag = null;
				}
			}
		}
		return tag;
	}

	/*
	 * Add resource and its parents to the List
	 */
	
	private void addWithParents(IResource resource, Set resources) {
		IResource current = resource;

		while (current.getType() != IResource.ROOT) {
			resources.add(current);
			current = current.getParent();
		}
	}
	
	/*
	* Perform a blanket refresh of all CVS decorations
	*/
	public static void refresh() {
		PlatformUI.getWorkbench().getDecoratorManager().update(CVSUIPlugin.DECORATOR_ID);
	}

	/*
	 * Update the decorators for every resource in project
	 */
	
	public void refresh(IProject project) {
		final List resources = new ArrayList();
		try {
			project.accept(resource -> {
				resources.add(resource);
				return true;
			});
			postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
		} catch (CoreException e) {
			handleException(project, e);
		}
	}
	
	@Override
	public void resourceSyncInfoChanged(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}
	
	@Override
	public void externalSyncInfoChange(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}
	
	@Override
	public void resourceModified(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceStateChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceStateChanged(IResource[] changedResources) {
		// add depth first so that update thread processes parents first.
		//System.out.println(">> State Change Event");
		Set resourcesToUpdate = new HashSet();

		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		boolean showingDeepDirtyIndicators = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);

		for (IResource resource : changedResources) {
			if(showingDeepDirtyIndicators) {
				addWithParents(resource, resourcesToUpdate);
			} else {
				resourcesToUpdate.add(resource);
			}
		}

		postLabelEvent(new LabelProviderChangedEvent(this, resourcesToUpdate.toArray()));
	}
	
	@Override
	public void projectConfigured(IProject project) {
		refresh(project);
	}
	@Override
	public void projectDeconfigured(IProject project) {
		refresh(project);
	}

	/**
	 * Post the label event to the UI thread
	 *
	 * @param events  the events to post
	 */
	private void postLabelEvent(final LabelProviderChangedEvent event) {
		Display.getDefault().asyncExec(() -> fireLabelProviderChanged(event));
	}
	
	@Override
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().removePropertyChangeListener(this);
		CVSProviderPlugin.broadcastDecoratorEnablementChanged(false /* disabled */);
		TeamUI.removePropertyChangeListener(this);
		CVSUIPlugin.removePropertyChangeListener(this);
	}
	
	/**
	 * Handle exceptions that occur in the decorator.
	 * Exceptions are only logged for resources that
	 * are accessible (i.e. exist in an open project).
	 */
	private static void handleException(IResource resource, CoreException e) {
		if (resource == null || resource.isAccessible())
			exceptions.handleException(e);
	}
	
	/**
	 * Handle exceptions that occur in the decorator.
	 * Exceptions are only logged for resources that
	 * are accessible (i.e. exist in an open project).
	 */
	private void handleException(Object element, CoreException e) {
		IResource resource = Utils.getResource(element);
		if (resource != null) {
			handleException(resource, e);
		}
		ResourceMapping mapping = Utils.getResourceMapping(element);
		IProject[] projects = mapping.getProjects();
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				return;
			}
		}
		exceptions.handleException(e);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (isEventOfInterest(event)) {
			ensureFontAndColorsCreated(fonts, colors);
			refresh();
		}	
	}

	private boolean isEventOfInterest(PropertyChangeEvent event) {
		String prop = event.getProperty();
		return prop.equals(TeamUI.GLOBAL_IGNORES_CHANGED) 
			|| prop.equals(TeamUI.GLOBAL_FILE_TYPES_CHANGED) 
			|| prop.equals(CVSUIPlugin.P_DECORATORS_CHANGED)
			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR)
			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR)
			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT)
			|| prop.equals(CVSDecoratorConfiguration.IGNORED_FOREGROUND_COLOR)
			|| prop.equals(CVSDecoratorConfiguration.IGNORED_BACKGROUND_COLOR)
			|| prop.equals(CVSDecoratorConfiguration.IGNORED_FONT);
	}
	
	private static CVSWorkspaceSubscriber getSubscriber() {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
	}
}
