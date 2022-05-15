package myfindaction.myfindaction.stuff;


import com.intellij.find.*;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.find.impl.FindPopupPanel;
import com.intellij.find.impl.FindUI;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MyFindUIHelper implements Disposable {
    @NotNull
    private final Project myProject;
    @NotNull private FindModel myModel;
    FindModel myPreviousModel;
    @NotNull private Runnable myOkHandler;

    FindUI myUI;

    MyFindUIHelper(@NotNull Project project, @NotNull FindModel model, @NotNull Runnable okHandler) {
        myProject = project;
        myModel = model;
        myOkHandler = okHandler;
        myUI = getOrCreateUI();
        myUI.initByModel();
    }

    private FindUI getOrCreateUI() {
        if (myUI == null) {
            JComponent component;
            MyFindPopupPanel panel = new MyFindPopupPanel(this);
            component = panel;
            myUI = panel;

//            registerAction("ReplaceInPath", true, component, myUI);
//            registerAction("FindInPath", false, component, myUI);
            Disposer.register(myUI.getDisposable(), this);
        }
        return myUI;
    }

    private void registerAction(String actionName, boolean replace, JComponent component, FindUI ui) {
        AnAction action = ActionManager.getInstance().getAction(actionName);
        new AnAction() {
            @Override
            public boolean isDumbAware() {
                return action.isDumbAware();
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ui.saveSettings();
                myModel.copyFrom(FindManager.getInstance(myProject).getFindInProjectModel());
                FindUtil.initStringToFindWithSelection(myModel, e.getData(CommonDataKeys.EDITOR));
                myModel.setReplaceState(replace);
                ui.initByModel();
            }
            //@NotNull
            //private DataContextWrapper prepareDataContextForFind(@NotNull AnActionEvent e) {
            //  DataContext dataContext = e.getDataContext();
            //  Project project = CommonDataKeys.PROJECT.getData(dataContext);
            //  Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
            //  final String selection = editor != null ? editor.getSelectionModel().getSelectedText() : null;
            //
            //  return new DataContextWrapper(dataContext) {
            //    @Nullable
            //    @Override
            //    public Object getData(@NonNls String dataId) {
            //      if (CommonDataKeys.PROJECT.is(dataId)) return project;
            //      if (PlatformDataKeys.PREDEFINED_TEXT.is(dataId)) return selection;
            //      return super.getData(dataId);
            //    }
            //  };
            //}

        }.registerCustomShortcutSet(action.getShortcutSet(), component);
    }


    boolean canSearchThisString() {
        return myUI != null && (!StringUtil.isEmpty(myUI.getStringToFind()) || !myModel.isReplaceState() && !myModel.isFindAllEnabled() && myUI.getFileTypeMask() != null);
    }


    @NotNull
    public Project getProject() {
        return myProject;
    }

    @NotNull
    public FindModel getModel() {
        return myModel;
    }

    public void setModel(@NotNull FindModel model) {
        myModel = model;
        myUI.initByModel();
    }

    public void setOkHandler(@NotNull Runnable okHandler) {
        myOkHandler = okHandler;
    }

    public void showUI() {
        myUI = getOrCreateUI();
        myUI.showUI();
    }

    @Override
    public void dispose() {
        System.out.println("YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
        System.out.println("Disposed");
        if (myUI != null && !Disposer.isDisposed(myUI.getDisposable())) {
            Disposer.dispose(myUI.getDisposable());
        }
        myUI = null;
    }

    void updateFindSettings() {
        FindManagerImpl base = (FindManagerImpl)FindManager.getInstance(this.myProject);
        var manager = new MyFindManagerImpl(base,this.myProject);
        manager.changeGlobalSettings(this.myModel);
        FindSettings findSettings = FindSettings.getInstance();
        findSettings.setCaseSensitive(myModel.isCaseSensitive());
        if (myModel.isReplaceState()) {
            findSettings.setPreserveCase(myModel.isPreserveCase());
        }

        findSettings.setWholeWordsOnly(myModel.isWholeWordsOnly());
        boolean saveContextBetweenRestarts = false;
        findSettings.setInStringLiteralsOnly(saveContextBetweenRestarts && myModel.isInStringLiteralsOnly());
        findSettings.setInCommentsOnly(saveContextBetweenRestarts && myModel.isInCommentsOnly());
        findSettings.setExceptComments(saveContextBetweenRestarts && myModel.isExceptComments());
        findSettings.setExceptStringLiterals(saveContextBetweenRestarts && myModel.isExceptStringLiterals());
        findSettings.setExceptCommentsAndLiterals(saveContextBetweenRestarts && myModel.isExceptCommentsAndStringLiterals());

        findSettings.setRegularExpressions(myModel.isRegularExpressions());
        if (!myModel.isMultipleFiles()){
            findSettings.setForward(myModel.isForward());
            findSettings.setFromCursor(myModel.isFromCursor());

            findSettings.setGlobal(myModel.isGlobal());
        } else{
            String directoryName = myModel.getDirectoryName();
            if (directoryName != null && !directoryName.isEmpty()) {
                findSettings.setWithSubdirectories(myModel.isWithSubdirectories());
            }
            else if (!StringUtil.isEmpty(myModel.getModuleName())) {
                //do nothing here
            }
            else if (myModel.getCustomScopeName() != null) {
                findSettings.setCustomScope(myModel.getCustomScopeName());
            }
        }

        findSettings.setFileMask(myModel.getFileFilter());
    }

    @Nls String getTitle() {
        if (myModel.isReplaceState()){
            return myModel.isMultipleFiles()
                    ? FindBundle.message("find.replace.in.project.dialog.title")
                    : FindBundle.message("find.replace.text.dialog.title");
        }
        return myModel.isMultipleFiles() ?
                FindBundle.message("find.in.path.dialog.title") :
                FindBundle.message("find.text.dialog.title");
    }

    public boolean isReplaceState() {
        return myModel.isReplaceState();
    }

    @NotNull
    public Runnable getOkHandler() {
        return myOkHandler;
    }

    public void doOKAction() {
        updateFindSettings();
        myOkHandler.run();
    }
}
