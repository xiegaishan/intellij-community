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

package com.intellij.history.integration;

import com.intellij.history.core.LocalHistoryFacade;
import com.intellij.history.core.LocalHistoryTestCase;
import com.intellij.history.core.Paths;
import com.intellij.history.core.revisions.Revision;
import com.intellij.history.core.tree.Entry;
import com.intellij.history.core.tree.RootEntry;
import com.intellij.history.utils.RunnableAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Clock;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class IntegrationTestCase extends PlatformTestCase {
  public static final int TIMESTAMP_INCREMENT = 3000;
  protected static final String FILTERED_DIR_NAME = "CVS";

  protected VirtualFile myRoot;
  protected IdeaGateway myGateway;

  @SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
  public IntegrationTestCase() {
    PlatformTestCase.initPlatformLangPrefix();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Clock.reset();
    Paths.useSystemCaseSensitivity();

    myGateway = new IdeaGateway();

    ApplicationManager.getApplication().runWriteAction(new RunnableAdapter() {
      @Override
      public void doRun() throws Exception {
        setUpInWriteAction();
      }
    });
  }

  @Override
  protected File getIprFile() throws IOException {
    return new File(createTempDirectory(), "test.ipr");
  }

  protected void setUpInWriteAction() throws Exception {
    myRoot = LocalFileSystem.getInstance().findFileByIoFile(createTempDirectory());
    PsiTestUtil.addContentRoot(myModule, myRoot);
  }

  @Override
  protected void tearDown() throws Exception {
    Clock.reset();
    Paths.useSystemCaseSensitivity();
    super.tearDown();
  }

  protected VirtualFile createFile(String name) throws IOException {
    return createFile(name, null);
  }

  @NotNull
  protected VirtualFile createFile(String name, String content) throws IOException {
    return LocalFileSystem.getInstance().refreshAndFindFileByPath(createFileExternally(name, content));
  }

  @NotNull
  protected VirtualFile createDirectory(String name) throws IOException {
    return LocalFileSystem.getInstance().refreshAndFindFileByPath(createDirectoryExternally(name));
  }

  protected void setContent(VirtualFile f, String content) throws IOException {
    setContent(f, content, f.getTimeStamp() + TIMESTAMP_INCREMENT);
  }

  protected void setContent(VirtualFile f, String content, long timestamp) throws IOException {
    f.setBinaryContent(content.getBytes(), -1, timestamp);
  }

  protected String createFileExternally(String name) throws IOException {
    return createFileExternally(name, null);
  }

  protected String createFileExternally(String name, String content) throws IOException {
    File f = new File(myRoot.getPath(), name);
    f.getParentFile().mkdirs();
    f.createNewFile();
    if (content != null) FileUtil.writeToFile(f, content.getBytes());
    return FileUtil.toSystemIndependentName(f.getPath());
  }

  protected String createDirectoryExternally(String name) throws IOException {
    File f = new File(myRoot.getPath(), name);
    f.mkdirs();
    return FileUtil.toSystemIndependentName(f.getPath());
  }

  protected void setContentExternally(String path, String content) throws IOException {
    File f = new File(path);
    FileUtil.writeToFile(f, content.getBytes());
    f.setLastModified(f.lastModified() + 2000);
  }

  protected void setDocumentTextFor(VirtualFile f, String text) {
    FileDocumentManager.getInstance().getDocument(f).setText(text);
  }

  protected LocalHistoryFacade getVcs() {
    return LocalHistoryImpl.getInstanceImpl().getFacade();
  }

  protected List<Revision> getRevisionsFor(VirtualFile f) {
    return getRevisionsFor(f, null);
  }

  protected List<Revision> getRevisionsFor(final VirtualFile f, final String pattern) {
    return ApplicationManager.getApplication().runReadAction(new Computable<List<Revision>>() {
            public List<Revision> compute() {
              return LocalHistoryTestCase.collectRevisions(getVcs(), getRootEntry(), f.getPath(), myProject.getLocationHash(), pattern);
            }
          });
  }

  protected RootEntry getRootEntry() {
    return myGateway.createTransientRootEntry();
  }

  protected void addContentRoot(String path) {
    addContentRoot(myModule, path);
  }

  protected void addContentRoot(final Module module, final String path) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        ModuleRootManager rm = ModuleRootManager.getInstance(module);
        ModifiableRootModel m = rm.getModifiableModel();
        m.addContentEntry(VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(path)));
        m.commit();
      }
    });
  }

  protected void addExcludedDir(final String path) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        ModuleRootManager rm = ModuleRootManager.getInstance(myModule);
        ModifiableRootModel m = rm.getModifiableModel();
        for (ContentEntry e : m.getContentEntries()) {
          if (e.getFile() != myRoot) continue;
          e.addExcludeFolder(VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(path)));
        }
        m.commit();
      }
    });
  }

  protected void addFileListenerDuring(VirtualFileListener l, Runnable r) throws Exception {
    VirtualFileManager.getInstance().addVirtualFileListener(l);
    try {
      r.run();
    }
    finally {
      VirtualFileManager.getInstance().removeVirtualFileListener(l);
    }
  }


  protected void assertContent(String expected, Entry e) {
    assertEquals(expected, new String(e.getContent().getBytes()));
  }

  protected class ContentChangesListener extends VirtualFileAdapter {
    private final VirtualFile myFile;
    private final String[] myContents = new String[2];

    public ContentChangesListener(VirtualFile f) {
      myFile = f;
    }

    public String getContentBefore() {
      return myContents[0];
    }

    public String getContentAfter() {
      return myContents[1];
    }

    @Override
    public void beforeContentsChange(VirtualFileEvent e) {
      logContent(e, 0);
    }

    @Override
    public void contentsChanged(VirtualFileEvent e) {
      logContent(e, 1);
    }

    private void logContent(VirtualFileEvent e, int i) {
      try {
        if (!e.getFile().equals(myFile)) return;
        myContents[i] = new String(myFile.contentsToByteArray());
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
