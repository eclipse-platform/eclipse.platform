FAQ: How do I create a Rich Client Application?
===============================================

An Eclipse RCP application in Eclipse 4 (e4) leverages the application model for defining the user interface and behavior. 
Unlike Eclipse 3.x, where WorkbenchAdvisor played a central role, Eclipse 4 RCP applications are more declarative, using application models and dependency injection.

## Step 1: Set Up Your Project

Create an Eclipse 4 RCP Project: Use the Eclipse IDE to create a new Eclipse 4 RCP project via the wizard, which sets up the basic structure and required dependencies for an e4 application.

## Step 2: Define the Application Model

The application model defines the UI structure (views, menus, toolbars, etc.) and is typically designed within the Eclipse IDE using the Application Model Editor.

Open the Application Model Editor: Navigate to the Application.e4xmi file in your project.
Design Your UI: Use the editor to add and configure parts, perspectives, menus, and other UI components.

## Step 3: Implement Business Logic

In Eclipse 4, you implement your application's business logic in classes annotated for dependency injection (e.g., @Inject for services or UI components).

Example of an Eclipse 4 RCP Application Entry Point for a handler called via a menu.

      import org.eclipse.e4.core.di.annotations.Execute;
      import org.eclipse.swt.widgets.Shell;

      public class Application {
         
         @Execute
         public void run(Shell shell) {
            // Your application logic here
         }
      }

The @Execute annotation marks the method to be run once the menu entry connnected with this handler is called.

## Step 4: Configure Your Application in the plugin.xml

Unlike Eclipse 3.x, most of the configuration in Eclipse 4 is done through the application model (Application.e4xmi). However, you still need to define your application's ID and point to the application model in your plugin.xml:

      <extension
         id="application"
         point="org.eclipse.core.runtime.applications">
      <application>
            <run class="org.eclipse.e4.ui.internal.workbench.swt.E4Application">
                  <parameter
                        name="org.eclipse.e4.ui.workbench.swt.E4Application"
                        value="path/to/your/Application.e4xmi">
                  </parameter>
            </run>
      </application>
      </extension>

Make sure to replace "path/to/your/Application.e4xmi" with the actual path to your application model file.

## Conclusion
Eclipse 4 RCP development focuses on the application model and dependency injection, providing a more flexible and modular approach to building rich client applications compared to the traditional Eclipse 3.x RCP. The rest of your RCP application development involves creating and populating perspectives, views, and other UI components using the Eclipse 4 application model.  

See Also:
---------

[FAQ\_How\_do\_I\_create\_a\_new_perspective?](./FAQ_How_do_I_create_a_new_perspective.md "FAQ How do I create a new perspective?")

[FAQ\_How\_do\_I\_create\_an\_application?](./FAQ_How_do_I_create_an_application.md "FAQ How do I create an application?")

[Building Eclipse RCP applications](https://www.vogella.com/tutorials/EclipseRCP/article.html)
