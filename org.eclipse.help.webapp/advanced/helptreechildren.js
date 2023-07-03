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

// Functions to update the nodes of a dynamic tree based
// on an XML dom. 

var selectedNode;
var highlightSelectedNode;

function updateTree(xml) { 
    var tocData = xml.documentElement;  
    if (tocData.tagName != "tree_data") {
        return;
    }      
    showErrors(xml);    
    var treeRoot = document.getElementById("tree_root");
    var nodes = tocData.childNodes;
    selectedNode = null;
    mergeChildren(treeRoot, nodes, 0);
    if (selectedNode !== null) {
        // Focusing on the last child will increase the chance that it is visible
        if (!highlightSelectedNode) {
            focusOnDeepestVisibleChild(selectedNode, false);
        }
        focusOnItem(selectedNode, highlightSelectedNode);
    }
 }
 
 function showErrors(xml) {
    var errorTags = xml.getElementsByTagName ("error");
    
    for (var i = 0; i < errorTags.length; i++) {
         var nextError = errorTags[i];
         var errorChildren = nextError.childNodes;
         // Is the next node a text node
         if (errorChildren.length > 0 && errorChildren[0].nodeType == 3) { 
             var message = errorChildren[0].data;
             alert(message);
         }
    }
    return errorTags.length > 0;
 }
 
function mergeChildren(treeItem, nodes, level) {
    var childContainer;
    if (treeItem.id == "tree_root") {
        childContainer=treeItem;
    } else {
        childContainer = findChild(treeItem, "DIV");
    }
    var childAdded = false;
    var hasPlaceholder = childContainer !== null && childContainer.className == "unopened";
    var existingChildren = hasExistingChildren(childContainer);
    var childCount = 0;
    var nodeIndex = 0;

    // Compute total # of nodes for accessibility attributes
    // nodes.length cannot be used because the list may contain xml elements
    // which are not nodes
        
    for (var i = 0; i < nodes.length; i++) {
        if (nodes[i].tagName == "node") {
            childCount++;
        }
     }
    if (nodes) {  
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            // If the children of this node have already been evaluated
            // and the child XML node has no children we can safely skip it 
            if (node.tagName == "node" && (!existingChildren || node.childNodes.length > 0)) {
                nodeIndex++;
                if (hasPlaceholder) {
                    // Remove the loading message
                    treeItem.removeChild(childContainer);
                    hasPlaceholder = false;
                    childContainer = null;
                }
                if (childContainer === null) {
                    childContainer = document.createElement("DIV");
                    childContainer.className = "group";              
                    setAccessibilityRole(childContainer, WAI_GROUP);
                    treeItem.appendChild(childContainer);
                }
                var id = node.getAttribute("id");  
                var childItem = null;
                var isLeaf = node.getAttribute("is_leaf");
                if (existingChildren) {
                    childItem = findChildById(childContainer, id);
                }
                if (childItem === null) {                    
                    var title = node.getAttribute("title");
                    var href = node.getAttribute("href");
                    var openImage = null;                
                    var closedImage = null; 
                    var imageAltText = "";
                    if (node.getAttribute("openImage")) {
                        openImage = "../topic" + node.getAttribute("openImage");
                        imageAltText = node.getAttribute("imageAlt");
                    } else {
                        openImage = node.getAttribute("image");
                        if (openImage) {
                            imageAltText = getAltText(openImage);
                            openImage = imagesDirectory + "/" + openImage + ".svg";
                        }
                    }              
                    if (node.getAttribute("closedImage")) {
                        closedImage = "../topic" + node.getAttribute("closedImage");
                    }             
                   childItem = addChild(childContainer, id, title, href, openImage, closedImage, imageAltText, isLeaf, nodeIndex, childCount, level + 1);           
                }
               
                if (!isLeaf) {
                    mergeChildren(childItem, node.childNodes, level + 1);
                } 
                var isSelected = node.getAttribute("is_selected");                   
                if (isSelected) {
                    selectedNode = childItem;
                    highlightSelectedNode = node.getAttribute("is_highlighted");
                }
                childAdded = true;
            }
        }   
     }

     if (childAdded) {
         // Expand this node if it was collapsed and has children in the xml tree        
        var childClass = getChildClass(treeItem);
        if (childClass == "hidden") {
            toggleExpandState(treeItem);
        } else {
            changeExpanderImage(treeItem, true); 
            setWAIExpansionState(treeItem, true);
        }
     }  
}

function hasExistingChildren(container) {
    var children = container.childNodes;
    if (children !== null) {
        for (var i = 0; i < children.length; i++) {
            if (children[i].nodeid) {
                return true;
            }
        }
    }
    return false;
}

function findChildById(treeItem, id) {
    var children = treeItem.childNodes;
    if (children !== null) {
        for (var i = 0; i < children.length; i++) {
            if (children[i].nodeid == id ) {
                return children[i];
            }
        }
    }
    return null;
}

// Create a child of treeItem
function addChild(treeItem, id, name, href, image, closedImage, imageAltText, 
                  isLeaf, position, setsize, level) {        
    var childItem = document.createElement("DIV");
    // roots should have a className of "root" to prevent indentation
    if (treeItem.id == "tree_root") {
        childItem.className = "root";
    } else {
        childItem.className = "visible"; 
    }
    childItem.nodeid = id;
    treeItem.appendChild(childItem);
    
    // Create a span to prevent line breaking
    var container = document.createElement("SPAN");
    container.className = "item";
    childItem.appendChild(container);
   
    var topicImage;
    if (image) {
        topicImage = document.createElement("IMG");
        //setImage(topicImage, image); 
        if (closedImage) {          
            topicImage.src = closedImage;
            topicImage.openImage = image;
            topicImage.closedImage = closedImage;
        } else {
            topicImage.src = image;
        }
        topicImage.alt = imageAltText;
    } 
    
    var topicName=document.createTextNode(name);
    
    if (showExpanders) {
        var plusMinusImage= document.createElement("IMG");
        plusMinusImage.className = "expander";
        setImage(plusMinusImage, "plus");
        if (isLeaf) {
            plusMinusImage.className = "h";
            plusMinusImage.alt = "";
        }
        container.appendChild(plusMinusImage);
    }
      
    var anchor = document.createElement("a");
    if (href === null) {
        anchor.className = "nolink";
    } else if (href.match(/^see:/)) {
         childItem.see = href.substring(4);
         anchor.className = "see";
    } else {
        anchor.href = href;
    }
    anchor.title = name;
    setAccessibilityRole(anchor, WAI_TREEITEM);    
    setAccessibilitySetsize(anchor, setsize); 
    setAccessibilityPosition(anchor, position); 
    setAccessibilityTreeLevel(anchor, level);
    
    if (topicImage) {
        anchor.appendChild(topicImage);
    }
    anchor.appendChild(topicName);
    container.appendChild(anchor);
    
    if (!isLeaf) {
        var innerDiv = document.createElement("DIV");  
        innerDiv.className = "unopened";
        setWAIExpansionState(anchor, false);
        childItem.appendChild(innerDiv);
    }
    return childItem;
}

function setLoadingMessage(treeItem, message) {   
    var placeholderDiv = findChild(treeItem, "DIV");
    if (placeholderDiv !== null && placeholderDiv.childNodes.length === 0) {      
        var msg = document.createTextNode(message);
        placeholderDiv.appendChild(msg);
    }
}
