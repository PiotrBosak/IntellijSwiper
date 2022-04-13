package myfindaction.myfindaction.stuff;


import com.intellij.find.FindModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.usages.FindUsagesProcessPresentation;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageInfoAdapter;
import java.util.Set;
import java.util.function.Function;
import javax.swing.table.TableCellRenderer;

public class MyFindInProjectExecutor {
    public static MyFindInProjectExecutor getInstance() {
        return (MyFindInProjectExecutor)ApplicationManager.getApplication().getService(MyFindInProjectExecutor.class);
    }

    public TableCellRenderer createTableCellRenderer() {
        return null;
    }

    public void findUsages(Project project, ProgressIndicatorEx progressIndicator, FindUsagesProcessPresentation presentation, FindModel findModel, Set<VirtualFile> filesToScan, Function<UsageInfoAdapter, Boolean> f) {
        MyFindInProjectUtil.findUsages(findModel, project, presentation, filesToScan, (info) -> {
            UsageInfoAdapter usage = (UsageInfoAdapter)UsageInfo2UsageAdapter.CONVERTER.fun(info);
            usage.getPresentation().getIcon();
            return (Boolean)f.apply(usage);
        });
    }
}
