package tech.muddykat.engineered_schematics.util;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ESMultiblocks {
    public static final String KEY_NAME = "desc." + EngineeredSchematics.MODID + ".multiblock.";

    private static final String METHOD_TRIGGER = "primaryTrigger";
    private static final String IC_REGISTRY = "com.immersiveconvergence.common.multiblock.IEMultiblockRegistry";
    private static final BlockPos NO_TRIGGER = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static final Map<String, BlockPos> TRIGGERS = triggers();
    private static final Map<String, BlockPos> RESOLVED = new HashMap<>();
    private static final Set<String> REVERSED_LENGTH = new HashSet<>(Arrays.asList("IE:Crusher", "IE:Squeezer", "IE:Fermenter", "IE:Mixer",
            "IE:Refinery", "IE:DieselGenerator", "IE:ArcFurnace", "IE:Excavator", "IE:AutoWorkbench", "IE:BottlingMachine"));

    private ESMultiblocks() {}

    private static Map<String, BlockPos> triggers() {
        Map<String, BlockPos> triggers = new HashMap<>();
        triggers.put("IE:AlloySmelter", new BlockPos(1, 1, 1));
        triggers.put("IE:ArcFurnace", new BlockPos(2, 0, 4));
        triggers.put("IE:Assembler", new BlockPos(1, 1, 2));
        triggers.put("IE:AutoWorkbench", new BlockPos(1, 1, 2));
        triggers.put("IE:BlastFurnace", new BlockPos(1, 1, 2));
        triggers.put("IE:BlastFurnaceAdvanced", new BlockPos(1, 1, 2));
        triggers.put("IE:BottlingMachine", new BlockPos(1, 1, 1));
        triggers.put("IE:BucketWheel", new BlockPos(0, 3, 3));
        triggers.put("IE:CokeOven", new BlockPos(1, 1, 2));
        triggers.put("IE:Crusher", new BlockPos(2, 1, 2));
        triggers.put("IE:DieselGenerator", new BlockPos(1, 1, 4));
        triggers.put("IE:Excavator", new BlockPos(1, 1, 5));
        triggers.put("IE:Fermenter", new BlockPos(1, 1, 1));
        triggers.put("IE:Lightningrod", new BlockPos(1, 1, 2));
        triggers.put("IE:MetalPress", new BlockPos(0, 1, 1));
        triggers.put("IE:Mixer", new BlockPos(1, 1, 1));
        triggers.put("IE:Refinery", new BlockPos(2, 1, 2));
        triggers.put("IE:SheetmetalTank", new BlockPos(1, 1, 2));
        triggers.put("IE:Silo", new BlockPos(1, 1, 2));
        triggers.put("IE:Squeezer", new BlockPos(1, 1, 1));
        return triggers;
    }

    public static boolean hasReversedLength(MultiblockHandler.IMultiblock multiblock) { return REVERSED_LENGTH.contains(multiblock.getUniqueName()); }

    @Nullable
    public static BlockPos getTriggerOffset(MultiblockHandler.IMultiblock multiblock) {
        String uniqueName = multiblock.getUniqueName();
        BlockPos resolved = RESOLVED.get(uniqueName);
        if (resolved == null) {
            resolved = fromConvergence(uniqueName);
            if (resolved == null) { resolved = TRIGGERS.get(uniqueName); }
            if (resolved == null) { resolved = readTriggerField(multiblock); }
            RESOLVED.put(uniqueName, resolved == null ? NO_TRIGGER : resolved);
        }
        return resolved == NO_TRIGGER ? null : resolved;
    }

    @Nullable
    private static BlockPos fromConvergence(String uniqueName) {
        try {
            Object multiblock = Class.forName(IC_REGISTRY).getMethod("get", String.class).invoke(null, uniqueName);
            if (multiblock == null) { return null; }
            Object value = multiblock.getClass().getMethod(METHOD_TRIGGER).invoke(multiblock);
            return value instanceof BlockPos ? (BlockPos)value : null;
        }
        catch (ReflectiveOperationException | LinkageError exception) { return null; }
    }

    @Nullable
    private static BlockPos readTriggerField(MultiblockHandler.IMultiblock multiblock) {
        try {
            Object value = multiblock.getClass().getMethod(METHOD_TRIGGER).invoke(multiblock);
            return value instanceof BlockPos ? (BlockPos)value : null;
        }
        catch (ReflectiveOperationException exception) { return null; }
    }

    @Nullable
    public static MultiblockHandler.IMultiblock getByUniqueName(String uniqueName) {
        for (MultiblockHandler.IMultiblock candidate : MultiblockHandler.getMultiblocks()) {
            if (candidate.getUniqueName().equals(uniqueName)) { return candidate; }
        }
        return null;
    }

    public static String getDisplayName(MultiblockHandler.IMultiblock multiblock) { return ESLang.translate(KEY_NAME + multiblock.getUniqueName()); }
}
