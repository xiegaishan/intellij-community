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

package com.intellij.openapi.editor.ex.util;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;

/**
 * @author max
 */
public class LayerDescriptor {
  private SyntaxHighlighter myLayerHighlighter;
  private String myTokenSeparator;
  private TextAttributesKey myBackground;

  public LayerDescriptor(final SyntaxHighlighter layerHighlighter, final String tokenSeparator, final TextAttributesKey background) {
    myBackground = background;
    myLayerHighlighter = layerHighlighter;
    myTokenSeparator = tokenSeparator;
  }
  public LayerDescriptor(final SyntaxHighlighter layerHighlighter, final String tokenSeparator) {
    this(layerHighlighter, tokenSeparator, null);
  }

  public SyntaxHighlighter getLayerHighlighter() {
    return myLayerHighlighter;
  }

  public String getTokenSeparator() {
    return myTokenSeparator;
  }

  public TextAttributesKey getBackgroundKey() {
    return myBackground;
  }
}
