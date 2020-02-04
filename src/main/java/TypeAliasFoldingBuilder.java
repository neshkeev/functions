import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.impl.source.tree.JavaSourceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypeAliasFoldingBuilder extends FoldingBuilderEx {

    private static final Map<String, String> types = new HashMap<>();

    static {
        types.put("IntLst", "ArrayList<Integer>");
        types.put("IntStrMap", "HashMap<Integer,String>");
    }

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        final FoldingGroup group = FoldingGroup.newGroup("typeAlias");
        final List<FoldingDescriptor> descriptors = new ArrayList<>();
        final Collection<PsiTypeElement> candidates = PsiTreeUtil.findChildrenOfType(root, PsiTypeElement.class);
        for (final PsiTypeElement candidate : candidates) {
            final PsiType type = candidate.getType();

            final String presentableText = type.getPresentableText();
            if(types.containsValue(presentableText))
                descriptors.add(new MyFoldingDescriptor(candidate, group));
        }
        final Collection<PsiJavaCodeReferenceElement> candidates1 = PsiTreeUtil.findChildrenOfType(root, PsiJavaCodeReferenceElement.class);
        for (PsiJavaCodeReferenceElement element : candidates1) {
            final String referenceText = JavaSourceUtil.getReferenceText(element);
            if(types.containsValue(referenceText))
                descriptors.add(new MyFoldingDescriptor1(element, group));
        }
        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return node.getText();
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }

    private static class MyFoldingDescriptor extends FoldingDescriptor {
        public MyFoldingDescriptor(
                PsiTypeElement candidate,
                FoldingGroup group
        ) {
            super(
                    candidate.getNode(),
                    new TextRange(candidate.getTextRange().getStartOffset(), candidate.getTextRange().getEndOffset()),
                    group
            );

            final String type = candidate.getType().getPresentableText();
            types.entrySet()
                    .stream()
                    .filter(e -> type.equals(e.getValue()))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .ifPresent(this::setPlaceholderText);
        }
    }
    private static class MyFoldingDescriptor1 extends FoldingDescriptor {
        public MyFoldingDescriptor1(
                PsiJavaCodeReferenceElement candidate,
                FoldingGroup group
        ) {
            super(
                    candidate.getNode(),
                    new TextRange(candidate.getTextRange().getStartOffset(), candidate.getTextRange().getEndOffset()),
                    group
            );

            final String type = JavaSourceUtil.getReferenceText(candidate);
            types.entrySet()
                    .stream()
                    .filter(e -> type.equals(e.getValue()))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .ifPresent(this::setPlaceholderText);
        }
    }
}
