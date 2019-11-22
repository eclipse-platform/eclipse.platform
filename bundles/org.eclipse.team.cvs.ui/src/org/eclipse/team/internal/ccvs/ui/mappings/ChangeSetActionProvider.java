/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
 * Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 94808 [Change Sets] "&" not showing up in dropdown menu
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.mapping.ChangeSetModelProvider;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.mapping.ResourceModelActionProvider;
import org.eclipse.team.internal.ui.mapping.ResourceModelTraversalCalculator;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;

public class ChangeSetActionProvider extends ResourceModelActionProvider {

	/**
	 * Menu group that can be added to the context menu
	 */
	public final static String CHANGE_SET_GROUP = "changeSetActions"; //$NON-NLS-1$

	// Constants for persisting sorting options
	private static final String P_LAST_COMMENTSORT = TeamUIPlugin.ID + ".P_LAST_COMMENT_SORT"; //$NON-NLS-1$

	private MenuManager sortByComment;
	private MenuManager addToChangeSet;
	private CreateChangeSetAction createChangeSet;
	private EditChangeSetAction editChangeSet;
	private RemoveChangeSetAction removeChangeSet;
	private MakeDefaultChangeSetAction makeDefault;
	private OpenChangeSetAction openCommitSet;

	private class CreateChangeSetAction extends ModelParticipantAction {

		public CreateChangeSetAction(ISynchronizePageConfiguration configuration) {
			super(TeamUIMessages.ChangeLogModelProvider_0, configuration);
		}

		@Override
		public void run() {
			final IDiff[] diffs = getLocalChanges(getStructuredSelection());
			syncExec(() -> createChangeSet(diffs));
		}

		/* package */void createChangeSet(IDiff[] diffs) {
			ActiveChangeSet set =  getChangeSetCapability().createChangeSet(getConfiguration(), diffs);
			if (set != null) {
				getActiveChangeSetManager().add(set);
			}
		}

		@Override
		protected boolean isEnabledForSelection(IStructuredSelection selection) {
			return isContentProviderEnabled()
					&& containsOnlyLocalChanges(selection);
		}
	}

	/**
	 * Escape a string so it can be used as an action text without '&amp;'
	 * being interpreted as a mnemonic. Specifically, turn each '&amp;' into '&amp;&amp;'.
	 */
	/* package */static String escapeActionText(String x) {
		// Loosely based on org.eclipse.jface.action.LegacyActionTools#removeMnemonics
		int ampersandIndex = x.indexOf('&');
		if (ampersandIndex == -1)
			return x;

		int len = x.length();
		StringBuilder sb = new StringBuilder(2 * len + 1);
		int doneIndex = 0;
		while (ampersandIndex != -1) {
			sb.append(x.substring(doneIndex, ampersandIndex));
			sb.append("&&"); //$NON-NLS-1$
			doneIndex = ampersandIndex + 1;
			ampersandIndex = x.indexOf('&', doneIndex);
		}
		if (doneIndex < len)
			sb.append(x.substring(doneIndex, len));
		return sb.toString();
	}

	private class AddToChangeSetAction extends ModelParticipantAction {

		private final ActiveChangeSet set;

		public AddToChangeSetAction(ISynchronizePageConfiguration configuration, ActiveChangeSet set, ISelection selection) {
			super(set == null ? TeamUIMessages.ChangeSetActionGroup_2 : escapeActionText(set.getTitle()), configuration); 
			this.set = set;
			selectionChanged(selection);
		}

		@Override
		public void run() {
			IDiff[] diffArray = getLocalChanges(getStructuredSelection());
			if (set != null) {
				set.add(diffArray);
			} else {
				ChangeSet[] sets = getActiveChangeSetManager().getSets();
				IResource[] resources = getResources(diffArray);
				for (ChangeSet s : sets) {
					ActiveChangeSet activeSet = (ActiveChangeSet) s;
					activeSet.remove(resources);
				}
			}
		}

		@Override
		protected boolean isEnabledForSelection(IStructuredSelection selection) {
			return isContentProviderEnabled()
					&& containsOnlyLocalChanges(selection);
		}
	}

	private abstract class ChangeSetAction extends BaseSelectionListenerAction {

		public ChangeSetAction(String title, ISynchronizePageConfiguration configuration) {
			super(title);
		}

		@Override
		protected boolean updateSelection(IStructuredSelection selection) {
			return getSelectedSet() != null;
		}

		protected ActiveChangeSet getSelectedSet() {
			IStructuredSelection selection = getStructuredSelection();
			if (selection.size() == 1) {
				Object first = selection.getFirstElement();
				if (first instanceof ActiveChangeSet) {
					ActiveChangeSet activeChangeSet = (ActiveChangeSet) first;
					if (activeChangeSet.isUserCreated())
						return activeChangeSet;
				}
			}
			return null;
		}
	}

	private class EditChangeSetAction extends ChangeSetAction {

		public EditChangeSetAction(ISynchronizePageConfiguration configuration) {
			super(TeamUIMessages.ChangeLogModelProvider_6, configuration);
		}

		@Override
		public void run() {
			ActiveChangeSet set = getSelectedSet();
			if (set == null) return;
			getChangeSetCapability().editChangeSet(internalGetSynchronizePageConfiguration(), set);
		}
	}

	private class RemoveChangeSetAction extends ModelParticipantAction {

		public RemoveChangeSetAction(ISynchronizePageConfiguration configuration) {
			super(TeamUIMessages.ChangeLogModelProvider_7, configuration);
		}

		@Override
		public void run() {
			IDiff[] diffArray = getLocalChanges(getStructuredSelection());
			ChangeSet[] sets = getActiveChangeSetManager().getSets();
			IResource[] resources = getResources(diffArray);
			for (ChangeSet set : sets) {
				ActiveChangeSet activeSet = (ActiveChangeSet) set;
				activeSet.remove(resources);
			}
		}

		@Override
		protected boolean isEnabledForSelection(IStructuredSelection selection) {
			return isContentProviderEnabled()
					&& containsOnlyLocalChanges(selection);
		}
	}

	private class MakeDefaultChangeSetAction extends ChangeSetAction {
		public MakeDefaultChangeSetAction(
				ISynchronizePageConfiguration configuration) {
			super(TeamUIMessages.ChangeLogModelProvider_9, configuration);
		}

		@Override
		protected boolean updateSelection(IStructuredSelection selection) {
			if (getSelectedSet() != null) {
				setText(TeamUIMessages.ChangeLogModelProvider_9);
				setChecked(getSelectedSet().equals(
						getActiveChangeSetManager().getDefaultSet()));
			} else {
				setText(TeamUIMessages.ChangeLogModelProvider_10);
				setChecked(false);
			}
			return true;
		}

		@Override
		public void run() {
			getActiveChangeSetManager().makeDefault(
					isChecked() ? getSelectedSet() : null);
			if (getSelectedSet() == null) {
				setChecked(false); // keep unchecked
			}
		}
	}

	/* *****************************************************************************
	 * Action that allows changing the model providers sort order.
	 */
	private class ToggleSortOrderAction extends Action {
		private int criteria;

		protected ToggleSortOrderAction(String name, int criteria) {
			super(name, IAction.AS_RADIO_BUTTON);
			this.criteria = criteria;
			update();
		}

		@Override
		public void run() {
			int sortCriteria = getSortCriteria(internalGetSynchronizePageConfiguration());
			if (isChecked() && sortCriteria != criteria) {
				setSortCriteria(internalGetSynchronizePageConfiguration(), criteria);
				update();
				((SynchronizePageConfiguration)internalGetSynchronizePageConfiguration()).getPage().getViewer().refresh();
			}
		}

		public void update() {
			setChecked(criteria == getSortCriteria(internalGetSynchronizePageConfiguration()));
		}
	}

	public static int getSortCriteria(ISynchronizePageConfiguration configuration) {
		int sortCriteria = ChangeSetSorter.DATE;
		if (configuration != null) {
			Object o = configuration.getProperty(P_LAST_COMMENTSORT);
			if (o instanceof Integer) {
				Integer wrapper = (Integer) o;
				sortCriteria = wrapper.intValue();
			} else {
				try {
					IDialogSettings pageSettings = configuration.getSite().getPageSettings();
					if (pageSettings != null) {
						sortCriteria = pageSettings.getInt(P_LAST_COMMENTSORT);
					}
				} catch (NumberFormatException e) {
					// ignore and use the defaults.
				}
			}
		}
		switch (sortCriteria) {
		case ChangeSetSorter.COMMENT:
		case ChangeSetSorter.DATE:
		case ChangeSetSorter.USER:
			break;
		default:
			sortCriteria = ChangeSetSorter.DATE;
			break;
		}
		return sortCriteria;
	}

	public static void setSortCriteria(ISynchronizePageConfiguration configuration, int criteria) {
		configuration.setProperty(P_LAST_COMMENTSORT, Integer.valueOf(criteria));
		IDialogSettings pageSettings = configuration.getSite().getPageSettings();
		if (pageSettings != null) {
			pageSettings.put(P_LAST_COMMENTSORT, criteria);
		}
	}

	public ChangeSetActionProvider() {
		super();
	}

	@Override
	protected void initialize() {
		super.initialize();
		if (getChangeSetCapability().supportsCheckedInChangeSets()) {
			sortByComment = new MenuManager(TeamUIMessages.ChangeLogModelProvider_0a);	 
			sortByComment.add(new ToggleSortOrderAction(TeamUIMessages.ChangeLogModelProvider_1a, ChangeSetSorter.COMMENT)); 
			sortByComment.add(new ToggleSortOrderAction(TeamUIMessages.ChangeLogModelProvider_2a, ChangeSetSorter.DATE)); 
			sortByComment.add(new ToggleSortOrderAction(TeamUIMessages.ChangeLogModelProvider_3a, ChangeSetSorter.USER));
			openCommitSet = new OpenChangeSetAction(getSynchronizePageConfiguration());
		}
		if (getChangeSetCapability().supportsActiveChangeSets()) {
			createChangeSet = new CreateChangeSetAction(
					getSynchronizePageConfiguration());
			editChangeSet = new EditChangeSetAction(
					getSynchronizePageConfiguration());
			makeDefault = new MakeDefaultChangeSetAction(
					getSynchronizePageConfiguration());
			removeChangeSet = new RemoveChangeSetAction(
					getSynchronizePageConfiguration());
		}
	}

	protected ActiveChangeSet getSelectedActiveChangeSet(
			IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if (first instanceof ActiveChangeSet) {
				ActiveChangeSet activeChangeSet = (ActiveChangeSet) first;
				if (activeChangeSet.isUserCreated())
					return activeChangeSet;
			}
		}
		return null;
	}

	private IResource[] getResources(IDiff[] diffArray) {
		List<IResource> result = new ArrayList<>();
		for (IDiff diff : diffArray) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null) {
				result.add(resource);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (isContentProviderEnabled()) {
			super.fillContextMenu(menu);
			if (getChangeSetCapability().enableCheckedInChangeSetsFor(
					getSynchronizePageConfiguration())) {
				appendToGroup(menu, "file-bottom", //$NON-NLS-1$
						openCommitSet);
				appendToGroup(menu, ISynchronizePageConfiguration.SORT_GROUP,
						sortByComment);
			}
			IStructuredSelection selection = (IStructuredSelection) getContext()
					.getSelection();
			if (getChangeSetCapability().enableActiveChangeSetsFor(
					getSynchronizePageConfiguration())
					&& containsOnlyLocalChanges(selection)) {

				if (containsOnlyUnassignedChanges(selection)) {
					// only local unassigned changes
					addToChangeSet = new MenuManager(
							TeamUIMessages.ChangeLogModelProvider_13);
					appendToGroup(menu, CHANGE_SET_GROUP, addToChangeSet);
				} else {
					addToChangeSet = new MenuManager(
							TeamUIMessages.ChangeLogModelProvider_12);
					appendToGroup(menu, CHANGE_SET_GROUP, addToChangeSet);
					appendToGroup(menu, CHANGE_SET_GROUP, removeChangeSet);
				}

				addChangeSets(addToChangeSet);

				if (getSelectedActiveChangeSet(selection) != null) {
					appendToGroup(menu, CHANGE_SET_GROUP, editChangeSet);
				}
				appendToGroup(menu, CHANGE_SET_GROUP, makeDefault);
			}
		}
	}
	
	protected void addChangeSets(IMenuManager manager) {
		ChangeSet[] sets = getActiveChangeSetManager().getSets();
		Arrays.sort(sets, new ChangeSetComparator());
		ISelection selection = getContext().getSelection();
		createChangeSet.selectionChanged(selection);
		manager.add(createChangeSet);
		manager.add(new Separator());
		for (ChangeSet s : sets) {
			ActiveChangeSet set = (ActiveChangeSet) s;
			AddToChangeSetAction action = new AddToChangeSetAction(
					getSynchronizePageConfiguration(), set, selection);
			manager.add(action);
		}
		manager.add(new Separator());
	}

	@Override
	public void dispose() {
		if (addToChangeSet != null) {
			addToChangeSet.dispose();
			addToChangeSet.removeAll();
		}
		if (sortByComment != null) {
			sortByComment.dispose();
			sortByComment.removeAll();
		}
		super.dispose();
	}

	private boolean appendToGroup(IContributionManager manager, String groupId, IContributionItem item) {
		if (manager == null || item == null) return false;
		IContributionItem group = manager.find(groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), item);
			return true;
		}
		return false;
	}

	private boolean appendToGroup(IContributionManager manager, String groupId, IAction action) {
		if (manager == null || action == null) return false;
		IContributionItem group = manager.find(groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), action);
			// registerActionWithWorkbench(action);
			return true;
		}
		return false;
	}

	public ChangeSetCapability getChangeSetCapability() {
		ISynchronizeParticipant participant = getSynchronizePageConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider provider = (IChangeSetProvider) participant;
			return provider.getChangeSetCapability();
		}
		return null;
	}

	/* package */void syncExec(final Runnable runnable) {
		final Control ctrl = getSynchronizePageConfiguration().getPage().getViewer().getControl();
		Utils.syncExec(runnable, ctrl);
	}

	/* package */ActiveChangeSetManager getActiveChangeSetManager() {
		return CVSUIPlugin.getPlugin().getChangeSetManager();
	}

	public IDiff[] getLocalChanges(IStructuredSelection selection) {
		if (selection instanceof ITreeSelection) {
			ITreeSelection ts = (ITreeSelection) selection;
			TreePath[] paths = ts.getPaths();
			List<IDiff> result = new ArrayList<>();
			for (TreePath path : paths) {
				IDiff[] diffs = getLocalChanges(path);
				Collections.addAll(result, diffs);
			}
			return result.toArray(new IDiff[result.size()]);
		}
		return new IDiff[0];
	}

	private IDiff[] getLocalChanges(TreePath path) {
		IResourceDiffTree tree = getDiffTree(path);
		if (path.getSegmentCount() == 1 && path.getLastSegment() instanceof IDiffTree) {
			return ((ResourceDiffTree) tree).getDiffs();
		}
		ResourceTraversal[] traversals = getTraversals(path.getLastSegment());
		return tree.getDiffs(traversals);
	}

	private IResourceDiffTree getDiffTree(TreePath path) {
		return getContentProvider().getDiffTree(path);
	}
	
	
	private boolean containsOnlyUnassignedChanges(IStructuredSelection selection) {
		IDiff[] diffArray = getLocalChanges(selection);
		ChangeSet[] activeChangeSets = getActiveChangeSetManager().getSets();
		IResource[] resources = getResources(diffArray);
		for (ChangeSet activeChangeSet : activeChangeSets) {
			for (IResource resource : resources) {
				if (activeChangeSet.contains(resource)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean containsOnlyLocalChanges(IStructuredSelection selection) {
		if (selection instanceof ITreeSelection) {
			ITreeSelection ts = (ITreeSelection) selection;
			TreePath[] paths = ts.getPaths();
			for (TreePath path : paths) {
				if (!containsOnlyLocalChanges(path)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean containsOnlyLocalChanges(TreePath path) {
		IResourceDiffTree tree = getDiffTree(path);
		ResourceTraversal[] traversals = getTraversals(path.getLastSegment());
		return !tree.hasMatchingDiffs(traversals, getNonLocalChangesFilter());
	}

	private ResourceTraversal[] getTraversals(Object element) {
		if (element instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) element;
			return new ResourceTraversal[] { new ResourceTraversal(set.getResources(), IResource.DEPTH_ZERO, IResource.NONE) };
		}
		if (element instanceof IProject) {
			IProject project = (IProject) element;
			return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { project }, IResource.DEPTH_INFINITE, IResource.NONE) };
		}
		if (element instanceof IFile) {
			IFile file = (IFile) element;
			return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { file }, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		if (element instanceof IFolder) {
			IFolder folder = (IFolder) element;
			if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT)) {
				return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { folder }, IResource.DEPTH_ONE, IResource.NONE) };
			} else if (getLayout().equals(IPreferenceIds.TREE_LAYOUT)) {
				return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { folder }, IResource.DEPTH_INFINITE, IResource.NONE) };
			} else if (getLayout().equals(IPreferenceIds.FLAT_LAYOUT)) {
				return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { folder }, IResource.DEPTH_ZERO, IResource.NONE) };
			}
		}
		return new ResourceTraversal[0];
	}
	
	private FastDiffFilter getNonLocalChangesFilter() {
		return new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				if (diff instanceof IThreeWayDiff && isVisible(diff)) {
					IThreeWayDiff twd = (IThreeWayDiff) diff;
					if (twd.getDirection() == IThreeWayDiff.OUTGOING
							|| twd.getDirection() == IThreeWayDiff.CONFLICTING) {
						return false;
					}
				}
				return true;
			}
		};
	}

	/* package */boolean isVisible(IDiff diff) {
		return ((SynchronizePageConfiguration)getSynchronizePageConfiguration()).isVisible(diff);
	}

	protected ResourceModelTraversalCalculator getTraversalCalculator() {
		return ResourceModelTraversalCalculator.getTraversalCalculator(getSynchronizePageConfiguration());
	}

	private ChangeSetContentProvider getContentProvider() {
		INavigatorContentExtension extension = getExtension();
		if (extension != null) {
			ITreeContentProvider provider = extension.getContentProvider();
			if (provider instanceof ChangeSetContentProvider) {
				return (ChangeSetContentProvider) provider;
			}
		}
		return null;
	}

	private INavigatorContentExtension getExtension() {
		INavigatorContentService service = getActionSite().getContentService();
		Set set = service.findContentExtensionsByTriggerPoint(getModelProvider());
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			INavigatorContentExtension extension = (INavigatorContentExtension) iter.next();
			return extension;
		}
		return null;
	}

	private Object getModelProvider() {
		return ChangeSetModelProvider.getProvider();
	}

	private String getLayout() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT);
	}

	@Override
	public void setContext(ActionContext context) {
		super.setContext(context);
		if (context != null) {
			if (editChangeSet != null)
				editChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
			if (removeChangeSet != null)
				removeChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
			if (makeDefault != null)
				makeDefault.selectionChanged((IStructuredSelection)getContext().getSelection());
		}
	}

	protected boolean isContentProviderEnabled() {
		ChangeSetContentProvider provider = getContentProvider();
		if (provider != null) {
			return provider.isEnabled();
		}
		return false;
	}

	/* package */ISynchronizePageConfiguration internalGetSynchronizePageConfiguration() {
		return getSynchronizePageConfiguration();
	}
}
