/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A View Pane Rendering Manager manages all the rendering from a view pane. It
 * is responsible for handling debug events and removing renderings from the
 * view pane as a debug session is terminated. In addition, the rendering
 * manager is responsible for persisting memory renderings. Renderings need to
 * be persisted when the memory view is disposed. If the view is opened again,
 * the same set of renderings will be created in the view pane if the renderings
 * are still valid.
 *
 * @since 3.1
 */
public class ViewPaneRenderingMgr implements IDebugEventSetListener {

	private final ArrayList<IMemoryRendering> fRenderings = new ArrayList<>();
	private final IMemoryRenderingContainer fViewPane;

	private static final String RENDERINGS_TAG = "persistedMemoryRenderings"; //$NON-NLS-1$
	private static final String MEMORY_RENDERING_TAG = "memoryRendering"; //$NON-NLS-1$
	private static final String MEMORY_BLOCK = "memoryBlock"; //$NON-NLS-1$
	private static final String RENDERING_ID = "renderingId"; //$NON-NLS-1$

	public ViewPaneRenderingMgr(IMemoryRenderingContainer viewPane) {
		fViewPane = viewPane;
		loadPersistedRenderings(getPrefId());
	}

	public void removeMemoryBlockRendering(IMemoryBlock mem, String renderingId) {
		if (fRenderings == null) {
			return;
		}

		IMemoryRendering[] toRemove = getRenderings(mem, renderingId);

		for (IMemoryRendering rendering : toRemove) {
			fRenderings.remove(rendering);
			// remove listener after the last memory block has been removed
			if (fRenderings.isEmpty()) {
				DebugPlugin.getDefault().removeDebugEventListener(this);
			}
		}

		storeRenderings();
	}

	public void addMemoryBlockRendering(IMemoryRendering rendering) {

		// do not allow duplicated objects
		if (fRenderings.contains(rendering)) {
			return;
		}

		fRenderings.add(rendering);

		// add listener for the first memory block added
		if (fRenderings.size() == 1) {
			DebugPlugin.getDefault().addDebugEventListener(this);
		}

		storeRenderings();
	}

	public void removeMemoryBlockRendering(IMemoryRendering rendering) {
		if (rendering == null) {
			return;
		}

		if (!fRenderings.contains(rendering)) {
			return;
		}

		fRenderings.remove(rendering);

		// remove listener after the last memory block has been removed
		if (fRenderings.isEmpty()) {
			DebugPlugin.getDefault().removeDebugEventListener(this);
		}

		storeRenderings();
	}

	public IMemoryRendering[] getRenderings(IMemoryBlock mem, String renderingId) {
		if (renderingId == null) {
			return getRenderingsFromMemoryBlock(mem);
		}

		ArrayList<IMemoryRendering> ret = new ArrayList<>();
		for (int i = 0; i < fRenderings.size(); i++) {
			if (fRenderings.get(i) != null) {
				IMemoryRendering rendering = fRenderings.get(i);
				if (rendering.getMemoryBlock() == mem && renderingId.equals(rendering.getRenderingId())) {
					ret.add(rendering);
				}
			}
		}

		return ret.toArray(new IMemoryRendering[ret.size()]);
	}

	public IMemoryRendering[] getRenderings() {
		return fRenderings.toArray(new IMemoryRendering[fRenderings.size()]);
	}

	public IMemoryRendering[] getRenderingsFromDebugTarget(IDebugTarget target) {
		ArrayList<IMemoryRendering> ret = new ArrayList<>();
		for (int i = 0; i < fRenderings.size(); i++) {
			if (fRenderings.get(i) != null) {
				IMemoryRendering rendering = fRenderings.get(i);
				if (rendering.getMemoryBlock().getDebugTarget() == target) {
					ret.add(rendering);
				}
			}
		}

		return ret.toArray(new IMemoryRendering[ret.size()]);
	}

	public IMemoryRendering[] getRenderingsFromMemoryBlock(IMemoryBlock block) {
		ArrayList<IMemoryRendering> ret = new ArrayList<>();
		for (int i = 0; i < fRenderings.size(); i++) {
			if (fRenderings.get(i) != null) {
				IMemoryRendering rendering = fRenderings.get(i);
				if (rendering.getMemoryBlock() == block) {
					ret.add(rendering);
				}
			}
		}

		return ret.toArray(new IMemoryRendering[ret.size()]);
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {

		for (DebugEvent event : events) {
			handleDebugEvent(event);
		}

	}

	public void handleDebugEvent(DebugEvent event) {
		Object obj = event.getSource();
		IDebugTarget dt = null;

		if (event.getKind() == DebugEvent.TERMINATE) {
			// a terminate event could happen from an IThread or IDebugTarget
			// Only handle terminate event from debug target
			if (obj instanceof IDebugTarget) {
				dt = ((IDebugTarget) obj);

				// returns empty array if dt == null
				IMemoryRendering[] deletedrendering = getRenderingsFromDebugTarget(dt);

				for (IMemoryRendering rendering : deletedrendering) {
					removeMemoryBlockRendering(rendering.getMemoryBlock(), rendering.getRenderingId());
					fViewPane.removeMemoryRendering(rendering);
				}
			}
		}
	}

	public void dispose() {
		// remove all renderings
		fRenderings.clear();

		String secondaryId = getViewSiteSecondaryId();
		if (secondaryId != null) {
			// do not save renderings if this is not the primary rendering view
			String prefid = getPrefId();
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
			if (node != null) {
				node.remove(prefid);
				try {
					node.flush();
				} catch (BackingStoreException e) {
					DebugUIPlugin.log(e);
				}
			}
		}

		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	/**
	 * Store renderings as preferences. If renderings are stored, renderings can
	 * be persisted even after the memory view is closed.
	 */
	private void storeRenderings() {
		String renderingsStr = IInternalDebugCoreConstants.EMPTY_STRING;
		try {
			renderingsStr = getRenderingsAsXML();
		} catch (IOException e) {
			DebugUIPlugin.log(e);
		} catch (ParserConfigurationException e) {
			DebugUIPlugin.log(e);
		} catch (TransformerException e) {
			DebugUIPlugin.log(e);
		}

		String prefid = getPrefId();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if (node != null) {
			if (renderingsStr != null) {
				node.put(prefid, renderingsStr);
			} else {
				IEclipsePreferences def = DefaultScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
				if (def != null) {
					node.put(prefid, def.get(prefid, IInternalDebugCoreConstants.EMPTY_STRING));
				}
			}
			try {
				node.flush();
			} catch (BackingStoreException e) {
				DebugUIPlugin.log(e);
			}
		}
	}

	private String getPrefId() {
		// constructs id based on memory view's secondary id + the rendering
		// view pane id
		// format: secondaryId:viewPaneId
		StringBuilder id = new StringBuilder();
		IMemoryRenderingSite renderingSite = fViewPane.getMemoryRenderingSite();
		IWorkbenchPartSite ps = renderingSite.getSite();
		if (ps instanceof IViewSite) {
			IViewSite vs = (IViewSite) ps;
			String secondaryId = vs.getSecondaryId();
			if (secondaryId != null) {
				id.append(secondaryId);
				id.append(":"); //$NON-NLS-1$
			}

		}
		id.append(fViewPane.getId());
		String prefId = id.toString();
		return prefId;
	}

	/**
	 * Convert renderings to xml text
	 *
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private String getRenderingsAsXML() throws IOException, ParserConfigurationException, TransformerException {
		IMemoryRendering[] renderings = fRenderings.toArray(new IMemoryRendering[fRenderings.size()]);

		if (renderings.length == 0) {
			return null;
		}

		Document document = LaunchManager.getDocument();
		Element rootElement = document.createElement(RENDERINGS_TAG);
		document.appendChild(rootElement);
		for (IMemoryRendering rendering : renderings) {
			Element element = document.createElement(MEMORY_RENDERING_TAG);
			element.setAttribute(MEMORY_BLOCK, Integer.toString(rendering.getMemoryBlock().hashCode()));
			element.setAttribute(RENDERING_ID, rendering.getRenderingId());
			rootElement.appendChild(element);
		}
		return LaunchManager.serializeDocument(document);
	}

	/**
	 * Load renderings currently stored.
	 */
	private void loadPersistedRenderings(String prefId) {
		String renderingsStr = Platform.getPreferencesService().getString(DebugUIPlugin.getUniqueIdentifier(), prefId, "", //$NON-NLS-1$
		null);
		if (renderingsStr.length() == 0) {
			return;
		}
		Element root;
		try {
			root = DebugPlugin.parseDocument(renderingsStr);
		} catch (CoreException e) {
			DebugUIPlugin.logErrorMessage("An exception occurred while loading memory renderings."); //$NON-NLS-1$
			return;
		}
		if (!root.getNodeName().equals(RENDERINGS_TAG)) {
			DebugUIPlugin.logErrorMessage("Invalid format encountered while loading memory renderings."); //$NON-NLS-1$
			return;
		}
		NodeList list = root.getChildNodes();
		boolean renderingsAdded = false;
		for (int i = 0, numItems = list.getLength(); i < numItems; i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (!element.getNodeName().equals(MEMORY_RENDERING_TAG)) {
					DebugUIPlugin.logErrorMessage("Invalid XML element encountered while loading memory rendering."); //$NON-NLS-1$
					continue;
				}
				String memoryBlockHashCode = element.getAttribute(MEMORY_BLOCK);
				String renderingId = element.getAttribute(RENDERING_ID);

				IMemoryBlock[] memoryBlocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks();
				IMemoryBlock memoryBlock = null;
				for (IMemoryBlock m : memoryBlocks) {
					if (Integer.toString(m.hashCode()).equals(memoryBlockHashCode)) {
						memoryBlock = m;
					}
				}

				if (memoryBlock != null) {
					IMemoryRenderingType[] types = DebugUITools.getMemoryRenderingManager().getRenderingTypes(memoryBlock);
					IMemoryRenderingType type = null;
					for (IMemoryRenderingType t : types) {
						if (t.getId().equals(renderingId)) {
							type = t;
						}
					}

					// if memory block is not found, the rendering is no longer
					// valid
					// simply ignore the rendering
					if (type != null) {
						try {

							IMemoryRendering rendering = type.createRendering();
							if (rendering != null) {
								rendering.init(fViewPane, memoryBlock);
								if (!fRenderings.contains(rendering)) {
									fRenderings.add(rendering);
									renderingsAdded = true;
								}
							}
						} catch (CoreException e1) {
							DebugUIPlugin.log(e1);
						}
					}
				}
			}
		}
		if (renderingsAdded) {
			DebugPlugin.getDefault().addDebugEventListener(this);
		}
	}

	/**
	 * @return secondary id, or null if not available
	 */
	private String getViewSiteSecondaryId() {
		IMemoryRenderingSite renderingSite = fViewPane.getMemoryRenderingSite();
		IWorkbenchPartSite ps = renderingSite.getSite();
		if (ps instanceof IViewSite) {
			IViewSite vs = (IViewSite) ps;
			String secondaryId = vs.getSecondaryId();
			return secondaryId;
		}
		return null;
	}
}
