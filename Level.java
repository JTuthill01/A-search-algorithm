package com.core.javagame.level;

import com.core.javagame.entity.Entity;
import com.core.javagame.entity.mob.Player;
import com.core.javagame.entity.particle.Particle;
import com.core.javagame.entity.projectile.Projectile;
import com.core.javagame.graphics.Screen;
import com.core.javagame.level.tile.Tile;
import com.core.javagame.utilities.Vector2i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Level
{
    public int width;
    public int height;
    private int[] tilesInt;
    protected int[] tiles;
    protected int tile_size;

    private int xScroll, yScroll;

    private List<Entity> entities = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();

    private List<Player> players = new ArrayList<>();

    private Comparator<Node> nodeSorter = (n0, n1) ->
    {
        if (n1.fCost < n0.fCost) return +1;
        if (n1.fCost > n0.fCost) return -1;
        return 0;
    };

    public static Level spawn = new SpawnLevel("/levels/spawn.png");

    public Level(int width, int height)
    {
        this.width = width;
        this.height = height;
        tilesInt = new int[width * height];
        generateLevel();
    }

    public Level(String path)
    {
        loadLevel(path);
        generateLevel();
    }

    protected void generateLevel()
    {
        for (int y = 0; y < 64; y++)
        {
            for (int x = 0; x < 64; x++)
            {
                getTile(x, y);
            }
        }
        tile_size = 16;
    }

    protected void loadLevel(String path) { }

    public void update()
    {
        for (Entity entity : entities)
            entity.update();

        for (Projectile projectile : projectiles)
            projectile.update();

        for (Particle particle : particles)
            particle.update();

        for (Player player : players)
            player.update();

        remove();
    }

    //public void onEvent(Event e) { getClientPlayer().onEvent(e); }

    private void remove()
    {
        for (int i = 0; i < entities.size(); i++)
            if (entities.get(i).isRemoved()) entities.removeIf(Entity::isRemoved);

        for (int p = 0; p < projectiles.size(); p++)
            if (projectiles.get(p).isRemoved()) projectiles.removeIf(Projectile::isRemoved);

        for (int i = 0; i < particles.size(); i++)
            if (particles.get(i).isRemoved()) particles.removeIf(Particle::isRemoved);

        for (int p = 0; p < players.size(); p++)
            if (players.get(p).isRemoved()) players.removeIf(Player::isRemoved);
    }

    public List<Projectile> getProjectiles() { return projectiles; }

    private void time() {}

    public boolean tileCollision(int x, int y, int size, int xOffset, int yOffset)
    {
        boolean solid = false;
        for (int c = 0; c < 4; c++)
        {
            int xt = (x - c % 2 * size + xOffset) >> 4;
            int yt = (y - c / 2 * size + yOffset) >> 4;
            if (getTile(xt, yt).isSolid()) solid = true;
        }
        return solid;
    }

    public void setScroll(int xScroll, int yScroll)
    {
        this.xScroll = xScroll;
        this.yScroll = yScroll;
    }

    public void render(int xScroll, int yScroll, Screen screen)
    {
        screen.setOffset(xScroll, yScroll);
        int x0 = xScroll >> 4;
        int x1 = (xScroll + screen.getWidth() + 16) >> 4;
        int y0 = yScroll >> 4;
        int y1 = (yScroll + screen.getHeight() + 16) >> 4;
        for (int y = y0; y < y1; y++)
        {
            for (int x = x0; x < x1; x++)
            {
                getTile(x, y).render(x, y, screen);
            }
        }

        for (Entity entity : entities)
            entity.render(screen);

        for (Projectile projectile : projectiles)
            projectile.render(screen);

        for (Particle particle : particles)
            particle.render(screen);

        for (Player player : players)
            player.render(screen);

    }

    public void add(Entity e)
    {
        e.init(this);

        if (e instanceof Particle) { particles.add((Particle) e); }

        else if (e instanceof Projectile) { projectiles.add((Projectile) e); }

        else if (e instanceof Player) { players.add((Player) e); }

        else { entities.add(e); }
    }

    public void addPlayer(Player player)
    {
        player.init(this);
        players.add(player);
    }

    public List<Player> getPlayers()
    {
        return players;
    }

    public Player getPlayerAt(int index)
    {
        return players.get(index);
    }

    public Player getClientPlayer()
    {
        return  players.get(0);
    }

    public List<Node> findPath(Vector2i start, Vector2i goal)
    {
        List<Node> openList = new ArrayList<>();
        List<Node> closedList = new ArrayList<>();
        Node current = new Node(start, null, 0, getDistance(start, goal));
        openList.add(current);
        while (openList.size() > 0)
        {
            Collections.sort(openList, nodeSorter);
            current = openList.get(0);
            if (current.tile.equals(goal))
            {
                List<Node> path = new ArrayList<>();
                while (current.parent != null)
                {
                    path.add(current);
                    current = current.parent;
                }
                openList.clear();
                closedList.clear();
                return path;
            }
            openList.remove(current);
            closedList.add(current);
            for (int i = 0; i < 9; i++)
            {
                if (i == 4) continue;
                int x = current.tile.getX();
                int y = current.tile.getY();
                int xi = (i % 3) - 1;
                int yi = (i / 3) - 1;
                Tile at = getTile(x + xi, y + yi);
                if (at == null) continue;
                if (at.isSolid()) continue;
                Vector2i a = new Vector2i(x + xi, y + yi);
                double gCost = current.gCost + (getDistance(current.tile, a) == 1 ? 1 : 0.95);
                double hCost = getDistance(a, goal);
                Node node = new Node(a, current, gCost, hCost);
                if (vecInList(closedList, a) && gCost >= node.gCost) continue;
                if (!vecInList(openList, a) || gCost < node.gCost) openList.add(node);
            }
        }
        closedList.clear();
        return null;
    }

    private boolean vecInList(List<Node> list, Vector2i vector)
    {
        for (Node n : list)
        {
            if (n.tile.equals(vector)) return true;
        }
        return false;
    }

    private double getDistance(Vector2i tile, Vector2i goal)
    {
        double dx = tile.getX() - goal.getX();
        double dy = tile.getY() - goal.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public List<Entity> getEntities(Entity e, int radius)
    {
        List<Entity> result = new ArrayList<>();
        int ex = e.getX();
        int ey = e.getY();
        for (int i = 0; i < entities.size(); i++)
        {
            Entity entity = entities.get(i);
            if (entity.equals(e)) continue;
            int x = entity.getX();
            int y = entity.getY();
            int dx = Math.abs(x - ex);
            int dy = Math.abs(y - ey);
            double distance = Math.sqrt((dx * dx) + (dy * dy));
            if (distance <= radius) result.add(entity);
        }
        return result;
    }

    public List<Player> getPlayers(Entity e, int radius)
    {
        List<Player> result = new ArrayList<>();
        int ex = e.getX();
        int ey = e.getY();
        for (int i = 0; i < players.size(); i++)
        {
            Player player = players.get(i);
            int x = player.getX();
            int y = player.getY();
            int dx = Math.abs(x - ex);
            int dy = Math.abs(y - ey);
            double distance = Math.sqrt((dx * dx) + (dy * dy));
            if (distance <= radius) result.add(player);
        }
        return result;
    }

    // Grass = 0xFF00FF00
// Flower = 0xFFFFFF00
// Rock = 0xFF7F7F00
    public Tile getTile(int x, int y)
    {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) return Tile.voidTile;
        if (tiles[x + y * getWidth()] == Tile.floor_color_spawn) return Tile.spawn_floor;
        if (tiles[x + y * getWidth()] == Tile.grass_color_spawn) return Tile.spawn_grass;
        if (tiles[x + y * getWidth()] == Tile.hedge_color_spawn) return Tile.spawn_hedge;
        if (tiles[x + y * getWidth()] == Tile.wall_1_color_spawn) return Tile.spawn_wall_1;
        if (tiles[x + y * getWidth()] == Tile.wall_2_color_spawn) return Tile.spawn_wall_2;
        if (tiles[x + y * getWidth()] == Tile.water_color_spawn) return Tile.spawn_water;
        return Tile.voidTile;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int[] getTilesInt()
    {
        return tilesInt;
    }
}
