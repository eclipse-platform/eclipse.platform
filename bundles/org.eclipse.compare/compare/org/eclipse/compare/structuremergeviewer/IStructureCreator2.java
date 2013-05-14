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

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An extension to the {@link IStructureCreator} interface that supports the
 * use of shared documents.
 * <p>
 * This interface is not intended to be implemented by clients. Client should instead
 * subclass {@link StructureCreator}.
 * </p>
 * @since 3.3
 */
public interface IStructureCreator2 extends IStructureCreator {
	
	/**
	 * Creates a tree structure consisting of <code>IStructureComparator</code>s
	 * from the given object and returns its root object. Implementing this
	 * method typically involves parsing the input object. In case of an error
	 * (e.g. a parsing error) the value <code>null</code> is returned.
	 * <p>
	 * This method is equivalent to
	 * {@link IStructureCreator#getStructure(Object)} with the exception that 
	 * the {@link #destroy(Object)} method must be called with the returned
	 * comparator as a parameter when the comparator is no longer
	 * needed. This is done to allow structure creators
	 * to make use of shared resources such a file buffer.
	 * <p>
	 * Also, the node returned from this method should adapt to an 
	 * {@link ISharedDocumentAdapter} if the provided input has 
	 * a shared document adapter and it is being used by the 
	 * this creator. The convenience class {@link SharedDocumentAdapterWrapper}
	 * is provided to allow the creator to wrap the adapter of the input
	 * so that the proper key can be returned.
	 * 
	 * @param input
	 *            the object from which to create the tree of
	 *            <code>IStructureComparator</code>
	 * @param monitor a progress monitor or <code>null</code> if progress and cancelation is not required
	 * @return the root node of the structure or <code>null</code> in case of
	 *         error
	 * @throws CoreException
	 * @see IStructureCreator#getStructure(Object)
	 * @see #destroy(Object)
	 */
	IStructureComparator createStructure(Object input, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Creates the single node specified by path from the given input object.
	 * This method is equivalent to
	 * {@link IStructureCreator#locate(Object, Object)} with the exception that 
	 * the {@link #destroy(Object)} method must be called with the returned
	 * element as a parameter when the element is no longer
	 * needed. This is done to allow structure creators
	 * to make use of shared resources such a file buffer.
	 * 
	 * @param element specifies a sub object within the input object
	 * @param input the object from which to create the
	 *            <code>ITypedElement</code>
	 * @param monitor a progress monitor or <code>null</code> if progress is not desired
	 * @return the single node specified by <code>path</code> or
	 *         <code>null</code>
	 * @throws CoreException if an error occurs while parsing the input
	 * 
	 * @see IStructureCreator#locate(Object, Object)
	 * @see #destroy(Object)
	 */
	ITypedElement createElement(Object element, Object input, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Release any resources associated with the given object.
	 * This method must be called for objects returned from either
	 * {@link #createStructure(Object, IProgressMonitor)} or
	 * {@link #createElement(Object, Object, IProgressMonitor)}.
	 * @param object the object to be destroyed
	 * @see #createElement(Object, Object, IProgressMonitor)
	 * @see #createStructure(Object, IProgressMonitor)
	 */
	void destroy(Object object);
}
