

FAQ How do I create an image registry for my plug-in?
=====================================================

If you're writing a plug-in with UI components, it should be a subclass of AbstractUIPlugin. This superclass already provides you with an empty image registry accessible by calling getImageRegistry. When the registry is first accessed, the hook method initializeImageRegistry will be called. You should override this method to populate your image registry with the image descriptors you need. You don't have to use this registry if you don't need it, and because it is created lazily on first access, there is no performance overhead if you never use it. Here is an example of a plug-in that adds a sample.gif image to its image registry:

      public class ExamplesPlugin extends AbstractUIPlugin {
         public static final String PLUGIN_ID = "org.eclipse.faq.examples";
         public static final String IMAGE_ID = "sample.image";
         ...
         protected void initializeImageRegistry(ImageRegistry registry) {
            Bundle bundle = Platform.getBundle(PLUGIN_ID);
            IPath path = new Path("icons/sample.gif");
            URL url = FileLocator.find(bundle, path, null);
            ImageDescriptor desc = ImageDescriptor.createFromURL(url);
            registry.put(IMAGE_ID, desc);
         }
      }

  

### Comments:

This FAQ seems misleading given the API comment on AbstractUIPlugin.getImageRegistry():

The image registry contains the images used by this plug-in that are very 
frequently used and so need to be globally shared within the plug-in. Since 
many OSs have a severe limit on the number of images that can be in memory at 
any given time, a plug-in should only keep a small number of images in their 
registry.

See Also:
---------

*   [FAQ How do I use image and font registries?](./FAQ_How_do_I_use_image_and_font_registries.md "FAQ How do I use image and font registries?")
*   [FAQ How do I use images defined by other plug-ins?](./FAQ_How_do_I_use_images_defined_by_other_plug-ins.md "FAQ How do I use images defined by other plug-ins?")

