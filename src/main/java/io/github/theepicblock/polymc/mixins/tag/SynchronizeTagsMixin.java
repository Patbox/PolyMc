package io.github.theepicblock.polymc.mixins.tag;

import io.github.theepicblock.polymc.impl.Util;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.Map;

@Mixin(SynchronizeTagsS2CPacket.class)
public class SynchronizeTagsMixin {
    @ModifyArg(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeMap(Ljava/util/Map;Lnet/minecraft/network/codec/PacketEncoder;Lnet/minecraft/network/codec/PacketEncoder;)V"))
    public Map<RegistryKey<? extends Registry<?>>, TagPacketSerializer.Serialized> editTagMap(Map<RegistryKey<? extends Registry<?>>, TagPacketSerializer.Serialized> groups) {
        var polyMap = Util.tryGetPolyMap(PacketContext.get());
        if (polyMap.isVanillaLikeMap()) {
            var regMap = new HashMap<RegistryKey<? extends Registry<?>>, TagPacketSerializer.Serialized>();
            for (var regEntry : groups.entrySet()) {
                // Vanilla doesn't like it if it receives tags for registries that don't exist
                if (!Util.isVanilla(regEntry.getKey().getValue())) {
                    continue;
                }

                var map = new HashMap<Identifier, IntList>();
                var reg = Registries.REGISTRIES.get((RegistryKey) regEntry.getKey());
                if (reg != null) {
                    for (var entry : ((SerializedAccessor) (Object) regEntry.getValue()).getContents().entrySet()) {
                        var list = new IntArrayList(entry.getValue().size());

                        for (int i : entry.getValue()) {
                            //noinspection unchecked
                            if (polyMap.canReceiveEntry(reg, reg.get(i))) {
                                list.add(i);
                            }
                        }
                        map.put(entry.getKey(), list);
                    }

                    regMap.put(regEntry.getKey(), SerializedAccessor.createSerialized(map));
                } else {
                    // Dynamic registry, client *should* understand it
                    regMap.put(regEntry.getKey(), regEntry.getValue());
                }
            }
            return regMap;
        } else {
            return groups;
        }
    }
}
