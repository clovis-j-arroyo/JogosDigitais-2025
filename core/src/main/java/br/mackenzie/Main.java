package br.mackenzie;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Main implements ApplicationListener {
    SpriteBatch batch;
    OrthographicCamera camera;
    ShapeRenderer shapeRenderer; // <-- usado para desenhar hitboxes

    // Texturas do jogador
    Texture cyclistSideTexture;
    Texture cyclistFrontTexture;
    Texture cyclistBackTexture;
    TextureRegion currentSprite;

    // Fundo
    Texture backgroundTexture;

    // Posição e movimentação
    Vector2 worldPosition;
    Rectangle hitboxJogador;
    float speed = 200f;
    int direction = 0;
    float scale = 0.25f;

    // Mundo
    float worldWidth = 3200f;
    float worldHeight = 2400f;

    // Objetos com colisão
    Array<GameObject> objetos;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Carrega texturas do ciclista
        cyclistSideTexture = new Texture("cyclist_side.png");
        cyclistFrontTexture = new Texture("cyclist_front.png");
        cyclistBackTexture = new Texture("cyclist_back.png");
        currentSprite = new TextureRegion(cyclistSideTexture);

        // Fundo de grama
        backgroundTexture = new Texture("tall_grass.png");
        backgroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // Posição inicial
        worldPosition = new Vector2(worldWidth / 2f, worldHeight / 2f);
        updatePlayerHitbox();

        // Cria lista de objetos
        objetos = new Array<>();

        // Adiciona um objeto fixo no centro (tipo 1 → desaparece ao tocar)
        Texture specialGrass = new Texture("GRASS+.png");
        float centerX = worldWidth / 2f - specialGrass.getWidth() / 2f;
        float centerY = worldHeight / 2f - specialGrass.getHeight() / 2f;
        objetos.add(new GameObject(centerX, centerY, specialGrass, 1));
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        handleInput(delta);
        logic();
        updateCamera();

        Gdx.gl.glClearColor(0.2f, 0.3f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        drawTiledBackground();

        // Desenha os objetos
        for (GameObject obj : objetos) {
            obj.draw(batch);
        }

        drawPlayer();
        batch.end();

        // Desenha hitboxes (para debug)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(hitboxJogador.x, hitboxJogador.y, hitboxJogador.width, hitboxJogador.height);

        shapeRenderer.setColor(Color.BLUE);
        for (GameObject obj : objetos) {
            shapeRenderer.rect(obj.bounds.x, obj.bounds.y, obj.bounds.width, obj.bounds.height);
        }
        shapeRenderer.end();
    }

    private void drawTiledBackground() {
        float camX = camera.position.x - camera.viewportWidth / 2;
        float camY = camera.position.y - camera.viewportHeight / 2;

        int tileSize = 64;
        int tilesX = (int) (camera.viewportWidth / tileSize) + 2;
        int tilesY = (int) (camera.viewportHeight / tileSize) + 2;

        int startX = (int) (camX / tileSize) * tileSize;
        int startY = (int) (camY / tileSize) * tileSize;

        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                float drawX = startX + x * tileSize;
                float drawY = startY + y * tileSize;
                if (drawX >= 0 && drawX < worldWidth && drawY >= 0 && drawY < worldHeight) {
                    batch.draw(backgroundTexture, drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }

    private void drawPlayer() {
        float width = currentSprite.getRegionWidth() * scale;
        float height = currentSprite.getRegionHeight() * scale;

        if (direction == 2) {
            batch.draw(currentSprite, worldPosition.x + width, worldPosition.y, -width, height);
        } else {
            batch.draw(currentSprite, worldPosition.x, worldPosition.y, width, height);
        }
    }

    private void updateCamera() {
        float targetX = worldPosition.x + (currentSprite.getRegionWidth() * scale) / 2;
        float targetY = worldPosition.y + (currentSprite.getRegionHeight() * scale) / 2;

        float halfWidth = camera.viewportWidth / 2;
        float halfHeight = camera.viewportHeight / 2;

        targetX = Math.max(halfWidth, Math.min(targetX, worldWidth - halfWidth));
        targetY = Math.max(halfHeight, Math.min(targetY, worldHeight - halfHeight));

        camera.position.set(targetX, targetY, 0);
    }

    void handleInput(float delta) {
        Vector2 movement = new Vector2(0, 0);

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            movement.x += speed * delta;
            direction = 0;
            currentSprite.setRegion(cyclistSideTexture);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            movement.x -= speed * delta;
            direction = 2;
            currentSprite.setRegion(cyclistSideTexture);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            movement.y += speed * delta;
            direction = 1;
            currentSprite.setRegion(cyclistBackTexture);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            movement.y -= speed * delta;
            direction = 3;
            currentSprite.setRegion(cyclistFrontTexture);
        }

        // Aplica movimento
        worldPosition.add(movement);
        updatePlayerHitbox();

        // Mantém dentro do mundo
        float width = currentSprite.getRegionWidth() * scale;
        float height = currentSprite.getRegionHeight() * scale;
        worldPosition.x = Math.max(0, Math.min(worldPosition.x, worldWidth - width));
        worldPosition.y = Math.max(0, Math.min(worldPosition.y, worldHeight - height));
    }

    private void logic() {
        // Loop inverso para evitar problemas ao remover objetos
        for (int i = objetos.size - 1; i >= 0; i--) {
            GameObject obj = objetos.get(i);

            if (hitboxJogador.overlaps(obj.bounds)) {
                if (obj.tipo == 1) {
                    objetos.removeIndex(i); // tipo 1 → some ao tocar
                }
            }
        }
    }

    private void updatePlayerHitbox() {
        float width = currentSprite.getRegionWidth() * scale;
        float height = currentSprite.getRegionHeight() * scale;
        hitboxJogador = new Rectangle(worldPosition.x, worldPosition.y, width, height);
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        cyclistSideTexture.dispose();
        cyclistFrontTexture.dispose();
        cyclistBackTexture.dispose();
        backgroundTexture.dispose();
        for (GameObject obj : objetos) {
            obj.dispose();
        }
    }

    // Classe interna representando objetos sólidos
    static class GameObject {
        Vector2 pos;
        Texture texture;
        Rectangle bounds;
        int tipo;

        GameObject(float x, float y, Texture texture, int tipo) {
            this.pos = new Vector2(x, y);
            this.texture = texture;
            this.bounds = new Rectangle(x, y, texture.getWidth(), texture.getHeight());
            this.tipo = tipo;
        }

        void draw(SpriteBatch batch) {
            batch.draw(texture, pos.x, pos.y);
        }

        void dispose() {
            texture.dispose();
        }
    }
}
