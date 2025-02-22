package org.bukkit.craftbukkit.v1_20_R3;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.bukkit.GameEvent;
import org.bukkit.Keyed;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.v1_20_R3.generator.structure.CraftStructure;
import org.bukkit.craftbukkit.v1_20_R3.generator.structure.CraftStructureType;
import org.bukkit.craftbukkit.v1_20_R3.inventory.trim.CraftTrimMaterial;
import org.bukkit.craftbukkit.v1_20_R3.inventory.trim.CraftTrimPattern;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.bukkit.craftbukkit.v1_20_R3.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_20_R3.potion.CraftPotionEffectType;

public class CraftRegistry<B extends Keyed, M> implements Registry<B> {

    private static RegistryAccess registry;

    public static void setMinecraftRegistry(RegistryAccess registry) {
        Preconditions.checkState(CraftRegistry.registry == null, "Registry already set");
        CraftRegistry.registry = registry;
    }

    public static RegistryAccess getMinecraftRegistry() {
        return registry;
    }

    public static <E> net.minecraft.core.Registry<E> getMinecraftRegistry(ResourceKey<net.minecraft.core.Registry<E>> key) {
        return getMinecraftRegistry().registryOrThrow(key);
    }


    public static <B extends Keyed> Registry<?> createRegistry(Class<B> bukkitClass, RegistryAccess registryHolder) {
        if (bukkitClass == Enchantment.class) {
            return new CraftRegistry<>(Enchantment.class, registryHolder.registryOrThrow(Registries.ENCHANTMENT), CraftEnchantment::new);
        }
        if (bukkitClass == GameEvent.class) {
            return new CraftRegistry<>(GameEvent.class, registryHolder.registryOrThrow(Registries.GAME_EVENT), CraftGameEvent::new);
        }
        if (bukkitClass == MusicInstrument.class) {
            return new CraftRegistry<>(MusicInstrument.class, registryHolder.registryOrThrow(Registries.INSTRUMENT), CraftMusicInstrument::new);
        }
        if (bukkitClass == PotionEffectType.class) {
            return new CraftRegistry<>(PotionEffectType.class, registryHolder.registryOrThrow(Registries.MOB_EFFECT), CraftPotionEffectType::new);
        }
        if (bukkitClass == Structure.class) {
            return new CraftRegistry<>(Structure.class, registryHolder.registryOrThrow(Registries.STRUCTURE), CraftStructure::new);
        }
        if (bukkitClass == StructureType.class) {
            return new CraftRegistry<>(StructureType.class, BuiltInRegistries.STRUCTURE_TYPE, CraftStructureType::new);
        }
        if (bukkitClass == TrimMaterial.class) {
            return new CraftRegistry<>(TrimMaterial.class, registryHolder.registryOrThrow(Registries.TRIM_MATERIAL), CraftTrimMaterial::new);
        }
        if (bukkitClass == TrimPattern.class) {
            return new CraftRegistry<>(TrimPattern.class, registryHolder.registryOrThrow(Registries.TRIM_PATTERN), CraftTrimPattern::new);
        }

        return null;
    }

    private final Class<? super B> bukkitClass;
    private final Map<NamespacedKey, B> cache = new HashMap<>();
    private final net.minecraft.core.Registry<M> minecraftRegistry;
    private final BiFunction<NamespacedKey, M, B> minecraftToBukkit;
    private boolean init;

    public CraftRegistry(Class<? super B> bukkitClass, net.minecraft.core.Registry<M> minecraftRegistry, BiFunction<NamespacedKey, M, B> minecraftToBukkit) {
        this.bukkitClass = bukkitClass;
        this.minecraftRegistry = minecraftRegistry;
        this.minecraftToBukkit = minecraftToBukkit;
    }

    @Override
    public B get(NamespacedKey namespacedKey) {
        B cached = cache.get(namespacedKey);
        if (cached != null) {
            return cached;
        }

        // Make sure that the bukkit class is loaded before creating an instance.
        // This ensures that only one instance with a given key is created.
        //
        // Without this code (when bukkit class is not loaded):
        // Registry#get -> #createBukkit -> (load class -> create default) -> put in cache
        // Result: Registry#get != <bukkitClass>.<field> for possible one registry item
        //
        // With this code (when bukkit class is not loaded):
        // Registry#get -> (load class -> create default) -> Registry#get -> get from cache
        // Result: Registry#get == <bukkitClass>.<field>
        if (!init) {
            init = true;
            try {
                Class.forName(bukkitClass.getName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not load registry class " + bukkitClass, e);
            }

            return get(namespacedKey);
        }

        B bukkit = createBukkit(namespacedKey, minecraftRegistry.getOptional(CraftNamespacedKey.toMinecraft(namespacedKey)).orElse(null));
        if (bukkit == null) {
            return null;
        }

        cache.put(namespacedKey, bukkit);

        return bukkit;
    }

    @NotNull
    @Override
    public Stream<B> stream() {
        return minecraftRegistry.keySet().stream().map(minecraftKey -> get(CraftNamespacedKey.fromMinecraft(minecraftKey)));
    }


    @Override
    public Iterator<B> iterator() {
        return stream().iterator();
    }

    public B createBukkit(NamespacedKey namespacedKey, M minecraft) {
        if (minecraft == null) {
            return null;
        }

        return minecraftToBukkit.apply(namespacedKey, minecraft);
    }
}
