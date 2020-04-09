package com.ray3k.skincomposer.dialog.scenecomposer.menulisteners;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.ray3k.skincomposer.PopTable;
import com.ray3k.skincomposer.Spinner;
import com.ray3k.skincomposer.data.StyleProperty;
import com.ray3k.skincomposer.dialog.DialogListener;
import com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposer;
import com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposerEvents;
import com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposerEvents.WidgetType;
import com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposerModel;
import com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposerModel.SimActor;
import com.ray3k.skincomposer.utils.IntPair;

public class TableListeners {
    public static EventListener tableNameListener(final DialogSceneComposer dialogSceneComposer) {
        var simTable = (DialogSceneComposerModel.SimTable) dialogSceneComposer.simActor;
        var popTableClickListener = new PopTable.PopTableClickListener(DialogSceneComposer.skin) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                
                update();
            }
            
            public void update() {
                var popTable = getPopTable();
                popTable.clearChildren();
    
                var label = new Label("Name:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label);
    
                popTable.row();
                var textField = new TextField("", DialogSceneComposer.skin, "scene");
                textField.setText(simTable.name);
                popTable.add(textField).minWidth(150);
                textField.addListener(DialogSceneComposer.main.getIbeamListener());
                textField.addListener(new TextTooltip("The name of the table to allow for convenient searching via Group#findActor().", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                textField.addListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode) {
                        if (keycode == Input.Keys.ENTER) {
                            popTable.hide();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                textField.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        dialogSceneComposer.events.tableName(textField.getText());
                    }
                });
    
                dialogSceneComposer.getStage().setKeyboardFocus(textField);
                textField.setSelection(0, textField.getText().length());
            }
        };
        
        popTableClickListener.update();
        
        return popTableClickListener;
    }
    
    public static EventListener tableColorListener(final DialogSceneComposerEvents events, SimActor simActor) {
        var simTable = (DialogSceneComposerModel.SimTable) simActor;
        var popTableClickListener = new PopTable.PopTableClickListener(DialogSceneComposer.skin) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                update();
            }
            
            public void update() {
                var popTable = getPopTable();
                popTable.clearChildren();
    
                var label = new Label("Color:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label);
    
                popTable.row();
                var imageButton = new ImageButton(DialogSceneComposer.skin, "scene-color");
                imageButton.getImage().setColor(simTable.color == null ? Color.WHITE : simTable.color.color);
                popTable.add(imageButton).minWidth(100);
                imageButton.addListener(DialogSceneComposer.main.getHandListener());
                imageButton.addListener(new TextTooltip("Select the color of the table background and of the table contents.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
    
                popTable.row();
                var colorLabel = new Label(simTable.color == null ? "No Color" : simTable.color.getName(), DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(colorLabel);
    
                imageButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        popTable.hide();
                        DialogSceneComposer.main.getDialogFactory().showDialogColors(new StyleProperty(), (colorData, pressedCancel) -> {
                            if (!pressedCancel) {
                                events.tableColor(colorData);
                                imageButton.getImage().setColor(colorData == null ? Color.WHITE : colorData.color);
                                colorLabel.setText(colorData == null ? "No Color" : colorData.getName());
                            }
                        }, new DialogListener() {
                            @Override
                            public void opened() {
                    
                            }
                
                            @Override
                            public void closed() {
                    
                            }
                        });
                    }
                });
            }
        };
        
        popTableClickListener.update();
    
        return popTableClickListener;
    }
    
    public static EventListener tablePaddingListener(final DialogSceneComposerEvents events, SimActor simActor) {
        var simTable = (DialogSceneComposerModel.SimTable) simActor;
        var popTableClickListener = new PopTable.PopTableClickListener(DialogSceneComposer.skin) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                update();
            }
            
            public void update() {
                var popTable = getPopTable();
                popTable.clearChildren();
    
                var changeListener = new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Spinner padLeft = popTable.findActor("pad-left");
                        Spinner padRight = popTable.findActor("pad-right");
                        Spinner padTop = popTable.findActor("pad-top");
                        Spinner padBottom = popTable.findActor("pad-bottom");
                        events.tablePadding((float) padLeft.getValue(), (float)  padRight.getValue(), (float)  padTop.getValue(), (float)  padBottom.getValue());
                    }
                };
    
                var label = new Label("Padding:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label).colspan(2);
    
                popTable.row();
                popTable.defaults().right().spaceRight(5);
                label = new Label("Left:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label);
    
                var spinner = new Spinner(0, 1, true, Spinner.Orientation.RIGHT_STACK, DialogSceneComposer.skin, "scene");
                spinner.setName("pad-left");
                spinner.setValue(simTable.padLeft);
                popTable.add(spinner);
                spinner.getTextField().addListener(DialogSceneComposer.main.getIbeamListener());
                spinner.getButtonMinus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.getButtonPlus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.addListener(new TextTooltip("The padding on the left of the contents.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                spinner.addListener(changeListener);
    
                popTable.row();
                label = new Label("Right:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label);
    
                spinner = new Spinner(0, 1, true, Spinner.Orientation.RIGHT_STACK, DialogSceneComposer.skin, "scene");
                spinner.setName("pad-right");
                spinner.setValue(simTable.padRight);
                popTable.add(spinner);
                spinner.getTextField().addListener(DialogSceneComposer.main.getIbeamListener());
                spinner.getButtonMinus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.getButtonPlus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.addListener(new TextTooltip("The padding on the right of the contents.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                spinner.addListener(changeListener);
    
                popTable.row();
                label = new Label("Top:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label);
    
                spinner = new Spinner(0, 1, true, Spinner.Orientation.RIGHT_STACK, DialogSceneComposer.skin, "scene");
                spinner.setName("pad-top");
                spinner.setValue(simTable.padTop);
                popTable.add(spinner);
                spinner.getTextField().addListener(DialogSceneComposer.main.getIbeamListener());
                spinner.getButtonMinus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.getButtonPlus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.addListener(new TextTooltip("The padding on the top of the contents.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                spinner.addListener(changeListener);
    
                popTable.row();
                label = new Label("Bottom:", DialogSceneComposer.skin, "scene-label-colored");
                popTable.add(label);
    
                spinner = new Spinner(0, 1, true, Spinner.Orientation.RIGHT_STACK, DialogSceneComposer.skin, "scene");
                spinner.setName("pad-bottom");
                spinner.setValue(simTable.padBottom);
                popTable.add(spinner);
                spinner.getTextField().addListener(DialogSceneComposer.main.getIbeamListener());
                spinner.getButtonMinus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.getButtonPlus().addListener(DialogSceneComposer.main.getHandListener());
                spinner.addListener(new TextTooltip("The padding on the bottom of the contents.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                spinner.addListener(changeListener);
            }
        };
        
        popTableClickListener.update();
        
        return popTableClickListener;
    }
    
    public static EventListener tableAlignListener(final DialogSceneComposerEvents events, SimActor simActor) {
        var simTable = (DialogSceneComposerModel.SimTable) simActor;
        var popTableClickListener = new PopTable.PopTableClickListener(DialogSceneComposer.skin) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                update();
            }
            
            public void update() {
                var popTable = getPopTable();
                popTable.clearChildren();
    
                var table = new Table();
                popTable.add(table);
    
                var label = new Label("Alignment:", DialogSceneComposer.skin, "scene-label-colored");
                table.add(label).colspan(3);
    
                table.row();
                table.defaults().space(10).left().uniformX();
                var buttonGroup = new ButtonGroup<ImageTextButton>();
                var imageTextButton = new ImageTextButton("Top-Left", DialogSceneComposer.skin, "scene-checkbox-colored");
                var topLeft = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the top left.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.topLeft);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                imageTextButton = new ImageTextButton("Top", DialogSceneComposer.skin, "scene-checkbox-colored");
                var top = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the top center.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.top);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                imageTextButton = new ImageTextButton("Top-Right", DialogSceneComposer.skin, "scene-checkbox-colored");
                var topRight = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the top right.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.topRight);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                table.row();
                imageTextButton = new ImageTextButton("Left", DialogSceneComposer.skin, "scene-checkbox-colored");
                var left = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the middle left.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.left);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                imageTextButton = new ImageTextButton("Center", DialogSceneComposer.skin, "scene-checkbox-colored");
                var center = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the center.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.center);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                imageTextButton = new ImageTextButton("Right", DialogSceneComposer.skin, "scene-checkbox-colored");
                var right = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the middle right.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.right);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                table.row();
                imageTextButton = new ImageTextButton("Bottom-Left", DialogSceneComposer.skin, "scene-checkbox-colored");
                var bottomLeft = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the bottom left.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.bottomLeft);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                imageTextButton = new ImageTextButton("Bottom", DialogSceneComposer.skin, "scene-checkbox-colored");
                var bottom = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the bottom center.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.bottom);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                imageTextButton = new ImageTextButton("Bottom-Right", DialogSceneComposer.skin, "scene-checkbox-colored");
                var bottomRight = imageTextButton;
                imageTextButton.setProgrammaticChangeEvents(false);
                table.add(imageTextButton);
                imageTextButton.addListener(DialogSceneComposer.main.getHandListener());
                imageTextButton.addListener(new TextTooltip("Align the contents to the bottom right.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
                imageTextButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        events.tableAlignment(Align.bottomRight);
                    }
                });
                buttonGroup.add(imageTextButton);
    
                switch (simTable.alignment) {
                    case Align.topLeft:
                        topLeft.setChecked(true);
                        break;
                    case Align.top:
                        top.setChecked(true);
                        break;
                    case Align.topRight:
                        topRight.setChecked(true);
                        break;
                    case Align.right:
                        right.setChecked(true);
                        break;
                    case Align.bottomRight:
                        bottomRight.setChecked(true);
                        break;
                    case Align.bottom:
                        bottom.setChecked(true);
                        break;
                    case Align.bottomLeft:
                        bottomLeft.setChecked(true);
                        break;
                    case Align.left:
                        left.setChecked(true);
                        break;
                    case Align.center:
                        center.setChecked(true);
                        break;
                }
            }
        };
        
        popTableClickListener.update();
        
        return popTableClickListener;
    }
    
    public static EventListener tableSetCellsListener(final DialogSceneComposerEvents events) {
        var popTableClickListener = new PopTable.PopTableClickListener(DialogSceneComposer.skin, "dark");
        var popTable = popTableClickListener.getPopTable();
        
        var label = new Label("Erase contents\nand create\nnew cells:", DialogSceneComposer.skin, "scene-label");
        label.setAlignment(Align.center);
        popTable.add(label);
        label.addListener(new TextTooltip("Sets the cells for this table. This will erase the existing contents.", DialogSceneComposer.main.getTooltipManager(), DialogSceneComposer.skin, "scene"));
        
        popTable.row();
        var table = new Table();
        popTable.add(table);
        
        table.pad(10).padTop(0);
        var buttons = new Button[6][6];
        for (int j = 0; j < 6; j++) {
            table.row();
            for (int i = 0; i < 6; i++) {
                var textButton = new Button(DialogSceneComposer.skin, "scene-table");
                textButton.setProgrammaticChangeEvents(false);
                textButton.setUserObject(new IntPair(i, j));
                table.add(textButton);
                buttons[i][j] = textButton;
                textButton.addListener(DialogSceneComposer.main.getHandListener());
                textButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        popTable.hide();
                        var intPair = (IntPair) textButton.getUserObject();
                        events.tableSetCells(intPair.x + 1, intPair.y + 1);
                    }
                });
                textButton.addListener(new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        var intPair = (IntPair) textButton.getUserObject();
                        for (int y1 = 0; y1 < 6; y1++) {
                            for (int x1 = 0; x1 < 6; x1++) {
                                buttons[x1][y1].setChecked(y1 <= intPair.y && x1 <= intPair.x);
                            }
                        }
                        textButton.setChecked(true);
                    }
                });
            }
        }
        
        table.setTouchable(Touchable.enabled);
        table.addListener(new InputListener() {
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                for (int y1 = 0; y1 < 6; y1++) {
                    for (int x1 = 0; x1 < 6; x1++) {
                        buttons[x1][y1].setChecked(false);
                    }
                }
            }
        });
        return popTableClickListener;
    }
    
    public static Dialog showConfirmCellSetWidgetDialog(final DialogSceneComposer dialogSceneComposer,
                                                        WidgetType widgetType,
                                                        PopTable popTable) {
        var simCell = (DialogSceneComposerModel.SimCell) dialogSceneComposer.simActor;
        if (simCell.child == null) {
            popTable.hide();
            dialogSceneComposer.events.cellSetWidget(widgetType);
            return null;
        } else {
            var dialog = new Dialog("", DialogSceneComposer.skin, "scene-dialog") {
                @Override
                protected void result(Object object) {
                    if ((Boolean) object) {
                        popTable.hide();
                        dialogSceneComposer.events.cellSetWidget(widgetType);
                    }
                }
            };
    
            var root = dialog.getTitleTable();
            root.clear();
    
            root.add().uniform();
    
            var label = new Label("Confirm Overwrite Widget", DialogSceneComposer.skin, "scene-title");
            root.add(label).expandX();
    
            var button = new Button(DialogSceneComposer.skin, "scene-close");
            root.add(button).uniform();
            button.addListener(DialogSceneComposer.main.getHandListener());
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    dialog.hide();
                }
            });
    
            root = dialog.getContentTable();
            root.pad(10);
    
            label = new Label("This will overwrite the existing widget in the cell.\nAre you okay with that?", DialogSceneComposer.skin, "scene-label-colored");
            label.setWrap(true);
            label.setAlignment(Align.center);
            root.add(label).growX();
    
            dialog.getButtonTable().defaults().uniformX();
            var textButton = new TextButton("OK", DialogSceneComposer.skin, "scene-med");
            dialog.button(textButton, true);
            textButton.addListener(DialogSceneComposer.main.getHandListener());
    
            textButton = new TextButton("Cancel", DialogSceneComposer.skin, "scene-med");
            dialog.button(textButton, false);
            textButton.addListener(DialogSceneComposer.main.getHandListener());
            
            dialog.key(Input.Keys.ENTER, true).key(Input.Keys.SPACE, true);
            dialog.key(Input.Keys.ESCAPE, false);
    
            dialog.show(dialogSceneComposer.getStage());
            dialog.setSize(500, 200);
            dialog.setPosition((int) (dialogSceneComposer.getStage().getWidth() / 2f - dialog.getWidth() / 2f), (int) (dialogSceneComposer.getStage().getHeight() / 2f - dialog.getHeight() / 2f));
    
            return dialog;
        }
    }
}
