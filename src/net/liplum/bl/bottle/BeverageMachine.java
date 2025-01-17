package net.liplum.bl.bottle;

import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import net.liplum.bl.Var;

import static java.lang.Math.max;

public class BeverageMachine extends Block {
    public float sellingReqTime = 120f;

    public BeverageMachine(String name) {
        super(name);
        buildType = BeverageBuild::new;
        hasPower = true;
        hasItems = true;
        update = true;
        solid = true;
        hasLiquids = true;
        consumesPower = true;
        outputsLiquid = true;
    }

    @Override
    public void setBars() {
        super.setBars();
        bars.add("progress",
                (BeverageBuild b) -> new Bar(
                        () -> (int) (b.getProgress() * 100) + "%",
                        () -> b.curSelling != null ? b.curSelling.color : Pal.gray,
                        b::getProgress
                ));
    }

    @Override
    public void init() {
        liquidCapacity = max(liquidCapacity, Var.liquidPerBottle * 3f);
        super.init();
    }

    public class BeverageBuild extends Building {
        public float curSellingTime = 0f;
        @Nullable
        public BottledLiquid curSelling = null;

        public float getProgress() {
            return curSellingTime / sellingReqTime;
        }

        @Override
        public void updateTile() {
            BottledLiquid selling = getCurrentSellingDrink();
            if (selling != null) {
                curSellingTime += edelta();
                if (curSellingTime > sellingReqTime) {
                    curSellingTime = 0f;
                    sell(curSelling);
                }
            }
        }

        /**
         * Output a bottle of drink.
         *
         * @param drink which to be sold
         */
        public void sell(BottledLiquid drink) {
            Liquid liquid = drink.liquid;
            liquids.add(liquid, Var.liquidPerBottle);
            dumpLiquid(liquid);
            curSelling = null;
        }

        /**
         * It won't change until the selling succeeded
         *
         * @return which drink to be sold or null if there is no drink.
         */
        @Nullable
        public BottledLiquid getCurrentSellingDrink() {
            if (curSelling != null) return curSelling;
            for (BottledLiquid drink : Bottling.liquid2Bottled.values()) {
                if (items.get(drink) >= 1) {
                    curSelling = drink;
                    return drink;
                }
            }
            return null;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return item instanceof BottledLiquid;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(curSellingTime);
            write.i(curSelling != null ? curSelling.id : -1);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            curSellingTime = read.f();
            int id = read.i();
            if (id != -1) {
                curSelling = null;
            } else {
                Item item = Vars.content.item(id);
                if (item instanceof BottledLiquid) {
                    curSelling = (BottledLiquid) item;
                } else {
                    curSelling = null;
                }
            }
        }
    }
}
