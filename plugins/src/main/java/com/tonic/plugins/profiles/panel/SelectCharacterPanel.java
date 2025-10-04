package com.tonic.plugins.profiles.panel;


import com.tonic.plugins.profiles.jagex.model.JagCharacter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectCharacterPanel extends JPanel {
    private final Map<Integer, JagCharacter> characterMap = new HashMap<>();
    private final Map<Integer, JCheckBox> checkBoxMap = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public SelectCharacterPanel(List<JagCharacter> characters) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for (JagCharacter character : characters) {
            int id = idGenerator.incrementAndGet();
            JCheckBox checkBox = new JCheckBox(character.getDisplayName());
            checkBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (checkBox.isSelected()) {
                        characterMap.put(id, character);
                    } else {
                        characterMap.remove(id);
                    }
                }
            });
            checkBoxMap.put(id, checkBox);
            add(checkBox);
        }
    }

    public Map<Integer, JagCharacter> getSelectedCharacters() {
        return characterMap;
    }
}