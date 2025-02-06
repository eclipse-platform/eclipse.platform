FAQ Which platforms support the SWT Browser, and which native renderers are available
=====================================================================================

The SWT Browser is currently available on all supported platforms. Available renderers and corresponding widget style flags are:

| Renderer              | Platform         | Style Flag     | Default |
| --------------------- | ---------------- | -------------- | ------- |
| Internet Explorer     | Windows          | `SWT.IE`       | Yes     |
| WebKit                | macOS, Linux GTK | `SWT.WEBKIT`   | Yes     |
| Edge (Chromium-based) | Windows          | `SWT.EDGE`     | No      |
| Chromium              | All              | `SWT.CHROMIUM` | No      |

_Note: As of Eclipse/SWT 4.8, Mozilla (`XULRunner`) renderer is no longer supported, `SWT.MOZILLA` flag is deprecated and has no effect._

Browser instances created with style `SWT.NONE` will use the default platform renderer according to the table above. The default renderer does not require additional software installation. It is possible to override the default native renderer. See [How do I specify the default type of native renderer that is used by the Browser](./FAQ-How-do-I-specify-the-default-type-of-native-renderer-that-is-used-by-the-Browser).

For additional information on specific renderers, see [How do I explicitly use Chromium as the Browser's underlying renderer](FAQ-How-do-I-explicitly-use-Chromium-as-the-Browser's-underlying-renderer) and [How do I explicitly use Edge as the Browser's underlying renderer](./FAQ-How-do-I-explicitly-use-Edge-as-the-Browser's-underlying-renderer).
