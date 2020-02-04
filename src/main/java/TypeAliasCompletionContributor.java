import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaDocumentedElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiInlineDocTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class TypeAliasCompletionContributor extends CompletionContributor {

    public TypeAliasCompletionContributor() {
        extend(CompletionType.BASIC, psiElement(), new TypeAliasCompletionProvider());
    }

    private static class TypeAliasCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition();
            while(position != null) {
                if (position instanceof PsiJavaDocumentedElement) {
                    final PsiJavaDocumentedElement psiMethod = (PsiJavaDocumentedElement) position;
                    final PsiDocComment docComment = psiMethod.getDocComment();
                    if (docComment == null) continue;
                    PsiElement child = docComment.getFirstChild();
                    while (child != docComment.getLastChild()) {
                        if (child instanceof PsiInlineDocTag) {
                            final PsiDocTag psiDocTag = (PsiDocTag) child;
                            if (psiDocTag.getName().equals("code")) {
                                final String aliasText = psiDocTag.getDataElements()[0].getText().substring(6);
                                final String[] tokens = aliasText.split("=");
                                final String aliasName = tokens[0].trim();
                                final String value = tokens[1].trim();
//                                System.out.println("types.put(\"" + aliasName + "\", \"" + value + "\");");
                            }
                        }
                        child = child.getNextSibling();
                    }
                }
                position = position.getParent();
            }
        }


    }
}
