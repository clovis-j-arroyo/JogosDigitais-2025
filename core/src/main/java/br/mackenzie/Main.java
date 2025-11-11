package br.mackenzie;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ============================================================
// Classe NPC (personagem não jogável, com diálogo)
// ============================================================
class NPC {
    Vector2 position;
    TextureRegion sprite;
    String id;

    NPC(float x, float y, TextureRegion sprite, String id) {
        this.position = new Vector2(x, y);
        this.sprite = new TextureRegion(sprite);
        this.id = id;
    }
}

// ============================================================
// Classe Enemy (inimigo que persegue o jogador)
// ============================================================
class Enemy {
    Vector2 position;
    TextureRegion sprite;
    float speed = 80f;        // velocidade de movimento
    float detectRange = 250f; // distância para começar a perseguir

    Enemy(float x, float y, TextureRegion sprite) {
        this.position = new Vector2(x, y);
        this.sprite = sprite;
    }
}

// ============================================================
// Classe principal do jogo
// ============================================================
public class Main implements ApplicationListener {

    SpriteBatch batch;
    OrthographicCamera camera;
    BitmapFont font;

    TiledMap tiledMap;
    OrthogonalTiledMapRenderer mapRenderer;
    float tileSize = 64f;

    // Texturas do jogador
    Texture cyclistSide, cyclistFront, cyclistBack;
    TextureRegion currentSprite;
    Vector2 worldPosition;
    float baseSpeed = 200f, boostSpeed = 400f, speed;
    int direction = 0;               // 0-right,1-up,2-left,3-down
    float scale = 0.25f;
    float boostTimer = 0f;

    // Inimigos
    Texture enemyTexture;
    List<Enemy> enemies = new ArrayList<>();

    // NPCs e itens
    List<NPC> npcs = new ArrayList<>();
    Set<String> inventory = new HashSet<>();
    String currentDialog = null;
    int currentLevel = 1; // 1,2,3

    // ============================================================
    // Inicialização
    // ============================================================
    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        cyclistSide = new Texture("cyclist_side.png");
        cyclistFront = new Texture("cyclist_front.png");
        cyclistBack = new Texture("cyclist_back.png");
        enemyTexture = new Texture("GRASS+.png");

        currentSprite = new TextureRegion(cyclistSide);
        worldPosition = new Vector2(100, 100);
        speed = baseSpeed;

        loadLevel(1);
    }

    // ============================================================
    // Carrega um nível do mapa
    // ============================================================
    private void loadLevel(int level) {
        if (mapRenderer != null) mapRenderer.dispose();
        if (tiledMap != null) tiledMap.dispose();

        String tmxPath = "map" + level + ".tmx";
        tiledMap = new TmxMapLoader().load(tmxPath);
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f / tileSize, batch);
        mapRenderer.setView(camera);

        npcs.clear();
        enemies.clear();
        inventory.clear();
        currentLevel = level;

        // --- Carregar NPCs ---
        TextureRegion npcRegion = new TextureRegion(cyclistFront);
        if (tiledMap.getLayers().get("NPCs") != null) {
            for (MapObject obj : tiledMap.getLayers().get("NPCs").getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) obj).getRectangle();
                    float worldX = r.x * tileSize;
                    float worldY = r.y * tileSize;
                    String id = obj.getProperties().get("id", String.class);
                    npcs.add(new NPC(worldX, worldY, npcRegion, id));
                }
            }
        }

        // --- Carregar Inimigos ---
        TextureRegion enemyRegion = new TextureRegion(enemyTexture);
        if (tiledMap.getLayers().get("Enemies") != null) {
            for (MapObject obj : tiledMap.getLayers().get("Enemies").getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) obj).getRectangle();
                    float worldX = r.x * tileSize;
                    float worldY = r.y * tileSize;
                    enemies.add(new Enemy(worldX, worldY, enemyRegion));
                }
            }
        }

        if (!npcs.isEmpty()) {
            NPC first = npcs.get(0);
            worldPosition.set(first.position.x - 100, first.position.y);
        }
    }

    // ============================================================
    // Redimensionar tela
    // ============================================================
    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        camera.setToOrtho(false, width, height);
        if (mapRenderer != null) mapRenderer.setView(camera);
    }

    // ============================================================
    // Loop principal do jogo
    // ============================================================
    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        handleInput(delta);
        updateEnemies(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.2f, 0.3f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawNPCs();
        drawEnemies();
        drawPlayer();

        if (currentDialog != null) {
            font.draw(batch, currentDialog,
                camera.position.x - camera.viewportWidth / 2 + 20,
                camera.position.y - camera.viewportHeight / 2 + 50);
        }

        batch.end();
    }

    // ============================================================
    // Desenhar NPCs
    // ============================================================
    private void drawNPCs() {
        for (NPC npc : npcs) {
            float w = npc.sprite.getRegionWidth() * scale;
            float h = npc.sprite.getRegionHeight() * scale;
            batch.draw(npc.sprite, npc.position.x, npc.position.y, w, h);
        }
    }

    // ============================================================
    // Desenhar inimigos
    // ============================================================
    private void drawEnemies() {
        for (Enemy e : enemies) {
            float w = e.sprite.getRegionWidth() * scale;
            float h = e.sprite.getRegionHeight() * scale;
            batch.draw(e.sprite, e.position.x, e.position.y, w, h);
        }
    }

    // ============================================================
    // Desenhar jogador
    // ============================================================
    private void drawPlayer() {
        float w = currentSprite.getRegionWidth() * scale;
        float h = currentSprite.getRegionHeight() * scale;
        if (direction == 2) { // esquerda
            batch.draw(currentSprite, worldPosition.x + w, worldPosition.y, -w, h);
        } else {
            batch.draw(currentSprite, worldPosition.x, worldPosition.y, w, h);
        }
    }

    // ============================================================
    // Atualizar inimigos
    // ============================================================
    private void updateEnemies(float delta) {
        for (Enemy e : enemies) {
            float dist = worldPosition.dst(e.position);
            if (dist < e.detectRange) {
                Vector2 dir = new Vector2(worldPosition).sub(e.position).nor();
                e.position.add(dir.scl(e.speed * delta));
            }
        }
    }

    // ============================================================
    // Atualizar câmera
    // ============================================================
    private void updateCamera() {
        float targetX = worldPosition.x + (currentSprite.getRegionWidth() * scale) / 2f;
        float targetY = worldPosition.y + (currentSprite.getRegionHeight() * scale) / 2f;

        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;

        float worldW = tiledMap.getProperties().get("width", Integer.class) * tileSize;
        float worldH = tiledMap.getProperties().get("height", Integer.class) * tileSize;

        targetX = Math.max(halfW, Math.min(targetX, worldW - halfW));
        targetY = Math.max(halfH, Math.min(targetY, worldH - halfH));

        camera.position.set(targetX, targetY, 0);
    }

    // ============================================================
    // Entrada do jogador
    // ============================================================
    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (currentDialog != null) {
                if (currentDialog.contains("complete")) {
                    int next = currentLevel + 1;
                    if (next <= 3) Gdx.app.postRunnable(() -> loadLevel(next));
                }
                currentDialog = null;
            } else {
                NPC nearest = findNearestNPC();
                if (nearest != null) interactWith(nearest);
            }
        }

        if (currentDialog != null) return; // bloqueia movimento durante diálogo

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            direction = 0; currentSprite.setRegion(cyclistSide);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            direction = 2; currentSprite.setRegion(cyclistSide);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            direction = 1; currentSprite.setRegion(cyclistBack);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            direction = 3; currentSprite.setRegion(cyclistFront);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            speed = boostSpeed;
            boostTimer = 0.2f;
        } else {
            if (boostTimer > 0f) { boostTimer -= delta; speed = boostSpeed; }
            else speed = baseSpeed;
        }

        Vector2 move = new Vector2();
        switch (direction) {
            case 0: move.x = speed * delta; break;
            case 1: move.y = speed * delta; break;
            case 2: move.x = -speed * delta; break;
            case 3: move.y = -speed * delta; break;
        }
        worldPosition.add(move);

        float worldW = tiledMap.getProperties().get("width", Integer.class) * tileSize;
        float worldH = tiledMap.getProperties().get("height", Integer.class) * tileSize;
        float pw = currentSprite.getRegionWidth() * scale;
        float ph = currentSprite.getRegionHeight() * scale;
        worldPosition.x = Math.max(0, Math.min(worldPosition.x, worldW - pw));
        worldPosition.y = Math.max(0, Math.min(worldPosition.y, worldH - ph));
    }

    // ============================================================
    // Funções de NPC (interação e diálogo)
    // ============================================================
    private NPC findNearestNPC() {
        NPC best = null;
        float bestDistSq = 70f * 70f;
        for (NPC n : npcs) {
            float d = worldPosition.dst2(n.position);
            if (d < bestDistSq) { bestDistSq = d; best = n; }
        }
        return best;
    }

    private void interactWith(NPC npc) {
        String id = npc.id;

        // ===== Lógica de missões =====
        if (id.equals("l1n1")) {
            if (!inventory.contains("item1")) {
                inventory.add("item1");
                currentDialog = "Take this item1 to l1n2";
            } else currentDialog = "Hi!";
        } else if (id.equals("l1n2")) {
            if (inventory.contains("item1") && !inventory.contains("item2")) {
                inventory.remove("item1"); inventory.add("item2");
                currentDialog = "Thanks! Take item2 to l1n3";
            } else if (inventory.contains("item2")) currentDialog = "Hi!";
            else currentDialog = "I need item1";
        } else if (id.equals("l1n3")) {
            if (inventory.contains("item2")) {
                inventory.remove("item2");
                currentDialog = "Level 1 complete! Press E to continue...";
            } else currentDialog = "I need item2";
        } else if (id.equals("l2n1")) {
            if (!inventory.contains("item3")) {
                inventory.add("item3");
                currentDialog = "Take this item3 to l2n2";
            } else currentDialog = "Hi!";
        } else if (id.equals("l2n2")) {
            if (inventory.contains("item3") && !inventory.contains("item4")) {
                inventory.remove("item3"); inventory.add("item4");
                currentDialog = "Thanks! Take item4 to l2n3";
            } else if (inventory.contains("item4")) currentDialog = "Hi!";
            else currentDialog = "I need item3";
        } else if (id.equals("l2n3")) {
            if (inventory.contains("item4")) {
                inventory.remove("item4");
                currentDialog = "Level 2 complete! Press E to continue...";
            } else currentDialog = "I need item4";
        } else if (id.equals("l3n1")) {
            if (!inventory.contains("item5")) {
                inventory.add("item5");
                currentDialog = "Take this item5 to l3n2";
            } else currentDialog = "Hi!";
        } else if (id.equals("l3n2")) {
            if (inventory.contains("item5") && !inventory.contains("item6")) {
                inventory.remove("item5"); inventory.add("item6");
                currentDialog = "Thanks! Take item6 to l3n3";
            } else if (inventory.contains("item6")) currentDialog = "Hi!";
            else currentDialog = "I need item5";
        } else if (id.equals("l3n3")) {
            if (inventory.contains("item6")) {
                inventory.remove("item6");
                currentDialog = "You win! Thanks for playing.";
            } else currentDialog = "I need item6";
        }
    }

    // ============================================================
    // Finalização
    // ============================================================
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        cyclistSide.dispose();
        cyclistFront.dispose();
        cyclistBack.dispose();
        enemyTexture.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (tiledMap != null) tiledMap.dispose();
    }
}
