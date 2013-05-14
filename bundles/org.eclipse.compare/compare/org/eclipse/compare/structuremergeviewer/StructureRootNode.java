/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.services.IDisposable;

/**
 * A node that acts as the root of the tree returned from a {@link StructureCreator}.
 * This node performs the following tasks tasks:
 * <ol>
 * <li>It adapts to an {@link ISharedDocumentAdapter} that provides the proper
 * document key (@see {@link #getAdapter(Class)}).</li>
 * <li>It invokes {@link IStructureCreator#save(IStructureComparator, Object)}
 * when {@link #nodeChanged(DocumentRangeNode)} is called.</li>
 * <li>It disposes of the {@link IDisposable} provided in the constructor when
 * {@link #dispose()} is called.</li>
 * </ol>
 * <p>
 * This class may be subclassed by clients.
 *
 * @since 3.3
 */
public class StructureRootNode extends DocumentRangeNode implements IDisposable, ITypedElement {

	/**
	 * The integer constant (value <code>0</code>) that is used as the type code of the root node.
	 * @see #getTypeCode()
	 */
	public static final int ROOT_TYPE = 0;
	
	/**
	 * The string constant (value <code>"root"</code>) that is used as the id of the root node.
	 * @see #getId()
	 */
	public static final String ROOT_ID = "root"; //$NON-NLS-1$

	private final Object fInput;
	private final StructureCreator fCreator;
	private ISharedDocumentAdapter fAdapter;
	
	/**
	 * Create the structure root node.
	 * @param document the document
	 * @param input the input associated with the document
	 * @param creator the structure creator that is creating the node
	 * @param adapter the shared document adapter from which the document was obtained or <code>null</code>
	 *    if the document was not obtained from an {@link ISharedDocumentAdapter}
	 */
	public StructureRootNode(IDocument document, Object input, StructureCreator creator, ISharedDocumentAdapter adapter) {
		super(null, ROOT_TYPE, ROOT_ID, document, 0, document.getLength());
		fInput = input;
		fCreator = creator;
		fAdapter = adapter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.services.IDisposable#dispose()
	 */
	public void dispose() {
		if (fAdapter != null) {
			fAdapter.disconnect(fInput);
		}
	}

	/**
	 * Override {@link IAdaptable#getAdapter(Class)} in order to provide
	 * an {@link ISharedDocumentAdapter} that provides the proper look up key based
	 * on the input from which this structure node was created.
	 * @param adapter the adapter class to look up
	 * @return the object adapted to the given class or <code>null</code>
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			return fAdapter;
		}
		return super.getAdapter(adapter);
	}
	
	/**
	 * Override in order to invoke {@link IStructureCreator#save(IStructureComparator, Object)} when the
	 * contents of a node have changed.
	 * @param node the changed node
	 */
	protected void nodeChanged(DocumentRangeNode node) {
		fCreator.save(this, fInput);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#replace(org.eclipse.compare.ITypedElement, org.eclipse.compare.ITypedElement)
	 */
	public ITypedElement replace(ITypedElement child, ITypedElement other) {
		// TODO: I believe the parent implementation is flawed but didn't to remove
		// it in case I was missing something so I overrode it instead
		nodeChanged(this);
		return child;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	public String getName() {
		return getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		return FOLDER_TYPE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#isReadOnly()
	 */
	public boolean isReadOnly() {
		if (fInput instanceof IEditableContentExtension) {
			IEditableContentExtension ext = (IEditableContentExtension) fInput;
			return ext.isReadOnly();
		}
		return super.isReadOnly();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#validateEdit(org.eclipse.swt.widgets.Shell)
	 */
	public IStatus validateEdit(Shell shell) {
		if (fInput instanceof IEditableContentExtension) {
			IEditableContentExtension ext = (IEditableContentExtension) fInput;
			return ext.validateEdit(shell);
		}
		return super.validateEdit(shell);
	}

}
