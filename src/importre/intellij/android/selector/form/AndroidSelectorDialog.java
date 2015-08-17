package importre.intellij.android.selector.form;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class AndroidSelectorDialog extends DialogWrapper {
    private final String drawableDir = "drawable";
    private final String drawableV21Dir = "drawable-v21";
    private final VirtualFile dir;
    private final Project project;

    private JPanel contentPane;
    private JTextField colorText;
    private JTextField colorPressed;
    private JTextField colorPressedV21;
    private JTextField filenameText;

    public AndroidSelectorDialog(@Nullable Project project, VirtualFile dir) {
        super(project);

        this.project = project;
        this.dir = dir;
        setTitle("Android Selector");
        setResizable(false);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        String f = filenameText.getText();
        final String filename = (f.endsWith(".xml") ? f : f + ".xml").trim();
        final String color = colorText.getText().trim();
        final String pressed = colorPressed.getText().trim();
        final String pressedV21 = colorPressedV21.getText().trim();

        if (!valid(filename, color, pressed, pressedV21)) {
            String title = "Invalidation";
            String msg = "color, pressed, pressedV21 must start with `@color/` or `@drawable`";
            Messages.showMessageDialog(project, msg, title, Messages.getErrorIcon());
            return;
        }

        if (exists(filename)) {
            String title = "Cannot create files";
            String msg = String.format(Locale.US, "`%s` already exists", filename);
            Messages.showMessageDialog(project, msg, title, Messages.getErrorIcon());
            return;
        }

        Application app = ApplicationManager.getApplication();
        app.runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    createDrawable(filename, color, pressed);
                    createDrawableV21(filename, color, pressedV21);
                } catch (Exception ignored) {
                }
            }
        });
        super.doOKAction();
    }

    private boolean valid(String filename, String color,
                          String pressed, String pressedV21) {
        if (filename.isEmpty() || ".xml".equals(filename))
            return false;

        String regex = "^@(color|drawable)/.+";
        return color.matches(regex) ||
                pressed.matches(regex) ||
                pressedV21.matches(regex);
    }

    private boolean exists(String filename) {
        String[] dirs = new String[]{drawableDir, drawableV21Dir};
        for (String d : dirs) {
            VirtualFile f = dir.findChild(d);
            if (f != null && f.isDirectory()) {
                VirtualFile dest = f.findChild(filename);
                if (dest != null && dest.exists()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void createDrawable(String filename,
                                String color,
                                String pressed) throws Exception {
        VirtualFile child = dir.findChild(drawableDir);
        if (child == null) {
            child = dir.createChildDirectory(null, drawableDir);
        }

        VirtualFile newXmlFile = child.findChild(filename);
        if (newXmlFile != null && newXmlFile.exists()) {
            newXmlFile.delete(null);
        }
        newXmlFile = child.createChildData(null, filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        String nsUri = "http://www.w3.org/2000/xmlns/";
        String androidUri = "http://schemas.android.com/apk/res/android";
        Document doc = builder.newDocument();
        Element root = doc.createElement("selector");
        root.setAttributeNS(nsUri, "xmlns:android", androidUri);
        doc.appendChild(root);

        Element item = doc.createElement("item");
        item.setAttribute("android:drawable",
                "@drawable/abc_list_selector_disabled_holo_light");
        item.setAttribute("android:state_enabled", "false");
        item.setAttribute("android:state_focused", "true");
        item.setAttribute("android:state_pressed", "true");
        root.appendChild(item);

        item = doc.createElement("item");
        item.setAttribute("android:drawable",
                "@drawable/abc_list_selector_disabled_holo_light");
        item.setAttribute("android:state_enabled", "false");
        item.setAttribute("android:state_focused", "true");
        root.appendChild(item);

        item = doc.createElement("item");
        item.setAttribute("android:drawable", pressed);
        item.setAttribute("android:state_focused", "true");
        item.setAttribute("android:state_pressed", "true");
        root.appendChild(item);

        item = doc.createElement("item");
        item.setAttribute("android:drawable", pressed);
        item.setAttribute("android:state_focused", "false");
        item.setAttribute("android:state_pressed", "true");
        root.appendChild(item);

        item = doc.createElement("item");
        item.setAttribute("android:drawable", color);
        root.appendChild(item);

        OutputStream os = newXmlFile.getOutputStream(null);
        PrintWriter out = new PrintWriter(os);

        StringWriter writer = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        out.println(writer.getBuffer().toString());
        out.close();
    }

    private void createDrawableV21(String filename,
                                   String color,
                                   String pressed) throws Exception {
        VirtualFile child = dir.findChild(drawableV21Dir);
        if (child == null) {
            child = dir.createChildDirectory(null, drawableV21Dir);
        }

        VirtualFile newXmlFile = child.findChild(filename);
        if (newXmlFile != null && newXmlFile.exists()) {
            newXmlFile.delete(null);
        }
        newXmlFile = child.createChildData(null, filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        String nsUri = "http://www.w3.org/2000/xmlns/";
        String androidUri = "http://schemas.android.com/apk/res/android";
        Document doc = builder.newDocument();
        Element root = doc.createElement("ripple");
        root.setAttributeNS(nsUri, "xmlns:android", androidUri);
        root.setAttribute("android:color", pressed);
        doc.appendChild(root);

        Element item = doc.createElement("item");
        item.setAttribute("android:drawable", color);
        root.appendChild(item);

        OutputStream os = newXmlFile.getOutputStream(null);
        PrintWriter out = new PrintWriter(os);

        StringWriter writer = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        out.println(writer.getBuffer().toString());
        out.close();
    }
}
