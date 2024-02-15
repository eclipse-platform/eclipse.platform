

FAQ How do I implement an incremental project builder?
======================================================

To implement an incremental project builder, you first have to create an extension for org.eclipse.core.resources.builders:

      <extension 
            id="Builder" 
            name="eScript Builder" 
            point="org.eclipse.core.resources.builders">
         <builder>
            <run class="org.eclipse.escript.builder.Builder">
               <parameter name="optimize" value="true"/>
               <parameter name="comment" value="escript Builder"/>
            </run>
         </builder>
      </extension>

  
The second step is to create a builder class that must extend the abstract IncrementalProjectBuilder superclass:

      public class Builder extends IncrementalProjectBuilder {   
         protected IProject[] build(int kind, Map args, 
         IProgressMonitor monitor) {
            if (kind == IncrementalProjectBuilder.FULL_BUILD) {
               fullBuild(monitor);
            } else {
               IResourceDelta delta = getDelta(getProject());
               if (delta == null) {
                  fullBuild(monitor);
               } else {
                  incrementalBuild(delta, monitor);
               }
            }
            return null;
         }   
         private void incrementalBuild(IResourceDelta delta, 
         IProgressMonitor monitor) {
            System.out.println("incremental build on "+delta);
            try {
               delta.accept(new IResourceDeltaVisitor() {
                  public boolean visit(IResourceDelta delta) {
                     System.out.println("changed: "+
                     delta.getResource().getRawLocation());
                     return true; // visit children too
                  }
               });
            } catch (CoreException e) {
               e.printStackTrace();
            }
         }
         private void fullBuild(IProgressMonitor monitor) {
            System.out.println("full build");
         }
      }

It is important to return true in the visit method for those folders that contain the resources of interest. If you return false, the children of the resource delta are not visited.

See Also:
---------

[FAQ How do I handle setup problems for a given builder?](./FAQ_How_do_I_handle_setup_problems_for_a_given_builder.md "FAQ How do I handle setup problems for a given builder?")

