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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.compare.ICompareInputLabelProvider;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;

/**
 * A change notifier for resource-based compare inputs.
 */
public class ResourceCompareInputChangeNotifier extends CompareInputChangeNotifier implements IDiffChangeListener {

	static final String RESOURCE_CHANGE_NOTIFIER_PROPERTY = "org.eclipse.team.ui.ResourceChangeNotifier"; //$NON-NLS-1$

	private ISynchronizationContext context;

	private class CompareInputLabelProvider extends BaseLabelProvider implements ICompareInputLabelProvider {

		@Override
		public Image getAncestorImage(Object input) {
			// No image desired
			return null;
		}

		@Override
		public String getAncestorLabel(Object input) {
			if (input instanceof ResourceDiffCompareInput) {
				ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) input;
				ITypedElement element = rdci.getAncestor();
				if (element != null) {
					final IFileRevision revision = ((FileRevisionTypedElement)element).getFileRevision();
					if (revision != null) {
						if (Utils.isShowAuthor()) {
							String author = ((FileRevisionTypedElement)element).getAuthor();
							if (author != null) {
								return NLS.bind(TeamUIMessages.SyncInfoCompareInput_baseLabelAuthorExists, new String[] { revision.getContentIdentifier(), author });
							} else if (revision.isPropertyMissing()) {
								fetchAuthors(rdci);
							}
						}
						return NLS.bind(TeamUIMessages.SyncInfoCompareInput_baseLabelExists, new String[] { revision.getContentIdentifier() });
					} else {
						return TeamUIMessages.SyncInfoCompareInput_baseLabel;
					}
				}
			}
			return null;
		}

		@Override
		public Image getLeftImage(Object input) {
			// No image desired
			return null;
		}

		@Override
		public String getLeftLabel(Object input) {
			if (input instanceof ResourceDiffCompareInput) {
				ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) input;
				String localContentId = rdci.getLocalContentId();
				if (localContentId != null) {
					ITypedElement element= rdci.getLeft();
					if (element instanceof LocalResourceTypedElement) {
						if (Utils.isShowAuthor()) {
							String author= ((LocalResourceTypedElement)element).getAuthor();
							if (author != null) {
								return NLS.bind(TeamUIMessages.SyncInfoCompareInput_localLabelAuthorExists, new String[] { localContentId, author });
							} else { // NOTE: Must not check for revision#isPropertyMissing() as this will always return true for the workspace file revision
								fetchAuthors(rdci);
							}
						}
					}
					return NLS.bind(TeamUIMessages.SyncInfoCompareInput_localLabelExists, new String[] { localContentId });
				} else {
					return TeamUIMessages.SyncInfoCompareInput_localLabel;
				}
			}
			return null;
		}

		@Override
		public Image getRightImage(Object input) {
			// No image desired
			return null;
		}

		@Override
		public String getRightLabel(Object input) {
			if (input instanceof ResourceDiffCompareInput) {
				ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) input;
				ITypedElement element = rdci.getRight();
				if (element != null) {
					final IFileRevision revision = ((FileRevisionTypedElement)element).getFileRevision();
					if (revision != null) {
						if (Utils.isShowAuthor()) {
							String author = ((FileRevisionTypedElement)element).getAuthor();
							if (author != null) {
								return NLS.bind(TeamUIMessages.SyncInfoCompareInput_remoteLabelAuthorExists, new String[] { revision.getContentIdentifier(), author });
							} else if (revision.isPropertyMissing()) {
								fetchAuthors(rdci);
							}
						}
						return NLS.bind(TeamUIMessages.SyncInfoCompareInput_remoteLabelExists, new String[] { revision.getContentIdentifier() });
					} else {
						return TeamUIMessages.SyncInfoCompareInput_remoteLabel;
					}
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof ICompareInput) {
				ICompareInput ci = (ICompareInput) element;
				return ci.getImage();
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ICompareInput) {
				ICompareInput ci = (ICompareInput) element;
				return ci.getName();
			}
			return null;
		}

		public void fireChangeEvent(final Object element) {
			Display.getDefault().asyncExec(() -> fireLabelProviderChanged(new LabelProviderChangedEvent(CompareInputLabelProvider.this, element)));
		}
	}

	/**
	 * Return a compare input change notifier that will detect changes in the synchronization context and
	 * translate them into compare input change events by calling {@link ResourceDiffCompareInput#update()}.
	 * @param context  the synchronization context
	 * @return a compare input change notifier
	 */
	public static ResourceCompareInputChangeNotifier getChangeNotifier(ISynchronizationContext context) {
		ResourceCompareInputChangeNotifier notifier = (ResourceCompareInputChangeNotifier)context.getCache().get(ResourceCompareInputChangeNotifier.RESOURCE_CHANGE_NOTIFIER_PROPERTY);
		if (notifier == null) {
			notifier = new ResourceCompareInputChangeNotifier(context);
			context.getCache().put(ResourceCompareInputChangeNotifier.RESOURCE_CHANGE_NOTIFIER_PROPERTY, notifier);
		}
		return notifier;
	}

	private final CompareInputLabelProvider labelProvider = new CompareInputLabelProvider();

	private Object fetchingInput;

	/**
	 * Create a notifier
	 * @param context a synchronization context
	 */
	public ResourceCompareInputChangeNotifier(ISynchronizationContext context) {
		super();
		this.context = context;
		// We can initialize in the constructor since the context will allow us to dispose
		initialize();
	}

	@Override
	public void initialize() {
		context.getDiffTree().addDiffChangeListener(this);
		context.getCache().addCacheListener(cache -> dispose());
		super.initialize();
	}

	@Override
	public void dispose() {
		super.dispose();
		context.getDiffTree().removeDiffChangeListener(this);
		labelProvider.dispose();
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		Set<ICompareInput> changedInputs = new HashSet<>();
		IDiff[] added = event.getAdditions();
		for (IDiff diff : added) {
			ICompareInput input = findInput(ResourceDiffTree.getResourceFor(diff));
			if (input != null)
				changedInputs.add(input);
		}
		IDiff[] changed = event.getChanges();
		for (IDiff diff : changed) {
			ICompareInput input = findInput(ResourceDiffTree.getResourceFor(diff));
			if (input != null)
				changedInputs.add(input);
		}
		IPath[] paths = event.getRemovals();
		for (IPath path : paths) {
			ICompareInput input = findInput(path);
			if (input != null)
				changedInputs.add(input);
		}

		if (!changedInputs.isEmpty())
			handleInputChanges(changedInputs.toArray(new ICompareInput[changedInputs.size()]), false);
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Property changes are not interesting w.r.t. state changes
	}

	@Override
	protected IResource[] getResources(ICompareInput input) {
		IResource resource = getResource(input);
		if (resource == null)
			return new IResource[0];
		return new IResource[] { resource };
	}

	private IResource getResource(ICompareInput input) {
		if (input instanceof IResourceProvider) {
			IResourceProvider rp = (IResourceProvider) input;
			return rp.getResource();
		}
		return Utils.getResource(input);
	}

	private ICompareInput findInput(IPath path) {
		ICompareInput[] inputs = getConnectedInputs();
		for (ICompareInput input : inputs) {
			IResource inputResource = getResource(input);
			if (inputResource != null && inputResource.getFullPath().equals(path)) {
				return input;
			}
		}
		return null;
	}

	private ICompareInput findInput(IResource resource) {
		ICompareInput[] inputs = getConnectedInputs();
		for (ICompareInput input : inputs) {
			IResource inputResource = getResource(input);
			if (inputResource != null && inputResource.equals(resource)) {
				return input;
			}
		}
		return null;
	}

	@Override
	protected void prepareInput(ICompareInput input, IProgressMonitor monitor) {
		if (input instanceof ResourceDiffCompareInput) {
			ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) input;
			IResource resource = rdci.getResource();
			IDiff diff = getContext().getDiffTree().getDiff(resource);
			try {
				ResourceDiffCompareInput.ensureContentsCached(diff, monitor);
			} catch (CoreException e) {
				// Log the exception and continue
				TeamUIPlugin.log(e);
			}
		}
		super.prepareInput(input, monitor);
	}

	/**
	 * Return the label provider for resource compare inputs.
	 * @return the label provider for resource compare inputs
	 */
	public ICompareInputLabelProvider getLabelProvider() {
		return labelProvider;
	}

	public void fetchAuthors(final ResourceDiffCompareInput input) {
		if (fetchingInput == input)
			return;
		fetchingInput= input;
		runInBackground(monitor -> fetchAuthors(input, monitor));
	}

	protected void fetchAuthors(ResourceDiffCompareInput input, IProgressMonitor monitor) throws CoreException {
		if (input.updateAuthorInfo(monitor))
			fireLabelProviderChange(input);
	}

	private void fireLabelProviderChange(Object input) {
		labelProvider.fireChangeEvent(input);
	}

	/**
	 * Return the synchronization context to which this notifier is associated.
	 * @return the synchronization context to which this notifier is associated
	 */
	public final ISynchronizationContext getContext() {
		return context;
	}

	@Override
	protected boolean belongsTo(Object family) {
		return family == getContext();
	}
}
