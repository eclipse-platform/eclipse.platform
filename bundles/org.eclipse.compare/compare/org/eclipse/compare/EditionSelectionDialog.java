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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.compare.internal.CompareContainer;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ResizableDialog;
import org.eclipse.compare.internal.StructureCreatorDescriptor;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;


/**
 * A dialog where one input element can be compared against
 * a list of historic variants (editions) of the same input element.
 * The dialog can be used to implement functions like "Compare/Replace with Version" or
 * "Compare/Replace from Local History" on workspace resources.
 * <p>
 * In addition it is possible to specify a subsection of the input element
 * (e.g. a method in a Java source file) by means of a "path".
 * In this case the dialog compares only the subsection (as specified by the path)
 * with the corresponding subsection in the list of editions.
 * Only those editions are shown where the subsection differs from the same subsection in
 * another edition thereby minimizing the number of presented variants.
 * This functionality can be used to implement "Replace from Local History"
 * for the Java language.
 * <p>
 * Subsections of an input element are determined by first finding an
 * <code>IStructureCreator</code> for the input's type.
 * Then the method <code>locate</code> is used to extract the subsection.
 * <p>
 * Each edition (variant in the list of variants) must implement the <code>IModificationDate</code> interface
 * so that the dialog can sort the editions and present them in a tree structure where every
 * node corresponds one day.
 * <p>
 * The functionality is surfaced in a single function <code>selectEdition</code>.
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 *
 * @see IModificationDate
 * @see ITypedElement
 * 
 * @deprecated Use an <code>org.eclipse.team.ui.history.IHistoryPageSource</code> in conjunction with
 * the <code>org.eclipse.team.ui.history.IHistoryView</code> or a <code>HistoryPageCompareEditorInput</code>.
 * For sub-file elements, a <code>org.eclipse.team.ui.history.ElementLocalHistoryPageSource</code> can be used.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EditionSelectionDialog extends ResizableDialog {
		
	/**
	 * An item in an underlying edition.
	 */
	private static class Pair {
		
		private ITypedElement fEdition;
		private ITypedElement fItem;
		private String fContent;
		private IStructureCreator fStructureCreator;
		private boolean fHasError= false;
				
		Pair(IStructureCreator structureCreator, ITypedElement edition, ITypedElement item) {
			fStructureCreator= structureCreator;
			fEdition= edition;
			fItem= item;
		}
		
		Pair(IStructureCreator structureCreator, ITypedElement edition) {
			this(structureCreator, edition, edition);
		}
		
		ITypedElement getEdition() {
			return fEdition;
		}

		ITypedElement getItem() {
			return fItem;
		}
		
		/*
		 * The content is lazily loaded
		 */
		private String getContent() {
			if (fContent == null) {
				if (fStructureCreator != null)
					fContent= fStructureCreator.getContents(fItem, false);
				else {
					if (fItem instanceof IStreamContentAccessor) {
						IStreamContentAccessor sca= (IStreamContentAccessor) fItem;
						try {
							fContent= Utilities.readString(sca);
						} catch (CoreException ex) {
							// NeedWork
							CompareUIPlugin.log(ex);
						}
					}
				}
				if (fContent == null)
					fContent= ""; //$NON-NLS-1$
			}
			return fContent;
		}
		
		public boolean equals(Object other) {
			if (other != null && other.getClass() == getClass()) {
				if (getContent().equals(((Pair)other).getContent()))
					return true;
			}
			return super.equals(other);
		}

		public int hashCode() {
			return getContent().hashCode();
		}
	}
	
	// Configuration options
	private CompareConfiguration fCompareConfiguration;
	private ArrayList fArrayList= new ArrayList();
	/** use a side-by-side compare viewer */
	private boolean fCompare= true;
	/** show target on right hand side */
	private boolean fTargetIsRight= false;
	/** hide entries which have identical content */
	private boolean fHideIdentical= true;
	/** add mode if true, otherwise replace mode */
	private boolean fAddMode= false;
	/** compare mode if true, otherwise replace/add mode */
	private boolean fCompareMode= false;
	/** perform structure compare on editions */
	private boolean fStructureCompare= false;
	/** allow for multiple selection */
	private boolean fMultiSelect= false;
	
	/**
	 * Maps from members to their corresponding editions.
	 * Has only a single entry if dialog is used in "Replace" (and not "Add") mode.
	 */
	private HashMap fMemberEditions;
	/**
	 * Maps from members to their corresponding selected edition.
	 */
	private HashMap fMemberSelection;
	/** The editions of the current selected member */
	private List fCurrentEditions;
	private Thread fThread;
	private Pair fTargetPair;
	/** The selected edition in the edition viewer */
	private ITypedElement fSelectedItem;
	private String fTitleArg;
	private Image fTitleImage;
	
	// SWT controls
	private CompareViewerSwitchingPane fContentPane;
	private Button fCommitButton;
	private Table fMemberTable;
	private CompareViewerPane fMemberPane;
	private Tree fEditionTree;
	private CompareViewerPane fEditionPane;
	private Image fDateImage;
	private Image fTimeImage;
	private CompareViewerSwitchingPane fStructuredComparePane;
	private Label statusLabel;
	
	/**
	 * Creates a new modal, resizable dialog.
	 * Various titles, icons, and labels are configured from the given resource bundle.
	 * The following resource keys are used:
	 * <pre>
	 *	key         type          description
	 *	title       String        dialog title
	 *	width       Integer       initial width of dialog
	 *	height      Integer       initial height of dialog
	 *	treeTitleFormat   MessageFormat pane title for edition tree; arg 0 is the target
	 *	dateIcon    String        icon for node in edition tree; path relative to plug-in
	 *	timeIcon    String        icon for leaf in edition tree; path relative to plug-in
	 *	todayFormat MessageFormat format string if date is todays date; arg 0 is date
	 *	yesterdayFormat MessageFormat format string if date is yesterdays date; arg 0 is date
	 *	dayFormat   MessageFormat format string if date is any other date; arg 0 is date
	 *	editionLabel String       label for editions side of compare viewer; arg 0 is the date
	 *	targetLabel  String       label for target side of compare viewer 
	 *  buttonLabel  String       label for OK button; default is IDialogConstants.OK_LABEL
	 * </pre>
	 *
	 * @param parent if not <code>null</code> the new dialog stays on top of this parent shell
	 * @param bundle <code>ResourceBundle</code> to configure the dialog
	 */
	public EditionSelectionDialog(Shell parent, ResourceBundle bundle) {
		super(parent, bundle);
	}
	
	private CompareConfiguration getCompareConfiguration() {
		if (fCompareConfiguration == null) {
			fCompareConfiguration= new CompareConfiguration();
			fCompareConfiguration.setLeftEditable(false);
			fCompareConfiguration.setRightEditable(false);
			fCompareConfiguration.setContainer(new CompareContainer() {
				public void setStatusMessage(String message) {
					if (statusLabel != null && !statusLabel.isDisposed()) {
						if (message == null) {
							statusLabel.setText(""); //$NON-NLS-1$
						} else {
							statusLabel.setText(message);
						}
					}
				}
			});
		}
		return fCompareConfiguration;
	}
	
	/**
	 * Sets the help context for this dialog.
	 * 
	 * @param contextId the help context id.
	 * @since 3.2
	 */
	public void setHelpContextId(String contextId) {
		super.setHelpContextId(contextId);
	}
	
	/**
	 * Sets an additional and optional argument for the edition pane's title.
	 *  
	 * @param titleArgument an optional argument for the edition pane's title
	 * @since 2.0
	 */
	public void setEditionTitleArgument(String titleArgument) {
		fTitleArg= titleArgument;
	}
	
	/**
	 * Sets an optional image for the edition pane's title.
	 *  
	 * @param titleImage an optional image for the edition pane's title
	 * @since 2.0
	 */
	public void setEditionTitleImage(Image titleImage) {
		fTitleImage= titleImage;
	}
	
	/**
	 * Select the previous edition (presenting a UI).
	 *
	 * @param target the input object against which the editions are compared; must not be <code>null</code>
	 * @param inputEditions the list of editions (element type: <code>ITypedElement</code>s)
	 * @param ppath If <code>null</code> dialog shows full input; if non <code>null</code> it extracts a subsection
	 * @return returns the selected edition or <code>null</code> if error occurred.
	 * The returned <code>ITypedElement</code> is one of the original editions
	 * if <code>path</code> was <code>null</code>; otherwise
	 * it is an <code>ITypedElement</code> returned from <code>IStructureCreator.locate(path, item)</code>
	 * @since 2.0
	 */
	public ITypedElement selectPreviousEdition(final ITypedElement target, ITypedElement[] inputEditions, Object ppath) {
		Assert.isNotNull(target);
		fTargetPair= new Pair(null, target);
		
		// sort input editions
		final int count= inputEditions.length;
		final IModificationDate[] editions= new IModificationDate[count];
		for (int i= 0; i < count; i++)
			editions[i]= (IModificationDate) inputEditions[i];
		if (count > 1)
			internalSort(editions);
			
		// find StructureCreator if ppath is not null
		IStructureCreator structureCreator= null;
		if (ppath != null) {
			String type= target.getType();
			StructureCreatorDescriptor scd= CompareUIPlugin.getDefault().getStructureCreator(type);
			if (scd != null)
				structureCreator= scd.createStructureCreator();
		}

		if (fAddMode) {
			// does not work in add mode
			return null;
		}
			
		if (structureCreator != null) {
			Pair pair= createPair(structureCreator, ppath, target);
			if (pair != null)
				fTargetPair= pair;
			else
				ppath= null;	// couldn't extract item because of error
		}
					
		// from front (newest) to back (oldest)
		for (int i= 0; i < count; i++) {
				
			ITypedElement edition= (ITypedElement) editions[i];
			Pair pair= null;
			
			if (structureCreator != null && ppath != null) {
				// extract sub element from edition
				pair= createPair(structureCreator, ppath, edition);
			} else {
				pair= new Pair(null, edition);
			}
			
			if (pair != null && pair.fHasError)
				return null;
				
			if (pair != null && !fTargetPair.equals(pair)) {
				return pair.fItem;
			}
		}
		
		// nothing found
		return null;
	}
	
	/**
	 * Presents this modal dialog with the functionality described in the class comment above.
	 *
	 * @param target the input object against which the editions are compared; must not be <code>null</code>
	 * @param inputEditions the list of editions (element type: <code>ITypedElement</code>s)
	 * @param ppath If <code>null</code> dialog shows full input; if non <code>null</code> it extracts a subsection
	 * @return returns the selected edition or <code>null</code> if dialog was cancelled.
	 * The returned <code>ITypedElement</code> is one of the original editions
	 * if <code>path</code> was <code>null</code>; otherwise
	 * it is an <code>ITypedElement</code> returned from <code>IStructureCreator.locate(path, item)</code>
	 */
	public ITypedElement selectEdition(final ITypedElement target, ITypedElement[] inputEditions, Object ppath) {
		
		Assert.isNotNull(target);
		fTargetPair= new Pair(null, target);
		
		// sort input editions
		final int count= inputEditions.length;
		final IModificationDate[] editions= new IModificationDate[count];
		for (int i= 0; i < count; i++)
			editions[i]= (IModificationDate) inputEditions[i];
		if (count > 1)
			internalSort(editions);
			
		// find StructureCreator if ppath is not null
		IStructureCreator structureCreator= null;
		if (ppath != null) {
			String type= target.getType();
			StructureCreatorDescriptor scd= CompareUIPlugin.getDefault().getStructureCreator(type);
			if (scd != null)
				structureCreator= scd.createStructureCreator();
		}

		if (!fAddMode) {
			// replace mode
			
			if (structureCreator != null) {
				Pair pair= createPair(structureCreator, ppath, target);
				if (pair != null)
					fTargetPair= pair;
				else
					ppath= null;	// couldn't extract item because of error
			}
			
			// set the left and right labels for the compare viewer
			String targetLabel= getTargetLabel(target, fTargetPair.getItem());
			if (fTargetIsRight)
				getCompareConfiguration().setRightLabel(targetLabel);
			else
				getCompareConfiguration().setLeftLabel(targetLabel);
			
			if (structureCreator != null && ppath != null) {	// extract sub element
				
				final IStructureCreator sc= structureCreator;
				final Object path= ppath;
				
				// construct the comparer thread
				// and perform the background extract
				fThread= new Thread() {
					public void run() {
																				
						// from front (newest) to back (oldest)
						for (int i= 0; i < count; i++) {
								
							if (fEditionTree == null || fEditionTree.isDisposed())
								break;
							ITypedElement edition= (ITypedElement) editions[i];
							
							// extract sub element from edition
							Pair pair= createPair(sc, path, edition);
							if (pair != null)
								sendPair(pair);
						}
						sendPair(null);
					}
				};
			} else {
				// create tree widget
				create();
				
				// from front (newest) to back (oldest)
				for (int i= 0; i < count; i++)
					addMemberEdition(new Pair(null, (ITypedElement) editions[i]));
			}
			
		} else {
			// add mode
			final Object container= ppath;
			Assert.isNotNull(container);
								
			if (structureCreator == null)
				return null;	// error
		
			// extract all elements of container
			final HashSet current= new HashSet();
			IStructureComparator sco= structureCreator.locate(container, target);
			if (sco != null) {
				Object[] children= sco.getChildren();
				if (children != null)
					for (int i= 0; i < children.length; i++)
						current.add(children[i]);
			}
			
			final IStructureCreator sc= structureCreator;
			
			// construct the comparer thread
			// and perform the background extract
			fThread= new Thread() {
				public void run() {
					
					// from front (newest) to back (oldest)
					for (int i= 0; i < count; i++) {
							
						if (fEditionTree == null || fEditionTree.isDisposed())
							break;
						ITypedElement edition= (ITypedElement) editions[i];
						
						IStructureComparator sco2= sc.locate(container, edition);
						if (sco2 != null) {
							Object[] children= sco2.getChildren();
							if (children != null) {
								for (int i2= 0; i2 < children.length; i2++) {
									ITypedElement child= (ITypedElement) children[i2];
									if (!current.contains(child))
										sendPair(new Pair(sc, edition, child));
								}
							}
						}
					}
					sendPair(null);
				}
			};
		}
		
		open();
		
		if (getReturnCode() == OK)
			return fSelectedItem;
		return null;
	}
	
	private Pair createPair(IStructureCreator sc, Object path, ITypedElement input) {
		IStructureComparator scmp= sc.locate(path, input);
		if (scmp == null && sc.getStructure(input) == null) {	// parse error
			Pair p= new Pair(sc, input);
			p.fHasError= true;
			return p;
		}
		if (scmp instanceof ITypedElement)
			return new Pair(sc, input, (ITypedElement) scmp);
		return null;
	}

	/**
	 * Controls whether identical entries are shown or not (default).
	 * This method must be called before <code>selectEdition</code>.
	 *
	 * @param hide if true identical entries are hidden; otherwise they are shown.
	 * @since 2.0
	 */
	public void setHideIdenticalEntries(boolean hide) {
		fHideIdentical= hide;
	}

	/**
	 * Controls whether workspace target is on the left (the default) or right hand side.
	 *
	 * @param isRight if true target is shown on right hand side.
	 * @since 2.0
	 */
	public void setTargetIsRight(boolean isRight) {
		fTargetIsRight= isRight;
	}
		
	/**
	 * Controls whether the <code>EditionSelectionDialog</code> is in 'add' mode
	 * or 'replace' mode (the default).
	 *
	 * @param addMode if true dialog is in 'add' mode.
	 * @since 2.0
	 */
	public void setAddMode(boolean addMode) {
		fAddMode= addMode;
		fMultiSelect= addMode;
	}
	
	/**
	 * Controls whether the <code>EditionSelectionDialog</code> is in 'compare' mode
	 * or 'add/replace' (the default) mode. 
	 *
	 * @param compareMode if true dialog is in 'add' mode.
	 * @since 2.0
	 */
	public void setCompareMode(boolean compareMode) {
		fCompareMode= compareMode;
		fStructureCompare= fCompareMode && !fAddMode;
	}
	
	/**
	 * Returns the input target that has been specified with the most recent call
	 * to <code>selectEdition</code>. If a not <code>null</code> path was specified this method
	 * returns a subsection of this target (<code>IStructureCreator.locate(path, target)</code>)
	 * instead of the input target.
	 * <p>
	 * For example if the <code>target</code> is a Java compilation unit and <code>path</code> specifies
	 * a method, the value returned from <code>getTarget</code> will be the method not the compilation unit.
	 *
	 * @return the last specified target or a subsection thereof.
	 */
	public ITypedElement getTarget() {
		return fTargetPair.getItem();
	}
 	
	/**
	 * Returns the editions that have been selected with the most
	 * recent call to <code>selectEdition</code>.
	 * 
	 * @return the selected editions as an array.
	 * @since 2.1
	 */
	public ITypedElement[] getSelection() {
		ArrayList result= new ArrayList();
		if (fMemberSelection != null) {
			Iterator iter= fArrayList.iterator();
			while (iter.hasNext()) {
				Object edition= iter.next();		
				Object item= fMemberSelection.get(edition);
				if (item != null)
					result.add(item);
			}
		} else if (fSelectedItem != null)
			result.add(fSelectedItem);
		return (ITypedElement[]) result.toArray(new ITypedElement[result.size()]);
	}
		
 	/**
 	 * Returns a label for identifying the target side of a compare viewer.
 	 * This implementation extracts the value for the key "targetLabel" from the resource bundle
 	 * and passes it as the format argument to <code>MessageFormat.format</code>.
 	 * The single format argument for <code>MessageFormat.format</code> ("{0}" in the format string)
 	 * is the name of the given input element.
	 * <p>
	 * Subclasses may override to create their own label.
	 * </p>
 	 *
 	 * @param target the target element for which a label must be returned
 	 * @param item if a path has been specified in <code>selectEdition</code> a sub element of the given target; otherwise the same as target
 	 * @return a label the target side of a compare viewer
  	 */
	protected String getTargetLabel(ITypedElement target, ITypedElement item) {
		String format= null;
		if (target instanceof ResourceNode)
			format= Utilities.getString(fBundle, "workspaceTargetLabel", null); //$NON-NLS-1$
		if (format == null)
			format= Utilities.getString(fBundle, "targetLabel"); //$NON-NLS-1$
		if (format == null)
			format= "x{0}"; //$NON-NLS-1$
		
		return formatString(format, target.getName());
	}

	private String formatString(String string, String variable) {
		// Only process the string if it contains a variable or an escaped quote (see bug 190023)
		if (hasVariable(string) || hasDoubleQuotes(string))
			return MessageFormat.format(string, new Object[] { variable });
		return string;
	}
	
 	private boolean hasDoubleQuotes(String string) {
		return string.indexOf("''") != -1; //$NON-NLS-1$
	}

	private boolean hasVariable(String string) {
		return string.indexOf("{0}") != -1; //$NON-NLS-1$
	}

	/**
 	 * Returns a label for identifying the edition side of a compare viewer.
 	 * This implementation extracts the value for the key "editionLabel" from the resource bundle
 	 * and passes it as the format argument to <code>MessageFormat.format</code>.
 	 * The single format argument for <code>MessageFormat.format</code> ("{0}" in the format string)
 	 * is the formatted modification date of the given input element.
 	 * <p>
	 * Subclasses may override to create their own label.
	 * </p>
	 *
	 * @param selectedEdition the selected edition for which a label must be returned
 	 * @param item if a path has been specified in <code>selectEdition</code> a sub element of the given selectedEdition; otherwise the same as selectedEdition
 	 * @return a label for the edition side of a compare viewer
  	 */
	protected String getEditionLabel(ITypedElement selectedEdition, ITypedElement item) {
		String format= null;
		if (selectedEdition instanceof ResourceNode)
			format= Utilities.getString(fBundle, "workspaceEditionLabel", null);	//$NON-NLS-1$
		else if (selectedEdition instanceof HistoryItem)
			format= Utilities.getString(fBundle, "historyEditionLabel", null);	//$NON-NLS-1$
		if (format == null)
			format= Utilities.getString(fBundle, "editionLabel");	//$NON-NLS-1$
		if (format == null)
			format= "x{0}";	//$NON-NLS-1$
		

		String date= "";	//$NON-NLS-1$
		if (selectedEdition instanceof IModificationDate) {
			long modDate= ((IModificationDate)selectedEdition).getModificationDate();
			date= DateFormat.getDateTimeInstance().format(new Date(modDate));
		}
		
		return formatString(format, date);
	}
	
 	/**
 	 * Returns a label for identifying a node in the edition tree viewer.
 	 * This implementation extracts the value for the key "workspaceTreeFormat" or
 	 * "treeFormat" (in that order) from the resource bundle
 	 * and passes it as the format argument to <code>MessageFormat.format</code>.
 	 * The single format argument for <code>MessageFormat.format</code> ("{0}" in the format string)
 	 * is the formatted modification date of the given input element.
 	 * <p>
	 * Subclasses may override to create their own label.
	 * </p>
	 *
	 * @param edition the edition for which a label must be returned
 	 * @param item if a path has been specified in <code>edition</code> a sub element of the given edition; otherwise the same as edition
 	 * @param date this date will be returned as part of the formatted string
 	 * @return a label of a node in the edition tree viewer
	 * @since 2.0
	 */
	protected String getShortEditionLabel(ITypedElement edition, ITypedElement item, Date date) {
		String format= null;
		if (edition instanceof ResourceNode)
			format= Utilities.getString(fBundle, "workspaceTreeFormat", null);	//$NON-NLS-1$
		if (format == null)
			format= Utilities.getString(fBundle, "treeFormat", null);	//$NON-NLS-1$
		if (format == null)
			format= "x{0}"; //$NON-NLS-1$

		String ds= DateFormat.getTimeInstance().format(date);
		return formatString(format, ds);
	}
	
 	/**
 	 * Returns an image for identifying the edition side of a compare viewer.
 	 * This implementation extracts the value for the key "editionLabel" from the resource bundle
 	 * and passes it as the format argument to <code>MessageFormat.format</code>.
 	 * The single format argument for <code>MessageFormat.format</code> ("{0}" in the format string)
 	 * is the formatted modification date of the given input element.
 	 * <p>
	 * Subclasses may override to create their own label.
	 * </p>
	 *
	 * @param selectedEdition the selected edition for which a label must be returned
 	 * @param item if a path has been specified in <code>selectEdition</code> a sub element of the given selectedEdition; otherwise the same as selectedEdition
 	 * @return a label the edition side of a compare viewer
  	 * @since 2.0
 	 */
	protected Image getEditionImage(ITypedElement selectedEdition, ITypedElement item) {
		if (selectedEdition instanceof ResourceNode)
			return selectedEdition.getImage();
		if (selectedEdition instanceof HistoryItem) {
			if (fTimeImage == null) {
				String iconName= Utilities.getString(fBundle, "timeIcon", "obj16/resource_obj.gif"); //$NON-NLS-1$ //$NON-NLS-2$
				ImageDescriptor id= CompareUIPlugin.getImageDescriptor(iconName);
				if (id != null)
					fTimeImage= id.createImage();
			}
			return fTimeImage;
		}
		return null;
	}
	
 	/* (non Javadoc)
 	 * Creates SWT control tree.
 	 */
	protected synchronized Control createDialogArea(Composite parent2) {
		
		Composite parent= (Composite) super.createDialogArea(parent2);

		getShell().setText(Utilities.getString(fBundle, "title")); //$NON-NLS-1$
		
		Splitter vsplitter= new Splitter(parent,  SWT.VERTICAL);
		vsplitter.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
					| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));

		vsplitter.addDisposeListener(
			new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (fCompareConfiguration != null) {
						fCompareConfiguration.dispose();
						fCompareConfiguration= null;
					}
					if (fDateImage != null) {
						fDateImage.dispose();
						fDateImage= null;
					}
					if (fTimeImage != null) {
						fTimeImage.dispose();						
						fTimeImage= null;
					}
				}
			}
		);
		
		if (fAddMode) {
			// we need two panes: the left for the elements, the right one for the editions
			Splitter hsplitter= new Splitter(vsplitter,  SWT.HORIZONTAL);
			
			fMemberPane= new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
			fMemberPane.setText(Utilities.getString(fBundle, "memberPaneTitle")); //$NON-NLS-1$
			
			int flags= SWT.H_SCROLL | SWT.V_SCROLL;
			if (fMultiSelect)
				flags|= SWT.CHECK;
			fMemberTable= new Table(fMemberPane, flags);
			fMemberTable.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (e.detail == SWT.CHECK) {
							if (e.item instanceof TableItem) {
								TableItem ti= (TableItem) e.item;
								Object data= ti.getData();
								if (ti.getChecked())
									fArrayList.add(data);
								else
									fArrayList.remove(data);
									
								if (fCommitButton != null)
									fCommitButton.setEnabled(fArrayList.size() > 0);
									
								fMemberTable.setSelection(new TableItem[] { ti });
							}
						}
						handleMemberSelect(e.item);
					}
				}
			);
			fMemberPane.setContent(fMemberTable);
			fMemberTable.setFocus();
						
			fEditionPane= new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		} else {
			if (fStructureCompare) {
				// we need two panes: the left for the elements, the right one for the structured diff
				Splitter hsplitter= new Splitter(vsplitter,  SWT.HORIZONTAL);
				
				fEditionPane= new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
				fStructuredComparePane= new CompareViewerSwitchingPane(hsplitter, SWT.BORDER | SWT.FLAT, true) {
					protected Viewer getViewer(Viewer oldViewer, Object input) {
						if (input instanceof ICompareInput)
							return CompareUI.findStructureViewer(oldViewer, (ICompareInput)input, this, getCompareConfiguration());
						return null;
					}
				};
				fStructuredComparePane.addSelectionChangedListener(
					new ISelectionChangedListener() {
						public void selectionChanged(SelectionChangedEvent e) {
							feedInput2(e.getSelection());
						}
					}
				);
			} else {
				// only a single pane showing the editions
				fEditionPane= new CompareViewerPane(vsplitter, SWT.BORDER | SWT.FLAT);
			}
			if (fTitleArg == null)
				fTitleArg= fTargetPair.getItem().getName();
			String titleFormat= Utilities.getString(fBundle, "treeTitleFormat"); //$NON-NLS-1$
			String title= MessageFormat.format(titleFormat, new String[] { fTitleArg });
			fEditionPane.setText(title);
			if (fTitleImage != null)
				fEditionPane.setImage(fTitleImage);
		}
		
		fEditionTree= new Tree(fEditionPane, SWT.H_SCROLL | SWT.V_SCROLL);
		fEditionTree.addSelectionListener(
			new SelectionAdapter() {
//				public void widgetDefaultSelected(SelectionEvent e) {
//					handleDefaultSelected();
//				}
				public void widgetSelected(SelectionEvent e) {
					feedInput(e.item);
				}
			}
		);
		fEditionPane.setContent(fEditionTree);		
		
		// now start the thread (and forget about it)
		if (fThread != null) {
			fThread.start();
			fThread= null;
		}
		
		fContentPane= new CompareViewerSwitchingPane(vsplitter, SWT.BORDER | SWT.FLAT) {
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				return CompareUI.findContentViewer(oldViewer, input, this, getCompareConfiguration());	
			}
		};
		vsplitter.setWeights(new int[] { 30, 70 });
		
		statusLabel = new Label(parent, SWT.NONE);
		statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		applyDialogFont(parent);				
		return parent;
	}	
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		String buttonLabel= Utilities.getString(fBundle, "buttonLabel", IDialogConstants.OK_LABEL); //$NON-NLS-1$
		if (fCompareMode) {
			// only a 'Done' button
			createButton(parent, IDialogConstants.CANCEL_ID, buttonLabel, false);
		} else {
			// a 'Cancel' and a 'Add/Replace' button
			fCommitButton= createButton(parent, IDialogConstants.OK_ID, buttonLabel, true);
			fCommitButton.setEnabled(false);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}
	}

	/**
	 * Overidden to disable dismiss on double click in compare mode.
	 * @since 2.0
	 */
	protected void okPressed() {
		if (fCompareMode) {
			// don't dismiss dialog
		} else
			super.okPressed();
	}

	//---- private stuff ----------------------------------------------------------------------------------------
				
	/*
	 * Asynchroneously sends a Pair (or null) to the UI thread.
	 */
	private void sendPair(final Pair pair) {		
		if (fEditionTree != null && !fEditionTree.isDisposed()) {
			Display display= fEditionTree.getDisplay();
			display.asyncExec(
				new Runnable() {
					public void run() {
						addMemberEdition(pair);
					}
				}
			);
		}
	}
	
	private static void internalSort(IModificationDate[] keys) { 
		Arrays.sort(keys, new Comparator() {
			public int compare(Object o1, Object o2) {
				IModificationDate d1= (IModificationDate) o1;
				IModificationDate d2= (IModificationDate) o2;
				long d= d2.getModificationDate() - d1.getModificationDate();
				if (d < 0)
					return -1;
				if (d > 0)
					return 1;
				return 0;
			}
		});
	}
	
	/*
	 * Adds the given Pair to the member editions.
	 * If HIDE_IDENTICAL is true the new Pair is only added if its contents
	 * is different from the preceeding Pair.
	 * If the argument is <code>null</code> the message "No Editions found" is shown
	 * in the member or edition viewer.
	 */
	private void addMemberEdition(Pair pair) {
		
		if (pair == null) {	// end of list of pairs
			if (fMemberTable != null) {	
				if (!fMemberTable.isDisposed() && fMemberTable.getItemCount() == 0) {
					if (fMultiSelect) {
						fMemberTable.dispose();
						fMemberTable= new Table(fMemberPane, SWT.NONE);
						fMemberPane.setContent(fMemberTable);
					}
					TableItem ti= new TableItem(fMemberTable, SWT.NONE);
					ti.setText(Utilities.getString(fBundle, "noAdditionalMembersMessage")); //$NON-NLS-1$
				}
				return;
			}
			if (fEditionTree != null && !fEditionTree.isDisposed() && fEditionTree.getItemCount() == 0) {
				TreeItem ti= new TreeItem(fEditionTree, SWT.NONE);
				ti.setText(Utilities.getString(fBundle, "notFoundInLocalHistoryMessage")); //$NON-NLS-1$
			}
			return;
		}
		
		if (fMemberEditions == null)
			fMemberEditions= new HashMap();
		if (fMultiSelect && fMemberSelection == null)
			fMemberSelection= new HashMap();
		
		ITypedElement item= pair.getItem();
		List editions= (List) fMemberEditions.get(item);
		if (editions == null) {
			editions= new ArrayList();
			fMemberEditions.put(item, editions);
			if (fMemberTable != null && !fMemberTable.isDisposed()) {
				ITypedElement te= item;
				String name= te.getName();
				
				// find position
				TableItem[] items= fMemberTable.getItems();
				int where= items.length;
				for (int i= 0; i < where; i++) {
					String n= items[i].getText();
					if (n.compareTo(name) > 0) {
						where= i;
						break;
					}
				}
				
				TableItem ti= new TableItem(fMemberTable, where, SWT.NULL);
				ti.setImage(te.getImage());
				ti.setText(name);
				ti.setData(editions);
			}
		}
		if (fHideIdentical) {
			Pair last= fTargetPair;
			int size= editions.size();
			if (size > 0)
				last= (Pair) editions.get(size-1);
			if (last != null && last.equals(pair))
				return;	// don't add since the new one is equal to old
		}
		editions.add(pair);
		
		if (!fAddMode || editions == fCurrentEditions)
			addEdition(pair);
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
	 * Adds the given Pair to the edition tree.
	 * It takes care of creating tree nodes for different dates.
	 */
	private void addEdition(Pair pair) {
		if (fEditionTree == null || fEditionTree.isDisposed())
			return;
		
		// find last day
		TreeItem[] days= fEditionTree.getItems();
		TreeItem lastDay= null;
		if (days.length > 0)
			lastDay= days[days.length-1];
		
		boolean first= lastDay == null;
		
		ITypedElement edition= pair.getEdition();
		ITypedElement item= pair.getItem();
		
		long ldate= ((IModificationDate)edition).getModificationDate();		
		long day= dayNumber(ldate);
		Date date= new Date(ldate);
		if (lastDay == null || day != dayNumber(((Date)lastDay.getData()).getTime())) {
			lastDay= new TreeItem(fEditionTree, SWT.NONE);
			if (fDateImage == null) {
				String iconName= Utilities.getString(fBundle, "dateIcon", "obj16/day_obj.gif"); //$NON-NLS-2$ //$NON-NLS-1$
				ImageDescriptor id= CompareUIPlugin.getImageDescriptor(iconName);
				if (id != null)
					fDateImage= id.createImage();
			}
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
		ti.setImage(getEditionImage(edition, item));
		
		String s= getShortEditionLabel(edition, item, date);
		if (pair.fHasError) {
			String pattern= Utilities.getString(fBundle, "parseErrorFormat"); //$NON-NLS-1$
			s= MessageFormat.format(pattern, new String[] { s } );
		}
		ti.setText(s);
		
		ti.setData(pair);
		
		// determine selected TreeItem
		TreeItem selection= first ? ti : null;
		if (fMemberSelection != null) {
			Object selected= fMemberSelection.get(fCurrentEditions);
			if (selected != null) {
				if (selected == pair.getItem())
					selection= ti;
				else
					selection= null;
			}
		}
		if (selection != null) {
			fEditionTree.setSelection(new TreeItem[] { selection });
			if (!fAddMode)
				fEditionTree.setFocus();
			feedInput(selection);
		}
		
		if (first) // expand first node
			lastDay.setExpanded(true);
	}
						
	/*
	 * Feeds selection from member viewer to edition viewer.
	 */
	private void handleMemberSelect(Widget w) {
		Object data= w.getData();
		if (data instanceof List) {
			List editions= (List) data;
			if (editions != fCurrentEditions) {
				fCurrentEditions= editions;
				fEditionTree.removeAll();
				
				String pattern= Utilities.getString(fBundle, "treeTitleFormat"); //$NON-NLS-1$
				String title= MessageFormat.format(pattern, new Object[] { ((Item)w).getText() });
				fEditionPane.setText(title);
								
				Iterator iter= editions.iterator();
				while (iter.hasNext()) {
					Object item= iter.next();
					if (item instanceof Pair)
						addEdition((Pair) item);
				}
			}
		}
	}
	
	private void setInput(Object input) {
		if (!fCompare && input instanceof ICompareInput) {
			ICompareInput ci= (ICompareInput) input;
			if (fTargetIsRight)
				input= ci.getLeft();
			else
				input= ci.getRight();
		}
		fContentPane.setInput(input);
		if (fStructuredComparePane != null)
			fStructuredComparePane.setInput(input);
	}
	
	/*
	 * Feeds selection from edition viewer to content (and structure) viewer.
	 */
	private void feedInput(Widget w) {
		Object input= w.getData();
		boolean isOK= false;
		if (input instanceof Pair) {
			Pair pair= (Pair) input;
			fSelectedItem= pair.getItem();
			isOK= !pair.fHasError;
			
			ITypedElement edition= pair.getEdition();
			String editionLabel= getEditionLabel(edition, fSelectedItem);
			Image editionImage= getEditionImage(edition, fSelectedItem);
					
			if (fAddMode) {
				if (fMemberSelection != null)
					fMemberSelection.put(fCurrentEditions, fSelectedItem);
				setInput(fSelectedItem);
				fContentPane.setText(editionLabel);
				fContentPane.setImage(editionImage);
			} else {
				getCompareConfiguration();
				if (fTargetIsRight) {
					fCompareConfiguration.setLeftLabel(editionLabel);
					fCompareConfiguration.setLeftImage(editionImage);
					setInput(new DiffNode(fSelectedItem, fTargetPair.getItem()));
				} else {
					fCompareConfiguration.setRightLabel(editionLabel);
					fCompareConfiguration.setRightImage(editionImage);
					setInput(new DiffNode(fTargetPair.getItem(), fSelectedItem));
				}
			}
		} else {
			fSelectedItem= null;
			setInput(null);
		}
		if (fCommitButton != null) {
			if (fMultiSelect)
				fCommitButton.setEnabled(isOK && fSelectedItem != null && fArrayList.size() > 0);
			else
				fCommitButton.setEnabled(isOK && fSelectedItem != null && fTargetPair.getItem() != fSelectedItem);
		}
	}
	
	/*
	 * Feeds selection from structure viewer to content viewer.
	 */
	private void feedInput2(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) sel;
			if (ss.size() == 1)
				fContentPane.setInput(ss.getFirstElement());
		}
	}
}
