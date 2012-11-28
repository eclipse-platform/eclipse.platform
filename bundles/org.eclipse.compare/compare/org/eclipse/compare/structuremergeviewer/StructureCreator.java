/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.patch.LineReader;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * An {@link IStructureCreator2} that attempts to use an {@link IDocumentProvider}
 * to obtain a shared document for an {@link ITypedElement}.
 * <p>
 * Clients may subclass this class.
 * </p>
 * 
 * @since 3.3
 */
public abstract class StructureCreator implements IStructureCreator2 {

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.IStructureCreator#getStructure(java.lang.Object)
	 */
	public IStructureComparator getStructure(Object input) {
		String contents= null;
		IDocument doc= CompareUI.getDocument(input);
		if (doc == null) {
			if (input instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) input;
				try {
					contents= Utilities.readString(sca);
				} catch (CoreException e) {
					// return null indicates the error.
					CompareUIPlugin.log(e);
					return null;
				}
			}
			
			if (contents == null) {
				// Node has no contents
				return null;
			}
			
			doc= new Document(contents);
			setupDocument(doc);
		}
		
		try {
			return createStructureComparator(input, doc, null, null);
		} catch (CoreException e) {
			CompareUIPlugin.log(e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.IStructureCreator2#createStructure(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStructureComparator createStructure(final Object element, final IProgressMonitor monitor) throws CoreException {
		final IStructureComparator[] result = new IStructureComparator[] { null };
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					result[0]= internalCreateStructure(element, monitor);
				} catch (OperationCanceledException ex) {
					return;
				}
			}
		};
		Utilities.runInUIThread(runnable);
		return result[0];
	}

	/*
	 * We need to create the structure in the UI thread since IDocument requires this
	 */
	private IStructureComparator internalCreateStructure(Object element,
			IProgressMonitor monitor) {
		final ISharedDocumentAdapter sda = SharedDocumentAdapterWrapper.getAdapter(element);
		if (sda != null) {
			final IEditorInput input = sda.getDocumentKey(element);
			if (input != null) {
				final IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(input);
				if (provider != null) {
					try {
						sda.connect(provider, input);
						IDocument document = provider.getDocument(input);
						setupDocument(document);
						return createStructureComparator(element, document, wrapSharedDocumentAdapter(sda, element, document), monitor);
					} catch (CoreException e) {
						// Connection to the document provider failed.
						// Log and fall through to use simple structure
						CompareUIPlugin.log(e);
					}
				}
			}
		}
		return getStructure(element);
	}
	
	/**
	 * Create an {@link IStructureComparator} for the given element using the
	 * contents available in the given document. If the provided
	 * {@link ISharedDocumentAdapter} is not <code>null</code> then the
	 * {@link IStructureComparator} returned by this method must implement the
	 * {@link IDisposable} interface and disconnect from the adapter when the
	 * comparator is disposed. The {@link StructureDiffViewer} class will call
	 * dispose if the {@link IStructureComparator} also implements
	 * {@link IDisposable}. Other clients must do the same.
	 * <p>
	 * It should be noted that the provided {@link ISharedDocumentAdapter}
	 * will provide the key associated with the given element when
	 * {@link ISharedDocumentAdapter#getDocumentKey(Object)} is called
	 * for any {@link IDocumentRange} node whose document matches the
	 * provided document. Thus, this adapter should also be returned
	 * by the structure comparator and its children when they are adapted
	 * to an {@link ISharedDocumentAdapter}.
	 * @param element the element
	 * @param document the document that has the contents for the element
	 * @param sharedDocumentAdapter the shared document adapter from which the
	 *            document was obtained or <code>null</code> if the document
	 *            is not shared.
	 * @param monitor a progress monitor or <code>null</code> if progress is not required
	 * 
	 * @return a structure comparator
	 * @throws CoreException
	 */
	protected abstract IStructureComparator createStructureComparator(
			final Object element, IDocument document,
			final ISharedDocumentAdapter sharedDocumentAdapter,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Setup the newly created document as appropriate. Any document partitioners
	 * should be added to a custom slot using the {@link IDocumentExtension3} interface
	 * in case the document is shared via a file buffer.
	 * @param document a document
	 */
	protected void setupDocument(IDocument document) {
		String partitioning = getDocumentPartitioning();
		if (partitioning == null || !(document instanceof IDocumentExtension3)) {
			if (document.getDocumentPartitioner() == null) {
				IDocumentPartitioner partitioner= getDocumentPartitioner();
				if (partitioner != null) {
					document.setDocumentPartitioner(partitioner);
					partitioner.connect(document);
				}
			}
		} else {
			IDocumentExtension3 ex3 = (IDocumentExtension3) document;
			if (ex3.getDocumentPartitioner(partitioning) == null) {
				IDocumentPartitioner partitioner= getDocumentPartitioner();
				if (partitioner != null) {
					ex3.setDocumentPartitioner(partitioning, partitioner);
					partitioner.connect(document);
				}
			}
		}
	}
	
	/**
	 * Return the partitioner to be associated with the document or
	 * <code>null</code> is partitioning is not needed or if the subclass
	 * overrode {@link #setupDocument(IDocument)} directly.
	 * @return a partitioner
	 */
	protected IDocumentPartitioner getDocumentPartitioner() {
		return null;
	}

	/**
	 * Return the partitioning to which the partitioner returned from
	 * {@link #getDocumentPartitioner()} is to be associated. Return <code>null</code>
	 * only if partitioning is not needed or if the subclass
	 * overrode {@link #setupDocument(IDocument)} directly.
	 * @see IDocumentExtension3
	 * @return a partitioning
	 */
	protected String getDocumentPartitioning() {
		return null;
	}
	
	/**
	 * Default implementation of save that extracts the contents from
	 * the document of an {@link IDocumentRange} and sets it on the
	 * input. If the input is an {@link IEncodedStreamContentAccessor},
	 * the charset of the input is used to extract the contents from the
	 * document. If the input adapts to {@link ISharedDocumentAdapter} and
	 * the document of the {@link IDocumentRange} matches that of the
	 * input, then the save is issued through the shared document adapter.
	 * @see org.eclipse.compare.structuremergeviewer.IStructureCreator#save(org.eclipse.compare.structuremergeviewer.IStructureComparator, java.lang.Object)
	 */
	public void save(IStructureComparator node, Object input) {
		if (node instanceof IDocumentRange && input instanceof IEditableContent) {
			IDocument document= ((IDocumentRange)node).getDocument();
			// First check to see if we have a shared document
			final ISharedDocumentAdapter sda = SharedDocumentAdapterWrapper.getAdapter(input);
			if (sda != null) {
				IEditorInput key = sda.getDocumentKey(input);
				if (key != null) {
					IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(key);
					if (provider != null) {
						IDocument providerDoc = provider.getDocument(key);
						// We have to make sure that the document we are saving is the same as the shared document
						if (providerDoc != null && providerDoc == document) {
							if (save(provider, document, input, sda, key))
								return;
						}
					}
				}
			}
			IEditableContent bca= (IEditableContent) input;
			String contents= document.get();
			String encoding= null;
			if (input instanceof IEncodedStreamContentAccessor) {
				try {
					encoding= ((IEncodedStreamContentAccessor)input).getCharset();
				} catch (CoreException e1) {
					// ignore
				}
			}
			if (encoding == null)
				encoding= ResourcesPlugin.getEncoding();
			byte[] bytes;
			try {
				bytes= contents.getBytes(encoding);
			} catch (UnsupportedEncodingException e) {
				bytes= contents.getBytes();
			}
			bca.setContent(bytes);
		}
	}

	private boolean save(final IDocumentProvider provider, final IDocument document,
			final Object input, final ISharedDocumentAdapter sda, final IEditorInput key) {
		try {
			sda.flushDocument(provider, key, document, false);
			return true;
		} catch (CoreException e) {
			CompareUIPlugin.log(e);
		}
		return false;
	}
	
	/**
	 * Create an {@link ISharedDocumentAdapter} that will provide the document key for the given input
	 * object for any {@link DocumentRangeNode} instances whose document is the same as the
	 * provided document.
	 * @param input the input element
	 * @param document the document associated with the input element
	 * @return a shared document adapter that provides the proper document key for document range nodes
	 */
	private final ISharedDocumentAdapter wrapSharedDocumentAdapter(ISharedDocumentAdapter elementAdapter, final Object input, final IDocument document) {
		// We need to wrap the adapter so that the proper document key gets returned
		return new SharedDocumentAdapterWrapper(elementAdapter) {
			public IEditorInput getDocumentKey(Object element) {
				if (hasSameDocument(element)) {
					return super.getDocumentKey(input);
				}
				return super.getDocumentKey(element);
			}
			private boolean hasSameDocument(Object element) {
				if (element instanceof DocumentRangeNode) {
					DocumentRangeNode drn = (DocumentRangeNode) element;
					return drn.getDocument() == document;
				}
				return false;
			}
		};
	}
	
	/**
	 * Default implementation of {@link #createElement(Object, Object, IProgressMonitor)}
	 * that uses {@link #getPath(Object, Object)} to determine the
	 * path for the element, {@link #createStructure(Object, IProgressMonitor)} to create the structure
	 * and {@link #findElement(IStructureComparator, String[])} to find the
	 * element in the structure. Subclasses may override.
	 * @param element the element
	 * @param input the containing input
	 * @param monitor a progress monitor
	 * @return the sub-structure element in the input for the given element
	 * @throws CoreException if a parse error occurred
	 */
	public ITypedElement createElement(Object element, Object input, IProgressMonitor monitor)
			throws CoreException {
		String[] path= getPath(element, input);
		if (path == null) {
			// TODO: Temporary code until subclasses are updated
			IStructureComparator locate = locate(element, input);
			if (locate instanceof ITypedElement) {
				return (ITypedElement)locate;
			}
			return null;
		}

		// Build the structure
		IStructureComparator structure= createStructure(input, monitor);
		if (structure == null)	// we couldn't parse the structure
			return null;		// so we can't find anything
			
		// find the path in the tree
		return findElement(structure, path);
	}
	
	/**
	 * Default implementation of {@link #locate(Object, Object)} that
	 * uses {@link #getPath(Object, Object)} to determine the
	 * path for the element, {@link #getStructure(Object)} to create the structure
	 * and {@link #findElement(IStructureComparator, String[])} to find the
	 * element in the structure.  Subclasses may override.
	 * @param element the element
	 * @param input the containing input
	 * @return the sub-structure element in the input for the given element
	 */
	public IStructureComparator locate(Object element, Object input) {
		String[] path= getPath(element, input);
		if (path == null)
			return null;
		// Build the structure
		IStructureComparator structure= getStructure(input);
		if (structure == null)	// we couldn't parse the structure
			return null;		// so we can't find anything
			
		// find the path in the tree
		return (IStructureComparator)findElement(structure, path);
	}
	
	/**
	 * Find the element at the given path in the given structure.
	 * This method is invoked from the {@link #createElement(Object, Object, IProgressMonitor)}
	 * and {@link #locate(Object, Object)} methods to find the element for
	 * the given path.
	 * @param structure the structure
	 * @param path the path of an element in the structure
	 * @return the element at the given path in the structure or <code>null</code>
	 */
	protected ITypedElement findElement(IStructureComparator structure, String[] path) {
		return (ITypedElement)find(structure, path, 0);
	}

	/**
	 * Recursively extracts the given path from the tree.
	 */
	private IStructureComparator find(IStructureComparator tree, String[] path, int index) {
		if (tree != null) {
			Object[] children= tree.getChildren();
			if (children != null) {
				for (int i= 0; i < children.length; i++) {
					IStructureComparator child= (IStructureComparator) children[i];
					if (child instanceof ITypedElement && child instanceof DocumentRangeNode) {
						String n1= null;
						if (child instanceof DocumentRangeNode)
							n1= ((DocumentRangeNode)child).getId();
						if (n1 == null)
							n1= ((ITypedElement)child).getName();
						String n2= path[index];
						if (n1.equals(n2)) {
							if (index == path.length-1)
								return child;
							IStructureComparator result= find(child, path, index+1);
							if (result != null)
								return result;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Return the path of the element in the structure of it's containing input
	 * or <code>null</code> if the element is not contained in the input. This method is
	 * invoked from {@link #createElement(Object, Object, IProgressMonitor)} and
	 * {@link #locate(Object, Object)} methods to determine
	 * the path to be passed to {@link #findElement(IStructureComparator, String[])}.
	 * By default, <code>null</code> is returned. Subclasses may override.
	 * @param element the element
	 * @param input the input
	 * @return the path of the element in the structure of it's containing input
	 * or <code>null</code>
	 */
	protected String[] getPath(Object element, Object input) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.IStructureCreator2#destroy(java.lang.Object)
	 */
	public void destroy(Object object) {
		IDisposable disposable = getDisposable(object);
		if (disposable != null)
			disposable.dispose();
	}

	private IDisposable getDisposable(Object object) {
		if (object instanceof IDisposable) {
			return (IDisposable) object;
		}
		if (object instanceof DocumentRangeNode) {
			DocumentRangeNode node = (DocumentRangeNode) object;
			return getDisposable(node.getParentNode());
		}
		return null;
	}

	/**
	 * Returns true if the two nodes are equal for comparison purposes. If
	 * <code>compareFilters</code> is not empty, the filters are applied to each
	 * line of each node's text representation.
	 * 
	 * @param node1
	 * @param contributor1
	 *            either 'A', 'L', or 'R' for ancestor, left or right
	 *            contributor
	 * @param node2
	 * @param contributor2
	 *            either 'A', 'L', or 'R' for ancestor, left or right
	 *            contributor
	 * @param ignoreWhitespace
	 *            if <code>true</code> whitespace characters will be ignored
	 *            when determining equality. Note: Will bypass any custom ignore
	 *            whitespace behaviors contributed through implementations of
	 *            <code>org.eclipse.compare.structuremergeviewer.IStructureCreator.getContents()</code>
	 * @param compareFilters
	 *            the filters used to customize the comparison of lines of text.
	 * @return whether the two nodes are equal for comparison purposes
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 3.6
	 */
	public boolean contentsEquals(Object node1, char contributor1,
			Object node2, char contributor2, boolean ignoreWhitespace,
			ICompareFilter[] compareFilters) {

		List lines1 = LineReader.readLines(new BufferedReader(new StringReader(
				getContents(node1, false))));
		List lines2 = LineReader.readLines(new BufferedReader(new StringReader(
				getContents(node2, false))));

		StringBuffer buffer1 = new StringBuffer();
		StringBuffer buffer2 = new StringBuffer();

		int maxLines = Math.max(lines1.size(), lines2.size());
		for (int i = 0; i < maxLines; i++) {
			String s1 = lines1.size() > i ? (String) lines1.get(i) : ""; //$NON-NLS-1$
			String s2 = lines2.size() > i ? (String) lines2.get(i) : ""; //$NON-NLS-1$

			if (compareFilters != null && compareFilters.length > 0) {
				s1 = Utilities.applyCompareFilters(s1, contributor1, s2,
						contributor2, compareFilters);
				s2 = Utilities.applyCompareFilters(s2, contributor2, s1,
						contributor1, compareFilters);
			}
			buffer1.append(s1);
			buffer2.append(s2);
		}
		if (ignoreWhitespace) {
			int l1 = buffer1.length();
			int l2 = buffer2.length();
			int c1 = 0, c2 = 0;
			int i1 = 0, i2 = 0;

			while (c1 != -1) {

				c1 = -1;
				while (i1 < l1) {
					char c = buffer1.charAt(i1++);
					if (!Character.isWhitespace(c)) {
						c1 = c;
						break;
					}
				}

				c2 = -1;
				while (i2 < l2) {
					char c = buffer2.charAt(i2++);
					if (!Character.isWhitespace(c)) {
						c2 = c;
						break;
					}
				}

				if (c1 != c2)
					return false;
			}
		} else if (!buffer1.toString().equals(buffer2.toString())) {
			return false;
		}

		return true;
	}
}
