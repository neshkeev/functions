import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodReferenceExpressionImpl;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.codeInsight.FunctionalInterfaceSuggester.suggestFunctionalInterfaces;
import static com.intellij.patterns.PlatformPatterns.psiElement;

public class MethodReferenceCompletionContributor extends CompletionContributor {

    public MethodReferenceCompletionContributor() {

        final PsiElementPattern.Capture<PsiElement> elementPattern = getElementPattern();

        extend(
                CompletionType.SMART,
                elementPattern
                ,new MethodReferenceCompletionProvider()
        );
    }

    @NotNull
    private PsiElementPattern.Capture<PsiElement> getElementPattern1() {
        Function<Object, String> s = Objects::toString;
        return psiElement()
                .with(new PatternCondition<PsiElement>("methodReference") {
                    @Override
                    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
                        final PsiElement paramsList = psiElement.getPrevSibling();
                        final PsiElement doubleColumn = paramsList.getPrevSibling();
                        final PsiElement referenceExpression = doubleColumn.getPrevSibling();
                        final PsiElement methodReference = psiElement.getParent();

                        return psiElement instanceof PsiIdentifier
                                && paramsList instanceof PsiReferenceParameterList
                                && (doubleColumn instanceof PsiJavaToken && doubleColumn.getText().equals("::"))
                                && referenceExpression instanceof PsiReferenceExpression
                                && methodReference instanceof PsiMethodReferenceExpression
                                ;
                    }
                })
                ;
    }

    @NotNull
    private PsiElementPattern.Capture<PsiElement> getElementPattern() {
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
        return methodLocation
                .andNot(firstParenthesisLocation)
                .andNot(classLocation)
                .andNot(doubleColumnLocationAfter)
                .andNot(parameterListLocationAfter)
                .andNot(parameterListLocationBefore);
    }

    private static final class MethodReferenceCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(
                @NotNull final CompletionParameters parameters,
                @NotNull final ProcessingContext context,
                @NotNull final CompletionResultSet result
        ) {
            System.out.println("HELLO");
            if (true) return;
            final PsiElement element = parameters.getOriginalFile().findElementAt(parameters.getOffset() - 5);
            if (element == null) return;

            // FIXME: this check should be gone when the element pattern is fixed
            final PsiElement parent = element.getParent();
            if (!(parent instanceof PsiMethodReferenceExpressionImpl)) return;

            final PsiMethodReferenceExpressionImpl methodReference = (PsiMethodReferenceExpressionImpl) parent;

            final Collection<? extends PsiType> types;
            if (methodReference.isConstructor()) {
                types = suggestFunctionalInterfaces(methodReference);
            } else {
                final PsiMethodImpl resolve = (PsiMethodImpl) methodReference.resolve();
                assert resolve != null;
                types = suggestFunctionalInterfaces(resolve);
            }
            System.out.println(types);

            final Map<PsiClass, ? extends PsiType> classesToTypes = types.stream()
                    .collect(
                            Collectors.toMap(
                                    PsiUtil::resolveClassInType,
                                    Function.identity()
                            )
                    );

            classesToTypes.keySet()
                    .stream()
                    .flatMap(e -> Arrays.stream(e.getMethods()))
                    .forEach(m -> {
                        result.addElement(
                                LookupElementBuilder.create(m.getName())
                                        .withTailText(m.getParameterList().getText())
                                        .withTypeText(m.getContainingClass().getName())
                                        .withPsiElement(m)
                                        .bold()
                                        .withInsertHandler((@NotNull InsertionContext ctx, @NotNull LookupElement item) -> {

                                            final PsiMethod psiElement = (PsiMethod) item.getPsiElement();
                                            assert psiElement != null;

                                            final PsiType casting = classesToTypes.get(psiElement.getContainingClass());

                                            if (casting instanceof PsiClassType) {
                                                final PsiClassReferenceType aClass = (PsiClassReferenceType) ((PsiClassType) casting).getParameters()[0];
//                                                System.out.println(aClass);
//                                                System.out.println(aClass.resolveGenerics().getElement().getText());
//                                                ((PsiClassType) casting).getParameters()
                                            }

                                            final PsiElementFactory factory = JavaPsiFacade.getInstance(ctx.getProject()).getElementFactory();

                                            final PsiTypeCastExpression cast = (PsiTypeCastExpression) factory.createExpressionFromText(
                                                    "(" + casting.getPresentableText() + ") ", null
                                            );
                                            System.out.println(cast.getCastType());

                                            final PsiParenthesizedExpression parens = (PsiParenthesizedExpression) factory.createExpressionFromText(
                                                    "(" + cast.getText() + methodReference.getText() + ")", null
                                            );
//                                            methodReference.replace(parens);
                                        })
                        );
                    });
            ;

        }
    }

    private static final class MethodReferenceCompletionProvider1 extends CompletionProvider<CompletionParameters> {

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
                final Collection<? extends PsiType> psiTypes = suggestFunctionalInterfaces(m);
                psiTypes.stream().map(PsiType::getCanonicalText).forEach(System.err::println);

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
                            final PsiTypeCastExpression cast = (PsiTypeCastExpression) factory.createExpressionFromText("(Function<" + typeFrom + ", " + typeTo + ">) ", null);

                            final PsiParenthesizedExpression parens = (PsiParenthesizedExpression) factory.createExpressionFromText(
                                    "(" + cast.getText() + methodReference.getText() + ")", null
                            );

//                            final String text = "((Function<" + typeFrom + ", " + typeTo + ">)" +
//                                    methodReference.getText() +
//                                    ")";
//
//                            final PsiParenthesizedExpressionImpl castExpr =
//                                    (PsiParenthesizedExpressionImpl)
//                                            factory.createExpressionFromText(
//                                                    text,
//                                                    null
//                                            );
//                            methodReference.replace(castExpr);
                            methodReference.replace(parens);
                        };

                result.addElement(LookupElementBuilder.create("compose").withTailText("()").withInsertHandler(handler).bold());
            };

            methods.filter(singleArgumentConstructor.or(singleArgumentMethod))
                    .peek(System.out::println)
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
    }

    @Contract(pure = true)
    @NotNull
    private static Stream<PsiMethod> methodsFromConstructors(@NotNull final PsiMethodReferenceExpressionImpl methodReference) {
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
