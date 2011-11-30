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

package com.intellij.codeInsight.folding.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class CollapseAllRegionsHandler implements CodeInsightActionHandler {
  public void invoke(@NotNull Project project, @NotNull final Editor editor, @NotNull PsiFile file){
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
    final FoldRegion[] allFoldRegions = editor.getFoldingModel().getAllFoldRegions();
    Runnable processor = new Runnable() {
      public void run() {
        for (int i = 0; i < allFoldRegions.length; i++) {
          FoldRegion region = allFoldRegions[i];
          region.setExpanded(false);
        }
      }
    };
    editor.getFoldingModel().runBatchFoldingOperation(processor);
  }

  public boolean startInWriteAction() {
    return true;
  }
}