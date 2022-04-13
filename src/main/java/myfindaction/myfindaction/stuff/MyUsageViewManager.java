package myfindaction.myfindaction.stuff;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.PsiElement;
import com.intellij.usages.*;
import com.intellij.usages.rules.PsiElementUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MyUsageViewManager extends UsageViewManager {
    private final UsageViewManager manager;

    public MyUsageViewManager(UsageViewManager manager) {
        this.manager = manager;
    }

    @NotNull
    public UsageView createUsageView(@NotNull UsageTarget[] targets, @NotNull Usage[] usages, @NotNull UsageViewPresentation presentation, @Nullable Factory<? extends UsageSearcher> usageSearcherFactory) {
        UsageView var10000 = this.manager.createUsageView(targets, usages, presentation, usageSearcherFactory);
        return var10000;
    }

    @NotNull
    public UsageView showUsages(@NotNull UsageTarget[] searchedFor, @NotNull Usage[] foundUsages, @NotNull UsageViewPresentation presentation, @Nullable Factory<? extends UsageSearcher> factory) {
        UsageView var10000 = this.manager.showUsages(searchedFor, foundUsages, presentation, factory);
        return var10000;
    }

    @NotNull
    public UsageView showUsages(@NotNull UsageTarget[] searchedFor, @NotNull Usage[] foundUsages, @NotNull UsageViewPresentation presentation) {
        UsageView var10000 = this.manager.showUsages(searchedFor, foundUsages, presentation);
        return var10000;
    }

    @Nullable("returns null in case of no usages found or usage view not shown for one usage")
    public UsageView searchAndShowUsages(@NotNull UsageTarget[] searchFor, @NotNull Factory<? extends UsageSearcher> searcherFactory, boolean showPanelIfOnlyOneUsage, boolean showNotFoundMessage, @NotNull UsageViewPresentation presentation, @Nullable UsageViewStateListener listener) {
        return this.manager.searchAndShowUsages(searchFor, searcherFactory, showPanelIfOnlyOneUsage, showNotFoundMessage, presentation, listener);
    }

    public void searchAndShowUsages(@NotNull UsageTarget[] searchFor, @NotNull Factory<? extends UsageSearcher> searcherFactory, @NotNull FindUsagesProcessPresentation processPresentation, @NotNull UsageViewPresentation presentation, @Nullable UsageViewStateListener listener) {
        this.manager.searchAndShowUsages(searchFor, searcherFactory, processPresentation, presentation, listener);
    }

    @Nullable
    public UsageView getSelectedUsageView() {
        return this.manager.getSelectedUsageView();
    }
}
