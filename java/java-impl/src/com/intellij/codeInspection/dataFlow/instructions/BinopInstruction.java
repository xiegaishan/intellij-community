/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Feb 7, 2002
 * Time: 1:11:08 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow.instructions;

import com.intellij.codeInspection.dataFlow.DataFlowRunner;
import com.intellij.codeInspection.dataFlow.DfaInstructionState;
import com.intellij.codeInspection.dataFlow.DfaMemoryState;
import com.intellij.codeInspection.dataFlow.InstructionVisitor;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class BinopInstruction extends BranchingInstruction {
  private final IElementType myOperationSign;
  private final Project myProject;

  public BinopInstruction(IElementType opSign, PsiElement psiAnchor, @NotNull Project project) {
    myProject = project;
    if (JavaTokenType.EQEQ == opSign || JavaTokenType.NE == opSign || JavaTokenType.INSTANCEOF_KEYWORD == opSign || JavaTokenType.PLUS == opSign) {
      myOperationSign = opSign;
    }
    else {
      myOperationSign = null;
    }

    setPsiAnchor(psiAnchor);
  }

  @Override
  public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
    return visitor.visitBinop(this, runner, stateBefore);
  }

  public DfaValue getNonNullStringValue(final DfaValueFactory factory) {
    PsiElement anchor = getPsiAnchor();
    Project project = myProject;
    PsiClassType string = PsiType.getJavaLangString(PsiManager.getInstance(project), anchor == null ? GlobalSearchScope.allScope(project) : anchor.getResolveScope());
    return factory.getNotNullFactory().create(string);
  }

  public String toString() {
    return "BINOP " + myOperationSign;
  }

  public IElementType getOperationSign() {
    return myOperationSign;
  }
}
