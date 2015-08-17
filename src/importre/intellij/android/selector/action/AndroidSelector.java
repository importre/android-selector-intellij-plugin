package importre.intellij.android.selector.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import importre.intellij.android.selector.form.AndroidSelectorDialog;

public class AndroidSelector extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);

        VirtualFile dir = e.getData(LangDataKeys.VIRTUAL_FILE);
        if (dir != null && dir.isDirectory()) {
            String text = dir.getName();
            e.getPresentation().setVisible("res".equals(text));
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final VirtualFile dir = e.getData(LangDataKeys.VIRTUAL_FILE);
        if (dir == null) {
            return;
        }

        Project project = e.getProject();
        AndroidSelectorDialog dialog = new AndroidSelectorDialog(project, dir);
        dialog.show();
    }
}
