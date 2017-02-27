/*
 * Copyright 2013-2017 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goide.inspections;

import com.goide.psi.*;
import com.goide.psi.impl.GoElementFactory;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

public class GoMaxProcsInspection extends GoInspectionBase {
  private static final String GO_MAX_PROCS_FUNCTION = "runtime.GOMAXPROCS";
  private static final Integer MAX_ARGUMENT_VALUE = 256;

  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {

      @Override
      public void visitCallExpr(@NotNull GoCallExpr o) {
        super.visitCallExpr(o);

        GoExpression expr = o.getExpression();
        if (expr == null || !expr.textMatches(GO_MAX_PROCS_FUNCTION) || o.getArgumentList() == null) return;
        List<GoExpression> argsExpressions = o.getArgumentList().getExpressionList();
        if (argsExpressions.size() != 1) return;
        GoExpression argument = argsExpressions.get(0);
        if (argument == null) return;
        GoType argumentType = argument.getGoType(null);
        if (argumentType != null && !argumentType.getText().equals("int")) return;
        GoExpression value = null;
        if (argument instanceof GoReferenceExpression) {
          PsiElement resolve = ((GoReferenceExpression)argument).resolve();
          if (resolve == null) return;
          if (resolve instanceof GoVarDefinition) {
            value = ((GoVarDefinition)resolve).getValue();
          }
          else {
            Logger.getLogger("type").info(resolve.toString());
          }
        }
        else {
          value = argument;
        }
        if (value == null) return;
        int intValue;
        try {
          intValue = Integer.parseInt(value.getText());
        } catch (NumberFormatException e) {
          return;
        } catch (NullPointerException e) {
          return;
        }
        if (intValue > MAX_ARGUMENT_VALUE) {
          holder.registerProblem(argsExpressions.get(0), "Max is 256", ProblemHighlightType.WEAK_WARNING,
                                 new LocalQuickFixBase("Replace by " + MAX_ARGUMENT_VALUE) {
            @Override
            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
              PsiElement element = descriptor.getPsiElement();
              if (element == null || !element.isValid()) return;
              if (element instanceof GoExpression) {
                element.replace(GoElementFactory.createExpression(project, MAX_ARGUMENT_VALUE.toString()));
              }
            }
          });
        }
      }
    };
  }
}
