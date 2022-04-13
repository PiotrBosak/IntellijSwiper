package myfindaction.myfindaction;



import com.intellij.find.FindModel;
import com.intellij.find.actions.FindInPathAction;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import myfindaction.myfindaction.stuff.MyFindInProjectManager;
import org.jetbrains.annotations.NotNull;

public class SwiperSearchInFile extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        DataContext dataContext = anActionEvent.getDataContext();
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if(project == null)
            return;

        FindInProjectManager findInProjectManagerBase = FindInProjectManager.getInstance(project);
        MyFindInProjectManager findInProjectManager = new MyFindInProjectManager(findInProjectManagerBase,project);
        FindModel findModel = new FindModel();
        findModel.setProjectScope(true);
        findModel.setReplaceState(false);
        findInProjectManager.findInProject(dataContext, findModel);
    }

    public void update(AnActionEvent e) {
        FindInPathAction action = new FindInPathAction();
        action.update(e);
    }

}
