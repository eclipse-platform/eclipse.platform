/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
 
 /*
Expands or contracts the control "expandable" to be the height of
the window less the size of fixed1, fixed2 and padding. fixed1 and fixed2 may be null
*/

function resizeVertical(expandableId, fixed1Id, fixed2Id, minSize, padding) {
    var isSafari = (navigator.userAgent.indexOf('Safari/') != -1);
    if (isSafari) padding += 20;  // newHeight gets computed too large on Safari	
	var newHeight;
	if (window.innerHeight) {
		newHeight = window.innerHeight;
	} else {
        newHeight = document.body.clientHeight;
	}
	if (fixed1Id) {
	    var fixed1 = document.getElementById(fixed1Id);
	    if (fixed1) newHeight -= fixed1.offsetHeight;
	}
	if (fixed2Id) {
	    var fixed2 = document.getElementById(fixed2Id);
	    if (fixed2) newHeight -= fixed2.offsetHeight;
	}
	if (padding) {
	    newHeight = newHeight - padding;
	}
	if (newHeight < minSize) {
	    newHeight = minSize;
	}
	var expandable = document.getElementById(expandableId);
	if (expandable) {
	    expandable.style.height = newHeight + "px";
	}
}