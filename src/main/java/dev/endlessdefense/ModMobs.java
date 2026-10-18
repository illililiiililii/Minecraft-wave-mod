package dev.endlessdefense;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = EndlessDefense.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModMobs {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EndlessDefense.MODID);
    public static final RegistryObject<EntityType<AshRunner>> ASH_RUNNER = TYPES.register("ash_runner", () ->
            EntityType.Builder.of(AshRunner::new, MobCategory.MONSTER).sized(0.6f, 1.95f).build("ash_runner"));
    public static final RegistryObject<EntityType<IronBulwark>> IRON_BULWARK = TYPES.register("iron_bulwark", () ->
            EntityType.Builder.of(IronBulwark::new, MobCategory.MONSTER).sized(0.6f, 1.95f).build("iron_bulwark"));
    public static final RegistryObject<EntityType<FrostWarden>> FROST_WARDEN = TYPES.register("frost_warden", () ->
            EntityType.Builder.of(FrostWarden::new, MobCategory.MONSTER).sized(0.6f, 1.95f).build("frost_warden"));
    public static final RegistryObject<EntityType<SiegeBrute>> SIEGE_BRUTE = TYPES.register("siege_brute", () ->
            EntityType.Builder.of(SiegeBrute::new, MobCategory.MONSTER).sized(0.9f, 2.1f).build("siege_brute"));
    public static final RegistryObject<EntityType<DreadKnight>> DREAD_KNIGHT = TYPES.register("dread_knight", () ->
            EntityType.Builder.of(DreadKnight::new, MobCategory.MONSTER).sized(0.65f, 2.0f).build("dread_knight"));

    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ASH_RUNNER.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 16).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.FOLLOW_RANGE, 32).build());
        event.put(IRON_BULWARK.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 36).add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4).build());
        event.put(FROST_WARDEN.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 26).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 36).build());
        event.put(SIEGE_BRUTE.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 52).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 7).add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).build());
        event.put(DREAD_KNIGHT.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 70).add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ATTACK_DAMAGE, 9).add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).build());
    }

    public static final class AshRunner extends Zombie {
        public AshRunner(EntityType<? extends Zombie> type, Level level) { super(type, level); }
        @Override protected boolean isSunSensitive() { return false; }
        @Override public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
            return hit;
        }
    }

    public static final class IronBulwark extends Zombie {
        public IronBulwark(EntityType<? extends Zombie> type, Level level) { super(type, level); }
        @Override protected boolean isSunSensitive() { return false; }
    }

    public static final class FrostWarden extends Zombie {
        public FrostWarden(EntityType<? extends Zombie> type, Level level) { super(type, level); }
        @Override protected boolean isSunSensitive() { return false; }
        @Override public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
            return hit;
        }
    }

    public static final class SiegeBrute extends Zombie {
        public SiegeBrute(EntityType<? extends Zombie> type, Level level) { super(type, level); }
        @Override protected boolean isSunSensitive() { return false; }
        @Override public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living)
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0), this);
            return hit;
        }
    }

    public static final class DreadKnight extends Zombie {
        public DreadKnight(EntityType<? extends Zombie> type, Level level) { super(type, level); }
        @Override protected boolean isSunSensitive() { return false; }
        @Override public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living)
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), this);
            return hit;
        }
    }
}
