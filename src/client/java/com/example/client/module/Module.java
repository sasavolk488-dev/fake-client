package com.example.client.module;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String name;
    public final String desc;
    public final String category;
    public boolean enabled;
    public float anim;
    public int bindKey = -1; // -1 = нет бинда
    public final List<Setting> settings = new ArrayList<>();

    public Module(String name, String desc, String category, boolean enabled) {
        this.name = name;
        this.desc = desc;
        this.category = category;
        this.enabled = enabled;
        this.anim = enabled ? 1f : 0f;
    }

    public void toggle() { enabled = !enabled; }

    public void onTick() {}

    public Setting num(String name, float def, float min, float max, float step) {
        Setting s = new Setting(name, def, min, max, step);
        settings.add(s);
        return s;
    }

    public Setting bool(String name, boolean def) {
        Setting s = new Setting(name, def);
        settings.add(s);
        return s;
    }

    public static class Setting {
        public final String name;
        public float value;
        public final float min;
        public final float max;
        public final float step;
        public final boolean isBool;
        public boolean boolValue;

        public Setting(String name, float value, float min, float max, float step) {
            this.name = name;
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
            this.isBool = false;
        }

        public Setting(String name, boolean value) {
            this.name = name;
            this.boolValue = value;
            this.value = 0;
            this.min = 0;
            this.max = 1;
            this.step = 1;
            this.isBool = true;
        }

        public void inc() {
            if (isBool) boolValue = !boolValue;
            else value = Math.min(max, value + step);
        }

        public void dec() {
            if (!isBool) value = Math.max(min, value - step);
        }
    }
        }
