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
package org.eclipse.compare.internal;

import java.io.*;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import java.util.ArrayList;
import com.ibm.icu.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.compare.*;


public class AddFromHistoryDialog extends ResizableDialog {
	
	static class HistoryInput implements ITypedElement, IEncodedStreamContentAccessor, IModificationDate {
		IFile fFile;
		IFileState fFileState;
		
		HistoryInput(IFile file, IFileState fileState) {
			fFile= file;
			fFileState= fileState;
		}
		public InputStream getContents() throws CoreException {
			return new BufferedInputStream(fFileState.getContents());
		}
		public String getCharset() {
			String charset= null;
			try {
				charset= fFileState.getCharset();
			} catch (CoreException e) {
				// fall through
			}
			if (charset == null)
				charset= Utilities.getCharset(fFile);
			return charset;
		}
		public String getName() {
			return fFile.getName();
		}
		public String getType() {
			return fFile.getFileExtension();
		}
		public Image getImage() {
			return CompareUI.getImage(fFile);
		}
		public long getModificationDate() {
			return fFileState.getModificationTime();
		}
	}
	
	static class FileHistory {
		private IFile fFile;
		private IFileState[] fStates;
		private int fSelected;
		
		FileHistory(IFile file) {
			fFile= file;
		}
		
		IFile getFile() {
			return fFile;
		}
		
		IFileState[] getStates() {
			if (fStates == null) {
				try {
					fStates= fFile.getHistory(new NullProgressMonitor());
				} catch (CoreException ex) {
					// NeedWork
				}
			}
			return fStates;
		}
		
		IFileState getSelectedState() {
			return getStates()[fSelected];
		}
		
		void setSelected(IFileState state) {
			for (int i= 0; i < fStates.length; i++) {
				if (fStates[i] == state) {
					fSelected= i;
					return;
				}
			}
		}
		
		HistoryInput getHistoryInput() {
			return new HistoryInput(fFile, getSelectedState());
		}
		
		boolean isSelected(int index) {
			return index == fSelected;
		}
	}

	private CompareConfiguration fCompareConfiguration;
	private ArrayList fArrayList= new ArrayList();
	private FileHistory fCurrentFileHistory;

	// SWT controls
	private CompareViewerSwitchingPane fContentPane;
	private Button fCommitButton;
	private Table fMemberTable;
	private CompareViewerPane fMemberPane;
	private Tree fEditionTree;
	private CompareViewerPane fEditionPane;
	private Image fDateImage;
	private Image fTimeImage;


	public AddFromHistoryDialog(Shell parent, ResourceBundle bundle) {
		super(parent, bundle);
					
		String iconName= Utilities.getString(fBundle, "dateIcon", "obj16/day_obj.gif"); //$NON-NLS-2$ //$NON-NLS-1$
		ImageDescriptor id= CompareUIPlugin.getImageDescriptor(iconName);
		if (id != null)
			fDateImage= id.createImage();
		iconName= Utilities.getString(fBundle, "timeIcon", "obj16/resource_obj.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		id= CompareUIPlugin.getImageDescriptor(iconName);
		if (id != null)
			fTimeImage= id.createImage();
	}
	
	public boolean select(IContainer root, IFile[] inputFiles) {
		
		create();	// create widgets
		
		String format= Utilities.getString(fBundle, "memberPaneTitle");	//$NON-NLS-1$
		String title= MessageFormat.format(format, new Object[] { root.getName() });
		fMemberPane.setImage(CompareUI.getImage(root));
		fMemberPane.setText(title);
		
		// sort input files
		final int count= inputFiles.length;
		final IFile[] files= new IFile[count];
		for (int i= 0; i < count; i++)
			files[i]= inputFiles[i];
		if (count > 1)
			internalSort(files, 0, count-1);
			
		
		String prefix= root.getFullPath().toString();
		
		if (fMemberTable != null && !fMemberTable.isDisposed()) {
			for (int i = 0; i < files.length; i++) {
				IFile file = files[i];
				String path = file.getFullPath().toString();
				
				// ignore a recently deleted file at the same path as the
				// container
				if (path.equals(prefix))
					continue;
				
				if (path.startsWith(prefix))
					path = path.substring(prefix.length() + 1);
				TableItem ti = new TableItem(fMemberTable, SWT.NONE);
				ti.setImage(CompareUI.getImage(file));
				ti.setText(path);
				ti.setData(new FileHistory(file));
			}
		}
		
		open();
		
		return (getReturnCode() == OK) && (fArrayList.size() > 0);
	}
		
	HistoryInput[] getSelected() {
		HistoryInput[] selected= new HistoryInput[fArrayList.size()];
		Iterator iter= fArrayList.iterator();
		for (int i= 0; iter.hasNext(); i++) {
			FileHistory h= (FileHistory) iter.next();
			selected[i]= h.getHistoryInput();
		}
		return selected;
	}
				
	protected synchronized Control createDialogArea(Composite parent2) {
		
		Composite parent= (Composite) super.createDialogArea(parent2);

		getShell().setText(Utilities.getString(fBundle, "title")); //$NON-NLS-1$
		
		org.eclipse.compare.Splitter vsplitter= new org.eclipse.compare.Splitter(parent,  SWT.VERTICAL);
		vsplitter.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
					| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));

		vsplitter.addDisposeListener(
			new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (fDateImage != null)
						fDateImage.dispose();
					if (fTimeImage != null)
						fTimeImage.dispose();
				}
			}
		);
		
		// we need two panes: the left for the elements, the right one for the editions
		Splitter hsplitter= new Splitter(vsplitter,  SWT.HORIZONTAL);
		
		Composite c= new Composite(hsplitter, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 2;
		layout.verticalSpacing= 2;
		layout.numColumns= 1;
		c.setLayout(layout);
		Label l1= new Label(c, SWT.NONE);
		l1.setText(Utilities.getString(fBundle, "memberDescription"));	//$NON-NLS-1$
		fMemberPane= new CompareViewerPane(c, SWT.BORDER | SWT.FLAT);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		fMemberPane.setLayoutData(gd);

		fMemberTable= new Table(fMemberPane, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
		fMemberTable.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.detail == SWT.CHECK) {
						if (e.item instanceof TableItem) {
							TableItem ti= (TableItem) e.item;
							if (ti.getChecked())
								fArrayList.add(ti.getData());
							else
								fArrayList.remove(ti.getData());
								
							if (fCommitButton != null)
								fCommitButton.setEnabled(fArrayList.size() > 0);
						}
					} else {
						handleMemberSelect(e.item);
					}
				}
			}
		);
				
		fMemberPane.setContent(fMemberTable);
		
		c= new Composite(hsplitter, SWT.NONE);
		layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 2;
		layout.verticalSpacing= 2;
		layout.numColumns= 1;
		c.setLayout(layout);
		Label l2= new Label(c, SWT.NONE);
		l2.setText(Utilities.getString(fBundle, "editionDescription"));	//$NON-NLS-1$
		fEditionPane= new CompareViewerPane(c, SWT.BORDER | SWT.FLAT);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		fEditionPane.setLayoutData(gd);
		
		fEditionTree= new Tree(fEditionPane, SWT.H_SCROLL | SWT.V_SCROLL);
		fEditionTree.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					feedContent(e.item);
				}
			}
		);
		fEditionPane.setContent(fEditionTree);		
		
		applyDialogFont(parent); // to avoid applying font to compare viewer
		fContentPane= new CompareViewerSwitchingPane(vsplitter, SWT.BORDER | SWT.FLAT) {
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				return CompareUI.findContentViewer(oldViewer, input, this, fCompareConfiguration);	
			}
		};
		vsplitter.setWeights(new int[] { 30, 70 });
		
		return parent;
	}
	
	/*
	 * Feeds selection from member viewer to edition viewer.
	 */
	private void handleMemberSelect(Widget w) {
		Object data= null;
		if (w != null)
			data= w.getData();
		if (data instanceof FileHistory) {
			
			FileHistory h= (FileHistory) data;
			fCurrentFileHistory= h;
			
			IFile file= h.getFile();
			IFileState[] states= h.getStates();
			
			fEditionPane.setImage(CompareUI.getImage(file));
			String pattern= Utilities.getString(fBundle, "treeTitleFormat"); //$NON-NLS-1$
			String title= MessageFormat.format(pattern, new Object[] { file.getName() });
			fEditionPane.setText(title);
			
			if (fEditionTree != null) {
				fEditionTree.setRedraw(false);
				fEditionTree.removeAll();
				for (int i= 0; i < states.length; i++) {
					addEdition(new HistoryInput(file, states[i]), h.isSelected(i));
				}
				fEditionTree.setRedraw(true);
			}
		} else
			fCurrentFileHistory= null;
	}
	
	/*
	 * Adds the given Pair to the edition tree.
	 * It takes care of creating tree nodes for different dates.
	 */
	private void addEdition(HistoryInput input, boolean isSelected) {
		if (fEditionTree == null || fEditionTree.isDisposed())
			return;
		
		IFileState state= input.fFileState;
		
		// find last day
		TreeItem[] days= fEditionTree.getItems();
		TreeItem lastDay= null;
		if (days.length > 0)
			lastDay= days[days.length-1];
						
		long ldate= state.getModificationTime();		
		long day= dayNumber(ldate);
		Date date= new Date(ldate);
		if (lastDay == null || day != dayNumber(((Date)lastDay.getData()).getTime())) {
			lastDay= new TreeItem(fEditionTree, SWT.NONE);
			lastDay.setImage(fDateImage);
			String df= DateFormat.getDateInstance().format(date);
			long today= dayNumber(System.currentTimeMillis());
			
			String formatKey;
			if (day == today)
				formatKey= "todayFormat"; //$NON-NLS-1$
			else if (day == today-1)
				formatKey= "yesterdayFormat"; //$NON-NLS-1$
			else
				formatKey= "dayFormat"; //$NON-NLS-1$
			String pattern= Utilities.getString(fBundle, formatKey);
			if (pattern != null)
				df= MessageFormat.format(pattern, new String[] { df });
			lastDay.setText(df);
			lastDay.setData(date);
		}
		TreeItem ti= new TreeItem(lastDay, SWT.NONE);
		ti.setImage(fTimeImage);
		ti.setText(DateFormat.getTimeInstance().format(date));
		ti.setData(input);

		if (isSelected) {
			lastDay.setExpanded(true);
			fEditionTree.setSelection(new TreeItem[] { ti });
			feedContent(ti);
		}
	}
						
	/*
	 * Returns the number of s since Jan 1st, 1970.
	 * The given date is converted to GMT and daylight saving is taken into account too.
	 */
	private long dayNumber(long date) {
		int ONE_DAY_MS= 24*60*60 * 1000; // one day in milli seconds
		
		Calendar calendar= Calendar.getInstance();
		long localTimeOffset= calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
		
		return (date + localTimeOffset) / ONE_DAY_MS;
	}
	
	/*
	 * Feeds the tree viewer's selection to the contentviewer
	 */
	private void feedContent(Widget w) {
		if (fContentPane != null && !fContentPane.isDisposed()) {
			Object o= w.getData();
			if (o instanceof HistoryInput) {
				HistoryInput selected= (HistoryInput) o;
				fContentPane.setInput(selected);
				fContentPane.setText(getEditionLabel(selected));
				fContentPane.setImage(fTimeImage);
				
				if (fCurrentFileHistory != null)
					fCurrentFileHistory.setSelected(selected.fFileState);
			} else {
				fContentPane.setInput(null);
			}
		}
	}
	
	protected String getEditionLabel(HistoryInput input) {
		String format= Utilities.getString(fBundle, "historyEditionLabel", null);	//$NON-NLS-1$
		if (format == null)
			format= Utilities.getString(fBundle, "editionLabel");	//$NON-NLS-1$
		if (format == null)
			format= "x{0}";	//$NON-NLS-1$
		
		long modDate= input.getModificationDate();
		String date= DateFormat.getDateTimeInstance().format(new Date(modDate));
		
		return MessageFormat.format(format, new Object[] { date });
	}
			
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		String buttonLabel= Utilities.getString(fBundle, "buttonLabel", IDialogConstants.OK_LABEL); //$NON-NLS-1$
		// a 'Cancel' and a 'Add' button
		fCommitButton= createButton(parent, IDialogConstants.OK_ID, buttonLabel, true);
		fCommitButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/*
	 * Returns true if the pathname of f1 comes after f2
	 */
	private static boolean greaterThan(IFile f1, IFile f2) {
		String[] ss1= f1.getFullPath().segments();
		String[] ss2= f2.getFullPath().segments();
		int l1= ss1.length;
		int l2= ss2.length;
		int n= Math.max(l1, l2);
		
		for (int i= 0; i < n; i++) {
			String s1= i < l1 ? ss1[i] : ""; //$NON-NLS-1$
			String s2= i < l2 ? ss2[i] : ""; //$NON-NLS-1$
			int rc= s1.compareToIgnoreCase(s2);
			if (rc != 0)
				return rc < 0;
		}
		return false;
	}
	
	private static void internalSort(IFile[] keys, int left, int right) { 
	
		int original_left= left;
		int original_right= right;
		
		IFile mid= keys[(left + right) / 2]; 
		do { 
			while (greaterThan(keys[left], mid))
				left++; 
			
			while (greaterThan(mid, keys[right]))
				right--; 
		
			if (left <= right) { 
				IFile tmp= keys[left]; 
				keys[left]= keys[right]; 
				keys[right]= tmp;			
				left++; 
				right--; 
			} 
		} while (left <= right);
		
		if (original_left < right)
			internalSort(keys, original_left, right); 
		
		if (left < original_right)
			internalSort(keys, left, original_right); 
	}
}
