package tech.muddykat.engineered_schematics.util;

import net.minecraft.util.text.TextComponentTranslation;

public class ESLang {
    private ESLang() {}

    public static String translate(String key) { return new TextComponentTranslation(key).getUnformattedText(); }

    public static String format(String key, Object... args) { return new TextComponentTranslation(key, args).getUnformattedText(); }
}
