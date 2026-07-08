package com.chihaya.moremultiblock.client;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MekanismMoreMultiblock.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MMMRegistry.CONTROLLER_MENU.get(), ControllerScreen::new);
            MenuScreens.register(MMMRegistry.CHEM_MACHINE_MENU.get(), ChemMachineScreen::new);
            MenuScreens.register(MMMRegistry.PORT_MENU.get(), PortScreen::new);
            MenuScreens.register(MMMRegistry.PBF_MENU.get(), PbfScreen::new);
        });
    }
}
