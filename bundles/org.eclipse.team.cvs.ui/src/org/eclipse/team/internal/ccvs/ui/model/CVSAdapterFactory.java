/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.model;

 
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.mappings.ChangeSetCompareAdapter;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.mapping.ITeamStateProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

public class CVSAdapterFactory implements IAdapterFactory {
	private static Object fileAdapter = new RemoteFileElement();
	private static Object folderAdapter = new RemoteFolderElement();
	private static Object rootAdapter = new CVSRepositoryRootElement();

	private static Object historyParticipant = new CVSHistoryPageSource();
	
	private static Object teamStateProvider;
	
	// Property cache
	private Object cachedPropertyObject = null;
	private Object cachedPropertyValue = null;
	private ChangeSetCompareAdapter compareAdapter;

	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (IWorkbenchAdapter.class == adapterType) {
			return adapterType.cast(getWorkbenchAdapter(adaptableObject));
		}
		
		if(IDeferredWorkbenchAdapter.class == adapterType) {
			Object o = getWorkbenchAdapter(adaptableObject);
			if(o != null && o instanceof IDeferredWorkbenchAdapter) {
				return adapterType.cast(o);
			}
			return null;
		}		
		
		if (IPropertySource.class == adapterType) {
			return adapterType.cast(getPropertySource(adaptableObject));
		}
		
		if (IHistoryPageSource.class == adapterType){
			return adapterType.cast(historyParticipant);
		}
		
		if (ITeamStateProvider.class == adapterType) {
			synchronized (this) {
				if (teamStateProvider == null)
					teamStateProvider = new CVSTeamStateProvider(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
			}
			return adapterType.cast(teamStateProvider);
		}
		
		if (ISynchronizationCompareAdapter.class == adapterType) {
			if (compareAdapter == null)
				compareAdapter = new ChangeSetCompareAdapter();
			return adapterType.cast(compareAdapter);
		}
		
		return null;
	}
	
	protected Object getWorkbenchAdapter(Object o) {
		if (o instanceof ICVSRemoteFile) {
			return fileAdapter;
		} else if (o instanceof ICVSRepositoryLocation) {
			return rootAdapter;
		}  else if (o instanceof RepositoryRoot) {
			return rootAdapter;
		} else if (o instanceof ICVSRemoteFolder) {
			return folderAdapter;
		}
		return null;
	}
	
	public Class<?>[] getAdapterList() {
		return new Class[] { IWorkbenchAdapter.class, IPropertySource.class,
				IDeferredWorkbenchAdapter.class, IHistoryPageSource.class,
				ISynchronizationCompareAdapter.class, ITeamStateProvider.class,
				IFileRevision.class};
	}
	
	/**
	 * Returns the property source for the given object.  Caches
	 * the result because the property sheet is extremely inefficient,
	 * it asks for the source seven times in a row.
	 */
	public Object getPropertySource(Object adaptableObject) {
		if (adaptableObject == cachedPropertyObject) {
			return cachedPropertyValue;
		}
		cachedPropertyObject = adaptableObject;
		if (adaptableObject instanceof ICVSRemoteFile) {
			cachedPropertyValue = new CVSRemoteFilePropertySource((ICVSRemoteFile)adaptableObject);
		} else if (adaptableObject instanceof ICVSRemoteFolder) {
			cachedPropertyValue = new CVSRemoteFolderPropertySource((ICVSRemoteFolder)adaptableObject);
		} else if (adaptableObject instanceof ICVSRepositoryLocation) {
			cachedPropertyValue = new CVSRepositoryLocationPropertySource((ICVSRepositoryLocation)adaptableObject);
		}  else if (adaptableObject instanceof RepositoryRoot) {
			cachedPropertyValue = new CVSRepositoryLocationPropertySource(((RepositoryRoot)adaptableObject).getRoot());
		} else {
			cachedPropertyValue = null;
		}
		return cachedPropertyValue;
	}
}
