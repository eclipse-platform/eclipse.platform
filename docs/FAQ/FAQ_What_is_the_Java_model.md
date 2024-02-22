FAQ What is the Java model?
===========================

The Java model is a hierarchical representation of the Java projects in your workspace. Figure 20.1 is a sample Spider graph of the Java model, showing the java.lang.VerifyError class and its two constructors.

Using a utility method provided by JavaCore, a Java model is obtained for the workspace. Using the IJavaModel, the Java projects can be inspected. Each project contains a classpath, consisting of multiple package fragment roots, such as source directories in the project or referenced JARs from required plug-ins. Each fragment contains a number of class files. An example of its use is given in the following piece of code:

      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IJavaModel javaModel = JavaCore.create(workspace.getRoot());
      IJavaProject projects[] = javaModel.getJavaProjects();
      for (int n = 0; n < projects.length; n++) {
         IJavaProject project = projects[n];
         IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
         int nClasses = 0;
         for (int k = 0; k < roots.length; k++) {
            IPackageFragmentRoot root = roots[k];
            IJavaElement[] elements = root.getChildren();
            for (int i = 0; i < elements.length; i++) {
               IJavaElement element = elements[i];
               PackageFragment fragment = (PackageFragment) element.getAdapter(PackageFragment.class);
               if (fragment == null) continue;
               IJavaElement fes[] = fragment.getChildren();
               for (int j = 0; j < fes.length; j++) {
                  String className = fes[j].getElementName();
                  nClasses++;
               }
            }
         }
         String projectName = projects[n].getElementName();
         System.out.println("Classpath for project "+ projectName +" contains "+nClasses+" classes.");
      }

The output of this code for a workspace with a single empty Java project is

	   Classpath for project P contains 12187 classes.

In other words, before you start adding your own classes or start referring to any other Eclipse plug-ins, you already have access to more than 12,000 classes in the Java 2, Standard Edition (J2SE) libraries.

The Java model is declared in package org.eclipse.jdt.core, which consists of 55 types providing access to anything you would ever need to analyze and manipulate your Java programs.

