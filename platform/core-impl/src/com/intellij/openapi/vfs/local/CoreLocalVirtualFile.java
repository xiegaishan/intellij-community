/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.local;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * @author yole
 */
public class CoreLocalVirtualFile extends VirtualFile {
  private final CoreLocalFileSystem myFileSystem;
  private final File myIoFile;

  public CoreLocalVirtualFile(CoreLocalFileSystem fileSystem, File ioFile) {
    myFileSystem = fileSystem;
    myIoFile = ioFile;
  }

  @NotNull
  @Override
  public String getName() {
    return myIoFile.getName();
  }

  @NotNull
  @Override
  public VirtualFileSystem getFileSystem() {
    return myFileSystem;
  }

  @Override
  public String getPath() {
    return myIoFile.getAbsolutePath();
  }

  @Override
  public boolean isWritable() {
    return myIoFile.canWrite();
  }

  @Override
  public boolean isDirectory() {
    return myIoFile.isDirectory();
  }

  @Override
  public boolean isValid() {
    return myIoFile.exists();
  }

  @Override
  public VirtualFile getParent() {
    File parentFile = myIoFile.getParentFile();
    return parentFile != null ? new CoreLocalVirtualFile(myFileSystem, parentFile) : null;
  }

  @Override
  public VirtualFile[] getChildren() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray() throws IOException {
    return FileUtil.loadFileBytes(myIoFile);
  }

  @Override
  public long getTimeStamp() {
    return myIoFile.lastModified();
  }

  @Override
  public long getLength() {
    return myIoFile.length();
  }

  @Override
  public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return VfsUtilCore.inputStreamSkippingBOM(new FileInputStream(myIoFile), this);
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoreLocalVirtualFile that = (CoreLocalVirtualFile)o;

    if (myIoFile != null ? !myIoFile.equals(that.myIoFile) : that.myIoFile != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myIoFile != null ? myIoFile.hashCode() : 0;
  }
}