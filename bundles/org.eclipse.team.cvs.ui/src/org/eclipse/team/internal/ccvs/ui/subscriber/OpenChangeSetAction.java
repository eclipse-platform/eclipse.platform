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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.*;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteCompareOperation.CompareTreeBuilder;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.eclipse.team.internal.ui.synchronize.ChangeSetDiffNode;
import org.eclipse.team.ui.synchronize.*;

class OpenChangeSetAction extends SynchronizeModelAction {

	protected OpenChangeSetAction(ISynchronizePageConfiguration configuration) {
		super(CVSUIMessages.OpenCommitSetAction_20, configuration); 
	}
	
	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new AndSyncInfoFilter(new FastSyncInfoFilter[] {
				new FastSyncInfoFilter() {
					@Override
					public boolean select(SyncInfo info) {
						return info.getLocal().getType() == IResource.FILE;
					}
				},
				new OrSyncInfoFilter(new FastSyncInfoFilter[] {
					new SyncInfoDirectionFilter(new int[] { SyncInfo.INCOMING, SyncInfo.CONFLICTING }),
					new FastSyncInfoFilter() {
						@Override
						public boolean select(SyncInfo info) {
							return !info.getComparator().isThreeWay();
						}
					}
				})
		});
	}
	
	private ChangeSet getChangeSet(IStructuredSelection selection) {
		// First, check to see if a change set is selected directly
		if (selection.size() == 1) {
			Object o = selection.getFirstElement();
			if (o instanceof IAdaptable) {
				ChangeSet set = ((IAdaptable)o).getAdapter(ChangeSet.class);
				if (set != null)
					return set;
			}
		}
		// Failing that, check to see if all the selected elements and their childen are in the same change set
		return getChangeSet(selection.toArray());
	}
	
	private ChangeSet getChangeSet(Object[] elements) {
		ChangeSet foundSet = null;
		for (Object object : elements) {
			ChangeSet set = getChangeSet((ISynchronizeModelElement)object);
			if (set == null) return null;
			if (foundSet == null) {
				foundSet = set;
			} else if (foundSet != set) {
				return null;
			}
		}
		return foundSet;
	}
	
	private ChangeSet getChangeSet(ISynchronizeModelElement element) {
		if (element == null) return null;
		if (element instanceof IAdaptable) {
			ChangeSet set = ((IAdaptable)element).getAdapter(ChangeSet.class);
			if (set != null)
				return set;
		}
		return getChangeSet((ISynchronizeModelElement)element.getParent());
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		boolean enabled = super.updateSelection(selection);
		if (enabled) {
			// The selection only contains appropriate files so
			// only enable if the selection is contained within a single change set
			ChangeSet set = getChangeSet(selection);
			return set != null;
		}
		return false;
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new SynchronizeModelOperation(configuration, elements) {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				SyncInfoSet set = getSyncInfoSet();
				SyncInfo[] infos = set.getSyncInfos();
				if (infos.length > 0) {
					ICVSRepositoryLocation location = getLocation(infos[0]);
					if (location == null) {
						handle(new CVSException(CVSUIMessages.OpenCommitSetAction_21)); 
						return;
					}
					CompareTreeBuilder builder = new CompareTreeBuilder(location, null, null);
					if (buildTrees(builder, infos)) {
						try {
							builder.cacheContents(monitor);
							builder.openCompareEditor(getConfiguration().getSite().getPart().getSite().getPage(), getCompareTitle(), getCompareToolTip());
						} catch (CVSException e) {
							handle(e);
							return;
						}
					}
				}
			}

			private String getCompareToolTip() {
				IDiffElement[] elements = getSelectedDiffElements();
				for (IDiffElement element : elements) {
					while (element != null) {
						if (element instanceof ChangeSetDiffNode) {
							return ((ChangeSetDiffNode)element).getName();
						}
						element = element.getParent();
					}
				}
				return null;
			}
			
			private String getCompareTitle() {
				IDiffElement[] elements = getSelectedDiffElements();
				ChangeSet set = getChangeSet(elements);
				if (set instanceof CheckedInChangeSet) {
					CheckedInChangeSet cics = (CheckedInChangeSet)set;
					String date = DateFormat.getDateTimeInstance().format(cics.getDate());
					return NLS.bind(CVSUIMessages.OpenChangeSetAction_0, new String[] {cics.getAuthor(), date});
				}
				return CVSUIMessages.OpenChangeSetAction_1;
			}

			private ICVSRepositoryLocation getLocation(SyncInfo info) {
				IResourceVariant remote = info.getRemote();
				if (remote == null) {
					remote = info.getBase();
				}
				if (remote != null) {
					return ((ICVSRemoteResource)remote).getRepository();
				}
				return null;
			}

			/*
			 * Build the trees that will be compared
			 */
			private boolean buildTrees(CompareTreeBuilder builder, SyncInfo[] infos) {
				for (SyncInfo info : infos) {
					IResourceVariant remote = info.getRemote();
					if (remote == null) {
						IResourceVariant predecessor = info.getBase();
						if (predecessor instanceof ICVSRemoteFile) {
							builder.addToTrees((ICVSRemoteFile)predecessor, null);
						}
					} else if (remote instanceof ICVSRemoteFile) {
						try {
							ICVSRemoteFile predecessor = getImmediatePredecessor(remote);
							builder.addToTrees(predecessor, (ICVSRemoteFile)remote);
						} catch (TeamException e) {
							handle(e);
							return false;
						}
					}
				}
				return true;
			}
		};
	}

	private ICVSRemoteFile getImmediatePredecessor(IResourceVariant remote) throws TeamException {
		CVSChangeSetCollector changeSetCollector = getChangeSetCollector();
		if (changeSetCollector != null) {
			return changeSetCollector.getImmediatePredecessor((ICVSRemoteFile)remote);
		}
		return null;
	}

	private CVSChangeSetCollector getChangeSetCollector() {
		return (CVSChangeSetCollector)getConfiguration().getProperty(CVSChangeSetCollector.CVS_CHECKED_IN_COLLECTOR);
	}

}
