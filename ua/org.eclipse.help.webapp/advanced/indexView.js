/*******************************************************************************
 * Copyright (c) 2005, 2010 Intel Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation 2006, refactored index view into a single frame
 *     IBM Corporation 2007, allow partial loading of index
 *     IBM Corporation 2010, add filtering in toc and index view
 *******************************************************************************/
 
var isMozilla = navigator.userAgent.indexOf("Mozilla") != -1 && parseInt(navigator.appVersion.substring(0,1)) >= 5;
var isIE = navigator.userAgent.indexOf("MSIE") != -1;
var showExpanders = false;
var shown = false;
var typeinPrevious = "";
var typein;
var lines = 30;
var firstEntry;
var lastEntry;
var showAllChanged = false;
var showAll;

/**
 * Set value of the typein input field.
 * The value can be anchor's id or anchor's text.
 */

function sizeList() {
    resizeVertical("indexList", "typeinTable", "navigation", 100, 5);
}

function computeLines() {
    // Compute the number of lines available using the placeholder
    try {
        var indexList = document.getElementById("indexList");
        var placeholder = document.getElementById("placeholder");
        // Add 1 to placeholder height allow for margin
	    var ratio = indexList.offsetHeight / ( placeholder.offsetHeight + 1);
	    lines = Math.floor(ratio);
	    if (lines < 1) {
	        lines = 1;
	    }
	} catch(ex) {}
}

/*
 * Called when the "DISPLAY" button is clicked
 */
function showIndex() {
   loadChildren(typein.value);
}

function isVisible() 
{
    var visibility = parent.parent.getVisibility("index");
    return visibility == "visible";
}


function onloadHandler() {

    setRootAccessibility();
	typein = document.getElementById("typein");

	typein.value = "";
	typeinPrevious = "";
	
	if (isIE) {
		document.onclick = treeMouseClickHandler;
		document.onkeydown = keyDownHandler;
	} else {
		document.addEventListener('click', treeMouseClickHandler, true);
		document.addEventListener('keydown', keyDownHandler, true);
	}
		
	setInterval("intervalHandler()", 200);
	sizeList();
	if (isVisible()) {
	    onShow();
	}
}

function setImage(imageNode, image) {
    var imageFile = imagesDirectory + "/" + image + ".svg";
    imageNode.src = imageFile;
    if (image == "plus") {
        imageNode.alt = altPlus;       
    } else if (image == "minus") {
        imageNode.alt = altMinus;     
    } else if (image == "toc_open") {
        imageNode.alt = altBookOpen;    
    } else if (image == "toc_closed") {
        imageNode.alt = altBookClosed;  
    } else if (image == "container_obj") {
        imageNode.alt = altContainer;  
    } else if (image == "container_topic") {
        imageNode.alt = altContainerTopic;  
    } else if (image == "topic") {
        imageNode.alt = altTopic;
    } else {
        imageNode.alt = "";
    }
}

function updateImage(imageNode, isExpanded) {
    var src = imageNode.src;
    if (isExpanded) {   
        if (src.match( /toc_closed.svg$/)) {
            setImage(imageNode, "toc_open");
        }
    } else {
        if (src.match( /toc_open.svg$/)) {           
            setImage(imageNode, "toc_closed");
        }
    }
}

/*
Remove any existing children and read new ones
*/

function loadChildren(startCharacters, mode, entry) { 
    var parameters = "";
    var treeRoot = document.getElementById("tree_root");
    if (treeRoot !== null) {
        while (treeRoot.childNodes.length > 0) {
            treeRoot.removeChild(treeRoot.childNodes[0]);
        }
        var placeholder = document.createElement("DIV");
        placeholder.className = "unopened";
        placeholder.id = "placeholder";
        treeRoot.appendChild(placeholder);
        setLoadingMessage(treeRoot, loadingMessage);
        computeLines();
        var separator = "?";
        if (startCharacters) {    
            parameters += "?start=";
            parameters += encodeURIComponent(startCharacters);
            separator = "&";
        }
        if (lines) {
            parameters += separator;
            parameters += "size=";
            parameters += lines;
            separator = "&";
        }
        if (mode) {
            parameters += separator;
            parameters += "mode=";
            parameters += mode;
            separator = "&";
        }
        if (entry) {
            parameters += separator;
            parameters += "entry=";
            parameters += entry;
            separator = "&";
        }
        if (showAllChanged) {
            showAllChanged = false;
            parameters += separator;
            parameters += "showAll="
            if (showAll) {
                parameters += "on";
            } else {
                parameters += "off";
            }
        }
        makeNodeRequest(parameters);
    }
}

function updateIndexTree(xml) {
    updateTree(xml);
    removePlaceholder();
    // Enable or disable the buttons
    var node = xml.documentElement;  
    var previous = document.getElementById("previous");
    var enablePrevious = node.getAttribute("enablePrevious");
    if (enablePrevious == "false") {
        previous.className = "h";
    } else {
        previous.className = "enabled";
    }
    var next = document.getElementById("next");
    var enableNext = node.getAttribute("enableNext");
    if (enableNext == "false") {
        next.className = "h";
    } else {
        next.className = "enabled";
    }
}

function removePlaceholder() {
    var treeRoot = document.getElementById("tree_root");
    if (treeRoot == null) return; 
    var placeholderDiv = findChild(treeRoot, "DIV");
    if (placeholderDiv && placeholderDiv.className == "unopened") {
        treeRoot.removeChild(placeholderDiv);
    }
}

function makeNodeRequest(parameters) {
    var href = "indexfragment" + parameters;
    var callback = function(xml) { updateIndexTree(xml);}; 
    var errorCallback = function() { 
        // alert("ajax error"); 
    };
    ajaxRequest(href, callback, errorCallback);
}

// Cache the first and last so that if a request fails we don't lose our place
function getFirstAndLast() {
    var treeRoot = document.getElementById("tree_root");
    if (treeRoot == null) return; 
    var firstDiv = findChild(treeRoot, "DIV");
    if (firstDiv && firstDiv.nodeid) {
        firstEntry = firstDiv.nodeid.substring(1);
    }
    var lastDiv = findLastChild(treeRoot, "DIV");
        
    if (lastDiv && lastDiv.nodeid) {
        lastEntry = lastDiv.nodeid.substring(1);
    }
}

function loadPreviousPage() {
    getFirstAndLast();
    if (firstEntry) {
        loadChildren("", "previous", firstEntry);
    } else {
        loadChildren("");
    }
}
       
function loadNextPage() {
    getFirstAndLast();
    if (lastEntry) {
        loadChildren("", "next", lastEntry);
    } else {
        loadChildren("");
    }
}
       
function loadCurrentPage() {
    getFirstAndLast();
    if (firstEntry && firstEntry > 0) {
        loadChildren("", "next", firstEntry - 1);
    } else {
        loadChildren("");
    }
}
       
function onShow() { 
	sizeList();
    if (!shown) {
        // View is being shown for the first time
        loadChildren("");
        shown = true;
    }  
}

function setShowAll(isShowAll) {
    showAll = isShowAll;
    showAllChanged = true;
    if (shown) {
        // Only refresh if we are already showing       
        loadCurrentPage();
    }
}

/*
 * Function called when the typein value may have changed
 */
 
function typeinChanged() {
    if (typein.value == typeinPrevious) {
        return;
    }
    typeinPrevious = typein.value;
    loadChildren(typeinPrevious);
}

/*
 * Handler for key down 
 */
function keyDownHandler(e)
{
    var key = getKeycode(e);
    if (key == 33) {
        // Page up  
        cancelEventBubble(e);
        loadPreviousPage(); 
        return;
    }
    if (key == 34) {
        // Page down 
        cancelEventBubble(e);
        loadNextPage(); 
        return; 
    }
	var clickedNode = getEventTarget(e);
	if (clickedNode && clickedNode.id == "typein") {
	    typeinKeyDownHandler(e);
	} else {
	    treeKeyDownHandler(e);
	}
}

function typeinKeyDownHandler(e) {
	var key = getKeycode(e);	

	if (key == 13) { // enter
	    typeinChanged();
	    cancelEventBubble(e);
	} else {
	    return treeKeyDownHandler(e);
	}

	return false;
}

function handleSee(target) {
    var pathSegments = target.split(", ");
    typeinPrevious = null;
    typein.value = pathSegments[0];
    typeinChanged();
}

function repaint() {
    var href = "indexView.jsp";
    location.replace(href);
}

/**
  * Select the corresponding item in the index list on typein value change.
  * Check is performed periodically in short interval.
  */
function intervalHandler() {
    typeinChanged();
}
