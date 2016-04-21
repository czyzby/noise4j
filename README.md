#Noise4J

Simple map generators based on various procedural content generation tutorials.

I really did not want to enforce any kind of map&tile system that you would have to modify, extend or even copy to your own implementation. The generators work on a simple 1D float array with a lightweight wrapper, that basically allows to use it as a 2D array. Result number ranges are never enforced - you can generate map with `[0,1]` values range (like in the examples), as well as `[0,1000]` - whatever suits you bests. `Grid` class also comes with some common math operations, so you can manually modify the values if you feel the need to. 

## Dependency
Gradle dependency:
```
    compile 'com.github.czyzby:noise4j:0.0.1-SNAPSHOT'
```
The first stable release will be available when I finally decide that "yeah, there are enough generators to make some simple games".

### LibGDX
While `Noise4J` was created with `LibGDX` games in mind, it has no external dependencies. It's GWT- and Java 6-compatible, so including it in `LibGDX` projects is pretty straightforward. Start with adding the mentioned Gradle dependency to the core project. Don't forget to also include the sources dependency in GWT project:
```
    compile 'com.github.czyzby:noise4j:0.0.1-SNAPSHOT:sources'
```

You need to inherit `Noise4J` GWT module in your `GdxDefinition`, otherwise GWT compiler will not recognize the classes:
```
	<inherits name='com.github.czyzby.noise4j.Noise4J' />
```

`Noise4J` does not use reflection, so its files usually do not need to be registered in any additional way.

## Noise generator

LibGDX usage example:

```
public class Example extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture texture;

    @Override
    public void create() {
        final Pixmap map = new Pixmap(512, 512, Format.RGBA8888);
        final Grid grid = new Grid(512);

        final NoiseGenerator noiseGenerator = new NoiseGenerator();
        noiseStage(grid, noiseGenerator, 32, 0.6f);
        noiseStage(grid, noiseGenerator, 16, 0.2f);
        noiseStage(grid, noiseGenerator, 8, 0.1f);
        noiseStage(grid, noiseGenerator, 4, 0.1f);
        noiseStage(grid, noiseGenerator, 1, 0.05f);

        final Color color = new Color();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                final float cell = grid.get(x, y);
                color.set(cell, cell, cell, 1f);
                map.drawPixel(x, y, Color.rgba8888(color));
            }
        }

        texture = new Texture(map);
        batch = new SpriteBatch();
        map.dispose();
    }

    private static void noiseStage(final Grid grid, final NoiseGenerator noiseGenerator, final int radius,
            final float modifier) {
        noiseGenerator.setRadius(radius);
        noiseGenerator.setModifier(modifier);
        // Seed ensures randomness, can be saved if you feel the need to
        // generate the same map in the future.
        noiseGenerator.setSeed(Generators.rollSeed());
        noiseGenerator.generate(grid);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        batch.draw(texture, 0f, 0f);
        batch.end();
    }

    @Override
    public void dispose() {
        texture.dispose();
        batch.dispose();
    }
}
```
![NoiseGenerator](https://github.com/czyzby/noise4j/blob/master/examples/noise.png "NoiseGenerator")

## Cellular automata generator

LibGDX usage example:

```
public class Example extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture texture;

    @Override
    public void create() {
        final Pixmap map = new Pixmap(512, 512, Format.RGBA8888);
        final Grid grid = new Grid(512);

        final CellularAutomataGenerator cellularGenerator = new CellularAutomataGenerator();
        cellularGenerator.setAliveChance(0.5f);
        cellularGenerator.setIterationsAmount(4);
        cellularGenerator.generate(grid);

        final Color color = new Color();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                final float cell = grid.get(x, y);
                color.set(cell, cell, cell, 1f);
                map.drawPixel(x, y, Color.rgba8888(color));
            }
        }

        texture = new Texture(map);
        batch = new SpriteBatch();
        map.dispose();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        batch.draw(texture, 0f, 0f);
        batch.end();
    }

    @Override
    public void dispose() {
        texture.dispose();
        batch.dispose();
    }
}
```
![CellularAutomataGenerator](https://github.com/czyzby/noise4j/blob/master/examples/cellular.png "CellularAutomataGenerator")


Bigger radius:
```
        final CellularAutomataGenerator cellularGenerator = new CellularAutomataGenerator();
        cellularGenerator.setAliveChance(0.5f);
        cellularGenerator.setRadius(2);
        cellularGenerator.setBirthLimit(13);
        cellularGenerator.setDeathLimit(9);
        cellularGenerator.setIterationsAmount(6);
        cellularGenerator.generate(grid);
```
![CellularAutomataGenerator](https://github.com/czyzby/noise4j/blob/master/examples/cellular-radius2.png "CellularAutomataGenerator")
Use more iterations for a smoother map. Keep in mind that using a big radius can make the algorithm significantly slower.


## Dungeon generator
LibGDX usage example:

```
public class Example extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture texture;

    @Override
    public void create() {
        final Pixmap map = new Pixmap(512, 512, Format.RGBA8888);
        final Grid grid = new Grid(512); // This algorithm likes odd-sized maps, although it works either way.

        final DungeonGenerator dungeonGenerator = new DungeonGenerator();
        dungeonGenerator.setRoomGenerationAttempts(500);
        dungeonGenerator.setMaxRoomSize(75);
        dungeonGenerator.setTolerance(10); // Max difference between width and height.
        dungeonGenerator.setMinRoomSize(9);
        dungeonGenerator.generate(grid);

        final Color color = new Color();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                final float cell = 1f - grid.get(x, y);
                color.set(cell, cell, cell, 1f);
                map.drawPixel(x, y, Color.rgba8888(color));
            }
        }

        texture = new Texture(map);
        batch = new SpriteBatch();
        map.dispose();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        batch.draw(texture, 0f, 0f);
        batch.end();
    }

    @Override
    public void dispose() {
        texture.dispose();
        batch.dispose();
    }
}
```
![DungeonGenerator](https://github.com/czyzby/noise4j/blob/master/examples/dungeon.png "DungeonGenerator")

This dungeon can be also used to create "perfect" mazes (as in: with one way to solve them):

```
        final DungeonGenerator dungeonGenerator = new DungeonGenerator();
        dungeonGenerator.setRoomGenerationAttempts(200);
        dungeonGenerator.setMaxRoomSize(25);
        dungeonGenerator.setTolerance(6);
        dungeonGenerator.setMinRoomSize(9);
        dungeonGenerator.setWindingChance(0.5f); // More chaotic!
        dungeonGenerator.setDeadEndRemovalIterations(5); // Introducing dead ends.
        dungeonGenerator.setRandomConnectorChance(0f); // One way to solve the maze.
        dungeonGenerator.generate(grid);
```
![DungeonGenerator](https://github.com/czyzby/noise4j/blob/master/examples/dungeon-maze.png "DungeonGenerator")

While these might seem chaotic (or even somewhat unplayable) at first sight, the generator is highly flexible - and no one is forcing you to create such huge maps(/rooms) either. This is a simple set-up for a rogue-like (256x256px, scaled x2):

![DungeonGenerator](https://github.com/czyzby/noise4j/blob/master/examples/dungeon-simple.png "DungeonGenerator")

In case you're wondering about the settings - it's simply `new DungeonGenerator().generate(grid)`. This is what comes out when you use the default setup, which features relatively small rooms. The one below, on the other hand, has custom min/max room sizes and rooms amount:

![DungeonGenerator](https://github.com/czyzby/noise4j/blob/master/examples/dungeon-tiny.png "DungeonGenerator")

### Combined

`NoiseGenerator` and `CellularAutomataGenerator` combined can generate maps similar to this:

![NoiseGenerator + CellularAutomataGenerator](https://github.com/czyzby/noise4j/blob/master/examples/noise%2Bcellular.png "NoiseGenerator + CellularAutomataGenerator")

On this scale, this might look somewhat like some cavern system, but you don't have to generate maps that big (or with the same parameters, for that matter). Note that here each tile is represented by a single pixel - given than the map's size is 512x512 tiles, with a relatively small tile image size at 16x16px, this map would still need 67108864 (8192x8192) pixels to be drawn. With a smaller map and appropriate tiles, such map could be just as easily used to represent islands, for example.

## What can I do with the Grid...

By default, **noise generator** adds `[0, modifier]` to each cell on each generation, smoothing values between the regions that you define. It ends up with "realistic" maps with smooth transitions between different regions.

**Cellular generator** sees cells as alive (`cell >= marker`) or dead (`cell < marker`); on each iteration, it can kill (`cell -= marker`) a cell with too few living neighbors or bring back to live (`cell += marker`) a dead cell with enough neighbors. It creates cave-like patterns.

**Dungeon generator** spawns multiple rooms and maze-like corridors between them, converting other cells to walls. Corridors and room floor values can be customized; wall value has to be higher than the other two (but `Grid` provides methods like `replace` or `negate`, so you can use pretty much any setup you need). As you can guess, it generates dungeons.

You'll usually end up creating a few `Grids` and merging them with your custom algorithms, depending on your needs.

### Usage idea: islands
- *Grid 1*: use cellular generator with a higher radius (2-3). (Find sensible birth and death limits! The higher the radius, the higher the limits.)
- *Grid 2*: use noise generator with a few stages, with modifiers summing up to `1f`. This will be the height map.
- *Grid 3*: use noise generator with a few stages, with modifiers summing up to `1f`. This will be the moisture map.
- Combine grids: create an instance of your tiled map. If the cell is alive(/dead) in the first grid, tile of your map becomes water - if it is also close to the ground, it can become shallow water. If the cell is not water, check height and moisture values to determine tile type. For example, desert/canyon can be low and dry, forest can be medium-high and wet, swamp - low and wet, grass - medium all the way, etc. Trigger the generators' parameters for the most realistic maps with smooth terrain transitions.
