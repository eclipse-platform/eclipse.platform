/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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

// Contains functions used by more than one view

function showAll() {
    var searchFrame = parent.parent.parent.parent.parent.HelpToolbarFrame.SearchFrame;
    if (searchFrame.getSearchWord) {
        searchFrame.location.replace("../scopeState.jsp?searchWord=" + searchFrame.getSearchWord() + "&workingSet=");
    }      
}

function rescope() {
    var searchFrame = parent.parent.parent.parent.parent.HelpToolbarFrame.SearchFrame;
    if (searchFrame.getSearchWord) {
        searchFrame.openAdvanced();
    }      
}