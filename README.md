#Noise4J

Simple map generators based on various procedural content generation tutorials.

I really did not want to enforce any kind of map&tile system that you would have to modify - or even copy - to your own implementation. The generators work on a simple 1D float array with a lightweight wrapper, that basically allows to use it as a 2D array. Scales or result values are never enforced - you can generate map with `[0,1]` values range (like in the examples), as well as `[0,1000]` - whatever suits you bests. `Grid` class also comes with some common math operations, so you can manually modify the values if you need to. 

More generators might be on their way. Making of a room/dungeon generator is somewhere on the TODO list.

## Dependency
Gradle dependency:
```
    compile 'com.github.czyzby:noise4j:0.0.1-SNAPSHOT'
```

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


### Combined

`NoiseGenerator` and `CellularAutomataGenerator` combined can generate maps similar to this:

![NoiseGenerator + CellularAutomataGenerator](https://github.com/czyzby/noise4j/blob/master/examples/noise%2Bcellular.png "NoiseGenerator + CellularAutomataGenerator")


On this scale, this might look like a cavern system, but you don't have to generate maps that big (or with the same parameters, for that matter). Note that here each tile is represented by a single pixel - given than the map's size is 512x512 tiles, with a relatively small tile image at 16x16px, this would take 67108864 (8192x8192) pixels to draw. With a smaller map and appropriate tiles, such map could be used to represent islands, for example.

## What can I do with the Grid...

By default, noise generator adds [0, `modifier`] to each cell on each generation.

Cellular generator sees cells as alive (`cell >= marker`) or dead (`cell < marker`); on each iteration, it can kill (`cell -= marker`) a cell with too few living neighbors or bring back to live (`cell += marker`) a dead cell with enough neighbors.

You'll usually end up creating a few `Grid`s, depending on your needs.

### Usage idea: islands
- Grid 1: use cellular generator with a higher radius (2-3). (Find sensible birth and death limits! The higher the radius, the higher the limits.)
- Grid 2: use noise generator with a few stages, with modifiers summing up to `1f`. This will be the height map.
- Grid 3: use noise generator with a few stages, with modifiers summing up to `1f`. This will be the moisture map.
- Combine grids: create an instance of your tiled map. If the cell is alive(/dead) in the first grid, tile of your map becomes water - if it is also close to the ground, it can become shallow water. If the cell is not water, check height and moisture values to determine tile type. For example, desert/canyon can be low and dry, forest can be medium-high and wet, swamp - low and wet, grass - medium all the way, etc. Trigger the generators' parameters for the most realistic maps with smooth terrain transitions.
