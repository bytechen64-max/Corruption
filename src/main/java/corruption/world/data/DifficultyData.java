package corruption.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

public class DifficultyData extends SavedData {
    private DifficultyLevel level = DifficultyLevel.NORMAL;
    private boolean initialized = false; // 是否已从NBT加载或手动设置过

    public static DifficultyData load(CompoundTag nbt) {
        DifficultyData data = new DifficultyData();
        data.level = DifficultyLevel.fromValue(nbt.getInt("DifficultyValue"));
        data.initialized = true; // 从NBT加载，说明已有保存
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.putInt("DifficultyValue", level.getValue());
        return nbt;
    }

    public DifficultyLevel getDifficulty() {
        return level;
    }

    public void setDifficulty(DifficultyLevel level) {
        this.level = level;
        this.initialized = true;
        this.setDirty(); // 标记需要保存
    }

    public boolean isInitialized() {
        return initialized;
    }

    public static DifficultyData get(ServerLevel world) {
        DimensionDataStorage storage = world.getDataStorage();
        return storage.computeIfAbsent(DifficultyData::load, DifficultyData::new, "corruption_difficulty");
    }
}