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
 
 var lastView = "";
/**
 * Switches to specified view
 */
function showView(view)
{ 	
	if (view == lastView) 
		return;
		
	lastView = view;
       	
	// show appropriate frame
 	var iframes = parent.ViewsFrame.document.body.getElementsByTagName("IFRAME");
 	for (var i=0; i<iframes.length; i++)
 	{			
  		if (iframes[i].id != view){
   			iframes[i].className = "hidden";
   			iframes[i].style.visibility="hidden";
  		}else{
   			iframes[i].className = "visible";
   			iframes[i].style.visibility="visible";
   			try {
   			    iframes[i].contentWindow.onShow();
   		    } catch(ex) {}
   		}
 	}
}

function getVisibility(view) {
    var iframes = parent.ViewsFrame.document.body.getElementsByTagName("IFRAME");
    for (var i=0; i<iframes.length; i++)
 	{			
  		if (iframes[i].id == view){
   			 return iframes[i].className;
   		}
 	}
}

var regExp=/&(showAll|synch)=(on|off|yes|no)/gi;
function toggleShowAll(){
	if(activityFiltering){
		if( displayShowAllConfirmation ){
			confirmShowAll();
		}else{
			showAll();
		}
	} else {
		dontShowAll();
	}
}

function dontAskAgain(){
	displayShowAllConfirmation = false;
}
function showAll(){
	var displayConfirmParam;
	if(displayShowAllConfirmation){
		displayConfirmParam="";
	}else{
		displayConfirmParam="&showconfirm=false";
	}
	activityFiltering=false;
	try{
		window.frames.toc.tocToolbarFrame.setButtonState("show_all", true);
	}catch(ex) {}
	try{
	    window.frames.index.indexToolbarFrame.setButtonState("show_all", true);
	}catch(ex) {}
	try{
		window.frames.search.searchToolbarFrame.setButtonState("show_all", true);
	}catch(ex) {}
	try{
		var newUrl = window.frames.toc.tocViewFrame.location.href.replace(regExp, "")+"&showAll=on"+displayConfirmParam;
	    window.frames.toc.tocViewFrame.setShowAll(true, newUrl);
	}catch(ex) {}
	try{
	    window.frames.index.indexViewFrame.setShowAll(true);
	}catch(ex) {}
	try{
		window.frames.search.searchViewFrame.location.replace(window.frames.search.searchViewFrame.location.href.replace(regExp, "")+"&showAll=on");
	}catch(ex) {}
}

function rescope(scopes) {
     window.location.replace("views.jsp?scope=" + scopes);
     try{
         window.frames.toc.tocViewFrame.collapseAll();
     }catch(ex) {}
     try{
         window.frames.index.indexViewFrame.setShowAll(true);
     }catch(ex) {}
}

function dontShowAll(){
	activityFiltering=true;
	try{
		window.frames.toc.tocToolbarFrame.setButtonState("show_all", false);
	}catch(ex) {}
	try{		
	    window.frames.index.indexToolbarFrame.setButtonState("show_all", false);
	}catch(ex) {}
	try{
		window.frames.search.searchToolbarFrame.setButtonState("show_all", false);
	}catch(ex) {}
	try{
		var newUrl = window.frames.toc.tocViewFrame.location.href.replace(regExp, "")+"&showAll=off";
	    window.frames.toc.tocViewFrame.setShowAll(false, newUrl);
	}catch(ex) {}
	try{		
	    window.frames.index.indexViewFrame.setShowAll(false);
	}catch(ex) {}
	try{
		window.frames.search.searchViewFrame.location.replace(window.frames.search.searchViewFrame.location.href.replace(regExp, "")+"&showAll=off");
	}catch(ex) {}
}

function closeConfirmShowAllDialog(){
	try {
		if (confirmShowAllDialog){
			confirmShowAllDialog.close();
		}
		if (selectScopeDialog) {
		    selectScopeDialog.close();
		}
	}
	catch(e) {}
}

var confirmShowAllDialog;
var selectScopeDialog;
var w = 470;
var h = 240;