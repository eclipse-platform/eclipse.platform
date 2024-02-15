

FAQ How do I use image and font registries?
===========================================

With SWT, you are responsible for managing the lifecycle of such operating system resources as fonts, colors, and images. Most of the time, you can simply tie the lifecycle of these resources to the lifecycle of the widget using them. They can be allocated when a shell is opened, for example, and disposed of when the shell is disposed of.

Sometimes, a resource is used very frequently or must be shared among several widgets for unknown lengths of time. In these situations, the resources can be stored in a permanent registry. JFace provides three such registries: FontRegistry, ImageRegistry, and ColorRegistry. You can instantiate one of these registries yourself or use the global ones accessible from the JFaceResources class. The advantage to using a private instance is that you don't have to worry about your keys overlapping with the keys of another component. Here is an example of creating and accessing images in a private image registry:

      static final String IMAGE_ID = "my.image";
      ...
      //declaring the image
      ImageRegistry registry = new ImageRegistry();
      ImageDescriptor desc = ImageDescriptor.createFromURL(...);
      registry.put(IMAGE_ID, desc);
      ...
      //using the image
      Button button = new Button(...);
      Image image = registry.get(IMAGE_ID);
      button.setImage(image);

These registries create resources lazily as they are accessed, thus avoiding the expense of creating resources that may never be used. The resources are disposed of automatically when the display is disposed of.

A FontRegistry can also be loaded from a Java properties file, allowing you to specify different fonts, depending on the operating system, windowing system, or locale. See the javadoc for FontRegistry for more information on the format and naming conventions for these property files. Finally, the JFace font registry allows you to replace the font for a given key. This allows you to centralize the handling of user font changes around the font registry. When the user changes the font, you put the new value into the font registry. The registry then sends out notifications to registered listeners, allowing them to update their presentation with the new font.

Keep in mind that it is not practical to store all your resources in a permanent registry. Windowing systems generally limit the number of resource handles that can be in use. A large application with many resources can cause the windowing system to run out of resource handles, causing failures in your application or even in other applications.

See Also:
---------

*   [FAQ Why do I have to dispose of colors, fonts, and images?](./FAQ_Why_do_I_have_to_dispose_of_colors_fonts_and_images.md "FAQ Why do I have to dispose of colors, fonts, and images?")

