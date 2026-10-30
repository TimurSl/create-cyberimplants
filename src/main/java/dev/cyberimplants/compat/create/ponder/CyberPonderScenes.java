package dev.cyberimplants.compat.create.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** The storyboards of {@link CyberPonderPlugin}. Text lives in the language files under cyberimplants.ponder.*. */
final class CyberPonderScenes {
    private static void intro(CreateSceneBuilder scene, String id, String title) {
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
    }

    private static void say(CreateSceneBuilder scene, int ticks, String text, BlockPos at, SceneBuildingUtil util) {
        scene.overlay().showText(ticks).text(text).pointAt(util.vector().topOf(at)).placeNearTarget().attachKeyFrame();
        scene.idle(ticks + 10);
    }

    private static void outline(CreateSceneBuilder scene, PonderPalette color, BlockPos at, int ticks) {
        scene.overlay().chaseBoundingBoxOutline(color, at, new AABB(at), ticks);
    }

    static void rig(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        intro(scene, "implantation_rig", "Installing implants on the Implantation Rig");
        BlockPos rig = util.grid().at(2, 1, 2);
        Selection drive = util.select().fromTo(2, 1, 3, 2, 1, 4);
        scene.world().showSection(util.select().position(rig), Direction.DOWN);
        scene.idle(10);
        say(scene, 70, "The Implantation Rig installs and removes implants while you sit on it", rig, util);
        scene.world().showSection(drive, Direction.NORTH);
        scene.world().setKineticSpeed(drive, 32);
        scene.world().setKineticSpeed(util.select().position(rig), 32);
        say(scene, 80, "It needs rotation from behind: 16 RPM for Tier I, up to 256 RPM for Tier V", rig, util);
        say(scene, 80, "Fill its tank with Bio-Interface Gel (Tiers I-III) or Neural Gel (IV-V) by pipe or bucket", rig, util);
        outline(scene, PonderPalette.RED, rig, 80);
        say(scene, 80, "Slowing down, running out of gel or leaving the chair aborts the operation", rig, util);
        scene.markAsFinished();
    }

    static void charger(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        intro(scene, "kinetic_charger", "Recharging at the Kinetic Charger");
        BlockPos pad = util.grid().at(2, 1, 2);
        Selection all = util.select().fromTo(2, 1, 2, 4, 1, 2);
        scene.world().showSection(all, Direction.DOWN);
        scene.world().setKineticSpeed(all, 64);
        scene.idle(10);
        say(scene, 70, "Stand on a powered Kinetic Charger to refill your Core's battery", pad, util);
        outline(scene, PonderPalette.GREEN, pad, 80);
        say(scene, 80, "It charges |RPM| / 8 Charge per second and stays idle below 16 RPM", pad, util);
        scene.markAsFinished();
    }

    static void etcher(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        intro(scene, "precision_etcher", "Etching chips with the Precision Etcher");
        BlockPos depot = util.grid().at(2, 1, 2);
        BlockPos etcher = util.grid().at(2, 3, 2);
        Selection head = util.select().fromTo(2, 3, 2, 4, 3, 2);
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(head, Direction.DOWN);
        scene.idle(10);
        say(scene, 70, "Mount the Precision Etcher one block above a depot or belt, like a Mechanical Press", etcher, util);
        scene.world().setKineticSpeed(head, 64);
        say(scene, 80, "Items below it are etched: Logic Chips need 64 RPM, Military Processors 128, Experimental Cores 256", depot, util);
        outline(scene, PonderPalette.RED, etcher, 80);
        say(scene, 80, "Below a recipe's speed the Etcher idles; Engineer's Goggles show the speed it needs", etcher, util);
        scene.world().setKineticSpeed(head, 256);
        say(scene, 80, "Etching steps also run inside sequenced assemblies, and faster shafts etch faster", depot, util);
        scene.markAsFinished();
    }

    static void port(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        intro(scene, "neural_port", "Linking machines to a cyberdeck");
        BlockPos machine = util.grid().at(2, 1, 2);
        BlockPos port = util.grid().at(2, 1, 1);
        Selection drive = util.select().fromTo(2, 1, 2, 2, 1, 4);
        scene.world().showSection(drive, Direction.DOWN);
        scene.world().setKineticSpeed(drive, 32);
        scene.idle(10);
        scene.world().showSection(util.select().position(port), Direction.SOUTH);
        say(scene, 70, "Place a Neural Port against any machine; name it in an anvil first", port, util);
        say(scene, 80, "Right-click your own port to bind it to your deck, then declare it with DEVICE x = port \"Name\"", port, util);
        outline(scene, PonderPalette.GREEN, machine, 80);
        say(scene, 80, "Scripts read the machine's speed and stress; better Machine Interfaces add redstone, stock and requests", machine, util);
        scene.markAsFinished();
    }

    static void sensor(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        intro(scene, "proximity_sensor", "Watching an area with the Proximity Sensor");
        BlockPos sensor = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
        scene.idle(10);
        say(scene, 70, "The Proximity Sensor emits a redstone signal while creatures are inside its detection box", sensor, util);
        outline(scene, PonderPalette.INPUT, sensor, 80);
        say(scene, 80, "Right-click to cycle what it detects, and sneak-right-click to change the box size", sensor, util);
        scene.markAsFinished();
    }

    private CyberPonderScenes() {
    }
}
