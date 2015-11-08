package importre.intellij.android.selector.form;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import importre.intellij.android.selector.color.ColorItemRenderer;
import org.jetbrains.annotations.NotNull;
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
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidSelectorDialog extends DialogWrapper {

    private final String drawableDir = "drawable";
    private final String drawableV21Dir = "drawable-v21";
    private final String valuesColorsXml = "values/colors.xml";
    private final VirtualFile dir;
    private final Project project;

    private JPanel contentPane;
    private JTextField filenameText;
    private JComboBox colorCombo;
    private JComboBox pressedCombo;
    private JComboBox pressedV21Combo;

    public AndroidSelectorDialog(@Nullable Project project, VirtualFile dir) {
        super(project);

        this.project = project;
        this.dir = dir;
        setTitle("Android Selector");
        setResizable(false);
        init();
    }

    @Override
    public void show() {
        try {
            if (initColors(dir)) {
                super.show();
            }
        } catch (Exception ignored) {
        }
    }

    @NotNull
    private String readStream(VirtualFile file) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(file.getInputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(bis));
        StringBuilder buff = new StringBuilder();

        String s;
        while ((s = in.readLine()) != null) {
            buff.append(s);
        }
        return buff.toString();
    }

    private boolean initColors(VirtualFile dir) throws Exception {
        VirtualFile colorsXml = dir.findFileByRelativePath(valuesColorsXml);
        if (colorsXml != null && colorsXml.exists()) {
            String data = readStream(colorsXml);
            data = data.replaceAll("(?s)<!--.+?-->", "");

            String regex = "<color\\s+name=\"(.+?)\">\\s*(\\S+)\\s*</color>";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(data);
            HashMap<String, String> map = new LinkedHashMap<String, String>();
            while (m.find()) {
                String name = m.group(1);
                String color = m.group(2);
                map.put(name, color);
            }

            if (map.isEmpty()) {
                String title = "Error";
                String msg = "Cannot find colors in colors.xml";
                showMessageDialog(title, msg);
                return false;
            }

            ArrayList<String[]> elements = new ArrayList<String[]>();
            for (String name : map.keySet()) {
                String color = map.get(name);
                while (color.startsWith("@color/")) {
                    color = color.replace("@color/", "");
                    color = map.get(color);
                }
                elements.add(new String[]{color, name});
            }

            ColorItemRenderer renderer = new ColorItemRenderer();
            colorCombo.setRenderer(renderer);
            pressedCombo.setRenderer(renderer);
            pressedV21Combo.setRenderer(renderer);
            for (Object element : elements) {
                colorCombo.addItem(element);
                pressedCombo.addItem(element);
                pressedV21Combo.addItem(element);
            }
            return true;
        }

        String title = "Error";
        String msg = String.format("Cannot find %s", valuesColorsXml);
        showMessageDialog(title, msg);
        return false;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private String getColorName(JComboBox combo) {
        Object colorItem = combo.getSelectedItem();
        try {
            if (colorItem instanceof Object[]) {
                return "@color/" + ((Object[]) (colorItem))[1];
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    protected void doOKAction() {
        String f = filenameText.getText();
        final String filename = (f.endsWith(".xml") ? f : f + ".xml").trim();
        final String color = getColorName(colorCombo);
        final String pressed = getColorName(pressedCombo);
        final String pressedV21 = getColorName(pressedV21Combo);

        if (!valid(filename, color, pressed, pressedV21)) {
            String title = "Invalidation";
            String msg = "color, pressed, pressedV21 must start with `@color/`";
            showMessageDialog(title, msg);
            return;
        }

        if (exists(filename)) {
            String title = "Cannot create files";
            String msg = String.format(Locale.US,
                    "`%s` already exists", filename);
            showMessageDialog(title, msg);
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

        String regex = "^@color/.+";
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

    private void showMessageDialog(String title, String message) {
        Messages.showMessageDialog(
                project, message, title, Messages.getErrorIcon());
    }
}
