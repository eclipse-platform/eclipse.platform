

FAQ How do I create problem markers for my compiler?
====================================================

Adding problem markers for eScript compilation happens in two simple steps.

  

*   Right before we compile the resource, we remove all problem markers from the resource:

      void compileResource(IResource resource) {
         resource.deleteMarkers(IMarker.PROBLEM, 
            true, IResource.DEPTH_INFINITE);      
         doCompileResource(resource);
      }

  

*   During compilation, errors are attached to the resource as follows:

      void reportError(IResource resource, int line, String msg) {
         IMarker m = resource.createMarker(IMarker.PROBLEM);
         m.setAttribute(IMarker.LINE_NUMBER, line);
         m.setAttribute(IMarker.MESSAGE, msg);
         m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
         m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
      }

  
To simplify matters, we use the existing problem-marker type. See the online Eclipse article called _Mark My Words_, for an explanation how to declare your own marker types.

Simply by attaching a marker to a resource, the IDE will take care of placing visual indicators at the two indicator bars in the editor. The IDE will also add entries to the Problems view. If we indicated additional information in the marker for IMarker.CHAR_START and IMarker.CHAR_END, the editor will also draw a red squiggly line under the offending problem. Figure 19.4 shows the result of a compilation of a problematic eScript file.

See Also:
---------

  \* [FAQ How do I implement Quick Fixes for my own language?](./FAQ_How_do_I_implement_Quick_Fixes_for_my_own_language.md "FAQ How do I implement Quick Fixes for my own language?")
  \* [Mark My Words](https://www.eclipse.org/articles/Article-Mark%20My%20Words/mark-my-words.html)


