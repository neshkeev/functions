import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodReferenceExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiParenthesizedExpressionImpl;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

        // FIXME: this element pattern handles false positive cases like "System." or when there is no receiver
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
            final PsiElement element = parameters.getOriginalFile().findElementAt(parameters.getOffset() - 5);
            if (element == null) return;

            // FIXME: this check should be gone when the element pattern is fixed
            final PsiElement parent = element.getParent();
            if (!(parent instanceof PsiMethodReferenceExpressionImpl)) return;

            final PsiMethodReferenceExpressionImpl methodReference = (PsiMethodReferenceExpressionImpl) parent;
            final Stream<PsiMethod> methods;

            if (methodReference.isConstructor()) {
                methods = methodsFromConstructors(methodReference);
            } else {
                final PsiMethodImpl resolve = (PsiMethodImpl) methodReference.resolve();
                methods = Stream.of(resolve);
            }

            final Predicate<PsiMethod> singleArgument = m -> m.getParameterList().getParametersCount() == 1;
            final Predicate<PsiMethod> voidReturn = m -> "void".equals(m.getReturnType().getCanonicalText());

            final Predicate<PsiMethod> isConstructor = m -> methodReference.isConstructor();

            final Predicate<PsiMethod> singleArgumentConstructor = isConstructor.and(singleArgument);
            final Predicate<PsiMethod> singleArgumentMethod = singleArgument.and(voidReturn.negate());

            final Consumer<PsiMethod> addCompose = m -> {
                final PsiTypeElement[] typeParams = methodReference.getParameterList().getTypeParameterElements();
                final String typeFrom;

                if (typeParams.length != 0) {
                    typeFrom = typeParams[0].getText();
                } else {
                    final PsiType type = m.getParameterList().getParameters()[0].getType();
                    typeFrom = fromPrimitive(type);
                }
                final String typeTo;
                if (m.isConstructor()) {
                    typeTo = m.getContainingClass().getName();
                } else {
                    final PsiType returnType = m.getReturnType();

                    typeTo = fromPrimitive(returnType);
                }
                final InsertHandler<LookupElement> handler =
                        (ctx, item) -> {
                            final PsiElementFactory factory = JavaPsiFacade.getInstance(ctx.getProject()).getElementFactory();

                            final String text = "((Function<" + typeFrom + ", " + typeTo + ">)" +
                                    methodReference.getText() +
                                    ")";

                            final PsiParenthesizedExpressionImpl castExpr =
                                    (PsiParenthesizedExpressionImpl)
                                            factory.createExpressionFromText(
                                                    text,
                                                    null
                                            );
                            methodReference.replace(castExpr);
                        };

                result.addElement(LookupElementBuilder.create("compose").withTailText("()").withInsertHandler(handler).bold());
            };

            methods.filter(singleArgumentConstructor.or(singleArgumentMethod))
                    .forEach(addCompose);
        }

        @NotNull
        private String fromPrimitive(@NotNull final PsiType type) {
            final String typeTo;
            if (type instanceof PsiPrimitiveType) {
                typeTo = ((PsiPrimitiveType) type).getBoxedTypeName();
            } else {
                typeTo = type.getCanonicalText(true);
            }
            return typeTo;
        }

        @Contract(pure = true)
        @NotNull
        private Stream<PsiMethod> methodsFromConstructors(@NotNull final PsiMethodReferenceExpressionImpl methodReference) {
            final Stream<PsiMethod> methods;
            final PsiElement constructor = methodReference.resolve();
            if (constructor == null) {
                // multiple constructors
                final JavaResolveResult[] javaResolveResults = methodReference.multiResolve(true);
                methods = Arrays.stream(javaResolveResults)
                        .map(JavaResolveResult.class::cast)
                        .map(JavaResolveResult::getElement)
                        .map(PsiMethod.class::cast)
                ;
            } else if (constructor instanceof PsiMethod) {
                // single constructor defined
                methods =  Stream.of((PsiMethod) constructor);
            } else if (constructor instanceof PsiClassImpl) {
                // no constructor defined
                final PsiClassImpl resolve = (PsiClassImpl) methodReference.resolve();
                methods = resolve == null ? Stream.empty() : Arrays.stream(resolve.getConstructors());
            } else {
                throw new IllegalArgumentException("I don't know how to handle constructor " + constructor);
            }
            return methods;
        }
    }
}
