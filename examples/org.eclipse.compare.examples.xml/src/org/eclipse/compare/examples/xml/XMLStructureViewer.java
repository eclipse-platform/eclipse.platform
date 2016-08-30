/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.examples.xml;

import java.text.MessageFormat;
import java.util.*;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

/**
 * An XML diff tree viewer that can be configured with a <code>IStructureCreator</code>
 * to retrieve a hierarchical structure from the input object (an <code>ICompareInput</code>)
 * and perform a two-way or three-way compare on it.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed outside
 * this package.
 * </p>
 *
 * @see ICompareInput
 */
public class XMLStructureViewer extends StructureDiffViewer {

	private CompareViewerSwitchingPane fParent;

	private HashMap fIdMapsInternal;
	private HashMap fIdMaps;
	private HashMap fOrderedElementsInternal;
	private HashMap fOrderedElements;

	protected static final char SIGN_SEPARATOR=
		XMLStructureCreator.SIGN_SEPARATOR;

	class XMLSorter extends ViewerSorter {

		ArrayList fOrdered;
		boolean fAlwaysOrderSort;

		public XMLSorter() {
			super();
			fAlwaysOrderSort= false;
		}

		public void setOrdered(ArrayList ordered) {
			fOrdered= ordered;
		}

		public void setAlwaysOrderSort(boolean alwaysOrderSort) {
			fAlwaysOrderSort= alwaysOrderSort;
		}

		public int category(Object node) {
			if (node instanceof DiffNode) {
				Object o= ((DiffNode) node).getId();
				if (o instanceof XMLNode) {
					String xmlType= ((XMLNode) o).getXMLType();
					if (xmlType.equals(XMLStructureCreator.TYPE_ATTRIBUTE))
						return 1;
					if (xmlType.equals(XMLStructureCreator.TYPE_ELEMENT))
						return 2;
					if (xmlType.equals(XMLStructureCreator.TYPE_TEXT))
						return 2;
				}
			}
			return 0;
		}

		public void sort(final Viewer viewer, Object[] elements) {
			if ((fOrdered != null || fAlwaysOrderSort)
				&& elements != null
				&& elements.length > 0
				&& elements[0] instanceof DiffNode) {
				Object o= ((DiffNode) elements[0]).getId();
				if (o instanceof XMLNode) {
					XMLNode parent= ((XMLNode) o).getParent();
					String sig= parent.getSignature();
					if (sig.endsWith(XMLStructureCreator.SIGN_ELEMENT)) {
						String newSig=
							sig.substring(
								0,
								sig.length()
									- XMLStructureCreator.SIGN_ELEMENT.length());
						if (fAlwaysOrderSort || fOrdered.contains(newSig)) {
							final ArrayList originalTree=
								new ArrayList(
									Arrays.asList(parent.getChildren()));
							Arrays.sort(elements, new Comparator() {
								public int compare(Object a, Object b) {
									return XMLSorter.this.compare(
										(DiffNode) a,
										(DiffNode) b,
										originalTree);
								}
							});
							return;
						}
					}
				}
			}
			super.sort(viewer, elements);
		}

		private int compare(DiffNode a, DiffNode b, ArrayList originalTree) {

			int index_a= originalTree.indexOf(a.getId());
			int index_b= originalTree.indexOf(b.getId());
			if (index_a < index_b)
				return -1;
			return 1;
		}
	}

	/**
	 * Creates a new viewer for the given SWT tree control with the specified configuration.
	 *
	 * @param tree the tree control
	 * @param configuration the configuration for this viewer
	 */
	public XMLStructureViewer(Tree tree, CompareConfiguration configuration) {
		super(tree, configuration);
		initialize();
	}

	/**
	 * Creates a new viewer under the given SWT parent with the specified configuration.
	 *
	 * @param parent the SWT control under which to create the viewer
	 * @param configuration the configuration for this viewer
	 */
	public XMLStructureViewer(
		Composite parent,
		CompareConfiguration configuration) {
		super(parent, configuration);
		if (parent instanceof CompareViewerSwitchingPane) {
			fParent= (CompareViewerSwitchingPane) parent;
		}
		initialize();
	}

	private void initialize() {
		setStructureCreator(new XMLStructureCreator());
		XMLPlugin plugin= XMLPlugin.getDefault();

		plugin.getViewers().add(this);

		fIdMaps= plugin.getIdMaps();
		fIdMapsInternal= plugin.getIdMapsInternal();
		fOrderedElements= plugin.getOrderedElements();
		fOrderedElementsInternal= plugin.getOrderedElementsInternal();

		XMLSorter sorter= new XMLSorter();
		setSorter(sorter);

	}

	protected XMLStructureCreator getXMLStructureCreator() {
		return (XMLStructureCreator) getStructureCreator();
	}

	/* (non Javadoc)
	 * Overridden to unregister all listeners.
	 */
	protected void handleDispose(DisposeEvent event) {

		XMLPlugin.getDefault().getViewers().remove(this);

		super.handleDispose(event);
	}

	/*
	 * Recreates the comparable structures for the input sides.
	 */
	protected void compareInputChanged(ICompareInput input) {
		if (input != null) {
			ITypedElement t= input.getLeft();
			if (t != null) {
				String fileExtension= t.getType();
				getXMLStructureCreator().setFileExtension(fileExtension);
			}
		}

		getXMLStructureCreator().initIdMaps();
		super.compareInputChanged(input);

		if (input != null && fParent.getTitleArgument() == null)
			appendToTitle(getXMLStructureCreator().getIdMap());
	}

	/**
	 * Calls <code>diff</code> whenever the byte contents changes.
	 */
	protected void contentChanged() {
		fIdMaps= XMLPlugin.getDefault().getIdMaps();
		fOrderedElements= XMLPlugin.getDefault().getOrderedElements();
		getXMLStructureCreator().updateIdMaps();
		if (isIdMapRemoved()) {
			getXMLStructureCreator().setIdMap(
				XMLStructureCreator.DEFAULT_IDMAP);
		}

		getXMLStructureCreator().initIdMaps();

		contentChanged(null);

		if (fParent.getTitleArgument() == null)
			appendToTitle(getXMLStructureCreator().getIdMap());

	}

	protected void preDiffHook(
		IStructureComparator ancestor,
		IStructureComparator left,
		IStructureComparator right, IProgressMonitor monitor) {
		//		if (!xsc.getIdMap().equals(XMLStructureCreator.USE_ORDERED)) {
		//TimeoutContext.run(true, TIMEOUT, getControl().getShell(), runnable);
		if (left != null && right != null) {
			performMatching((XMLNode)left, (XMLNode)right, (XMLNode)ancestor, monitor);
		}
	}

	/**
	 * Overriden to create buttons in the viewer's pane control bar.
	 * <p>
	 *
	 * @param toolBarManager the toolbar manager for which to add the buttons
	 */
	protected void createToolItems(ToolBarManager toolBarManager) {
		super.createToolItems(toolBarManager);
		toolBarManager.appendToGroup("modes", new ChooseMatcherDropDownAction(this)); //$NON-NLS-1$
		toolBarManager.appendToGroup("modes", new CreateNewIdMapAction(this)); //$NON-NLS-1$
	}

	/**
	 * Overriden to create a context menu.
	 * <p>
	 *
	 * @param manager the menu manager for which to add menu items
	 */
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		ISelection s= getSelection();
		if (s instanceof StructuredSelection
			&& ((StructuredSelection) s).getFirstElement() instanceof DiffNode
			&& ((DiffNode) ((StructuredSelection) s).getFirstElement()).getId()
				instanceof XMLNode) {
			DiffNode diffnode=
				(DiffNode) ((StructuredSelection) s).getFirstElement();
			String diffnodeIdSig= ((XMLNode) diffnode.getId()).getSignature();
			fIdMaps= XMLPlugin.getDefault().getIdMaps();
			String idmap_name= getXMLStructureCreator().getIdMap();
			if (diffnodeIdSig.endsWith(XMLStructureCreator.SIGN_ATTRIBUTE) || (diffnodeIdSig.endsWith(XMLStructureCreator.SIGN_TEXT) && ((XMLNode) diffnode.getId()).getOrigId().endsWith("(1)"))) { //$NON-NLS-1$
				Action action= new SetAsIdAction(diffnode);
				if (!fIdMaps.containsKey(idmap_name)) {
					action.setText(XMLCompareMessages.XMLStructureViewer_action_notUserIdMap); 
					action.setEnabled(false);
				} else {
					HashMap idmapHM= (HashMap) fIdMaps.get(idmap_name);
					XMLNode idNode= (XMLNode) diffnode.getId();
					String signature= idNode.getSignature();
					String idname= ""; //$NON-NLS-1$
					if (idNode
						.getSignature()
						.endsWith(XMLStructureCreator.SIGN_ATTRIBUTE)) {
						signature=
							signature.substring(
								0,
								signature.indexOf(
									XMLStructureCreator.SIGN_ATTRIBUTE));
						int end_of_signature=
							signature.lastIndexOf(
								SIGN_SEPARATOR,
								signature.length() - 2);
						idname=
							signature.substring(
								end_of_signature + 1,
								signature.length() - 1);
						signature= signature.substring(0, end_of_signature + 1);
					} else if (
						idNode.getSignature().endsWith(
							XMLStructureCreator.SIGN_TEXT)) {
						XMLNode textNode= (XMLNode) diffnode.getId();
						XMLNode idelem= textNode.getParent();
						XMLNode elem= idelem.getParent();
						signature=
							elem.getSignature().substring(
								0,
								elem.getSignature().indexOf(
									XMLStructureCreator.SIGN_ELEMENT));
						idname= idelem.getOrigId();
						idname=
							idname.substring(
								0,
								idname.indexOf(
									XMLStructureCreator.ID_SEPARATOR));
						idname=
							new Character(XMLStructureCreator.ID_TYPE_BODY)
								+ idname;
					}
					if (idmapHM.containsKey(signature)) {
						if (idmapHM.get(signature).equals(idname)) {
							action.setText(XMLCompareMessages.XMLStructureViewer_action_setId_text1); 
							action.setEnabled(false);
						} else {
							String oldId= (String) idmapHM.get(signature);
							if (oldId
								.startsWith(
									(new Character(XMLStructureCreator
										.ID_TYPE_BODY))
										.toString()))
								oldId= oldId.substring(1);
							action.setText(MessageFormat.format("{0} {1}", XMLCompareMessages.XMLStructureViewer_action_setId_text2, oldId));  //$NON-NLS-1$
							action.setEnabled(true);
						}
					} else {
						action.setText(XMLCompareMessages.XMLStructureViewer_action_setId_text3); 
						action.setEnabled(true);
					}
				}
				manager.add(action);
			} else if (
				diffnodeIdSig.endsWith(XMLStructureCreator.SIGN_ELEMENT)) {
				SetOrderedAction action= new SetOrderedAction(idmap_name);
				if (!fIdMaps.containsKey(idmap_name)) {
					action.setText(XMLCompareMessages.XMLStructureViewer_action_notUserIdMap); 
					action.setEnabled(false);
				} else {
					ArrayList idmapOrdered=
						(ArrayList) fOrderedElements.get(idmap_name);
					XMLNode idNode= (XMLNode) diffnode.getId();
					String signature= idNode.getSignature();
					//					String idname= "";
					signature=
						signature.substring(
							0,
							signature.indexOf(
								XMLStructureCreator.SIGN_ELEMENT));
					if (idmapOrdered != null
						&& idmapOrdered.contains(signature)) {
						action.setText(XMLCompareMessages.XMLStructureViewer_action_setOrdered_exists); 
						action.setEnabled(false);
					} else {
						action.setText(XMLCompareMessages.XMLStructureViewer_action_setOrdered); 
						action.setSignature(signature);
						action.setEnabled(true);
					}
				}

				manager.add(action);
			}
		}
	}

	protected void appendToTitle(String idmap_name) {
		if (fParent != null) {
			getXMLStructureCreator().setIdMap(idmap_name);
			fParent.setTitleArgument(idmap_name);
		}
	}

	/*
	 * Returns true if the current Id Map scheme has been removed.
	 */
	private boolean isIdMapRemoved() {
		XMLStructureCreator xsc= getXMLStructureCreator();
		String IdMapName= xsc.getIdMap();
		return !IdMapName.equals(XMLStructureCreator.USE_UNORDERED)
			&& !IdMapName.equals(XMLStructureCreator.USE_ORDERED)
			&& !fIdMaps.containsKey(IdMapName)
			&& !fIdMapsInternal.containsKey(IdMapName)
			&& !fOrderedElements.containsKey(IdMapName);
	}

	protected class SetAsIdAction extends Action {

		DiffNode fDiffNode;

		public SetAsIdAction(DiffNode diffnode) {
			fDiffNode= diffnode;
		}

		public void run() {
			XMLStructureCreator sc= getXMLStructureCreator();
			//			DiffNode diffnode = (DiffNode) ((StructuredSelection) getSelection()).getFirstElement();
			String idmap_name= sc.getIdMap();
			if (fIdMaps.containsKey(idmap_name)) {
				HashMap idmapHM= (HashMap) fIdMaps.get(idmap_name);
				if (((XMLNode) fDiffNode.getId())
					.getSignature()
					.endsWith(XMLStructureCreator.SIGN_ATTRIBUTE)) {
					XMLNode attrNode= (XMLNode) fDiffNode.getId();
					String signature= attrNode.getSignature();
					signature=
						signature.substring(
							0,
							signature.indexOf(
								XMLStructureCreator.SIGN_ATTRIBUTE));
					int end_of_signature=
						signature.lastIndexOf(
							SIGN_SEPARATOR,
							signature.length() - 2);
					String idattr=
						signature.substring(
							end_of_signature + 1,
							signature.length() - 1);
					signature= signature.substring(0, end_of_signature + 1);
					idmapHM.put(signature, idattr);
					XMLPlugin.getDefault().setIdMaps(
						fIdMaps,
						null,
						null,
						false);
					//contentChanged();
				} else if (
					((XMLNode) fDiffNode.getId()).getSignature().endsWith(
						XMLStructureCreator.SIGN_TEXT)) {
					XMLNode textNode= (XMLNode) fDiffNode.getId();
					XMLNode idelem= textNode.getParent();
					XMLNode elem= idelem.getParent();
					String signature=
						elem.getSignature().substring(
							0,
							elem.getSignature().indexOf(
								XMLStructureCreator.SIGN_ELEMENT));
					String idname= idelem.getOrigId();
					idname=
						idname.substring(
							0,
							idname.indexOf(XMLStructureCreator.ID_SEPARATOR));
					idname=
						new Character(XMLStructureCreator.ID_TYPE_BODY)
							+ idname;
					idmapHM.put(signature, idname);
					XMLPlugin.getDefault().setIdMaps(
						fIdMaps,
						null,
						null,
						false);
					//contentChanged();
				}
			}
		}
	}

	protected class SetOrderedAction extends Action {

		String fIdMapName;
		String fSignature;

		public SetOrderedAction(String idmap_name) {
			fIdMapName= idmap_name;
		}

		public void run() {
			//String idmap_name= getXMLStructureCreator().getIdMap();
			if (fSignature != null) {
				ArrayList idmapOrdered=
					(ArrayList) fOrderedElements.get(fIdMapName);
				if (idmapOrdered == null) {
					idmapOrdered= new ArrayList();
					fOrderedElements.put(fIdMapName, idmapOrdered);
				}
				idmapOrdered.add(fSignature);
			}
		}

		public void setSignature(String signature) {
			fSignature= signature;
		}
	}

	protected void updateIdMaps() {
		getXMLStructureCreator().updateIdMaps();
	}

	/*
	 * Tracks property changes of the configuration object.
	 * Clients may override to track their own property changes.
	 * In this case they must call the inherited method.
	 */
	protected void propertyChange(PropertyChangeEvent event) {
		String key= event.getProperty();
		if (key.equals(CompareConfiguration.IGNORE_WHITESPACE)) {
			getXMLStructureCreator().setRemoveWhiteSpace(
				!getXMLStructureCreator().getRemoveWhiteSpace());
			contentChanged();
		}
		super.propertyChange(event);
	}

	private void performMatching(final XMLNode left, final XMLNode right,
			final XMLNode ancestor, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		int totalWork;
		if (ancestor != null)
			totalWork= 1;
		else
			totalWork= 3;
		monitor.beginTask(XMLCompareMessages.XMLStructureViewer_matching_beginTask, totalWork); 
		ArrayList ordered= null;
		if (!getXMLStructureCreator()
			.getIdMap()
			.equals(XMLStructureCreator.USE_UNORDERED)
			&& !getXMLStructureCreator().getIdMap().equals(
				XMLStructureCreator.USE_ORDERED)) {
			ordered=
				(ArrayList) fOrderedElements.get(
					getXMLStructureCreator().getIdMap());
			if (ordered == null)
				ordered=
					(ArrayList) fOrderedElementsInternal.get(
						getXMLStructureCreator().getIdMap());
		}
		if (getSorter() instanceof XMLSorter)
			 ((XMLSorter) getSorter()).setOrdered(ordered);
		AbstractMatching m= null;
		if (getXMLStructureCreator()
			.getIdMap()
			.equals(XMLStructureCreator.USE_ORDERED)) {
			m= new OrderedMatching();
			if (getSorter() instanceof XMLSorter)
				 ((XMLSorter) getSorter()).setAlwaysOrderSort(true);
		}
		try {
			if (m != null) {
				m.match(left, right, false, monitor);
				if (ancestor != null) {
					m.match(
						left,
						ancestor,
						true,
						new SubProgressMonitor(monitor, 1));
					m.match(
						right,
						ancestor,
						true,
						new SubProgressMonitor(monitor, 1));
				}
				//				} catch (InterruptedException e) {
				//					System.out.println("in run");
				//					e.printStackTrace();
			}
		} finally {
			monitor.done();
		}
	}
}
