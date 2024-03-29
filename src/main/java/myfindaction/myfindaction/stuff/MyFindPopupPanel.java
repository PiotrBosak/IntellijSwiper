package myfindaction.myfindaction.stuff;


import com.intellij.CommonBundle;
import com.intellij.accessibility.TextFieldWithListAccessibleContext;
import com.intellij.diff.util.DiffDrawUtil;
import com.intellij.find.*;
import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.find.impl.*;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider;
import com.intellij.openapi.actionSystem.ex.TooltipLinkProvider;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.*;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.pom.Navigatable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopeUtil;
import com.intellij.reference.SoftReference;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.*;
import com.intellij.ui.dsl.builder.SpacingConfiguration;
import com.intellij.ui.dsl.gridLayout.builders.RowBuilder;
import com.intellij.ui.hover.TableHoverListener;
import com.intellij.ui.mac.touchbar.Touchbar;
import com.intellij.ui.popup.PopupState;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.JBTable;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.*;
import myfindaction.myfindaction.StringConverter;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.intellij.openapi.actionSystem.IdeActions.ACTION_OPEN_IN_RIGHT_SPLIT;
import static com.intellij.ui.SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;
import static com.intellij.util.FontUtil.spaceAndThinSpace;

public class MyFindPopupPanel extends JBPanel<MyFindPopupPanel> implements FindUI {
    public static Editor currentEditor;
    public static int currentLine;
    public static boolean wasSelected;
    public static RangeHighlighter highlighter;

    public static FindPopupScopeUI.ScopeType globalScopeType;
    public static List<UsageInfo> myCoolUsages;
    public static String currentSearchAfterChanges = "";
    public static String[] searchWords = new String[]{};
    public static GlobalSearchScope fileScope;
    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final KeyStroke PREVIOUS = KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_DOWN_MASK);
    private static final KeyStroke NEXT = KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_DOWN_MASK);
    private static final KeyStroke CLEAR = KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK);
    private static final KeyStroke ENTER_WITH_MODIFIERS = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, SystemInfo.isMac
            ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke REPLACE_ALL = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
    private static final KeyStroke RESET_FILTERS = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK);

    private static final String SERVICE_KEY = "find.popup";
    private static final String SPLITTER_SERVICE_KEY = "find.popup.splitter";
    @NotNull
    private final MyFindUIHelper myHelper;
    @NotNull
    private final Project myProject;
    @NotNull
    private final Disposable myDisposable;
    private final Alarm myPreviewUpdater;
    private JComponent myCodePreviewComponent;
    private SearchTextArea mySearchTextArea;
    private SearchTextArea myReplaceTextArea;
    private ActionListener myOkActionListener;
    private final AtomicBoolean myCanClose = new AtomicBoolean(true);
    private final AtomicBoolean myIsPinned = new AtomicBoolean(false);
    private JBLabel myOKHintLabel;
    private JBLabel myNavigationHintLabel;
    private Alarm mySearchRescheduleOnCancellationsAlarm;
    private volatile ProgressIndicatorBase myResultsPreviewSearchProgress;

    private final AtomicBoolean myCaseSensitiveState = new AtomicBoolean();
    private final AtomicBoolean myPreserveCaseState = new AtomicBoolean();
    private final AtomicBoolean myWholeWordsState = new AtomicBoolean();
    private final AtomicBoolean myRegexState = new AtomicBoolean();
    private JButton myOKButton;
    private JTextArea mySearchComponent;
    private JTextArea myReplaceComponent;
    private String mySelectedContextName = FindBundle.message("find.context.anywhere.scope.label");
    private FindPopupScopeUI.ScopeType mySelectedScope;

    private JBTable myResultsPreviewTable;
    private DefaultTableModel myResultsPreviewTableModel;
    private SimpleColoredComponent myUsagePreviewTitle;
    private MyUsagePreviewPanel myUsagePreviewPanel;
    private DialogWrapper myDialog;
    private int myLoadingHash;
    private final AtomicBoolean myNeedReset = new AtomicBoolean(true);
    private String myUsagesCount;
    private String myFilesCount;
    private UsageViewPresentation myUsageViewPresentation;
    private final ComponentValidator myComponentValidator;
    private AnAction myCaseSensitiveAction;
    private AnAction myWholeWordsAction;
    private AnAction myRegexAction;

    private AnAction previousEntryAction;
    private AnAction nextEntryAction;
    private AnAction clearAction;
    private boolean mySuggestRegexHintForEmptyResults = true;
    private JBSplitter myPreviewSplitter;

    MyFindPopupPanel(@NotNull MyFindUIHelper helper) {
        myHelper = helper;
        myProject = myHelper.getProject();
        myDisposable = Disposer.newDisposable();
        myPreviewUpdater = new Alarm(myDisposable);
        myComponentValidator = new ComponentValidator(myDisposable) {
            @Override
            public void updateInfo(@Nullable ValidationInfo info) {
                if (info != null && info.component == mySearchComponent) {
                    super.updateInfo(null);
                } else {
                    super.updateInfo(info);
                }
            }
        };

        Disposer.register(myDisposable, () -> {
            finishPreviousPreviewSearch();
            if (mySearchRescheduleOnCancellationsAlarm != null)
                Disposer.dispose(mySearchRescheduleOnCancellationsAlarm);
            if (myUsagePreviewPanel != null) Disposer.dispose(myUsagePreviewPanel);
        });

        initComponents();
        initByModel();

        FindUsagesCollector.triggerUsedOptionsStats(myProject, FindUsagesCollector.FIND_IN_PATH, myHelper.getModel());
    }

    @Override
    public void showUI() {
        if (myDialog != null && myDialog.isVisible()) {
            return;
        }
        if (myDialog != null && !myDialog.isDisposed()) {
            myDialog.doCancelAction();
        }
        if (myDialog == null || myDialog.isDisposed()) {
            myDialog = new DialogWrapper(myHelper.getProject(), null, true, DialogWrapper.IdeModalityType.MODELESS, false) {
                {
                    init();
                    getRootPane().setDefaultButton(null);
                }

                @Override
                protected void doOKAction() {
                    myOkActionListener.actionPerformed(null);
                }

                @Override
                protected void dispose() {
                    saveSettings();
                    super.dispose();
                }

                @Nullable
                @Override
                protected Border createContentPaneBorder() {
                    return null;
                }

                @Override
                protected JComponent createCenterPanel() {
                    return MyFindPopupPanel.this;
                }

                @Override
                protected String getDimensionServiceKey() {
                    return SERVICE_KEY;
                }
            };
            myDialog.setUndecorated(true);
            ApplicationManager.getApplication().getMessageBus().connect(myDialog.getDisposable()).subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
                @Override
                public void projectClosed(@NotNull Project project) {
                    closeImmediately();
                }
            });
            Disposer.register(myDialog.getDisposable(), myDisposable);

            final Window window = WindowManager.getInstance().suggestParentWindow(myProject);
            Component parent = UIUtil.findUltimateParent(window);
            RelativePoint showPoint = null;
            Point screenPoint = DimensionService.getInstance().getLocation(SERVICE_KEY, myProject);
            if (screenPoint != null) {
                if (parent != null) {
                    SwingUtilities.convertPointFromScreen(screenPoint, parent);
                    showPoint = new RelativePoint(parent, screenPoint);
                } else {
                    showPoint = new RelativePoint(screenPoint);
                }
            }
            if (parent != null && showPoint == null) {
                int height = UISettings.getInstance().getShowNavigationBar() ? 135 : 115;
                if (parent instanceof IdeFrameImpl && ((IdeFrameImpl) parent).isInFullScreen()) {
                    height -= 20;
                }
                showPoint = new RelativePoint(parent, new Point((parent.getSize().width - getPreferredSize().width) / 2, height));
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (mySearchComponent.getCaret() != null) {
                    mySearchComponent.selectAll();
                }
            });
            WindowMoveListener windowListener = new WindowMoveListener(this);
            addMouseListener(windowListener);
            addMouseMotionListener(windowListener);
            Dimension panelSize = getPreferredSize();
            Dimension prev = DimensionService.getInstance().getSize(SERVICE_KEY, myProject);
            panelSize.width += JBUIScale.scale(24);//hidden 'loading' icon
            panelSize.height *= 2;
            if (prev != null && prev.height < panelSize.height) prev.height = panelSize.height;
            Window dialogWindow = myDialog.getPeer().getWindow();
            final AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
            JRootPane root = ((RootPaneContainer) dialogWindow).getRootPane();

            IdeGlassPaneImpl glass = (IdeGlassPaneImpl) myDialog.getRootPane().getGlassPane();
            WindowResizeListener resizeListener = new WindowResizeListener(
                    root,
                    JBUI.insets(4),
                    null) {
                private Cursor myCursor;

                @Override
                protected void setCursor(@NotNull Component content, Cursor cursor) {
                    if (myCursor != cursor || myCursor != Cursor.getDefaultCursor()) {
                        glass.setCursor(cursor, this);
                        myCursor = cursor;

                        if (content instanceof JComponent) {
                            IdeGlassPaneImpl.savePreProcessedCursor((JComponent) content, content.getCursor());
                        }
                        super.setCursor(content, cursor);
                    }
                }
            };
            glass.addMousePreprocessor(resizeListener, myDisposable);
            glass.addMouseMotionPreprocessor(resizeListener, myDisposable);

            DumbAwareAction.create(e -> closeImmediately())
                    .registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), root, myDisposable);
            root.setWindowDecorationStyle(JRootPane.NONE);
            root.setBorder(PopupBorder.Factory.create(true, true));
            UIUtil.markAsPossibleOwner((Dialog) dialogWindow);
            dialogWindow.setBackground(UIUtil.getPanelBackground());
            dialogWindow.setMinimumSize(panelSize);
            if (prev == null) {
                panelSize.height *= 1.5;
                panelSize.width *= 1.15;
            }
            dialogWindow.setSize(prev != null ? prev : panelSize);

            IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
            if (showPoint != null) {
                myDialog.setLocation(showPoint.getScreenPoint());
            } else {
                dialogWindow.setLocationRelativeTo(null);
            }
            mySuggestRegexHintForEmptyResults = true;
            myDialog.show();

            WindowAdapter focusListener = new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    closeIfPossible();
                }
            };

            dialogWindow.addWindowListener(new WindowAdapter() {
                private boolean wasOpened = false;

                @Override
                public void windowDeactivated(WindowEvent e) {
                    if (!wasOpened) {
                        return;
                    }
                    // At the moment of deactivation there is just "temporary" focus owner (main frame),
                    // true focus owner (Search Everywhere popup etc.) appears later so the check should be postponed too
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Component focusOwner = IdeFocusManager.getInstance(myProject).getFocusOwner();
                        if (focusOwner == null || SwingUtilities.isDescendingFrom(focusOwner, MyFindPopupPanel.this))
                            return;
                        Window w = ComponentUtil.getWindow(focusOwner);
                        if (w != null && w.getOwner() != dialogWindow) {
                            closeIfPossible();
                        }
                    }, ModalityState.current());
                }

                @Override
                public void windowOpened(WindowEvent e) {
                    wasOpened = true;
                    Arrays.stream(Frame.getFrames())
                            .filter(f -> f != null && f.getOwner() != dialogWindow
                                    && f instanceof IdeFrame && ((IdeFrame) f).getProject() == myProject)
                            .forEach(win -> {
                                win.addWindowFocusListener(focusListener);
                                Disposer.register(myDisposable, () -> win.removeWindowFocusListener(focusListener));
                            });
                }
            });

            JRootPane rootPane = getRootPane();
            if (rootPane != null) {
                rootPane.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ENTER_WITH_MODIFIERS, "openInFindWindow");
                rootPane.getActionMap().put("openInFindWindow", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        myOkActionListener.actionPerformed(null);
                    }
                });
            }
            ApplicationManager.getApplication().invokeLater(this::scheduleResultsUpdate, ModalityState.any());
        }
        clear();
    }

    public void closeIfPossible() {
        if (canBeClosed() && !myIsPinned.get()) {
            myDialog.doCancelAction();
        }
    }

    protected boolean canBeClosed() {
        if (myProject.isDisposed()) return true;
        if (!myCanClose.get()) return false;
        if (myIsPinned.get()) return false;
        if (!ApplicationManager.getApplication().isActive()) return false;
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null) return false;
        List<JBPopup> popups = ContainerUtil.filter(JBPopupFactory.getInstance().getChildPopups(this), popup -> !popup.isDisposed());
        if (!popups.isEmpty()) {
            for (JBPopup popup : popups) {
                popup.cancel();
            }
            return false;
        }
        return true;
    }

    @Override
    public void saveSettings() {
        Window window = myDialog.getWindow();
        if (!window.isShowing()) return;
        DimensionService.getInstance().setSize(SERVICE_KEY, myDialog.getSize(), myHelper.getProject());
        DimensionService.getInstance().setLocation(SERVICE_KEY, window.getLocationOnScreen(), myHelper.getProject());
        FindSettings findSettings = FindSettings.getInstance();
        myHelper.updateFindSettings();
        applyTo(FindManager.getInstance(myProject).getFindInProjectModel());
    }

    @NotNull
    @Override
    public Disposable getDisposable() {
        return myDisposable;
    }

    @NotNull
    public Project getProject() {
        return myProject;
    }

    @NotNull
    public MyFindUIHelper getHelper() {
        return myHelper;
    }

    @NotNull
    public AtomicBoolean getCanClose() {
        return myCanClose;
    }

    private int counter = 0;

    private void showPreviousEntry() {
        var history = FindInProjectSettings.getInstance(myProject).getRecentFindStrings();
        if (counter < history.length - 1) {
            counter++;
        }
        if (counter <= history.length - 1) {
            mySearchComponent.setText(history[history.length - 1 - counter]);
        }

    }

    private void showNextEntry() {
        var history = FindInProjectSettings.getInstance(myProject).getRecentFindStrings();
        if (counter > 0) {
            counter--;
        }
        if (counter <= history.length - 1) {
            mySearchComponent.setText(history[history.length - 1 - counter]);
        }

    }

    private void clear() {
        mySearchComponent.setText("");
    }

    private void initComponents() {
        AnAction myShowFilterPopupAction = new MyShowFilterPopupAction();
        myShowFilterPopupAction.registerCustomShortcutSet(myShowFilterPopupAction.getShortcutSet(), this);


        previousEntryAction = DumbAwareAction.create(event -> showPreviousEntry());
        nextEntryAction = DumbAwareAction.create(event -> showNextEntry());
        clearAction = DumbAwareAction.create(e -> clear());
        previousEntryAction.registerCustomShortcutSet(new CustomShortcutSet(PREVIOUS), this);
        nextEntryAction.registerCustomShortcutSet(new CustomShortcutSet(NEXT), this);
        nextEntryAction.registerCustomShortcutSet(new CustomShortcutSet(NEXT), this);
        myOKButton = new JButton(FindBundle.message("find.popup.find.button"));
        myOkActionListener = __ -> doOK(true);
        myOKButton.addActionListener(myOkActionListener);
        boolean enterAsOK = false;

        AnAction openInRightSplit = ActionManager.getInstance().getAction(ACTION_OPEN_IN_RIGHT_SPLIT);
        if (openInRightSplit != null) {
            ShortcutSet set = openInRightSplit.getShortcutSet();
            new MyFindPopupPanel.MyEnterAction(false).registerCustomShortcutSet(set, this);
        }

        new MyFindPopupPanel.MyEnterAction(enterAsOK).registerCustomShortcutSet(new CustomShortcutSet(ENTER), this);
        DumbAwareAction.create(
                __ -> myOkActionListener.actionPerformed(null)).registerCustomShortcutSet(new CustomShortcutSet(ENTER_WITH_MODIFIERS), this);

        List<Shortcut> navigationKeyStrokes = new ArrayList<>();
        KeyStroke viewSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getViewSource());
        if (viewSourceKeyStroke != null && !Comparing.equal(viewSourceKeyStroke, ENTER_WITH_MODIFIERS) && !Comparing.equal(viewSourceKeyStroke, ENTER)) {
            navigationKeyStrokes.add(new KeyboardShortcut(viewSourceKeyStroke, null));
        }
        KeyStroke editSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
        if (editSourceKeyStroke != null && !Comparing.equal(editSourceKeyStroke, ENTER_WITH_MODIFIERS) && !Comparing.equal(editSourceKeyStroke, ENTER)) {
            navigationKeyStrokes.add(new KeyboardShortcut(editSourceKeyStroke, null));
        }
        if (!navigationKeyStrokes.isEmpty()) {
            DumbAwareAction.create(e -> navigateToSelectedUsage(e))
                    .registerCustomShortcutSet(new CustomShortcutSet(navigationKeyStrokes.toArray(Shortcut.EMPTY_ARRAY)), this);
        }

        myResultsPreviewTableModel = createTableModel();
        myResultsPreviewTable = new JBTable(myResultsPreviewTableModel) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(getWidth(), 1 + getRowHeight() * 4);
            }
        };
        myResultsPreviewTable.setFocusable(false);
        myResultsPreviewTable.putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true);
        myResultsPreviewTable.getEmptyText().setShowAboveCenter(false);
        myResultsPreviewTable.setShowColumns(false);
        myResultsPreviewTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        myResultsPreviewTable.setShowGrid(false);
        myResultsPreviewTable.setIntercellSpacing(JBUI.emptySize());

        mySearchComponent = new MyFindPopupPanel.JBTextAreaWithMixedAccessibleContext(myResultsPreviewTable.getAccessibleContext());
        mySearchComponent.setColumns(25);
        mySearchComponent.setRows(1);
        mySearchComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.search.accessible.name"));
        myReplaceComponent = new MyFindPopupPanel.JBTextAreaWithMixedAccessibleContext(myResultsPreviewTable.getAccessibleContext());
        myReplaceComponent.setColumns(25);
        myReplaceComponent.setRows(1);
        myReplaceComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.replace.accessible.name"));
        mySearchTextArea = new SearchTextArea(mySearchComponent, true);
        myReplaceTextArea = new SearchTextArea(myReplaceComponent, false);
        myCaseSensitiveAction =
                new MyFindPopupPanel.MySwitchStateToggleAction("find.popup.case.sensitive", MyFindPopupPanel.ToggleOptionName.CaseSensitive,
                        AllIcons.Actions.MatchCase, AllIcons.Actions.MatchCaseHovered, AllIcons.Actions.MatchCaseSelected,
                        myCaseSensitiveState, () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get());
        myWholeWordsAction =
                new MyFindPopupPanel.MySwitchStateToggleAction("find.whole.words", MyFindPopupPanel.ToggleOptionName.WholeWords,
                        AllIcons.Actions.Words, AllIcons.Actions.WordsHovered, AllIcons.Actions.WordsSelected,
                        myWholeWordsState, () -> !myRegexState.get());
        myRegexAction =
                new MyFindPopupPanel.MySwitchStateToggleAction("find.regex", MyFindPopupPanel.ToggleOptionName.Regex,
                        AllIcons.Actions.Regex, AllIcons.Actions.RegexHovered, AllIcons.Actions.RegexSelected,
                        myRegexState, () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get(),
                        new TooltipLinkProvider.TooltipLink(FindBundle.message("find.regex.help.link"),
                                RegExHelpPopup.createRegExLinkRunnable(mySearchTextArea)));
        List<Component> searchExtraButtons =
                mySearchTextArea.setExtraActions(myCaseSensitiveAction, myWholeWordsAction, myRegexAction);
        AnAction preserveCaseAction =
                new MyFindPopupPanel.MySwitchStateToggleAction("find.options.replace.preserve.case", MyFindPopupPanel.ToggleOptionName.PreserveCase,
                        AllIcons.Actions.PreserveCase, AllIcons.Actions.PreserveCaseHover, AllIcons.Actions.PreserveCaseSelected,
                        myPreserveCaseState, () -> !myRegexState.get() && !myCaseSensitiveState.get());
        List<Component> replaceExtraButtons = myReplaceTextArea.setExtraActions(
                preserveCaseAction);

        mySelectedScope = globalScopeType;

        TableHoverListener.DEFAULT.removeFrom(myResultsPreviewTable);
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                if (event.getSource() != myResultsPreviewTable) return false;
                navigateToSelectedUsage(null);
                return true;
            }
        }.installOn(myResultsPreviewTable);
        myResultsPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                myResultsPreviewTable.transferFocus();
            }
        });
        applyFont(JBFont.label(), myResultsPreviewTable);
        JComponent[] tableAware = {mySearchComponent, myReplaceComponent};
        for (JComponent component : tableAware) {
            ScrollingUtil.installActions(myResultsPreviewTable, false, component);
        }

        ActionListener helpAction = __ -> HelpManager.getInstance().invokeHelp("reference.dialogs.findinpath");
        registerKeyboardAction(helpAction, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(helpAction, KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeymapManager keymapManager = KeymapManager.getInstance();
        Keymap activeKeymap = keymapManager != null ? keymapManager.getActiveKeymap() : null;
        if (activeKeymap != null) {
            ShortcutSet findNextShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindNext"));
            ShortcutSet findPreviousShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindPrevious"));
            DumbAwareAction findNextAction = DumbAwareAction.create(event -> {
                int selectedRow = myResultsPreviewTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < myResultsPreviewTable.getRowCount() - 1) {
                    myResultsPreviewTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                    ScrollingUtil.ensureIndexIsVisible(myResultsPreviewTable, selectedRow + 1, 1);
                }
            });
            DumbAwareAction findPreviousAction = DumbAwareAction.create(event -> {
                int selectedRow = myResultsPreviewTable.getSelectedRow();
                if (selectedRow > 0 && selectedRow <= myResultsPreviewTable.getRowCount() - 1) {
                    myResultsPreviewTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    ScrollingUtil.ensureIndexIsVisible(myResultsPreviewTable, selectedRow - 1, 1);
                }
            });
            for (JComponent component : tableAware) {
                findNextAction.registerCustomShortcutSet(findNextShortcutSet, component);
                findPreviousAction.registerCustomShortcutSet(findPreviousShortcutSet, component);
            }
        }
        myUsagePreviewTitle = new SimpleColoredComponent();
        myUsageViewPresentation = new UsageViewPresentation();
        myUsagePreviewPanel = new MyUsagePreviewPanel(myProject, myUsageViewPresentation, true) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(myResultsPreviewTable.getWidth(), Math.max(getHeight(), getLineHeight() * 15));
            }
        };
        Disposer.register(myDisposable, myUsagePreviewPanel);
        final Runnable updatePreviewRunnable = () -> {
            if (Disposer.isDisposed(myDisposable)) return;
            int[] selectedRows = myResultsPreviewTable.getSelectedRows();
            final List<Promise<UsageInfo[]>> selectedUsagePromises = new SmartList<>();
            String file = null;
            for (int row : selectedRows) {
                Object value = myResultsPreviewTable.getModel().getValueAt(row, 0);
                UsageInfoAdapter adapter = (UsageInfoAdapter) value;
                file = adapter.getPath();
                if (adapter.isValid()) {
//                    selectedUsagePromises.add(adapter.getMergedInfosAsync());
                }
            }

            final String selectedFile = file;
            Promises.collectResults(selectedUsagePromises).onSuccess(data -> {
                final List<UsageInfo> selectedUsages = new SmartList<>();
                for (UsageInfo[] usageInfos : data) {
                    Collections.addAll(selectedUsages, usageInfos);
                }
                MyFindInProjectUtil.setupViewPresentation(myUsageViewPresentation, myHelper.getModel().clone());
                myUsagePreviewPanel.updateLayout(selectedUsages);
                myUsagePreviewTitle.clear();
                if (myUsagePreviewPanel.getCannotPreviewMessage(selectedUsages) == null && selectedFile != null) {
                    myUsagePreviewTitle.append(PathUtil.getFileName(selectedFile), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(selectedFile), true);
                    String locationPath = virtualFile == null ? null : getPresentablePath(myProject, virtualFile.getParent(), 120);
                    if (locationPath != null) {
                        myUsagePreviewTitle.append(spaceAndThinSpace() + locationPath,
                                new SimpleTextAttributes(STYLE_PLAIN, UIUtil.getContextHelpForeground()));
                    }
                }
            });
        };
        myResultsPreviewTable.getSelectionModel().addListSelectionListener(e -> {
            System.out.println("QQQQQQQQQQQQ");
            if (myResultsPreviewTable.getSelectedRow() != -1 && currentEditor != null && fileScope != null) {
                var usage = (UsageInfo2UsageAdapter) myResultsPreviewTableModel.getDataVector().get(myResultsPreviewTable.getSelectedRow()).get(0);
                var scrollingModel = currentEditor.getScrollingModel();
                int lineOffset = usage.getDocument().getLineStartOffset(usage.getLine() - 1);
                var verticalPosition = currentEditor.offsetToLogicalPosition(lineOffset);
                scrollingModel.scrollTo(verticalPosition, ScrollType.CENTER);

                MarkupModel markupModel = currentEditor.getMarkupModel();

                int lineStartOffset = currentEditor.getDocument().getLineStartOffset(usage.getLine());
                int lineEndOffset = currentEditor.getDocument().getLineEndOffset(usage.getLine());
//                var sth = currentEditor.getDocument().getText(new TextRange(lineStartOffset, lineEndOffset));


                Key<Boolean> IN_PREVIEW_USAGE_FLAG = Key.create("IN_PREVIEW_USAGE_FLAG");
                if (highlighter != null) {
                    highlighter.dispose();
                }
                highlighter = markupModel.addRangeHighlighter(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES,
                        lineStartOffset,
                        lineEndOffset,
                        HighlighterLayer.ADDITIONAL_SYNTAX,
                        HighlighterTargetArea.EXACT_RANGE);

                highlighter.putUserData(IN_PREVIEW_USAGE_FLAG, Boolean.TRUE);

            }

            if (e.getValueIsAdjusting() || Disposer.isDisposed(myPreviewUpdater)) return;
            myPreviewUpdater.addRequest(updatePreviewRunnable, 50); //todo[vasya]: remove this dirty hack of updating preview panel after clicking on Replace button
        });
        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (myDialog == null) return;
                if (e.getDocument() == mySearchComponent.getDocument()) {
                    scheduleResultsUpdate();
                }
                if (e.getDocument() == myReplaceComponent.getDocument()) {
                    applyTo(myHelper.getModel());
                    if (myHelper.getModel().isRegularExpressions()) {
                        myComponentValidator.updateInfo(getValidationInfo(myHelper.getModel()));
                    }
                    ApplicationManager.getApplication().invokeLater(updatePreviewRunnable);
                }
            }
        };
        mySearchComponent.getDocument().addDocumentListener(documentAdapter);
        myReplaceComponent.getDocument().addDocumentListener(documentAdapter);

        mySearchRescheduleOnCancellationsAlarm = new Alarm();

        myPreviewSplitter = new OnePixelSplitter(true, .33f);
        myPreviewSplitter.setSplitterProportionKey(SPLITTER_SERVICE_KEY);
        myPreviewSplitter.getDivider().setBackground(OnePixelDivider.BACKGROUND);
        JBScrollPane scrollPane = new JBScrollPane(myResultsPreviewTable) {
            @Override
            public Dimension getMinimumSize() {
                Dimension size = super.getMinimumSize();
                size.height = myResultsPreviewTable.getPreferredScrollableViewportSize().height;
                return size;
            }
        };
        scrollPane.setBorder(JBUI.Borders.empty());
        myPreviewSplitter.setFirstComponent(scrollPane);
        myOKHintLabel = new JBLabel("");
        myOKHintLabel.setEnabled(false);
        myNavigationHintLabel = new JBLabel("");
        myNavigationHintLabel.setEnabled(false);
        myNavigationHintLabel.setFont(JBUI.Fonts.smallFont());
        Insets insets = myOKButton.getInsets();
        String btnGapLeft = "gapleft " + Math.max(0, JBUIScale.scale(12) - insets.left - insets.right);


        myCodePreviewComponent = myUsagePreviewPanel.createComponent();
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(myUsagePreviewTitle, BorderLayout.NORTH);
        previewPanel.add(myCodePreviewComponent, BorderLayout.CENTER);
        myPreviewSplitter.setSecondComponent(previewPanel);
        setLayout(new MigLayout("flowx, ins 0, gap 0, fillx, hidemode 3"));

        myIsPinned.set(UISettings.getInstance().getPinFindInPath());

        mySearchTextArea.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 1, 0, 1, 0),
                JBUI.Borders.empty(1, 0, 2, 0)));
        myReplaceTextArea.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(1, 0, 2, 0)));
        myUsagePreviewTitle.setBorder(JBUI.Borders.empty(3, 8, 4, 8));

        add(mySearchTextArea, "pushx, growx, wrap");
        add(myReplaceTextArea, "pushx, growx, wrap");
        add(myPreviewSplitter, "pushx, growx, growy, pushy, wrap");

        List<Component> focusOrder = new ArrayList<>();
        focusOrder.add(mySearchComponent);
        focusOrder.add(myReplaceComponent);
        focusOrder.addAll(searchExtraButtons);
        focusOrder.addAll(replaceExtraButtons);
        setFocusCycleRoot(true);
        setFocusTraversalPolicy(new ListFocusTraversalPolicy(focusOrder));

        if (SystemInfo.isMac) {
            List<JButton> principalButtons = new ArrayList<>();
            principalButtons.add(myOKButton);

//            Touchbar.setButtonActions(bottomPanel, null, principalButtons, myOKButton, new DefaultActionGroup(myCaseSensitiveAction, myWholeWordsAction, myRegexAction));
        }
    }

    @Contract("_,!null,_->!null")
    private static @NlsSafe String getPresentablePath(@NotNull Project project, @Nullable VirtualFile virtualFile, int maxChars) {
        if (virtualFile == null) return null;
        String path = ScratchUtil.isScratch(virtualFile)
                ? ScratchUtil.getRelativePath(project, virtualFile)
                : VfsUtilCore.isAncestor(project.getBaseDir(), virtualFile, true)
                ? VfsUtilCore.getRelativeLocation(virtualFile, project.getBaseDir())
                : FileUtil.getLocationRelativeToUserHome(virtualFile.getPath());
        return path == null ? null : maxChars < 0 ? path : StringUtil.trimMiddle(path, maxChars);
    }

    @NotNull
    private DefaultTableModel createTableModel() {
        final DefaultTableModel model = new DefaultTableModel() {
            private String firstResultPath;

            private final Comparator<Vector<UsageInfoAdapter>> COMPARATOR = (v1, v2) -> {
                UsageInfoAdapter u1 = v1.get(0);
                UsageInfoAdapter u2 = v2.get(0);
                String u2Path = u2.getPath();
                final String u1Path = u1.getPath();
                if (u1Path.equals(firstResultPath) && !u2Path.equals(firstResultPath))
                    return -1; // first result is always sorted first
                if (!u1Path.equals(firstResultPath) && u2Path.equals(firstResultPath)) return 1;
                int c = u1Path.compareTo(u2Path);
                if (c != 0) return c;
                c = Integer.compare(u1.getLine(), u2.getLine());
                if (c != 0) return c;
                return Integer.compare(u1.getNavigationOffset(), u2.getNavigationOffset());
            };

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @SuppressWarnings({"UseOfObsoleteCollectionType", "unchecked", "rawtypes"})
            @Override
            //Inserts search results in sorted order
            public void addRow(Object[] rowData) {
                if (myNeedReset.compareAndSet(true, false)) {
                    dataVector.clear();
                    fireTableDataChanged();
                }
                final Vector<UsageInfoAdapter> v = (Vector) convertToVector(rowData);
                if (dataVector.isEmpty()) {
                    addRow(v);
                } else {
                    firstResultPath = v.get(0).getPath();
                    int p = Collections.binarySearch((Vector<Vector<UsageInfoAdapter>>) ((Vector) dataVector), v, COMPARATOR);
                    if (p < 0) {
                        // only insert when not already present.
                        int row = -(p + 1);
                        this.insertRow(row, v);
                    }
                }


                if (fileScope != null) {
                    var line = FileEditorManager.getInstance(myProject).getSelectedTextEditor().getCaretModel().getLogicalPosition().line;
                    myResultsPreviewTable.getSelectionModel().setSelectionInterval(0, 0);
                    Vector<Vector<UsageInfoAdapter>> vec = (Vector<Vector<UsageInfoAdapter>>) ((Vector) dataVector);
                    var vecList = new ArrayList<>(vec);
                    var closestLine = ListUtils.zipWithIndex(vecList)
                            .stream()
                            .map(usage -> new Pair<>(usage.getFirst().get(0).getLine(), usage.getSecond()))
                            .map(pair -> new Pair<>(pair.first - line, pair.second))
                            .map(pair -> new Pair<>(Math.abs(pair.first), pair.second))
                            .min(new Comparator<Pair<Integer, Integer>>() {
                                @Override
                                public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                                    return o1.first - o2.first;
                                }
                            });
                    closestLine.ifPresent(l -> {
                        System.out.println("WWWWWWWWWWWWWWWWWWWWWW");
                        System.out.println(l);
                        myResultsPreviewTable.getSelectionModel().setSelectionInterval(l.second, l.second);
//                    myResultsPreviewTable.getSelectionModel().setLeadSelectionIndex(l);
                    });
                } else {
                    myResultsPreviewTable.getSelectionModel().setSelectionInterval(0, 0);
                }
            }
        };

        model.addColumn("Usages");
        return model;
    }


    private void closeImmediately() {
        if (canBeClosedImmediately() && myDialog != null && myDialog.isVisible()) {
            myIsPinned.set(false);
            myDialog.doCancelAction();
        }
    }

    //Some popups shown above may prevent panel closing, first of all we should close them
    private boolean canBeClosedImmediately() {
        boolean state = myIsPinned.get();
        myIsPinned.set(false);
        try {
            //Here we actually close popups
            return myDialog != null && canBeClosed();
        } finally {
            myIsPinned.set(state);
        }
    }

    private void doOK(boolean openInFindWindow) {
        if (!canBeClosedImmediately()) {
            return;
        }

        FindModel validateModel = myHelper.getModel().clone();
        applyTo(validateModel);

        ValidationInfo validationInfo = getValidationInfo(validateModel);

        if (validationInfo == null) {
            if (validateModel.isReplaceState() &&
                    !openInFindWindow &&
                    myResultsPreviewTable.getRowCount() > 1 &&
                    !ReplaceInProjectManager.getInstance(myProject).showReplaceAllConfirmDialog(
                            myUsagesCount,
                            getStringToFind(),
                            myFilesCount,
                            getStringToReplace())) {
                return;
            }
            myHelper.getModel().copyFrom(validateModel);
            myHelper.getModel().setPromptOnReplace(openInFindWindow);
            myHelper.doOKAction();
        } else {
            String message = validationInfo.message;
            Messages.showMessageDialog(this, message, CommonBundle.getErrorTitle(), Messages.getErrorIcon());
            return;
        }
        myIsPinned.set(false);
        myDialog.doCancelAction();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ApplicationManager.getApplication().invokeLater(() -> ScrollingUtil.ensureSelectionExists(myResultsPreviewTable), ModalityState.any());
    }

    @Override
    public void initByModel() {
        FindModel myModel = myHelper.getModel();
        myCaseSensitiveState.set(myModel.isCaseSensitive());
        myWholeWordsState.set(myModel.isWholeWordsOnly());
        myRegexState.set(myModel.isRegularExpressions());

        mySelectedContextName = getSearchContextName(myModel);
        if (myModel.isReplaceState()) {
            myPreserveCaseState.set(myModel.isPreserveCase());
        }

        mySelectedScope = globalScopeType;

        boolean isThereFileFilter = myModel.getFileFilter() != null && !myModel.getFileFilter().isEmpty();
        FindSettings findSettings = FindSettings.getInstance();
        @NlsSafe String[] fileMasks = findSettings.getRecentFileMasks();
        String toSearch = myModel.getStringToFind();
        FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);

        if (StringUtil.isEmpty(toSearch)) {
            String[] history = findInProjectSettings.getRecentFindStrings();
            toSearch = history.length > 0 ? history[history.length - 1] : "";
        }

        mySearchComponent.setText(toSearch);
        String toReplace = myModel.getStringToReplace();

        if (StringUtil.isEmpty(toReplace)) {
            String[] history = findInProjectSettings.getRecentReplaceStrings();
            toReplace = history.length > 0 ? history[history.length - 1] : "";
        }
        myReplaceComponent.setText(toReplace);
        updateControls();

        boolean isReplaceState = myHelper.isReplaceState();
        myReplaceTextArea.setVisible(isReplaceState);
        myOKHintLabel.setText(KeymapUtil.getKeystrokeText(ENTER_WITH_MODIFIERS));
        myOKButton.setText(FindBundle.message("find.popup.find.button"));
    }

    private void updateControls() {
        myNavigationHintLabel.setVisible(mySearchComponent.getText().contains("\n"));
        mySearchTextArea.updateExtraActions();
        myReplaceTextArea.updateExtraActions();
        if (myNavigationHintLabel.isVisible()) {
            myNavigationHintLabel.setText("");
            KeymapManager keymapManager = KeymapManager.getInstance();
            Keymap activeKeymap = keymapManager != null ? keymapManager.getActiveKeymap() : null;
            if (activeKeymap != null) {
                String findNextText = KeymapUtil.getFirstKeyboardShortcutText("FindNext");
                String findPreviousText = KeymapUtil.getFirstKeyboardShortcutText("FindPrevious");
                if (!StringUtil.isEmpty(findNextText) && !StringUtil.isEmpty(findPreviousText)) {
                    myNavigationHintLabel.setText(FindBundle.message("label.use.0.and.1.to.select.usages", findNextText, findPreviousText));
                }
            }
        }
    }

    public void scheduleResultsUpdate() {
        if (myDialog == null || !myDialog.isVisible()) return;
        if (mySearchRescheduleOnCancellationsAlarm == null || mySearchRescheduleOnCancellationsAlarm.isDisposed())
            return;
        updateControls();
        mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
        mySearchRescheduleOnCancellationsAlarm.addRequest(this::findSettingsChanged, 100);
    }

    private void finishPreviousPreviewSearch() {
        if (myResultsPreviewSearchProgress != null && !myResultsPreviewSearchProgress.isCanceled()) {
            myResultsPreviewSearchProgress.cancel();
        }
    }

    private void findSettingsChanged() {
        if (isShowing()) {
            ScrollingUtil.ensureSelectionExists(myResultsPreviewTable);
        }
        final ModalityState state = ModalityState.current();
        finishPreviousPreviewSearch();
        mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
        applyTo(myHelper.getModel());
        FindModel findModel = new FindModel();
        findModel.copyFrom(myHelper.getModel());
        if (findModel.getStringToFind().contains("\n")) {
            findModel.setMultiline(true);
        }

        ValidationInfo result = getValidationInfo(myHelper.getModel());
        myComponentValidator.updateInfo(result);

        final ProgressIndicatorBase progressIndicatorWhenSearchStarted = new ProgressIndicatorBase() {
            @Override
            public void stop() {
                super.stop();
                onStop(System.identityHashCode(this));
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (myNeedReset.compareAndSet(true, false)) { //nothing is found, let's clear previous results
                        reset();
                    }
                });
            }
        };
        myResultsPreviewSearchProgress = progressIndicatorWhenSearchStarted;
        final int hash = System.identityHashCode(myResultsPreviewSearchProgress);

        // Use previously shown usage files as hint for faster search and better usage preview performance if pattern length increased
        Set<VirtualFile> filesToScanInitially = new LinkedHashSet<>();

        if (myHelper.myPreviousModel != null && myHelper.myPreviousModel.getStringToFind().length() < myHelper.getModel().getStringToFind().length()) {
            final DefaultTableModel previousModel = (DefaultTableModel) myResultsPreviewTable.getModel();
            for (int i = 0, len = previousModel.getRowCount(); i < len; ++i) {
                final Object value = previousModel.getValueAt(i, 0);
                if (value instanceof UsageInfo2UsageAdapter) {
                    final UsageInfo2UsageAdapter usage = (UsageInfo2UsageAdapter) value;
                    final VirtualFile file = usage.getFile();
                    if (file != null) filesToScanInitially.add(file);
                }
            }
        }

        myHelper.myPreviousModel = myHelper.getModel().clone();

        onStart(hash);
        if (result != null && result.component != myReplaceComponent) {
            onStop(hash, result.message);
            reset();
            return;
        }

        MyFindInProjectExecutor projectExecutor = new MyFindInProjectExecutor();
        GlobalSearchScope scope = GlobalSearchScopeUtil.toGlobalSearchScope(
                MyFindInProjectUtil.getScopeFromModel(myProject, myHelper.myPreviousModel), myProject);
        TableCellRenderer renderer = projectExecutor.createTableCellRenderer();
        if (renderer == null) renderer = new MyFindPopupPanel.UsageTableCellRenderer(scope);
        myResultsPreviewTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

        final Ref<Integer> resultsCount = Ref.create(0);
        final AtomicInteger resultsFilesCount = new AtomicInteger();
        FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, findModel);
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(new Task.Backgroundable(myProject,
                FindBundle.message("find.usages.progress.title")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final FindUsagesProcessPresentation processPresentation =
                        FindInProjectUtil.setupProcessPresentation(myProject, myUsageViewPresentation);
                ThreadLocal<String> lastUsageFileRef = new ThreadLocal<>();
                ThreadLocal<Reference<Usage>> recentUsageRef = new ThreadLocal<>();
                if (fileScope == null) {
                    findModel.setCustomScope(false);
                    findModel.setProjectScope(true);
                } else {
                    findModel.setProjectScope(false);
                    findModel.setCustomScope(true);
                    findModel.setCustomScope(fileScope);
                }
                StringConverter converter = new StringConverter();
                searchWords = findModel
                        .getStringToFind()
                        .split(" ");
                String s = findModel.isProjectScope() ?
                        converter.convertPermutations(findModel.getStringToFind()) :
                        converter.convert(findModel.getStringToFind());
                currentSearchAfterChanges = s;


                findModel.setStringToFind(s);
                findModel.setRegularExpressions(true);
                projectExecutor.findUsages(myProject, myResultsPreviewSearchProgress, processPresentation, findModel, filesToScanInitially, usage -> {
                    if (isCancelled()) {
                        onStop(hash);
                        return false;
                    }

                    synchronized (resultsCount) {
                        if (resultsCount.get() >= ShowUsagesAction.getUsagesPageSize()) {
                            onStop(hash);
                            return false;
                        }
                        resultsCount.set(resultsCount.get() + 1);
                    }

                    String file = lastUsageFileRef.get();
                    String usageFile = PathUtil.toSystemIndependentName(usage.getPath());
                    if (!usageFile.equals(file)) {
                        resultsFilesCount.incrementAndGet();
                        lastUsageFileRef.set(usageFile);
                    }

                    Usage recent = SoftReference.dereference(recentUsageRef.get());
                    UsageInfoAdapter recentAdapter = recent instanceof UsageInfoAdapter ? (UsageInfoAdapter) recent : null;
                    final boolean merged = !myHelper.isReplaceState() && recentAdapter != null && recentAdapter.merge(usage);
                    if (!merged) {
                        recentUsageRef.set(new WeakReference<>(usage));
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (isCancelled()) {
                            onStop(hash);
                            return;
                        }
                        //second component to jest chyba ten code preview duzy, w aktualnym matchu
                        myPreviewSplitter.getSecondComponent().setVisible(true);
                        DefaultTableModel model = (DefaultTableModel) myResultsPreviewTable.getModel();
                        //todo tutaj chyba dodajemy wszystkie elementy do tabeli
                        if (!merged) {
                            model.addRow(new Object[]{usage});
                        } else {
                            model.fireTableRowsUpdated(model.getRowCount() - 1, model.getRowCount() - 1);
                        }
                        myCodePreviewComponent.setVisible(true);
                        if (model.getRowCount() == 1) {
                            myResultsPreviewTable.setRowSelectionInterval(0, 0);
                        }
                        int occurrences;
                        synchronized (resultsCount) {
                            occurrences = resultsCount.get();
                        }
                        int filesWithOccurrences = resultsFilesCount.get();
                        myCodePreviewComponent.setVisible(occurrences > 0);

                        if (occurrences > 0) {
                            if (occurrences < ShowUsagesAction.getUsagesPageSize()) {
                                myUsagesCount = String.valueOf(occurrences);
                                myFilesCount = String.valueOf(filesWithOccurrences);
                            } else {
                                myUsagesCount = occurrences + "+";
                                myFilesCount = filesWithOccurrences + "+";
                            }
                        } else {
                        }
                    }, state);

                    return true;
                });
                //todo sprobojmy tutaj zmienic ten aktualny row
                //todo tutaj nie dziala

            }

            @Override
            public void onCancel() {
                if (isShowing() && progressIndicatorWhenSearchStarted == myResultsPreviewSearchProgress) {
                    scheduleResultsUpdate();
                }

            }

            boolean isCancelled() {
                return progressIndicatorWhenSearchStarted != myResultsPreviewSearchProgress || progressIndicatorWhenSearchStarted.isCanceled();
            }

            @Override
            public void onFinished() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isCancelled()) {
                        boolean isEmpty;
                        synchronized (resultsCount) {
                            isEmpty = resultsCount.get() == 0;
                        }
                        if (isEmpty) {
                            showEmptyText(null);
                        }
                    }
                    onStop(hash);
                }, state);
            }
        }, myResultsPreviewSearchProgress);
    }

    private void reset() {
        ((DefaultTableModel) myResultsPreviewTable.getModel()).getDataVector().clear();
        ((DefaultTableModel) myResultsPreviewTable.getModel()).fireTableDataChanged();
        myResultsPreviewTable.getSelectionModel().clearSelection();
        myPreviewSplitter.getSecondComponent().setVisible(false);
    }

    private void showEmptyText(@Nullable @NlsContexts.StatusText String message) {
        StatusText emptyText = myResultsPreviewTable.getEmptyText();
        emptyText.clear();
        FindModel model = myHelper.getModel();
        boolean dotAdded = false;
        if (StringUtil.isEmpty(model.getStringToFind())) {
            emptyText.setText(FindBundle.message("message.type.search.query"));
        } else {
            emptyText.setText(message != null ? message : FindBundle.message("message.nothingFound"));
        }
        if (mySelectedScope == MyFindPopupScopeUIImpl.DIRECTORY && !model.isWithSubdirectories()) {
            emptyText.appendText(".");
            dotAdded = true;
            emptyText.appendSecondaryText(FindBundle.message("find.recursively.hint"),
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> {
                        model.setWithSubdirectories(true);
                        scheduleResultsUpdate();
                    });
        }
        List<Object> usedOptions = new SmartList<>();
        if (model.isCaseSensitive() && isEnabled(myCaseSensitiveAction)) {
            usedOptions.add(myCaseSensitiveAction);
        }
        if (model.isWholeWordsOnly() && isEnabled(myWholeWordsAction)) {
            usedOptions.add(myWholeWordsAction);
        }
        if (model.isRegularExpressions() && isEnabled(myRegexAction)) {
            usedOptions.add(myRegexAction);
        }
        boolean couldBeRegexp = false;
        if (mySuggestRegexHintForEmptyResults) {
            String stringToFind = model.getStringToFind();
            if (!model.isRegularExpressions() && isEnabled(myRegexAction)) {
                String regexSymbols = ".$|()[]{}^?*+\\";
                for (int i = 0; i < stringToFind.length(); i++) {
                    if (regexSymbols.indexOf(stringToFind.charAt(i)) != -1) {
                        couldBeRegexp = true;
                        break;
                    }
                }
            }
            if (couldBeRegexp) {
                try {
                    Pattern.compile(stringToFind);
                    usedOptions.add(myRegexAction);
                } catch (Exception e) {
                    couldBeRegexp = false;
                }
            }
        }
        String fileTypeMask = getFileTypeMask();
        if (model.isInCommentsOnly()
                || model.isInStringLiteralsOnly()
                || model.isExceptComments()
                || model.isExceptStringLiterals()
                || model.isExceptCommentsAndStringLiterals()) {
            usedOptions.add(model);
        }
        if (!usedOptions.isEmpty()) {
            if (!dotAdded) emptyText.appendText(".");
            emptyText.appendLine(" ");
            if (couldBeRegexp) {
                emptyText.appendLine(FindBundle.message("message.nothingFound.search.with.regex"), LINK_PLAIN_ATTRIBUTES, __ -> {
                    toggleOption(myRegexAction);
                    mySuggestRegexHintForEmptyResults = false;
                }).appendText(" " + KeymapUtil.getFirstKeyboardShortcutText(myRegexAction.getShortcutSet()));
            }
        }
    }

    private boolean isEnabled(@NotNull AnAction action) {
        Presentation presentation = new Presentation();
        action.update(new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0));
        return presentation.isEnabled();
    }

    @Nullable
    private static @NlsContexts.StatusText String getOptionText(Object option, boolean full) {
        if (option instanceof AnAction) {
            String text = ((AnAction) option).getTemplateText();
            if (text == null) text = "";
            return (text + (full ? " " + KeymapUtil.getFirstKeyboardShortcutText(((AnAction) option).getShortcutSet()) : "")).trim();
        }
        if (option instanceof JToggleButton) {
            CustomShortcutSet shortcutSet = KeymapUtil.getShortcutsForMnemonicCode(((JToggleButton) option).getMnemonic());
            return (((JToggleButton) option).getText().replace(":", "") +
                    (shortcutSet != null && full ? " " + KeymapUtil.getFirstKeyboardShortcutText(shortcutSet) : "")).trim();
        }
        if (option instanceof FindModel) return FindBundle.message("message.nothingFound.context.filter");
        return null;
    }

    private void toggleOption(@NotNull Object option) {
        if (option instanceof AnAction) {
            ((AnAction) option).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
        } else if (option instanceof JToggleButton) {
            ((JToggleButton) option).doClick();
        } else if (option instanceof FindModel) {
            mySelectedContextName = FindInProjectUtil.getPresentableName(FindModel.SearchContext.ANY);
            scheduleResultsUpdate();
        }
    }

    private void onStart(int hash) {
        myNeedReset.set(true);
        myLoadingHash = hash;
        myResultsPreviewTable.getEmptyText().setText(FindBundle.message("empty.text.searching"));
    }


    private void onStop(int hash) {
        onStop(hash, null);
    }

    private void onStop(int hash, String message) {
        if (hash != myLoadingHash) {
            return;
        }
        UIUtil.invokeLaterIfNeeded(() -> {
            //noinspection HardCodedStringLiteral
            showEmptyText(message);
        });
    }

    @Override
    @Nullable
    public String getFileTypeMask() {
        return null;
    }

    @Nullable("null means OK")
    private ValidationInfo getValidationInfo(@NotNull FindModel model) {
        if (!myHelper.canSearchThisString()) {
            return new ValidationInfo(FindBundle.message("find.empty.search.text.error"), mySearchComponent);
        }

        if (model.isRegularExpressions()) {
            String toFind = model.getStringToFind();
            Pattern pattern;
            try {
                pattern = Pattern.compile(toFind, model.isCaseSensitive() ? Pattern.MULTILINE : Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                if (pattern.matcher("").matches() && !toFind.endsWith("$") && !toFind.startsWith("^")) {
                    return new ValidationInfo(FindBundle.message("find.empty.match.regular.expression.error"), mySearchComponent);
                }
            } catch (PatternSyntaxException e) {
                return new ValidationInfo(FindBundle.message("find.invalid.regular.expression.error", toFind, e.getDescription()),
                        mySearchComponent);
            }
            if (model.isReplaceState()) {
                if (myResultsPreviewTable.getRowCount() > 0) {
                    //todo TUTAJ CHYBA
                    int myNewRow;
                    if (myResultsPreviewTable.getModel().getRowCount() > 1) {
                        myNewRow = 1;
                    } else myNewRow = 0;
                    Object value = myResultsPreviewTable.getModel().getValueAt(myNewRow, 0);
                    if (value instanceof Usage) {
                        try {
                            // Just check
                            ReplaceInProjectManager.getInstance(myProject).replaceUsage((Usage) value, model, Collections.emptySet(), true);
                        } catch (FindManager.MalformedReplacementStringException e) {
                            return new ValidationInfo(e.getMessage(), myReplaceComponent);
                        }
                    }
                }

                try {
                    RegExReplacementBuilder.validate(pattern, getStringToReplace());
                } catch (IllegalArgumentException e) {
                    return new ValidationInfo(FindBundle.message("find.replace.invalid.replacement.string", e.getMessage()),
                            myReplaceComponent);
                }
            }
        }

        final String mask = getFileTypeMask();

        if (mask != null) {
        }
        return null;
    }

    @Override
    @NotNull
    public String getStringToFind() {
        return mySearchComponent.getText();
    }

    @NotNull
    private String getStringToReplace() {
        return myReplaceComponent.getText();
    }

    private void applyTo(@NotNull FindModel model) {
        model.setCaseSensitive(myCaseSensitiveState.get());
        if (model.isReplaceState()) {
            model.setPreserveCase(myPreserveCaseState.get());
        }
        model.setWholeWordsOnly(myWholeWordsState.get());

        FindModel.SearchContext searchContext = parseSearchContext(mySelectedContextName);

        model.setSearchContext(searchContext);
        model.setRegularExpressions(myRegexState.get());
        model.setStringToFind(getStringToFind());

        if (model.isReplaceState()) {
            model.setStringToReplace(StringUtil.convertLineSeparators(getStringToReplace()));
        }

        model.setProjectScope(false);
        model.setDirectoryName(null);
        model.setModuleName(null);
        model.setCustomScopeName(null);
        model.setCustomScope(null);
        model.setCustomScope(false);
        model.setFindAll(false);

        String mask = getFileTypeMask();
        model.setFileFilter(mask);
    }

    private void navigateToSelectedUsage(@Nullable AnActionEvent e) {
        System.out.println("YYYYYYYYYYYYYYYYYYYYYYYYY");
        wasSelected = true;
        System.out.println("selected");
        Navigatable[] navigatables = e != null ? e.getData(CommonDataKeys.NAVIGATABLE_ARRAY) : null;
        if (navigatables != null) {
            if (canBeClosed()) {
                myDialog.doCancelAction();
            }
            OpenSourceUtil.navigate(navigatables);
            return;
        }

        Map<Integer, Usage> usages = getSelectedUsages();
        if (usages != null) {
            if (canBeClosed()) {
                myDialog.doCancelAction();
            }
            boolean first = true;
            for (Usage usage : usages.values()) {
                if (first) {
                    usage.navigate(true);
                } else {
                    usage.highlightInEditor();
                }
                first = false;
            }
        }
    }

    @Nullable
    private Map<Integer, Usage> getSelectedUsages() {
        int[] rows = myResultsPreviewTable.getSelectedRows();
        Map<Integer, Usage> result = null;
        for (int i = rows.length - 1; i >= 0; i--) {
            int row = rows[i];
            Object valueAt = myResultsPreviewTable.getModel().getValueAt(row, 0);
            if (valueAt instanceof Usage) {
                if (result == null) result = new LinkedHashMap<>();
                result.put(row, (Usage) valueAt);
            }
        }
        return result;
    }

    @NotNull
    public static ActionToolbarImpl createToolbar(AnAction @NotNull ... actions) {
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, new DefaultActionGroup(actions), true);
        toolbar.setForceMinimumSize(true);
        toolbar.setTargetComponent(toolbar);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        toolbar.setBorder(JBUI.Borders.empty(3));
        return toolbar;
    }

    @NotNull
    private static ActionToolbarImpl createScopeToolbar(AnAction @NotNull ... actions) {
        ActionToolbarImpl toolbar = new ActionToolbarImpl(ActionPlaces.EDITOR_TOOLBAR, new DefaultActionGroup(actions), true) {
            @Override
            protected @NotNull ActionButton createToolbarButton(@NotNull AnAction action,
                                                                ActionButtonLook look,
                                                                @NotNull String place,
                                                                @NotNull Presentation presentation,
                                                                @NotNull Dimension minimumSize) {
                if (!ExperimentalUI.isNewUI()) {
                    return super.createToolbarButton(action, look, place, presentation, minimumSize);
                }

                ActionButtonWithText result = new ActionButtonWithText(action, presentation, place, minimumSize) {
                    @Override
                    protected Insets getMargins() {
                        return new JBInsets(4, 6, 4, 6);
                    }
                };

                result.setLook(look);
                result.setBorder(JBUI.Borders.emptyRight(4));
                result.setFont(JBFont.medium());

                return result;
            }
        };

        toolbar.setForceMinimumSize(true);
        toolbar.setTargetComponent(toolbar);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);

        return toolbar;
    }

    private static void applyFont(@NotNull JBFont font, Component @NotNull ... components) {
        for (Component component : components) {
            component.setFont(font);
        }
    }

    private static void createFileMaskRegExp(@NotNull String filter) throws PatternSyntaxException {
        String pattern;
        final List<String> strings = StringUtil.split(filter, ",");
        if (strings.size() == 1) {
            pattern = PatternUtil.convertToRegex(filter.trim());
        } else {
            pattern = StringUtil.join(strings, s -> "(" + PatternUtil.convertToRegex(s.trim()) + ")", "|");
        }
        // just check validity
        //noinspection ResultOfMethodCallIgnored
        Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    @NotNull
    private static String getSearchContextName(@NotNull FindModel model) {
        String searchContext = FindBundle.message("find.context.anywhere.scope.label");
        if (model.isInCommentsOnly()) searchContext = FindBundle.message("find.context.in.comments.scope.label");
        else if (model.isInStringLiteralsOnly())
            searchContext = FindBundle.message("find.context.in.literals.scope.label");
        else if (model.isExceptStringLiterals())
            searchContext = FindBundle.message("find.context.except.literals.scope.label");
        else if (model.isExceptComments())
            searchContext = FindBundle.message("find.context.except.comments.scope.label");
        else if (model.isExceptCommentsAndStringLiterals())
            searchContext = FindBundle.message("find.context.except.comments.and.literals.scope.label");
        return searchContext;
    }

    @NotNull
    private static FindModel.SearchContext parseSearchContext(String presentableName) {
        FindModel.SearchContext searchContext = FindModel.SearchContext.ANY;
        if (FindBundle.message("find.context.in.literals.scope.label").equals(presentableName)) {
            searchContext = FindModel.SearchContext.IN_STRING_LITERALS;
        } else if (FindBundle.message("find.context.in.comments.scope.label").equals(presentableName)) {
            searchContext = FindModel.SearchContext.IN_COMMENTS;
        } else if (FindBundle.message("find.context.except.comments.scope.label").equals(presentableName)) {
            searchContext = FindModel.SearchContext.EXCEPT_COMMENTS;
        } else if (FindBundle.message("find.context.except.literals.scope.label").equals(presentableName)) {
            searchContext = FindModel.SearchContext.EXCEPT_STRING_LITERALS;
        } else if (FindBundle.message("find.context.except.comments.and.literals.scope.label").equals(presentableName)) {
            searchContext = FindModel.SearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS;
        }
        return searchContext;
    }

    @NotNull
    private static JBIterable<Component> focusableComponents(@NotNull Component component) {
        return UIUtil.uiTraverser(component)
                .bfsTraversal()
                .filter(c -> c instanceof JComboBox || c instanceof AbstractButton || c instanceof JTextComponent);
    }

    public enum ToggleOptionName {CaseSensitive, PreserveCase, WholeWords, Regex, FileFilter}

    private final class MySwitchStateToggleAction extends DumbAwareToggleAction implements TooltipLinkProvider, TooltipDescriptionProvider {
        private final MyFindPopupPanel.ToggleOptionName myOptionName;
        private final AtomicBoolean myState;
        private final Producer<Boolean> myEnableStateProvider;
        private final TooltipLink myTooltipLink;

        private MySwitchStateToggleAction(@NotNull String message,
                                          @NotNull MyFindPopupPanel.ToggleOptionName optionName,
                                          @NotNull Icon icon, @NotNull Icon hoveredIcon, @NotNull Icon selectedIcon,
                                          @NotNull AtomicBoolean state,
                                          @NotNull Producer<Boolean> enableStateProvider) {
            this(message, optionName, icon, hoveredIcon, selectedIcon, state, enableStateProvider, null);
        }

        private MySwitchStateToggleAction(@NotNull String message,
                                          @NotNull MyFindPopupPanel.ToggleOptionName optionName,
                                          @NotNull Icon icon, @NotNull Icon hoveredIcon, @NotNull Icon selectedIcon,
                                          @NotNull AtomicBoolean state,
                                          @NotNull Producer<Boolean> enableStateProvider,
                                          @Nullable TooltipLink tooltipLink) {
            super(FindBundle.message(message), null, icon);
            myOptionName = optionName;
            myState = state;
            myEnableStateProvider = enableStateProvider;
            myTooltipLink = tooltipLink;
            getTemplatePresentation().setHoveredIcon(hoveredIcon);
            getTemplatePresentation().setSelectedIcon(selectedIcon);
            ShortcutSet shortcut = ActionUtil.getMnemonicAsShortcut(this);
            if (shortcut != null) {
                setShortcutSet(shortcut);
                registerCustomShortcutSet(shortcut, MyFindPopupPanel.this);
            }
        }

        @Override
        public @Nullable TooltipLink getTooltipLink(@Nullable JComponent owner) {
            return myTooltipLink;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return myState.get();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(myEnableStateProvider.produce());
            Toggleable.setSelected(e.getPresentation(), myState.get());
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean selected) {
            myState.set(selected);
            if (myState == myRegexState) {
                mySuggestRegexHintForEmptyResults = false;
                if (selected) myWholeWordsState.set(false);
            }
            scheduleResultsUpdate();
        }
    }

    private class MySwitchContextToggleAction extends ToggleAction implements DumbAware {
        MySwitchContextToggleAction(@NotNull FindModel.SearchContext context) {
            super(FindInProjectUtil.getPresentableName(context));
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return Objects.equals(mySelectedContextName, getTemplatePresentation().getText());
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            if (state) {
                mySelectedContextName = getTemplatePresentation().getText();
                scheduleResultsUpdate();
            }
        }
    }

    private class MyShowFilterPopupAction extends DumbAwareAction {
        private final PopupState<JBPopup> myPopupState = PopupState.forPopup();
        private final DefaultActionGroup mySwitchContextGroup;

        MyShowFilterPopupAction() {
            super(FindBundle.messagePointer("find.popup.show.filter.popup"), Presentation.NULL_STRING, AllIcons.General.Filter);
            KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
            if (keyboardShortcut != null) {
                setShortcutSet(new CustomShortcutSet(keyboardShortcut));
            }
            mySwitchContextGroup = new DefaultActionGroup();
            mySwitchContextGroup.add(new MyFindPopupPanel.MySwitchContextToggleAction(FindModel.SearchContext.ANY));
            mySwitchContextGroup.add(new MyFindPopupPanel.MySwitchContextToggleAction(FindModel.SearchContext.IN_COMMENTS));
            mySwitchContextGroup.add(new MyFindPopupPanel.MySwitchContextToggleAction(FindModel.SearchContext.IN_STRING_LITERALS));
            mySwitchContextGroup.add(new MyFindPopupPanel.MySwitchContextToggleAction(FindModel.SearchContext.EXCEPT_COMMENTS));
            mySwitchContextGroup.add(new MyFindPopupPanel.MySwitchContextToggleAction(FindModel.SearchContext.EXCEPT_STRING_LITERALS));
            mySwitchContextGroup.add(new MyFindPopupPanel.MySwitchContextToggleAction(FindModel.SearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS));
            mySwitchContextGroup.setPopup(true);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) == null) return;
            if (myPopupState.isRecentlyHidden()) return;

            ListPopup listPopup =
                    JBPopupFactory.getInstance().createActionGroupPopup(null, mySwitchContextGroup, e.getDataContext(), false, null, 10);
            myPopupState.prepareToShow(listPopup);
        }
    }


    static class UsageTableCellRenderer extends JPanel implements TableCellRenderer {
        private final ColoredTableCellRenderer myUsageRenderer = new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                if (value instanceof UsageInfo2UsageAdapter) {
                    if (!((UsageInfo2UsageAdapter) value).isValid()) {
                        myUsageRenderer.append(" " + UsageViewBundle.message("node.invalid") + " ", SimpleTextAttributes.ERROR_ATTRIBUTES);
                    }
                    TextChunk[] text = ((UsageInfo2UsageAdapter) value).getPresentation().getText();

                    // skip line number / file info
                    for (int i = 1; i < text.length; ++i) {
                        TextChunk textChunk = text[i];
                        SimpleTextAttributes attributes = getAttributes(textChunk);
                        myUsageRenderer.append(textChunk.getText(), attributes);
                    }
                }
                setBorder(null);
            }

            @NotNull
            private SimpleTextAttributes getAttributes(@NotNull TextChunk textChunk) {
                SimpleTextAttributes at = textChunk.getSimpleAttributesIgnoreBackground();
                boolean highlighted = at.getFontStyle() == Font.BOLD;
                return highlighted
                        ? new SimpleTextAttributes(null, at.getFgColor(), at.getWaveColor(),
                        at.getStyle() & ~SimpleTextAttributes.STYLE_BOLD |
                                SimpleTextAttributes.STYLE_SEARCH_MATCH)
                        : at;
            }
        };

        public static final SimpleTextAttributes ORDINAL_ATTRIBUTES = new SimpleTextAttributes(STYLE_PLAIN, JBColor.namedColor("Component.infoForeground", 0x999999, 0x999999));
        public static final SimpleTextAttributes REPEATED_FILE_ATTRIBUTES = new SimpleTextAttributes(STYLE_PLAIN, ColorUtil.withAlpha(JBColor.namedColor("Component.infoForeground", 0xCCCCCC, 0x5E5E5E), .5));

        private final ColoredTableCellRenderer myFileAndLineNumber = new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                if (value instanceof UsageInfo2UsageAdapter) {
                    UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter) value;
                    TextChunk[] text = usageAdapter.getPresentation().getText();
                    // line number / file info
                    VirtualFile file = usageAdapter.getFile();
                    String uniqueVirtualFilePath = SlowOperations.allowSlowOperations(() -> getFilePath(usageAdapter));
                    VirtualFile prevFile = findPrevFile(table, row, column);
                    SimpleTextAttributes attributes = Comparing.equal(file, prevFile) ? REPEATED_FILE_ATTRIBUTES : ORDINAL_ATTRIBUTES;
                    append(uniqueVirtualFilePath, attributes);
                    if (text.length > 0) append(" " + text[0].getText(), ORDINAL_ATTRIBUTES);
                }
                setBorder(null);
            }

            @NotNull
            private @NlsSafe String getFilePath(@NotNull UsageInfo2UsageAdapter ua) {
                VirtualFile file = ua.getFile();
                if (ScratchUtil.isScratch(file)) {
                    return StringUtil.notNullize(getPresentablePath(ua.getUsageInfo().getProject(), ua.getFile(), 60));
                }
                return UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(ua.getUsageInfo().getProject(), file, myScope);
            }

            @Nullable
            private VirtualFile findPrevFile(@NotNull JTable table, int row, int column) {
                if (row <= 0) return null;
                Object prev = table.getValueAt(row - 1, column);
                return prev instanceof UsageInfo2UsageAdapter ? ((UsageInfo2UsageAdapter) prev).getFile() : null;
            }
        };

        private static final int MARGIN = 2;
        private final GlobalSearchScope myScope;

        UsageTableCellRenderer(@NotNull GlobalSearchScope scope) {
            myScope = scope;
            setLayout(new BorderLayout());
            add(myUsageRenderer, BorderLayout.CENTER);
            add(myFileAndLineNumber, BorderLayout.EAST);
            setBorder(JBUI.Borders.empty(MARGIN, MARGIN, MARGIN, 0));
        }

        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJPanel() {
                    @Override
                    public AccessibleRole getAccessibleRole() {
                        return AccessibleRole.UNKNOWN;
                    }

                    @Override
                    public AccessibleStateSet getAccessibleStateSet() {
                        AccessibleStateSet stateSet = new AccessibleStateSet();
                        stateSet.addAll(myUsageRenderer.getAccessibleContext().getAccessibleStateSet().toArray());
                        stateSet.addAll(myFileAndLineNumber.getAccessibleContext().getAccessibleStateSet().toArray());
                        return stateSet;
                    }

                    @Override
                    public int getAccessibleIndexInParent() {
                        return 0;
                    }

                    @Override
                    public int getAccessibleChildrenCount() {
                        return 0;
                    }

                    @Override
                    public Accessible getAccessibleChild(int i) {
                        return null;
                    }
                };
            }
            return accessibleContext;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            myUsageRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            myFileAndLineNumber.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(myUsageRenderer.getBackground());
            if (!isSelected && value instanceof UsageInfo2UsageAdapter) {
                UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter) value;
                Project project = usageAdapter.getUsageInfo().getProject();
                Color color = VfsPresentationUtil.getFileBackgroundColor(project, usageAdapter.getFile());
                setBackground(color);
                myUsageRenderer.setBackground(color);
                myFileAndLineNumber.setBackground(color);
            }
            getAccessibleContext().setAccessibleName(FindBundle.message("find.popup.found.element.accesible.name", myUsageRenderer.getAccessibleContext().getAccessibleName(), myFileAndLineNumber.getAccessibleContext().getAccessibleName()));
            return this;
        }
    }

    private final class MyPinAction extends ToggleAction {
        private MyPinAction() {
            super(IdeBundle.messagePointer("action.ToggleAction.text.pin.window"),
                    IdeBundle.messagePointer("action.ToggleAction.description.pin.window"), AllIcons.General.Pin_tab);
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return UISettings.getInstance().getPinFindInPath();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            myIsPinned.set(state);
            UISettings.getInstance().setPinFindInPath(state);
            FindUsagesCollector.PIN_TOGGLED.log(state);
        }
    }

    private final class MyEnterAction extends DumbAwareAction {
        private final boolean myEnterAsOK;

        private MyEnterAction(boolean enterAsOK) {
            myEnterAsOK = enterAsOK;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (myEnterAsOK) {
                myOkActionListener.actionPerformed(null);
            } else {
                navigateToSelectedUsage(null);
            }
        }
    }

    private static class JBTextAreaWithMixedAccessibleContext extends JBTextArea {

        private final AccessibleContext tableContext;

        private JBTextAreaWithMixedAccessibleContext(AccessibleContext context) {
            tableContext = context;
        }

        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new TextFieldWithListAccessibleContext(this, tableContext);
            }
            return accessibleContext;
        }
    }
}
