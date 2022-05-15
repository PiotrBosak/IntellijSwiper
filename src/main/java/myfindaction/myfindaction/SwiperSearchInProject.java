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
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.EmptyIcon;
import myfindaction.myfindaction.stuff.MyFindInProjectManager;
import myfindaction.myfindaction.stuff.MyFindPopupPanel;
import org.jetbrains.annotations.NotNull;

public class SwiperSearchInProject extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        try {
            DataContext dataContext = anActionEvent.getDataContext();
            Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
            if (project == null)
                return;


            FindInProjectManager findInProjectManagerBase = FindInProjectManager.getInstance(project);
            MyFindInProjectManager findInProjectManager = new MyFindInProjectManager(findInProjectManagerBase, project);
//        MyFindPopupPanel.globalScopeType = new FindPopupScopeUI.ScopeType("Project", FindBundle.messagePointer("find.popup.scope.project"), EmptyIcon.ICON_0);
//        MyFindPopupPanel.fileScope = null;
            FindModel findModel = new FindModel();
            findModel.setProjectScope(true);
            findModel.setReplaceState(false);
//        MyFindPopupPanel.currentFindModel = findModel;
            MyFindPopupPanel.fileScope = null;
            findInProjectManager.findInProject(dataContext, findModel);
        }
        catch (Exception ignore) {

        }
    }

    public void update(AnActionEvent e) {
        try {
            FindInPathAction action = new FindInPathAction();
            action.update(e);
        }
        catch (Exception ignore) {

        }
    }

}
