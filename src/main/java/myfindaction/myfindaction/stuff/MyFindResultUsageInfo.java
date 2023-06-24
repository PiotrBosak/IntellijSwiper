package myfindaction.myfindaction.stuff;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiFileRange;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

class MyFindResultUsageInfo extends UsageInfo {
    private final FindManager myFindManager;
    private final FindModel myFindModel;
    private final SmartPsiFileRange myAnchor;

    private Boolean myCachedResult;
    private long myTimestamp;

    private static final Key<Long> DOCUMENT_TIMESTAMP_KEY = Key.create("MyFindResultUsageInfo.DOCUMENT_TIMESTAMP_KEY");

    MyFindResultUsageInfo(@NotNull FindManager finder,
                        @NotNull PsiFile file,
                        int offset,
                        @NotNull FindModel findModel,
                        @NotNull FindResult result) {
        super(file, result.getStartOffset(), result.getEndOffset());
        myFindManager = finder;
        myFindModel = findModel;

        assert result.isStringFound();

        if (myFindModel.isRegularExpressions() ||
                myFindModel.isInCommentsOnly() ||
                myFindModel.isInStringLiteralsOnly() ||
                myFindModel.isExceptStringLiterals() ||
                myFindModel.isExceptCommentsAndStringLiterals() ||
                myFindModel.isExceptComments()) {
            myAnchor = SmartPointerManager.getInstance(getProject()).createSmartPsiFileRangePointer(file, TextRange.from(offset, 0));
        }
        else {
            myAnchor = null;
        }
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        PsiFile psiFile = getPsiFile();
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
        if (document == null) {
            myCachedResult = null;
            return false;
        }

        Boolean cachedResult = myCachedResult;
        if (document.getModificationStamp() == myTimestamp && cachedResult != null) {
            return cachedResult;
        }
        myTimestamp = document.getModificationStamp();

        Segment segment = getSegment();
        boolean isFileOrBinary = isFileOrBinary();
        if (segment == null && !isFileOrBinary) {
            myCachedResult = false;
            return false;
        }

        VirtualFile file = psiFile.getVirtualFile();
        if (isFileOrBinary) {
            myCachedResult = file.isValid();
            return myCachedResult;
        }

        Segment searchOffset;
        if (myAnchor != null) {
            searchOffset = myAnchor.getRange();
            if (searchOffset == null) {
                myCachedResult = false;
                return false;
            }
        }
        else {
            searchOffset = segment;
        }

        int offset = searchOffset.getStartOffset();
        Long data = myFindModel.getUserData(DOCUMENT_TIMESTAMP_KEY);
        if (data == null || data != myTimestamp) {
            data = myTimestamp;
            MyFindManagerImpl.clearPreviousFindData(myFindModel);
        }
        myFindModel.putUserData(DOCUMENT_TIMESTAMP_KEY, data);
        FindResult result;
        do {
            result = myFindManager.findString(document.getCharsSequence(), offset, myFindModel, file);
            offset = result.getEndOffset() == offset ? offset + 1 : result.getEndOffset();
            if (!result.isStringFound()) {
                myCachedResult = false;
                return false;
            }
        } while (result.getStartOffset() < segment.getStartOffset());

        boolean ret = segment.getStartOffset() == result.getStartOffset() && segment.getEndOffset() == result.getEndOffset();
        myCachedResult = ret;
        return ret;
    }

    private PsiFile getPsiFile() {
        return (PsiFile)getElement();
    }

    @Override
    public String toString() {
        return "MyFindResultUsageInfo: myFindModel=" + myFindModel + " in " + getSmartPointer() +"; segment="+getSegment();
    }
}
