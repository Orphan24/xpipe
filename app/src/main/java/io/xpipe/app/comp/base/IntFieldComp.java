package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IntFieldComp extends Comp<CompStructure<TextField>> {

    Property<Integer> value;
    int minValue;
    int maxValue;

    public IntFieldComp(Property<Integer> value) {
        this.value = value;
        this.minValue = 0;
        this.maxValue = Integer.MAX_VALUE;
    }

    public IntFieldComp(Property<Integer> value, int minValue, int maxValue) {
        this.value = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public CompStructure<TextField> createBase() {
        var text = new TextField(value.getValue() != null ? value.getValue().toString() : null);

        value.addListener((ChangeListener<Number>) (observableValue, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (newValue == null) {
                    text.setText("");
                } else {
                    if (newValue.intValue() < minValue) {
                        value.setValue(minValue);
                        return;
                    }

                    if (newValue.intValue() > maxValue) {
                        value.setValue(maxValue);
                        return;
                    }

                    text.setText(newValue.toString());
                }
            });
        });

        text.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (minValue < 0) {
                if (!"-0123456789".contains(keyEvent.getCharacter())) {
                    keyEvent.consume();
                }
            } else {
                if (!"0123456789".contains(keyEvent.getCharacter())) {
                    keyEvent.consume();
                }
            }
        });

        text.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == null
                    || newValue.isEmpty()
                    || (minValue < 0 && "-".equals(newValue))
                    || !newValue.matches("-?\\d+")) {
                value.setValue(null);
                return;
            }

            int intValue = Integer.parseInt(newValue);
            if (minValue > intValue || intValue > maxValue) {
                text.textProperty().setValue(oldValue);
            }

            value.setValue(Integer.parseInt(text.textProperty().get()));
        });

        return new SimpleCompStructure<>(text);
    }
}
