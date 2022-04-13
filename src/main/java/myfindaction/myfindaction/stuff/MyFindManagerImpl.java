package myfindaction.myfindaction.stuff;


import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.highlighting.HighlightManagerImpl;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.find.*;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.find.impl.FindResultImpl;
import com.intellij.find.impl.FindUIHelper;
import com.intellij.find.impl.RegExReplacementBuilder;
import com.intellij.find.impl.livePreview.SearchResults;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.ParserDefinition;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.navigation.NavigationItem;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.StringPattern;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.reference.SoftReference;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.ReplacePromptDialog;
import com.intellij.usages.ChunkExtractor;
import com.intellij.usages.impl.SyntaxHighlighterOverEditorHighlighter;
import com.intellij.util.containers.IntObjectMap;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.ImmutableCharSequence;
import com.intellij.util.text.StringSearcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
        FindResult var10000 = this.manager.findString(text, offset, model, findContextFile);
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
