/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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

// Utility functions used by multiple jsp pages. This contains most of
// the functions that have different behavior on different browsers

// Constants used in flags, the const keyword is not supported in IE 6 and earlier
// so they are declared as var

var SCROLL_LEFT = 1;
var SCROLL_RIGHT = 2;
var SCROLL_UP = 4;
var SCROLL_DOWN = 8;
var SCROLL_HORIZONTAL = SCROLL_LEFT + SCROLL_RIGHT;
var SCROLL_VERTICAL = SCROLL_UP + SCROLL_DOWN;
var SCROLL_HORIZONTAL_AND_VERTICAL = SCROLL_HORIZONTAL + SCROLL_VERTICAL;

var isInternetExplorer = navigator.userAgent.indexOf('MSIE') != -1;
var isSafari = (navigator.userAgent.indexOf('Safari/') != -1)
			|| (navigator.userAgent.indexOf('AppleWebKit/') != -1);

/**
 * Scrolls the page to show the specified element
 * If the entire element can be show scroll the minimum amount necessary to 
 * show the entire element.
 * If the element is larger than the client area align it at the top or left of the page.
 */
function scrollUntilVisible(node, flags)
{
    if (node === null) { 
        return; 
    }
	var nodeTop = node.offsetTop;
	var nodeBottom = nodeTop + node.offsetHeight;
	var nodeLeft = node.offsetLeft;
	var nodeRight = nodeLeft + node.offsetWidth;
	var pageTop = 0;
	var pageBottom = 0;
	var pageLeft = 0;
	var pageRight = 0;
	var isScrollLeft = flags & SCROLL_LEFT;
	var isScrollRight = flags & SCROLL_RIGHT;
	var isScrollUp = flags & SCROLL_UP;
	var isScrollDown = flags & SCROLL_DOWN;
	
	if (isInternetExplorer)
	{
		pageTop = document.body.scrollTop; 
		pageBottom = pageTop + document.body.clientHeight;	
		pageLeft = document.body.scrollLeft;
		pageRight = document.body.scrollRight;
	} 
	else 
	{
		pageTop = window.pageYOffset;
		pageBottom = pageTop + window.innerHeight - node.offsetHeight;
		pageLeft = window.pageXOffset;
		pageRight = pageLeft + window.innerWidth - node.offsetWidth;
	}
	
	var vScroll = 0;
	var hScroll = 0;
	
	if (flags & SCROLL_VERTICAL) {
	    if (nodeTop < pageTop && isScrollUp) {
	        // Scroll up so node is at the top of the view
		    vScroll = nodeTop - pageTop;
	    } else {
	        if (nodeBottom > pageBottom && isScrollDown) {	
		        vScroll = Math.min(nodeTop - pageTop, nodeBottom - pageBottom);
		    } else {
		        vScroll = 0; // already in view
		    } 
	    }
	}
	
	if ((flags & SCROLL_HORIZONTAL ) && isRTL) {
	    // In RTL mode aligning the right edge is more important than aligning the left
	    if (nodeRight > pageRight && isScrollRight) {
		    hScroll = nodeRight - pageRight; 
	    } else {            
	        if (nodeLeft < pageLeft && isScrollLeft) {
		        hScroll = Math.max(nodeLeft - pageLeft, nodeRight - pageRight);
		    } else {
		        hScroll = 0; // already in view
		    } 	
	    }
    } else if (flags & SCROLL_HORIZONTAL ) {
	    if (nodeLeft < pageLeft && isScrollLeft) {
		    hScroll = nodeLeft - pageLeft; 
	    } else {
	        if (nodeRight > pageRight && isScrollRight) {
		        hScroll = Math.min(nodeLeft - pageLeft, nodeRight - pageRight);
		    } else {
		        hScroll = 0; // already in view
		    } 	
	    }
	}
		
	if (hScroll != 0 || vScroll != 0) {	
	    window.scrollBy(hScroll, vScroll);
	}
}

function cancelEventBubble(e) {
  	if (isInternetExplorer) {
  		window.event.cancelBubble = true; 	 
  	} 
  	if (e && e.preventDefault) {
  		e.preventDefault();
    }
}

/**
 * Returns the target node of an event
 */
function getEventTarget(e) {
	var target;
  	if (isIE) {
   		target = window.event.srcElement; 
   	} else {  	
  		target = e.target;
  	}

	return target;
}

function getKeycode(e) {
    if (isInternetExplorer) {
		return window.event.keyCode;
	} else {
		return e.keyCode;
	}
}

function getCookie(name) {
	var nameEquals = name + "=";
	var cookies = document.cookie.split(";");
	for (var i=0;i<cookies.length;++i) {
		var cookie = cookies[i];
		if (cookie.charAt(0) == ' ') {
			cookie = cookie.substring(1, cookie.length);
		}
		if (cookie.indexOf(nameEquals) == 0) {
			return cookie.substring(nameEquals.length, cookie.length);
		}
	}
	return null;
}

function setCookie(name, value) {
	var date = new Date();
	date.setTime(date.getTime()+(365*24*60*60*1000));
	var path = window.location.pathname;
	var cookiePath = "/help/";
	var slash = path.indexOf('/', 1);
	if(slash > 0) {
	   cookiePath = path.substr(0, slash + 1); 
	}			
	document.cookie = name + "=" + value + "; expires=" + date.toGMTString() + ";path=" + cookiePath;
}

function setCSSRule(selector, property, value) {
	var theRules = new Array();
	for (var i=0;i<document.styleSheets.length;i++) {
		if (document.styleSheets[i].cssRules) {
			theRules = document.styleSheets[i].cssRules;
		}
		else {
			theRules = document.styleSheets[i].rules;
		}
		for (var j=theRules.length-1;j>=0;j--) {
			if (theRules[j].selectorText == selector) {
				theRules[j].style[property] = value;
				return;
			}
		}
	}
}
