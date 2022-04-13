package myfindaction.myfindaction.stuff

import com.intellij.find.FindBundle
import com.intellij.find.FindUsagesCollector
import com.intellij.find.impl.FindPopupPanel
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.StateRestoringCheckBox
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.MathUtil
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.math.max

internal class MyFindPopupHeader(project: Project, filterContextButton: ActionButton, pinAction: ToggleAction) {

    @JvmField
    val panel: DialogPanel
    lateinit var titleLabel: JLabel
    lateinit var infoLabel: JLabel
    lateinit var loadingIcon: JLabel
    lateinit var cbFileFilter: StateRestoringCheckBox
    lateinit var fileMaskField: ComboBox<String>

    init {
        panel = panel {
            row {
                titleLabel = label(FindBundle.message("find.in.path.dialog.title"))
                        .bold()
                        .gap(RightGap.SMALL)
                        .component
                titleLabel.hide()
                infoLabel = label("")
                        .gap(RightGap.SMALL)
                        .component
                infoLabel.hide()
                UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, infoLabel)
                loadingIcon = icon(EmptyIcon.ICON_16)
                        .resizableColumn()
                        .component
                loadingIcon.hide()
                cbFileFilter = cell(createCheckBox(project))
                        .gap(RightGap.SMALL)
                        .component
                cbFileFilter.hide()
                fileMaskField = cell(createFileFilter()).component
                fileMaskField.hide()
                cell(filterContextButton)
                        .gap(RightGap.SMALL)
                filterContextButton.hide()
                if (!ExperimentalUI.isNewUI()) {
                    cell(createSeparator())
                            .gap(RightGap.SMALL)
                }
                actionButton(pinAction)
                titleLabel.hide()
                infoLabel.hide()
                loadingIcon.hide()
                cbFileFilter.hide()
                fileMaskField.hide()
            }
        }
    }

    private fun createCheckBox(project: Project): StateRestoringCheckBox {
        val checkBox = StateRestoringCheckBox(FindBundle.message("find.popup.filemask"))
        checkBox.addActionListener {
            FindUsagesCollector.CHECK_BOX_TOGGLED.log(project, FindUsagesCollector.FIND_IN_PATH, FindPopupPanel.ToggleOptionName.FileFilter, checkBox.isSelected)
        }
        return checkBox
    }

    private fun createFileFilter(): ComboBox<String> {
        val result = object : ComboBox<String>() {
            override fun getPreferredSize(): Dimension {
                var width = 0
                var buttonWidth = 0
                val components = components
                for (component in components) {
                    val size = component.preferredSize
                    val w = size?.width ?: 0
                    if (component is JButton) {
                        buttonWidth = w
                    }
                    width += w
                }
                val editor = getEditor()
                if (editor != null) {
                    val editorComponent = editor.editorComponent
                    if (editorComponent != null) {
                        val fontMetrics = editorComponent.getFontMetrics(editorComponent.font)
                        val item: Any? = selectedItem
                        width = max(width, fontMetrics.stringWidth(item.toString()) + buttonWidth)
                        // Let's reserve some extra space for just one 'the next' letter
                        width += fontMetrics.stringWidth("m")
                    }
                }
                val size = super.getPreferredSize()
                val insets = insets
                width += insets.left + insets.right
                size.width = MathUtil.clamp(width, JBUIScale.scale(80), JBUIScale.scale(500))
                return size
            }
        }

        result.isEditable = true
        result.maximumRowCount = 8

        return result
    }

    private fun createSeparator(): JComponent {
        val result = Box.createRigidArea(JBDimension(1, 24)) as JComponent
        result.isOpaque = true
        result.background = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
        return result;
    }
}