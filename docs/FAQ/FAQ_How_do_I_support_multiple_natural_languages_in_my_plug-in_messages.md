FAQ How do I support multiple natural languages in my plug-in messages?
=======================================================================

Almost all plug-ins in Eclipse use java.util.ResourceBundle, a messages.properties file, and look up messages by using a key. The MessageFormat class can be used to insert parameters into the translated message. Here is an example:

      String translate(String key, String[] parms) {
         try {
            ResourceBundle bundle = 
               ResourceBundle.getBundle("messages");
            String msg = bundle.getString(key);
            return MessageFormat.format(msg, parms);
         } catch (MissingResourceException e) {
            return key;
         }
      }

Eclipse includes special support to replace constant strings in your plug-in source code by equivalent Java code that uses key-based lookup. Execute the context menu option **Source > Externalize Strings...** and follow the instructions. To save memory, we recommend choosing a short prefix for the generated keys.

If you reject translation of a given string, the externalization tool will place comments like //$NON-NLS-1$ at the end of the line that contains the string. Otherwise, it will replace the string by the lookup code.

_Caveat_: In the current Eclipse distribution, the various plug-ins declare and ship around 30,000 property keys to support multiple languages. The bytes these keys occupy amount to roughly 8 percent of the uncompressed distribution size. But, more important, resource bundles are loaded whole, as in the preceding sample. This happens even when no string is ever loaded from it. Therefore, a large properties file can easily generate a lot of wasted space.

Conservative estimates have shown that a typical Eclipse launch uses upward of 1MB to store the keys in memory. This memory is used by the ResourceBundle implementation, which typically uses a hash table and by the classes that declare and pass a key to the resource bundle to translate the string. The keys are stored in the class files of the plug-in and saved somewhere in the JVM's data structures. In short, there is a big incentive to keep property keys short. For large offerings delivered on top of Eclipse, one should consider writing specialized ResourceBundle subclasses that use integer constants to lookup string bindings, for instance.

