package myfindaction.myfindaction;



import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.find.actions.FindInPathAction;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.find.impl.FindPopupScopeUI;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.EmptyIcon;
import myfindaction.myfindaction.stuff.MyFindInProjectManager;
import myfindaction.myfindaction.stuff.MyFindPopupPanel;
import org.jetbrains.annotations.NotNull;

public class SwiperSearchInFile extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        try {
            DataContext dataContext = anActionEvent.getDataContext();
            Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
            if (project == null)
                return;

            Editor editor = anActionEvent.getDataContext().getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
            FindInProjectManager findInProjectManagerBase = FindInProjectManager.getInstance(project);
            MyFindInProjectManager findInProjectManager = new MyFindInProjectManager(findInProjectManagerBase, project);
            FindModel findModel = new FindModel();
            findModel.setCustomScope(true);
            MyFindPopupPanel.currentEditor = editor;
            MyFindPopupPanel.wasSelected = false;

            SelectionModel selectionModel = editor.getSelectionModel();
//            Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
            TextRange selectionRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
            int lineNumber = editor.getDocument().getLineNumber(selectionRange.getStartOffset());
            MyFindPopupPanel.currentLine = lineNumber;


            Document currentDoc = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument();
            VirtualFile currentFile = FileDocumentManager.getInstance().getFile(currentDoc);
            var scope = GlobalSearchScope.fileScope(project, currentFile);
            findModel.setCustomScope(scope);
            MyFindPopupPanel.fileScope = scope;
            findInProjectManager.findInProject(dataContext, findModel);
        }
        catch (Exception ignore){

        }
    }

    public void update(AnActionEvent e) {
        try {
            FindInPathAction action = new FindInPathAction();
            action.update(e);
        }
        catch (Exception ignore) {}
    }

}
