package myfindaction.myfindaction.stuff;


import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MyFindInProjectManager {
    private final FindInProjectManager manager;
    private final Project myProject;
    private volatile boolean myIsFindInProgress;

    public MyFindInProjectManager(FindInProjectManager manager, Project project) {
        this.manager = manager;
        this.myProject = project;
    }

    public void findInProject(@NotNull DataContext dataContext, @Nullable FindModel model) {
        FindManagerImpl findManagerBase = (FindManagerImpl) FindManager.getInstance(this.myProject);
        FindManager findManager = new MyFindManagerImpl(findManagerBase, this.myProject);
        FindModel findModel;
        if (model != null) {
            findModel = model.clone();
        } else {
            findModel = findManager.getFindInProjectModel().clone();
            findModel.setReplaceState(false);
            this.initModel(findModel, dataContext);
        }

        findManager.showFindDialog(findModel, () -> {
            if (findModel.isReplaceState()) {
                ReplaceInProjectManager.getInstance(this.myProject).replaceInPath(findModel);
            } else {
                this.findInPath(findModel);
            }

        });
    }

    public void findInPath(@NotNull FindModel findModel) {
        this.startFindInProject(findModel);
    }

    protected void initModel(@NotNull FindModel findModel, @NotNull DataContext dataContext) {
        FindInProjectUtil.setDirectoryName(findModel, dataContext);
        String text = (String) PlatformDataKeys.PREDEFINED_TEXT.getData(dataContext);
        if (text != null) {
            FindModel.initStringToFind(findModel, text);
        } else {
            FindInProjectUtil.initStringToFindFromDataContext(findModel, dataContext);
        }

    }

    public void startFindInProject(@NotNull FindModel findModel) {
        if (findModel.getDirectoryName() == null || FindInProjectUtil.getDirectory(findModel) != null) {
            UsageViewManager managerBase = UsageViewManager.getInstance(this.myProject);
            MyUsageViewManager manager = new MyUsageViewManager(managerBase);
            if (manager != null) {
                FindManagerImpl findManagerBase = (FindManagerImpl) FindManager.getInstance(this.myProject);
                MyFindManagerImpl findManager = new MyFindManagerImpl(findManagerBase, this.myProject);
                findManager.getFindInProjectModel().copyFrom(findModel);
                FindModel findModelCopy = findModel.clone();
                UsageViewPresentation presentation = MyFindInProjectUtil.setupViewPresentation(findModelCopy);
                FindUsagesProcessPresentation processPresentation = MyFindInProjectUtil.setupProcessPresentation(this.myProject, presentation);
                ConfigurableUsageTarget usageTarget = new MyFindInProjectUtil.StringUsageTarget(this.myProject, findModel);
                FindManagerImpl nextBase = (FindManagerImpl) FindManager.getInstance(this.myProject);
                MyFindManagerImpl next = new MyFindManagerImpl(nextBase, this.myProject);
                next.getFindUsagesManager().addToHistory(usageTarget);
                manager.searchAndShowUsages(new UsageTarget[]{usageTarget}, () -> {
                    return (processor) -> {
                        this.myIsFindInProgress = true;

                        try {
                            Processor<UsageInfo> consumer = (info) -> {
                                Usage usage = (Usage) UsageInfo2UsageAdapter.CONVERTER.fun(info);
                                usage.getPresentation().getIcon();
                                return processor.process(usage);
                            };
                            MyFindInProjectUtil.findUsages(findModelCopy, this.myProject, consumer, processPresentation);
                        } finally {
                            this.myIsFindInProgress = false;
                        }

                    };
                }, processPresentation, presentation, (UsageViewManager.UsageViewStateListener) null);
            }
        }
    }

    public boolean isWorkInProgress() {
        return this.myIsFindInProgress;
    }

    public boolean isEnabled() {
        return !this.myIsFindInProgress && !ReplaceInProjectManager.getInstance(this.myProject).isWorkInProgress();
    }
}
