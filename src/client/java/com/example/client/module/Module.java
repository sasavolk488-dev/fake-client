package com.example.client.module;

public class Module {
    public final String name;
    public final String desc;
    public final String category;
    public boolean enabled;
    public float anim;

    public Module(String name, String desc, String category, boolean enabled) {
        this.name = name;
        this.desc = desc;
        this.category = category;
        this.enabled = enabled;
        this.anim = enabled ? 1f : 0f;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void onTick() {
    }
}
