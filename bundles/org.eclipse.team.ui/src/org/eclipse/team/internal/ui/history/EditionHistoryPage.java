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
package org.eclipse.team.internal.ui.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator2;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.core.history.LocalFileHistory;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IWorkbenchPage;

/**
 * A history page for a sub-element of a file.
 * <p>
 * If the site is modal, the local edition is created up-front and destroyed when the page is destroyed.
 * Otherwise, the local edition is only created when needed. The {@link #getCompareInput(Object)} and
 * {@link #prepareInput(ICompareInput, org.eclipse.compare.CompareConfiguration, IProgressMonitor)}
 * methods are only used when the site is modal so they can use the localEdition.
 */
public class EditionHistoryPage extends LocalHistoryPage {
	private IFile file;

	private Object element;

	private LocalResourceTypedElement localFileElement;
	private IStructureCreator structureCreator;
	private Map<IFileRevision, ITypedElement> editions = new HashMap<>();
	private ITypedElement localEdition;
	private String name;

	class CompareEditionAction extends CompareRevisionAction {

		public CompareEditionAction(HistoryPage page) {
			super(page);
		}

		@Override
		protected ITypedElement getElementFor(IResource resource) {
			if (resource.equals(file))
				return localFileElement;
			return super.getElementFor(resource);
		}

		@Override
		protected CompareFileRevisionEditorInput createCompareEditorInput(ITypedElement left, ITypedElement right, IWorkbenchPage page) {
			ITypedElement leftEdition = getEdition(left);
			boolean leftIsLocal = false;
			if (leftEdition == null && left instanceof LocalResourceTypedElement) {
				leftEdition = createLocalEdition(structureCreator, localFileElement, element);
				leftIsLocal = true;
			}
			ITypedElement rightEdition = getEdition(right);
			return new CompareEditionsEditorInput(structureCreator, left, right, leftEdition, rightEdition, leftIsLocal, page);
		}

		private ITypedElement getEdition(ITypedElement input) {
			if (input instanceof FileRevisionTypedElement) {
				FileRevisionTypedElement te = (FileRevisionTypedElement) input;
				return getEditionFor(te.getRevision());
			}
			return null;
		}
	}

	static class CompareEditionsEditorInput extends CompareFileRevisionEditorInput {

		private final ITypedElement leftRevision;
		private final ITypedElement rightRevision;
		private final boolean leftIsLocal;
		private IStructureCreator structureCreator;

		public CompareEditionsEditorInput(IStructureCreator structureCreator, ITypedElement left,
				ITypedElement right, ITypedElement leftEdition,
				ITypedElement rightEdition, boolean leftIsLocal, IWorkbenchPage page) {
			super(leftEdition, rightEdition, page);
			this.structureCreator = structureCreator;
			leftRevision = left;
			rightRevision = right;
			this.leftIsLocal = leftIsLocal;
		}

		@Override
		public LocalResourceTypedElement getLocalElement() {
			if (leftRevision instanceof LocalResourceTypedElement) {
				return (LocalResourceTypedElement) leftRevision;
			}
			return super.getLocalElement();
		}

		@Override
		protected FileRevisionTypedElement getRightRevision() {
			if (rightRevision instanceof FileRevisionTypedElement) {
				return (FileRevisionTypedElement) rightRevision;
			}
			return null;
		}

		@Override
		protected FileRevisionTypedElement getLeftRevision() {
			if (leftRevision instanceof FileRevisionTypedElement) {
				return (FileRevisionTypedElement) leftRevision;
			}
			return null;
		}
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IFile.class)
				return null;
			return super.getAdapter(adapter);
		}

		@Override
		protected void handleDispose() {
			if (leftIsLocal && structureCreator != null)
				internalDestroy(structureCreator, getLeft());
			structureCreator = null;
			super.handleDispose();
		}
	}

	public static ITypedElement getPreviousState(IFile file, Object element) throws TeamException {
		LocalResourceTypedElement localFileElement= new LocalResourceTypedElement(file);
		IStructureCreator structureCreator = CompareUI.createStructureCreator(localFileElement);
		if (structureCreator == null)
			return null;
		LocalFileHistory history = new LocalFileHistory(file, false);
		history.refresh(new NullProgressMonitor());
		IFileRevision[] revisions = history.getFileRevisions();
		if (revisions.length == 0)
			return null;
		sortDescending(revisions);
		ITypedElement localEdition = null;
		try {
			localEdition = createLocalEdition(structureCreator, localFileElement, element);
			for (IFileRevision revision : revisions) {
				ITypedElement edition = createEdition(structureCreator, element, new FileRevisionTypedElement(revision));
				if (edition != null && !contentsEqual(structureCreator, localEdition, edition)) {
					return edition;
				}
			}
		} finally {
			if (localEdition != null)
				destroyLocalEdition(structureCreator, localFileElement, localEdition);
		}
		return null;
	}

	public EditionHistoryPage(IFile file, Object element) {
		super(ON | ALWAYS);
		setInput(file, element);
	}

	void setInput(IFile file, Object element) {
		Assert.isNotNull(file);
		Assert.isNotNull(element);
		this.file = file;
		this.element = element;
		this.localFileElement= new LocalResourceTypedElement(getFile());
		structureCreator = CompareUI.createStructureCreator(localFileElement);
	}

	@Override
	public void setSite(IHistoryPageSite site) {
		super.setSite(site);
		// If the site is modal, create the local edition
		if (site.isModal()) {
			localEdition = createLocalEdition(structureCreator, localFileElement, element);
		}
	}

	@Override
	protected IFile getFile() {
		return file;
	}

	@Override
	protected void update(IFileRevision[] revisions, IProgressMonitor monitor) {
		monitor.beginTask(null, 100);
		ITypedElement te = null;
		try {
			if (localEdition == null) {
				te = createLocalEdition(structureCreator, localFileElement, element);
			} else {
				te = localEdition;
			}
			if (te != null) {
				String oldValue = getName();
				String oldDesc = getDescription();
				name = te.getName();
				IFileRevision[] filtered = filterRevisions(te, revisions, Policy.subMonitorFor(monitor, 75));
				super.update(filtered, Policy.subMonitorFor(monitor, 25));
				firePropertyChange(this, IHistoryPage.P_NAME, oldValue, getName());
				firePropertyChange(this, IHistoryPage.P_DESCRIPTION, oldDesc, getDescription());
			}
		} finally {
			if (localEdition == null && te != null)
				internalDestroy(structureCreator, te);
			monitor.done();
		}
	}

	private IFileRevision[] filterRevisions(ITypedElement localEdition, IFileRevision[] revisions,
			IProgressMonitor monitor) {
		ITypedElement previousEdition = localEdition;
		List<IFileRevision> result = new ArrayList<>();
		sortDescending(revisions);
		editions.clear();
		for (IFileRevision revision : revisions) {
			ITypedElement edition = createEdition(new FileRevisionTypedElement(revision));
			if (edition != null && !contentsEqual(structureCreator, previousEdition, edition)) {
				editions.put(revision, edition);
				previousEdition = edition;
				result.add(revision);
			}
		}
		return result.toArray(new IFileRevision[result.size()]);
	}

	private static void sortDescending(IFileRevision[] revisions) {
		Arrays.sort(revisions, (d1, d2) -> -Long.compare(d1.getTimestamp(), d2.getTimestamp()));
	}

	private static boolean contentsEqual(IStructureCreator creator, ITypedElement previousEdition,
			ITypedElement edition) {
		if (previousEdition == null || creator == null || edition == null)
			return false;
		String contents1 = creator.getContents(previousEdition, false /* TODO: Ignore whitespace */);
		String contents2 = creator.getContents(edition, false /* TODO: Ignore whitespace */);
		return (contents1 != null && contents2 != null && contents1.equals(contents2));
	}

	@Override
	public ICompareInput getCompareInput(Object object) {
		ITypedElement edition = getEditionFor(object);
		if (edition != null && localEdition != null)
			return new DiffNode(localEdition, edition);
		return null;
	}

	public ITypedElement getEditionFor(Object object) {
		return editions.get(object);
	}

	private static ITypedElement createLocalEdition(IStructureCreator creator, ITypedElement input, Object element) {
		if (creator == null)
			return null;
		ITypedElement result = null;
		if (creator instanceof IStructureCreator2) {
			IStructureCreator2 sc2 = (IStructureCreator2) creator;
			try {
				result = sc2.createElement(element, input, null);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		if (result == null) {
			result = createEdition(creator, element, input);
		}
		return result;
	}

	private ITypedElement createEdition(ITypedElement input) {
		return createEdition(structureCreator, element, input);
	}

	private static ITypedElement createEdition(IStructureCreator creator, Object element, ITypedElement input) {
		if (creator == null)
			return null;
		IStructureComparator edition = creator.locate(element, input);
		if (edition instanceof ITypedElement)
			return (ITypedElement) edition;
		return null;
	}

	@Override
	public boolean isValidInput(Object object) {
		// This page doesn't support input changes
		return object.equals(element);
	}

	@Override
	public void dispose() {
		try {
			disconnect();
		} finally {
			super.dispose();
		}
	}

	private void disconnect() {
		if (localFileElement != null)
			localFileElement.discardBuffer();
		internalDestroy(structureCreator, localEdition);
		localEdition = null;
		structureCreator = null;
	}

	private static void internalDestroy(IStructureCreator creator, ITypedElement te) {
		if (te != null && creator instanceof IStructureCreator2) {
			IStructureCreator2 sc2 = (IStructureCreator2) creator;
			sc2.destroy(te);
		}
	}

	private static void destroyLocalEdition(
			IStructureCreator structureCreator, LocalResourceTypedElement localFileElement, ITypedElement localEdition) {
		if (localFileElement != null)
			localFileElement.discardBuffer();
		if (localEdition != null && structureCreator instanceof IStructureCreator2) {
			IStructureCreator2 sc2 = (IStructureCreator2) structureCreator;
			sc2.destroy(localEdition);
		}
	}

	@Override
	protected String getNoChangesMessage() {
		if (name != null)
			return NLS.bind(TeamUIMessages.EditionHistoryPage_0, name);
		return TeamUIMessages.EditionHistoryPage_1;
	}

	@Override
	protected Image getImage(Object object) {
		if (object == localEdition)
			return localEdition.getImage();
		Object revision = getRevisionFor(object);
		if (revision != null)
			return super.getImage(revision);
		return super.getImage(object);
	}

	@Override
	protected String getLabel(Object object) {
		Object revision = getRevisionFor(object);
		if (revision != null)
			return super.getLabel(revision);
		return super.getLabel(object);
	}

	private Object getRevisionFor(Object object) {
		if (object == localEdition)
			return localFileElement;
		for (IFileRevision revision : editions.keySet()) {
			if (editions.get(revision) == object) {
				return revision;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		if (name != null)
			return name;
		return super.getName();
	}

	@Override
	public String getDescription() {
		if (name != null)
			return NLS.bind(TeamUIMessages.EditionHistoryPage_2, name);
		return super.getDescription();
	}

	@Override
	protected CompareRevisionAction createCompareAction() {
		return new CompareEditionAction(this);
	}

}
