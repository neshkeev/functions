import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.PsiExpressionPattern;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodReferenceExpressionImpl;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class MethodReferenceCompletion extends CompletionContributor  {

    public MethodReferenceCompletion() {

        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement(PsiIdentifier.class).withParent(PsiMethodReferenceExpression.class),
                new MethodReferenceCompletionProvider()
        );
    }

    private static final class MethodReferenceCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(
                @NotNull final CompletionParameters parameters,
                @NotNull final ProcessingContext context,
                @NotNull final CompletionResultSet result
        ) {
//            System.out.println(parameters.);
            result.addElement(LookupElementBuilder.create("NEW_COMPLETION").bold().withInsertHandler(
                    (ctx, item) -> {
                        System.out.println(item + " is selected");
                    }
            ));
        }
    }

    private static CompletionContributor create() {
        return null;
    }

}
