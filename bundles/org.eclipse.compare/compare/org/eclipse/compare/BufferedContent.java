/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.internal.ContentChangeNotifier;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.CoreException;

/**
 * Abstract implementation for a buffered <code>IStreamContentAccessor</code>.
 * <p>
 * Subclasses must implement the <code>createStream</code> method
 * to connect the buffered content with a streamable source (e.g., a file).
 * <p>
 * As long as the contents of <code>BufferedContent</code> is only retrieved as an input stream
 * (by means of <code>getContents</code>) and the <code>BufferedContent</code> is not modified (with
 * <code>setContent</code>) no buffering takes place.
 * Buffering starts when either method <code>getContent</code> or <code>setContent</code> is called.
 * 
 * @see IContentChangeNotifier
 * @see IStreamContentAccessor
 */
public abstract class BufferedContent implements IContentChangeNotifier, IStreamContentAccessor {
	
	byte[] fContent;
	private ContentChangeNotifier fChangeNotifier;
	
	/**
	 * Creates a buffered stream content accessor.
	 */
	protected BufferedContent() {
		// empty implementation
	}
		
	/* (non-Javadoc)
	 * see IStreamContentAccessor.getContents
	 */
	public InputStream getContents() throws CoreException {
		if (fContent != null)
			return new ByteArrayInputStream(fContent);
		return createStream();
	}

	/**
	 * Creates and returns a stream for reading the contents.
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 *
	 * @return the stream from which the content is read
	 * @exception CoreException if the contents could not be accessed
	 */
	protected abstract InputStream createStream() throws CoreException;
	
	/**
	 * Sets the contents. Registered content change listeners are notified.
	 *
	 * @param contents the new contents
	 */
	public void setContent(byte[] contents) {
		fContent= contents;
		fireContentChanged();
	}
	
	/**
	 * Returns the contents as an array of bytes.
	 *
	 * @return the contents as an array of bytes, or <code>null</code> if
	 *   the contents could not be accessed
	 */
	public byte[] getContent() {
		if (fContent == null) {
			try {
				InputStream is= createStream();
				fContent= Utilities.readBytes(is);
			} catch(CoreException ex) {
				// NeedWork
			}
		}
		return fContent;
	}

	/**
	 * Discards the buffered content.
	 */
	public void discardBuffer() {
		fContent= null;
	}
	
	/* (non-Javadoc)
	 * see IContentChangeNotifier.addChangeListener
	 */
	public void addContentChangeListener(IContentChangeListener listener) {
		if (fChangeNotifier == null)
			fChangeNotifier= new ContentChangeNotifier(this);
		fChangeNotifier.addContentChangeListener(listener);
	}
	
	/* (non-Javadoc)
	 * see IContentChangeNotifier.removeChangeListener
	 */
	public void removeContentChangeListener(IContentChangeListener listener) {
		if (fChangeNotifier != null) {
			fChangeNotifier.removeContentChangeListener(listener);
			if (fChangeNotifier.isEmpty())
				fChangeNotifier= null;
		}
	}
	
	/**
	 * Notifies all registered <code>IContentChangeListener</code>s of a content change.
	 */
	protected void fireContentChanged() {
		if (fChangeNotifier == null || fChangeNotifier.isEmpty()) {
			return;
		}
		fChangeNotifier.fireContentChanged();
	}
}

