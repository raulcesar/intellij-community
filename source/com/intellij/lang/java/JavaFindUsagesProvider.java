package com.intellij.lang.java;

import com.intellij.find.impl.HelpID;
import com.intellij.lang.LangBundle;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.*;
import com.intellij.psi.impl.search.ThrowSearchUtil;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.xml.TypeNameManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 */
public class JavaFindUsagesProvider implements FindUsagesProvider {
  public static final String DEFAULT_PACKAGE_NAME = UsageViewBundle.message("default.package.presentable.name");

  public boolean canFindUsagesFor(@NotNull PsiElement element) {
    if (element instanceof PsiDirectory) {
      PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(((PsiDirectory)element));
      return psiPackage != null && psiPackage.getQualifiedName().length() != 0;
    }

    return element instanceof PsiClass ||
           element instanceof PsiVariable ||
           element instanceof PsiMethod ||
           element instanceof PsiPackage ||
           element instanceof PsiLabeledStatement ||
           ThrowSearchUtil.isSearchable(element) ||
           (element instanceof PsiMetaOwner && ((PsiMetaOwner)element).getMetaData() != null);
  }

  public String getHelpId(@NotNull PsiElement element) {
    if (element instanceof PsiPackage) {
      return HelpID.FIND_PACKAGE_USAGES;
    }
    else if (element instanceof PsiClass) {
      return HelpID.FIND_CLASS_USAGES;
    }
    else if (element instanceof PsiMethod) {
      return HelpID.FIND_METHOD_USAGES;
    }
    else if (ThrowSearchUtil.isSearchable(element)) {
      return HelpID.FIND_THROW_USAGES;
    }
    else return HelpID.FIND_OTHER_USAGES;
  }

  @NotNull
  public String getType(@NotNull PsiElement element) {
    if (element instanceof PsiDirectory) {
      return LangBundle.message("terms.directory");
    }
    if (element instanceof PsiFile) {
      return LangBundle.message("terms.file");
    }
    if (ThrowSearchUtil.isSearchable(element)) {
      return LangBundle.message("java.terms.exception");
    }
    if (element instanceof PsiPackage) {
      return LangBundle.message("java.terms.package");
    }
    if (element instanceof PsiLabeledStatement) {
      return LangBundle.message("java.terms.label");
    }
    if (element instanceof PsiClass) {
      if (((PsiClass)element).isAnnotationType()) {
        return LangBundle.message("java.terms.annotation.interface");
      } else if (((PsiClass)element).isEnum()) {
        return LangBundle.message("java.terms.enum");
      } else if (((PsiClass)element).isInterface()) {
        return LangBundle.message("java.terms.interface");
      } else if (element instanceof PsiTypeParameter) {
        return LangBundle.message("java.terms.type.parameter");
      }
      return LangBundle.message("java.terms.class");
    }
    if (element instanceof PsiField) {
      return LangBundle.message("java.terms.field");
    }
    if (element instanceof PsiParameter) {
      return LangBundle.message("java.terms.parameter");
    }
    if (element instanceof PsiLocalVariable) {
      return LangBundle.message("java.terms.variable");
    }
    if (element instanceof PsiMethod) {
      final PsiMethod psiMethod = (PsiMethod)element;
      final boolean isConstructor = psiMethod.isConstructor();
      if (isConstructor) {
        return LangBundle.message("java.terms.constructor");
      }
      else {
        return LangBundle.message("java.terms.method");
      }
    }

    final String name = TypeNameManager.getTypeName(element.getClass());
    if (name != null) {
      return name;
    }
    return "";
  }

  @NotNull
  public String getDescriptiveName(@NotNull final PsiElement element) {
    if (ThrowSearchUtil.isSearchable(element)) {
      return ThrowSearchUtil.getSearchableTypeName(element);
    }
    if (element instanceof PsiDirectory) {
      return getPackageName((PsiDirectory)element, false);
    }
    else if (element instanceof PsiPackage) {
      return getPackageName((PsiPackage)element);
    }
    else if (element instanceof PsiFile) {
      return ((PsiFile)element).getVirtualFile().getPresentableUrl();
    }
    else if (element instanceof PsiLabeledStatement) {
      return ((PsiLabeledStatement)element).getLabelIdentifier().getText();
    }
    else if (element instanceof PsiClass) {
      if (element instanceof PsiAnonymousClass) {
        return LangBundle.message("java.terms.anonymous.class");
      }
      else {
        final PsiClass aClass = (PsiClass)element;
        String qName =  aClass.getQualifiedName();
        return qName == null ? aClass.getName() : qName;
      }
    }
    else if (element instanceof PsiMethod) {
      PsiMethod psiMethod = (PsiMethod)element;
      String formatted = PsiFormatUtil.formatMethod(psiMethod,
                                                    PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS,
                                                    PsiFormatUtil.SHOW_TYPE);
      PsiClass psiClass = psiMethod.getContainingClass();
      if (psiClass != null) {
        return getContainingClassDescription(psiClass, formatted);
      }

      return formatted;
    }
    else if (element instanceof PsiField) {
      PsiField psiField = (PsiField)element;
      String formatted = PsiFormatUtil.formatVariable(psiField, PsiFormatUtil.SHOW_NAME, PsiSubstitutor.EMPTY);
      PsiClass psiClass = psiField.getContainingClass();
      if (psiClass != null) {
        return getContainingClassDescription(psiClass, formatted);
      }

      return formatted;
    }
    else if (element instanceof PsiVariable) {
      return PsiFormatUtil.formatVariable((PsiVariable)element, PsiFormatUtil.SHOW_NAME, PsiSubstitutor.EMPTY);
    }

    return "";
  }

  private static String getContainingClassDescription(PsiClass aClass, String formatted) {
    if (aClass instanceof PsiAnonymousClass) {
      return LangBundle.message("java.terms.of.anonymous.class", formatted);
    }
    else {
      String className = aClass.getName();
      if (aClass.isInterface()) {
        return LangBundle.message("java.terms.of.interface", formatted, className);
      }
      else if (aClass.isEnum()) {
        return LangBundle.message("java.terms.of.enum", formatted, className);
      }
      else if (aClass.isAnnotationType()) {
        return LangBundle.message("java.terms.of.annotation.type", formatted, className);
      }
      else {
        return LangBundle.message("java.terms.of.class", formatted, className);
      }
    }
  }

  @NotNull
  public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    if (element instanceof PsiDirectory) {
      return getPackageName((PsiDirectory)element, false);
    }
    if (element instanceof PsiPackage) {
      return getPackageName((PsiPackage)element);
    }
    if (element instanceof PsiFile) {
      return useFullName ? ((PsiFile)element).getVirtualFile().getPresentableUrl() : ((PsiFile)element).getName();
    }
    if (element instanceof PsiLabeledStatement) {
      return ((PsiLabeledStatement)element).getLabelIdentifier().getText();
    }
    if (ThrowSearchUtil.isSearchable(element)) {
      return ThrowSearchUtil.getSearchableTypeName(element);
    }

    if (element instanceof PsiClass) {
      String name = ((PsiClass)element).getQualifiedName();
      if (name == null || !useFullName) {
        name = ((PsiClass)element).getName();
      }
      if (name != null) return name;
    }
    if (element instanceof PsiMethod) {
      PsiMethod psiMethod = (PsiMethod)element;
      if (useFullName) {
        String s = PsiFormatUtil.formatMethod((PsiMethod)element,
                                              PsiSubstitutor.EMPTY, PsiFormatUtil.TYPE_AFTER | PsiFormatUtil.SHOW_TYPE |
                                                                    PsiFormatUtil.SHOW_NAME |
                                                                    PsiFormatUtil.SHOW_PARAMETERS,
                                              PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.SHOW_NAME);
        final PsiClass psiClass = psiMethod.getContainingClass();
        if (psiClass != null) {
          final String qName = psiClass.getQualifiedName();
          if (qName != null) {
            if (psiClass.isInterface()) {
              s = LangBundle.message("java.terms.of.interface", s, qName);
            }
            else {
              s = LangBundle.message("java.terms.of.class", s, qName);
            }
          }
        }
        return s;
      }
      else {
        return PsiFormatUtil.formatMethod(psiMethod,
                                          PsiSubstitutor.EMPTY,
                                          PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS,
                                          PsiFormatUtil.SHOW_TYPE);
      }
    }
    else if (element instanceof PsiParameter && ((PsiParameter)element).getDeclarationScope() instanceof PsiMethod) {
      PsiMethod method = (PsiMethod)((PsiParameter)element).getDeclarationScope();
      String s = LangBundle.message("java.terms.variable.of.method",
                                    PsiFormatUtil.formatVariable((PsiVariable)element,
                                                                 PsiFormatUtil.TYPE_AFTER | PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.SHOW_NAME,
                                                                 PsiSubstitutor.EMPTY),
                                    PsiFormatUtil.formatMethod(method,
                                                               PsiSubstitutor.EMPTY,
                                                               PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS,
                                                               PsiFormatUtil.SHOW_TYPE));

      final PsiClass psiClass = method.getContainingClass();
      if (psiClass != null && psiClass.getQualifiedName() != null) {
        if (psiClass.isInterface()) {
          s = LangBundle.message("java.terms.of.interface", s, psiClass.getQualifiedName());
        }
        else {
          s = LangBundle.message("java.terms.of.class", s, psiClass.getQualifiedName());
        }
      }
      return s;
    }
    else if (element instanceof PsiField) {
      PsiField psiField = (PsiField)element;
      String s = PsiFormatUtil.formatVariable(psiField,
                                              PsiFormatUtil.TYPE_AFTER | PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.SHOW_NAME,
                                              PsiSubstitutor.EMPTY);
      PsiClass psiClass = psiField.getContainingClass();
      if (psiClass != null) {
        String qName = psiClass.getQualifiedName();
        if (qName != null) {
          if (psiClass.isInterface()) {
            s = LangBundle.message("java.terms.of.interface", s, qName);
          }
          else {
            s = LangBundle.message("java.terms.of.class", s, qName);
          }
        }
      }
      return s;
    }
    else if (element instanceof PsiVariable) {
      return PsiFormatUtil.formatVariable((PsiVariable)element,
                                          PsiFormatUtil.TYPE_AFTER | PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.SHOW_NAME,
                                          PsiSubstitutor.EMPTY);
    }

    return "";
  }

  public static String getPackageName(PsiDirectory directory, boolean includeRootDir) {
    PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
    if (aPackage == null) {
      return directory.getVirtualFile().getPresentableUrl();
    }
    else {
      String packageName = getPackageName(aPackage);
      if (includeRootDir) {
        String rootDir = getRootDirectoryForPackage(directory);
        if (rootDir != null) {
          return UsageViewBundle.message("usage.target.package.in.directory", packageName, rootDir);
        }
      }
      return packageName;
    }
  }

  public static String getRootDirectoryForPackage(PsiDirectory directory) {
    PsiManager manager = directory.getManager();
    final VirtualFile virtualFile = directory.getVirtualFile();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(manager.getProject()).getFileIndex();
    VirtualFile root = fileIndex.getSourceRootForFile(virtualFile);

    if (root == null) {
      root = fileIndex.getClassRootForFile(virtualFile);
    }
    if (root != null) {
      return root.getPresentableUrl();
    }
    else {
      return null;
    }
  }

  public static String getPackageName(PsiPackage psiPackage) {
    if (psiPackage == null) {
      return null;
    }
    String name = psiPackage.getQualifiedName();
    if (name.length() > 0) {
      return name;
    }
    else {
      return DEFAULT_PACKAGE_NAME;
    }
  }

  private static class Inner {
    private static final TokenSet COMMENT_BIT_SET = TokenSet.create(JavaDocTokenType.DOC_COMMENT_DATA, JavaDocTokenType.DOC_TAG_VALUE_TOKEN,
                                                                    JavaTokenType.C_STYLE_COMMENT, JavaTokenType.END_OF_LINE_COMMENT);
  }

  public WordsScanner getWordsScanner() {
    return null;
  }

  public static boolean mayHaveReferencesImpl(final IElementType token, final short searchContext) {
    if ((searchContext & UsageSearchContext.IN_STRINGS) != 0 && token == JavaElementType.LITERAL_EXPRESSION) return true;
    if ((searchContext & UsageSearchContext.IN_COMMENTS) != 0 && Inner.COMMENT_BIT_SET.contains(token)) return true;
    if ((searchContext & UsageSearchContext.IN_CODE) != 0 && (token == JavaTokenType.IDENTIFIER || token == JavaDocTokenType
      .DOC_TAG_VALUE_TOKEN)) {
      return true;
    }
    // Java string literal to properties file
    return (searchContext & UsageSearchContext.IN_FOREIGN_LANGUAGES) != 0 && token == JavaElementType.LITERAL_EXPRESSION;
  }
}
