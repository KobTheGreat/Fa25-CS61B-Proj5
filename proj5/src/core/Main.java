package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;
import utils.FileUtils;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static class zombie {
        int x, y;
        public zombie(int a, int b) {
            x = a;
            y = b;
        }
    }

    private static final int WIDTH = World.getWidth();
    private static final int HEIGHT = World.getHeight();
    private static final int HUD_HEIGHT = 2;
    private static final String SAVE_FILE = "save.txt";
    private static final int HP = 1;
    private static final int INT = 35;

    private final TERenderer ter;
    private TETile[][] world;
    private int avatarX;
    private int avatarY;
    private long seed;
    private int hp;
    private int interval;
    private int goldCount;
    private int waterCount;
    private int fireCount;
    private int woodCount;
    private int soilCount;
    private int killCount;
    private int result;
    private int gold;
    private String picked_element;
    private TETile ava;
    private final List<zombie> zombies = new ArrayList<>();

    public Main() {
        ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT, 0, HUD_HEIGHT);
    }

    // 显示主菜单，返回用户选择
    private char showMainMenu() {
        while (true) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.WHITE);

            Font titleFont = new Font("Monaco", Font.BOLD, 30);
            StdDraw.setFont(titleFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.7, "CS 61B: Element Magician");

            Font optionFont = new Font("Monaco", Font.PLAIN, 20);
            StdDraw.setFont(optionFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.5, "New Game (N)");
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.4, "Load Game (L)");
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.3, "Quit (Q)");

            StdDraw.show();
            // 添加 pause 防止菜单界面频闪
            //StdDraw.pause(20);

            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'n' || key == 'l' || key == 'q') {
                    return key;
                }
            }
        }
    }

    // 让用户输入种子
    private long getSeedInput() {
        StringBuilder seedStr = new StringBuilder();

        while (true) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.WHITE);

            Font titleFont = new Font("Monaco", Font.BOLD, 30);
            StdDraw.setFont(titleFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.7, "Enter Seed");

            Font inputFont = new Font("Monaco", Font.PLAIN, 20);
            StdDraw.setFont(inputFont);
            String displayText = !seedStr.isEmpty() ? seedStr.toString() : "";
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.5, displayText);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.3, "Press S to Start");

            StdDraw.show();
            //StdDraw.pause(20);

            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 's' && !seedStr.isEmpty()) {
                    try {
                        return Long.parseLong(seedStr.toString());
                    } catch (NumberFormatException e) {
                    }
                } else if (key >= '0' && key <= '9') {
                    seedStr.append(key);
                }
            }
        }
    }

    // 用给定的种子开始新游戏
    private void startNewGame(long gameSeed) {

        this.seed = gameSeed;
        this.hp = HP;
        this.goldCount = 0;
        this.waterCount = 0;
        this.fireCount = 0;
        this.woodCount = 0;
        this.soilCount = 0;
        this.killCount = 0;
        this.interval = INT;
        this.result = 0;
        this.gold = interval * 3;
        this.ava = Tileset.G_AVATAR;
        this.picked_element = null;

        World worldGen = new World(gameSeed);
        this.world = worldGen.generate();

        // 找到左下角第一个地板作为角色起始位置
        int[] startPos = World.findAvatarStart(world);
        if (startPos != null) {
            this.avatarX = startPos[0];
            this.avatarY = startPos[1];
            world[avatarX][avatarY] = ava;
        }
        for (int i = 0; i < 25; i++) {
            startPos = World.getRandomPos(world);
            zombie z = new zombie(startPos[0], startPos[1]);
            world[startPos[0]][startPos[1]] = Tileset.ZOMBIE;
            zombies.add(z);
        }

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                startPos = World.getRandomPos(world);
                switch (i) {
                    case 0: world[startPos[0]][startPos[1]] = Tileset.GOLD; break;
                    case 1: world[startPos[0]][startPos[1]] = Tileset.WATER; break;
                    case 2: world[startPos[0]][startPos[1]] = Tileset.FIRE; break;
                    case 3: world[startPos[0]][startPos[1]] = Tileset.WOOD; break;
                    case 4: world[startPos[0]][startPos[1]] = Tileset.SOIL; break;
                }
            }
        }

        playGame();

    }

    // 从保存文件加载游戏
    private void loadGame() {
        if (!FileUtils.fileExists(SAVE_FILE)) {
            startNewGame(0);
            return;
        }

        String saveData = FileUtils.readFile(SAVE_FILE);
        String[] parts = saveData.split("\n");

        try {
            int idx = 0;

            seed = Long.parseLong(parts[idx++]);
            int id = Integer.parseInt(parts[idx++]);
            if (id == 19) ava = Tileset.G_AVATAR;
            else if (id == 20) ava = Tileset.F_AVATAR;
            else ava = Tileset.AVATAR;
            avatarX = Integer.parseInt(parts[idx++]);
            avatarY = Integer.parseInt(parts[idx++]);
            hp = Integer.parseInt(parts[idx++]);
            goldCount = Integer.parseInt(parts[idx++]);
            waterCount = Integer.parseInt(parts[idx++]);
            fireCount = Integer.parseInt(parts[idx++]);
            woodCount = Integer.parseInt(parts[idx++]);
            soilCount = Integer.parseInt(parts[idx++]);
            killCount = Integer.parseInt(parts[idx++]);
            interval = Integer.parseInt(parts[idx++]);

            World worldGen = new World(seed);
            world = worldGen.generate();

            zombies.clear();
            String[] zinfo = parts[idx++].split(" ");
            int zcount = Integer.parseInt(zinfo[1]);

            for (int i = 0; i < zcount; i++) {
                String[] zp = parts[idx++].split(" ");
                int zx = Integer.parseInt(zp[0]);
                int zy = Integer.parseInt(zp[1]);
                zombies.add(new zombie(zx, zy));
                world[zx][zy] = Tileset.ZOMBIE;
            }

            if (parts[idx].equals("ITEMS")) idx++;
            while (idx < parts.length) {
                String[] it = parts[idx++].split(" ");
                String name = it[0];
                int x = Integer.parseInt(it[1]);
                int y = Integer.parseInt(it[2]);

                switch (name) {
                    case "gold": world[x][y] = Tileset.GOLD; break;
                    case "water": world[x][y] = Tileset.WATER; break;
                    case "fire": world[x][y] = Tileset.FIRE; break;
                    case "wood": world[x][y] = Tileset.WOOD; break;
                    case "soil": world[x][y] = Tileset.SOIL; break;
                }
            }

            world[avatarX][avatarY] = ava;
            playGame();

        } catch (Exception e) {
            startNewGame(0);
        }
    }

    // 保存游戏状态到文件
    private void saveGame() {
        StringBuilder sb = new StringBuilder();

        sb.append(seed).append("\n");
        sb.append(ava.id()).append("\n");
        sb.append(avatarX).append("\n");
        sb.append(avatarY).append("\n");
        sb.append(hp).append("\n");
        sb.append(goldCount).append("\n");
        sb.append(waterCount).append("\n");
        sb.append(fireCount).append("\n");
        sb.append(woodCount).append("\n");
        sb.append(soilCount).append("\n");
        sb.append(killCount).append("\n");
        sb.append(interval).append("\n");

        // 保存僵尸
        sb.append("ZOMBIES ").append(zombies.size()).append("\n");
        for (zombie z : zombies) {
            sb.append(z.x).append(" ").append(z.y).append("\n");
        }

        // 保存物品
        sb.append("ITEMS\n");
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                TETile t = world[x][y];
                if (t == Tileset.GOLD || t == Tileset.WATER || t == Tileset.FIRE || t == Tileset.WOOD || t == Tileset.SOIL) {
                    sb.append(t.description()).append(" ").append(x).append(" ").append(y).append("\n");
                }
            }
        }

        FileUtils.writeFile(SAVE_FILE, sb.toString());
    }

    // 主游戏循环，处理移动和渲染
    private void playGame() {
        boolean colonPressed = false;
        int cnt = 0;

        while (true) {
            if (result == 1) return;
            cnt++;
            if (gold > 0) {
                gold--;
                if (gold == 0)
                    ava = Tileset.AVATAR;
            }
            if (cnt % interval == 0) {
                if (cnt % (interval * 10) == 0) {
                    interval = INT;
                }
                for (int i = 0; i < zombies.size(); i++) {
                    move(zombies.get(i));
                }
                cnt = 0;
            }

            while (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (colonPressed) {
                    if (key == 'q') {
                        saveGame();
                        System.exit(0);
                    }
                    colonPressed = false;
                } else if (key == ':') {
                    colonPressed = true;
                } else {
                    colonPressed = false;

                    int newX = avatarX;
                    int newY = avatarY;

                    switch (key) {
                        case 'w': newY += 1; break;
                        case 's': newY -= 1; break;
                        case 'a': newX -= 1; break;
                        case 'd': newX += 1; break;
                    }

                    // 检查新位置是否有效（在地图内且是地板）
                    if (check_bound(newX, newY) && (gold > 0 || !world[newX][newY].equals(Tileset.WALL))) {
                        if (world[newX][newY].equals(Tileset.NOTHING)) gameOver();
                        world[avatarX][avatarY] = Tileset.FLOOR;
                        avatarX = newX;
                        avatarY = newY;
                        if (world[newX][newY].equals(Tileset.ZOMBIE)) {
                            battle(newX, newY);
                        } else if (!world[newX][newY].equals(Tileset.FLOOR) && !world[newX][newY].equals(Tileset.WALL)) {
                            collect(newX, newY);
                        }
                        world[avatarX][avatarY] = ava;
                    }
                }
            }
            renderWorld();
            //StdDraw.pause(20); // 添加 pause 以减少频闪
        }
    }

    private boolean check_bound(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    // 渲染世界和HUD
    private void renderWorld() {
        StdDraw.clear(Color.BLACK);
        ter.drawTiles(world);
        drawHUD();
        StdDraw.show();
    }

    private void collect(int x, int y) {
        TETile t = world[x][y];
        picked_element = t.description();
        if (t.equals(Tileset.GOLD)) {
            gold = interval * 12;
            ava = Tileset.G_AVATAR;
            goldCount++;
        } else if (t.equals(Tileset.WATER)) {
            interval *= 2;
            waterCount++;
        } else if (t.equals(Tileset.FIRE)) {
            ava = Tileset.F_AVATAR;
            fireCount++;
        } else if (t.equals(Tileset.WOOD)) {
            hp+=2;
            woodCount++;
        } else if ((t.equals(Tileset.SOIL))) {
            for (int i = x - 2; i <= x + 2; i++) {
                for (int j = y - 2; j <= y + 2; j++) {
                    if (check_bound(i, j) && !world[i][j].equals(ava) && Math.random() < 0.15) {
                        world[i][j] = Tileset.WALL;
                        if (world[i][j].equals(Tileset.ZOMBIE)) kill(i, j);
                    }
                }
            }
            soilCount++;
        }
        if (goldCount >= 1 && fireCount >= 1 && waterCount >= 1 && woodCount >= 1 && soilCount >= 1) {
            world[avatarX][avatarY] = Tileset.FLOOR;
            renderWorld();
            winGame();
        }
    }

    private void move(zombie z) {
        while (true) {
            int newX = z.x;
            int newY = z.y;
            double r = Math.random();
            if (r > 0.75) {
                newX++;
            } else if (r > 0.5) {
                newX--;
            } else if (r > 0.25) {
                newY++;
            } else {
                newY--;
            }
            if (check_bound(newX, newY) && !world[newX][newY].equals(Tileset.WALL)) {
                world[z.x][z.y] = Tileset.FLOOR;
                z.x = newX;
                z.y = newY;
                if (world[newX][newY].equals(ava)) {
                    battle(newX, newY);
                    world[newX][newY] = ava;
                } else {
                    world[z.x][z.y] = Tileset.ZOMBIE;
                }
                return;
            }
        }
    }

    private void battle(int x, int y) {
        if (gold == 0) hp--;
        if (ava == Tileset.F_AVATAR) {
            for (int i = x - 3; i <= x + 3; i++)
                for (int j = y - 3; j <= y + 3; j++)
                    if (check_bound(i, j) && world[i][j].equals(Tileset.ZOMBIE)) {
                        world[i][j] = Tileset.FLOOR;
                        kill(i, j);
                    }
            ava = gold > 0 ? Tileset.G_AVATAR : Tileset.AVATAR;
        }
        if (hp <= 0) {
            world[x][y] = Tileset.FLOOR;
            renderWorld();
            gameOver();
        } else {
            kill(x, y);
        }
    }

    private void kill(int x, int y) {
        killCount++;
        if (killCount == 10) winGame();
        for (int i = 0; i < zombies.size(); i++) {
            zombie z = zombies.get(i);
            if (z.x == x && z.y == y) {
                zombies.remove(i);
                break;
            }
        }
    }

    private void gameOver() {
        while (true) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.RED);

            Font titleFont = new Font("Monaco", Font.BOLD, 40);
            StdDraw.setFont(titleFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.6, "GAME OVER");

            Font statsFont = new Font("Monaco", Font.PLAIN, 20);
            StdDraw.setFont(statsFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.5, String.format(
                    "Final Score: Gold: %d  Water: %d  Fire: %d Wood: %d Soil: %d Kill: %d",
                    goldCount, waterCount, fireCount, woodCount, soilCount, killCount));

            Font optionFont = new Font("Monaco", Font.PLAIN, 16);
            StdDraw.setFont(optionFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.4, "Press Q to Quit");
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.35, "Press R to Main Menu");

            StdDraw.show();

            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'q') System.exit(0);;
                if (key == 'r') {
                    result = 1;
                    return;
                }
            }
        }
    }

    private void winGame() {
        while (true) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.YELLOW);

            Font titleFont = new Font("Monaco", Font.BOLD, 40);
            StdDraw.setFont(titleFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.6, "VICTORY!");

            Font statsFont = new Font("Monaco", Font.PLAIN, 20);
            StdDraw.setFont(statsFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.5, "You collected all elements!");
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.45,
                    String.format("Gold: %d  Water: %d  Fire: %d Wood: %d Soil: %d Kill: %d",
                            goldCount, waterCount, fireCount, woodCount, soilCount, killCount));
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.40,
                    String.format("Final HP: %d", hp));

            Font optionFont = new Font("Monaco", Font.PLAIN, 16);
            StdDraw.setFont(optionFont);
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.3, "Press Q to Quit");
            StdDraw.text(WIDTH / 2.0, HEIGHT * 0.25, "Press R to Main Menu");

            StdDraw.show();

            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'q') System.exit(0);
                if (key == 'r') {
                    result = 1;
                    return;
                }
            }
        }
    }


    // 绘制HUD，显示鼠标下的tile描述
    private void drawHUD() {
        double mouseX = StdDraw.mouseX();
        double mouseY = StdDraw.mouseY();

        int tileX = (int) mouseX;
        int tileY = (int) (mouseY - HUD_HEIGHT);

        String description = "";
        if (tileX >= 0 && tileX < WIDTH && tileY >= 0 && tileY < HEIGHT) {
            description = world[tileX][tileY].description();
        }

        // HUD底部黑色背景条
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(WIDTH / 2.0, HUD_HEIGHT / 2.0, WIDTH / 2.0, HUD_HEIGHT / 2.0);

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 14));
        // 显示在左下角区域
        StdDraw.textLeft(1, HUD_HEIGHT / 2.0, description);

        String status = String.format("HP: %d Gold: %d  Water: %d  Fire: %d Wood: %d Soil: %d Kill: %d",
        hp, goldCount, waterCount, fireCount, woodCount, soilCount, killCount);
        StdDraw.textRight(WIDTH - 1, HUD_HEIGHT / 2.0, status);

        if (picked_element != null) {
            String txt = "";
            Color c = Color.WHITE;
            switch (picked_element) {
                case "water": txt = "凌波微步 罗袜生尘"; c = Color.cyan; break;
                case "fire": txt = "待从头 收拾旧山河 朝天阙"; c = Color.red; break;
                case "wood": txt = "宁可食无肉 不可居无竹"; c = Color.green; break;
                case "gold": txt = "金戈铁马 气吞万里如虎"; c = Color.yellow; break;
                case "soil": txt = "地势坤 君子以厚德载物"; c = Color.orange; break;
            }
            StdDraw.setPenColor(c);
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 14));
            double xPosition = WIDTH / 2.0 - 5;
            StdDraw.text(xPosition, HUD_HEIGHT / 2.0, txt);
        }

        // 画一条白线作为分隔
        StdDraw.line(0, HUD_HEIGHT, WIDTH, HUD_HEIGHT);
    }

    public static void main(String[] args) {

        while (true) {
            Main game = new Main();
            char choice = game.showMainMenu();

            switch (choice) {
                case 'n':
                    long seed = game.getSeedInput();
                    System.out.println(seed);
                    game.startNewGame(seed);
                    break;
                case 'l':
                    game.loadGame();
                    break;
                case 'q':
                    System.exit(0);
                    break;
            }
        }
    }
}
