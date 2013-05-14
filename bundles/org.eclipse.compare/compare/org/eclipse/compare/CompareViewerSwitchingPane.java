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

import org.eclipse.compare.contentmergeviewer.IFlushable;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.IFlushable2;
import org.eclipse.compare.internal.NullViewer;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.MessageFormat;


/**
 * A custom <code>CompareViewerPane</code> that supports dynamic viewer switching.
 * 
 * <p>
 * Clients must implement the viewer switching strategy by implementing
 * the <code>getViewer(Viewer, Object)</code> method.
 * <p>
 * If a property with the name <code>CompareUI.COMPARE_VIEWER_TITLE</code> is set
 * on the top level SWT control of a viewer, it is used as a title in the <code>CompareViewerPane</code>'s
 * title bar.
 * 
 * @since 2.0
 */
public abstract class CompareViewerSwitchingPane extends CompareViewerPane {
	
	private Viewer fViewer;
	private boolean fControlVisibility= false;
	private String fTitle;
	private String fTitleArgument;
	
	/**
	 * Creates a <code>CompareViewerSwitchingPane</code> as a child of the given parent and with the
	 * specified SWT style bits.
	 *
	 * @param parent a widget which will be the parent of the new instance (cannot be null)
	 * @param style the style of widget to construct
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 * </ul>
	 * @exception org.eclipse.swt.SWTException <ul>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
	 * </ul>
	 */		
	public CompareViewerSwitchingPane(Composite parent, int style) {
		this(parent, style, false);
	}
	
	/**
	 * Creates a <code>CompareViewerSwitchingPane</code> as a child of the given parent and with the
	 * specified SWT style bits.
	 *
	 * @param parent a widget which will be the parent of the new instance (cannot be null)
	 * @param style the style of widget to construct
	 * @param visibility the initial visibility of the CompareViewerSwitchingPane
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 * </ul>
	 * @exception org.eclipse.swt.SWTException <ul>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
	 * </ul>
	 */		
	public CompareViewerSwitchingPane(Composite parent, int style, boolean visibility) {
		super(parent, style);

		fControlVisibility= visibility;
		
		setViewer(new NullViewer(this));
		
		addDisposeListener(
			new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (fViewer != null)
						fViewer.removeSelectionChangedListener(CompareViewerSwitchingPane.this);
					if (fViewer instanceof StructuredViewer) {
						StructuredViewer sv= (StructuredViewer) fViewer;
						sv.removeDoubleClickListener(CompareViewerSwitchingPane.this);
						sv.removeOpenListener(CompareViewerSwitchingPane.this);
					}
					fViewer= null;
				}
			}
		);
	}
	
	/**
	 * Returns the current viewer.
	 * 
	 * @return the current viewer
	 */
	public Viewer getViewer() {
		return fViewer;
	}
	
	private void setViewer(Viewer newViewer) {
		
		if (newViewer == fViewer)
			return;
				
		boolean oldEmpty= isEmpty();

		if (fViewer != null) {
			
			fViewer.removeSelectionChangedListener(this);
				 
			if (fViewer instanceof StructuredViewer) {
				StructuredViewer sv= (StructuredViewer) fViewer;
				sv.removeDoubleClickListener(this);
				sv.removeOpenListener(this);
			}

			Control content= getContent();
			setContent(null);
			
			fViewer.setInput(null);
								
			if (content != null && !content.isDisposed())
				content.dispose();

		} else {
			oldEmpty= false;
		}

		setContent(null);

		fViewer= newViewer;

		if (fViewer != null) {
			// we have to remember and restore the old visibility of the CustomPane
			// since setContent changes the visibility
			boolean old= getVisible();
			setContent(fViewer.getControl());
			setVisible(old);	// restore old visibility

			boolean newEmpty= isEmpty();

			fViewer.addSelectionChangedListener(this);

			if (fViewer instanceof StructuredViewer) {
				StructuredViewer sv= (StructuredViewer) fViewer;
				sv.addDoubleClickListener(this);
				sv.addOpenListener(this);
			}
			
			if (oldEmpty != newEmpty) {	// re-layout my container
				Composite parent= getParent();
				if (parent instanceof Splitter)
					((Splitter)parent).setVisible(this, fControlVisibility ? !newEmpty : true);
			}
				
			layout(true);
		}
	}

	/**
	 * Returns the optional title argument that has been set with
	 * <code>setTitelArgument</code> or <code>null</code> if no optional title
	 * argument has been set.
	 * 
	 * @return the optional title argument or <code>null</code>
	 * @noreference This method is for internal use only. Clients should not
	 *              call this method.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public String getTitleArgument() {
		return fTitleArgument;
	}

	/**
	 * Returns <code>true</code> if no viewer is installed or if the current viewer
	 * is a <code>NullViewer</code>.
	 * 
	 * @return <code>true</code> if no viewer is installed or if the current viewer is a <code>NullViewer</code>
	 */
	public boolean isEmpty() {
		return fViewer == null || fViewer instanceof NullViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareViewerPane#getSelection()
	 */
	public ISelection getSelection() {
		if (fViewer != null)
			return fViewer.getSelection();
		return super.getSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareViewerPane#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	public void setSelection(ISelection s) {
		if (fViewer != null)
			 fViewer.setSelection(s);
	}
	
	private boolean hasFocus2() {
		// do we have focus?
		Display display= getDisplay();
		if (display != null)
			for (Control focus= display.getFocusControl(); focus != null; focus= focus.getParent())
				if (focus == this)
					return true;
		return false;
	}
	
	/**
	 * @param input the input
	 * @return true, if the input is considered as changed
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	protected boolean inputChanged(Object input) {
		return getInput() != input;
	}
		
	/**
	 * Sets the input object of this pane. 
	 * For this input object a suitable viewer is determined by calling the abstract
	 * method <code>getViewer(Viewer, Object)</code>.
	 * If the returned viewer differs from the current one, the old viewer
	 * is disposed and the new one installed. Then the input object is fed
	 * into the newly installed viewer by calling its <code>setInput(Object)</code> method.
	 * If new and old viewer don't differ no new viewer is installed but just
	 * <code>setInput(Object)</code> is called.
	 * If the input is <code>null</code> the pane is cleared,
	 * that is the current viewer is disposed.
	 * 
	 * @param input the new input object or <code>null</code>
	 */ 
	public void setInput(Object input) {

		if (!inputChanged(input))
			return;

		boolean hadFocus = hasFocus2();
		
		super.setInput(input);

		// viewer switching
		Viewer newViewer= null;
		if (input != null)
			newViewer= getViewer(fViewer, input);

		if (newViewer == null) {
			if (fViewer instanceof NullViewer)
				return;
			newViewer= new NullViewer(this);
		}
		
		setViewer(newViewer);

		// set input
		fViewer.setInput(input);

		if (getViewer() == null || !Utilities.okToUse(getViewer().getControl()))
			return;

		Image image= null;
		if (!(fViewer instanceof NullViewer) && input instanceof ICompareInput)
			image= ((ICompareInput)input).getImage();
		setImage(image);
		
		String title= null;	
		if (fViewer != null) {
			Control c= fViewer.getControl();
			if (c != null) {
				Object data= c.getData(CompareUI.COMPARE_VIEWER_TITLE);
				if (data instanceof String)
					title= (String) data;
				if (hadFocus)
					c.setFocus();
			}	
		}
			
		fTitle= title;
		updateTitle();
	}
	
	/**
	 * Sets an additional and optional argument for the pane's title.
	 * 
	 * @param argument
	 *            an optional argument for the pane's title
	 * @noreference This method is for internal use only. Clients should not
	 *              call this method.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public void setTitleArgument(String argument) {
		fTitleArgument= argument;
		updateTitle();
	}

	private void updateTitle() {
		if (fTitle != null) {
			if (fTitleArgument != null) {
				String format= CompareMessages.CompareViewerSwitchingPane_Titleformat;	
				String t= MessageFormat.format(format, new String[] { fTitle, fTitleArgument } );
				setText(t);
			} else
				setText(fTitle);			
		} else {
			setText("");	//$NON-NLS-1$
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @since 3.3
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == INavigatable.class) {
			if (isEmpty())
				return null;
			Viewer viewer= getViewer();
			if (viewer == null)
				return null;
			Control control= viewer.getControl();
			if (control == null)
				return null;
			Object data= control.getData(INavigatable.NAVIGATOR_PROPERTY);
			if (data instanceof INavigatable)
				return data;
		}
		if (adapter == IFlushable.class) {
			Viewer v= getViewer();
			if (v != null) {
				IFlushable flushable = (IFlushable)Utilities.getAdapter(v, IFlushable.class);
				if (flushable != null)
					return flushable;
			}
		}
		if (adapter == IFlushable2.class) {
			Viewer v= getViewer();
			if (v != null) {
				IFlushable2 flushable = (IFlushable2)Utilities.getAdapter(v, IFlushable2.class);
				if (flushable != null)
					return flushable;
			}
		}
		return super.getAdapter(adapter);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Composite#setFocus()
	 */
	public boolean setFocus() {
		Viewer v= getViewer();
		if (v != null) {
			Control c= v.getControl();
			if (c != null) {
				if (c.setFocus())
					return true;
			}
		}
		return super.setFocus();
	}

	/**
	 * Returns a viewer which is able to display the given input.
	 * If no viewer can be found, <code>null</code> is returned.
	 * The additional argument oldViewer represents the viewer currently installed
	 * in the pane (or <code>null</code> if no viewer is installed).
	 * It can be returned from this method if the current viewer can deal with the
	 * input (and no new viewer must be created).
	 *
	 * @param oldViewer the currently installed viewer or <code>null</code>
	 * @param input the input object for which a viewer must be determined or <code>null</code>
	 * @return a viewer for the given input, or <code>null</code> if no viewer can be determined
	 */
	abstract protected Viewer getViewer(Viewer oldViewer, Object input);
}
