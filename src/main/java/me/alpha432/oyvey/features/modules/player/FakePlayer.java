package me.alpha432.oyvey.features.modules.player;

import com.mojang.authlib.GameProfile;
import me.alpha432.oyvey.event.events.PacketEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.setting.Setting;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

public class FakePlayer
        extends Module {
    public Setting<Boolean> inv = this.register(new Setting<Boolean>("Inventory", true));
    public Setting<Boolean> pop = this.register(new Setting<Boolean>("TotemPop", true));
    public Setting<String> plrName = this.register(new Setting<String>("Name", "is Pig"));
    private EntityOtherPlayerMP falesnejhrac;

    public FakePlayer() {
        super("FakePlayer", "Spawns a FakePlayer for testing", Module.Category.PLAYER, false, false, false);
    }

    @Override
    public void onEnable() {
        if (FakePlayer.fullNullCheck()) {
            return;
        }
        this.falesnejhrac = new EntityOtherPlayerMP(FakePlayer.mc.world, new GameProfile(UUID.fromString("69722c53-cdba-4a82-89d7-06df2214082f"), this.plrName.getValue()));
        this.falesnejhrac.copyLocationAndAnglesFrom(FakePlayer.mc.player);
        this.falesnejhrac.rotationYawHead = FakePlayer.mc.player.rotationYawHead;
        if (this.inv.getValue().booleanValue()) {
            this.falesnejhrac.inventory = FakePlayer.mc.player.inventory;
        }
        this.falesnejhrac.setHealth(36.0f);
        FakePlayer.mc.world.addEntityToWorld(-100, this.falesnejhrac);
    }

    @Override
    public void onLogout() {
        if (this.isEnabled()) {
            this.disable();
        }
    }

    @Override
    public void onDisable() {
        if (FakePlayer.fullNullCheck()) {
            return;
        }
        try {
            FakePlayer.mc.world.removeEntity(this.falesnejhrac);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (this.pop.getValue().booleanValue() && this.isEnabled() && !FakePlayer.fullNullCheck() && event.getPacket() instanceof SPacketDestroyEntities) {
            SPacketDestroyEntities packet = (SPacketDestroyEntities)event.getPacket();
            for (int id : packet.getEntityIDs()) {
                Entity entity = FakePlayer.mc.world.getEntityByID(id);
                if (!(entity instanceof EntityEnderCrystal) || !(entity.getDistanceSq(this.falesnejhrac) < 144.0)) continue;
                float rawDamage = FakePlayer.calculateDamage(entity.posX, entity.posY, entity.posZ, this.falesnejhrac);
                float absorption = this.falesnejhrac.getAbsorptionAmount() - rawDamage;
                boolean hasHealthDmg = absorption < 0.0f;
                float health = this.falesnejhrac.getHealth() + absorption;
                if (hasHealthDmg && health > 0.0f) {
                    this.falesnejhrac.setHealth(health);
                    this.falesnejhrac.setAbsorptionAmount(0.0f);
                } else if (health > 0.0f) {
                    this.falesnejhrac.setAbsorptionAmount(absorption);
                } else {
                    this.falesnejhrac.setHealth(2.0f);
                    this.falesnejhrac.setAbsorptionAmount(8.0f);
                    this.falesnejhrac.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 5));
                    this.falesnejhrac.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 1));
                    try {
                        FakePlayer.mc.player.connection.handleEntityStatus(new SPacketEntityStatus(this.falesnejhrac, (byte) 35));
                    }
                    catch (Exception exception) {
                    }
                }
                this.falesnejhrac.hurtTime = 5;
            }
        }
    }

    public static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0f;
        double distancedsize = entity.getDistance(posX, posY, posZ) / (double)doubleExplosionSize;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = 0.0;
        try {
            blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
        }
        catch (Exception exception) {
        }
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int)((v * v + v) / 2.0 * 7.0 * (double)doubleExplosionSize + 1.0);
        double finaldamage = 1.0;
        if (entity instanceof EntityLivingBase) {
            finaldamage = FakePlayer.getBlastReduction((EntityLivingBase)entity, FakePlayer.getDamageMultiplied(damage), new Explosion(FakePlayer.mc.world, null, posX, posY, posZ, 6.0f, false, true));
        }
        return (float)finaldamage;
    }

    public static float getBlastReduction(EntityLivingBase entity, float damageI, Explosion explosion) {
        float damage = damageI;
        if (entity instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer)entity;
            DamageSource ds = DamageSource.causeExplosionDamage((Explosion)explosion);
            damage = CombatRules.getDamageAfterAbsorb((float)damage, (float)ep.getTotalArmorValue(), (float)((float)ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue()));
            int k = 0;
            try {
                k = EnchantmentHelper.getEnchantmentModifierDamage((Iterable)ep.getArmorInventoryList(), (DamageSource)ds);
            }
            catch (Exception exception) {
                // empty catch block
            }
            float f = MathHelper.clamp((float)k, (float)0.0f, (float)20.0f);
            damage *= 1.0f - f / 25.0f;
            if (entity.isPotionActive(MobEffects.RESISTANCE)) {
                damage -= damage / 4.0f;
            }
            damage = Math.max(damage, 0.0f);
            return damage;
        }
        damage = CombatRules.getDamageAfterAbsorb((float)damage, (float)entity.getTotalArmorValue(), (float)((float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue()));
        return damage;
    }

    public static float getDamageMultiplied(float damage) {
        int diff = FakePlayer.mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }
}


