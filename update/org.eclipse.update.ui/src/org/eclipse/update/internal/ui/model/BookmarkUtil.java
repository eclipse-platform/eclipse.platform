/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.model;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.internal.ui.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class BookmarkUtil {
	private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

	public static void parse(String fileName, Vector bookmarks) {
		File file = new File(fileName);
		if (!file.exists())
			return;

		try {
			documentBuilderFactory.setNamespaceAware(true);
			DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
			Document doc = parser.parse(file);
			Node root = doc.getDocumentElement();
			processRoot(root, bookmarks);
		} catch (ParserConfigurationException e) {
			UpdateUI.logException(e);
		} catch (SAXException e) {
			UpdateUI.logException(e);
		} catch (IOException e) {
			UpdateUI.logException(e);
		}
	}

	public static SiteBookmark[] getBookmarks(Vector bookmarks) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < bookmarks.size(); i++) {
			processEntry(bookmarks.get(i), result);
		}
		return (SiteBookmark[]) result.toArray(new SiteBookmark[result.size()]);
	}

	public static BookmarkFolder getFolder(Vector bookmarks, IPath path) {
		NamedModelObject object = find(bookmarks, path);
		if (object != null && object instanceof BookmarkFolder)
			return (BookmarkFolder) object;
		return null;
	}

	public static NamedModelObject find(Vector bookmarks, IPath path) {
		Object[] array = bookmarks.toArray();
		return find(array, path);
	}

	private static NamedModelObject find(Object[] array, IPath path) {
		String name = path.segment(0);
		for (int i = 0; i < array.length; i++) {
			NamedModelObject obj = (NamedModelObject) array[i];
			if (obj.getName().equals(name)) {
				if (obj instanceof BookmarkFolder) {
					if (path.segmentCount() > 1) {
						IPath childPath = path.removeFirstSegments(1);
						BookmarkFolder folder = (BookmarkFolder) obj;
						return find(folder.getChildren(null), childPath);
					}
				}
				return obj;
			}
		}
		return null;
	}

	private static void processRoot(Node root, Vector bookmarks) {
		if (root.getNodeName().equals("bookmarks")) { //$NON-NLS-1$
			NodeList children = root.getChildNodes();
			processChildren(children, null, bookmarks);
		}
	}
	private static void processChildren(
		NodeList children,
		BookmarkFolder folder,
		Vector bookmarks) {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			NamedModelObject object = null;
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("site")) { //$NON-NLS-1$
					object = createSite(child);

				} else if (child.getNodeName().equals("folder")) { //$NON-NLS-1$
					object = createFolder(child);
				}
			}
			if (object != null) {
				if (folder != null) {
					folder.addChild(object);
				} else {
					bookmarks.add(object);
				}
				object.setModel(model);
			}
		}
	}

	private static SiteBookmark createSite(Node child) {
		String name = getAttribute(child, "name"); //$NON-NLS-1$
		URL url = null;
		try {
			url = new URL(getAttribute(child, "url")); //$NON-NLS-1$
		} catch (MalformedURLException e) {
		}

		String web = getAttribute(child, "web"); //$NON-NLS-1$
		boolean webBookmark = (web != null && web.equals("true")); //$NON-NLS-1$

		String sel = getAttribute(child, "selected"); //$NON-NLS-1$
		boolean selected = (sel != null && sel.equals("true")); //$NON-NLS-1$

		SiteBookmark bookmark = new SiteBookmark(name, url, webBookmark, selected);

		String local = getAttribute(child, "local"); //$NON-NLS-1$
		bookmark.setLocal(local != null && local.equals("true")); //$NON-NLS-1$

		String ign = getAttribute(child, "ignored-categories"); //$NON-NLS-1$
		if (ign != null) {
			StringTokenizer stok = new StringTokenizer(ign, ","); //$NON-NLS-1$
			ArrayList array = new ArrayList();
			while (stok.hasMoreTokens()) {
				String tok = stok.nextToken();
				array.add(tok);
			}
			bookmark.setIgnoredCategories((String[]) array.toArray(new String[array.size()]));
		}
		return bookmark;
	}

	private static BookmarkFolder createFolder(Node child) {
		BookmarkFolder folder = new BookmarkFolder();
		String name = getAttribute(child, "name"); //$NON-NLS-1$
		folder.setName(name);
		if (child.hasChildNodes()) {
			NodeList children = child.getChildNodes();
			processChildren(children, folder, null);
		}
		return folder;
	}

	public static void store(String fileName, Vector bookmarks) {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		PrintWriter writer = null;
		try {
			fos = new FileOutputStream(fileName);
			osw = new OutputStreamWriter(fos, "UTF8"); //$NON-NLS-1$
			writer = new PrintWriter(osw);
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<bookmarks>"); //$NON-NLS-1$
			for (int i = 0; i < bookmarks.size(); i++) {
				Object obj = bookmarks.get(i);
				writeObject("   ", obj, writer); //$NON-NLS-1$
			}
			writer.println("</bookmarks>"); //$NON-NLS-1$
			writer.flush();
			writer.close();
		} catch (IOException e) {
		} finally {
			try {
				if (osw != null)
					osw.close();
			} catch (IOException e1) {
			}
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e2) {
			}
		}
	}
	private static void writeObject(
		String indent,
		Object obj,
		PrintWriter writer) {
		if (obj instanceof SiteBookmark) {
			SiteBookmark bookmark = (SiteBookmark) obj;
			String name = bookmark.getName();
			String url = bookmark.getURL().toString();
			String web = bookmark.isWebBookmark()?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
			String sel = bookmark.isSelected()?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
			String local = bookmark.isLocal() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
			String [] ign = bookmark.getIgnoredCategories();
			StringBuffer wign = new StringBuffer();
			for (int i = 0; i < ign.length; i++) {
				if (i > 0)
					wign.append(',');
				wign.append(ign[i]);
			}
			writer.print(indent + "<site name=\"" + name + "\" url=\"" + url + "\" web=\"" + web + "\" selected=\"" + sel + "\" local=\"" + local + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			if (wign.length() > 0)
				writer.print(" ignored-categories=\""+wign.toString()+"\""); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println("/>"); //$NON-NLS-1$
		} else if (obj instanceof BookmarkFolder) {
			BookmarkFolder folder = (BookmarkFolder) obj;
			String name = folder.getName();
			writer.println(indent + "<folder name=\"" + name + "\">"); //$NON-NLS-1$ //$NON-NLS-2$
			Object[] children = folder.getChildren(folder);
			String indent2 = indent + "   "; //$NON-NLS-1$
			for (int i = 0; i < children.length; i++) {
				writeObject(indent2, children[i], writer);
			}
			writer.println(indent + "</folder>"); //$NON-NLS-1$
		}
	}

	private static String getAttribute(Node node, String name) {
		NamedNodeMap atts = node.getAttributes();
		Node att = atts.getNamedItem(name);
		if (att != null) {
			return att.getNodeValue();
		}
		return ""; //$NON-NLS-1$
	}
	private static void processFolder(BookmarkFolder folder, ArrayList result) {
		Object[] children = folder.getChildren(folder);
		for (int i = 0; i < children.length; i++) {
			processEntry(children[i], result);
		}
	}
	private static void processEntry(Object obj, ArrayList result) {
		if (obj instanceof SiteBookmark)
			result.add(obj);
		else if (obj instanceof BookmarkFolder) {
			processFolder((BookmarkFolder) obj, result);
		}
	}
}
