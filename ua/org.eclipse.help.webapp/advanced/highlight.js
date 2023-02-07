/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

var isMozilla = navigator.userAgent.indexOf('Mozilla') != -1 && parseInt(navigator.appVersion.substring(0,1)) >= 5;
var isIE = navigator.userAgent.indexOf('MSIE') != -1;
var firstNodeHighlighted=false;
var firstNodeToBeHighlighted;
var topmostScroll;
var isSafari = (navigator.userAgent.indexOf('Safari/') != -1)
			|| (navigator.userAgent.indexOf('AppleWebKit/') != -1);
var highlighted=false;
var defaultHighlight;
var currentHighlight;
var startTime;
var MAX_DURATION=3000;
// w3c standard
if (window.addEventListener) {
    window.addEventListener("load", highlight, false);
}
// IE
else {
    window.attachEvent("onload", highlight);
}

var visited = 0;

function highlight(){
	if(highlighted){
		return;
	}
	highlighted=true;
	if (!document.body) return;
	if (parent.ContentToolbarFrame) {
		loadCookie();
		parent.ContentToolbarFrame.setButtonState("toggle_highlight",defaultHighlight);
	} else {
		defaultHighlight=pluginDefault;
	}
	if ((defaultHighlight == false) && document.styleSheets) {
		setRule(".resultofText","");
	}
	currentHighlight = defaultHighlight;
	
	if(document.body.innerHTML.length < 50000 && !isIE){
		for(i=0; i<keywords.length; i++){
		    firstNodeHighlighted=false;
			word=keywords[i].toLowerCase();
			highlightWordInNode(word, document.body);
			if(firstNodeHighlighted){
				var scroll=getVerticalScroll(firstNodeToBeHighlighted);
			}
			if (topmostScroll==null||topmostScroll>scroll){
				topmostScroll=scroll;
			}
		}
	} else {
	    visited = 0;
	    startTime=new Date().getTime();
		for(i=0; i<keywords.length && new Date().getTime() < startTime+MAX_DURATION; i++){
			word=keywords[i].toLowerCase();			
			highlightWordInNodeTimed(word, document.body);
			if(firstNodeHighlighted){
				var scroll=getVerticalScroll(firstNodeToBeHighlighted);
			}
			if (topmostScroll==null||topmostScroll>scroll){
				topmostScroll=scroll;
			}
		}	
	}
	scrollIntoView(topmostScroll);
}

function setRule(selector, css) {
	var theRules = new Array();
	for (var i = 0; i< document.styleSheets.length; i++) {
		if (document.styleSheets[i].cssRules)
			theRules = document.styleSheets[i].cssRules;
		else if (document.styleSheets[i].rules)
			theRules = document.styleSheets[i].rules;
		for (var j = theRules.length-1; j>=0; j--) {
			if (theRules[j].selectorText==selector) {
				theRules[j].style.cssText=css;
			}
		}
	}
}

function highlightWordInNode(aWord, aNode){
    if (aNode.nodeType == 1){
    	var children = aNode.childNodes;
    	for(var i=0; i < children.length; i++) {
    		highlightWordInNode(aWord, children[i]);
    	}
    }
    else if(aNode.nodeType==3){
    	highlightWordInText(aWord, aNode);
	}

}

function highlightWordInNodeTimed(aWord, aNode){
    if (aNode.nodeType == 1){
    	var children = aNode.childNodes;
    	for(var i=0; i < children.length; i++) {
    		highlightWordInNodeTimed(aWord, children[i]);
    		if (visited % 128 == 0) {
    		    // Getting the time is slow only check periodically
			    if(new Date().getTime()>startTime+MAX_DURATION) {
			        return;
			    }
			}
    	}
    }
    else if(aNode.nodeType==3){
    	highlightWordInText(aWord, aNode);
	}

}

function highlightWordInText(aWord, textNode){
	allText=new String(textNode.data);
	allTextLowerCase=allText.toLowerCase();
	index=allTextLowerCase.indexOf(aWord);
	if(index>=0){
		// create a node to replace the textNode so we end up
		// not changing number of children of textNode.parent
		replacementNode=document.createElement("span");
		textNode.parentNode.insertBefore(replacementNode, textNode);
		while(index>=0){
			before=allText.substring(0,index);
			newBefore=document.createTextNode(before);
			replacementNode.appendChild(newBefore);
			spanNode=document.createElement("span");
			spanNode.setAttribute("name","resultofMatch");
			if(isSafari){
				if (defaultHighlight == true) {
					spanNode.style.color="#000000";
					spanNode.style.background="#FFFF66";
				} else {
					spanNode.style.color=null;
					spanNode.style.background=null;
				}
			}else{
			    if ((defaultHighlight == false) && !document.styleSheets) {
				    if (isIE) spanNode.setAttribute("className",null);
				    else spanNode.setAttribute("class",null);
			    } else {
				    if (isIE) {
				        spanNode.className ="resultofText";
				    } else {
				        spanNode.setAttribute("class","resultofText");
				    }
			    }
			}

			replacementNode.appendChild(spanNode);
			if(!firstNodeHighlighted){
			firstNodeToBeHighlighted=spanNode;
			firstNodeHighlighted=true;
			}
			boldText=document.createTextNode(allText.substring(index,index+aWord.length));
			spanNode.appendChild(boldText);
			allText=allText.substring(index+aWord.length);
			allTextLowerCase=allText.toLowerCase();
			index=allTextLowerCase.indexOf(aWord);
		}
		newAfter=document.createTextNode(allText);
		replacementNode.appendChild(newAfter);
		textNode.parentNode.removeChild(textNode);
	}
}

/**
 * Scrolls the page to show the specified element
 */
function scrollIntoView(scroll)
{
	if (scroll != 0)
		window.scrollBy(0, scroll);
}

/**
 * Get the position of the specified element
 */
function getVerticalScroll(node)
{
	if(node==null)
	{
	 return 0;
	}
	
    var nodeTop =0;
    var nodeBottom=0;
	var pageBottom = 0;
	var nodeOffsetHeight=node.offsetHeight;
	
  try{
  
	  if(node.offsetParent) 
		 { 
				for( ; node.offsetParent; node = node.offsetParent ) 
			    {
					nodeTop += node.offsetTop; 
					
				} 
				
		 }else{
		 nodeTop = node.offsetTop;
		 }
		
	 } catch (e){}
	 
	 
	 nodeBottom = nodeTop + nodeOffsetHeight;
	 
	 if (isIE)
		{
	
		
		pageBottom = window.document.documentElement.clientHeight;
		
		if(pageBottom==0){
		
			pageBottom = window.document.body.clientHeight;
			
			}
	
		} else if (isMozilla)
			{
		
			pageBottom = window.innerHeight ;
			
			}
		
	var scroll = 0;
	
	if (nodeTop >= 0 )
	{
		if (nodeBottom <= pageBottom)
		{		
		  scroll = 0; // already in view		  
		} else {		
			scroll = nodeTop;			
			}
	} else	{
		scroll = nodeTop;
	 }
	
    return scroll;
	
}

function toggleHighlight() {
	setHighlight(currentHighlight == false);
}

function setHighlight(current) {
	currentHighlight = (current==true);
	if (isSafari) {
		var color;
		var backgnd;
		if (currentHighlight) {
			color = "#000000";
			backgnd = "#FFFF66";
		}
		else {
			color = null;
			backgnd = null;
		}
		var elements = document.getElementsByName("resultofMatch");
		for (var i = 0; i<elements.length; i++){
			elements[i].style.color=color;
			elements[i].style.backgroundColor= backgnd;
		}
	}
	else if (document.styleSheets){
		var text;
		if (currentHighlight) {
			text = "COLOR: #000000; BACKGROUND-COLOR: #FFFF66;";
		} else {
			text = "";
		}
		setRule(".resultofText",text);
	}
}

function loadCookie() {
	var i = document.cookie.indexOf("highlight");
	if (i != -1) {
		var result = document.cookie.substring(i+10);
		i = result.indexOf(";");
		if (i != -1) {
			result = result.substring(0,i);
		}
		defaultHighlight = new Boolean(result == "true");
	} else {
		defaultHighlight = new Boolean(true);
	}
}
