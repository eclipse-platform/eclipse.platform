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

import java.util.ArrayList;

import org.eclipse.jface.text.IDocument;

/**
 * No API yet.
 */
public class DocumentManager {
	
	private static final boolean DEBUG= false;
	
	private static ArrayList fgKeys= new ArrayList();
	private static ArrayList fgValues= new ArrayList();
	
	public static IDocument get(Object o) {
		
		for (int i= 0; i < fgKeys.size(); i++) {
			if (fgKeys.get(i) == o)
				return (IDocument) fgValues.get(i);
		}
		return null;
	}
	
	public static void put(Object o, IDocument document) {
		if (DEBUG) System.out.println("DocumentManager.put: " + document);	//$NON-NLS-1$
		for (int i= 0; i < fgKeys.size(); i++) {
			if (fgKeys.get(i) == o) {
				fgValues.set(i, document);
				return;
			}
		}
		fgKeys.add(o);
		fgValues.add(document);	
	}
	
	public static void remove(IDocument document) {
		if (document != null) {
			if (DEBUG) System.out.println("DocumentManager.remove: " + document);	//$NON-NLS-1$
			for (int i= 0; i < fgValues.size(); i++) {
				if (fgValues.get(i) == document) {
					fgKeys.remove(i);
					fgValues.remove(i);
					return;
				}
			}
			if (DEBUG) System.out.println("DocumentManager.remove: not found");	//$NON-NLS-1$
		}
	}
	
	public static void dump() {
		if (DEBUG) System.out.println("DocumentManager: managed docs:" + fgValues.size());	//$NON-NLS-1$
	}
}
