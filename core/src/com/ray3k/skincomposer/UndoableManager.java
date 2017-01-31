/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2017 Raymond Buckley
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.ray3k.skincomposer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.utils.Array;
import com.ray3k.skincomposer.data.AtlasData;
import com.ray3k.skincomposer.data.ColorData;
import com.ray3k.skincomposer.data.CustomClass;
import com.ray3k.skincomposer.data.CustomProperty;
import com.ray3k.skincomposer.data.CustomProperty.PropertyType;
import com.ray3k.skincomposer.data.CustomStyle;
import com.ray3k.skincomposer.data.FontData;
import com.ray3k.skincomposer.data.JsonData;
import com.ray3k.skincomposer.data.StyleData;
import com.ray3k.skincomposer.data.StyleProperty;
import java.util.Iterator;

public class UndoableManager {
    private final Array<Undoable> undoables;
    private int undoIndex;
    private final Main main;

    public UndoableManager(Main main) {
        undoables = new Array<>();
        undoIndex = -1;
        this.main = main;
    }
    
    public void clearUndoables() {
        undoables.clear();
        undoIndex = -1;
        
        main.getRootTable().setUndoDisabled(true);
        main.getRootTable().setRedoDisabled(true);
        
        main.getRootTable().setUndoText("Undo");
        main.getRootTable().setRedoText("Redo");
    }
    
    public void undo() {
        if (undoIndex >= 0 && undoIndex < undoables.size) {
            main.getProjectData().setChangesSaved(false);
            Undoable undoable = undoables.get(undoIndex);
            undoable.undo();
            undoIndex--;

            if (undoIndex < 0) {
                main.getRootTable().setUndoDisabled(true);
                main.getRootTable().setUndoText("Undo");
            } else {
                main.getRootTable().setUndoText("Undo " + undoables.get(undoIndex).getUndoText());
            }

            main.getRootTable().setRedoDisabled(false);
            main.getRootTable().setRedoText("Redo " + undoable.getUndoText());
        }
    }
    
    public void redo() {
        if (undoIndex >= -1 && undoIndex < undoables.size) {
            main.getProjectData().setChangesSaved(false);
            if (undoIndex < undoables.size - 1) {
                undoIndex++;
                undoables.get(undoIndex).redo();
            }

            if (undoIndex >= undoables.size - 1) {
                main.getRootTable().setRedoDisabled(true);
                main.getRootTable().setRedoText("Redo");
            } else {
                main.getRootTable().setRedoText("Redo " + undoables.get(undoIndex + 1).getUndoText());
            }

            main.getRootTable().setUndoDisabled(false);
            main.getRootTable().setUndoText("Undo " + undoables.get(undoIndex).getUndoText());
        }
    }
    
    public void addUndoable(Undoable undoable, boolean redoImmediately) {
        main.getProjectData().setChangesSaved(false);
        undoIndex++;
        if (undoIndex <= undoables.size - 1) {
            undoables.removeRange(undoIndex, undoables.size - 1);
        }
        undoables.add(undoable);
        
        if (redoImmediately) {
            undoable.redo();
        }
        
        main.getRootTable().setUndoDisabled(false);
        main.getRootTable().setRedoDisabled(true);
        main.getRootTable().setRedoText("Redo");
        main.getRootTable().setUndoText("Undo " + undoable.getUndoText());
        
        if (undoables.size > main.getProjectData().getMaxUndos()) {
            int offset = undoables.size - main.getProjectData().getMaxUndos();
            
            undoIndex -= offset;
            undoIndex = MathUtils.clamp(undoIndex, -1, undoables.size - 1);
            undoables.removeRange(0, offset - 1);
        }
    }
    
    public void addUndoable(Undoable undoable) {
        addUndoable(undoable, false);
    }
    
    public static class DoubleUndoable implements Undoable {
        private final StyleProperty property;
        private final double oldValue;
        private final double newValue;
        private final Main main;

        public DoubleUndoable(Main main, StyleProperty property, double newValue) {
            this.property = property;
            oldValue = (double) property.value;
            this.newValue = newValue;
            this.main = main;
            
            property.value = newValue;
        }
        
        @Override
        public void undo() {
            property.value = oldValue;
            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();
        }

        @Override
        public void redo() {
            property.value = newValue;
            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.name;
        }
    }
    
    public static class CustomDoubleUndoable implements Undoable {
        private final CustomProperty property;
        private final double oldValue;
        private final double newValue;
        private final Main main;

        public CustomDoubleUndoable(Main main, CustomProperty property, double newValue) {
            this.property = property;
            oldValue = (double) property.getValue();
            this.newValue = newValue;
            this.main = main;
            
            property.setValue(newValue);
        }
        
        @Override
        public void undo() {
            property.setValue(oldValue);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public void redo() {
            property.setValue(newValue);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.getName();
        }
    }
    
    public static class CustomTextUndoable implements Undoable {
        private final CustomProperty property;
        private final String oldValue;
        private final String newValue;
        private final Main main;

        public CustomTextUndoable(Main main, CustomProperty property, String newValue) {
            this.property = property;
            oldValue = (String) property.getValue();
            this.newValue = newValue;
            this.main = main;
            
            property.setValue(newValue);
        }
        
        @Override
        public void undo() {
            property.setValue(oldValue);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public void redo() {
            property.setValue(newValue);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.getName();
        }
    }
    
    public static class DrawableUndoable implements Undoable {
        private StyleProperty property;
        private Object oldValue, newValue;
        private RootTable rootTable;
        private AtlasData atlasData;

        public DrawableUndoable(RootTable rootTable, AtlasData atlasData, StyleProperty property, Object oldValue, Object newValue) {
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.rootTable = rootTable;
            this.atlasData = atlasData;
        }

        @Override
        public void undo() {
            rootTable.produceAtlas();
            if (oldValue == null || atlasData.getDrawable((String) oldValue) != null) {
                property.value = oldValue;
            }
            rootTable.setStatusBarMessage("Drawable selected: " + oldValue);
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public void redo() {
            rootTable.produceAtlas();
            if (newValue == null || atlasData.getDrawable((String) newValue) != null) {
                property.value = newValue;
            }
            rootTable.setStatusBarMessage("Drawable selected: " + newValue);
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.name;
        }
        
    }
    
    public static class CustomDrawableUndoable implements Undoable {
        private final CustomProperty property;
        private final String oldValue, newValue;
        private final Main main;

        public CustomDrawableUndoable(Main main, CustomProperty property, String newValue) {
            this.property = property;
            this.oldValue = (String) property.getValue();
            this.newValue = newValue;
            this.main = main;
        }

        @Override
        public void undo() {
            main.getRootTable().produceAtlas();
            if (oldValue == null || main.getAtlasData().getDrawable(oldValue) != null) {
                property.setValue(oldValue);
            }
            main.getRootTable().setStatusBarMessage("Drawable selected: " + oldValue);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public void redo() {
            main.getRootTable().produceAtlas();
            if (newValue == null || main.getAtlasData().getDrawable(newValue) != null) {
                property.setValue(newValue);
            }
            main.getRootTable().setStatusBarMessage("Drawable selected: " + newValue);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.getName();
        }
        
    }
    
    public static class ColorUndoable implements Undoable {
        private StyleProperty property;
        private Object oldValue, newValue;
        private RootTable rootTable;
        private JsonData jsonData;

        public ColorUndoable(RootTable rootTable, JsonData jsonData, StyleProperty property, Object oldValue, Object newValue) {
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.rootTable = rootTable;
            this.jsonData = jsonData;
        }
        
        @Override
        public void undo() {
            if (oldValue == null) {
                property.value = oldValue;
            } else {
                for (ColorData color : jsonData.getColors()) {
                    if (color.getName().equals((String) oldValue)) {
                        property.value = oldValue;
                        break;
                    }
                }
            }
            rootTable.setStatusBarMessage("Selected color: " + oldValue);
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public void redo() {
            if (newValue == null) {
                property.value = newValue;
            } else {
                for (ColorData color : jsonData.getColors()) {
                    if (color.getName().equals((String) newValue)) {
                        property.value = newValue;
                        break;
                    }
                }
            }
            rootTable.setStatusBarMessage("Selected color: " + newValue);
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.name;
        }
    }
    
    public static class CustomColorUndoable implements Undoable {
        private final CustomProperty property;
        private final Object oldValue, newValue;
        private final Main main;

        public CustomColorUndoable(Main main, CustomProperty property, Object newValue) {
            this.property = property;
            oldValue = property.getValue();
            this.newValue = newValue;
            this.main = main;
        }
        
        @Override
        public void undo() {
            if (oldValue == null) {
                property.setValue(oldValue);
            } else {
                for (ColorData color : main.getJsonData().getColors()) {
                    if (color.getName().equals((String) oldValue)) {
                        property.setValue(oldValue);
                        break;
                    }
                }
            }
            main.getRootTable().setStatusBarMessage("Selected color: " + oldValue);
            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();
        }

        @Override
        public void redo() {
            if (newValue == null) {
                property.setValue(newValue);
            } else {
                for (ColorData color : main.getJsonData().getColors()) {
                    if (color.getName().equals((String) newValue)) {
                        property.setValue(newValue);
                        break;
                    }
                }
            }
            main.getRootTable().setStatusBarMessage("Selected color: " + newValue);
            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.getName();
        }
    }
    
    public static class FontUndoable implements Undoable {
        private StyleProperty property;
        private Object oldValue, newValue;
        private RootTable rootTable;
        private JsonData jsonData;
    
        public FontUndoable(RootTable rootTable, JsonData jsonData, StyleProperty property, Object oldValue, Object newValue) {
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.rootTable = rootTable;
            this.jsonData = jsonData;
        }
    
        @Override
        public void undo() {
            if (oldValue == null) {
                property.value = oldValue;
            } else {
                for (FontData font : jsonData.getFonts()) {
                    if (font.getName().equals((String) oldValue)) {
                        property.value = oldValue;
                        break;
                    }
                }
            }
            rootTable.setStatusBarMessage("Selected Font: " + oldValue);
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }
    
        @Override
        public void redo() {
            if (newValue == null) {
                property.value = newValue;
            } else {
                for (FontData font : jsonData.getFonts()) {
                    if (font.getName().equals((String) newValue)) {
                        property.value = newValue;
                        break;
                    }
                }
            }
            rootTable.setStatusBarMessage("Selected Font: " + newValue);
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.name;
        }
    }
    
    public static class CustomFontUndoable implements Undoable {
        private final CustomProperty property;
        private final Object oldValue, newValue;
        private final Main main;
    
        public CustomFontUndoable(Main main, CustomProperty property, Object newValue) {
            this.property = property;
            this.oldValue = property.getValue();
            this.newValue = newValue;
            this.main = main;
        }
    
        @Override
        public void undo() {
            if (oldValue == null) {
                property.setValue(oldValue);
            } else {
                for (FontData font : main.getJsonData().getFonts()) {
                    if (font.getName().equals((String) oldValue)) {
                        property.setValue(oldValue);
                        break;
                    }
                }
            }
            main.getRootTable().setStatusBarMessage("Selected Font: " + oldValue);
            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();
        }
    
        @Override
        public void redo() {
            if (newValue == null) {
                property.setValue(newValue);
            } else {
                for (FontData font : main.getJsonData().getFonts()) {
                    if (font.getName().equals((String) newValue)) {
                        property.setValue(newValue);
                        break;
                    }
                }
            }
            main.getRootTable().setStatusBarMessage("Selected Font: " + newValue);
            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.getName();
        }
    }
    
    public static class SelectBoxUndoable implements Undoable {
        private StyleProperty property;
        private SelectBox<StyleData> selectBox;
        private String oldValue, newValue;
        private RootTable rootTable;
    
        public SelectBoxUndoable(RootTable rootTable, StyleProperty property, SelectBox<StyleData> selectBox) {
            this.property = property;
            this.selectBox = selectBox;

            oldValue = (String) property.value;
            newValue = selectBox.getSelected().name;
            this.rootTable = rootTable;
        }

        @Override
        public void undo() {
            property.value = oldValue;
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public void redo() {
            property.value = newValue;
            rootTable.refreshStyleProperties(true);
            rootTable.refreshPreview();
        }

        @Override
        public String getUndoText() {
            return "Change Style Property " + property.name;
        }
    }

    public static class NewStyleUndoable implements Undoable {
        private StyleData styleData;
        private final Main main;
        private final Class selectedClass;
        private final String name;

        public NewStyleUndoable(Class selectedClass, String name, Main main) {
            this.main = main;
            this.selectedClass = selectedClass;
            this.name = name;
        }
        
        @Override
        public void undo() {
            main.getJsonData().deleteStyle(styleData);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            styleData = main.getJsonData().newStyle(selectedClass, name);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Create Style \"" + styleData.name + "\"";
        }
    }

    public static class DuplicateStyleUndoable implements Undoable {
        private StyleData styleData;
        private final Main main;
        private final String name;
        private final StyleData originalStyle;

        public DuplicateStyleUndoable(StyleData originalStyle, String name, Main main) {
            this.main = main;
            this.name = name;
            this.originalStyle = originalStyle;
        }
        
        @Override
        public void undo() {
            main.getJsonData().deleteStyle(styleData);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            styleData = main.getJsonData().copyStyle(originalStyle, name);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Duplicate Style \"" + styleData.name + "\"";
        }
    }
    
    public static class DeleteStyleUndoable implements Undoable {
        private final StyleData styleData;
        private final Main main;

        public DeleteStyleUndoable(StyleData styleData, Main main) {
            this.styleData = styleData;
            this.main = main;
        }

        @Override
        public void undo() {
            main.getJsonData().copyStyle(styleData, styleData.name);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            main.getJsonData().deleteStyle(styleData);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Delete Style \"" + styleData.name + "\"";
        }
    }

    public static class RenameStyleUndoable implements Undoable {
        private final StyleData styleData;
        private final Main main;
        private final String oldName;
        private final String newName;

        public RenameStyleUndoable(StyleData styleData, Main main, String name) {
            this.styleData = styleData;
            this.main = main;
            
            oldName = styleData.name;
            newName = name;
        }
        
        @Override
        public void undo() {
            styleData.name = oldName;
            
            for (Array<StyleData> styles : main.getJsonData().getClassStyleMap().values()) {
                for (StyleData style : styles) {
                    for (StyleProperty styleProperty : style.properties.values()) {
                        if (styleProperty.type.equals(Main.basicToStyleClass(styleData.clazz)) && styleProperty.value.equals(newName)) {
                            styleProperty.value = oldName;
                        }
                    }
                }
            }
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            styleData.name = newName;
            
            for (Array<StyleData> styles : main.getJsonData().getClassStyleMap().values()) {
                for (StyleData style : styles) {
                    for (StyleProperty styleProperty : style.properties.values()) {
                        if (styleProperty.type.equals(Main.basicToStyleClass(styleData.clazz)) && styleProperty.value.equals(oldName)) {
                            styleProperty.value = newName;
                        }
                    }
                }
            }
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Rename Style \"" + styleData.name + "\"";
        }
        
    }

    public static class NewCustomClassUndoable implements Undoable {
        private final String displayName;
        private final Main main;
        private final CustomClass customClass;

        public NewCustomClassUndoable(String fullyQualifiedName, String displayName, Main main) {
            this.displayName = displayName;
            this.main = main;
            customClass = new CustomClass(fullyQualifiedName, displayName);
        }
        
        @Override
        public void undo() {
            main.getJsonData().getCustomClasses().removeValue(customClass, true);
        }

        @Override
        public void redo() {
            main.getJsonData().getCustomClasses().add(customClass);
            main.getRootTable().refreshClasses(true);
        }

        @Override
        public String getUndoText() {
            return "New Class " + displayName;
        }
    }
    
    public static class RenameCustomClassUndoable implements Undoable {
        private final Main main;
        private final String displayName;
        private final String fullyQualifiedName;
        private final String oldName;
        private final String oldFullyQualifiedName;
        private final CustomClass customClass;

        public RenameCustomClassUndoable(Main main, String displayName, String fullyQualifiedName) {
            this.main = main;
            this.displayName = displayName;
            this.fullyQualifiedName = fullyQualifiedName;
            customClass = (CustomClass) main.getRootTable().getClassSelectBox().getSelected();
            oldName = customClass.getDisplayName();
            oldFullyQualifiedName = customClass.getFullyQualifiedName();
        }

        @Override
        public void undo() {
            customClass.setDisplayName(oldName);
            customClass.setFullyQualifiedName(oldFullyQualifiedName);
            main.getRootTable().refreshClasses(false);
        }

        @Override
        public void redo() {
            customClass.setDisplayName(displayName);
            customClass.setFullyQualifiedName(fullyQualifiedName);
            main.getRootTable().refreshClasses(false);
        }

        @Override
        public String getUndoText() {
            return "Rename Class to " + displayName;
        }
        
    }
    
    public static class DeleteCustomClassUndoable implements Undoable {
        private final Main main;
        private final CustomClass customClass;

        public DeleteCustomClassUndoable(Main main) {
            this.main = main;
            customClass = (CustomClass) main.getRootTable().getClassSelectBox().getSelected();
        }
        
        @Override
        public void undo() {
            main.getJsonData().getCustomClasses().add(customClass);
            main.getRootTable().refreshClasses(true);
            main.getRootTable().refreshClasses(false);
        }

        @Override
        public void redo() {
            main.getJsonData().getCustomClasses().removeValue(customClass, true);
            main.getRootTable().refreshClasses(false);
        }

        @Override
        public String getUndoText() {
            return "Delete class " + customClass.getDisplayName();
        }
        
    }
    
    public static class DuplicateCustomClassUndoable implements Undoable{
        private Main main;
        private CustomClass customClass;

        public DuplicateCustomClassUndoable(Main main, String displayName, String fullyQualifiedName) {
            this.main = main;
            
            Object selected = main.getRootTable().getClassSelectBox().getSelected();

            if (selected instanceof CustomClass) {
                customClass = ((CustomClass) selected).copy();
                customClass.setDisplayName(displayName);
                customClass.setFullyQualifiedName(fullyQualifiedName);
            }
        }
        
        @Override
        public void undo() {
            main.getJsonData().getCustomClasses().removeValue(customClass, true);
            main.getRootTable().refreshClasses(true);
        }

        @Override
        public void redo() {
            main.getJsonData().getCustomClasses().add(customClass);
            main.getRootTable().refreshClasses(true);
        }

        @Override
        public String getUndoText() {
            return "Duplicate class " + customClass.getDisplayName();
        }
    }
    
    public static class NewCustomPropertyUndoable implements Undoable {
        private final Main main;
        private final CustomClass customClass;
        private final CustomProperty customProperty;

        public NewCustomPropertyUndoable(Main main, String propertyName, PropertyType propertyType) {
            this.main = main;
            
            customProperty = new CustomProperty(propertyName, propertyType);
            customClass = (CustomClass) main.getRootTable().getClassSelectBox().getSelected();
        }
        
        
        @Override
        public void undo() {
            customClass.getTemplateStyle().getProperties().removeValue(customProperty, true);

            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                Iterator<CustomProperty> iter = style.getProperties().iterator();
                while (iter.hasNext()) {
                    CustomProperty property = iter.next();
                    if (property.getName().equals(customProperty.getName())) {
                        iter.remove();
                    }
                }
            }
        }

        @Override
        public void redo() {
            customClass.getTemplateStyle().getProperties().add(customProperty);

            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                style.getProperties().add(customProperty.copy());
            }
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public String getUndoText() {
            return "New Property " + customProperty.getName();
        }
    }
    
    public static class DuplicateCustomPropertyUndoable implements Undoable {
        private final Main main;
        private final CustomClass customClass;
        private CustomProperty customProperty;

        public DuplicateCustomPropertyUndoable(Main main, CustomProperty originalProperty, String propertyName, PropertyType propertyType) {
            this.main = main;
            
            customProperty = originalProperty.copy();
            
            customClass = (CustomClass) main.getRootTable().getClassSelectBox().getSelected();
        }
        
        
        @Override
        public void undo() {
            customClass.getTemplateStyle().getProperties().removeValue(customProperty, true);

            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                Iterator<CustomProperty> iter = style.getProperties().iterator();
                while (iter.hasNext()) {
                    CustomProperty property = iter.next();
                    if (property.getName().equals(customProperty.getName())) {
                        iter.remove();
                    }
                }
            }
        }

        @Override
        public void redo() {
            customClass.getTemplateStyle().getProperties().add(customProperty);

            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                style.getProperties().add(customProperty.copy());
            }
        }

        @Override
        public String getUndoText() {
            return "Duplicate Property " + customProperty.getName();
        }
    }
    
    public static class RenameCustomPropertyUndoable implements Undoable {
        private final Main main;
        private final CustomClass customClass;
        private final CustomProperty customProperty;
        private final String oldName;
        private final PropertyType oldType;
        private final String newName;
        private final PropertyType newType;

        public RenameCustomPropertyUndoable(Main main, CustomProperty customProperty, String propertyName, PropertyType propertyType) {
            this.main = main;
            
            this.customProperty = customProperty;
            
            customClass = (CustomClass) main.getRootTable().getClassSelectBox().getSelected();
            
            oldName = customProperty.getName();
            oldType = customProperty.getType();
            newName = propertyName;
            newType = propertyType;
        }
        
        
        @Override
        public void undo() {
            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                //rename the property in every style in this class.
                for (CustomProperty property : style.getProperties()) {
                    if (property.getName().equals(customProperty.getName())) {
                        property.setName(oldName);
                        property.setType(oldType);
                    }
                }
            }

            //rename the template style
            customProperty.setName(oldName);
            customProperty.setType(oldType);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public void redo() {
            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                //rename the property in every style in this class.
                for (CustomProperty property : style.getProperties()) {
                    if (property.getName().equals(customProperty.getName())) {
                        property.setName(newName);
                        property.setType(newType);
                    }
                }
            }

            //rename the template style
            customProperty.setName(newName);
            customProperty.setType(newType);
            main.getRootTable().refreshStyleProperties(true);
        }

        @Override
        public String getUndoText() {
            return "Rename Property " + customProperty.getName();
        }
    }

    public static class DeleteCustomPropertyUndoable implements Undoable {
        private Main main;
        private CustomClass customClass;
        private CustomProperty customProperty;

        public DeleteCustomPropertyUndoable(Main main, CustomProperty customProperty) {
            this.main = main;
            
            this.customProperty = customProperty;
            
            customClass = (CustomClass) main.getRootTable().getClassSelectBox().getSelected();
        }
        
        @Override
        public void undo() {
            customClass.getTemplateStyle().getProperties().add(customProperty);

            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                style.getProperties().add(customProperty.copy());
            }
        }
        
        @Override
        public void redo() {
            customClass.getTemplateStyle().getProperties().removeValue(customProperty, true);

            for (com.ray3k.skincomposer.data.CustomStyle style : customClass.getStyles()) {
                Iterator<CustomProperty> iter = style.getProperties().iterator();
                while (iter.hasNext()) {
                    CustomProperty property = iter.next();
                    if (property.getName().equals(customProperty.getName())) {
                        iter.remove();
                    }
                }
            }
        }

        @Override
        public String getUndoText() {
            return "Delete Property " + customProperty.getName();
        }
    }

    public static class NewCustomStyleUndoable implements Undoable {
        private final Main main;
        private final CustomClass parent;
        private final CustomStyle style;

        public NewCustomStyleUndoable(Main main, String name, CustomClass parent) {
            this.main = main;
            this.parent = parent;
            
            style = parent.getTemplateStyle().copy();
            style.setName(name);
        }
        
        @Override
        public void undo() {
            parent.getStyles().removeValue(style, true);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            parent.getStyles().add(style);
            main.getRootTable().getClassSelectBox().setSelected(parent);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "New style " + style.getName();
        }
    }
    
    public static class DuplicateCustomStyleUndoable implements Undoable {
        private final Main main;
        private final CustomStyle style;

        public DuplicateCustomStyleUndoable(Main main, String name, CustomStyle originalStyle) {
            this.main = main;
            
            style = originalStyle.copy();
            style.setName(name);
            style.setDeletable(true);
        }
        
        @Override
        public void undo() {
            style.getParentClass().getStyles().removeValue(style, true);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            style.getParentClass().getStyles().add(style);
            main.getRootTable().getClassSelectBox().setSelected(style.getParentClass());
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Duplicate style " + style.getName();
        }
    }
    
    public static class DeleteCustomStyleUndoable implements Undoable {
        private final Main main;
        private final CustomStyle style;

        public DeleteCustomStyleUndoable(Main main, CustomStyle style) {
            this.main = main;
            
            this.style = style;
        }
        
        @Override
        public void undo() {
            style.getParentClass().getStyles().add(style);
            main.getRootTable().getClassSelectBox().setSelected(style.getParentClass());
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            style.getParentClass().getStyles().removeValue(style, true);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Delete style " + style.getName();
        }
    }

    public static class RenameCustomStyleUndoable implements Undoable {
        private final Main main;
        private final CustomStyle style;
        String oldName, name;

        public RenameCustomStyleUndoable(Main main, String name, CustomStyle style) {
            this.main = main;
            
            this.style = style;
            oldName = style.getName();
            this.name = name;
        }
        
        @Override
        public void undo() {
            style.setName(oldName);
            style.getParentClass().getStyles().removeValue(style, true);
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public void redo() {
            style.setName(name);
            main.getRootTable().getClassSelectBox().setSelected(style.getParentClass());
            main.getRootTable().refreshStyles(true);
        }

        @Override
        public String getUndoText() {
            return "Duplicate style " + style.getName();
        }
    }
}