/*******************************************************************************
 *  Copyright (c) 2006, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.mapping.AbstractCompareInput;
import org.eclipse.team.internal.ui.mapping.CompareInputChangeNotifier;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchPage;

public class CompareFileRevisionEditorInput extends SaveableCompareEditorInput {

	private ITypedElement left;
	private ITypedElement right;

	CompareInputChangeNotifier notifier = new CompareInputChangeNotifier() {
		@Override
		protected IResource[] getResources(ICompareInput input) {
			IResource resource = getResource(input);
			if (resource == null)
				return new IResource[0];
			return new IResource[] { resource };
		}
	};

	/*
	 * Returns <code>true</code> if the other object is of type
	 * <code>CompareFileRevisionEditorInput</code> and both of their
	 * corresponding left and right objects are identical. The content is not
	 * considered.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof CompareFileRevisionEditorInput) {
			CompareFileRevisionEditorInput other = (CompareFileRevisionEditorInput) obj;
			return other.getLeft().equals(left)
					&& other.getRightRevision().equals(right);
		}
		return false;
	}

	public class MyDiffNode extends AbstractCompareInput {
		public MyDiffNode(ITypedElement left, ITypedElement right) {
			super(Differencer.CHANGE, null, left, right);
		}
		@Override
		public void fireChange() {
			super.fireChange();
		}
		@Override
		protected CompareInputChangeNotifier getChangeNotifier() {
			return notifier;
		}
		@Override
		public boolean needsUpdate() {
			// The remote never changes
			return false;
		}
		@Override
		public void update() {
			fireChange();
		}
	}

	/**
	 * Creates a new CompareFileRevisionEditorInput.
	 * @param left
	 * @param right
	 * @param page
	 */
	public CompareFileRevisionEditorInput(ITypedElement left, ITypedElement right, IWorkbenchPage page) {
		super(new CompareConfiguration(), page);
		this.left = left;
		this.right = right;
		setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title, new String[] { left.getName() }));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.LocalResourceCompareEditorInput#internalPrepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected ICompareInput prepareCompareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		ICompareInput input = createCompareInput();
		getCompareConfiguration().setLeftEditable(isLeftEditable(input));
		getCompareConfiguration().setRightEditable(false);
		ensureContentsCached(getLeftRevision(), getRightRevision(), monitor);
		initLabels(input);
		return input;
	}

	protected FileRevisionTypedElement getRightRevision() {
		if (right instanceof FileRevisionTypedElement) {
			return (FileRevisionTypedElement) right;
		}
		return null;
	}

	protected FileRevisionTypedElement getLeftRevision() {
		if (left instanceof FileRevisionTypedElement) {
			return (FileRevisionTypedElement) left;
		}
		return null;
	}

	private static void ensureContentsCached(FileRevisionTypedElement left, FileRevisionTypedElement right,
			IProgressMonitor monitor) {
		if (left != null) {
			try {
				left.cacheContents(monitor);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		if (right != null) {
			try {
				right.cacheContents(monitor);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
	}

	private boolean isLeftEditable(ICompareInput input) {
		Object left = input.getLeft();
		if (left instanceof IEditableContent) {
			return ((IEditableContent) left).isEditable();
		}
		return false;
	}

	private IResource getResource(ICompareInput input) {
		if (getLocalElement() instanceof IResourceProvider) {
			return ((IResourceProvider) getLocalElement()).getResource();
		}
		return null;
	}

	private ICompareInput createCompareInput() {
		MyDiffNode input = new MyDiffNode(left,right);
		return input;
	}

	private void initLabels(ICompareInput input) {
		CompareConfiguration cc = getCompareConfiguration();
		if (getLeftRevision() != null) {
			String leftLabel = getFileRevisionLabel(getLeftRevision());
			cc.setLeftLabel(leftLabel);
		} else if (left instanceof LocalResourceTypedElement) {
			String name= TextProcessor.process(input.getLeft().getName());
			String leftLabel= getLocalResourceRevisionLabel((LocalResourceTypedElement)left, name);
			cc.setLeftLabel(leftLabel);
		} else if (getResource(input) != null) {
			String leftLabel= NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_workspace, new Object[] { TextProcessor.process(input.getLeft().getName()) });
			cc.setLeftLabel(leftLabel);
		}
		if (getRightRevision() != null) {
			String rightLabel = getFileRevisionLabel(getRightRevision());
			cc.setRightLabel(rightLabel);
		}
	}

	private String getLocalResourceRevisionLabel(LocalResourceTypedElement localElement, String name) {
		if (Utils.isShowAuthor()) {
			String author= localElement.getAuthor();
			if (author == null) {
				try {
					localElement.fetchAuthor(null);
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
				author= localElement.getAuthor();
			}
			if (author != null)
				return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_workspace_authorExists, new Object[] { name, author });
		}
		return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_workspace, new Object[] { name });
	}

	private String getFileRevisionLabel(FileRevisionTypedElement element) {
		Object fileObject = element.getFileRevision();
		if (fileObject instanceof LocalFileRevision){
			IFileState state = ((LocalFileRevision) fileObject).getState();
			if (state != null) {
				return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_localRevision, new Object[]{TextProcessor.process(element.getName()), element.getTimestamp()});
			}
		} else {
			if (Utils.isShowAuthor())
				return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_repository, new Object[] { element.getName(), element.getContentIdentifier(), element.getAuthor() });
			else
				return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_repositoryWithoutAuthor, new Object[] { element.getName(), element.getContentIdentifier() });
		}
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		Object[] titleObject = new Object[3];
		titleObject[0] = getLongName(left);
		titleObject[1] = getContentIdentifier(getLeftRevision());
		titleObject[2] = getContentIdentifier(getRightRevision());
		return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_compareResourceAndVersions, titleObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getTitle()
	 */
	@Override
	public String getTitle() {
		Object[] titleObject = new Object[3];
		titleObject[0] = getShortName(left);
		titleObject[1] = getContentIdentifier(getLeftRevision());
		titleObject[2] = getContentIdentifier(getRightRevision());
		return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_compareResourceAndVersions, titleObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IFile.class || adapter == IResource.class) {
			if (getLocalElement() != null) {
				return getLocalElement().getResource();
			}
			return null;
		}
		return super.getAdapter(adapter);
	}

	private String getShortName(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getName();
		}
		else if (element instanceof LocalResourceTypedElement){
			LocalResourceTypedElement typedContent = (LocalResourceTypedElement) element;
			return typedContent.getResource().getName();
		}
		return element.getName();
	}

	private String getLongName(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getPath();
		}
		else if (element instanceof LocalResourceTypedElement){
			LocalResourceTypedElement typedContent = (LocalResourceTypedElement) element;
			return typedContent.getResource().getFullPath().toString();
		}
		return element.getName();
	}

	private String getContentIdentifier(ITypedElement element){
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			Object fileObject = fileRevisionElement.getFileRevision();
			if (fileObject instanceof LocalFileRevision){
				try {
					IStorage storage = ((LocalFileRevision) fileObject).getStorage(new NullProgressMonitor());
					if (Utils.getAdapter(storage, IFileState.class) != null){
						//local revision
						return TeamUIMessages.CompareFileRevisionEditorInput_0;
					} else if (Utils.getAdapter(storage, IFile.class) != null) {
						//current revision
						return TeamUIMessages.CompareFileRevisionEditorInput_1;
					}
				} catch (CoreException e) {
				}
			} else {
				return fileRevisionElement.getContentIdentifier();
			}
		}
		return TeamUIMessages.CompareFileRevisionEditorInput_2;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.LocalResourceCompareEditorInput#fireInputChange()
	 */
	@Override
	protected void fireInputChange() {
		((MyDiffNode)getCompareResult()).fireChange();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SaveableCompareEditorInput#contentsCreated()
	 */
	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		notifier.initialize();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SaveableCompareEditorInput#handleDispose()
	 */
	@Override
	protected void handleDispose() {
		super.handleDispose();
		notifier.dispose();
		if (getLocalElement() != null) {
			getLocalElement().discardBuffer();
		}
	}

	public LocalResourceTypedElement getLocalElement() {
		if (left instanceof LocalResourceTypedElement) {
			return (LocalResourceTypedElement) left;
		}
		return null;
	}

	public ITypedElement getLeft() {
		return left;
	}
}
