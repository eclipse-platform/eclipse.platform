/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
 
 var lastView = 'toc';

function toggleTitleView(button){
	//window.parent.parent.toggleShowAll();
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
	var navFrame = parent.parent.NavFrame;

	if (isOn()) {
	    setButtonState("tsearch", false);    
	    navFrame.showView("toc");
	} else {	    
	    setButtonState("tsearch", true);
	    navFrame.showView("titlesearch");	
	}
}

function isOn() {
    var control = document.getElementById("tdb_tsearch");
	if(!control) {	    
	    alert('No Control found' + buttonName);
		return false;
	}
	return control.className  == "buttonOn";
}
