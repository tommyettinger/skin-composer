/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Raymond Buckley
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
package com.ray3k.skincomposer.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Values;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Sort;
import com.ray3k.skincomposer.FilesDroppedListener;
import com.ray3k.skincomposer.Main;
import com.ray3k.skincomposer.Spinner;
import com.ray3k.skincomposer.Undoable;
import com.ray3k.skincomposer.UndoableManager;
import com.ray3k.skincomposer.UndoableManager.CustomDrawableUndoable;
import com.ray3k.skincomposer.UndoableManager.DrawableUndoable;
import com.ray3k.skincomposer.data.ColorData;
import com.ray3k.skincomposer.data.CustomProperty;
import com.ray3k.skincomposer.data.DrawableData;
import com.ray3k.skincomposer.data.StyleData;
import com.ray3k.skincomposer.data.StyleProperty;
import com.ray3k.skincomposer.utils.Utils;
import java.io.File;
import java.util.Iterator;
import java.util.List;

public class DialogDrawables extends Dialog {
    public static DialogDrawables instance;
    private final static int[] sizes = {125, 150, 200, 250};
    private static float scrollPosition = 0.0f;
    private static int sortSelection = 0;
    private SelectBox sortSelectBox;
    private ScrollPane scrollPane;
    private Slider zoomSlider;
    private StyleProperty property;
    private CustomProperty customProperty;
    private Array<DrawableData> drawables;
    private ObjectMap<DrawableData, Drawable> drawablePairs;
    private TextureAtlas atlas;
    private HorizontalGroup contentGroup;
    private FilesDroppedListener filesDroppedListener;
    private EventListener listener;
    private Main main;
    
    public DialogDrawables(Main main, StyleProperty property, EventListener listener) {
        super("", main.getSkin(), "dialog");
        this.property = property;
        initialize(main, listener);
    }
    
    public DialogDrawables(Main main, CustomProperty property, EventListener listener) {
        super("", main.getSkin(), "dialog");
        this.customProperty = property;
        initialize(main, listener);
    }
    
    public void initialize(Main main, EventListener listener) {
        this.main = main;
        
        instance = this;
        
        this.listener = listener;
        
        filesDroppedListener = (Array<FileHandle> files) -> {
            Iterator<FileHandle> iter = files.iterator();
            while (iter.hasNext()) {
                FileHandle file = iter.next();
                if (file.isDirectory() || !(file.name().toLowerCase().endsWith(".png") || file.name().toLowerCase().endsWith(".jpg") || file.name().toLowerCase().endsWith(".jpeg") || file.name().toLowerCase().endsWith(".bmp") || file.name().toLowerCase().endsWith(".gif"))) {
                    iter.remove();
                }
            }
            if (files.size > 0) {
                drawablesSelected(files);
            }
        };
        
        main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);

        drawablePairs = new ObjectMap<>();
        
        gatherDrawables();
        
        produceAtlas();
        
        populate();
    }
    
    /**
     * Recreates the drawables array only including visible drawables.
     */
    private void gatherDrawables() {
        drawables = new Array<>(main.getAtlasData().getDrawables());
        Iterator<DrawableData> iter = drawables.iterator();
        while(iter.hasNext()) {
            DrawableData drawable = iter.next();
            if (!drawable.visible) iter.remove();
        }
    }
    
    /**
     * Writes a TextureAtlas based on drawables list. Creates drawables to be
     * displayed on screen
     * @return 
     */
    private boolean produceAtlas() {
        try {
            if (atlas != null) {
                atlas.dispose();
                atlas = null;
            }
            if (!main.getAtlasData().atlasCurrent) {
                main.getAtlasData().writeAtlas();
                main.getAtlasData().atlasCurrent = true;
            }
            atlas = main.getAtlasData().getAtlas();

            for (DrawableData data : main.getAtlasData().getDrawables()) {
                Drawable drawable;
                if (data.customized) {
                    drawable = getSkin().getDrawable("custom-drawable-skincomposer-image");
                } else if (data.tiled) {
                    String name = data.file.name();
                    name = DrawableData.proper(name);
                    drawable = new TiledDrawable(atlas.findRegion(name));
                    drawable.setMinWidth(data.minWidth);
                    drawable.setMinHeight(data.minHeight);
                    ((TiledDrawable) drawable).getColor().set(main.getJsonData().getColorByName(data.tintName).color);
                } else if (data.file.name().matches(".*\\.9\\.[a-zA-Z0-9]*$")) {
                    String name = data.file.name();
                    name = DrawableData.proper(name);
                    drawable = new NinePatchDrawable(atlas.createPatch(name));
                    if (data.tint != null) {
                        drawable = ((NinePatchDrawable) drawable).tint(data.tint);
                    } else if (data.tintName != null) {
                        drawable = ((NinePatchDrawable) drawable).tint(main.getJsonData().getColorByName(data.tintName).color);
                    }
                } else {
                    String name = data.file.name();
                    name = DrawableData.proper(name);
                    drawable = new SpriteDrawable(atlas.createSprite(name));
                    if (data.tint != null) {
                        drawable = ((SpriteDrawable) drawable).tint(data.tint);
                    } else if (data.tintName != null) {
                        drawable = ((SpriteDrawable) drawable).tint(main.getJsonData().getColorByName(data.tintName).color);
                    }
                }
                
                drawablePairs.put(data, drawable);
            }
            return true;
        } catch (Exception e) {
            Gdx.app.error(getClass().getName(), "Error while attempting to generate drawables.", e);
            main.getDialogFactory().showDialogError("Atlas Error...","Error while attempting to generate drawables.\n\nOpen log?");
            return false;
        }
    }
    
    public void populate() {
        getContentTable().clear();
        
        getButtonTable().padBottom(15.0f);
        
        if (property == null && customProperty == null) {
            getContentTable().add(new Label("Drawables", getSkin(), "title"));
        } else {
            getContentTable().add(new Label("Select a Drawable", getSkin(), "title"));
        }
        
        getContentTable().row();
        Table table = new Table(getSkin());
        table.defaults().pad(10.0f);
        getContentTable().add(table).growX();
        
        table.add("Sort by:");
        
        sortSelectBox = new SelectBox(getSkin());
        sortSelectBox.setItems("A-Z", "Z-A", "Oldest", "Newest");
        sortSelectBox.setSelectedIndex(sortSelection);
        sortSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                sortSelection = sortSelectBox.getSelectedIndex();
                sortBySelectedMode();
            }
        });
        sortSelectBox.addListener(main.getHandListener());
        sortSelectBox.getList().addListener(main.getHandListener());
        table.add(sortSelectBox);
        
        TextButton textButton = new TextButton("Add Drawable", getSkin(), "new");
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                newDrawableDialog();
            }
        });
        textButton.addListener(main.getHandListener());
        table.add(textButton);
        
        textButton = new TextButton("Custom Drawable", getSkin(), "new");
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                customDrawableDialog();
            }
        });
        textButton.addListener(main.getHandListener());
        table.add(textButton);
        
        table.add(new Label("Zoom:", getSkin())).right().expandX();
        zoomSlider = new Slider(0, 3, 1, false, getSkin());
        zoomSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                refreshDrawableDisplay();
            }
        });
        zoomSlider.addListener(main.getHandListener());
        table.add(zoomSlider);
        
        getContentTable().row();
        contentGroup = new HorizontalGroup();
        contentGroup.center().wrap(true).space(5.0f).wrapSpace(5.0f).rowAlign(Align.left);
        scrollPane = new ScrollPane(contentGroup, getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setFlickScroll(false);
        getContentTable().add(scrollPane).grow();
        sortBySelectedMode();
        
        getContentTable().row();
        if (property != null || customProperty != null) {
            button("Clear Drawable", true);
            button("Cancel", false);
            getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
            getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        } else {
            button("Close", false);
            getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
        }
    }

    @Override
    public Dialog show(Stage stage, Action action) {
        Dialog dialog = super.show(stage, action);
        stage.setScrollFocus(scrollPane);
        validate();
        scrollPane.setScrollY(scrollPosition);
        return dialog;
    }
    
    private void refreshDrawableDisplay() {
        contentGroup.clear();
        
        if (drawables.size == 0) {
            Label label = new Label("No drawables have been added!", getSkin());
            contentGroup.addActor(label);
        }
        
        for (DrawableData drawable : drawables) {
            Button drawableButton;
            
            if (property != null || customProperty != null) {
                drawableButton = new Button(getSkin(), "color-base");
                drawableButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        result(drawable);
                        hide();
                    }
                });
                drawableButton.addListener(main.getHandListener());
            } else {
                drawableButton = new Button(getSkin(), "color-base-static");
            }
            contentGroup.addActor(drawableButton);
            
            Table table = new Table();
            drawableButton.add(table).width(sizes[MathUtils.floor(zoomSlider.getValue())]).height(sizes[MathUtils.floor(zoomSlider.getValue())]);

            ClickListener fixDuplicateTouchListener = new ClickListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    event.setBubbles(false);
                    return super.touchDown(event, x, y, pointer, button);
                }
            };
            
            //color wheel
            if (!drawable.customized && !drawable.tiled) {
                Button button = new Button(getSkin(), "colorwheel");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        newTintedDrawable(drawable);
                        event.setBubbles(false);
                    }
                });
                button.addListener(fixDuplicateTouchListener);
                if (property == null && customProperty == null) {
                    button.addListener(main.getHandListener());
                }
                table.add(button);

                TextTooltip toolTip = new TextTooltip("New Tinted Drawable", main.getTooltipManager(), getSkin());
                button.addListener(toolTip);
            } else {
                table.add();
            }

            //swatches
            if (!drawable.customized && !drawable.tiled) {
                Button button = new Button(getSkin(), "swatches");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        colorSwatchesDialog(drawable);
                        event.setBubbles(false);
                    }
                });
                button.addListener(fixDuplicateTouchListener);
                if (property == null && customProperty == null) {
                    button.addListener(main.getHandListener());
                }
                table.add(button);

                TextTooltip toolTip = new TextTooltip("Tinted Drawable from Colors", main.getTooltipManager(), getSkin());
                button.addListener(toolTip);
            } else {
                table.add();
            }
            
            //tiles button (NOT FOR TINTS OR CUSTOM DRAWABLES)
            if (!drawable.customized && drawable.tint == null && drawable.tintName == null) {;
                Button button = new Button(getSkin(), "tiles");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event,
                            Actor actor) {
                        tiledDrawableDialog(drawable);
                        event.setBubbles(false);
                    }
                });
                button.addListener(fixDuplicateTouchListener);
                if (property == null && customProperty == null) {
                    button.addListener(main.getHandListener());
                }
                table.add(button);

                TextTooltip toolTip = new TextTooltip("Tiled Drawable", main.getTooltipManager(), getSkin());
                button.addListener(toolTip);
            } else {
                table.add();
            }
            
            //tiled settings
            if (drawable.tiled) {
                Button button = new Button(getSkin(), "settings-small");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        tiledDrawableSettingsDialog(drawable);
                        event.setBubbles(false);
                    }
                });
                button.addListener(fixDuplicateTouchListener);
                if (property == null && customProperty == null) {
                    button.addListener(main.getHandListener());
                }
                table.add(button);
                
                TextTooltip toolTip = new TextTooltip("Tiled Drawable Settings", main.getTooltipManager(), getSkin());
                button.addListener(toolTip);
            }
            //rename (ONLY FOR TINTS)
            else if (drawable.tint != null || drawable.tintName != null) {
                Button button = new Button(getSkin(), "settings-small");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        renameDrawableDialog(drawable);
                        event.setBubbles(false);
                    }
                });
                button.addListener(fixDuplicateTouchListener);
                if (property == null && customProperty == null) {
                    button.addListener(main.getHandListener());
                }
                table.add(button);
                
                TextTooltip toolTip = new TextTooltip("Rename Tinted Drawable", main.getTooltipManager(), getSkin());
                button.addListener(toolTip);
            }
            //settings for custom drawables
            else if (drawable.customized) {
                Button button = new Button(getSkin(), "settings-small");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        renameCustomDrawableDialog(drawable);
                        event.setBubbles(false);
                    }
                });
                button.addListener(fixDuplicateTouchListener);
                if (property == null && customProperty == null) {
                    button.addListener(main.getHandListener());
                }
                table.add(button);
                
                TextTooltip toolTip = new TextTooltip("Rename Custom Drawable", main.getTooltipManager(), getSkin());
                button.addListener(toolTip);
            } else {
                table.add();
            }

            //delete
            Button button = new Button(getSkin(), "delete-small");
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    deleteDrawable(drawable);
                    event.setBubbles(false);
                }
            });
            button.addListener(fixDuplicateTouchListener);
            if (property == null && customProperty == null) {
                button.addListener(main.getHandListener());
            }
            table.add(button).expandX().right();
            
            TextTooltip toolTip = new TextTooltip("Delete Drawable", main.getTooltipManager(), getSkin());
            button.addListener(toolTip);

            //preview
            table.row();
            Container bg = new Container();
            bg.setClip(true);
            bg.setBackground(getSkin().getDrawable("white"));
            bg.setColor(drawable.bgColor);
            
            Image image = new Image(drawablePairs.get(drawable));
            if (MathUtils.isZero(zoomSlider.getValue())) {
                image.setScaling(Scaling.fit);
                bg.fill(false);
            } else {
                image.setScaling(Scaling.stretch);
                bg.fill();
            }
            bg.setActor(image);
            table.add(bg).colspan(5).grow();

            //name
            table.row();
            Label label = new Label(drawable.name, getSkin());
            label.setEllipsis("...");
            label.setEllipsis(true);
            label.setAlignment(Align.center);
            table.add(label).colspan(5).growX().width(sizes[MathUtils.floor(zoomSlider.getValue())]);
            
            //Tooltip
            toolTip = new TextTooltip(drawable.name, main.getTooltipManager(), getSkin());
            label.addListener(toolTip);
        }
    }
    
    private void colorSwatchesDialog(DrawableData drawableData) {
        DialogColors dialog = new DialogColors(main, (StyleProperty) null, true, (ColorData colorData) -> {
            if (colorData != null) {
                final DrawableData tintedDrawable = new DrawableData(drawableData.file);
                tintedDrawable.tintName = colorData.getName();

                //Fix background color for new, tinted drawable
                Color temp = Utils.averageEdgeColor(tintedDrawable.file, colorData.color);

                if (Utils.brightness(temp) > .5f) {
                    tintedDrawable.bgColor = Color.BLACK;
                } else {
                    tintedDrawable.bgColor = Color.WHITE;
                }

                final TextField textField = new TextField(drawableData.name, getSkin());
                final TextButton button = new TextButton("OK", getSkin());
                button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                button.addListener(main.getHandListener());
                textField.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event,
                            Actor actor) {
                        button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                    }
                });
                textField.addListener(main.getIbeamListener());

                Dialog approveDialog = new Dialog("TintedDrawable...", getSkin(), "bg") {
                    @Override
                    protected void result(Object object) {
                        if (object instanceof Boolean && (boolean) object) {
                            tintedDrawable.name = textField.getText();
                            main.getAtlasData().getDrawables().add(tintedDrawable);
                            main.getProjectData().setChangesSaved(false);
                        }
                    }

                    @Override
                    public boolean remove() {
                        gatherDrawables();
                        produceAtlas();
                        sortBySelectedMode();
                        getStage().setScrollFocus(scrollPane);
                        return super.remove();
                    }
                };
                approveDialog.addCaptureListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode2) {
                        if (keycode2 == Input.Keys.ENTER) {
                            if (!button.isDisabled()) {
                                tintedDrawable.name = textField.getText();
                                main.getAtlasData().getDrawables().add(tintedDrawable);
                                main.getProjectData().setChangesSaved(false);
                                approveDialog.hide();
                            }
                        }
                        return false;
                    }
                });

                approveDialog.getTitleTable().padLeft(5.0f);
                approveDialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
                approveDialog.getButtonTable().padBottom(15.0f);

                approveDialog.text("What is the name of the new tinted drawable?");

                Drawable drawable = drawablePairs.get(drawableData);
                Drawable preview = null;
                if (drawable instanceof SpriteDrawable) {
                    preview = ((SpriteDrawable) drawable).tint(colorData.color);
                } else if (drawable instanceof NinePatchDrawable) {
                    preview = ((NinePatchDrawable) drawable).tint(colorData.color);
                }
                if (preview != null) {
                    approveDialog.getContentTable().row();
                    Table table = new Table();
                    table.setBackground(preview);
                    approveDialog.getContentTable().add(table);
                }

                approveDialog.getContentTable().row();
                approveDialog.getContentTable().add(textField).growX();

                approveDialog.button(button, true);
                approveDialog.button("Cancel", false);
                approveDialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
                approveDialog.key(Input.Keys.ESCAPE, false);
                approveDialog.show(getStage());
                getStage().setKeyboardFocus(textField);
                textField.selectAll();

                textField.setFocusTraversal(false);
            }
        });
        dialog.setFillParent(true);
        dialog.show(getStage());
        dialog.refreshTable();
    }
    
    private void renameDrawableDialog(DrawableData drawable) {
        TextField textField = new TextField("", getSkin());
        Dialog dialog = new Dialog("Rename drawable?", getSkin(), "bg") {
            @Override
            protected void result(Object object) {
                super.result(object);
                
                if (object instanceof Boolean && (boolean) object == true) {
                    renameDrawable(drawable, textField.getText());
                }
                getStage().setScrollFocus(scrollPane);
            }

            @Override
            public Dialog show(Stage stage) {
                Dialog dialog = super.show(stage);
                stage.setKeyboardFocus(textField);
                return dialog;
            }
        };
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        dialog.getContentTable().add(new Label("Please enter a new name for the drawable: ", getSkin()));
        
        dialog.button("OK", true);
        dialog.button("Cancel", false).key(Keys.ESCAPE, false);
        TextButton okButton = (TextButton) dialog.getButtonTable().getCells().first().getActor();
        okButton.setDisabled(true);
        okButton.addListener(main.getHandListener());
        dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        
        dialog.getContentTable().row();
        textField.setText(drawable.name);
        textField.selectAll();
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                boolean disable = !DrawableData.validate(textField.getText());
                if (!disable) {
                    for (DrawableData data : main.getAtlasData().getDrawables()) {
                        if (data.name.equals(textField.getText())) {
                            disable = true;
                            break;
                        }
                    }
                }
                okButton.setDisabled(disable);
            }
        });
        textField.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                if (c == '\n') {
                    if (!okButton.isDisabled()) {
                        renameDrawable(drawable, textField.getText());
                        dialog.hide();
                    }
                }
            }
        });
        textField.addListener(main.getIbeamListener());
        dialog.getContentTable().add(textField);
        
        textField.setFocusTraversal(false);
        
        dialog.show(getStage());
    }
    
    private void renameDrawable(DrawableData drawable, String name) {
        String oldName = drawable.name;
        drawable.name = name;

        main.getUndoableManager().clearUndoables();
        updateStyleValuesForRename(oldName, name);
        
        main.getRootTable().refreshStyleProperties(true);
        main.getRootTable().produceAtlas();
        main.getRootTable().refreshPreview();
        
        main.getProjectData().setChangesSaved(false);
        
        sortBySelectedMode();
    }
    
    private void tiledDrawableDialog(DrawableData drawable) {
        DialogColors dialog = new DialogColors(main, (StyleProperty) null, true, (ColorData colorData) -> {
            if (colorData != null) {
                final Spinner minWidthSpinner = new Spinner(0.0f, 1.0f, true, Spinner.Orientation.HORIZONTAL, getSkin());
                final Spinner minHeightSpinner = new Spinner(0.0f, 1.0f, true, Spinner.Orientation.HORIZONTAL, getSkin());
                TextField textField = new TextField("", getSkin()) {
                    @Override
                    public void next(boolean up) {
                        if (up) {
                            getStage().setKeyboardFocus(minHeightSpinner.getTextField());
                            minHeightSpinner.getTextField().selectAll();
                        } else {
                            getStage().setKeyboardFocus(minWidthSpinner.getTextField());
                            minWidthSpinner.getTextField().selectAll();
                        }
                    }
                    
                };
                Dialog tileDialog = new Dialog("New Tiled Drawable", getSkin(), "bg") {
                    @Override
                    protected void result(Object object) {
                        super.result(object);

                        if (object instanceof Boolean && (boolean) object == true) {
                            tiledDrawable(drawable, colorData, (float) minWidthSpinner.getValue(), (float) minHeightSpinner.getValue(), textField.getText());
                        }
                        getStage().setScrollFocus(scrollPane);
                    }

                    @Override
                    public Dialog show(Stage stage) {
                        Dialog dialog = super.show(stage);
                        stage.setKeyboardFocus(textField);
                        return dialog;
                    }
                };

                tileDialog.getTitleTable().padLeft(5.0f);
                tileDialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
                tileDialog.getButtonTable().padBottom(15.0f);

                tileDialog.getContentTable().add(new Label("Please enter a name for the TiledDrawable: ", getSkin()));

                tileDialog.button("OK", true);
                tileDialog.button("Cancel", false).key(Keys.ESCAPE, false);
                TextButton okButton = (TextButton) tileDialog.getButtonTable().getCells().first().getActor();
                okButton.setDisabled(true);
                okButton.addListener(main.getHandListener());
                tileDialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());

                tileDialog.getContentTable().row();
                textField.setText(drawable.name);
                textField.selectAll();
                tileDialog.getContentTable().add(textField);
                
                Vector2 dimensions = Utils.imageDimensions(drawable.file);
                
                tileDialog.getContentTable().row();
                Table table = new Table();
                table.defaults().space(10.0f);
                tileDialog.getContentTable().add(table);
                Label label = new Label("MinWidth:", getSkin());
                table.add(label);
                minWidthSpinner.setValue(dimensions.x);
                minWidthSpinner.setMinimum(0.0f);
                table.add(minWidthSpinner).minWidth(150.0f);
                minWidthSpinner.setTransversalPrevious(textField);
                minWidthSpinner.setTransversalNext(minHeightSpinner.getTextField());
                
                table.row();
                label = new Label("MinHeight:", getSkin());
                table.add(label);
                minHeightSpinner.setValue(dimensions.y);
                minHeightSpinner.setMinimum(0.0f);
                table.add(minHeightSpinner).minWidth(150.0f);
                minHeightSpinner.setTransversalPrevious(minWidthSpinner.getTextField());
                minHeightSpinner.setTransversalNext(textField);

                textField.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event,
                            Actor actor) {
                        boolean disable = !DrawableData.validate(textField.getText());
                        if (!disable) {
                            for (DrawableData data : main.getAtlasData().getDrawables()) {
                                if (data.name.equals(textField.getText())) {
                                    disable = true;
                                    break;
                                }
                            }
                        }
                        okButton.setDisabled(disable);
                    }
                });
                textField.setTextFieldListener(new TextField.TextFieldListener() {
                    @Override
                    public void keyTyped(TextField textField, char c) {
                        if (c == '\n') {
                            if (!okButton.isDisabled()) {
                                tiledDrawable(drawable, colorData, (float) minWidthSpinner.getValue(), (float) minHeightSpinner.getValue(), textField.getText());
                                tileDialog.hide();
                            }
                        }
                    }
                });
                textField.addListener(main.getIbeamListener());

                tileDialog.show(getStage());
            }
        });
        dialog.setFillParent(true);
        dialog.show(getStage());
        dialog.refreshTable();
    }
    
    private void tiledDrawable(DrawableData drawable, ColorData colorData, float minWidth, float minHeight, String name) {
        DrawableData tiledDrawable = new DrawableData();
        tiledDrawable.name = name;
        tiledDrawable.tintName = colorData.getName();
        tiledDrawable.file = drawable.file;
        tiledDrawable.tiled = true;
        tiledDrawable.visible = true;
        tiledDrawable.minWidth = minWidth;
        tiledDrawable.minHeight = minHeight;
        
        //Fix background color for new, tinted drawable
        Color temp = Utils.averageEdgeColor(tiledDrawable.file, colorData.color);

        if (Utils.brightness(temp) > .5f) {
            tiledDrawable.bgColor = Color.BLACK;
        } else {
            tiledDrawable.bgColor = Color.WHITE;
        }
        
        main.getAtlasData().getDrawables().add(tiledDrawable);
        main.getProjectData().setChangesSaved(false);
        gatherDrawables();
        produceAtlas();
        sortBySelectedMode();
        getStage().setScrollFocus(scrollPane);
        
    }
    
    private void tiledDrawableSettingsDialog(DrawableData drawable) {
        DialogColors dialog = new DialogColors(main, (StyleProperty) null, true, (ColorData colorData) -> {
            if (colorData != null) {
                final Spinner minWidthSpinner = new Spinner(0.0f, 1.0f, true, Spinner.Orientation.HORIZONTAL, getSkin());
                final Spinner minHeightSpinner = new Spinner(0.0f, 1.0f, true, Spinner.Orientation.HORIZONTAL, getSkin());
                TextField textField = new TextField("", getSkin()) {
                    @Override
                    public void next(boolean up) {
                        if (up) {
                            getStage().setKeyboardFocus(minHeightSpinner.getTextField());
                            minHeightSpinner.getTextField().selectAll();
                        } else {
                            getStage().setKeyboardFocus(minWidthSpinner.getTextField());
                            minWidthSpinner.getTextField().selectAll();
                        }
                    }
                    
                };
                Dialog tileDialog = new Dialog("Tiled Drawable Settings", getSkin(), "bg") {
                    @Override
                    protected void result(Object object) {
                        super.result(object);

                        if (object instanceof Boolean && (boolean) object == true) {
                            tiledDrawableSettings(drawable, colorData, (float) minWidthSpinner.getValue(), (float) minHeightSpinner.getValue(), textField.getText());
                        }
                        getStage().setScrollFocus(scrollPane);
                    }

                    @Override
                    public Dialog show(Stage stage) {
                        Dialog dialog = super.show(stage);
                        stage.setKeyboardFocus(textField);
                        return dialog;
                    }
                };

                tileDialog.getTitleTable().padLeft(5.0f);
                tileDialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
                tileDialog.getButtonTable().padBottom(15.0f);

                tileDialog.getContentTable().add(new Label("Please enter a name for the TiledDrawable: ", getSkin()));

                tileDialog.button("OK", true);
                tileDialog.button("Cancel", false).key(Keys.ESCAPE, false);
                TextButton okButton = (TextButton) tileDialog.getButtonTable().getCells().first().getActor();
                okButton.addListener(main.getHandListener());
                tileDialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());

                tileDialog.getContentTable().row();
                textField.setText(drawable.name);
                textField.selectAll();
                tileDialog.getContentTable().add(textField);
                
                tileDialog.getContentTable().row();
                Table table = new Table();
                table.defaults().space(10.0f);
                tileDialog.getContentTable().add(table);
                Label label = new Label("MinWidth:", getSkin());
                table.add(label);
                minWidthSpinner.setValue(drawable.minWidth);
                minWidthSpinner.setMinimum(0.0f);
                table.add(minWidthSpinner).minWidth(150.0f);
                minWidthSpinner.setTransversalPrevious(textField);
                minWidthSpinner.setTransversalNext(minHeightSpinner.getTextField());
                
                table.row();
                label = new Label("MinHeight:", getSkin());
                table.add(label);
                minHeightSpinner.setValue(drawable.minHeight);
                minHeightSpinner.setMinimum(0.0f);
                table.add(minHeightSpinner).minWidth(150.0f);
                minHeightSpinner.setTransversalPrevious(minWidthSpinner.getTextField());
                minHeightSpinner.setTransversalNext(textField);

                textField.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event,
                            Actor actor) {
                        boolean disable = !DrawableData.validate(textField.getText());
                        if (!disable) {
                            if (!drawable.name.equals(textField.getText())) {
                                for (DrawableData data : main.getAtlasData().getDrawables()) {
                                    if (data.name.equals(textField.getText())) {
                                        disable = true;
                                        break;
                                    }
                                }
                            }
                        }
                        okButton.setDisabled(disable);
                    }
                });
                textField.setTextFieldListener(new TextField.TextFieldListener() {
                    @Override
                    public void keyTyped(TextField textField, char c) {
                        if (c == '\n') {
                            if (!okButton.isDisabled()) {
                                tiledDrawableSettings(drawable, colorData, (float) minWidthSpinner.getValue(), (float) minHeightSpinner.getValue(), textField.getText());
                                tileDialog.hide();
                            }
                        }
                    }
                });
                textField.addListener(main.getIbeamListener());

                tileDialog.show(getStage());
            }
        });
        dialog.setFillParent(true);
        dialog.show(getStage());
        dialog.refreshTable();
    }
    
    private void tiledDrawableSettings(DrawableData drawable, ColorData colorData, float minWidth, float minHeight, String name) {
        drawable.name = name;
        drawable.tintName = colorData.getName();
        drawable.minWidth = minWidth;
        drawable.minHeight = minHeight;
        
        //Fix background color for new, tinted drawable
        Color temp = Utils.averageEdgeColor(drawable.file, colorData.color);

        if (Utils.brightness(temp) > .5f) {
            drawable.bgColor = Color.BLACK;
        } else {
            drawable.bgColor = Color.WHITE;
        }
        
        main.getProjectData().setChangesSaved(false);
        gatherDrawables();
        produceAtlas();
        sortBySelectedMode();
        getStage().setScrollFocus(scrollPane);
        
    }
    
    private void updateStyleValuesForRename(String oldName, String newName) {
        Values<Array<StyleData>> values = main.getJsonData().getClassStyleMap().values();
        for (Array<StyleData> styles : values) {
            for (StyleData style : styles) {
                for (StyleProperty styleProperty : style.properties.values()) {
                    if (Drawable.class.isAssignableFrom(styleProperty.type)) {
                        if (styleProperty.value != null && styleProperty.value.equals(oldName)) {
                            styleProperty.value = newName;
                        }
                    }
                }
            }
        }
    }
    
    private void deleteDrawable(DrawableData drawable) {
        if (!drawable.customized && drawable.tint == null && drawable.tintName == null && checkDuplicateDrawables(drawable.file, 1)) {
            showConfirmDeleteDialog(drawable);
        } else {
            main.getAtlasData().getDrawables().removeValue(drawable, true);

            for (Array<StyleData> datas : main.getJsonData().getClassStyleMap().values()) {
                for (StyleData data : datas) {
                    for (StyleProperty styleProperty : data.properties.values()) {
                        if (styleProperty != null && styleProperty.type.equals(Drawable.class) && styleProperty.value != null && styleProperty.value.equals(drawable.toString())) {
                            styleProperty.value = null;
                        }
                    }
                }
            }

            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();

            main.getUndoableManager().clearUndoables();
            
            main.getProjectData().setChangesSaved(false);

            gatherDrawables();
            sortBySelectedMode();
        }
    }

    /**
     * Shows a dialog to confirm deletion of all TintedDrawables based on the
     * provided drawable data. This is called when the delete button is pressed
     * on a drawable in the drawable list.
     * @param drawable 
     */
    private void showConfirmDeleteDialog(DrawableData drawable) {
        Dialog dialog = new Dialog("Delete duplicates?", getSkin(), "bg"){
            @Override
            protected void result(Object object) {
                if ((boolean) object) {
                    main.getProjectData().setChangesSaved(false);
                    removeDuplicateDrawables(drawable.file);
                    gatherDrawables();
                    sortBySelectedMode();
                }
            }
        };
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        dialog.text("Deleting this drawable will also delete one or more tinted drawables.\n"
                + "Delete duplicates?");
        dialog.button("OK", true);
        dialog.button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
        dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(getStage());
    }
    
    /**
     * Sorts by selected sort order and populates the list.
     */
    private void sortBySelectedMode() {
        switch (sortSelectBox.getSelectedIndex()) {
            case 0:
                sortDrawablesAZ();
                break;
            case 1:
                sortDrawablesZA();
                break;
            case 2:
                sortDrawablesOldest();
                break;
            case 3:
                sortDrawablesNewest();
                break;
        }
    }
    
    /**
     * Sorts alphabetically from A to Z.
     */
    private void sortDrawablesAZ() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
        refreshDrawableDisplay();
    }
    
    /**
     * Sorts alphabetically from Z to A.
     */
    private void sortDrawablesZA() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> o1.toString().compareToIgnoreCase(o2.toString()) * -1);
        refreshDrawableDisplay();
    }
    
    /**
     * Sorts by modified date with oldest first.
     */
    private void sortDrawablesOldest() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> {
            if (o1.file.lastModified() < o2.file.lastModified()) {
                return -1;
            } else if (o1.file.lastModified() > o2.file.lastModified()) {
                return 1;
            } else {
                return 0;
            }
        });
        refreshDrawableDisplay();
    }
    
    /**
     * Sorts by modified date with newest first.
     */
    private void sortDrawablesNewest() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> {
            if (o1.file.lastModified() < o2.file.lastModified()) {
                return 1;
            } else if (o1.file.lastModified() > o2.file.lastModified()) {
                return -1;
            } else {
                return 0;
            }
        });
        refreshDrawableDisplay();
    }
    
    /**
     * Checks if there are any drawables that have the same file name as the specified file.
     * This ignores the file extension.
     * @param handle
     * @param minimum The minimum allowed matches before it's considered a duplicate
     * @return 
     */
    private boolean checkDuplicateDrawables(FileHandle handle, int minimum) {
        int count = 0;
        String name = DrawableData.proper(handle.name());
        for (int i = 0; i < main.getAtlasData().getDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getDrawables().get(i);
            if (name.equals(DrawableData.proper(data.file.name()))) {
            if (data.file != null && name.equals(DrawableData.proper(data.file.name()))) {
                count++;
            }
        }
        
        return count > minimum;
    }
    
    /**
     * Removes any duplicate drawables that share the same file name. This
     * ignores the file extension and also deletes TintedDrawables from the
     * same file.
     * @param handle 
     */
    private void removeDuplicateDrawables(FileHandle handle) {
        boolean refreshDrawables = false;
        String name = DrawableData.proper(handle.name());
        for (int i = 0; i < main.getAtlasData().getDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getDrawables().get(i);
            if (name.equals(DrawableData.proper(data.file.name()))) {
                main.getAtlasData().getDrawables().removeValue(data, true);
                
                for (Array<StyleData> datas : main.getJsonData().getClassStyleMap().values()) {
                    for (StyleData tempData : datas) {
                        for (StyleProperty prop : tempData.properties.values()) {
                            if (prop != null && prop.type.equals(Drawable.class) && prop.value != null && prop.value.equals(data.toString())) {
                                prop.value = null;
                            }
                        }
                    }
                }
                
                refreshDrawables = true;
                i--;
            }
        }
        
        main.getRootTable().refreshStyleProperties(true);
        main.getRootTable().refreshPreview();
        
        if (refreshDrawables) {
            gatherDrawables();
        }
    }
    
    /**
     * Show an setStatusBarError indicating a drawable that exceeds project specifications
     */
    private void showDrawableError() {
        Dialog dialog = new Dialog("Error...", getSkin(), "bg");
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        Label label = new Label("Error while adding new drawables.\nEnsure that image dimensions are\nless than maximums specified in project.\nRolling back changes...", getSkin());
        label.setAlignment(Align.center);
        dialog.text(label);
        dialog.button("OK");
        dialog.show(getStage());
    }
    
    private void newDrawableDialog() {
        String defaultPath = "";
        
        if (main.getProjectData().getLastDrawablePath() != null) {
            FileHandle fileHandle = new FileHandle(main.getProjectData().getLastDrawablePath());
            if (fileHandle.parent().exists()) {
                defaultPath = main.getProjectData().getLastDrawablePath();
            }
        }
        
        String[] filterPatterns = null;
        if (!Utils.isMac()) {
            filterPatterns = new String[] {"*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"};
        }
        
        List<File> files = main.getDesktopWorker().openMultipleDialog("Choose drawable file(s)...", defaultPath, filterPatterns, "Image files");
        if (files != null && files.size() > 0) {
            drawablesSelected(files);
        }
    }
    
    private void customDrawableDialog() {
        Array<DrawableData> backup = new Array<>();
        
        main.getDialogFactory().showCustomDrawableDialog(getSkin(), getStage(), new DialogFactory.CustomDrawableListener() {
            @Override
            public void run(String name) {
                DrawableData drawable = new DrawableData(name);
                main.getAtlasData().getDrawables().add(drawable);
                
                gatherDrawables();

                main.getDialogFactory().showDialogLoading(() -> {
                    if (!produceAtlas()) {
                        showDrawableError();
                        Gdx.app.log(getClass().getName(), "Attempting to reload drawables backup...");
                        main.getAtlasData().getDrawables().clear();
                        main.getAtlasData().getDrawables().addAll(backup);
                        gatherDrawables();
                        if (produceAtlas()) {
                            Gdx.app.log(getClass().getName(), "Successfully rolled back changes to drawables");
                        } else {
                            Gdx.app.error(getClass().getName(), "Critical failure, could not roll back changes to drawables");
                        }
                    } else {
                        if (main.getProjectData().areResourcesRelative()) {
                            main.getProjectData().makeResourcesRelative();
                        }

                        main.getProjectData().setChangesSaved(false);
                    }

                    sortBySelectedMode();
                });
            }
        });
    }
    
    private void renameCustomDrawableDialog(DrawableData drawableData) {
        main.getDialogFactory().showCustomDrawableDialog(main.getSkin(), main.getStage(), drawableData, new DialogFactory.CustomDrawableListener() {
            @Override
            public void run(String name) {
                renameDrawable(drawableData, name);
            }
        });
    }

    /**
     * Called when a selection of drawables has been chosen from the
     * newDrawablesDialog(). Adds the new drawables to the project.
     * @param files 
     */
    private void drawablesSelected(List<File> files) {
        Array<FileHandle> fileHandles = new Array<>();
        
        for (File file : files) {
            fileHandles.add(new FileHandle(file));
        }
        
        drawablesSelected(fileHandles);
    }
    
    private void drawablesSelected(Array<FileHandle> files) {
        main.getAtlasData().atlasCurrent = false;
        Array<DrawableData> backup = new Array<>(main.getAtlasData().getDrawables());
        Array<FileHandle> unhandledFiles = new Array<>();
        Array<FileHandle> filesToProcess = new Array<>();
        
        main.getProjectData().setLastDrawablePath(files.get(0).parent().path() + "/");
        for (FileHandle fileHandle : files) {
            if (checkDuplicateDrawables(fileHandle, 0)) {
                unhandledFiles.add(fileHandle);
            } else {
                filesToProcess.add(fileHandle);
            }
        }
        
        if (unhandledFiles.size > 0) {
            showRemoveDuplicatesDialog(unhandledFiles, backup, filesToProcess);
        } else {
            finalizeDrawables(backup, filesToProcess);
        }
    }
    
    /**
     * Shows a dialog to confirm removal of duplicate drawables that have the
     * same name without extension. This is called after selecting new drawables.
     * @param unhandledFiles
     * @param backup
     * @param filesToProcess 
     */
    private void showRemoveDuplicatesDialog(Array<FileHandle> unhandledFiles, Array<DrawableData> backup, Array<FileHandle> filesToProcess) {
        Dialog dialog = new Dialog("Delete duplicates?", getSkin(), "bg"){
            @Override
            protected void result(Object object) {
                if ((boolean) object) {
                    for (FileHandle fileHandle : unhandledFiles) {
                        removeDuplicateDrawables(fileHandle);
                        filesToProcess.add(fileHandle);
                    }
                }
                finalizeDrawables(backup, filesToProcess);
            }
        };
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        dialog.text("Adding this drawable will overwrite one or more drawables\n"
                + "Delete duplicates?");
        dialog.button("OK", true);
        dialog.button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
        dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(getStage());
    }
    
    /**
     * Adds the drawables to the project.
     * @param backup If there is a failure, the drawable list will be rolled
     * back to the provided backup.
     * @param filesToProcess 
     */
    private void finalizeDrawables(Array<DrawableData> backup, Array<FileHandle> filesToProcess) {
        for (FileHandle file : filesToProcess) {
            DrawableData data = new DrawableData(file);
            if (!checkIfNameExists(data.name)) {
                main.getAtlasData().getDrawables().add(data);
            }
        }        
        
        gatherDrawables();

        main.getDialogFactory().showDialogLoading(() -> {
            if (!produceAtlas()) {
                showDrawableError();
                Gdx.app.log(getClass().getName(), "Attempting to reload drawables backup...");
                main.getAtlasData().getDrawables().clear();
                main.getAtlasData().getDrawables().addAll(backup);
                gatherDrawables();
                if (produceAtlas()) {
                    Gdx.app.log(getClass().getName(), "Successfully rolled back changes to drawables");
                } else {
                    Gdx.app.error(getClass().getName(), "Critical failure, could not roll back changes to drawables");
                }
            } else {
                if (main.getProjectData().areResourcesRelative()) {
                    main.getProjectData().makeResourcesRelative();
                }
                
                main.getProjectData().setChangesSaved(false);
            }

            sortBySelectedMode();
        });
    }
    
    /**
     * Creates a TintedDrawable based on the provided DrawableData. Prompts
     * user for a Color and name.
     * @param drawableData 
     */
    private void newTintedDrawable(DrawableData drawableData) {
        Color previousColor = Color.WHITE;
        if (drawableData.tint != null) {
            previousColor = drawableData.tint;
        }
        main.getDialogFactory().showDialogColorPicker(previousColor, new DialogColorPicker.ColorListener() {
            @Override
            public void selected(Color color) {
                if (color != null) {
                    final DrawableData tintedDrawable = new DrawableData(drawableData.file);
                    tintedDrawable.tint = color;
                    
                    //Fix background color for new, tinted drawable
                    Color temp = Utils.averageEdgeColor(tintedDrawable.file, tintedDrawable.tint);
                    
                    if (Utils.brightness(temp) > .5f) {
                        tintedDrawable.bgColor = Color.BLACK;
                    } else {
                        tintedDrawable.bgColor = Color.WHITE;
                    }
                    
                    final TextField textField = new TextField(drawableData.name, getSkin());
                    final TextButton button = new TextButton("OK", getSkin());
                    button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                    textField.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                        }
                    });
                    textField.addListener(main.getIbeamListener());

                    Dialog dialog = new Dialog("TintedDrawable...", getSkin(), "bg") {
                        @Override
                        protected void result(Object object) {
                            if (object instanceof Boolean && (boolean) object) {
                                tintedDrawable.name = textField.getText();
                                main.getAtlasData().getDrawables().add(tintedDrawable);
                                main.getProjectData().setChangesSaved(false);
                            }
                        }

                        @Override
                        public boolean remove() {
                            gatherDrawables();
                            produceAtlas();
                            sortBySelectedMode();
                            getStage().setScrollFocus(scrollPane);
                            return super.remove();
                        }
                    };
                    dialog.getTitleTable().getCells().first().padLeft(5.0f);
                    dialog.addCaptureListener(new InputListener() {
                        @Override
                        public boolean keyDown(InputEvent event, int keycode2) {
                            if (keycode2 == Input.Keys.ENTER) {
                                if (!button.isDisabled()) {
                                    tintedDrawable.name = textField.getText();
                                    main.getAtlasData().getDrawables().add(tintedDrawable);
                                    main.getProjectData().setChangesSaved(false);
                                    dialog.hide();
                                }
                            }
                            return false;
                        }
                    });
                    dialog.text("What is the name of the new tinted drawable?");
                    dialog.getContentTable().getCells().first().pad(10.0f);

                    Drawable drawable = drawablePairs.get(drawableData);
                    Drawable preview = null;
                    if (drawable instanceof SpriteDrawable) {
                        preview = ((SpriteDrawable) drawable).tint(color);
                    } else if (drawable instanceof NinePatchDrawable) {
                        preview = ((NinePatchDrawable) drawable).tint(color);
                    }
                    if (preview != null) {
                        dialog.getContentTable().row();
                        Table table = new Table();
                        table.setBackground(preview);
                        dialog.getContentTable().add(table);
                    }

                    dialog.getContentTable().row();
                    dialog.getContentTable().add(textField).growX().pad(10.0f);

                    dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
                    dialog.button(button, true);
                    button.addListener(main.getHandListener());
                    dialog.button("Cancel", false);
                    dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
                    dialog.key(Input.Keys.ESCAPE, false);
                    dialog.show(getStage());
                    getStage().setKeyboardFocus(textField);
                    textField.selectAll();
                    textField.setFocusTraversal(false);
                }
            }
        });
    }
    
    /**
     * Returns true if any existing drawable has the indicated name.
     * @param name
     * @return 
     */
    private boolean checkIfNameExists(String name) {
        boolean returnValue = false;
        
        for (DrawableData drawable : drawables) {
            if (drawable.name.equals(name)) {
                returnValue = true;
                break;
            }
        }
        
        return returnValue;
    }
    
    @Override
    public boolean remove() {
        scrollPosition = scrollPane.getScrollY();
        
        main.getDesktopWorker().removeFilesDroppedListener(filesDroppedListener);
        
        try {
            if (!main.getAtlasData().atlasCurrent) {
                main.getAtlasData().writeAtlas();
                main.getAtlasData().atlasCurrent = true;
            }
        } catch (Exception e) {
            Gdx.app.error(getClass().getName(), "Error creating atlas upon drawable dialog exit", e);
            main.getDialogFactory().showDialogError("Atlas Error...", "Error creating atlas upon drawable dialog exit.\n\nOpen log?");
        }
        
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
        }
        return super.remove();
    }
    
    @Override
    protected void result(Object object) {
        instance = null;
        if (object != null) {
            if (object instanceof DrawableData) {
                main.getProjectData().setChangesSaved(false);
                DrawableData drawable = (DrawableData) object;

                Undoable undoable;
                if (property != null) {
                    undoable = new DrawableUndoable(main.getRootTable(), main.getAtlasData(),
                                    property, property.value, drawable.name);
                } else {
                    undoable = new UndoableManager.CustomDrawableUndoable(main, customProperty, drawable.name);
                }
                main.getUndoableManager().addUndoable(undoable, true);
            } else if (object instanceof Boolean) {
                if (property != null) {
                    if ((boolean) object) {
                        main.getProjectData().setChangesSaved(false);
                        DrawableUndoable undoable =
                                new DrawableUndoable(main.getRootTable(), main.getAtlasData(),
                                        property, property.value, null);
                        main.getUndoableManager().addUndoable(undoable, true);
                        main.getRootTable().setStatusBarMessage("Drawable emptied for \"" + property.name + "\"");
                    } else {
                        boolean hasDrawable = false;
                        for (DrawableData drawable : main.getAtlasData().getDrawables()) {
                            if (drawable.name.equals(property.value)) {
                                hasDrawable = true;
                                break;
                            }
                        }

                        if (!hasDrawable) {
                            main.getProjectData().setChangesSaved(false);
                            main.getUndoableManager().clearUndoables();
                            property.value = null;
                            main.getRootTable().setStatusBarMessage("Drawable deleted for \"" + property.name + "\"");
                            main.getRootTable().refreshStyleProperties(true);
                        }
                    }
                } else if (customProperty != null) {
                    if ((boolean) object) {
                        main.getProjectData().setChangesSaved(false);
                        CustomDrawableUndoable undoable = new CustomDrawableUndoable(main, customProperty, null);
                        main.getUndoableManager().addUndoable(undoable, true);
                        main.getRootTable().setStatusBarMessage("Drawable emptied for \"" + customProperty.getName() + "\"");
                    } else {
                        boolean hasDrawable = false;
                        for (DrawableData drawable : main.getAtlasData().getDrawables()) {
                            if (drawable.name.equals(customProperty.getValue())) {
                                hasDrawable = true;
                                break;
                            }
                        }

                        if (!hasDrawable) {
                            main.getProjectData().setChangesSaved(false);
                            main.getUndoableManager().clearUndoables();
                            customProperty.setValue(null);
                            main.getRootTable().setStatusBarMessage("Drawable deleted for \"" + customProperty.getName() + "\"");
                            main.getRootTable().refreshStyleProperties(true);
                        }
                    }
                }
            }
        }
        
        //todo: do proper implementation of event handling with fire(), etc.
        if (listener != null) {
            listener.handle(null);
            listener = null;
        }
    }
}
