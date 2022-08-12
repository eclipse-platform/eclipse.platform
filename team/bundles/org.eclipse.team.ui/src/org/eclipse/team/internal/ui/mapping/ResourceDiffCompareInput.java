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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

/**
 * A resource-based compare input that gets it's contributors from an {@link IDiff}.
 */
public class ResourceDiffCompareInput extends AbstractCompareInput implements ISynchronizationCompareInput, IAdaptable, IResourceProvider {

	private IDiff node;
	private final ISynchronizationContext context;

	public static int getCompareKind(IDiff node) {
		int compareKind = 0;
		if (node != null) {
			switch (node.getKind()) {
			case IDiff.ADD:
				compareKind = Differencer.ADDITION;
				break;
			case IDiff.REMOVE:
				compareKind = Differencer.DELETION;
				break;
			case IDiff.CHANGE:
				compareKind = Differencer.CHANGE;
				break;
			}
			if (node instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) node;
				switch (twd.getDirection()) {
				case IThreeWayDiff.OUTGOING :
					compareKind |= Differencer.RIGHT;
					break;
				case IThreeWayDiff.INCOMING :
					compareKind |= Differencer.LEFT;
					break;
				case IThreeWayDiff.CONFLICTING :
					compareKind |= Differencer.LEFT;
					compareKind |= Differencer.RIGHT;
					break;
				}
			}
		}
		return compareKind;
	}

	private static FileRevisionTypedElement getRightContributor(IDiff node) {
		// For a resource diff, use the after state
		if (node instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) node;
			return asTypedElement(rd.getAfterState(), getLocalEncoding(node));
		}
		if (node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
			// If there is a remote change, use the after state
			if (diff != null)
				return getRightContributor(diff);
			// There's no remote change so use the before state of the local
			diff = (IResourceDiff)twd.getLocalChange();
			return asTypedElement(diff.getBeforeState(), getLocalEncoding(node));

		}
		return null;
	}

	private static LocalResourceTypedElement getLeftContributor(final IDiff node) {
		// The left contributor is always the local resource
		return new LocalResourceTypedElement(ResourceDiffTree.getResourceFor(node));
	}

	private static FileRevisionTypedElement getAncestor(IDiff node) {
		if (node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			IResourceDiff diff = (IResourceDiff)twd.getLocalChange();
			if (diff == null)
				diff = (IResourceDiff)twd.getRemoteChange();
			return asTypedElement(diff.getBeforeState(), getLocalEncoding(node));

		}
		return null;
	}

	private static String getLocalEncoding(IDiff node) {
		IResource resource = ResourceDiffTree.getResourceFor(node);
		if (resource instanceof IEncodedStorage) {
			IEncodedStorage es = (IEncodedStorage) resource;
			try {
				return es.getCharset();
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return null;
	}

	private static FileRevisionTypedElement asTypedElement(IFileRevision state, String localEncoding) {
		if (state == null)
			return null;
		return new FileRevisionTypedElement(state, localEncoding);
	}

	public static void ensureContentsCached(IDiff diff, IProgressMonitor monitor) throws CoreException {
		if (diff != null) {
			ensureContentsCached(getAncestor(diff), getRightContributor(diff), monitor);
		}
	}

	private static void ensureContentsCached(Object ancestor, Object right,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor sm = SubMonitor.convert(monitor, 100);
		if (ancestor instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement fste = (FileRevisionTypedElement) ancestor;
			fste.cacheContents(sm.newChild(50));
		} else {
			sm.setWorkRemaining(50);
		}
		if (right instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement fste = (FileRevisionTypedElement) right;
			fste.cacheContents(sm.newChild(50));
		}
		if (monitor != null)
			monitor.done();
	}

	/**
	 * Create the compare input on the given diff.
	 * @param diff the diff
	 * @param context the synchronization context
	 */
	public ResourceDiffCompareInput(IDiff diff, ISynchronizationContext context) {
		super(getCompareKind(diff), getAncestor(diff), getLeftContributor(diff), getRightContributor(diff));
		this.node = diff;
		this.context = context;
	}

	/**
	 * Fire a compare input change event.
	 * This method is public so that the change can be fired
	 * by the containing editor input on a save.
	 */
	@Override
	public void fireChange() {
		super.fireChange();
	}

	@Override
	public void prepareInput(CompareConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		configuration.setLabelProvider(this, ((ResourceCompareInputChangeNotifier)getChangeNotifier()).getLabelProvider());
		ensureContentsCached(getAncestor(), getRight(), monitor);
	}

	@Override
	public SaveableComparison getSaveable() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IFile.class || adapter == IResource.class) {
			return (T) ResourceDiffTree.getResourceFor(node);
		}
		if (adapter == ResourceMapping.class) {
			IResource resource = ResourceDiffTree.getResourceFor(node);
			return resource.getAdapter(adapter);
		}
		return null;
	}

	@Override
	public String getFullPath() {
		final IResource resource = ResourceDiffTree.getResourceFor(node);
		if (resource != null)
			return resource.getFullPath().toString();
		return getName();
	}

	@Override
	public boolean isCompareInputFor(Object object) {
		final IResource resource = ResourceDiffTree.getResourceFor(node);
		IResource other = Utils.getResource(object);
		if (resource != null && other != null)
			return resource.equals(other);
		return false;
	}

	@Override
	public IResource getResource() {
		return ResourceDiffTree.getResourceFor(node);
	}

	/**
	 * Return a compare input change notifier that will detect changes in the synchronization context and
	 * translate them into compare input change events by calling {@link #update()}.
	 * @return a compare input change notifier
	 */
	@Override
	public CompareInputChangeNotifier getChangeNotifier() {
		return ResourceCompareInputChangeNotifier.getChangeNotifier(context);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof ResourceDiffCompareInput) {
			ResourceDiffCompareInput otherInput = (ResourceDiffCompareInput) other;
			return (isEqual(getLeft(), otherInput.getLeft())
					&& isEqual(getRight(), otherInput.getRight())
					&& isEqual(getAncestor(), otherInput.getAncestor()));
		}
		return false;
	}

	private boolean isEqual(ITypedElement e1, ITypedElement e2) {
		if (e1 == null) {
			return e2 == null;
		}
		if (e2 == null)
			return false;
		return e1.equals(e2);
	}

	@Override
	public int hashCode() {
		return getResource().hashCode();
	}

	/**
	 * Re-obtain the diff for this compare input and update the kind and 3
	 * contributor appropriately.
	 */
	@Override
	public void update() {
		IDiff newNode = context.getDiffTree().getDiff(getResource());
		if (newNode == null) {
			// The resource is in-sync. Just leave the ancestor and right the same and set the kind
			setKind(Differencer.NO_CHANGE);
			fireChange();
		} else {
			LocalResourceTypedElement left = (LocalResourceTypedElement)getLeft();
			if (!this.node.equals(newNode) || !left.isSynchronized()) {
				this.node = newNode;
				setKind(getCompareKind(node));
				left.update();
				FileRevisionTypedElement newRight = getRightContributor(node);
				propogateAuthorIfSameRevision((FileRevisionTypedElement)getRight(), newRight);
				setRight(newRight);
				FileRevisionTypedElement newAncestor = getAncestor(node);
				propogateAuthorIfSameRevision((FileRevisionTypedElement)getAncestor(), newAncestor);
				setAncestor(newAncestor);
				propogateAuthorIfSameRevision((FileRevisionTypedElement)getAncestor(), (FileRevisionTypedElement)getRight());
			}
			fireChange();
		}
	}

	private boolean propogateAuthorIfSameRevision(FileRevisionTypedElement oldContributor, FileRevisionTypedElement newContributor) {
		if (oldContributor == null || newContributor == null)
			return false;
		String author= oldContributor.getAuthor();
		if (newContributor.getAuthor() == null && author != null && oldContributor.getContentIdentifier().equals(newContributor.getContentIdentifier())) {
			newContributor.setAuthor(author);
			return true;
		}
		return false;
	}

	private boolean propogateAuthorIfSameRevision(FileRevisionTypedElement oldContributor, LocalResourceTypedElement newContributor) {
		if (oldContributor == null || newContributor == null)
			return false;
		String author= oldContributor.getAuthor();
		if (newContributor.getAuthor() == null && author != null && oldContributor.getContentIdentifier().equals(getLocalContentId())) {
			newContributor.setAuthor(author);
			return true;
		}
		return false;
	}

	/**
	 * Return whether the diff associated with this input has changed.
	 * @return whether the diff associated with this input has changed
	 */
	@Override
	public boolean needsUpdate() {
		IDiff newNode= context.getDiffTree().getDiff(getResource());
		return newNode == null || !newNode.equals(node);
	}

	/**
	 * Return the local content id for this compare input.
	 * @return the local content id for this compare input
	 */
	public String getLocalContentId() {
		return Utils.getLocalContentId(node);
	}

	public boolean updateAuthorInfo(IProgressMonitor monitor) throws CoreException {
		boolean fireEvent= false;
		FileRevisionTypedElement ancestor= (FileRevisionTypedElement)getAncestor();
		FileRevisionTypedElement right= (FileRevisionTypedElement)getRight();
		LocalResourceTypedElement left= (LocalResourceTypedElement)getLeft();

		if (ancestor != null && ancestor.getAuthor() == null) {
			ancestor.fetchAuthor(monitor);
			fireEvent|= ancestor.getAuthor() != null;
		}

		fireEvent|= propogateAuthorIfSameRevision(ancestor, right);
		fireEvent|= propogateAuthorIfSameRevision(ancestor, left);

		if (right != null && right.getAuthor() == null) {
			right.fetchAuthor(monitor);
			fireEvent|= right.getAuthor() != null;
		}

		fireEvent|= propogateAuthorIfSameRevision(right, left);

		if (left != null && left.getAuthor() == null) {
			left.fetchAuthor(monitor);
			fireEvent|= fireEvent= left.getAuthor() != null;
		}

		return fireEvent;
	}

}
