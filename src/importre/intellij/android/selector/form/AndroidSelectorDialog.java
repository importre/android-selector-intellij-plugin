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
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;

public class AndroidSelectorDialog extends DialogWrapper {

    private static final String INDENT_SPACE = "{http://xml.apache.org/xslt}indent-amount";

    private static final String drawableDir = "drawable";
    private static final String drawableV21Dir = "drawable-v21";
    private static final String valuesColorsXml = "values/colors.xml";
    private static final String localProps = "local.properties";
    private static final String platformsRes = "%s/platforms/%s/data/res/values";
    private static final String nsUri = "http://www.w3.org/2000/xmlns/";
    private static final String androidUri = "http://schemas.android.com/apk/res/android";

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean initColors(VirtualFile dir) {
        VirtualFile colorsXml = dir.findFileByRelativePath(valuesColorsXml);
        if (colorsXml != null && colorsXml.exists()) {
            HashMap<String, String> cmap = parseColorsXml(colorsXml);
            HashMap<String, String> andCmap = parseAndroidColorsXml();

            if (cmap.isEmpty()) {
                String title = "Error";
                String msg = "Cannot find colors in colors.xml";
                showMessageDialog(title, msg);
                return false;
            }

            String regex = "^@(android:)?color/(.+$)";
            ArrayList<String[]> elements = new ArrayList<String[]>();
            for (String name : cmap.keySet()) {
                String color = cmap.get(name);
                while (color != null && color.matches(regex)) {
                    if (color.startsWith("@color/")) {
                        String key = color.replace("@color/", "");
                        color = cmap.get(key);
                    } else if (color.startsWith("@android:color/")) {
                        String key = color.replace("@android:color/", "");
                        color = andCmap.get(key);
                    } else {
                        // not reachable...
                    }
                }

                if (color != null) {
                    elements.add(new String[]{color, name});
                }
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
            return !elements.isEmpty();
        }

        String title = "Error";
        String msg = String.format("Cannot find %s", valuesColorsXml);
        showMessageDialog(title, msg);
        return false;
    }

    @NotNull
    private HashMap<String, String> parseColorsXml(VirtualFile colorsXml) {
        HashMap<String, String> map = new LinkedHashMap<String, String>();
        try {
            NodeList colors = getColorNodes(colorsXml.getInputStream());
            makeColorMap(colors, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private void makeColorMap(NodeList colors, HashMap<String, String> map) {
        for (int i = 0; i < colors.getLength(); i++) {
            Element node = (Element) colors.item(i);
            String nodeName = node.getNodeName();
            if ("color".equals(nodeName) || "item".equals(nodeName)) {
                String name = node.getAttribute("name");
                String color = node.getTextContent();
                if (name != null && color != null && !map.containsKey(name)) {
                    map.put(name, color);
                }
            }
        }
    }

    private NodeList getColorNodes(InputStream stream) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//item[@type=\"color\"]|//color";
        XPathExpression compile = xPath.compile(expression);
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = f.newDocumentBuilder();
        Document doc = builder.parse(stream);
        return (NodeList) compile.evaluate(doc, XPathConstants.NODESET);
    }

    @NotNull
    private HashMap<String, String> parseAndroidColorsXml() {
        HashMap<String, String> map = new HashMap<String, String>();
        if (project == null) return map;
        VirtualFile baseDir = project.getBaseDir();
        VirtualFile prop = baseDir.findFileByRelativePath(localProps);
        if (prop == null) return map;

        Properties properties = new Properties();
        try {
            properties.load(prop.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String sdkDir = properties.getProperty("sdk.dir");
        File file = new File(sdkDir + File.separator + "platforms");
        if (!file.isDirectory()) return map;

        ArrayList<String> platforms = new ArrayList<String>();
        Collections.addAll(platforms, file.list());
        Collections.reverse(platforms);
        for (int i = 0, size = platforms.size(); i < size; i++) {
            String platform = platforms.get(i);
            if (platform.matches("^android-\\d+$")) continue;
            if (i > 3) break;

            String path = String.format(platformsRes, sdkDir, platform);
            File[] files = new File(path).listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.getName().matches("colors.*\\.xml")) {
                    try {
                        FileInputStream stream = new FileInputStream(f);
                        NodeList colors = getColorNodes(stream);
                        makeColorMap(colors, map);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return map;
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
        } catch (Exception e) {
            e.printStackTrace();
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
                } catch (Exception e) {
                    e.printStackTrace();
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
        transformer.setOutputProperty(INDENT_SPACE, "4");
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
        transformer.setOutputProperty(INDENT_SPACE, "4");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        out.println(writer.getBuffer().toString());
        out.close();
    }

    private void showMessageDialog(String title, String message) {
        Messages.showMessageDialog(
                project, message, title, Messages.getErrorIcon());
    }
}
