package br.mackenzie;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main implements ApplicationListener {
    private SpriteBatch batch;
    private GameScreen gameScreen;

    @Override
    public void create() {
        batch = new SpriteBatch();
        gameScreen = new GameScreen();
        gameScreen.create();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.17f, 0.31f, 0.09f, 1f);
        float deltaTime = Gdx.graphics.getDeltaTime();
        gameScreen.update(deltaTime);
        gameScreen.render(batch);
    }

    @Override
    public void resize(int width, int height) {
        if (gameScreen != null) {
            gameScreen.resize(width, height);
        }
    }

    @Override
    public void pause() {
        System.out.println("Game paused");
    }

    @Override
    public void resume() {
        System.out.println("Game resumed");
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (gameScreen != null) {
            gameScreen.dispose();
        }
    }

    public static class Player {
        public float x, y;
        private float previousX, previousY;
        private static final float BASE_SPEED = 120f;
        public float currentSpeed = 0f;
        private static final float MAX_SPEED = 300f;
        private static final float ACCELERATION = 80f;
        private static final float DECELERATION = 150f;

        public float directionX = 0f;
        public float directionY = 0f;

        public int lives = 3;
        private boolean invincible = false;
        private float invincibilityTimer = 0f;
        private static final float INVINCIBILITY_DURATION = 1f;

        private Array<ItemInfo> inventory;
        private Texture texture;
        private TextureRegion currentFrame;
        private Rectangle bounds;

        private float animationTimer = 0f;
        private int animationFrame = 0;
        private static final float ANIMATION_SPEED = 0.15f;

        private long lastSpacePress = 0;
        private static final long SPACE_COOLDOWN = 50;

        public static class ItemInfo {
            public String name;
            public Color color;

            public ItemInfo(String name, Color color) {
                this.name = name;
                this.color = color;
            }
        }

        public Player(float x, float y, Texture texture) {
            this.x = x;
            this.y = y;
            this.previousX = x;
            this.previousY = y;
            this.texture = texture;
            this.currentFrame = new TextureRegion(texture);
            this.inventory = new Array<>();
            this.bounds = new Rectangle(x - 16, y - 16, 32, 32);
        }

        public void setDirection(float dx, float dy) {
            if (dx != 0 && dy != 0) {
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                this.directionX = dx / length;
                this.directionY = dy / length;
            } else {
                this.directionX = dx;
                this.directionY = dy;
            }
        }

        public void accelerate() {
            long currentTime = TimeUtils.millis();
            if (currentTime - lastSpacePress < SPACE_COOLDOWN) {
                return;
            }
            lastSpacePress = currentTime;
            if (directionX != 0 || directionY != 0) {
                currentSpeed = Math.min(MAX_SPEED, currentSpeed + ACCELERATION);
            }
        }

        public void storePreviousPosition() {
            previousX = x;
            previousY = y;
        }

        public void revertPosition() {
            x = previousX;
            y = previousY;
            currentSpeed = 0;
            bounds.setPosition(x - 16, y - 16);
        }

        public void update(float deltaTime) {
            currentSpeed = Math.max(0, currentSpeed - DECELERATION * deltaTime);
            if (currentSpeed > 0 && (directionX != 0 || directionY != 0)) {
                x += directionX * currentSpeed * deltaTime;
                y += directionY * currentSpeed * deltaTime;
                animationTimer += deltaTime;
                if (animationTimer > ANIMATION_SPEED) {
                    animationFrame = (animationFrame + 1) % 4;
                    animationTimer = 0;
                }
            }
            bounds.setPosition(x - 16, y - 16);

            // Update invincibility
            if (invincible) {
                invincibilityTimer += deltaTime;
                if (invincibilityTimer >= INVINCIBILITY_DURATION) {
                    invincible = false;
                    invincibilityTimer = 0f;
                }
            }
        }

        public void addItem(String name, Color color) {
            inventory.add(new ItemInfo(name, color));
        }

        public void removeItem(String item) {
            for (int i = 0; i < inventory.size; i++) {
                if (inventory.get(i).name.equals(item)) {
                    inventory.removeIndex(i);
                    return;
                }
            }
        }

        public boolean hasItem(String item) {
            for (ItemInfo info : inventory) {
                if (info.name.equals(item)) return true;
            }
            return false;
        }

        public Array<ItemInfo> getInventory() {
            return inventory;
        }

        public float getDistanceTo(NPC npc) {
            float dx = x - npc.x;
            float dy = y - npc.y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void loseLife() {
            if (!invincible) {
                lives--;
                invincible = true;
                invincibilityTimer = 0f;
                currentSpeed = 0;
            }
        }

        public boolean isInvincible() {
            return invincible;
        }

        public void resetLives() {
            lives = 3;
            invincible = false;
            invincibilityTimer = 0f;
        }

        public void render(SpriteBatch batch) {
            // Flash effect when invincible
            if (invincible && ((int)(invincibilityTimer * 10) % 2 == 0)) {
                return; // Skip rendering to create flashing effect
            }

            if (currentSpeed > 0) {
                float speedRatio = currentSpeed / MAX_SPEED;
                batch.setColor(1f, 1f, 0f, speedRatio * 0.3f);
                batch.draw(currentFrame, x - 32, y - 32, 64, 64);
                batch.setColor(1f, 1f, 1f, 1f);
            }
            batch.draw(currentFrame, x - 24, y - 24, 48, 48);
        }

        public void resetInventory() {
            inventory.clear();
        }
    }

    public static class NPC {
        public float x, y;
        public String name;
        public String dialogue;
        public String wantsItem;
        public boolean isCompleted = false;
        public String completionDialogue;
        public Color color;

        private Texture texture;
        private TextureRegion currentFrame;
        private Rectangle bounds;

        public NPC(float x, float y, String name, String dialogue, String wantsItem, Color color, Texture texture) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.dialogue = dialogue;
            this.wantsItem = wantsItem;
            this.color = color;
            this.texture = texture;
            this.currentFrame = new TextureRegion(texture);
            this.bounds = new Rectangle(x - 16, y - 16, 32, 32);
            this.completionDialogue = wantsItem != null ? name + ": Thank you so much!" : null;
        }

        public void complete() {
            this.isCompleted = true;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void render(SpriteBatch batch) {
            if (isCompleted) {
                batch.setColor(0.13f, 0.77f, 0.37f, 1f);
            } else {
                batch.setColor(color);
            }
            batch.draw(currentFrame, x - 32, y - 32, 64, 64);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    public static class Enemy {
        public float x, y;
        private Array<Vector2> patrolPath;
        private int currentPathIndex = 0;
        private float speed;

        private Texture texture;
        private TextureRegion currentFrame;
        private Rectangle bounds;

        private float animationTimer = 0f;
        private int animationFrame = 0;
        private static final float ANIMATION_SPEED = 0.2f;

        public Enemy(float x, float y, Array<Vector2> patrolPath, float speed, Texture texture) {
            this.x = x;
            this.y = y;
            this.patrolPath = patrolPath;
            this.speed = speed;
            this.texture = texture;
            this.currentFrame = new TextureRegion(texture);
            this.bounds = new Rectangle(x - 12, y - 12, 24, 24);
        }

        public void update(float deltaTime) {
            if (patrolPath.size == 0) return;

            Vector2 target = patrolPath.get(currentPathIndex);
            float dx = target.x - x;
            float dy = target.y - y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < 5f) {
                currentPathIndex = (currentPathIndex + 1) % patrolPath.size;
            } else {
                x += (dx / distance) * speed * deltaTime;
                y += (dy / distance) * speed * deltaTime;
                animationTimer += deltaTime;
                if (animationTimer > ANIMATION_SPEED) {
                    animationFrame = (animationFrame + 1) % 4;
                    animationTimer = 0;
                }
            }
            bounds.setPosition(x - 12, y - 12);
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void render(SpriteBatch batch) {
            batch.setColor(0.86f, 0.15f, 0.15f, 1f);
            float rotation = animationFrame * 5f;
            batch.draw(currentFrame, x - 16, y - 16, 16, 16, 32, 32, 1f, 1f, rotation);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    public static class Item {
        public float x, y;
        public String name;
        public String type;
        public Color color;

        private Texture texture;
        private TextureRegion currentFrame;
        private Rectangle bounds;

        private float bobTimer = 0f;
        private float sparkleTimer = 0f;

        public Item(float x, float y, String name, String type, Color color, Texture texture) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.type = type;
            this.color = color;
            this.texture = texture;
            this.currentFrame = new TextureRegion(texture);
            this.bounds = new Rectangle(x - 16, y - 16, 32, 32);
        }

        public void update(float deltaTime) {
            bobTimer += deltaTime * 2f;
            sparkleTimer += deltaTime * 3f;
            bounds.setPosition(x - 16, y - 16 + getBobOffset());
        }

        private float getBobOffset() {
            return (float) Math.sin(bobTimer) * 3f;
        }

        private boolean shouldSparkle() {
            return Math.sin(sparkleTimer) > 0.7f;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void render(SpriteBatch batch) {
            float bobOffset = getBobOffset();
            batch.setColor(color.r, color.g, color.b, 0.3f);
            batch.draw(currentFrame, x - 20, y - 20 + bobOffset, 40, 40);

            batch.setColor(color);
            batch.draw(currentFrame, x - 16, y - 16 + bobOffset, 32, 32);

            if (shouldSparkle()) {
                batch.setColor(1f, 1f, 1f, 1f);
            }
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    public static class Block {
        public float x, y, width, height;
        private Rectangle bounds;

        public Block(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.bounds = new Rectangle(x, y, width, height);
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
            shapeRenderer.rect(x, y, width, height);
        }
    }

    public static class LevelData {
        public static class NPCData {
            public float x, y;
            public String name, dialogue, wantsItem;
            public Color color;

            public NPCData(float x, float y, String name, String dialogue, String wantsItem, Color color) {
                this.x = x; this.y = y; this.name = name;
                this.dialogue = dialogue; this.wantsItem = wantsItem;
                this.color = color;
            }
        }

        public static class EnemyData {
            public float x, y;
            public Array<Vector2> patrolPath;
            public float speed;

            public EnemyData(float x, float y, Array<Vector2> patrolPath, float speed) {
                this.x = x; this.y = y; this.patrolPath = patrolPath;
                this.speed = speed;
            }
        }

        public static class ItemData {
            public float x, y;
            public String name, type;
            public Color color;

            public ItemData(float x, float y, String name, String type, Color color) {
                this.x = x; this.y = y; this.name = name; this.type = type;
                this.color = color;
            }
        }

        public static class BlockData {
            public float x, y, width, height;

            public BlockData(float x, float y, float width, float height) {
                this.x = x; this.y = y; this.width = width; this.height = height;
            }
        }

        public Vector2 playerStart;
        public Array<NPCData> npcs;
        public Array<EnemyData> enemies;
        public Array<ItemData> items;
        public Array<BlockData> blocks;
        public float mapWidth, mapHeight;

        public static LevelData createLevel1() {
            LevelData level = new LevelData();
            level.playerStart = new Vector2(100, 360);
            level.mapWidth = 1280;
            level.mapHeight = 720;

            Color redColor = new Color(0.93f, 0.28f, 0.28f, 1f);
            Color blueColor = new Color(0.28f, 0.50f, 0.93f, 1f);

            level.npcs = new Array<>();
            level.npcs.add(new NPCData(1100, 360, "Red Knight", "I need a Red Gem!", "Red Gem", redColor));
            level.npcs.add(new NPCData(640, 150, "Blue Mage", "Bring me the Blue Potion!", "Blue Potion", blueColor));

            level.enemies = new Array<>();
            Array<Vector2> patrol1 = new Array<>();
            patrol1.add(new Vector2(400, 360));
            patrol1.add(new Vector2(400, 550));
            patrol1.add(new Vector2(700, 550));
            patrol1.add(new Vector2(700, 360));
            level.enemies.add(new EnemyData(400, 360, patrol1, 80f));

            level.items = new Array<>();
            level.items.add(new ItemData(300, 250, "Red Gem", "gem", redColor));
            level.items.add(new ItemData(850, 600, "Blue Potion", "potion", blueColor));

            level.blocks = new Array<>();
            level.blocks.add(new BlockData(0, 0, 1280, 20));
            level.blocks.add(new BlockData(0, 0, 20, 720));
            level.blocks.add(new BlockData(1260, 0, 20, 720));
            level.blocks.add(new BlockData(0, 700, 1280, 20));
            level.blocks.add(new BlockData(300, 450, 150, 40));

            return level;
        }

        public static LevelData createLevel2() {
            LevelData level = new LevelData();
            level.playerStart = new Vector2(100, 360);
            level.mapWidth = 1280;
            level.mapHeight = 720;

            Color greenColor = new Color(0.28f, 0.93f, 0.50f, 1f);
            Color yellowColor = new Color(0.93f, 0.85f, 0.28f, 1f);
            Color purpleColor = new Color(0.70f, 0.28f, 0.93f, 1f);

            level.npcs = new Array<>();
            level.npcs.add(new NPCData(1150, 360, "Green Wizard", "I need a Green Scroll!", "Green Scroll", greenColor));
            level.npcs.add(new NPCData(640, 600, "Yellow Merchant", "Bring me the Yellow Key!", "Yellow Key", yellowColor));
            level.npcs.add(new NPCData(640, 100, "Purple Guard", "I want the Purple Crystal!", "Purple Crystal", purpleColor));

            level.enemies = new Array<>();
            Array<Vector2> patrol1 = new Array<>();
            patrol1.add(new Vector2(300, 200));
            patrol1.add(new Vector2(500, 200));
            patrol1.add(new Vector2(500, 400));
            patrol1.add(new Vector2(300, 400));
            level.enemies.add(new EnemyData(300, 200, patrol1, 90f));

            Array<Vector2> patrol2 = new Array<>();
            patrol2.add(new Vector2(800, 300));
            patrol2.add(new Vector2(900, 500));
            level.enemies.add(new EnemyData(800, 300, patrol2, 95f));

            level.items = new Array<>();
            level.items.add(new ItemData(200, 600, "Green Scroll", "gem", greenColor));
            level.items.add(new ItemData(1100, 150, "Yellow Key", "key", yellowColor));
            level.items.add(new ItemData(500, 500, "Purple Crystal", "gem", purpleColor));

            level.blocks = new Array<>();
            level.blocks.add(new BlockData(0, 0, 1280, 20));
            level.blocks.add(new BlockData(0, 0, 20, 720));
            level.blocks.add(new BlockData(1260, 0, 20, 720));
            level.blocks.add(new BlockData(0, 700, 1280, 20));
            level.blocks.add(new BlockData(200, 350, 150, 40));
            level.blocks.add(new BlockData(600, 250, 80, 200));

            return level;
        }

        public static LevelData createLevel3() {
            LevelData level = new LevelData();
            level.playerStart = new Vector2(100, 360);
            level.mapWidth = 1280;
            level.mapHeight = 720;

            Color orangeColor = new Color(0.93f, 0.55f, 0.28f, 1f);
            Color cyanColor = new Color(0.28f, 0.85f, 0.93f, 1f);
            Color pinkColor = new Color(0.93f, 0.28f, 0.70f, 1f);

            level.npcs = new Array<>();
            level.npcs.add(new NPCData(1150, 200, "Orange Warrior", "I need an Orange Shield!", "Orange Shield", orangeColor));
            level.npcs.add(new NPCData(200, 600, "Cyan Priest", "Bring me the Cyan Orb!", "Cyan Orb", cyanColor));
            level.npcs.add(new NPCData(1100, 600, "Pink Ranger", "I want the Pink Bow!", "Pink Bow", pinkColor));

            level.enemies = new Array<>();
            Array<Vector2> patrol1 = new Array<>();
            patrol1.add(new Vector2(400, 100));
            patrol1.add(new Vector2(600, 100));
            patrol1.add(new Vector2(600, 300));
            patrol1.add(new Vector2(400, 300));
            level.enemies.add(new EnemyData(400, 100, patrol1, 100f));

            Array<Vector2> patrol2 = new Array<>();
            patrol2.add(new Vector2(700, 400));
            patrol2.add(new Vector2(900, 400));
            patrol2.add(new Vector2(900, 600));
            patrol2.add(new Vector2(700, 600));
            level.enemies.add(new EnemyData(700, 400, patrol2, 105f));

            Array<Vector2> patrol3 = new Array<>();
            patrol3.add(new Vector2(400, 500));
            patrol3.add(new Vector2(500, 500));
            level.enemies.add(new EnemyData(400, 500, patrol3, 110f));

            level.items = new Array<>();
            level.items.add(new ItemData(900, 150, "Orange Shield", "key", orangeColor));
            level.items.add(new ItemData(550, 200, "Cyan Orb", "gem", cyanColor));
            level.items.add(new ItemData(300, 400, "Pink Bow", "gem", pinkColor));

            level.blocks = new Array<>();
            level.blocks.add(new BlockData(0, 0, 1280, 20));
            level.blocks.add(new BlockData(0, 0, 20, 720));
            level.blocks.add(new BlockData(1260, 0, 20, 720));
            level.blocks.add(new BlockData(0, 700, 1280, 20));
            level.blocks.add(new BlockData(250, 200, 100, 150));
            level.blocks.add(new BlockData(750, 150, 100, 100));
            level.blocks.add(new BlockData(500, 400, 150, 40));

            return level;
        }

        public static LevelData createLevel4() {
            LevelData level = new LevelData();
            level.playerStart = new Vector2(100, 100);
            level.mapWidth = 1280;
            level.mapHeight = 720;

            Color brownColor = new Color(0.65f, 0.45f, 0.28f, 1f);
            Color tealColor = new Color(0.28f, 0.75f, 0.65f, 1f);
            Color crimsonColor = new Color(0.85f, 0.15f, 0.28f, 1f);
            Color goldColor = new Color(0.93f, 0.75f, 0.28f, 1f);

            level.npcs = new Array<>();
            level.npcs.add(new NPCData(1150, 100, "Brown Monk", "I need a Brown Tablet!", "Brown Tablet", brownColor));
            level.npcs.add(new NPCData(100, 650, "Teal Sorcerer", "Bring me the Teal Ring!", "Teal Ring", tealColor));
            level.npcs.add(new NPCData(1150, 650, "Crimson Knight", "I want the Crimson Sword!", "Crimson Sword", crimsonColor));
            level.npcs.add(new NPCData(640, 360, "Gold King", "Bring me the Gold Crown!", "Gold Crown", goldColor));

            level.enemies = new Array<>();
            Array<Vector2> patrol1 = new Array<>();
            patrol1.add(new Vector2(300, 150));
            patrol1.add(new Vector2(500, 150));
            patrol1.add(new Vector2(500, 300));
            patrol1.add(new Vector2(300, 300));
            level.enemies.add(new EnemyData(300, 150, patrol1, 115f));

            Array<Vector2> patrol2 = new Array<>();
            patrol2.add(new Vector2(800, 150));
            patrol2.add(new Vector2(1000, 150));
            patrol2.add(new Vector2(1000, 300));
            patrol2.add(new Vector2(800, 300));
            level.enemies.add(new EnemyData(800, 150, patrol2, 120f));

            Array<Vector2> patrol3 = new Array<>();
            patrol3.add(new Vector2(300, 450));
            patrol3.add(new Vector2(500, 450));
            patrol3.add(new Vector2(500, 600));
            patrol3.add(new Vector2(300, 600));
            level.enemies.add(new EnemyData(300, 450, patrol3, 125f));

            Array<Vector2> patrol4 = new Array<>();
            patrol4.add(new Vector2(800, 450));
            patrol4.add(new Vector2(1000, 450));
            patrol4.add(new Vector2(1000, 600));
            patrol4.add(new Vector2(800, 600));
            level.enemies.add(new EnemyData(800, 450, patrol4, 130f));

            level.items = new Array<>();
            level.items.add(new ItemData(900, 200, "Brown Tablet", "key", brownColor));
            level.items.add(new ItemData(400, 250, "Teal Ring", "gem", tealColor));
            level.items.add(new ItemData(900, 500, "Crimson Sword", "key", crimsonColor));
            level.items.add(new ItemData(400, 500, "Gold Crown", "gem", goldColor));

            level.blocks = new Array<>();
            level.blocks.add(new BlockData(0, 0, 1280, 20));
            level.blocks.add(new BlockData(0, 0, 20, 720));
            level.blocks.add(new BlockData(1260, 0, 20, 720));
            level.blocks.add(new BlockData(0, 700, 1280, 20));
            level.blocks.add(new BlockData(580, 150, 120, 120));
            level.blocks.add(new BlockData(580, 450, 120, 120));
            level.blocks.add(new BlockData(200, 330, 200, 60));
            level.blocks.add(new BlockData(880, 330, 200, 60));

            return level;
        }

        public static LevelData createLevel5() {
            LevelData level = new LevelData();
            level.playerStart = new Vector2(640, 50);
            level.mapWidth = 1280;
            level.mapHeight = 720;

            Color indigoColor = new Color(0.35f, 0.28f, 0.75f, 1f);
            Color limeColor = new Color(0.65f, 0.93f, 0.28f, 1f);
            Color maroonColor = new Color(0.65f, 0.15f, 0.28f, 1f);
            Color silverColor = new Color(0.75f, 0.75f, 0.75f, 1f);
            Color amberColor = new Color(0.93f, 0.65f, 0.15f, 1f);

            level.npcs = new Array<>();
            level.npcs.add(new NPCData(100, 100, "Indigo Sage", "I need the Indigo Tome!", "Indigo Tome", indigoColor));
            level.npcs.add(new NPCData(1180, 100, "Lime Druid", "Bring me the Lime Staff!", "Lime Staff", limeColor));
            level.npcs.add(new NPCData(100, 620, "Maroon Berserker", "I want the Maroon Axe!", "Maroon Axe", maroonColor));
            level.npcs.add(new NPCData(1180, 620, "Silver Paladin", "Bring me the Silver Lance!", "Silver Lance", silverColor));
            level.npcs.add(new NPCData(240, 360, "Amber Archmage", "I need the Amber Amulet!", "Amber Amulet", amberColor));

            level.enemies = new Array<>();
            Array<Vector2> patrol1 = new Array<>();
            patrol1.add(new Vector2(350, 180));
            patrol1.add(new Vector2(450, 180));
            patrol1.add(new Vector2(450, 280));
            patrol1.add(new Vector2(350, 280));
            level.enemies.add(new EnemyData(350, 180, patrol1, 135f));

            Array<Vector2> patrol2 = new Array<>();
            patrol2.add(new Vector2(830, 180));
            patrol2.add(new Vector2(930, 180));
            patrol2.add(new Vector2(930, 280));
            patrol2.add(new Vector2(830, 280));
            level.enemies.add(new EnemyData(830, 180, patrol2, 140f));

            Array<Vector2> patrol3 = new Array<>();
            patrol3.add(new Vector2(350, 440));
            patrol3.add(new Vector2(450, 440));
            patrol3.add(new Vector2(450, 540));
            patrol3.add(new Vector2(350, 540));
            level.enemies.add(new EnemyData(350, 440, patrol3, 145f));

            Array<Vector2> patrol4 = new Array<>();
            patrol4.add(new Vector2(830, 440));
            patrol4.add(new Vector2(930, 440));
            patrol4.add(new Vector2(930, 540));
            patrol4.add(new Vector2(830, 540));
            level.enemies.add(new EnemyData(830, 440, patrol4, 150f));

            Array<Vector2> patrol5 = new Array<>();
            patrol5.add(new Vector2(580, 300));
            patrol5.add(new Vector2(700, 300));
            patrol5.add(new Vector2(700, 420));
            patrol5.add(new Vector2(580, 420));
            level.enemies.add(new EnemyData(580, 300, patrol5, 155f));

            Array<Vector2> patrol6 = new Array<>();
            patrol6.add(new Vector2(200, 150));
            patrol6.add(new Vector2(280, 150));
            level.enemies.add(new EnemyData(200, 150, patrol6, 160f));

            Array<Vector2> patrol7 = new Array<>();
            patrol7.add(new Vector2(1000, 570));
            patrol7.add(new Vector2(1100, 570));
            level.enemies.add(new EnemyData(1000, 570, patrol7, 165f));

            Array<Vector2> patrol8 = new Array<>();
            patrol8.add(new Vector2(640, 100));
            patrol8.add(new Vector2(640, 200));
            level.enemies.add(new EnemyData(640, 100, patrol8, 170f));

            level.items = new Array<>();
            level.items.add(new ItemData(250, 200, "Indigo Tome", "gem", indigoColor));
            level.items.add(new ItemData(1030, 200, "Lime Staff", "key", limeColor));
            level.items.add(new ItemData(250, 520, "Maroon Axe", "key", maroonColor));
            level.items.add(new ItemData(1030, 520, "Silver Lance", "key", silverColor));
            level.items.add(new ItemData(550, 360, "Amber Amulet", "gem", amberColor));

            level.blocks = new Array<>();
            level.blocks.add(new BlockData(0, 0, 1280, 20));
            level.blocks.add(new BlockData(0, 0, 20, 720));
            level.blocks.add(new BlockData(1260, 0, 20, 720));
            level.blocks.add(new BlockData(0, 700, 1280, 20));
            level.blocks.add(new BlockData(340, 250, 80, 80));
            level.blocks.add(new BlockData(860, 250, 80, 80));
            level.blocks.add(new BlockData(340, 390, 80, 80));
            level.blocks.add(new BlockData(860, 390, 80, 80));
            level.blocks.add(new BlockData(590, 330, 100, 60));

            return level;
        }
    }

    public class GameScreen {
        private static final float VIEWPORT_WIDTH = 1280f;
        private static final float VIEWPORT_HEIGHT = 720f;

        private float mapWidth;
        private float mapHeight;

        private OrthographicCamera camera;
        private Viewport viewport;
        private OrthographicCamera hudCamera;
        private Viewport hudViewport;

        private Player player;
        private Array<NPC> npcs;
        private Array<Enemy> enemies;
        private Array<Item> items;
        private Array<Block> blocks;

        private Texture playerTexture;
        private Texture npcTexture;
        private Texture enemyTexture;
        private Texture itemTexture;

        private ShapeRenderer shapeRenderer;
        private BitmapFont font;

        private Music backgroundMusic;
        private Sound collectSound;
        private Sound interactSound;
        private Sound victorySound;

        private int currentLevel = 1;
        private boolean gameComplete = false;
        private boolean gameOver = false;

        public void create() {
            camera = new OrthographicCamera();
            viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);

            hudCamera = new OrthographicCamera();
            hudViewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, hudCamera);
            hudCamera.position.set(VIEWPORT_WIDTH / 2, VIEWPORT_HEIGHT / 2, 0);

            playerTexture = new Texture("player.png");
            npcTexture = new Texture("npc.png");
            enemyTexture = new Texture("enemy.png");
            itemTexture = new Texture("item.png");

            shapeRenderer = new ShapeRenderer();
            font = new BitmapFont();
            font.getData().setScale(1.2f);

            loadLevel(1);

            try {
                backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background_music.ogg"));
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(0.5f);
                backgroundMusic.play();
            } catch (Exception e) {
                System.out.println("Could not load background music: " + e.getMessage());
            }

            try {
                collectSound = Gdx.audio.newSound(Gdx.files.internal("collect.mp3"));
                interactSound = Gdx.audio.newSound(Gdx.files.internal("interact.mp3"));
                victorySound = Gdx.audio.newSound(Gdx.files.internal("victory.mp3"));
            } catch (Exception e) {
                System.out.println("Could not load sound effects: " + e.getMessage());
            }
        }

        private void loadLevel(int levelNumber) {
            currentLevel = levelNumber;

            LevelData levelData;
            switch (levelNumber) {
                case 1: levelData = LevelData.createLevel1(); break;
                case 2: levelData = LevelData.createLevel2(); break;
                case 3: levelData = LevelData.createLevel3(); break;
                case 4: levelData = LevelData.createLevel4(); break;
                case 5: levelData = LevelData.createLevel5(); break;
                default:
                    gameComplete = true;
                    return;
            }

            mapWidth = levelData.mapWidth;
            mapHeight = levelData.mapHeight;

            // Store current lives before creating new player
            int currentLives = (player != null) ? player.lives : 3;

            player = new Player(levelData.playerStart.x, levelData.playerStart.y, playerTexture);

            // Reset lives to 3 on game restart (level 1 from defeat/victory)
            if (levelNumber == 1 && player.lives == 3 && currentLives != 3) {
                // This is a restart after losing lives
                System.out.println("Game restarted - Lives reset to 3");
            }

            player.lives = (levelNumber == 1) ? 3 : currentLives;

            // Recover 1 heart when reaching level 5
            if (levelNumber == 5 && player.lives < 3) {
                player.lives++;
                System.out.println("Life recovered! Lives: " + player.lives);
            }

            npcs = new Array<>();
            for (LevelData.NPCData npcData : levelData.npcs) {
                npcs.add(new NPC(npcData.x, npcData.y, npcData.name, npcData.dialogue, npcData.wantsItem, npcData.color, npcTexture));
            }

            enemies = new Array<>();
            for (LevelData.EnemyData enemyData : levelData.enemies) {
                enemies.add(new Enemy(enemyData.x, enemyData.y, enemyData.patrolPath, enemyData.speed, enemyTexture));
            }

            items = new Array<>();
            for (LevelData.ItemData itemData : levelData.items) {
                items.add(new Item(itemData.x, itemData.y, itemData.name, itemData.type, itemData.color, itemTexture));
            }

            blocks = new Array<>();
            for (LevelData.BlockData blockData : levelData.blocks) {
                blocks.add(new Block(blockData.x, blockData.y, blockData.width, blockData.height));
            }

            System.out.println("Loaded Level " + levelNumber + " - Lives: " + player.lives);
        }

        public void update(float deltaTime) {
            if (gameComplete) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    gameComplete = false;
                    player = null;

                    // Restart music when restarting from victory
                    if (backgroundMusic != null) {
                        backgroundMusic.play();
                    }

                    loadLevel(1);
                }
                return;
            }

            if (gameOver) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    gameOver = false;
                    player = null;

                    // Restart music before loading level
                    if (backgroundMusic != null) {
                        backgroundMusic.play();
                    }

                    loadLevel(1);
                }
                return;
            }

            handleInput(deltaTime);

            player.storePreviousPosition();
            player.update(deltaTime);

            player.x = Math.max(16, Math.min(mapWidth - 16, player.x));
            player.y = Math.max(16, Math.min(mapHeight - 16, player.y));

            // Check collision with blocks only
            for (Block block : blocks) {
                if (player.getBounds().overlaps(block.getBounds())) {
                    player.revertPosition();
                    break;
                }
            }

            for (Enemy enemy : enemies) {
                enemy.update(deltaTime);
                if (player.getBounds().overlaps(enemy.getBounds())) {
                    player.loseLife();

                    if (player.lives <= 0) {
                        gameOver = true;
                        if (backgroundMusic != null) {
                            backgroundMusic.stop();
                        }
                    }
                }
            }

            for (Item item : items) {
                item.update(deltaTime);
            }

            for (int i = items.size - 1; i >= 0; i--) {
                Item item = items.get(i);
                if (player.getBounds().overlaps(item.getBounds())) {
                    player.addItem(item.name, item.color);
                    items.removeIndex(i);

                    if (collectSound != null) {
                        collectSound.play(0.7f);
                    }
                }
            }

            float cameraX = Math.max(VIEWPORT_WIDTH / 2, Math.min(mapWidth - VIEWPORT_WIDTH / 2, player.x));
            float cameraY = Math.max(VIEWPORT_HEIGHT / 2, Math.min(mapHeight - VIEWPORT_HEIGHT / 2, player.y));
            camera.position.set(cameraX, cameraY, 0);
            camera.update();

            hudCamera.update();
        }

        private void handleInput(float deltaTime) {
            float dx = 0, dy = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx = -1;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx = 1;
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy = 1;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy = -1;

            player.setDirection(dx, dy);

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                // Always allow acceleration
                player.accelerate();

                // Also check for NPC interaction
                for (NPC npc : npcs) {
                    if (player.getDistanceTo(npc) < 50) {
                        handleNPCInteraction(npc);
                        break;
                    }
                }
            }
        }

        private void checkNPCInteractions() {
        }

        private void handleNPCInteraction(NPC npc) {
            if (npc.isCompleted) return;

            if (npc.wantsItem != null && player.hasItem(npc.wantsItem)) {
                player.removeItem(npc.wantsItem);
                npc.complete();
                System.out.println("Delivered " + npc.wantsItem + " to " + npc.name);

                if (interactSound != null) {
                    interactSound.play(0.8f);
                }

                boolean allCompleted = true;
                for (NPC n : npcs) {
                    if (!n.isCompleted) {
                        allCompleted = false;
                        break;
                    }
                }

                if (allCompleted) {
                    System.out.println("Level " + currentLevel + " Complete!");
                    player.currentSpeed = 0;
                    player.directionX = 0;
                    player.directionY = 0;

                    if (currentLevel < 5) {
                        loadLevel(currentLevel + 1);
                    } else {
                        gameComplete = true;
                        System.out.println("Game Complete!");

                        if (backgroundMusic != null) {
                            backgroundMusic.stop();
                        }

                        if (victorySound != null) {
                            victorySound.play(1.0f);
                        }
                    }
                }
            } else {
                System.out.println(npc.name + ": " + npc.dialogue);
            }
        }

        public void render(SpriteBatch batch) {
            if (gameComplete) {
                renderVictoryScreen(batch);
                return;
            }

            if (gameOver) {
                renderDefeatScreen(batch);
                return;
            }

            viewport.apply();
            batch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Block block : blocks) {
                block.render(shapeRenderer);
            }
            shapeRenderer.end();

            batch.begin();

            for (Item item : items) {
                item.render(batch);
            }

            for (NPC npc : npcs) {
                npc.render(batch);
            }

            for (Enemy enemy : enemies) {
                enemy.render(batch);
            }

            player.render(batch);

            batch.end();

            renderHUD(batch);
        }

        private void renderHUD(SpriteBatch batch) {
            hudViewport.apply();
            batch.setProjectionMatrix(hudCamera.combined);

            batch.begin();

            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Level: " + currentLevel, 20, VIEWPORT_HEIGHT - 20);

            // Draw lives with heart symbols
            font.setColor(Color.RED);
            String livesText = "Lives: ";
            for (int i = 0; i < player.lives; i++) {
                livesText += "♥ ";
            }
            font.draw(batch, livesText, 20, VIEWPORT_HEIGHT - 50);

            font.setColor(Color.WHITE);
            font.draw(batch, "Inventory:", 20, VIEWPORT_HEIGHT - 80);

            int yOffset = 105;
            for (Player.ItemInfo itemInfo : player.getInventory()) {
                font.setColor(itemInfo.color);
                font.draw(batch, "- " + itemInfo.name, 30, VIEWPORT_HEIGHT - yOffset);
                yOffset += 25;
            }

            font.setColor(Color.YELLOW);
            yOffset += 15;
            font.draw(batch, "Quests:", 20, VIEWPORT_HEIGHT - yOffset);
            yOffset += 30;

            for (NPC npc : npcs) {
                if (!npc.isCompleted && npc.wantsItem != null) {
                    font.setColor(npc.color);
                    font.draw(batch, npc.name + " needs:", 30, VIEWPORT_HEIGHT - yOffset);
                    yOffset += 25;
                    font.draw(batch, "  " + npc.wantsItem, 30, VIEWPORT_HEIGHT - yOffset);
                    yOffset += 30;
                }
            }

            batch.end();
        }

        private void renderVictoryScreen(SpriteBatch batch) {
            hudViewport.apply();
            batch.setProjectionMatrix(hudCamera.combined);

            batch.begin();

            font.getData().setScale(3f);
            font.setColor(Color.GOLD);
            String victoryText = "VICTORY!";
            float textWidth = 200;
            font.draw(batch, victoryText, VIEWPORT_WIDTH / 2 - textWidth, VIEWPORT_HEIGHT / 2 + 50);

            font.getData().setScale(1.5f);
            font.setColor(Color.WHITE);
            String completionText = "You completed all 5 levels!";
            font.draw(batch, completionText, VIEWPORT_WIDTH / 2 - 200, VIEWPORT_HEIGHT / 2);

            font.setColor(Color.YELLOW);
            String restartText = "Press R to restart";
            font.draw(batch, restartText, VIEWPORT_WIDTH / 2 - 150, VIEWPORT_HEIGHT / 2 - 50);

            batch.end();

            font.getData().setScale(1.2f);
        }

        private void renderDefeatScreen(SpriteBatch batch) {
            hudViewport.apply();
            batch.setProjectionMatrix(hudCamera.combined);

            batch.begin();

            font.getData().setScale(3f);
            font.setColor(Color.RED);
            String defeatText = "DEFEAT!";
            float textWidth = 150;
            font.draw(batch, defeatText, VIEWPORT_WIDTH / 2 - textWidth, VIEWPORT_HEIGHT / 2 + 50);

            font.getData().setScale(1.5f);
            font.setColor(Color.WHITE);
            String lostText = "You ran out of lives!";
            font.draw(batch, lostText, VIEWPORT_WIDTH / 2 - 170, VIEWPORT_HEIGHT / 2);

            font.setColor(Color.YELLOW);
            String restartText = "Press R to restart";
            font.draw(batch, restartText, VIEWPORT_WIDTH / 2 - 150, VIEWPORT_HEIGHT / 2 - 50);

            batch.end();

            font.getData().setScale(1.2f);
        }

        public void resize(int width, int height) {
            viewport.update(width, height, false);
            hudViewport.update(width, height, true);
        }

        public void dispose() {
            playerTexture.dispose();
            npcTexture.dispose();
            enemyTexture.dispose();
            itemTexture.dispose();
            shapeRenderer.dispose();
            font.dispose();

            if (backgroundMusic != null) {
                backgroundMusic.dispose();
            }
            if (collectSound != null) {
                collectSound.dispose();
            }
            if (interactSound != null) {
                interactSound.dispose();
            }
            if (victorySound != null) {
                victorySound.dispose();
            }
        }
    }
}
