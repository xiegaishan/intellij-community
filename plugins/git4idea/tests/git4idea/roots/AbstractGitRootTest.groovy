/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package git4idea.roots

import git4idea.test.GitMockVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import git4idea.test.GitTestPlatformFacade
import git4idea.test.GitMockProject

/**
 * 
 * @author Kirill Likhodedov
 */
class AbstractGitRootTest {

  GitTestPlatformFacade myPlatformFacade

  AbstractGitRootTest() {
    myPlatformFacade = new GitTestPlatformFacade()
  }

  /**
   * Creates the necessary temporary directories in the filesystem with empty ".git" directories for given roots.
   * And creates an instance of the project.
   * @param gitRoots path to actual .git roots, relative to the project dir.
   */
  Project initProject(Collection<String> gitRoots, Collection<String> projectStructure, Collection<String> contentRoots = []) {
    String projectDir = createDirs(gitRoots)
    Project project = new GitMockProject(projectDir)
    createProjectStructure(project, projectStructure);
    createProjectStructure(project, contentRoots);

    contentRoots.each { myPlatformFacade.myProjectRootManager.myContentRoots << GitMockVirtualFile.fromPath(it, project)}

    project
  }

  void createProjectStructure(Project project, Collection<String> paths) {
    paths.each { String path ->
      File file = new File(project.baseDir.path + "/" + path)
      file.mkdir()
    }
  }

  /**
   * @return path to the project
   */
  private static String createDirs(Collection<String> gitRoots) {
    if (gitRoots.empty) {
      return FileUtil.createTempDirectory("grdt", null);
    }

    File baseDir = createBaseTempDir()
    int maxDepth = findMaxDepthAboveProject(gitRoots)
    File projectDir = createChild(baseDir, maxDepth)
    gitRoots.each { String path ->
      File file = new File(projectDir.path + "/" + path)
      file.mkdirs()
      file.deleteOnExit()

      File gitDir = new File(file, ".git")
      gitDir.mkdir()
      gitDir.deleteOnExit()
    }
    return projectDir.path
  }

  private static File createBaseTempDir() {
    FileUtil.createTempDirectory("pref", null)
  }

  private static File createChild(File base, int depth) {
    File dir = base
    depth.times { dir = FileUtil.createTempDirectory(dir, "grdt", null)}
    dir
  }

  // Assuming that there are no ".." inside the path - only in the beginning
  static int findMaxDepthAboveProject(Collection<String> paths) {
    def len = { String path -> path.split("/").count("..") }
    len(paths.max(len))
  }

}
