package de.dreamlab.dash;


import com.intellij.lang.Language;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class DashLauncherAction extends AnAction {
    public static final Boolean VERBOSE_DEBUG = true;

    private static final String XML_LANGUAGE_ID = "XML";

    private KeywordLookup keywordLookup;
    private DashLauncher dashLauncher;

    public DashLauncherAction()
    {
        if ( DashLauncherAction.VERBOSE_DEBUG ) {
            Notifications.Bus.notify(new Notification("Dash", "Dash: Verbose Debug", "Plugin initialized", NotificationType.INFORMATION));
        }

        keywordLookup = new KeywordLookup();
        dashLauncher = new DashLauncher();
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(PlatformDataKeys.EDITOR.getData(e.getDataContext()) != null);
    }

    public void actionPerformed(AnActionEvent e) {
        Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());

        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        PsiElement psiElement = null;
        Language language = null;

        if ( psiFile != null ) {
            psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
            language = elementLanguage(psiElement);
        }

        String query = null;

        SelectionModel selectionModel = editor.getSelectionModel();
        if ( selectionModel.hasSelection() ) {
            query = selectionModel.getSelectedText();
        }
        else {
            if ( psiElement == null || psiElement instanceof PsiComment ) {
                query = getWordAtCursor(editor);
            }
            else {
                query = psiElement.getText();
            }
        }

        if ( query != null ) {
            // show status message for potential troubleshooting
            String resolvedLanguage = keywordLookup.findLanguageName(language);

            String message;
            if ( resolvedLanguage == null ) {
                message = "Searching all docsets in Dash";
            }
            else {
                message = "Searching \"" + resolvedLanguage + "\" docsets in Dash";
            }

            if ( !language.getID().equals(resolvedLanguage) ) {
                message += ". Based on \"" + language.getID() + "\" context";
            }

            StatusBarUtil.setStatusBarInfo(e.getProject(), message);

            // open dash
            dashLauncher.search(keywordLookup.findKeywords(language), query);
        }
    }

    private Language elementLanguage(PsiElement element)
    {
        if ( element == null ) {
            return null;
        }

        if ( XML_LANGUAGE_ID.equals(element.getLanguage().getID()) ) {
            PsiElement parent = element.getParent();

            if ( !XML_LANGUAGE_ID.equals(parent.getLanguage().getID()) && XML_LANGUAGE_ID.equals(parent.getLanguage().getBaseLanguage().getID()) ) {
                return parent.getLanguage();
            }
        }

        return element.getLanguage();
    }

    private String getWordAtCursor(Editor editor) {
        CharSequence editorText = editor.getDocument().getCharsSequence();
        int cursorOffset = editor.getCaretModel().getOffset();
        int editorTextLength = editorText.length();

        if ( editorTextLength == 0 ) {
            return null;
        }

        if ( (cursorOffset >= editorTextLength) || (cursorOffset > 1 && !isIdentifierPart(editorText.charAt(cursorOffset) ) && isIdentifierPart(editorText.charAt(cursorOffset - 1))) ) {
            cursorOffset--;
        }

        if ( isIdentifierPart(editorText.charAt(cursorOffset)) ) {
            int start = cursorOffset;
            int end = cursorOffset;

            while ( start > 0 && isIdentifierPart(editorText.charAt(start-1)) ) {
                start--;
            }

            while ( end < editorTextLength && isIdentifierPart(editorText.charAt(end)) ) {
                end++;
            }

            return editorText.subSequence(start, end).toString();
        }
        return null;
    }

    private boolean isIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }
}
