package myfindaction.myfindaction;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.SearchTextField;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ui.SearchTextField.KEY;

public class MyFindAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        SearchTextField search = event.getData(KEY);
        if (search != null) {
            var updatedText = search
                    .getText().replaceAll(" ", ".*");
            search.setText(updatedText);
            search.selectText();
            search.requestFocus();
        }
    }
}
