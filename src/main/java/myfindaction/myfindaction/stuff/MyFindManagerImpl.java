package myfindaction.myfindaction.stuff;


import com.intellij.find.FindInProjectSettings;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class MyFindManagerImpl extends FindManager {
    private final FindManagerImpl manager;
    private final Project myProject;
    private MyFindUIHelper myHelper;
    private final FindModel myFindInProjectModel = new FindModel();


    public MyFindManagerImpl(FindManagerImpl manager, Project myProject) {
        this.manager = manager;
        this.myProject = myProject;
    }

    public FindModel createReplaceInFileModel() {
        return this.manager.createReplaceInFileModel();
    }

    @Nullable
    public FindModel getPreviousFindModel() {
        return this.manager.getPreviousFindModel();
    }

    public void setPreviousFindModel(FindModel previousFindModel) {
        this.manager.setPreviousFindModel(previousFindModel);
    }

    public void showSettingsAndFindUsages(@NotNull NavigationItem[] targets) {
        this.manager.showSettingsAndFindUsages(targets);
    }

    public void showFindDialog(@NotNull FindModel model, @NotNull Runnable okHandler) {
        if (this.myHelper != null && !Disposer.isDisposed(this.myHelper)) {
            this.myHelper.setModel(model);
            this.myHelper.setOkHandler(okHandler);
        } else {
            this.myHelper = new MyFindUIHelper(this.myProject, model, okHandler);
            Disposer.register(this.myHelper, () -> {
                this.myHelper = null;
            });
        }

        this.myHelper.showUI();
    }

    @Override
    public void closeFindDialog() {

    }

    public int showPromptDialog(@NotNull FindModel model, @NlsContexts.DialogTitle String title) {
        return this.manager.showPromptDialog(model, title);
    }

    @NotNull
    public FindModel getFindInFileModel() {
        FindModel var10000 = this.manager.getFindInFileModel();
        return var10000;
    }

    @NotNull
    public FindModel getFindInProjectModel() {
        FindModel var10000 = this.manager.getFindInProjectModel();
        return var10000;
    }

    @NotNull
    public FindResult findString(@NotNull CharSequence text, int offset, @NotNull FindModel model) {
        FindResult var10000 = this.manager.findString(text, offset, model);
        return var10000;
    }

    @NotNull
    public FindResult findString(@NotNull CharSequence text, int offset, @NotNull FindModel model, @Nullable VirtualFile findContextFile) {
        FindResult var10000 =
                this.manager.findString(text, offset, model, findContextFile);
        return var10000;
    }

    public int showMalformedReplacementPrompt(@NotNull FindModel model, @NlsContexts.DialogTitle String title, MalformedReplacementStringException exception) {
        return this.manager.showMalformedReplacementPrompt(model, title, exception);
    }

    @NlsSafe
    public String getStringToReplace(@NotNull String foundString, @NotNull FindModel model, int startOffset, @NotNull CharSequence documentText) throws MalformedReplacementStringException {
        return this.manager.getStringToReplace(foundString, model, startOffset, documentText);
    }

    public boolean findWasPerformed() {
        return this.manager.findWasPerformed();
    }

    public void setFindWasPerformed() {
        this.manager.setFindWasPerformed();
    }

    public boolean selectNextOccurrenceWasPerformed() {
        return this.manager.selectNextOccurrenceWasPerformed();
    }

    public void setSelectNextOccurrenceWasPerformed() {
        this.manager.setSelectNextOccurrenceWasPerformed();
    }

    public void clearFindingNextUsageInFile() {
        this.manager.clearFindingNextUsageInFile();
    }

    public void setFindNextModel(FindModel model) {
        this.manager.setFindNextModel(model);
    }

    public FindModel getFindNextModel() {
        return this.manager.getFindNextModel();
    }

    public FindModel getFindNextModel(@NotNull Editor editor) {
        return this.manager.getFindNextModel(editor);
    }

    public boolean canFindUsages(@NotNull PsiElement element) {
        return this.manager.canFindUsages(element);
    }

    public void findUsages(@NotNull PsiElement element) {
        this.manager.findUsages(element);
    }

    public void findUsagesInScope(@NotNull PsiElement element, @NotNull SearchScope searchScope) {
        this.manager.findUsagesInScope(element, searchScope);
    }

    public void findUsages(@NotNull PsiElement element, boolean showDialog) {
        this.manager.findUsages(element, showDialog);
    }

    public void findUsagesInEditor(@NotNull PsiElement element, @NotNull FileEditor editor) {
        this.manager.findUsagesInEditor(element, editor);
    }

    public boolean findNextUsageInEditor(@NotNull Editor editor) {
        return this.manager.findNextUsageInEditor(editor);
    }

    public boolean findPreviousUsageInEditor(@NotNull Editor editor) {
        return this.manager.findPreviousUsageInEditor(editor);
    }

    static void clearPreviousFindData(FindModel model) {
    }

    @NotNull
    public FindUsagesManager getFindUsagesManager() {
        FindUsagesManager var10000 = this.manager.getFindUsagesManager();
        return var10000;
    }

    void changeGlobalSettings(FindModel findModel) {
        String stringToFind = findModel.getStringToFind();
        FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(this.myProject);
        if (!StringUtil.isEmpty(stringToFind)) {
            findInProjectSettings.addStringToFind(stringToFind);
        }

        if (!findModel.isMultipleFiles()) {
            this.setFindWasPerformed();
        }

        if (findModel.isReplaceState()) {
            findInProjectSettings.addStringToReplace(findModel.getStringToReplace());
        }

        if (findModel.isMultipleFiles() && !findModel.isProjectScope() && findModel.getDirectoryName() != null) {
            findInProjectSettings.addDirectory(findModel.getDirectoryName());
            this.myFindInProjectModel.setWithSubdirectories(findModel.isWithSubdirectories());
        }

    }
}
