import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class MethodReferenceCompletionContributor extends CompletionContributor {

    public MethodReferenceCompletionContributor() {

        final PsiElementPattern.Capture<PsiElement> methodLocation = psiElement()
                .afterLeafSkipping(
                        psiElement()
                                .withParent(PsiMethodReferenceExpression.class)
                                .withText("::")
                                .afterSibling(psiElement(PsiReferenceParameterList.class)),
                        psiElement()
                );
        final PsiElementPattern.Capture<PsiElement> doubleColumnLocationAfter = psiElement().afterLeaf("::");
        final PsiElementPattern.Capture<PsiElement> parameterListLocationAfter = psiElement().afterLeaf("<", ">");
        final PsiElementPattern.Capture<PsiElement> parameterListLocationBefore = psiElement().beforeLeaf("<", ">");
        final PsiElementPattern.Capture<PsiElement> classLocation = psiElement().beforeLeaf(psiElement().withText("::"));
        final PsiElementPattern.Capture<PsiElement> firstParenthesisLocation = psiElement().beforeLeafSkipping(psiElement().withText("::"), psiElement().beforeLeaf("("));

        final PsiElementPattern.Capture<PsiElement> elementPattern = methodLocation
                .andNot(firstParenthesisLocation)
                .andNot(classLocation)
                .andNot(doubleColumnLocationAfter)
                .andNot(parameterListLocationAfter)
                .andNot(parameterListLocationBefore)
                ;
        extend(
                CompletionType.BASIC,
                elementPattern
                ,new MethodReferenceCompletionProvider()
        );
    }

    private static final class MethodReferenceCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(
                @NotNull final CompletionParameters parameters,
                @NotNull final ProcessingContext context,
                @NotNull final CompletionResultSet result
        ) {
            result.addElement(LookupElementBuilder.create("NEW_COMPLETION").bold().withInsertHandler(
                    (ctx, item) -> {
                        final PsiElement forElement = ctx.getFile().findElementAt(ctx.getSelectionEndOffset() - item.getLookupString().length() - 2);
                        System.out.println(item.getLookupString() + " is selected for " + forElement.getNode());
                    }
            ));
        }
    }

}
