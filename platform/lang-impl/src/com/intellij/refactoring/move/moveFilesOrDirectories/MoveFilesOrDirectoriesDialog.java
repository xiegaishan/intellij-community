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

package com.intellij.refactoring.move.moveFilesOrDirectories;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MoveFilesOrDirectoriesDialog extends DialogWrapper{
  @NonNls private static final String RECENT_KEYS = "MoveFile.RECENT_KEYS";

  public interface Callback {
    void run(MoveFilesOrDirectoriesDialog dialog);
  }

  private JLabel myNameLabel;
  private TextFieldWithHistoryWithBrowseButton myTargetDirectoryField;
  private String myHelpID;
  private final Project myProject;
  private final Callback myCallback;
  private PsiDirectory myTargetDirectory;
  private JCheckBox myCbSearchForReferences;

  public MoveFilesOrDirectoriesDialog(Project project, Callback callback) {
    super(project, true);
    myProject = project;
    myCallback = callback;
    setTitle(RefactoringBundle.message("move.title"));
    init();
  }

  protected Action[] createActions(){
    return new Action[]{getOKAction(),getCancelAction(),getHelpAction()};
  }

  public JComponent getPreferredFocusedComponent() {
    return myTargetDirectoryField.getChildComponent();
  }

  protected JComponent createCenterPanel() {
    return null;
  }

  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 2, 0, 0);

    myNameLabel = new JLabel();
    panel.add(myNameLabel, c);
    c.insets.top = 10;
    c.gridy++;    
    panel.add(new JLabel(RefactoringBundle.message("move.files.to.directory.label")), c);
    c.insets.top = 0;
    
    myTargetDirectoryField = new TextFieldWithHistoryWithBrowseButton();
    final List<String> recentEntries = RecentsManager.getInstance(myProject).getRecentEntries(RECENT_KEYS);
    if (recentEntries != null) {
      myTargetDirectoryField.getChildComponent().setHistory(recentEntries);
    }
    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    myTargetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                   RefactoringBundle.message("the.file.will.be.moved.to.this.directory"),
                                                   myProject,
                                                   descriptor,
                                                   TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
    final JTextField textField = myTargetDirectoryField.getChildComponent().getTextEditor();
    FileChooserFactory.getInstance().installFileCompletion(textField, descriptor, true, getDisposable());
    myTargetDirectoryField.setTextFieldPreferredWidth(60);
    c.insets.left = 0;
    c.gridy++;
    panel.add(myTargetDirectoryField, c);
    String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
    final JLabel label = new JLabel(RefactoringBundle.message("path.completion.shortcut", shortcutText));
    UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, label);
    c.insets.left = 6;
    c.gridy++;
    panel.add(label, c);

    myCbSearchForReferences = new NonFocusableCheckBox(RefactoringBundle.message("search.for.references"));
    myCbSearchForReferences.setSelected(RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE);
    c.insets.top = 10;
    c.insets.left = 0;
    c.gridy++;
    panel.add(myCbSearchForReferences, c);

    textField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        validateOKButton();
      }
    });
    Disposer.register(getDisposable(), myTargetDirectoryField);

    return panel;
  }

  public void setData(PsiElement[] psiElements, PsiDirectory initialTargetDirectory, @NonNls String helpID) {
    if (psiElements.length == 1) {
      String text;
      if (psiElements[0] instanceof PsiFile) {
        text = RefactoringBundle.message("move.file.0",
                                         ((PsiFile)psiElements[0]).getVirtualFile().getPresentableUrl());
      }
      else {
        text = RefactoringBundle.message("move.directory.0",
                                         ((PsiDirectory)psiElements[0]).getVirtualFile().getPresentableUrl());
      }
      myNameLabel.setText(text);
    }
    else {
      boolean isFile = true;
      boolean isDirectory = true;
      for (PsiElement psiElement : psiElements) {
        isFile &= psiElement instanceof PsiFile;
        isDirectory &= psiElement instanceof PsiDirectory;
      }
      myNameLabel.setText(isFile ?
                          RefactoringBundle.message("move.specified.files") :
                          isDirectory ?
                          RefactoringBundle.message("move.specified.directories") :
                          RefactoringBundle.message("move.specified.elements"));
    }

    myTargetDirectoryField.getChildComponent().setText(initialTargetDirectory == null ? "" : initialTargetDirectory.getVirtualFile().getPresentableUrl());

    validateOKButton();
    myHelpID = helpID;
  }

  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(myHelpID);
  }

  private void validateOKButton() {
    setOKActionEnabled(myTargetDirectoryField.getChildComponent().getText().length() > 0);
  }

  protected void doOKAction() {
    //myTargetDirectoryField.getChildComponent().addCurrentTextToHistory();
    RecentsManager.getInstance(myProject).registerRecentEntry(RECENT_KEYS, myTargetDirectoryField.getChildComponent().getText());
    RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE = myCbSearchForReferences.isSelected();
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      public void run() {
        final Runnable action = new Runnable() {
          public void run() {
            String directoryName = myTargetDirectoryField.getChildComponent().getText().replace(File.separatorChar, '/');
            try {
              myTargetDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(myProject), directoryName);
            }
            catch (IncorrectOperationException e) {
              // ignore
            }
          }
        };

        ApplicationManager.getApplication().runWriteAction(action);
        if (myTargetDirectory == null) {
          CommonRefactoringUtil.showErrorMessage(getTitle(),
                                                 RefactoringBundle.message("cannot.create.directory"), myHelpID, myProject);
          return;
        }
        myCallback.run(MoveFilesOrDirectoriesDialog.this);
      }
    }, RefactoringBundle.message("move.title"), null);
  }

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }
}
