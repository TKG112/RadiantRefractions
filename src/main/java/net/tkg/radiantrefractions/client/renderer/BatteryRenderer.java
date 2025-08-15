package net.tkg.radiantrefractions.client.renderer;

import net.tkg.radiantrefractions.client.model.BatteryItemModel;
import net.tkg.radiantrefractions.server.item.BatteryItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BatteryRenderer extends GeoItemRenderer<BatteryItem> {

    public BatteryRenderer() {
        super(new BatteryItemModel());
    }

    public static class BatteryItemRenderer extends GeoItemRenderer<BatteryItem> {
        public BatteryItemRenderer() { super(new BatteryItemModel()); }
    }
}
