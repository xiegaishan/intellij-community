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
package com.intellij.android.designer.propertyTable.renderers;

import com.intellij.designer.model.RadComponent;
import com.intellij.designer.propertyTable.PropertyRenderer;
import com.intellij.designer.propertyTable.PropertyTable;
import com.intellij.designer.propertyTable.renderers.BooleanRenderer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.android.dom.attrs.AttributeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * @author Alexander Lobas
 */
public class ResourceRenderer implements PropertyRenderer {
  public static final String[] DIMENSIONS = {"dp", "sp", "pt", "px", "mm", "in"};
  private static final String ANDROID_PREFIX = "@android:";

  private final ColorIcon myColorIcon = new ColorIcon(10, 9);
  private BooleanRenderer myBooleanRenderer;
  protected final SimpleColoredComponent myColoredComponent;
  private final Set<AttributeFormat> myFormats;

  public ResourceRenderer(Set<AttributeFormat> formats) {
    if (formats.contains(AttributeFormat.Boolean)) {
      myBooleanRenderer = new BooleanRenderer();
    }

    myColoredComponent = new SimpleColoredComponent();

    myFormats = formats;
  }

  @NotNull
  @Override
  public JComponent getComponent(@Nullable RadComponent component, @Nullable Object object, boolean selected, boolean hasFocus) {
    String value = (String)object;

    if (myBooleanRenderer != null && (StringUtil.isEmpty(value) || "false".equals(value) || "true".equals(value))) {
      myBooleanRenderer.setText(value);
      return myBooleanRenderer.getComponent(component, "true".equals(value), selected, hasFocus);
    }

    myColoredComponent.clear();
    PropertyTable.updateRenderer(myColoredComponent, selected);
    formatValue(value);

    return myColoredComponent;
  }

  protected void formatValue(String value) {
    if (!StringUtil.isEmpty(value)) {
      int prefix = -1;
      if (value.startsWith("#")) {
        prefix = 1;
      }
      else if (value.startsWith(ANDROID_PREFIX)) {
        prefix = ANDROID_PREFIX.length();
      }
      else if (value.startsWith("@")) {
        prefix = 1;
      }
      if (prefix != -1) {
        myColoredComponent.append(value.substring(0, prefix), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        myColoredComponent.append(value.substring(prefix));
      }
      else if (myFormats.contains(AttributeFormat.Dimension) && value.length() > 2) {
        int index = value.length() - 2;
        String dimension = value.substring(index);
        if (ArrayUtil.indexOf(DIMENSIONS, dimension) != -1) {
          myColoredComponent.append(value.substring(0, index));
          myColoredComponent.append(dimension, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
        else {
          myColoredComponent.append(value);
        }
      }
      else {
        myColoredComponent.append(value);
      }
      if (myFormats.contains(AttributeFormat.Color) && value.startsWith("#")) {
        try {
          myColorIcon.setColor(new Color(Integer.parseInt(value.substring(1), 16)));
          myColoredComponent.setIcon(myColorIcon);
        }
        catch (Throwable e) {
        }
      }
    }
  }

  @Override
  public void updateUI() {
    if (myBooleanRenderer != null) {
      SwingUtilities.updateComponentTreeUI(myBooleanRenderer);
    }
    SwingUtilities.updateComponentTreeUI(myColoredComponent);
  }

  private static class ColorIcon extends EmptyIcon {
    private final int myColorSize;
    private Color myColor;

    public ColorIcon(int size, int colorSize) {
      super(size, size);
      myColorSize = colorSize;
    }

    public void setColor(Color color) {
      myColor = color;
    }

    @Override
    public void paintIcon(Component component, Graphics g, final int left, final int top) {
      int iconWidth = getIconWidth();
      int iconHeight = getIconHeight();

      SimpleColoredComponent coloredComponent = (SimpleColoredComponent)component;
      g.setColor(component.getBackground());
      g.fillRect(left - coloredComponent.getIpad().left, 0,
                 iconWidth + coloredComponent.getIpad().left + coloredComponent.getIconTextGap(), component.getHeight());

      int x = left + (iconWidth - myColorSize) / 2;
      int y = top + (iconHeight - myColorSize) / 2;

      g.setColor(myColor);
      g.fillRect(x, y, myColorSize, myColorSize);

      g.setColor(Color.BLACK);
      g.drawRect(x, y, myColorSize, myColorSize);
    }
  }
}