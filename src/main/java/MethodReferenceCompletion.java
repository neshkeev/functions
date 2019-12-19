import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class MethodReferenceCompletion extends CompletionContributor  {
    public MethodReferenceCompletion() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement(),
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
            result.addElement(LookupElementBuilder.create("NEW_COMPLETION").bold());
        }
    }
}
