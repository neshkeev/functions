import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TypeAliasMultiHostInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (context instanceof PsiDocComment) return;
        final PsiComment psiComment = (PsiComment) context;

        registrar.startInjecting(JavaLanguage.INSTANCE);
        registrar.addPlace(
                "class A { void method() { ",
                "; }}",
                (PsiLanguageInjectionHost) context,
                new TextRange(3, psiComment.getTextLength())
        );
        registrar.doneInjecting();
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(PsiComment.class);
    }
}

// com.intellij.psi.JavaTokenType.C_STYLE_COMMENT
// com.intellij.psi.JavaTokenType.END_OF_LINE_COMMENT
